package com.odyssey.travelplanner.data

import com.odyssey.travelplanner.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.SettingsSessionManager
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
            }
            install(Postgrest)
            install(Storage)
            install(Functions)
        }
    }

    val persistentClient: SupabaseClient by lazy { newClient(rememberSession = true) }
    val sessionOnlyClient: SupabaseClient by lazy { newClient(rememberSession = false) }
    private var activeClient: SupabaseClient = sessionOnlyClient

    fun selectSessionPersistence(rememberSession: Boolean) {
        activeClient = if (rememberSession) persistentClient else sessionOnlyClient
    }

    suspend fun restorePersistentSession(): Boolean = runCatching {
        val restored = withContext(Dispatchers.IO) {
            persistentClient.auth.loadFromStorage(autoRefresh = false)
        }
        if (restored) {
            activeClient = persistentClient
            withContext(Dispatchers.IO) {
                persistentClient.auth.startAutoRefreshForCurrentSession()
            }
            return@runCatching true
        }
        sessionOnlyClient.auth.currentSessionOrNull() != null
    }.getOrDefault(false)

    fun clientForCurrentAuthFlow(): SupabaseClient = activeClient
}
