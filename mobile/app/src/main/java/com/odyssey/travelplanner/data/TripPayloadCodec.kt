package com.odyssey.travelplanner.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

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

    private fun JsonObject.stringValue(key: String): String? =
        this[key]?.toString()?.trim('"')

    private fun JsonObject.matchesId(id: String): Boolean =
        stringValue("id") == id || (stringValue("id").isNullOrBlank() && stringValue("name") == id)
}
