package com.odyssey.travelplanner.data

import com.odyssey.travelplanner.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseProvider {
    val isConfigured: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    private fun newClient(rememberSession: Boolean): SupabaseClient {
        check(isConfigured) { "Supabase is not configured." }
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        ) {
            httpEngine = OkHttp.create()
            install(Auth) {
                sessionManager = if (rememberSession) SettingsSessionManager() else MemorySessionManager()
                autoLoadFromStorage = false
                autoSaveToStorage = rememberSession
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

    val persistentClient: SupabaseClient by lazy { newClient(rememberSession = true) }
    val sessionOnlyClient: SupabaseClient by lazy { newClient(rememberSession = false) }
    private var activeClient: SupabaseClient = sessionOnlyClient

    suspend fun selectSessionPersistence(rememberSession: Boolean) {
        if (!rememberSession) {
            persistentClient.auth.stopAutoRefreshForCurrentSession()
            persistentClient.auth.clearSession()
            persistentClient.auth.sessionManager.deleteSession()
        }
        activeClient = if (rememberSession) persistentClient else sessionOnlyClient
    }

    suspend fun restorePersistentSession(): Boolean = runCatching {
        val restored = withContext(Dispatchers.IO) {
            persistentClient.auth.loadFromStorage(autoRefresh = true)
        }
        if (restored && persistentClient.auth.currentUserOrNull() != null) {
            activeClient = persistentClient
            withContext(Dispatchers.IO) {
                persistentClient.auth.startAutoRefreshForCurrentSession()
            }
            return@runCatching true
        }
        activeClient = sessionOnlyClient
        sessionOnlyClient.auth.currentUserOrNull() != null
    }.getOrDefault(false)

    suspend fun ensureActiveSession(): Boolean = withContext(Dispatchers.IO) {
        val client = activeClient
        if (client.auth.currentUserOrNull() != null) return@withContext true
        runCatching { client.auth.refreshCurrentSession() }.isSuccess &&
            client.auth.currentUserOrNull() != null
    }

    suspend fun clearActiveSessionLocally() {
        activeClient.auth.stopAutoRefreshForCurrentSession()
        activeClient.auth.clearSession()
        activeClient.auth.sessionManager.deleteSession()
    }

    fun clientForCurrentAuthFlow(): SupabaseClient = activeClient
}
