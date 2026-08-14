package com.odyssey.travelplanner.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

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
    @SerialName("created_at") val createdAt: String = "",
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
)

class SightCatalogRepository(private val client: SupabaseClient) {
    suspend fun search(city: String, query: String, limit: Int = 60): List<SightCatalogEntry> {
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
