package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SightLinkParserTest {
    @Test
    fun parsesGooglePlaceCoordinatesFromAtSegment() {
        val point = parseSightLinkCoordinates("https://www.google.com/maps/place/Eiffel+Tower/@48.85837,2.294481,17z")

        assertEquals(2.294481, point?.longitude)
        assertEquals(48.85837, point?.latitude)
    }

    @Test
    fun parsesGoogleSearchQueryCoordinates() {
        val point = parseSightLinkCoordinates("https://www.google.com/maps/search/?api=1&query=48.85837%2C2.294481")

        assertEquals(SightLinkCoordinates(2.294481, 48.85837), point)
    }

    @Test
    fun parsesYandexLongitudeLatitudeParameter() {
        val point = parseSightLinkCoordinates("https://yandex.ru/maps/?ll=37.6173%2C55.7558&z=12")

        assertEquals(SightLinkCoordinates(37.6173, 55.7558), point)
    }

    @Test
    fun ignoresTextLinksWithoutCoordinates() {
        assertNull(parseSightLinkCoordinates("https://www.google.com/maps/search/?api=1&query=Eiffel+Tower"))
    }
}
