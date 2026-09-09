package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RouteDistanceTest {
    @Test
    fun googleMapsApiLinkKeepsOrderedWaypoints() {
        val route = googleRouteCoordinates(
            "https://www.google.com/maps/dir/?api=1" +
                "&origin=45.8130357%2C9.0809691" +
                "&destination=45.9005485%2C9.4120248" +
                "&waypoints=45.8449282%2C9.0798188%7C45.9650877%2C9.2025018" +
                "&travelmode=walking",
        )

        assertEquals(true, route.fromQuery)
        assertEquals(4, route.coordinates.size)
        assertEquals(45.8130357, route.coordinates.first().latitude, absoluteTolerance = 0.0000001)
        assertEquals(9.0798188, route.coordinates[1].longitude, absoluteTolerance = 0.0000001)
        assertEquals(45.9005485, route.coordinates.last().latitude, absoluteTolerance = 0.0000001)
        assertEquals("walking", googleTravelProfile("https://www.google.com/maps/dir/?travelmode=walking"))
    }

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
