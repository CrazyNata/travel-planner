package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RouteDistanceTest {
    @Test
    fun oneDegreeOfLongitudeAtEquatorIsAbout111Kilometers() {
        val distance = straightLineDistanceKm(
            from = CityLocation(latitude = 0.0, longitude = 0.0),
            to = CityLocation(latitude = 0.0, longitude = 1.0),
        )

        assertEquals(111.195, distance, absoluteTolerance = 0.01)
    }

    @Test
    fun routeDistanceResolvesKnownCitiesFromCatalog() {
        val route = listOf(
            RouteLeg(
                dayId = "day-1",
                from = "Рим",
                to = "Флоренция",
                date = "",
                checkIn = "",
                checkOut = "",
                notes = "",
                mapsUrl = "",
            ),
        )

        val distance = straightLineRouteDistanceKm(route, emptyMap())

        val resolvedDistance = assertNotNull(distance)
        assertEquals(231.0, resolvedDistance, absoluteTolerance = 2.0)
    }
}
