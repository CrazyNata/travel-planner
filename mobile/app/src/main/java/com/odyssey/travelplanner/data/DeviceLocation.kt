package com.odyssey.travelplanner.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** The device's current foreground location. It is transient and is never persisted. */
data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
)

fun distanceMeters(
    origin: DeviceLocation,
    latitude: Double?,
    longitude: Double?,
): Double? {
    if (latitude == null || longitude == null) return null
    if (!origin.latitude.isFinite() || !origin.longitude.isFinite() || !latitude.isFinite() || !longitude.isFinite()) return null

    val earthRadiusMeters = 6_371_000.0
    val latitudeDelta = Math.toRadians(latitude - origin.latitude)
    val longitudeDelta = Math.toRadians(longitude - origin.longitude)
    val originLatitude = Math.toRadians(origin.latitude)
    val targetLatitude = Math.toRadians(latitude)
    val haversine = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(originLatitude) * cos(targetLatitude) *
        sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
    return earthRadiusMeters * 2 * atan2(sqrt(haversine.coerceIn(0.0, 1.0)), sqrt((1 - haversine).coerceIn(0.0, 1.0)))
}
