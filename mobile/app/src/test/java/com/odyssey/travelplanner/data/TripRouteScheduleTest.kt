package com.odyssey.travelplanner.data

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class TripRouteScheduleTest {
    @Test
    fun routeCardsAreReadInChronologicalOrder() {
        val days = buildJsonArray {
            add(routeDay("first", "2026-08-16", "Prague", "Tallinn"))
            add(routeDay("second", "2026-08-17", "Tallinn", "Lake Como"))
            add(routeDay("third", "2026-08-04", "Saint Petersburg", "Tyumen"))
        }

        assertEquals(
            listOf("third", "first", "second"),
            routeDayIdsInDateOrder(days, LocalDate.of(2026, 8, 16)),
        )
    }

    @Test
    fun draggingRouteCardsAssignsTheDateSlotsToTheNewOrder() {
        val days = buildJsonArray {
            add(routeDay("first", "2026-08-16", "Prague", "Tallinn"))
            add(routeDay("second", "2026-08-17", "Tallinn", "Lake Como"))
            add(routeDay("third", "2026-08-04", "Saint Petersburg", "Tyumen"))
        }

        val next = synchronizeRouteDayOrder(
            days = days,
            orderedRouteDayIds = listOf("second", "third", "first"),
            startDate = LocalDate.of(2026, 8, 16),
        )
        val routeDays = next.filter { it is JsonObject && it["roadLeg"] != null }

        assertEquals(
            listOf("second", "third", "first"),
            routeDayIdsInDateOrder(next, LocalDate.of(2026, 8, 16)),
        )
        assertEquals(
            listOf("2026-08-04", "2026-08-16", "2026-08-17"),
            routeDays.map { (it as JsonObject)["date"]?.jsonPrimitive?.content },
        )
        assertEquals(
            listOf("Tallinn", "Saint Petersburg", "Prague"),
            routeDayObjectsInDateOrder(next, LocalDate.of(2026, 8, 16))
                .map { (it["roadLeg"] as JsonObject)["from"]!!.jsonPrimitive.content },
        )
    }

    @Test
    fun insertingRouteDayKeepsItsDateBetweenExistingCards() {
        val days = buildJsonArray {
            add(routeDay("first", "2026-08-16", "Prague", "Tallinn"))
            add(routeDay("second", "2026-08-17", "Tallinn", "Lake Como"))
        }

        val next = insertRouteDayInDateOrder(
            days = days,
            newDay = routeDay("new", "2026-08-16", "Berlin", "Prague"),
            startDate = LocalDate.of(2026, 8, 16),
        )

        assertEquals(
            listOf("first", "new", "second"),
            routeDayIdsInDateOrder(next, LocalDate.of(2026, 8, 16)),
        )
    }

    @Test
    fun sightOnlyDaysArePreservedWhenRouteCardsMove() {
        val days = buildJsonArray {
            add(routeDay("first", "2026-08-16", "Prague", "Tallinn"))
            add(buildJsonObject {
                put("id", "sights")
                put("places", buildJsonArray {
                    add(buildJsonObject { put("name", "Old Town") })
                })
            })
            add(routeDay("second", "2026-08-17", "Tallinn", "Lake Como"))
        }

        val next = synchronizeRouteDayOrder(
            days = days,
            orderedRouteDayIds = listOf("second", "first"),
            startDate = LocalDate.of(2026, 8, 16),
        )

        assertEquals("sights", (next[1] as JsonObject)["id"]?.jsonPrimitive?.content)
        assertEquals(
            listOf("second", "first"),
            routeDayIdsInDateOrder(next, LocalDate.of(2026, 8, 16)),
        )
    }

    private fun routeDay(id: String, date: String, from: String, to: String) = buildJsonObject {
        put("id", id)
        put("city", to)
        put("date", date)
        put("places", buildJsonArray { })
        put("roadLeg", buildJsonObject {
            put("from", from)
            put("to", to)
            put("completed", buildJsonArray { })
        })
    }
}
