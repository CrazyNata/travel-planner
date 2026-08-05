package com.odyssey.travelplanner

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.mapbox.common.MapboxOptions
import com.odyssey.travelplanner.ui.OdysseyApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        setContent { OdysseyApp(onThemeChanged = ::updateSystemBars) }
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
