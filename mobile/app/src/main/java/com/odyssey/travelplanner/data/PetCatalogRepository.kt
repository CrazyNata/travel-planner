package com.odyssey.travelplanner.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class PetCatalogEntry(
    val id: String,
    val googlePlaceId: String,
    val name: String,
    val city: String,
    val type: String,
    val category: String = "",
    val address: String = "",
    val phone: String = "",
    val website: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mapsUrl: String = "",
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val photoUrl: String? = null,
    val photoName: String = "",
    val photoNames: List<String> = emptyList(),
    val photoAttribution: String? = null,
    val isLiveResult: Boolean = true,
    val openNow: Boolean? = null,
)

@Serializable
private data class PetCatalogResponse(val petPlaces: List<PetCatalogResponsePlace> = emptyList())

@Serializable
private data class PetCatalogResponsePlace(
    @SerialName("place_id") val placeId: String = "",
    val name: String = "",
    val address: String = "",
    val category: String = "",
    val type: String = "",
    val phone: String = "",
    val website: String = "",
    val rating: Double? = null,
    @SerialName("rating_count") val ratingCount: Int? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("photo_name") val photoName: String = "",
    @SerialName("photo_names") val photoNames: List<String> = emptyList(),
    @SerialName("photo_attribution") val photoAttribution: String? = null,
    @SerialName("google_maps_url") val googleMapsUrl: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("open_now") val openNow: Boolean? = null,
)

@Serializable
private data class PetPhotoResponseItem(
    @SerialName("photo_name") val photoName: String = "",
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("photo_attribution") val photoAttribution: String? = null,
)

@Serializable
private data class PetPhotosResponse(val photos: List<PetPhotoResponseItem> = emptyList())

data class PetPhotoResult(val photoUrl: String?, val photoAttribution: String?)

class PetCatalogRepository(private val client: SupabaseClient) {
    private data class SearchCacheEntry(
        val createdAtMillis: Long,
        val entries: List<PetCatalogEntry>,
    )

    private companion object {
        const val SEARCH_CACHE_TTL_MILLIS = 5 * 60 * 1000L
    }

    private val photoCache = ConcurrentHashMap<String, PetPhotoResult>()
    private val searchCache = ConcurrentHashMap<String, SearchCacheEntry>()
    private val authMutex = Mutex()

    suspend fun search(
        city: String,
        type: String,
        query: String,
        language: String,
        limit: Int = 40,
    ): List<PetCatalogEntry> {
        val cityName = city.trim()
        if (cityName.isBlank()) return emptyList()
        val normalizedType = if (type.trim().lowercase(Locale.ROOT) == "vet") "vet" else "shop"
        val normalizedQuery = query.trim().take(80)
        val languageCode = placesLanguageCodeForPets(language)
        val normalizedLimit = limit.coerceIn(1, 60)
        val cacheKey = listOf(
            cityName.lowercase(Locale.ROOT),
            normalizedType,
            normalizedQuery.lowercase(Locale.ROOT),
            languageCode,
            normalizedLimit,
        ).joinToString("|")
        val now = System.currentTimeMillis()
        searchCache[cacheKey]?.takeIf { now - it.createdAtMillis < SEARCH_CACHE_TTL_MILLIS }?.let { cached ->
            return cached.entries
        }
        val response = client.functions.invoke(
            function = "restaurant-enrichment",
            body = buildJsonObject {
                put("category", "pet")
                put("petType", normalizedType)
                put("city", cityName.take(100))
                put("query", normalizedQuery)
                put("languageCode", languageCode)
                put("limit", normalizedLimit)
            },
            headers = authHeaders(),
        )
        val places = Json { ignoreUnknownKeys = true }
            .decodeFromString<PetCatalogResponse>(response.bodyAsText())
            .petPlaces
        val entries = places.mapIndexed { index, place ->
            val googleId = place.placeId.trim()
            PetCatalogEntry(
                id = "google:${googleId.ifBlank { "$cityName:$normalizedType:$index" }}",
                googlePlaceId = googleId,
                name = place.name.ifBlank { if (normalizedType == "vet") "Ветеринарная клиника" else "Зоомагазин" },
                city = cityName,
                type = normalizedType,
                category = place.category.ifBlank { place.type },
                address = place.address,
                phone = place.phone,
                website = place.website,
                latitude = place.latitude,
                longitude = place.longitude,
                mapsUrl = place.googleMapsUrl,
                rating = place.rating,
                reviewCount = place.ratingCount,
                photoUrl = place.photoUrl,
                photoName = place.photoName,
                photoNames = place.photoNames.ifEmpty { listOfNotNull(place.photoName.takeIf(String::isNotBlank)) },
                photoAttribution = place.photoAttribution,
                openNow = place.openNow,
            )
        }
        searchCache[cacheKey] = SearchCacheEntry(System.currentTimeMillis(), entries)
        return entries
    }

    suspend fun resolvePhotos(photoNames: List<String>): Map<String, PetPhotoResult> {
        val clean = photoNames.map(String::trim).filter { it.isNotBlank() }.distinct()
        if (clean.isEmpty()) return emptyMap()
        val cached = clean.mapNotNull { photoCache[it]?.let { result -> it to result } }.toMap()
        val missing = clean.filterNot(cached::containsKey)
        if (missing.isEmpty()) return cached
        val response = client.functions.invoke(
            function = "restaurant-enrichment",
            body = buildJsonObject {
                put("photoNames", buildJsonArray { missing.take(24).forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
            },
            headers = authHeaders(),
        )
        val resolved = Json { ignoreUnknownKeys = true }
            .decodeFromString<PetPhotosResponse>(response.bodyAsText())
            .photos.filter { it.photoName.isNotBlank() }
            .associate { it.photoName to PetPhotoResult(it.photoUrl, it.photoAttribution) }
        // Keep successful resolutions in the cache. A null URL can be a
        // transient Places media failure (especially when several cities are
        // loaded at once), so it must remain retryable for the visible card.
        resolved.forEach { (key, value) ->
            if (!value.photoUrl.isNullOrBlank()) photoCache[key] = value
        }
        return cached + resolved
    }

    private suspend fun authHeaders(): Headers = authMutex.withLock {
        if (client.auth.currentSessionOrNull() == null) {
            runCatching { client.auth.refreshCurrentSession() }
        }
        val accessToken = client.auth.currentAccessTokenOrNull()
        Headers.build {
            append(HttpHeaders.ContentType, "application/json")
            if (!accessToken.isNullOrBlank()) append(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }
}

private fun placesLanguageCodeForPets(language: String): String = when (
    language.trim().uppercase(Locale.ROOT).substringBefore('-')
) {
    "EN" -> "en"
    "ES" -> "es"
    "DE" -> "de"
    else -> "ru"
}
