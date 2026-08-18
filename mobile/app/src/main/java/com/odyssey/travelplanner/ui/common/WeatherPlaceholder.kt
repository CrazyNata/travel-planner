package com.odyssey.travelplanner.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.odyssey.travelplanner.data.WeatherSnapshot
import com.odyssey.travelplanner.data.CoverPhoto
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.i18n.localizedWeatherCondition
import com.odyssey.travelplanner.ui.theme.Manrope

@Composable
internal fun WeatherPlaceholder(
    city: String,
    photo: com.odyssey.travelplanner.data.CoverPhoto?,
    weather: WeatherSnapshot?,
    tripDatesWeather: Boolean,
) {
    val temperature = weather?.temperature?.removeSuffix("°C")?.toIntOrNull()
    val displayedTemperature = if (tripDatesWeather) weather?.tripTemperature else weather?.temperature
    val displayedCondition = if (tripDatesWeather) {
        weather?.tripCondition?.let { localizedWeatherCondition(it) }
            ?: localized("Прогноз пока недоступен", "Forecast unavailable", "Pronóstico no disponible", "Vorhersage nicht verfügbar")
    } else {
        weather?.condition?.let { localizedWeatherCondition(it) }
    }
    Box(
        modifier = Modifier.width(120.dp).height(150.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF6C5CE7)),
    ) {
        if (photo != null) {
            AsyncImage(
                model = photo.imageUrl,
                contentDescription = city,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xA6000000)))))
        Text(
                    text = localizedCityName(city),
            color = Color.White,
            fontFamily = Manrope,
            fontWeight = FontWeight.W700,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            val temperatureText = if (tripDatesWeather && weather?.tripIsEstimate == true) {
                displayedTemperature?.let { "≈ $it" } ?: "…"
            } else {
                displayedTemperature ?: "…"
            }
            Text(temperatureText, color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 26.sp)
            Text(displayedCondition.orEmpty(), color = Color(0xDDFFFFFF), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.sp)
        }
    }
}

