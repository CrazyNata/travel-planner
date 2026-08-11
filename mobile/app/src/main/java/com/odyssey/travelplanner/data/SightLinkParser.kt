package com.odyssey.travelplanner.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder

data class SightLinkCoordinates(
    val longitude: Double,
    val latitude: Double,
)

private const val NUMBER = "-?(?:\\d+(?:\\.\\d+)?|\\.\\d+)"
private val googleAtPattern = Regex("""@($NUMBER),($NUMBER)""")
private val googleDataPattern = Regex("""!3d($NUMBER)!4d($NUMBER)""")
private val coordinatePairPattern = Regex("""(?<![\d.-])($NUMBER)\s*[,;]\s*($NUMBER)(?![\d.-])""")

/**
 * Extracts a map point from common Google Maps, Yandex Maps and direct
 * coordinate links. Coordinates are returned in Mapbox order: longitude,
 * latitude.
 */
fun parseSightLinkCoordinates(rawLink: String): SightLinkCoordinates? {
    val raw = rawLink.trim()
    if (raw.isBlank()) return null

    val decoded = decodeUrl(raw)
    googleAtPattern.find(decoded)?.let { match ->
        return latLon(match.groupValues[1], match.groupValues[2])
    }
    googleDataPattern.find(decoded)?.let { match ->
        return latLon(match.groupValues[1], match.groupValues[2])
    }

    val query = runCatching { URI(raw).rawQuery.orEmpty() }
        .getOrDefault("")
        .split('&')
        .mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            decodeUrl(part.substring(0, separator)).lowercase() to decodeUrl(part.substring(separator + 1))
        }
        .toMap()

    listOf("query", "q", "center", "coords", "coordinates")
        .firstNotNullOfOrNull { key -> query[key]?.let(::parseLatitudeLongitude) }
        ?.let { return it }
    query["ll"]?.let { parseLongitudeLatitude(it)?.let { point -> return point } }
    query["map"]?.let { parseMapParameter(it)?.let { point -> return point } }

    coordinatePairPattern.find(decoded)?.let { match ->
        return latLon(match.groupValues[1], match.groupValues[2])
    }
    return null
}

/**
 * Resolves short links such as maps.app.goo.gl before applying the same
 * coordinate parser. Direct links are handled without a network request.
 */
suspend fun resolveSightLinkCoordinates(rawLink: String): SightLinkCoordinates? {
    parseSightLinkCoordinates(rawLink)?.let { return it }
    val raw = rawLink.trim()
    if (!raw.startsWith("https://", ignoreCase = true) && !raw.startsWith("http://", ignoreCase = true)) return null

    return withContext(Dispatchers.IO) {
        val connection = runCatching { URL(raw).openConnection() as? HttpURLConnection }.getOrNull() ?: return@withContext null
        try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 6_000
            connection.readTimeout = 6_000
            connection.requestMethod = "GET"
            connection.connect()
            parseSightLinkCoordinates(connection.url.toString())
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}

private fun parseLatitudeLongitude(rawValue: String): SightLinkCoordinates? =
    coordinatePairPattern.find(decodeUrl(rawValue))?.let { match ->
        latLon(match.groupValues[1], match.groupValues[2])
    }

private fun parseLongitudeLatitude(rawValue: String): SightLinkCoordinates? =
    coordinatePairPattern.find(decodeUrl(rawValue))?.let { match ->
        latLon(match.groupValues[2], match.groupValues[1])
    }

private fun parseMapParameter(rawValue: String): SightLinkCoordinates? {
    val parts = decodeUrl(rawValue).split('/')
    if (parts.size < 3) return null
    return latLon(parts[1], parts[2])
}

private fun latLon(latitudeValue: String, longitudeValue: String): SightLinkCoordinates? {
    val latitude = latitudeValue.toDoubleOrNull() ?: return null
    val longitude = longitudeValue.toDoubleOrNull() ?: return null
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    return SightLinkCoordinates(longitude = longitude, latitude = latitude)
}

private fun decodeUrl(value: String): String =
    runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrDefault(value)
