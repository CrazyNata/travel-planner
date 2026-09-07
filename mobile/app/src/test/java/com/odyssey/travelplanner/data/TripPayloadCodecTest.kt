package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class TripPayloadCodecTest {
    @Test
    fun sectionUpdateKeepsUnknownKeys() {
        val payload = buildJsonObject {
            put("title", "Existing trip")
            put("webOnlyField", "must survive")
        }

        val next = TripPayloadCodec.withSection(payload, "status", JsonPrimitive("Черновик"))

        assertEquals("Existing trip", next["title"]?.jsonPrimitive?.content)
        assertEquals("must survive", next["webOnlyField"]?.jsonPrimitive?.content)
        assertEquals("Черновик", next["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun arrayItemUpdateDoesNotReplaceOtherItems() {
        val payload = buildJsonObject {
            put("sights", kotlinx.serialization.json.buildJsonArray {
                add(buildJsonObject { put("id", "one"); put("name", "One") })
                add(buildJsonObject { put("id", "two"); put("name", "Two") })
            })
            put("metadata", "legacy")
        }

        val next = TripPayloadCodec.updateArrayItem(payload, "sights", "one") { item ->
            buildJsonObject {
                item.forEach { (key, value) -> put(key, value) }
                put("name", "Updated")
            }
        }

        val sights = next["sights"].toString()
        assertTrue(sights.contains("Updated"))
        assertTrue(sights.contains("Two"))
        assertEquals("legacy", next["metadata"]?.jsonPrimitive?.content)
    }

    @Test
    fun arrayItemsCanBeRemovedWithoutTouchingOtherPayloadKeys() {
        val payload = buildJsonObject {
            put("sights", kotlinx.serialization.json.buildJsonArray {
                add(buildJsonObject { put("id", "day-one"); put("walkDay", 1) })
                add(buildJsonObject { put("id", "day-two"); put("walkDay", 2) })
                add(buildJsonObject { put("id", "legacy"); put("walkDay", 0) })
            })
            put("metadata", "legacy")
        }

        val next = TripPayloadCodec.removeArrayItems(payload, "sights") { sight ->
            (sight["walkDay"]?.jsonPrimitive?.intOrNull ?: 0).coerceAtLeast(1) == 1
        }

        val sights = next["sights"].toString()
        assertTrue(!sights.contains("day-one"))
        assertTrue(sights.contains("day-two"))
        assertTrue(!sights.contains("legacy"))
        assertEquals("legacy", next["metadata"]?.jsonPrimitive?.content)
    }

    @Test
    fun sightseeingDayReorderKeepsNotesAndMovesSightsWithTheirDay() {
        val payload = buildJsonObject {
            put("sightDays", buildJsonArray {
                add(buildJsonObject { put("id", "day-a"); put("title", "Рим") })
                add(buildJsonObject { put("id", "day-b"); put("title", "Флоренция") })
            })
            put("sightDaysVersion", 1)
            put("sightNotes", buildJsonObject { put("day-b", "Lampredotto") })
            put("sights", buildJsonArray {
                add(buildJsonObject { put("id", "rome"); put("name", "Колизей"); put("walkDay", 1) })
                add(buildJsonObject { put("id", "florence"); put("name", "Piazza del Duomo"); put("walkDay", 2) })
            })
            put("webOnlyField", "must survive")
        }

        val next = TripPayloadCodec.reorderSightDays(
            payload = payload,
            currentDayIds = listOf("day-a", "day-b"),
            orderedDayIds = listOf("day-b", "day-a"),
        )

        assertEquals("day-b", next["sightDays"]!!.jsonArray[0].jsonObject["id"]?.jsonPrimitive?.content)
        assertEquals(2, next["sights"]!!.jsonArray[0].jsonObject["walkDay"]?.jsonPrimitive?.intOrNull)
        assertEquals(1, next["sights"]!!.jsonArray[1].jsonObject["walkDay"]?.jsonPrimitive?.intOrNull)
        assertEquals("Lampredotto", next["sightNotes"]!!.jsonObject["day-b"]?.jsonPrimitive?.contentOrNull)
        assertEquals("must survive", next["webOnlyField"]?.jsonPrimitive?.content)
    }
}
