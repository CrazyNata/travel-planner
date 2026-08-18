package com.odyssey.travelplanner.ui.domain

import android.net.Uri
import androidx.compose.runtime.getValue
import com.mapbox.geojson.Point
import androidx.compose.runtime.setValue
import java.util.Locale
import com.odyssey.travelplanner.data.CityLocation
import com.odyssey.travelplanner.data.cityCatalogEntry

internal fun cityFilterKey(city: String): String {
    val point = mapCoordinate(city)
    return if (point != null) {
        "point:${String.format(Locale.US, "%.4f:%.4f", point.longitude(), point.latitude())}"
    } else {
        city.substringBefore(",").substringBefore(" — ").trim().lowercase(Locale.ROOT)
    }
}

internal fun mapCoordinate(city: String, cityCoordinates: Map<String, CityLocation> = emptyMap()): Point? {
    cityCoordinates[city]?.let { return Point.fromLngLat(it.longitude, it.latitude) }
    val cityPart = city.substringBefore(" — ").trim()
    cityCatalogEntry(cityPart)?.let { entry ->
        return Point.fromLngLat(entry.longitude, entry.latitude)
    }
    return null
}

internal fun restaurantLinkUri(value: String): String? {
    val raw = value.trim()
    if (raw.isBlank()) return null
    return if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
        raw
    } else {
        "https://www.google.com/maps/search/?api=1&query=${Uri.encode(raw)}"
    }
}

internal fun formatSightCoordinate(point: Point): String =
    String.format(Locale.US, "%.5f, %.5f", point.latitude(), point.longitude())


