package com.odyssey.travelplanner.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Small compatibility layer for the JSON payload stored in the existing trips table.
 * It deliberately changes only the requested key and keeps every other key untouched.
 */
object TripPayloadCodec {
    fun withSection(payload: JsonObject, key: String, value: JsonElement): JsonObject =
        JsonObject(payload.toMutableMap().apply { put(key, value) })

    fun append(payload: JsonObject, key: String, value: JsonElement): JsonObject {
        val next = buildList {
            payload[key]?.let { element ->
                if (element is JsonArray) addAll(element)
            }
            add(value)
        }
        return withSection(payload, key, JsonArray(next))
    }

    fun updateArrayItem(
        payload: JsonObject,
        key: String,
        id: String,
        update: (JsonObject) -> JsonObject,
    ): JsonObject = withSection(
        payload,
        key,
        JsonArray(
            (payload[key] as? JsonArray).orEmpty().map { item ->
                val objectItem = item as? JsonObject
                if (objectItem?.matchesId(id) == true) update(objectItem) else item
            },
        ),
    )

    fun removeArrayItem(payload: JsonObject, key: String, id: String): JsonObject = withSection(
        payload,
        key,
        JsonArray(
            (payload[key] as? JsonArray).orEmpty().filterNot { item ->
                (item as? JsonObject)?.matchesId(id) == true
            },
        ),
    )

    fun removeArrayItems(
        payload: JsonObject,
        key: String,
        predicate: (JsonObject) -> Boolean,
    ): JsonObject = withSection(
        payload,
        key,
        JsonArray(
            (payload[key] as? JsonArray).orEmpty().filterNot { item ->
                (item as? JsonObject)?.let(predicate) == true
            },
        ),
    )

    /**
     * Reorders the sightseeing-day rail and keeps every sight attached to the
     * same day by translating its numeric walkDay to the new position.
     *
     * The web client introduced sightDays after older Android payloads had
     * already been created. When that section is missing, the supplied day
     * ids still provide a stable compatibility mapping and the new section is
     * created without discarding any other payload keys.
     */
    fun reorderSightDays(
        payload: JsonObject,
        currentDayIds: List<String>,
        orderedDayIds: List<String>,
    ): JsonObject {
        require(currentDayIds.isNotEmpty()) { "Дни достопримечательностей отсутствуют" }
        require(currentDayIds.distinct().size == currentDayIds.size) { "Дни достопримечательностей содержат дубликаты" }
        require(orderedDayIds.size == currentDayIds.size && orderedDayIds.distinct().size == orderedDayIds.size) {
            "Список дней достопримечательностей изменился"
        }
        require(orderedDayIds.toSet() == currentDayIds.toSet()) {
            "Список дней достопримечательностей изменился"
        }

        val existingDays = (payload["sightDays"] as? JsonArray).orEmpty()
        val existingDaysById = existingDays.mapNotNull { item ->
            val day = item as? JsonObject ?: return@mapNotNull null
            day.stringValue("id")?.takeIf(String::isNotBlank)?.let { it to day }
        }.toMap()
        val fallbackTitles = (payload["days"] as? JsonArray).orEmpty().map { item ->
            val day = item as? JsonObject ?: return@map ""
            val roadLeg = day["roadLeg"] as? JsonObject
            roadLeg?.stringValue("to")
                ?.takeIf(String::isNotBlank)
                ?: day.stringValue("city").orEmpty()
        }
        val fallbackSightCities = (payload["sights"] as? JsonArray).orEmpty()
            .mapNotNull { item ->
                val sight = item as? JsonObject ?: return@mapNotNull null
                val walkDay = sight.stringValue("walkDay")?.toIntOrNull() ?: return@mapNotNull null
                val city = sight.stringValue("city").orEmpty()
                walkDay to city
            }
            .toMap()

        val nextDays = JsonArray(orderedDayIds.map { id ->
            existingDaysById[id] ?: buildJsonObject {
                put("id", JsonPrimitive(id))
                val fallbackIndex = currentDayIds.indexOf(id)
                (fallbackTitles.getOrNull(fallbackIndex)
                    ?.takeIf(String::isNotBlank)
                    ?: fallbackSightCities[fallbackIndex + 1])
                    ?.takeIf(String::isNotBlank)
                    ?.let { put("title", JsonPrimitive(it)) }
            }
        })
        val newPositionByDayId = orderedDayIds.withIndex().associate { (index, id) -> id to index + 1 }
        val oldDayIdByPosition = currentDayIds.withIndex().associate { (index, id) -> index + 1 to id }
        val nextSights = (payload["sights"] as? JsonArray)?.let { sights ->
            JsonArray(sights.map { item ->
                val sight = item as? JsonObject ?: return@map item
                val oldWalkDay = sight["walkDay"]?.let { value ->
                    value.toString().trim('"').toIntOrNull()
                } ?: return@map item
                val oldDayId = oldDayIdByPosition[oldWalkDay] ?: return@map item
                val newWalkDay = newPositionByDayId[oldDayId] ?: return@map item
                JsonObject(sight.toMutableMap().apply {
                    put("walkDay", JsonPrimitive(newWalkDay))
                })
            })
        } ?: JsonArray(emptyList())

        return JsonObject(payload.toMutableMap().apply {
            put("sightDays", nextDays)
            put("sightDaysVersion", JsonPrimitive(1))
            put("sights", nextSights)
        })
    }

    private fun JsonObject.stringValue(key: String): String? =
        this[key]?.toString()?.trim('"')

    private fun JsonObject.matchesId(id: String): Boolean =
        stringValue("id") == id || (stringValue("id").isNullOrBlank() && stringValue("name") == id)
}
