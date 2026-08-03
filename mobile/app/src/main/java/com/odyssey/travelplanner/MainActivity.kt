package com.odyssey.travelplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mapbox.common.MapboxOptions
import com.odyssey.travelplanner.ui.OdysseyApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        setContent { OdysseyApp() }
    }
}
