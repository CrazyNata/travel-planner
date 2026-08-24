package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class AccommodationOrderTest {
    @Test
    fun draggingAccommodationPreservesObjectsAndChangesOnlyTheirOrder() {
        val accommodations = buildJsonArray {
            add(accommodation("later", "Munich", "photo-a"))
            add(accommodation("first", "Inzell", "photo-b"))
        }

        val reordered = reorderAccommodationItems(accommodations, listOf("first", "later"))

        assertEquals(
            listOf("first", "later"),
            reordered.map { it.jsonObject["id"]!!.jsonPrimitive.content },
        )
        assertEquals("photo-b", reordered[0].jsonObject["photo"]!!.jsonPrimitive.content)
        assertEquals("photo-a", reordered[1].jsonObject["photo"]!!.jsonPrimitive.content)
    }

    private fun accommodation(id: String, city: String, photo: String) = buildJsonObject {
        put("id", id)
        put("name", "$city hotel")
        put("city", city)
        put("photo", photo)
    }
}
