package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CityCatalogTest {
    @Test
    fun localizedAliasesResolveToTheSameCity() {
        val aliases = listOf("Зальцбург", "Salzburg", "Salzburgo", "Salzburg, Austria")

        aliases.forEach { alias ->
            assertEquals("salzburg", cityCatalogEntry(alias)?.key)
        }
    }

    @Test
    fun everyCatalogEntryHasCoordinatesAndLocalizedNames() {
        cityCatalog.forEach { entry ->
            assertNotNull(cityCatalogEntry(entry.russian))
            assertNotNull(cityCatalogEntry(entry.english))
            assertNotNull(cityCatalogEntry(entry.spanish))
            assertNotNull(cityCatalogEntry(entry.german))
            assertEquals(entry.russian, entry.localized("RU"))
            assertEquals(entry.english, entry.localized("EN"))
            assertEquals(entry.spanish, entry.localized("ES"))
            assertEquals(entry.german, entry.localized("DE"))
        }
    }
}
