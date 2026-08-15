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
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class SightCatalogEntry(
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
    val descriptionRu: String,
    val descriptionEn: String,
    val descriptionEs: String,
    val descriptionDe: String,
    val category: String,
    val latitude: Double?,
    val longitude: Double?,
    val mapUrl: String,
    val searchText: String,
    val sortOrder: Int,
    val photoUrl: String? = null,
    val photoName: String = "",
    val photoAttribution: String? = null,
    val rating: Double? = null,
    val ratingCount: Int? = null,
    val ratingPlaceUrl: String = "",
) {
    fun name(language: String): String = when (language.trim().uppercase(Locale.ROOT).substringBefore('-')) {
        "EN" -> nameEn.ifBlank { nameRu }
        "ES" -> nameEs.ifBlank { nameRu.ifBlank { nameEn } }
        "DE" -> nameDe.ifBlank { nameRu.ifBlank { nameEn } }
        else -> nameRu.ifBlank { nameEn }
    }

    fun description(language: String): String = when (language.trim().uppercase(Locale.ROOT).substringBefore('-')) {
        "EN" -> descriptionEn.ifBlank { descriptionRu }
        "ES" -> descriptionEs.ifBlank { descriptionEn.ifBlank { descriptionRu } }
        "DE" -> descriptionDe.ifBlank { descriptionEn.ifBlank { descriptionRu } }
        else -> descriptionRu.ifBlank { descriptionEn }
    }

    fun allNames(): List<String> = listOf(nameRu, nameEn, nameEs, nameDe) + searchText.split('|')
}

@Serializable
private data class SightCatalogRow(
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
    @SerialName("description_ru") val descriptionRu: String = "",
    @SerialName("description_en") val descriptionEn: String = "",
    @SerialName("description_es") val descriptionEs: String = "",
    @SerialName("description_de") val descriptionDe: String = "",
    val category: String = "достопримечательности",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("map_url") val mapUrl: String = "",
    @SerialName("search_text") val searchText: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("photo_name") val photoName: String = "",
    @SerialName("photo_attribution") val photoAttribution: String? = null,
    val rating: Double? = null,
    @SerialName("rating_count") val ratingCount: Int? = null,
    @SerialName("rating_place_url") val ratingPlaceUrl: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
private data class SightEnrichmentResponse(
    val sights: List<SightEnrichmentPlace> = emptyList(),
)

@Serializable
private data class SightEnrichmentPlace(
    @SerialName("place_id") val placeId: String = "",
    val name: String = "",
    val address: String = "",
    val category: String = "",
    val rating: Double? = null,
    @SerialName("rating_count") val ratingCount: Int? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("photo_name") val photoName: String = "",
    @SerialName("photo_attribution") val photoAttribution: String? = null,
    @SerialName("google_maps_url") val googleMapsUrl: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
private data class SightPhotosResponse(
    val photos: List<SightPhotoResponseItem> = emptyList(),
)

@Serializable
private data class SightPhotoResponseItem(
    @SerialName("photo_name") val photoName: String = "",
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("photo_attribution") val photoAttribution: String? = null,
)

data class SightPhotoResult(
    val photoUrl: String?,
    val photoAttribution: String?,
)

data class SightCatalogSearchResult(
    val entries: List<SightCatalogEntry>,
    val liveRatingsAvailable: Boolean,
)

private fun SightCatalogRow.toEntry() = SightCatalogEntry(
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
    descriptionRu = descriptionRu,
    descriptionEn = descriptionEn,
    descriptionEs = descriptionEs,
    descriptionDe = descriptionDe,
    category = category,
    latitude = latitude,
    longitude = longitude,
    mapUrl = mapUrl,
    searchText = searchText,
    sortOrder = sortOrder,
    photoUrl = photoUrl,
    photoName = photoName,
    photoAttribution = photoAttribution,
    rating = rating,
    ratingCount = ratingCount,
    ratingPlaceUrl = ratingPlaceUrl,
)

class SightCatalogRepository(private val client: SupabaseClient) {
    private val photoCache = ConcurrentHashMap<String, SightPhotoResult>()

    suspend fun search(city: String, query: String, limit: Int = 200): List<SightCatalogEntry> {
        val cityName = catalogCityName(city)
        if (cityName.isBlank()) return emptyList()

        val rows = client.from("sight_catalog").select {
            filter {
                or {
                    eq("city_name_ru", cityName)
                    eq("city_name_en", cityName)
                    eq("city_name_es", cityName)
                    eq("city_name_de", cityName)
                }
            }
        }.decodeList<SightCatalogRow>()

        val normalizedQuery = normalizeCatalogText(query)
        return rows
            .map(SightCatalogRow::toEntry)
            .filter { entry ->
                normalizedQuery.isBlank() || entry.allNames().any { name ->
                    normalizeCatalogText(name).startsWith(normalizedQuery) || normalizeCatalogText(name).contains(normalizedQuery)
                }
            }
            .sortedWith(
                compareBy<SightCatalogEntry> { entry ->
                    if (normalizedQuery.isBlank()) 0 else if (normalizeCatalogText(entry.nameEn).startsWith(normalizedQuery) || normalizeCatalogText(entry.nameRu).startsWith(normalizedQuery)) 0 else 1
                }.thenBy { it.sortOrder }.thenBy { normalizeCatalogText(it.nameEn.ifBlank { it.nameRu }) },
            )
            .take(limit)
    }

    /**
     * Keeps the curated catalog as the stable fallback, while enriching the
     * cards shown to the user with live Google ratings and photo references.
     * Live values stay in memory and are never written to the catalog table.
     */
    suspend fun searchWithLiveRatings(
        city: String,
        query: String,
        language: String,
        limit: Int = 60,
    ): SightCatalogSearchResult {
        val fallback = search(city = city, query = query, limit = limit)
        val liveEntries = runCatching {
            searchLiveRatings(city = city, query = query, language = language, limit = limit)
        }.getOrNull().orEmpty()
        if (liveEntries.isEmpty()) return SightCatalogSearchResult(fallback, false)

        val matchedFallbackIds = mutableSetOf<String>()
        val seenLiveIds = mutableSetOf<String>()
        val merged = liveEntries.mapIndexedNotNull { index, live ->
            if (!seenLiveIds.add(live.id)) return@mapIndexedNotNull null
            val match = fallback
                .map { entry -> entry to sightMatchScore(entry, live) }
                .filter { (entry, score) -> score > 0 && entry.id !in matchedFallbackIds }
                .maxByOrNull { (_, score) -> score }
                ?.first
            if (match != null) {
                matchedFallbackIds += match.id
                match.copy(
                    photoUrl = live.photoUrl ?: match.photoUrl,
                    photoName = live.photoName,
                    photoAttribution = live.photoAttribution,
                    rating = live.rating ?: match.rating,
                    ratingCount = live.ratingCount ?: match.ratingCount,
                    ratingPlaceUrl = live.ratingPlaceUrl,
                    mapUrl = live.mapUrl.ifBlank { match.mapUrl },
                    latitude = live.latitude ?: match.latitude,
                    longitude = live.longitude ?: match.longitude,
                    sortOrder = index,
                )
            } else {
                live.copy(sortOrder = index)
            }
        }
        return SightCatalogSearchResult(merged, true)
    }

    /** Fetches complete live attraction cards without persisting Google data. */
    suspend fun searchLiveRatings(
        city: String,
        query: String,
        language: String,
        limit: Int = 60,
    ): List<SightCatalogEntry> {
        val cityName = catalogCityName(city)
        if (cityName.isBlank()) return emptyList()

        if (client.auth.currentSessionOrNull() == null) {
            runCatching { client.auth.refreshCurrentSession() }
        }
        val accessToken = client.auth.currentAccessTokenOrNull()
        val response = client.functions.invoke(
            function = "restaurant-enrichment",
            body = buildJsonObject {
                put("category", "sight")
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
        val places = Json { ignoreUnknownKeys = true }
            .decodeFromString<SightEnrichmentResponse>(response.bodyAsText())
            .sights

        return places.mapIndexedNotNull { index, place ->
            val name = place.name.trim()
            val rating = place.rating
            val hasPhotoReference = place.photoUrl?.isNotBlank() == true || place.photoName.isNotBlank()
            if (name.isBlank() || rating == null || !hasPhotoReference) return@mapIndexedNotNull null
            val category = place.category.ifBlank { "достопримечательность" }
            SightCatalogEntry(
                id = "google:sight:${place.placeId.ifBlank { "$cityName:$index:$name" }}",
                cityKey = city,
                cityNameRu = cityName,
                cityNameEn = cityName,
                cityNameEs = cityName,
                cityNameDe = cityName,
                nameRu = name,
                nameEn = name,
                nameEs = name,
                nameDe = name,
                descriptionRu = "Популярная достопримечательность города.",
                descriptionEn = "Popular attraction in $cityName.",
                descriptionEs = "Lugar popular de la ciudad.",
                descriptionDe = "Beliebte Sehenswürdigkeit der Stadt.",
                category = category,
                latitude = place.latitude,
                longitude = place.longitude,
                mapUrl = place.googleMapsUrl,
                searchText = listOf(name, place.address, category).filter(String::isNotBlank).joinToString(" "),
                sortOrder = index,
                photoUrl = place.photoUrl,
                photoName = place.photoName,
                photoAttribution = place.photoAttribution,
                rating = rating,
                ratingCount = place.ratingCount,
                ratingPlaceUrl = place.googleMapsUrl,
            )
        }
    }

    suspend fun resolveSightPhoto(photoName: String): SightPhotoResult? =
        resolveSightPhotos(listOf(photoName))[photoName.trim()]

    /** Resolves the visible attraction photo batch in one Edge Function call. */
    suspend fun resolveSightPhotos(photoNames: List<String>): Map<String, SightPhotoResult> {
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
            .decodeFromString<SightPhotosResponse>(response.bodyAsText())
        val resolved = payload.photos
            .filter { it.photoName.isNotBlank() }
            .associate { item ->
                item.photoName to SightPhotoResult(item.photoUrl, item.photoAttribution)
            }
        resolved.forEach { (name, photo) -> photoCache[name] = photo }
        return cached + resolved
    }
}

private fun sightMatchScore(entry: SightCatalogEntry, live: SightCatalogEntry): Int {
    val liveName = normalizeCatalogText(live.nameEn.ifBlank { live.nameRu })
    if (liveName.isBlank()) return 0
    val staticNames = entry.allNames().map(::normalizeCatalogText).filter(String::isNotBlank)
    if (staticNames.any { it == liveName }) return 100
    if (staticNames.any { it.contains(liveName) || liveName.contains(it) }) return 70
    val coordinatesMatch = entry.latitude != null && entry.longitude != null &&
        live.latitude != null && live.longitude != null &&
        kotlin.math.abs(entry.latitude - live.latitude) < 0.01 &&
        kotlin.math.abs(entry.longitude - live.longitude) < 0.01
    return if (coordinatesMatch) 40 else 0
}

private fun placesLanguageCode(language: String): String = when (
    language.trim().uppercase(Locale.ROOT).substringBefore('-')
) {
    "EN" -> "en"
    "ES" -> "es"
    "DE" -> "de"
    else -> "ru"
}

fun catalogCityName(value: String): String = value
    .substringBefore(",")
    .substringBefore(" — ")
    .trim()

fun normalizeCatalogText(value: String): String = value
    .trim()
    .lowercase(Locale.ROOT)
    .replace('ё', 'е')
    .replace(Regex("\\s+"), " ")
