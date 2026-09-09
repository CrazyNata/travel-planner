package com.odyssey.travelplanner.data

import android.net.Uri
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
import kotlin.math.abs

internal data class RouteDistanceSummary(
    val distanceKm: Double,
    val isApproximate: Boolean,
)

internal data class RouteLegCoordinates(
    val from: CityLocation,
    val to: CityLocation,
)

private const val EarthRadiusKm = 6_371.0088

/**
 * Ramingo route-distance rule shared by every Android route screen:
 * preserve ordered map stops and travel mode, ask a routing provider for the
 * network distance, and mark the straight-line fallback as approximate.
 */
private object RouteDistancePolicy {
    const val endpointToleranceKm = 75.0
    const val maxCoordinatesPerRoute = 25
    const val requestTimeoutMs = 8_000
}

private data class RouteDistancePath(
    val coordinates: List<CityLocation>,
    val profile: String,
)

internal data class GoogleRouteCoordinates(
    val coordinates: List<CityLocation>,
    val fromQuery: Boolean,
    val origin: CityLocation?,
    val waypoints: List<CityLocation>,
    val destination: CityLocation?,
)

private val googleCoordinatePattern = Regex(
    "^\\s*(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)\\s*$",
)

private fun googleCoordinate(value: String): CityLocation? {
    val match = googleCoordinatePattern.matchEntire(value) ?: return null
    val latitude = match.groupValues[1].toDoubleOrNull() ?: return null
    val longitude = match.groupValues[2].toDoubleOrNull() ?: return null
    if (!latitude.isFinite() || !longitude.isFinite() || abs(latitude) > 90.0 || abs(longitude) > 180.0) return null
    return CityLocation(latitude = latitude, longitude = longitude)
}

private fun uniqueConsecutiveCoordinates(coordinates: List<CityLocation>): List<CityLocation> =
    coordinates.filterIndexed { index, coordinate ->
        val previous = coordinates.getOrNull(index - 1)
        previous == null || previous.latitude != coordinate.latitude || previous.longitude != coordinate.longitude
    }

internal fun googleRouteCoordinates(mapsUrl: String): GoogleRouteCoordinates {
    val emptyResult = GoogleRouteCoordinates(
        coordinates = emptyList(),
        fromQuery = false,
        origin = null,
        waypoints = emptyList(),
        destination = null,
    )
    if (mapsUrl.isBlank()) return emptyResult
    val uri = runCatching { Uri.parse(mapsUrl) }.getOrNull() ?: return emptyResult
    val origin = uri.getQueryParameter("origin")
    val waypoints = uri.getQueryParameter("waypoints")
    val destination = uri.getQueryParameter("destination")
    val hasQuery = origin != null || waypoints != null || destination != null
    val originCoordinate = origin?.let(::googleCoordinate)
    val waypointCoordinates = waypoints
        ?.split('|', ';')
        .orEmpty()
        .mapNotNull(::googleCoordinate)
    val destinationCoordinate = destination?.let(::googleCoordinate)
    val queryCoordinates = buildList {
        originCoordinate?.let(::add)
        addAll(waypointCoordinates)
        destinationCoordinate?.let(::add)
    }
    if (hasQuery) {
        return GoogleRouteCoordinates(
            coordinates = uniqueConsecutiveCoordinates(queryCoordinates),
            fromQuery = true,
            origin = originCoordinate,
            waypoints = waypointCoordinates,
            destination = destinationCoordinate,
        )
    }

    val directionPath = uri.path
        ?.substringAfter("/dir/", "")
        ?.substringBefore("/@", "")
        .orEmpty()
    val coordinates = uniqueConsecutiveCoordinates(
        directionPath.split('/').mapNotNull(::googleCoordinate),
    )
    return GoogleRouteCoordinates(
        coordinates = coordinates,
        fromQuery = false,
        origin = coordinates.firstOrNull(),
        waypoints = coordinates.drop(1).dropLast(1),
        destination = coordinates.lastOrNull().takeIf { coordinates.size >= 2 },
    )
}

internal fun googleTravelProfile(mapsUrl: String): String {
    val travelMode = runCatching { Uri.parse(mapsUrl).getQueryParameter("travelmode") }
        .getOrNull()
        ?.lowercase()
    return when (travelMode) {
        "walking" -> "walking"
        "bicycling" -> "cycling"
        else -> "driving"
    }
}

private suspend fun resolveGoogleRedirect(mapsUrl: String): String = withContext(Dispatchers.IO) {
    if (!mapsUrl.contains("maps.app.goo.gl") && !mapsUrl.contains("goo.gl/maps")) return@withContext mapsUrl
    val connection = runCatching { URL(mapsUrl).openConnection() as HttpURLConnection }.getOrNull()
        ?: return@withContext mapsUrl
    try {
        connection.connectTimeout = RouteDistancePolicy.requestTimeoutMs
        connection.readTimeout = RouteDistancePolicy.requestTimeoutMs
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = true
        connection.useCaches = false
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")
        connection.setRequestProperty("User-Agent", "RamingoTravelPlanner/0.1 (Android)")
        connection.responseCode
        connection.url?.toString()?.takeIf { it.isNotBlank() } ?: mapsUrl
    } catch (_: Exception) {
        mapsUrl
    } finally {
        connection.disconnect()
    }
}

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

private suspend fun routeDistancePath(
    leg: RouteLeg,
    savedCoordinates: Map<String, CityLocation>,
): RouteDistancePath? {
    val from = resolveCityLocation(leg.from, savedCoordinates) ?: return null
    val to = resolveCityLocation(leg.to, savedCoordinates) ?: return null
    val resolvedUrl = resolveGoogleRedirect(leg.mapsUrl)
    val parsedMapRoute = googleRouteCoordinates(resolvedUrl)
    fun endpointMatchesCity(endpoint: CityLocation?, city: CityLocation): Boolean =
        endpoint != null &&
            straightLineDistanceKm(endpoint, city) <= RouteDistancePolicy.endpointToleranceKm
    val coordinates = when {
        parsedMapRoute.fromQuery && parsedMapRoute.coordinates.isNotEmpty() -> uniqueConsecutiveCoordinates(
            buildList {
                add(if (endpointMatchesCity(parsedMapRoute.origin, from)) parsedMapRoute.origin!! else from)
                addAll(parsedMapRoute.waypoints)
                add(if (endpointMatchesCity(parsedMapRoute.destination, to)) parsedMapRoute.destination!! else to)
            },
        )
        parsedMapRoute.coordinates.size >= 2 -> parsedMapRoute.coordinates.mapIndexed { index, coordinate ->
            when {
                index == 0 && !endpointMatchesCity(coordinate, from) -> from
                index == parsedMapRoute.coordinates.lastIndex && !endpointMatchesCity(coordinate, to) -> to
                else -> coordinate
            }
        }
        parsedMapRoute.coordinates.size == 1 -> uniqueConsecutiveCoordinates(listOf(from, parsedMapRoute.coordinates.first(), to))
        else -> listOf(from, to)
    }
    return coordinates.takeIf { it.size >= 2 }?.let {
        RouteDistancePath(coordinates = it, profile = googleTravelProfile(resolvedUrl))
    }
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
    val paths = supervisorScope {
        routeLegs.map { leg ->
            async { routeDistancePath(leg, savedCoordinates) }
        }.awaitAll()
    }
    if (paths.any { it == null }) return null
    val resolvedPaths = paths.filterNotNull()
    val token = mapboxAccessToken.trim()

    // Keep the public routing service within a predictable request rate. A
    // temporary burst failure must not turn the whole trip into a bad total.
    val routeDistances = resolvedPaths.map { path ->
        routeDistanceMeters(path.coordinates, path.profile, token)
    }
    // Never mix straight-line segments into the displayed trip total. The
    // Android screen shows a distance only after every leg has a network
    // route; this keeps a temporary provider outage from producing a wrong
    // number that looks authoritative.
    if (routeDistances.any { it == null }) return null
    val distanceKm = routeDistances.filterNotNull().sumOf { it } / 1000.0
    return RouteDistanceSummary(
        distanceKm = distanceKm,
        isApproximate = false,
    )
}

private suspend fun routeDistanceMeters(
    coordinates: List<CityLocation>,
    profile: String,
    mapboxAccessToken: String,
): Double? = valhallaRouteDistanceMeters(coordinates, profile)
    ?: mapboxRouteDistanceMeters(coordinates, profile, mapboxAccessToken)
    ?: openStreetMapRouteDistanceMeters(coordinates, profile)

private fun valhallaCosting(profile: String): String = when (profile) {
    "walking" -> "pedestrian"
    "cycling" -> "bicycle"
    else -> "auto"
}

private suspend fun valhallaRouteDistanceMeters(
    coordinates: List<CityLocation>,
    profile: String,
): Double? = withContext(Dispatchers.IO) {
    if (coordinates.size > RouteDistancePolicy.maxCoordinatesPerRoute) return@withContext null
    val locations = coordinates.joinToString(",", prefix = "[", postfix = "]") {
        "{\"lat\":${it.latitude},\"lon\":${it.longitude}}"
    }
    val body = "{\"locations\":$locations,\"costing\":\"${valhallaCosting(profile)}\",\"units\":\"kilometers\"}"
    val endpoints = listOf(
        "https://ramingo.online/routing/valhalla/route",
        "https://valhalla1.openstreetmap.de/route",
    )
    for (endpoint in endpoints) {
        val connection = runCatching {
            URL(endpoint).openConnection() as HttpURLConnection
        }.getOrNull() ?: continue
        val result = try {
            connection.connectTimeout = RouteDistancePolicy.requestTimeoutMs
            connection.readTimeout = RouteDistancePolicy.requestTimeoutMs
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "text/plain")
            connection.setRequestProperty("User-Agent", "RamingoTravelPlanner/0.1 (Android)")
            connection.outputStream.bufferedWriter().use { it.write(body) }
            if (connection.responseCode !in 200..299) null
            else connection.inputStream.bufferedReader().use { parseValhallaRouteDistance(it.readText()) }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
        if (result != null) return@withContext result
    }
    null
}

private fun openStreetMapRouter(profile: String): String = when (profile) {
    "walking" -> "routed-foot"
    "cycling" -> "routed-bike"
    else -> "routed-car"
}

private suspend fun openStreetMapRouteDistanceMeters(
    coordinates: List<CityLocation>,
    profile: String,
): Double? = withContext(Dispatchers.IO) {
    if (coordinates.size > RouteDistancePolicy.maxCoordinatesPerRoute) return@withContext null
    val path = coordinates.joinToString(";") { "${it.longitude},${it.latitude}" }
    val endpoint = "https://routing.openstreetmap.de/${openStreetMapRouter(profile)}/route/v1/driving/" +
        path +
        "?overview=false"
    val connection = runCatching { URL(endpoint).openConnection() as HttpURLConnection }.getOrNull()
        ?: return@withContext null
    try {
        connection.connectTimeout = RouteDistancePolicy.requestTimeoutMs
        connection.readTimeout = RouteDistancePolicy.requestTimeoutMs
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "RamingoTravelPlanner/0.1 (Android)")
        if (connection.responseCode !in 200..299) return@withContext null
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        parseRouteDistance(body)
    } catch (_: Exception) {
        null
    } finally {
        connection.disconnect()
    }
}

private suspend fun mapboxRouteDistanceMeters(
    coordinates: List<CityLocation>,
    profile: String,
    accessToken: String,
): Double? = withContext(Dispatchers.IO) {
    if (coordinates.size > RouteDistancePolicy.maxCoordinatesPerRoute || accessToken.isBlank()) return@withContext null
    val encodedToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8.toString())
    val path = coordinates.joinToString(";") { "${it.longitude},${it.latitude}" }
    val endpoint = "https://api.mapbox.com/directions/v5/mapbox/$profile/" +
        path +
        "?overview=false&access_token=$encodedToken"
    val connection = runCatching { URL(endpoint).openConnection() as HttpURLConnection }.getOrNull()
        ?: return@withContext null
    try {
        connection.connectTimeout = RouteDistancePolicy.requestTimeoutMs
        connection.readTimeout = RouteDistancePolicy.requestTimeoutMs
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        if (connection.responseCode !in 200..299) return@withContext null
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        parseRouteDistance(body)
    } catch (_: Exception) {
        null
    } finally {
        connection.disconnect()
    }
}

private fun parseRouteDistance(body: String): Double? = runCatching {
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

private fun parseValhallaRouteDistance(body: String): Double? = runCatching {
    Json.parseToJsonElement(body)
        .jsonObject["trip"]
        ?.jsonObject
        ?.get("summary")
        ?.jsonObject
        ?.get("length")
        ?.jsonPrimitive
        ?.doubleOrNull
        ?.takeIf { it.isFinite() && it >= 0.0 }
        ?.times(1000.0)
}.getOrNull()
