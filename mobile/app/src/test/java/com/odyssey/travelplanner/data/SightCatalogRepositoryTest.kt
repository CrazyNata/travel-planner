package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SightCatalogRepositoryTest {
    @Test
    fun cityNameStripsRouteSuffixesBeforeCatalogLookup() {
        assertEquals("Prague", catalogCityName("Prague, Czechia"))
        assertEquals("Прага", catalogCityName("Прага — день 1"))
    }

    @Test
    fun searchTextNormalizationSupportsCyrillicAliases() {
        assertEquals("мюнхен", normalizeCatalogText("  МЮНХЁН  "))
        assertTrue(normalizeCatalogText("Карлов мост").startsWith("карлов"))
    }

    @Test
    fun localizedNameAndDescriptionFallBackToRussian() {
        val entry = SightCatalogEntry(
            id = "test",
            cityKey = "test-city",
            cityNameRu = "Тест",
            cityNameEn = "Test",
            cityNameEs = "",
            cityNameDe = "",
            nameRu = "Место",
            nameEn = "Place",
            nameEs = "",
            nameDe = "",
            descriptionRu = "Описание",
            descriptionEn = "Description",
            descriptionEs = "",
            descriptionDe = "",
            category = "достопримечательности",
            latitude = null,
            longitude = null,
            mapUrl = "",
            searchText = "place|место",
            sortOrder = 0,
        )

        assertEquals("Place", entry.name("EN"))
        assertEquals("Место", entry.name("ES"))
        assertEquals("Description", entry.description("DE"))
    }
}
