package com.odyssey.travelplanner.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.hours

private const val TRIP_PHOTO_BUCKET = "trip-photos"
private const val TRIP_PHOTO_REFERENCE_PREFIX = "storage://$TRIP_PHOTO_BUCKET/"
private const val SIGNED_URL_CACHE_TTL_MILLIS = 55 * 60 * 1000L
private val photoUrlResolveSemaphore = Semaphore(6)
private data class CachedTripPhotoUrl(val value: String, val expiresAtMillis: Long)
private val signedTripPhotoUrlCache = java.util.concurrent.ConcurrentHashMap<String, CachedTripPhotoUrl>()
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
    val now = System.currentTimeMillis()
    signedTripPhotoUrlCache[path]?.let { cached ->
        if (cached.expiresAtMillis > now) return cached.value
        signedTripPhotoUrlCache.remove(path, cached)
    }

    return photoUrlResolveSemaphore.withPermit {
        val refreshedNow = System.currentTimeMillis()
        signedTripPhotoUrlCache[path]?.let { cached ->
            if (cached.expiresAtMillis > refreshedNow) return@withPermit cached.value
            signedTripPhotoUrlCache.remove(path, cached)
        }

        val bucket = storage.from(TRIP_PHOTO_BUCKET)
        val signedUrl = runCatching { bucket.createSignedUrl(path, expiresIn = 1.hours) }.getOrNull()
        if (signedUrl != null) {
            signedTripPhotoUrlCache[path] = CachedTripPhotoUrl(
                value = signedUrl,
                expiresAtMillis = refreshedNow + SIGNED_URL_CACHE_TTL_MILLIS,
            )
        }
        signedUrl ?: bucket.publicUrl(path)
    }
}

internal suspend fun SupabaseClient.resolveTripPhotoReferences(references: List<String>): List<String> =
    supervisorScope {
        references.map { reference ->
            async(Dispatchers.IO) {
                runCatching { resolveTripPhotoReference(reference) ?: reference }.getOrDefault(reference)
            }
        }.awaitAll()
    }
