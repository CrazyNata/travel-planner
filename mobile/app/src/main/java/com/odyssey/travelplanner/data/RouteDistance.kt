package com.odyssey.travelplanner.data

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class RouteDistanceSummary(
    val distanceKm: Double,
    val isApproximate: Boolean,
)

internal data class RouteLegCoordinates(
    val from: CityLocation,
    val to: CityLocation,
)

private const val EarthRadiusKm = 6_371.0088

private fun resolveCityLocation(city: String, savedCoordinates: Map<String, CityLocation>): CityLocation? {
    savedCoordinates[city]?.let { return it }
    val catalogKey = cityCatalogEntry(city)?.key
    if (catalogKey != null) {
        savedCoordinates.entries.firstOrNull { (savedCity, _) ->
            cityCatalogEntry(savedCity)?.key == catalogKey
        }?.value?.let { return it }
        cityCatalogEntry(city)?.let { entry ->
            return CityLocation(entry.latitude, entry.longitude)
        }
    }
    return null
}

internal fun routeLegCoordinates(
    leg: RouteLeg,
    savedCoordinates: Map<String, CityLocation>,
): RouteLegCoordinates? {
    val from = resolveCityLocation(leg.from, savedCoordinates) ?: return null
    val to = resolveCityLocation(leg.to, savedCoordinates) ?: return null
    return RouteLegCoordinates(from = from, to = to)
}

internal fun straightLineDistanceKm(from: CityLocation, to: CityLocation): Double {
    val latitudeDelta = Math.toRadians(to.latitude - from.latitude)
    val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
    val fromLatitude = Math.toRadians(from.latitude)
    val toLatitude = Math.toRadians(to.latitude)
    val a = kotlin.math.sin(latitudeDelta / 2) * kotlin.math.sin(latitudeDelta / 2) +
        kotlin.math.cos(fromLatitude) * kotlin.math.cos(toLatitude) *
        kotlin.math.sin(longitudeDelta / 2) * kotlin.math.sin(longitudeDelta / 2)
    return EarthRadiusKm * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
}

internal fun straightLineRouteDistanceKm(
    routeLegs: List<RouteLeg>,
    savedCoordinates: Map<String, CityLocation>,
): Double? {
    if (routeLegs.isEmpty()) return null
    val coordinates = routeLegs.map { routeLegCoordinates(it, savedCoordinates) }
    if (coordinates.any { it == null }) return null
    return coordinates.filterNotNull().sumOf { straightLineDistanceKm(it.from, it.to) }
}

internal suspend fun loadRouteDistanceSummary(
    routeLegs: List<RouteLeg>,
    savedCoordinates: Map<String, CityLocation>,
    mapboxAccessToken: String,
): RouteDistanceSummary? {
    if (routeLegs.isEmpty()) return null
    val coordinates = routeLegs.map { routeLegCoordinates(it, savedCoordinates) }
    if (coordinates.any { it == null }) return null
    val resolvedCoordinates = coordinates.filterNotNull()
    val fallbackDistanceKm = resolvedCoordinates.sumOf { straightLineDistanceKm(it.from, it.to) }
    val token = mapboxAccessToken.trim()
    if (token.isBlank()) {
        return RouteDistanceSummary(fallbackDistanceKm, isApproximate = true)
    }

    val drivingDistances = supervisorScope {
        resolvedCoordinates.map { leg ->
            async {
                mapboxDrivingDistanceMeters(leg.from, leg.to, token)
            }
        }.awaitAll()
    }
    return if (drivingDistances.all { it != null }) {
        RouteDistanceSummary(
            distanceKm = drivingDistances.filterNotNull().sum() / 1000.0,
            isApproximate = false,
        )
    } else {
        RouteDistanceSummary(fallbackDistanceKm, isApproximate = true)
    }
}

private suspend fun mapboxDrivingDistanceMeters(
    from: CityLocation,
    to: CityLocation,
    accessToken: String,
): Double? = withContext(Dispatchers.IO) {
    val encodedToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8.toString())
    val endpoint = "https://api.mapbox.com/directions/v5/mapbox/driving/" +
        "${from.longitude},${from.latitude};${to.longitude},${to.latitude}" +
        "?overview=false&access_token=$encodedToken"
    val connection = runCatching { URL(endpoint).openConnection() as HttpURLConnection }.getOrNull()
        ?: return@withContext null
    try {
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        if (connection.responseCode !in 200..299) return@withContext null
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        runCatching {
            Json.parseToJsonElement(body)
                .jsonObject["routes"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("distance")
                ?.jsonPrimitive
                ?.doubleOrNull
                ?.takeIf { it.isFinite() && it >= 0.0 }
        }.getOrNull()
    } catch (_: Exception) {
        null
    } finally {
        connection.disconnect()
    }
}
