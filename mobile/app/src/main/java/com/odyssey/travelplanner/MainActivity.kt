package com.odyssey.travelplanner

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.mapbox.common.MapboxOptions
import com.odyssey.travelplanner.ui.OdysseyApp
import com.odyssey.travelplanner.data.SupabaseProvider
import io.github.jan.supabase.auth.handleDeeplinks
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal sealed interface AndroidDeepLink {
    data class Invite(val tripId: String) : AndroidDeepLink
    data object PasswordReset : AndroidDeepLink
}

private val supportedAppLinkHosts = setOf(
    "ramingo.online",
    "travelplanner.muntim.ru",
)

internal fun parseAndroidDeepLink(value: String?): AndroidDeepLink? {
    val uri = runCatching { URI(value.orEmpty()) }.getOrNull() ?: return null
    if (uri.scheme != "https" || uri.host !in supportedAppLinkHosts) return null
    return when {
        uri.path?.startsWith("/mobile/invite") == true -> {
            val tripId = uri.rawQuery.orEmpty()
                .split('&')
                .mapNotNull { pair ->
                    val parts = pair.split('=', limit = 2)
                    if (parts.firstOrNull() == "tripId") parts.getOrNull(1) else null
                }
                .firstOrNull()
                ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                ?.takeIf(String::isNotBlank)
                ?: return null
            AndroidDeepLink.Invite(tripId)
        }
        uri.path?.startsWith("/mobile/reset") == true -> AndroidDeepLink.PasswordReset
        else -> null
    }
}

class MainActivity : ComponentActivity() {
    private var pendingTripId by mutableStateOf<String?>(null)
    private var pendingPasswordReset by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseProvider.clientForCurrentAuthFlow().handleDeeplinks(intent)
        readDeepLink(intent)
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        setContent {
            OdysseyApp(
                onThemeChanged = ::updateSystemBars,
                pendingTripId = pendingTripId,
                pendingPasswordReset = pendingPasswordReset,
                onPendingDeepLinkHandled = {
                    pendingTripId = null
                    pendingPasswordReset = false
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SupabaseProvider.clientForCurrentAuthFlow().handleDeeplinks(intent)
        readDeepLink(intent)
    }

    private fun readDeepLink(intent: Intent?) {
        when (val deepLink = parseAndroidDeepLink(intent?.dataString)) {
            is AndroidDeepLink.Invite -> pendingTripId = deepLink.tripId
            AndroidDeepLink.PasswordReset -> pendingPasswordReset = true
            null -> Unit
        }
    }

    private fun updateSystemBars(darkTheme: Boolean) {
        val barColor = Color.parseColor(if (darkTheme) "#141416" else "#F4F4F7")
        window.statusBarColor = barColor
        window.navigationBarColor = barColor
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }
}
