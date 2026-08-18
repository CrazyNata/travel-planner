package com.odyssey.travelplanner.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class RestaurantCatalogEntry(
    val id: String,
    val cityKey: String,
    val cityNameRu: String,
    val cityNameEn: String,
    val cityNameEs: String,
    val cityNameDe: String,
    val nameRu: String,
    val nameEn: String,
    val nameEs: String,
    val nameDe: String,
    val cuisine: String,
    val address: String,
    val website: String,
    val phone: String,
    val latitude: Double?,
    val longitude: Double?,
    val mapUrl: String,
    val searchText: String,
    val sortOrder: Int,
    /** Live Google Places data. These values are never persisted in Supabase. */
    val rating: Double? = null,
    val ratingCount: Int? = null,
    val photoUrl: String? = null,
    /** Google Places photo resource name, resolved only when its card is visible. */
    val photoName: String = "",
    val photoAttribution: String? = null,
    val ratingPlaceUrl: String = "",
    val isLiveResult: Boolean = false,
    /** Google Places price level, from 1 (inexpensive) to 4 (very expensive). */
    val priceLevel: Int? = null,
) {
    fun name(language: String): String = when (language.trim().uppercase(Locale.ROOT).substringBefore('-')) {
        "EN" -> nameEn.ifBlank { nameRu }
        "ES" -> nameEs.ifBlank { nameEn.ifBlank { nameRu } }
        "DE" -> nameDe.ifBlank { nameEn.ifBlank { nameRu } }
        else -> nameRu.ifBlank { nameEn }
    }

    fun cityName(language: String): String = when (language.trim().uppercase(Locale.ROOT).substringBefore('-')) {
        "EN" -> cityNameEn.ifBlank { cityNameRu }
        "ES" -> cityNameEs.ifBlank { cityNameEn.ifBlank { cityNameRu } }
        "DE" -> cityNameDe.ifBlank { cityNameEn.ifBlank { cityNameRu } }
        else -> cityNameRu.ifBlank { cityNameEn }
    }

    fun allSearchableText(): List<String> = listOf(
        nameRu,
        nameEn,
        nameEs,
        nameDe,
        cuisine,
        address,
        website,
        searchText,
    )
}

@Serializable
private data class RestaurantCatalogRow(
    val id: String,
    @SerialName("city_key") val cityKey: String,
    @SerialName("city_name_ru") val cityNameRu: String,
    @SerialName("city_name_en") val cityNameEn: String,
    @SerialName("city_name_es") val cityNameEs: String = "",
    @SerialName("city_name_de") val cityNameDe: String = "",
    @SerialName("name_ru") val nameRu: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("name_es") val nameEs: String = "",
    @SerialName("name_de") val nameDe: String = "",
    val cuisine: String = "",
    val address: String = "",
    val website: String = "",
    val phone: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("map_url") val mapUrl: String = "",
    @SerialName("search_text") val searchText: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
private data class RestaurantEnrichmentResponse(
    val restaurants: List<RestaurantEnrichmentPlace> = emptyList(),
)

@Serializable
private data class RestaurantEnrichmentPlace(
    @SerialName("place_id") val placeId: String = "",
    val name: String = "",
    val address: String = "",
    val cuisine: String = "",
    val rating: Double? = null,
    @SerialName("rating_count") val ratingCount: Int? = null,
    @SerialName("price_level") val priceLevel: Int? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("photo_name") val photoName: String = "",
    @SerialName("photo_attribution") val photoAttribution: String? = null,
    @SerialName("google_maps_url") val googleMapsUrl: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
private data class RestaurantPhotoResponse(
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("photo_attribution") val photoAttribution: String? = null,
)

@Serializable
private data class RestaurantPhotosResponse(
    val photos: List<RestaurantPhotoResponseItem> = emptyList(),
)

@Serializable
private data class RestaurantPhotoResponseItem(
    @SerialName("photo_name") val photoName: String = "",
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("photo_attribution") val photoAttribution: String? = null,
)

data class RestaurantPhotoResult(
    val photoUrl: String?,
    val photoAttribution: String?,
)

data class RestaurantCatalogSearchResult(
    val entries: List<RestaurantCatalogEntry>,
    val liveRatingsAvailable: Boolean,
)

private fun RestaurantCatalogRow.toEntry() = RestaurantCatalogEntry(
    id = id,
    cityKey = cityKey,
    cityNameRu = cityNameRu,
    cityNameEn = cityNameEn,
    cityNameEs = cityNameEs,
    cityNameDe = cityNameDe,
    nameRu = nameRu,
    nameEn = nameEn,
    nameEs = nameEs,
    nameDe = nameDe,
    cuisine = cuisine,
    address = address,
    website = website,
    phone = phone,
    latitude = latitude,
    longitude = longitude,
    mapUrl = mapUrl,
    searchText = searchText,
    sortOrder = sortOrder,
)

class RestaurantCatalogRepository(private val client: SupabaseClient) {
    // Process-wide: the UI builds a fresh repository per call site, so a
    // per-instance cache would start empty every time and never hit.
    private val photoCache get() = sharedRestaurantPhotoCache

    suspend fun search(city: String, query: String, limit: Int = 120): List<RestaurantCatalogEntry> {
        val cityName = catalogCityName(city)
        if (cityName.isBlank()) return emptyList()

        val rows = client.from("restaurant_catalog").select {
            filter {
                or {
                    eq("city_name_ru", cityName)
                    eq("city_name_en", cityName)
                    eq("city_name_es", cityName)
                    eq("city_name_de", cityName)
                }
            }
        }.decodeList<RestaurantCatalogRow>()

        val normalizedQuery = normalizeCatalogText(query)
        return rows
            .map(RestaurantCatalogRow::toEntry)
            .filter { entry ->
                normalizedQuery.isBlank() || entry.allSearchableText().any { value ->
                    val normalizedValue = normalizeCatalogText(value)
                    normalizedValue.startsWith(normalizedQuery) || normalizedValue.contains(normalizedQuery)
                }
            }
            .sortedWith(
                compareBy<RestaurantCatalogEntry> { entry ->
                    if (normalizedQuery.isBlank()) {
                        0
                    } else if (normalizeCatalogText(entry.nameEn).startsWith(normalizedQuery) || normalizeCatalogText(entry.nameRu).startsWith(normalizedQuery)) {
                        0
                    } else {
                        1
                    }
                }.thenBy { it.sortOrder }
                    .thenBy { normalizeCatalogText(it.nameEn.ifBlank { it.nameRu }) },
            )
            .take(limit)
    }

    /**
     * Loads the stable OSM catalog first, then tries to replace the visible list with
     * live Google Places results containing ratings and photos. Google data is kept in
     * memory only because photo references and ratings must not be cached in our DB.
     */
    suspend fun searchWithLiveRatings(
        city: String,
        query: String,
        language: String,
        limit: Int = 60,
    ): RestaurantCatalogSearchResult {
        val fallback = search(city = city, query = query, limit = limit)
        val liveEntries = runCatching {
            searchLiveRatings(city = city, query = query, language = language, limit = limit)
        }.getOrNull().orEmpty()
        return if (liveEntries.isEmpty()) {
            RestaurantCatalogSearchResult(fallback, false)
        } else {
            RestaurantCatalogSearchResult(liveEntries, true)
        }
    }

    /** Fetches live ratings/photos without touching the persistent catalog. */
    suspend fun searchLiveRatings(
        city: String,
        query: String,
        language: String,
        limit: Int = 60,
    ): List<RestaurantCatalogEntry> {
        val cityName = catalogCityName(city)
        if (cityName.isBlank()) return emptyList()

        // Edge Functions require a user JWT. PostgREST can still return the
        // public catalog with only the publishable key, so do not assume that
        // a successful catalog request means the session is ready here.
        if (client.auth.currentSessionOrNull() == null) {
            runCatching { client.auth.refreshCurrentSession() }
        }
        val accessToken = client.auth.currentAccessTokenOrNull()

        val response = client.functions.invoke(
            function = "restaurant-enrichment",
            body = buildJsonObject {
                put("city", cityName)
                put("query", query.trim().take(80))
                put("languageCode", placesLanguageCode(language))
                put("limit", limit.coerceIn(1, 60))
            },
            headers = Headers.build {
                append(HttpHeaders.ContentType, "application/json")
                if (!accessToken.isNullOrBlank()) {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            },
        )
        val livePlaces = Json { ignoreUnknownKeys = true }
            .decodeFromString<RestaurantEnrichmentResponse>(response.bodyAsText())
            .restaurants

        return livePlaces.mapIndexedNotNull { index, place ->
            val stableName = place.name.ifBlank { "Restaurant ${index + 1}" }
            val hasPhotoReference = place.photoUrl?.isNotBlank() == true || place.photoName.isNotBlank()
            if (place.rating == null || !hasPhotoReference) return@mapIndexedNotNull null
            RestaurantCatalogEntry(
                id = "google:${place.placeId.ifBlank { "$cityName:$index:$stableName" }}",
                cityKey = city,
                cityNameRu = cityName,
                cityNameEn = cityName,
                cityNameEs = cityName,
                cityNameDe = cityName,
                nameRu = stableName,
                nameEn = stableName,
                nameEs = stableName,
                nameDe = stableName,
                cuisine = place.cuisine,
                address = place.address,
                website = "",
                phone = "",
                latitude = place.latitude,
                longitude = place.longitude,
                mapUrl = place.googleMapsUrl,
                searchText = listOf(stableName, place.address, place.cuisine).joinToString(" "),
                sortOrder = index,
                rating = place.rating,
                ratingCount = place.ratingCount,
                priceLevel = place.priceLevel,
                photoUrl = place.photoUrl,
                photoName = place.photoName,
                photoAttribution = place.photoAttribution,
                ratingPlaceUrl = place.googleMapsUrl,
                isLiveResult = true,
            )
        }
    }

    /** Resolves one Google photo only when its catalog card is visible. */
    suspend fun resolveRestaurantPhoto(photoName: String): RestaurantPhotoResult? {
        val cleanPhotoName = photoName.trim()
        if (cleanPhotoName.isBlank()) return null

        return resolveRestaurantPhotos(listOf(cleanPhotoName))[cleanPhotoName]
    }

    /** Resolves the visible batch in one Edge Function request. */
    suspend fun resolveRestaurantPhotos(photoNames: List<String>): Map<String, RestaurantPhotoResult> {
        val cleanPhotoNames = photoNames
            .map(String::trim)
            .filter { it.isNotBlank() }
            .distinct()
        if (cleanPhotoNames.isEmpty()) return emptyMap()

        val cached = cleanPhotoNames.mapNotNull { name -> photoCache[name]?.let { name to it } }.toMap()
        val missing = cleanPhotoNames.filterNot { it in cached }
        if (missing.isEmpty()) return cached

        if (client.auth.currentSessionOrNull() == null) {
            runCatching { client.auth.refreshCurrentSession() }
        }
        val accessToken = client.auth.currentAccessTokenOrNull()
        val response = client.functions.invoke(
            function = "restaurant-enrichment",
            body = buildJsonObject {
                put("photoNames", kotlinx.serialization.json.buildJsonArray {
                    missing.take(24).forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                })
            },
            headers = Headers.build {
                append(HttpHeaders.ContentType, "application/json")
                if (!accessToken.isNullOrBlank()) {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            },
        )
        val payload = Json { ignoreUnknownKeys = true }
            .decodeFromString<RestaurantPhotosResponse>(response.bodyAsText())
        val resolved = payload.photos
            .filter { it.photoName.isNotBlank() }
            .associate { item ->
                item.photoName to RestaurantPhotoResult(item.photoUrl, item.photoAttribution)
            }
        resolved.forEach { (name, photo) -> photoCache[name] = photo }
        return cached + resolved
    }
}

private val sharedRestaurantPhotoCache = ConcurrentHashMap<String, RestaurantPhotoResult>()

private fun placesLanguageCode(language: String): String = when (
    language.trim().uppercase(Locale.ROOT).substringBefore('-')
) {
    "EN" -> "en"
    "ES" -> "es"
    "DE" -> "de"
    else -> "ru"
}
