package com.odyssey.travelplanner.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.Locale

data class WeatherSnapshot(
    val temperature: String,
    val condition: String,
    val tripTemperature: String? = null,
    val tripCondition: String? = null,
    val tripIsEstimate: Boolean = false,
)

private val cityCoordinates = mapOf(
    "prague" to Pair(50.0755, 14.4378),
    "salzburg" to Pair(47.8095, 13.0550),
    "verona" to Pair(45.4384, 10.9916),
    "rome" to Pair(41.9028, 12.4964),
    "pisa" to Pair(43.7228, 10.4017),
    "figline valdarno" to Pair(43.6190, 11.4690),
    "figline-valdarno" to Pair(43.6190, 11.4690),
    "фильине-вальдарно" to Pair(43.6190, 11.4690),
    "фильине-валдарно" to Pair(43.6190, 11.4690),
    "san marino" to Pair(43.9424, 12.4578),
    "chioggia" to Pair(45.2181, 12.2786),
    "кьоджа" to Pair(45.2181, 12.2786),
    "ravensburg" to Pair(47.7810, 9.6110),
    "равенсбург" to Pair(47.7810, 9.6110),
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
private data class OpenMeteoArchiveResponse(
    val daily: OpenMeteoDaily? = null,
)

@Serializable
private data class OpenMeteoClimateResponse(
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
    val temperature_2m_mean: List<Double> = emptyList(),
    val temperature_2m_max: List<Double> = emptyList(),
    val weather_code: List<Int> = emptyList(),
    val precipitation_sum: List<Double> = emptyList(),
    val cloud_cover_mean: List<Double> = emptyList(),
)

private data class TripDayWeather(
    val temperature: String?,
    val condition: String?,
    val isEstimate: Boolean = false,
)

class WeatherRepository {
    private val http = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    suspend fun loadCurrent(cities: List<String>, tripDates: String = ""): Map<String, WeatherSnapshot> {
        val targetDate = tripDateFrom(tripDates)
        return supervisorScope {
            cities.mapNotNull { city ->
        val cityKey = city.substringBefore(",").trim().lowercase(Locale.ROOT)
                val coordinates = cityCoordinates[cityKey] ?: return@mapNotNull null
                async {
                    runCatching {
            val weather: OpenMeteoResponse = http.get(
                "https://api.open-meteo.com/v1/forecast?latitude=${coordinates.first}&longitude=${coordinates.second}&current=temperature_2m,weather_code&daily=temperature_2m_max,weather_code&forecast_days=16&timezone=auto",
            ).body()
            var tripWeather = targetDate?.let { tripWeatherAt(weather.daily, it) }
            if (tripWeather == null && targetDate?.isBefore(LocalDate.now()) == true) {
                tripWeather = loadArchivedTripWeather(coordinates, targetDate)
            }
            if (tripWeather == null && targetDate?.isAfter(LocalDate.now().plusDays(15)) == true) {
                tripWeather = loadClimateTripWeather(coordinates, targetDate)
            }
            city to WeatherSnapshot(
                temperature = "${weather.current.temperature_2m.toInt()}°C",
                condition = conditionFor(weather.current.weather_code),
                tripTemperature = tripWeather?.temperature,
                tripCondition = tripWeather?.condition,
                tripIsEstimate = tripWeather?.isEstimate == true,
            )
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull().toMap()
        }
    }

    private suspend fun loadArchivedTripWeather(
        coordinates: Pair<Double, Double>,
        targetDate: LocalDate,
    ): TripDayWeather? = runCatching {
        val archive: OpenMeteoArchiveResponse = http.get(
            "https://archive-api.open-meteo.com/v1/archive?latitude=${coordinates.first}&longitude=${coordinates.second}&start_date=$targetDate&end_date=$targetDate&daily=temperature_2m_max,weather_code&timezone=auto",
        ).body()
        tripWeatherAt(archive.daily, targetDate)
    }.getOrNull()

    private suspend fun loadClimateTripWeather(
        coordinates: Pair<Double, Double>,
        targetDate: LocalDate,
    ): TripDayWeather? = runCatching {
        val climate: OpenMeteoClimateResponse = http.get(
            "https://climate-api.open-meteo.com/v1/climate?latitude=${coordinates.first}&longitude=${coordinates.second}&start_date=$targetDate&end_date=$targetDate&models=EC_Earth3P_HR&daily=temperature_2m_mean,precipitation_sum,cloud_cover_mean&timezone=auto",
        ).body()
        climateWeatherAt(climate.daily, targetDate)
    }.getOrNull()
}

private fun tripDateFrom(value: String): LocalDate? {
    val iso = Regex("\\d{4}-\\d{2}-\\d{2}").find(value)?.value
    if (iso != null) return runCatching { LocalDate.parse(iso) }.getOrNull()

    val russianMonths = mapOf(
        "января" to 1, "январь" to 1,
        "февраля" to 2, "февраль" to 2,
        "марта" to 3, "март" to 3,
        "апреля" to 4, "апрель" to 4,
        "мая" to 5, "май" to 5,
        "июня" to 6, "июнь" to 6,
        "июля" to 7, "июль" to 7,
        "августа" to 8, "август" to 8,
        "сентября" to 9, "сентябрь" to 9,
        "октября" to 10, "октябрь" to 10,
        "ноября" to 11, "ноябрь" to 11,
        "декабря" to 12, "декабрь" to 12,
    )
    val match = Regex(
        "(\\d{1,2})\\s+(${russianMonths.keys.joinToString("|")})\\s+(\\d{4})",
        RegexOption.IGNORE_CASE,
    ).find(value) ?: return null
    val day = match.groupValues[1].toIntOrNull() ?: return null
    val month = russianMonths[match.groupValues[2].lowercase(Locale.ROOT)] ?: return null
    val year = match.groupValues[3].toIntOrNull() ?: return null
    return runCatching { LocalDate.of(year, month, day) }.getOrNull()
}

private fun tripWeatherAt(daily: OpenMeteoDaily?, targetDate: LocalDate): TripDayWeather? {
    val index = daily?.time?.indexOf(targetDate.toString())?.takeIf { it >= 0 } ?: return null
    val temperature = daily.temperature_2m_max.getOrNull(index)?.let { "${it.toInt()}°C" }
    val condition = daily.weather_code.getOrNull(index)?.let(::conditionFor)
    return if (temperature == null && condition == null) null else TripDayWeather(temperature, condition)
}

private fun climateWeatherAt(daily: OpenMeteoDaily?, targetDate: LocalDate): TripDayWeather? {
    val index = daily?.time?.indexOf(targetDate.toString())?.takeIf { it >= 0 } ?: return null
    val temperature = daily.temperature_2m_mean.getOrNull(index)?.let { "${it.toInt()}°C" }
    val precipitation = daily.precipitation_sum.getOrNull(index) ?: 0.0
    val cloudCover = daily.cloud_cover_mean.getOrNull(index) ?: 0.0
    val condition = when {
        precipitation >= 1.0 -> "Дождь"
        cloudCover >= 65.0 -> "Облачно"
        else -> "Ясно"
    }
    return temperature?.let { TripDayWeather(it, condition, isEstimate = true) }
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
