package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TripProgressTest {
    @Test
    fun emptyTripHasNoProgress() {
        assertEquals(0, calculateTripProgress(buildJsonObject { }))
    }

    @Test
    fun populatedRouteIsNotStuckAtZero() {
        val payload = buildJsonObject {
            put("title", "Италия")
            put("dates", "2026-09-01 — 2026-09-10")
            put("cities", "Рим, Флоренция")
            put("days", buildJsonArray {
                add(buildJsonObject {
                    put("city", "Рим")
                    put("roadLeg", buildJsonObject {
                        put("from", "Рим")
                        put("to", "Флоренция")
                    })
                })
            })
            put("sights", buildJsonArray {
                add(buildJsonObject { put("name", "Колизей") })
            })
            put("budgetExpenses", buildJsonArray {
                add(buildJsonObject {
                    put("name", "Билеты")
                    put("amount", 100.0)
                })
            })
        }

        assertEquals(75, calculateTripProgress(payload))
    }

    @Test
    fun optionalSectionsCompleteTheProgress() {
        val payload = buildJsonObject {
            put("title", "Италия")
            put("startDate", "2026-09-01")
            put("cities", "Рим")
            put("days", buildJsonArray { add(buildJsonObject { put("city", "Рим") }) })
            put("sights", buildJsonArray { add(buildJsonObject { put("name", "Колизей") }) })
            put("restaurants", buildJsonArray { add(buildJsonObject { put("name", "Ресторан") }) })
            put("accommodations", buildJsonArray { add(buildJsonObject { put("name", "Отель") }) })
            put("budgetExpenses", buildJsonArray {
                add(buildJsonObject { put("name", "Билеты"); put("amount", 100.0) })
            })
            put("coverPhotos", buildJsonArray { add(buildJsonObject { put("image", "stored://cover") }) })
        }

        assertEquals(100, calculateTripProgress(payload))
    }

    @Test
    fun malformedLegacySectionsAreIgnored() {
        val payload = buildJsonObject {
            put("cities", 42)
            put("days", "not-an-array")
            put("sights", buildJsonArray { add(buildJsonObject { put("name", "") }) })
            put("budgetExpenses", buildJsonArray { add(buildJsonObject { put("amount", -1.0) }) })
        }

        assertTrue(calculateTripProgress(payload) >= 0)
    }
}
