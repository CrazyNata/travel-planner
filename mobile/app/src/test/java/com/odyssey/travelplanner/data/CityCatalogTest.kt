package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals

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

    @Test
    fun flagsResolveFromCityNamesAndCountryNames() {
        assertEquals("🇪🇸", cityFlag("Мадрид"))
        assertEquals("🇪🇸", cityFlag("Madrid, Spain"))
        assertEquals("🇩🇪", cityFlag("Berlin, Германия"))
        assertEquals("🇮🇹", cityFlag("Венеция"))
        assertEquals("🇫🇮", cityFlag("Хельсинки"))
        assertEquals("🇫🇮", cityFlag("Helsinki, Finland"))
        assertEquals("🇩🇪", cityFlag("Дрезден"))
        assertEquals("🇩🇪", cityFlag("Dresden"))
        assertEquals("📍", cityFlag("test city"))
    }

    @Test
    fun everyBundledCatalogCityHasAFlag() {
        cityCatalog.forEach { entry ->
            listOf(entry.russian, entry.english, entry.spanish, entry.german).forEach { name ->
                assertNotEquals("📍", cityFlag(name), "Missing flag for $name")
            }
        }
    }
}
