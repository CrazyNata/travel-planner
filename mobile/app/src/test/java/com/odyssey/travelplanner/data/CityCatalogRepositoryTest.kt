package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CityCatalogRepositoryTest {
    @Test
    fun searchIndexIncludesAllLocalizedNamesAndCountry() {
        val entry = CityCatalogEntry(
            key = "world:1",
            russian = "Moscow RU",
            english = "Moscow",
            spanish = "Moscu",
            german = "Moskau",
            latitude = 55.7522,
            longitude = 37.6156,
            aliases = setOf("moscow city"),
            countryName = "Russia",
        )

        val searchText = cityCatalogSearchText(
            entry.aliases +
                entry.countryName +
                entry.localized("RU") +
                entry.localized("EN") +
                entry.localized("ES") +
                entry.localized("DE"),
        )

        assertTrue(searchText.contains("moscow"))
        assertTrue(searchText.contains("moscow city"))
        assertTrue(searchText.contains("russia"))
        assertEquals(300, cityCatalogSearchScore(searchText, "moscow"))
        assertEquals(200, cityCatalogSearchScore(searchText, "mos"))
        assertEquals(100, cityCatalogSearchScore(searchText, "scow"))
        assertNull(cityCatalogSearchScore(searchText, "berlin"))
    }

    @Test
    fun searchScoreAllowsACommonTranspositionTypo() {
        val searchText = cityCatalogSearchText(listOf("Salzburg"))

        assertEquals(50, cityCatalogSearchScore(searchText, "Salzbrug"))
    }
}
