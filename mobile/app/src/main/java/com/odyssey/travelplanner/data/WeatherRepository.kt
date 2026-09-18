package com.odyssey.travelplanner.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.net.URLEncoder
import java.util.Locale

data class WeatherSnapshot(
    val temperature: String,
    val condition: String,
    val tripTemperature: String? = null,
    val tripCondition: String? = null,
    val tripIsEstimate: Boolean = false,
    val tripDays: Map<String, WeatherDaySnapshot> = emptyMap(),
)

data class WeatherDaySnapshot(
    val temperature: String? = null,
    val condition: String? = null,
    val isEstimate: Boolean = false,
)

@Serializable
private data class OpenMeteoResponse(
    val current: OpenMeteoCurrent? = null,
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
private data class OpenMeteoGeocodingResponse(
    val results: List<OpenMeteoGeocodingResult>? = null,
)

@Serializable
private data class OpenMeteoGeocodingResult(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
private data class OpenMeteoCurrent(
    val temperature_2m: Double? = null,
    val weather_code: Int? = null,
)

@Serializable
private data class OpenMeteoDaily(
    val time: List<String> = emptyList(),
    val temperature_2m_mean: List<Double?> = emptyList(),
    val temperature_2m_max: List<Double?> = emptyList(),
    val weather_code: List<Int?> = emptyList(),
    val precipitation_sum: List<Double?> = emptyList(),
    val cloud_cover_mean: List<Double?> = emptyList(),
)

private data class TripDayWeather(
    val temperature: String?,
    val condition: String?,
    val isEstimate: Boolean = false,
)

private const val OPTIONAL_TRIP_WEATHER_TIMEOUT_MILLIS = 4_000L

class WeatherRepository {
    private val http = HttpClient(OkHttp) {
        expectSuccess = true
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 15_000
            socketTimeoutMillis = 15_000
        }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    fun close() {
        http.close()
    }

    suspend fun loadCurrent(
        cities: List<String>,
        tripDates: String = "",
        cityCoordinates: Map<String, CityLocation> = emptyMap(),
    ): Map<String, WeatherSnapshot> {
        val tripDateRange = tripDateRangeFrom(tripDates)
        val targetDate = tripDateRange?.first
        val tripDatesToLoad = tripDateRange?.let(::datesBetween) ?: emptyList()
        return supervisorScope {
            cities
                .filter(String::isNotBlank)
                .distinctBy { it.trim().lowercase(Locale.ROOT) }
                .map { city ->
                async {
                    val coordinates = cityCoordinates[city]?.let { it.latitude to it.longitude }
                        ?: cityCatalogEntry(city)?.let { it.latitude to it.longitude }
                        ?: resolveCoordinates(city)
                        ?: return@async null
                    val weather = requestOrNull<OpenMeteoResponse> {
                        http.get(
                            "https://api.open-meteo.com/v1/forecast?latitude=${coordinates.first}&longitude=${coordinates.second}&current=temperature_2m,weather_code&daily=temperature_2m_max,weather_code&forecast_days=16&timezone=auto",
                        ).body<OpenMeteoResponse>()
                    } ?: return@async null
                    val current = weather.current ?: return@async null
                    val currentTemperature = current.temperature_2m ?: return@async null
                    val currentWeatherCode = current.weather_code ?: return@async null
                    val tripDays = weather.daily.toTripDays().toMutableMap()
                    val missingTripDates = tripDatesToLoad.filterNot { date ->
                        tripDays.containsKey(date.toString())
                    }
                    val today = LocalDate.now()
                    val archivedDates = missingTripDates.filter { it.isBefore(today) }
                    if (archivedDates.isNotEmpty()) {
                        withTimeoutOrNull(OPTIONAL_TRIP_WEATHER_TIMEOUT_MILLIS) {
                            loadArchivedTripWeather(coordinates, archivedDates.first(), archivedDates.last())
                        }.orEmpty().forEach { (date, tripWeather) ->
                            tripDays[date] = WeatherDaySnapshot(
                                temperature = tripWeather.temperature,
                                condition = tripWeather.condition,
                                isEstimate = tripWeather.isEstimate,
                            )
                        }
                    }
                    val climateDates = missingTripDates.filter { it.isAfter(today.plusDays(15)) }
                    if (climateDates.isNotEmpty()) {
                        withTimeoutOrNull(OPTIONAL_TRIP_WEATHER_TIMEOUT_MILLIS) {
                            loadClimateTripWeather(coordinates, climateDates.first(), climateDates.last())
                        }.orEmpty().forEach { (date, tripWeather) ->
                            tripDays[date] = WeatherDaySnapshot(
                                temperature = tripWeather.temperature,
                                condition = tripWeather.condition,
                                isEstimate = tripWeather.isEstimate,
                            )
                        }
                    }
                    val firstTripDay = targetDate?.let { tripDays[it.toString()] }
                    city to WeatherSnapshot(
                        temperature = "${currentTemperature.toInt()}°C",
                        condition = conditionFor(currentWeatherCode),
                        tripTemperature = firstTripDay?.temperature,
                        tripCondition = firstTripDay?.condition,
                        tripIsEstimate = firstTripDay?.isEstimate == true,
                        tripDays = tripDays,
                    )
                }
            }.awaitAll().filterNotNull().toMap()
        }
    }

    private suspend fun resolveCoordinates(city: String): Pair<Double, Double>? {
        val query = city.trim()
            .replace(" — ", ", ")
            .takeIf { it.length >= 2 }
            ?: return null
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val response = requestOrNull<OpenMeteoGeocodingResponse> {
            http.get(
                "https://geocoding-api.open-meteo.com/v1/search?name=$encodedQuery&count=1&language=en&format=json",
            ).body<OpenMeteoGeocodingResponse>()
        } ?: return null
        return response.results?.firstOrNull()?.let { it.latitude to it.longitude }
    }

    private suspend fun loadArchivedTripWeather(
        coordinates: Pair<Double, Double>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Map<String, TripDayWeather> {
        val archive = requestOrNull<OpenMeteoArchiveResponse> {
            http.get(
                "https://archive-api.open-meteo.com/v1/archive?latitude=${coordinates.first}&longitude=${coordinates.second}&start_date=$startDate&end_date=$endDate&daily=temperature_2m_max,weather_code&timezone=auto",
            ).body<OpenMeteoArchiveResponse>()
        } ?: return emptyMap()
        return archive.daily.toTripWeatherDays()
    }

    private suspend fun loadClimateTripWeather(
        coordinates: Pair<Double, Double>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Map<String, TripDayWeather> {
        val climate = requestOrNull<OpenMeteoClimateResponse> {
            http.get(
                "https://climate-api.open-meteo.com/v1/climate?latitude=${coordinates.first}&longitude=${coordinates.second}&start_date=$startDate&end_date=$endDate&models=EC_Earth3P_HR&daily=temperature_2m_mean,precipitation_sum,cloud_cover_mean&timezone=auto",
            ).body<OpenMeteoClimateResponse>()
        } ?: return emptyMap()
        return climate.daily.toClimateTripWeatherDays()
    }

    private suspend fun <T> requestOrNull(request: suspend () -> T): T? = try {
        request()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        null
    }
}

internal fun tripDateRangeFrom(value: String): Pair<LocalDate, LocalDate>? {
    val dotted = Regex("\\d{1,2}\\.\\d{1,2}\\.\\d{4}").find(value)?.value
    if (dotted != null) {
        val dates = Regex("\\d{1,2}\\.\\d{1,2}\\.\\d{4}").findAll(value).mapNotNull { match ->
            val parts = match.value.split('.')
            runCatching { LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt()) }.getOrNull()
        }.toList()
        val start = dates.firstOrNull() ?: return null
        return start to dates.getOrElse(1) { start }
    }

    val isoDates = Regex("\\d{4}-\\d{2}-\\d{2}").findAll(value).mapNotNull { match ->
        runCatching { LocalDate.parse(match.value) }.getOrNull()
    }.toList()
    if (isoDates.isNotEmpty()) {
        val start = isoDates.first()
        return start to isoDates.getOrElse(1) { start }
    }

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
    val compactHumanRange = Regex(
        "(\\d{1,2})\\s*[–—-]\\s*(\\d{1,2})\\s+(${russianMonths.keys.joinToString("|")})\\s+(\\d{4})",
        RegexOption.IGNORE_CASE,
    ).find(value)
    if (compactHumanRange != null) {
        val startDay = compactHumanRange.groupValues[1].toIntOrNull() ?: return null
        val endDay = compactHumanRange.groupValues[2].toIntOrNull() ?: return null
        val month = russianMonths[compactHumanRange.groupValues[3].lowercase(Locale.ROOT)] ?: return null
        val year = compactHumanRange.groupValues[4].toIntOrNull() ?: return null
        val start = runCatching { LocalDate.of(year, month, startDay) }.getOrNull() ?: return null
        val end = runCatching { LocalDate.of(year, month, endDay) }.getOrNull() ?: return null
        return start to end
    }
    val dates = Regex(
        "(\\d{1,2})\\s+(${russianMonths.keys.joinToString("|")})\\s+(\\d{4})",
        RegexOption.IGNORE_CASE,
    ).findAll(value).mapNotNull { match ->
        val day = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
        val month = russianMonths[match.groupValues[2].lowercase(Locale.ROOT)] ?: return@mapNotNull null
        val year = match.groupValues[3].toIntOrNull() ?: return@mapNotNull null
        runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }.toList()
    val start = dates.firstOrNull() ?: return null
    return start to dates.getOrElse(1) { start }
}

private fun datesBetween(range: Pair<LocalDate, LocalDate>, maxDays: Int = 366): List<LocalDate> {
    val dayCount = ChronoUnit.DAYS.between(range.first, range.second)
    if (dayCount < 0) return emptyList()
    val lastOffset = dayCount.coerceAtMost((maxDays - 1).coerceAtLeast(0).toLong())
    return (0..lastOffset).map { offset -> range.first.plusDays(offset) }
}

private fun OpenMeteoDaily?.toTripDays(): Map<String, WeatherDaySnapshot> {
    val daily = this ?: return emptyMap()
    return daily.time.mapIndexedNotNull { index, date ->
        val temperature = daily.temperature_2m_max.getOrNull(index)?.let { "${it.toInt()}°C" }
        val condition = daily.weather_code.getOrNull(index)?.let(::conditionFor)
        if (temperature == null && condition == null) {
            null
        } else {
            date to WeatherDaySnapshot(temperature = temperature, condition = condition)
        }
    }.toMap()
}

private fun OpenMeteoDaily?.toTripWeatherDays(): Map<String, TripDayWeather> {
    val daily = this ?: return emptyMap()
    return daily.time.mapIndexedNotNull { index, date ->
        val temperature = daily.temperature_2m_max.getOrNull(index)?.let { "${it.toInt()}°C" }
        val condition = daily.weather_code.getOrNull(index)?.let(::conditionFor)
        if (temperature == null && condition == null) {
            null
        } else {
            date to TripDayWeather(temperature, condition)
        }
    }.toMap()
}

private fun OpenMeteoDaily?.toClimateTripWeatherDays(): Map<String, TripDayWeather> {
    val daily = this ?: return emptyMap()
    return daily.time.mapIndexedNotNull { index, date ->
        val temperature = daily.temperature_2m_mean.getOrNull(index)?.let { "${it.toInt()}°C" }
        val precipitation = daily.precipitation_sum.getOrNull(index) ?: 0.0
        val cloudCover = daily.cloud_cover_mean.getOrNull(index) ?: 0.0
        val condition = when {
            precipitation >= 1.0 -> "Дождь"
            cloudCover >= 65.0 -> "Облачно"
            else -> "Ясно"
        }
        if (temperature == null && condition.isBlank()) {
            null
        } else {
            date to TripDayWeather(temperature, condition, isEstimate = true)
        }
    }.toMap()
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
