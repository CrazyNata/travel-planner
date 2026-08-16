package com.odyssey.travelplanner.data

import com.odyssey.travelplanner.BuildConfig
import com.russhwolf.settings.Settings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class RememberedAccount(
    val id: String,
    val displayName: String,
    val email: String,
)

object SupabaseProvider {
    val isConfigured: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    private const val REMEMBERED_SESSION_PREFIX = "ramingo.auth.session."

    private val authSettings: Settings by lazy { Settings() }
    private val sessionJson = Json { ignoreUnknownKeys = true }
    private val rememberedClients = mutableMapOf<String, SupabaseClient>()

    private fun newClient(
        sessionManager: SessionManager,
        autoSaveToStorage: Boolean,
    ): SupabaseClient {
        check(isConfigured) { "Supabase is not configured." }
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        ) {
            httpEngine = OkHttp.create()
            install(Auth) {
                this.sessionManager = sessionManager
                autoLoadFromStorage = false
                this.autoSaveToStorage = autoSaveToStorage
                flowType = FlowType.PKCE
                scheme = "https"
                host = "ramingo.online"
                defaultRedirectUrl = "https://ramingo.online/mobile/auth"
            }
            install(Postgrest)
            install(Storage)
            install(Functions)
        }
    }

    // Kept as a compatibility client for the single-session format used by
    // older app versions. New remembered sessions are stored below by user id.
    val persistentClient: SupabaseClient by lazy {
        newClient(SettingsSessionManager(), autoSaveToStorage = true)
    }
    val sessionOnlyClient: SupabaseClient by lazy {
        newClient(MemorySessionManager(), autoSaveToStorage = false)
    }
    private var activeClient: SupabaseClient = sessionOnlyClient

    private fun rememberedSessionKey(accountId: String): String =
        REMEMBERED_SESSION_PREFIX + accountId

    private fun rememberedClient(accountId: String): SupabaseClient = synchronized(rememberedClients) {
        rememberedClients.getOrPut(accountId) {
            newClient(
                SettingsSessionManager(
                    authSettings,
                    rememberedSessionKey(accountId),
                    sessionJson,
                ),
                autoSaveToStorage = true,
            )
        }
    }

    private fun accountId(session: UserSession): String = session.user?.id?.toString().orEmpty()

    private fun rememberedAccount(client: SupabaseClient): RememberedAccount? {
        val user = client.auth.currentUserOrNull() ?: return null
        val email = user.email.orEmpty()
        val metadataName = user.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val displayName = metadataName
            .ifBlank { email.substringBefore("@").trim() }
            .ifBlank { "Ramingo" }
        return RememberedAccount(
            id = user.id.toString(),
            displayName = displayName,
            email = email,
        )
    }

    /** Returns every auth client so the UI can observe sign-out events after a switch. */
    fun authClients(): List<SupabaseClient> = synchronized(rememberedClients) {
        (listOf(sessionOnlyClient, persistentClient) + rememberedClients.values).distinct()
    }

    /** Reads named sessions without selecting any of them. */
    suspend fun loadRememberedAccounts(): List<RememberedAccount> = withContext(Dispatchers.IO) {
        val accountIds = authSettings.keys
            .filter { it.startsWith(REMEMBERED_SESSION_PREFIX) }
            .map { it.removePrefix(REMEMBERED_SESSION_PREFIX) }
            .filter { it.isNotBlank() }
            .distinct()

        accountIds.mapNotNull { accountId ->
            val client = rememberedClient(accountId)
            runCatching { client.auth.loadFromStorage(autoRefresh = false) }
            rememberedAccount(client) ?: runCatching {
                client.auth.sessionManager.deleteSession()
            }.getOrNull().let { null }
        }.sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName.ifBlank { it.email } },
        )
    }

    /**
     * Restores an in-memory callback session first. If the old one-account
     * storage contains a remembered session, migrate it to per-account storage
     * and show the account chooser instead of silently picking it.
     */
    suspend fun restorePersistentSession(): Boolean = runCatching {
        if (sessionOnlyClient.auth.currentUserOrNull() != null) {
            activeClient = sessionOnlyClient
            return@runCatching true
        }

        if (loadRememberedAccounts().isNotEmpty()) {
            activeClient = sessionOnlyClient
            return@runCatching false
        }

        val restoredLegacy = withContext(Dispatchers.IO) {
            persistentClient.auth.loadFromStorage(autoRefresh = false)
        }
        val legacySession = persistentClient.auth.currentSessionOrNull()
        if (restoredLegacy && legacySession != null) {
            check(saveRememberedSession(legacySession)) { "Could not migrate the authentication session" }
            clearClientSession(persistentClient)
            activeClient = sessionOnlyClient
            return@runCatching false
        }

        activeClient = sessionOnlyClient
        sessionOnlyClient.auth.currentUserOrNull() != null
    }.getOrDefault(false)

    private suspend fun saveRememberedSession(session: UserSession): Boolean {
        val id = accountId(session)
        if (id.isBlank()) return false
        val client = rememberedClient(id)
        return runCatching {
            client.auth.importSession(
                session = session,
                autoRefresh = true,
                source = SessionSource.Storage,
            )
            // importSession persists when auto-save is enabled. Saving once
            // explicitly also makes migration independent of that flag.
            client.auth.sessionManager.saveSession(session)
            client.auth.startAutoRefreshForCurrentSession()
            activeClient = client
        }.isSuccess
    }

    /** Completes a successful login and moves remembered sessions to a named key. */
    suspend fun finalizeAuthentication(rememberSession: Boolean) {
        if (rememberSession) {
            val session = activeClient.auth.currentSessionOrNull()
                ?: error("The authentication session is empty")
            check(saveRememberedSession(session)) { "Could not save the authentication session" }
            if (activeClient !== persistentClient) {
                clearClientSession(persistentClient)
            }
        } else {
            activeClient = sessionOnlyClient
        }
    }

    suspend fun selectSessionPersistence(rememberSession: Boolean) {
        if (rememberSession) {
            // This compatibility client is only a staging client for a new
            // login. Never let it replace another account's named session.
            clearClientSession(persistentClient)
            activeClient = persistentClient
        } else {
            activeClient = sessionOnlyClient
        }
    }

    suspend fun activateRememberedAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        val client = rememberedClient(accountId)
        runCatching { client.auth.loadFromStorage(autoRefresh = true) }
        if (client.auth.currentUserOrNull() == null) {
            removeRememberedAccount(accountId)
            return@withContext false
        }
        activeClient = client
        true
    }

    suspend fun removeRememberedAccount(accountId: String) {
        val client = synchronized(rememberedClients) { rememberedClients[accountId] }
        if (client != null) {
            clearClientSession(client)
        } else {
            authSettings.remove(rememberedSessionKey(accountId))
        }
        synchronized(rememberedClients) {
            rememberedClients.remove(accountId)
        }
        if (activeClient === client) {
            activeClient = sessionOnlyClient
        }
    }

    suspend fun ensureActiveSession(): Boolean = withContext(Dispatchers.IO) {
        // Do not silently switch to another remembered account if the active
        // account expires. Account switching is always explicit in the UI.
        val clients = if (activeClient === sessionOnlyClient) {
            listOf(sessionOnlyClient, persistentClient)
        } else {
            listOf(activeClient)
        }
        for (client in clients) {
            if (client.auth.currentUserOrNull() != null) {
                activeClient = client
                return@withContext true
            }
        }
        for (client in clients) {
            val refreshed = runCatching { client.auth.refreshCurrentSession() }.isSuccess
            if (refreshed && client.auth.currentUserOrNull() != null) {
                activeClient = client
                client.auth.startAutoRefreshForCurrentSession()
                return@withContext true
            }
        }
        false
    }

    private suspend fun clearClientSession(client: SupabaseClient) {
        client.auth.stopAutoRefreshForCurrentSession()
        client.auth.clearSession()
        client.auth.sessionManager.deleteSession()
    }

    suspend fun clearActiveSessionLocally() {
        val client = activeClient
        clearClientSession(client)
        activeClient = sessionOnlyClient
    }

    /**
     * Leaves the current account without deleting a remembered login.
     *
     * A normal sign-out should return to the account chooser when the user
     * selected "Remember me". The remembered client keeps its session in
     * storage, while the app stops treating it as the active client. The
     * explicit "Remove from device" action is still responsible for deleting
     * that stored session.
     */
    suspend fun signOutForAccountPicker() {
        if (activeClient === sessionOnlyClient) {
            clearClientSession(sessionOnlyClient)
        } else {
            activeClient.auth.stopAutoRefreshForCurrentSession()
            activeClient = sessionOnlyClient
        }
    }

    fun clientForCurrentAuthFlow(): SupabaseClient = activeClient
}
