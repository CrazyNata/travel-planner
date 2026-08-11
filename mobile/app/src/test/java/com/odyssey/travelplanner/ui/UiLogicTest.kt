package com.odyssey.travelplanner.ui

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
