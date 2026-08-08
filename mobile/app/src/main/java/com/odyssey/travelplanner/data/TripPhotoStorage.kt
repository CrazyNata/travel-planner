package com.odyssey.travelplanner.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.hours

private const val TRIP_PHOTO_BUCKET = "trip-photos"
private const val TRIP_PHOTO_REFERENCE_PREFIX = "storage://$TRIP_PHOTO_BUCKET/"
private val storageUrlMarkers = listOf(
    "/storage/v1/object/public/$TRIP_PHOTO_BUCKET/",
    "/storage/v1/object/sign/$TRIP_PHOTO_BUCKET/",
    "/storage/v1/object/authenticated/$TRIP_PHOTO_BUCKET/",
)

internal fun storedTripPhotoReference(path: String): String =
    "$TRIP_PHOTO_REFERENCE_PREFIX${path.trimStart('/')}"

internal fun tripPhotoPath(reference: String?): String? {
    val value = reference?.trim().orEmpty()
    if (value.isBlank()) return null
    if (value.startsWith(TRIP_PHOTO_REFERENCE_PREFIX)) {
        return value.removePrefix(TRIP_PHOTO_REFERENCE_PREFIX).takeIf(String::isNotBlank)
    }
    val marker = storageUrlMarkers.firstOrNull(value::contains) ?: return null
    val encodedPath = value.substringAfter(marker).substringBefore('?').substringBefore('#')
    if (encodedPath.isBlank()) return null
    return runCatching {
        URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.name())
    }.getOrDefault(encodedPath)
}

internal fun canonicalTripPhotoReference(reference: String?): String? =
    reference?.let { value -> tripPhotoPath(value)?.let(::storedTripPhotoReference) ?: value }

internal suspend fun SupabaseClient.resolveTripPhotoReference(reference: String?): String? {
    val value = reference?.takeIf(String::isNotBlank) ?: return null
    val path = tripPhotoPath(value) ?: return value
    val bucket = storage.from(TRIP_PHOTO_BUCKET)
    return runCatching { bucket.createSignedUrl(path, expiresIn = 1.hours) }
        .getOrElse { bucket.publicUrl(path) }
}
