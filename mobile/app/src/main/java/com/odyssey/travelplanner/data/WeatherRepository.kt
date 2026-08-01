package com.odyssey.travelplanner.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class WeatherSnapshot(
    val temperature: String,
    val condition: String,
    val tripTemperature: String? = null,
    val tripCondition: String? = null,
)

private val cityCoordinates = mapOf(
    "prague" to Pair(50.0755, 14.4378),
    "salzburg" to Pair(47.8095, 13.0550),
    "verona" to Pair(45.4384, 10.9916),
    "rome" to Pair(41.9028, 12.4964),
    "pisa" to Pair(43.7228, 10.4017),
    "figline valdarno" to Pair(43.6190, 11.4690),
    "san marino" to Pair(43.9424, 12.4578),
    "chioggia" to Pair(45.2181, 12.2786),
    "milan" to Pair(45.4642, 9.1900),
    "valdidentro" to Pair(46.4890, 10.2940),
    "munich" to Pair(48.1351, 11.5820),
    "vienna" to Pair(48.2082, 16.3738),
    "вена" to Pair(48.2082, 16.3738),
    "innsbruck" to Pair(47.2692, 11.4041),
    "инсбрук" to Pair(47.2692, 11.4041),
    "florence" to Pair(43.7696, 11.2558),
    "флоренция" to Pair(43.7696, 11.2558),
    "venice" to Pair(45.4408, 12.3155),
    "венеция" to Pair(45.4408, 12.3155),
    "tallinn" to Pair(59.4370, 24.7536),
    "таллин" to Pair(59.4370, 24.7536),
    "riga" to Pair(56.9496, 24.1052),
    "рига" to Pair(56.9496, 24.1052),
    "vilnius" to Pair(54.6872, 25.2797),
    "вильнюс" to Pair(54.6872, 25.2797),
    "мюнхен" to Pair(48.1351, 11.5820),
    "прага" to Pair(50.0755, 14.4378),
    "рим" to Pair(41.9028, 12.4964),
    "пиза" to Pair(43.7228, 10.4017),
    "верона" to Pair(45.4384, 10.9916),
    "милан" to Pair(45.4642, 9.1900),
)

@Serializable
private data class OpenMeteoResponse(
    val current: OpenMeteoCurrent,
    val daily: OpenMeteoDaily? = null,
)

@Serializable
private data class OpenMeteoCurrent(
    val temperature_2m: Double,
    val weather_code: Int,
)

@Serializable
private data class OpenMeteoDaily(
    val time: List<String> = emptyList(),
    val temperature_2m_max: List<Double> = emptyList(),
    val weather_code: List<Int> = emptyList(),
)

class WeatherRepository {
    private val http = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    suspend fun loadCurrent(cities: List<String>, tripDates: String = ""): Map<String, WeatherSnapshot> {
        val targetDate = Regex("\\d{4}-\\d{2}-\\d{2}").find(tripDates)?.value
        return cities.mapNotNull { city ->
        val coordinates = cityCoordinates[city.trim().lowercase()] ?: return@mapNotNull null
        runCatching {
            val weather: OpenMeteoResponse = http.get(
                "https://api.open-meteo.com/v1/forecast?latitude=${coordinates.first}&longitude=${coordinates.second}&current=temperature_2m,weather_code&daily=temperature_2m_max,weather_code&forecast_days=16&timezone=auto",
            ).body()
            val dateIndex = targetDate?.let { weather.daily?.time?.indexOf(it) ?: -1 }?.takeIf { it >= 0 }
            city to WeatherSnapshot(
                temperature = "${weather.current.temperature_2m.toInt()}°C",
                condition = conditionFor(weather.current.weather_code),
                tripTemperature = dateIndex?.let { index -> weather.daily?.temperature_2m_max?.getOrNull(index)?.let { "${it.toInt()}°C" } },
                tripCondition = dateIndex?.let { index -> weather.daily?.weather_code?.getOrNull(index)?.let(::conditionFor) },
            )
        }.getOrNull()
        }.toMap()
    }
}

private fun conditionFor(code: Int): String = when (code) {
    0 -> "Ясно"
    1, 2, 3 -> "Облачно"
    45, 48 -> "Туман"
    51, 53, 55, 56, 57 -> "Морось"
    61, 63, 65, 66, 67, 80, 81, 82 -> "Дождь"
    71, 73, 75, 77, 85, 86 -> "Снег"
    95, 96, 99 -> "Гроза"
    else -> "—"
}
