package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeviceLocationTest {
    @Test
    fun sameCoordinatesHaveZeroDistance() {
        val origin = DeviceLocation(45.4386, 10.9928)

        assertEquals(0.0, distanceMeters(origin, origin.latitude, origin.longitude))
    }

    @Test
    fun missingPlaceCoordinatesDoNotSortAsNearby() {
        val origin = DeviceLocation(45.4386, 10.9928)

        assertEquals(null, distanceMeters(origin, null, 10.9928))
        assertTrue(distanceMeters(origin, 45.44, 10.99)!! > 0.0)
    }
}
