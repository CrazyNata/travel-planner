package com.odyssey.travelplanner.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Calculates the route completion shown on a trip card from the payload that
 * already belongs to the trip. This deliberately stays in the Android client:
 * the shared `trips.payload` schema remains unchanged.
 */
internal fun calculateTripProgress(payload: JsonObject): Int {
    fun text(key: String): String = runCatching {
        payload[key]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    }.getOrDefault("")

    fun array(key: String): JsonArray = runCatching {
        payload[key]?.jsonArray ?: JsonArray(emptyList())
    }.getOrDefault(JsonArray(emptyList()))

    fun hasNamedEntry(key: String): Boolean = array(key).any { element ->
        runCatching { element.jsonObject["name"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true }
            .getOrDefault(false)
    }

    fun hasRouteDay(): Boolean = array("days").any { element ->
        runCatching {
            val day = element.jsonObject
            day["city"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true ||
                day["date"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true ||
                day["roadLeg"]?.jsonObject?.let { leg ->
                    leg["from"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true ||
                        leg["to"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true
                } == true
        }.getOrDefault(false)
    }

    fun hasCity(): Boolean {
        val value = payload["cities"] ?: return false
        return when (value) {
            is JsonArray -> value.any { runCatching { it.jsonPrimitive.contentOrNull?.isNotBlank() == true }.getOrDefault(false) }
            else -> text("cities").split(',').any(String::isNotBlank)
        }
    }

    fun hasBudget(): Boolean = array("budgetExpenses").any { element ->
        runCatching {
            val expense = element.jsonObject
            val name = expense["name"]?.jsonPrimitive?.contentOrNull.orEmpty().isNotBlank()
            val amount = expense["amount"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            name && amount > 0.0
        }.getOrDefault(false)
    }

    fun hasCover(): Boolean = text("coverImage").isNotBlank() || array("coverPhotos").any { element ->
        runCatching {
            val cover = element.jsonObject
            listOf("image", "url", "photo").any { key ->
                cover[key]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true
            }
        }.getOrDefault(false)
    }

    // Weights add up to 100. Core route fields carry most of the score while
    // optional planning sections make steady progress instead of jumping to
    // 100% after the first sight is added.
    val completedWeight = listOf(
        5 to text("title").isNotBlank(),
        5 to (text("dates").isNotBlank() || text("startDate").isNotBlank() || text("endDate").isNotBlank()),
        15 to hasCity(),
        20 to hasRouteDay(),
        20 to hasNamedEntry("sights"),
        10 to hasNamedEntry("restaurants"),
        10 to hasNamedEntry("accommodations"),
        10 to hasBudget(),
        5 to hasCover(),
    ).sumOf { (weight, complete) -> if (complete) weight else 0 }

    return completedWeight.coerceIn(0, 100)
}
