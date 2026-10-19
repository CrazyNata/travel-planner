package com.odyssey.travelplanner.ui

import com.odyssey.travelplanner.data.CityLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RestaurantMapTest {
    @Test
    fun emptyRestaurantListFramesTripCitiesInsteadOfRome() {
        val points = restaurantMapCameraPoints(emptyList(), listOf("Прага", "Зальцбург"))
        assertEquals(2, points.size)
        assertTrue(points.all { it.latitude() > 47.0 })
        assertTrue(points.none { it.latitude() in 41.0..42.0 })
    }

    @Test
    fun restaurantLocationsTakePriorityAndAreDeduplicated() {
        val points = restaurantMapCameraPoints(listOf("Рим", "Rome"), listOf("Прага"))
        assertEquals(1, points.size)
        assertTrue(points.single().latitude() in 41.0..42.0)
    }

    @Test
    fun selectedCityAndNewlyResolvedCoordinatesAreUsed() {
        assertTrue(restaurantMapCameraPoints(emptyList(), listOf("QA custom city")).isEmpty())
        val points = restaurantMapCameraPoints(emptyList(), listOf("QA custom city"), mapOf("QA custom city" to CityLocation(50.1, 14.4)))
        assertEquals(50.1, points.single().latitude())
        assertEquals(14.4, points.single().longitude())
        assertTrue(restaurantMapCameraPoints(emptyList(), emptyList()).isEmpty())
    }
}
