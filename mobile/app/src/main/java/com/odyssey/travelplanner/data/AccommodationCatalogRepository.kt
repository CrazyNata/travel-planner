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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale

data class AccommodationCatalogEntry(
    val id: String,
    val placeId: String,
    val name: String,
    val city: String,
    val address: String,
    val type: String,
    val rating: Double?,
    val reviewCount: Int?,
    val priceLevel: Int?,
    val photoUrl: String?,
    val photoName: String,
    val photoNames: List<String> = emptyList(),
    val photoAttribution: String?,
    val googleMapsUrl: String,
    val website: String,
    val phone: String,
    val latitude: Double?,
    val longitude: Double?,
    val source: String = "google",
)

data class AccommodationPhotoResult(
    val photoUrl: String?,
    val photoAttribution: String?,
)

@Serializable
private data class AccommodationEnrichmentResponse(
    val accommodations: List<AccommodationEnrichmentPlace> = emptyList(),
)

@Serializable
private data class AccommodationEnrichmentPlace(
    @SerialName("place_id") val placeId: String = "",
    val name: String = "",
    val address: String = "",
    val type: String = "",
    val category: String = "",
    val rating: Double? = null,
    @SerialName("rating_count") val reviewCount: Int? = null,
    @SerialName("price_level") val priceLevel: Int? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("photo_name") val photoName: String = "",
    @SerialName("photo_names") val photoNames: List<String> = emptyList(),
    @SerialName("photo_attribution") val photoAttribution: String? = null,
    @SerialName("google_maps_url") val googleMapsUrl: String = "",
    val website: String = "",
    val phone: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
private data class AccommodationPhotoResponse(
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("photo_attribution") val photoAttribution: String? = null,
)

class AccommodationCatalogRepository(private val client: SupabaseClient) {
    suspend fun search(
        city: String,
        query: String,
        language: String,
        limit: Int = 60,
    ): List<AccommodationCatalogEntry> {
        val cityName = city.trim()
        if (cityName.isBlank()) return emptyList()
        if (client.auth.currentSessionOrNull() == null) {
            runCatching { client.auth.refreshCurrentSession() }
        }
        val accessToken = client.auth.currentAccessTokenOrNull()
        val response = client.functions.invoke(
            function = "restaurant-enrichment",
            body = buildJsonObject {
                put("category", "accommodation")
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
            .decodeFromString<AccommodationEnrichmentResponse>(response.bodyAsText())
            .accommodations
        return places.mapIndexedNotNull { index, place ->
            val name = place.name.trim()
            if (name.isBlank()) return@mapIndexedNotNull null
            AccommodationCatalogEntry(
                id = "google:${place.placeId.ifBlank { "$cityName:$index:$name" }}",
                placeId = place.placeId,
                name = name,
                city = cityName,
                address = place.address,
                type = place.type.ifBlank { place.category },
                rating = place.rating,
                reviewCount = place.reviewCount,
                priceLevel = place.priceLevel,
                photoUrl = place.photoUrl,
                photoName = place.photoName,
                photoNames = place.photoNames.ifEmpty { listOfNotNull(place.photoName.takeIf(String::isNotBlank)) },
                photoAttribution = place.photoAttribution,
                googleMapsUrl = place.googleMapsUrl,
                website = place.website,
                phone = place.phone,
                latitude = place.latitude,
                longitude = place.longitude,
            )
        }
    }

    suspend fun resolvePhoto(photoName: String): AccommodationPhotoResult? {
        val cleanPhotoName = photoName.trim()
        if (cleanPhotoName.isBlank()) return null
        if (client.auth.currentSessionOrNull() == null) {
            runCatching { client.auth.refreshCurrentSession() }
        }
        val accessToken = client.auth.currentAccessTokenOrNull()
        val response = client.functions.invoke(
            function = "restaurant-enrichment",
            body = buildJsonObject { put("photoName", cleanPhotoName) },
            headers = Headers.build {
                append(HttpHeaders.ContentType, "application/json")
                if (!accessToken.isNullOrBlank()) {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            },
        )
        return Json { ignoreUnknownKeys = true }
            .decodeFromString<AccommodationPhotoResponse>(response.bodyAsText())
            .let { AccommodationPhotoResult(it.photoUrl, it.photoAttribution) }
    }
}

private fun placesLanguageCode(language: String): String = when (
    language.trim().uppercase(Locale.ROOT).substringBefore('-')
) {
    "EN" -> "en"
    "ES" -> "es"
    "DE" -> "de"
    else -> "ru"
}
