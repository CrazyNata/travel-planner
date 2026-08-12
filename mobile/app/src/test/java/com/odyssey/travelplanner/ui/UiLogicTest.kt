package com.odyssey.travelplanner.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UiLogicTest {
    @Test
    fun daySightNamesIgnoreEmptyDraftAfterAddingAPlace() {
        assertEquals(
            listOf("Colosseum"),
            daySightNamesToSave(listOf(" Colosseum "), "  "),
        )
    }

    @Test
    fun daySightNamesIncludeTypedDraftWhenItIsNotBlank() {
        assertEquals(
            listOf("Colosseum", "Trevi Fountain"),
            daySightNamesToSave(listOf("Colosseum"), " Trevi Fountain "),
        )
    }

    @Test
    fun routeTimingUsesCheckOutWhenCheckInIsEmpty() {
        assertEquals(RouteTiming(isCheckOut = true, value = "18:00"), routeTiming(" ", " 18:00 "))
    }

    @Test
    fun accommodationStatusNormalizesStoredValues() {
        assertEquals("бронь", normalizeAccommodationStatus("reserved"))
        assertEquals("оплачено", normalizeAccommodationStatus("paid"))
        assertEquals("хочу", normalizeAccommodationStatus(""))
    }

    @Test
    fun russianCountWordsUseCorrectForms() {
        fun cityWord(count: Int) = localizedCountWord(
            count,
            "RU",
            "город",
            "города",
            "городов",
            "city",
            "cities",
            "ciudad",
            "ciudades",
            "Stadt",
            "Städte",
        )

        assertEquals("город", cityWord(1))
        assertEquals("города", cityWord(2))
        assertEquals("городов", cityWord(5))
        assertEquals("городов", cityWord(11))
    }

    @Test
    fun routeSummaryOmitsUnknownTripDays() {
        assertEquals("1 ГОРОД", localizedRouteSummary(null, 1, "RU"))
        assertEquals("2 ДНЯ · 3 ГОРОДА", localizedRouteSummary(2, 3, "RU"))
    }

    @Test
    fun alreadyRegisteredAuthErrorsAreRecognizedThroughCauseChain() {
        assertTrue(isAlreadyRegisteredAuthError(IllegalStateException("User already registered")))
        assertTrue(isAlreadyRegisteredAuthError(IllegalStateException("request failed", IllegalArgumentException("user_already_exists"))))
        assertFalse(isAlreadyRegisteredAuthError(IllegalStateException("invalid login credentials")))
    }
}
