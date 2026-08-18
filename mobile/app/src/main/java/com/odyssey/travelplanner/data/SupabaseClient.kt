package com.odyssey.travelplanner.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class RememberedAccount(
    val id: String,
    val displayName: String,
    val email: String,
)

data class RememberedCredentials(
    val email: String,
    val password: String,
)

enum class AuthRestoreResult {
    RESTORED,
    NO_SESSION,
    FAILED,
}

object SupabaseProvider {
    private const val TAG = "SupabaseProvider"
    val isConfigured: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    private const val REMEMBERED_SESSION_PREFIX = "ramingo.auth.session."
    private const val LAST_ACTIVE_ACCOUNT_KEY = "ramingo.auth.last-account-id"
    private const val PENDING_REMEMBER_KEY = "ramingo.auth.remember.pending"
    private const val REMEMBERED_CREDENTIALS_KEY = "ramingo.auth.credentials"
    private const val CREDENTIALS_KEY_ALIAS = "ramingo.auth.credentials.key"

    private val authSettings: Settings by lazy { Settings() }
    private val sessionJson = Json { ignoreUnknownKeys = true }
    private val rememberedClients = mutableMapOf<String, SupabaseClient>()
    private val restoreMutex = Mutex()

    @Serializable
    private data class StoredCredentials(
        val email: String,
        val password: String,
    )

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

    // Kept as a compatibility client for the auth-session format used by
    // older app versions. The current active session always uses this client.
    val persistentClient: SupabaseClient by lazy {
        newClient(SettingsSessionManager(), autoSaveToStorage = true)
    }
    val sessionOnlyClient: SupabaseClient by lazy {
        newClient(MemorySessionManager(), autoSaveToStorage = false)
    }
    // Auth sessions are kept independently of the "remember credentials"
    // checkbox. This lets Android recreate the activity/process after the
    // screen is locked without treating that as an explicit logout.
    //
    // Written from IO coroutines at fourteen call sites and read from the main
    // thread by clientForCurrentAuthFlow(); @Volatile is what makes a write on
    // one thread visible to the next read on another. Without it a restored
    // session could still be read as the previous client.
    @Volatile
    private var activeClient: SupabaseClient = persistentClient

    private fun credentialsKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = (keyStore.getEntry(CREDENTIALS_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)
            ?.secretKey
        if (existing != null) return existing

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    CREDENTIALS_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)
                    .build(),
            )
        }.generateKey()
    }

    private fun encryptCredentials(credentials: RememberedCredentials): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, credentialsKey())
        val payload = sessionJson.encodeToString(
            StoredCredentials.serializer(),
            StoredCredentials(credentials.email, credentials.password),
        ).toByteArray(StandardCharsets.UTF_8)
        val encrypted = cipher.doFinal(payload)
        val combined = ByteArray(cipher.iv.size + encrypted.size)
        cipher.iv.copyInto(combined)
        encrypted.copyInto(combined, destinationOffset = cipher.iv.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptCredentials(value: String): RememberedCredentials? {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        if (combined.size <= 12) return null
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, credentialsKey(), GCMParameterSpec(128, iv))
        val stored = sessionJson.decodeFromString<StoredCredentials>(
            cipher.doFinal(encrypted).toString(StandardCharsets.UTF_8),
        )
        return RememberedCredentials(stored.email, stored.password)
    }

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

    /** Reads the optional login form data saved by the remember-credentials checkbox. */
    suspend fun loadRememberedCredentials(): RememberedCredentials? = withContext(Dispatchers.IO) {
        val stored = authSettings.getStringOrNull(REMEMBERED_CREDENTIALS_KEY) ?: return@withContext null
        runCatching { decryptCredentials(stored) }
            .getOrElse {
                // A key can become invalid after a device security reset. Do
                // not leave an unreadable blob that prevents future saves.
                authSettings.remove(REMEMBERED_CREDENTIALS_KEY)
                null
            }
    }

    /** Stores only the login form data; the auth session is handled separately. */
    suspend fun updateRememberedCredentials(
        email: String,
        password: String,
        remember: Boolean,
    ) = withContext(Dispatchers.IO) {
        if (!remember || email.isBlank() || password.isBlank()) {
            authSettings.remove(REMEMBERED_CREDENTIALS_KEY)
            return@withContext
        }
        runCatching {
            authSettings.putString(
                REMEMBERED_CREDENTIALS_KEY,
                encryptCredentials(RememberedCredentials(email.trim(), password)),
            )
        }
    }

    suspend fun clearRememberedCredentials() = withContext(Dispatchers.IO) {
        authSettings.remove(REMEMBERED_CREDENTIALS_KEY)
    }

    private suspend fun legacyAccountIds(): List<String> = withContext(Dispatchers.IO) {
        authSettings.keys
            .filter { it.startsWith(REMEMBERED_SESSION_PREFIX) }
            .map { it.removePrefix(REMEMBERED_SESSION_PREFIX) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    /** Migrates one account from the account-picker storage used by older builds. */
    private suspend fun migrateLegacyRememberedSession(accountId: String): UserSession? = withContext(Dispatchers.IO) {
        val legacyClient = rememberedClient(accountId)
        runCatching { legacyClient.auth.loadFromStorage(autoRefresh = false) }
        val legacySession = legacyClient.auth.currentSessionOrNull() ?: return@withContext null
        runCatching {
            persistentClient.auth.importSession(
                session = legacySession,
                autoRefresh = false,
                source = SessionSource.Storage,
            )
            persistentClient.auth.sessionManager.saveSession(legacySession)
            authSettings.putString(LAST_ACTIVE_ACCOUNT_KEY, accountId)
            clearClientSession(legacyClient)
            synchronized(rememberedClients) { rememberedClients.remove(accountId) }
        }.getOrNull()?.let { legacySession }
    }

    /** Migrates the only legacy account, or the one selected in a previous build. */
    private suspend fun migrateLegacyRememberedSession(): UserSession? {
        val accountIds = legacyAccountIds()
        val accountId = when {
            accountIds.size == 1 -> accountIds.single()
            else -> authSettings.getStringOrNull(LAST_ACTIVE_ACCOUNT_KEY)
                ?.takeIf { it in accountIds }
        } ?: return null
        return migrateLegacyRememberedSession(accountId)
    }

    /**
     * Restores the active auth session independently of the remember-credentials
     * checkbox. A saved session means the user is still authenticated until an
     * explicit sign-out, including after the Android process is recreated.
     */
    suspend fun restorePersistentSession(): AuthRestoreResult = restoreMutex.withLock {
        try {
            val inMemorySession = sessionOnlyClient.auth.currentSessionOrNull()
            if (inMemorySession != null) {
                persistentClient.auth.importSession(
                    session = inMemorySession,
                    autoRefresh = false,
                    source = SessionSource.Storage,
                )
                persistentClient.auth.sessionManager.saveSession(inMemorySession)
                clearClientSession(sessionOnlyClient)
                activeClient = persistentClient
                runCatching { persistentClient.auth.startAutoRefreshForCurrentSession() }
                authSettings.remove(PENDING_REMEMBER_KEY)
                return@withLock AuthRestoreResult.RESTORED
            }

            // Do not reload an already active session on every ON_RESUME. In
            // particular, reloading with auto-refresh=true can turn a
            // temporary network error into a false logout during screen lock.
            if (persistentClient.auth.currentSessionOrNull() != null) {
                activeClient = persistentClient
                runCatching { persistentClient.auth.startAutoRefreshForCurrentSession() }
                authSettings.remove(PENDING_REMEMBER_KEY)
                return@withLock AuthRestoreResult.RESTORED
            }

            // Read the stored session first. Refreshing is handled by the
            // auth client's auto-refresh job and must not gate showing the
            // already authenticated app after a process recreation.
            withContext(Dispatchers.IO) {
                persistentClient.auth.loadFromStorage(autoRefresh = false)
            }
            if (persistentClient.auth.currentSessionOrNull() != null) {
                activeClient = persistentClient
                runCatching { persistentClient.auth.startAutoRefreshForCurrentSession() }
                authSettings.remove(PENDING_REMEMBER_KEY)
                return@withLock AuthRestoreResult.RESTORED
            }

            if (migrateLegacyRememberedSession() != null) {
                activeClient = persistentClient
                runCatching { persistentClient.auth.startAutoRefreshForCurrentSession() }
                authSettings.remove(PENDING_REMEMBER_KEY)
                return@withLock AuthRestoreResult.RESTORED
            }

            activeClient = persistentClient
            authSettings.remove(PENDING_REMEMBER_KEY)
            AuthRestoreResult.NO_SESSION
        } catch (error: Throwable) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            Log.w(TAG, "Auth session restore failed: ${error::class.simpleName}")
            AuthRestoreResult.FAILED
        }
    }

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

    /** Completes a successful login and always persists the active auth session. */
    suspend fun finalizeAuthentication() {
        val sourceClient = activeClient
        val session = sourceClient.auth.currentSessionOrNull()
            ?: error("The authentication session is empty")
        if (sourceClient !== persistentClient) {
            persistentClient.auth.importSession(
                session = session,
                autoRefresh = true,
                source = SessionSource.Storage,
            )
            persistentClient.auth.sessionManager.saveSession(session)
            clearClientSession(sourceClient)
        }
        persistentClient.auth.sessionManager.saveSession(session)
        accountId(session).takeIf(String::isNotBlank)?.let {
            authSettings.putString(LAST_ACTIVE_ACCOUNT_KEY, it)
        }
        activeClient = persistentClient
        persistentClient.auth.startAutoRefreshForCurrentSession()
        authSettings.remove(PENDING_REMEMBER_KEY)
    }

    /** Selects the persistent auth client for a new flow. Credential saving is separate. */
    suspend fun prepareAuthentication() {
        authSettings.remove(PENDING_REMEMBER_KEY)
        activeClient = persistentClient
    }

    suspend fun activateRememberedAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        val client = rememberedClient(accountId)
        runCatching { client.auth.loadFromStorage(autoRefresh = false) }
        val session = client.auth.currentSessionOrNull()
        if (session == null) {
            removeRememberedAccount(accountId)
            return@withContext false
        }
        runCatching {
            persistentClient.auth.importSession(
                session = session,
                autoRefresh = false,
                source = SessionSource.Storage,
            )
            persistentClient.auth.sessionManager.saveSession(session)
            authSettings.putString(LAST_ACTIVE_ACCOUNT_KEY, accountId)
            clearClientSession(client)
            synchronized(rememberedClients) { rememberedClients.remove(accountId) }
            activeClient = persistentClient
            runCatching { persistentClient.auth.startAutoRefreshForCurrentSession() }
        }.isSuccess
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
            // Check-and-swap under the same lock that guards rememberedClients,
            // so a concurrent activation cannot slip between the two lines and
            // leave the removed account's client active.
            if (activeClient === client) {
                activeClient = persistentClient
            }
        }
    }

    suspend fun ensureActiveSession(): Boolean = withContext(Dispatchers.IO) {
        val clients = listOf(activeClient, sessionOnlyClient, persistentClient).distinct()
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
        for (client in listOf(activeClient, sessionOnlyClient, persistentClient).distinct()) {
            clearClientSession(client)
        }
        activeClient = persistentClient
    }

    suspend fun signOutForAccountPicker() {
        for (client in listOf(activeClient, sessionOnlyClient, persistentClient).distinct()) {
            clearClientSession(client)
        }
        activeClient = persistentClient
    }

    fun clientForCurrentAuthFlow(): SupabaseClient = activeClient
}
