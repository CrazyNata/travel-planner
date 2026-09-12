package com.odyssey.travelplanner.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.time.LocalDate

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
    fun accommodationCheckOutCannotPrecedeCheckIn() {
        assertTrue(accommodationDatesAreChronological("2026-08-25", "2026-08-25"))
        assertTrue(accommodationDatesAreChronological("2026-08-25", "2026-08-26"))
        assertFalse(accommodationDatesAreChronological("2026-08-25", "2026-08-24"))
        assertTrue(accommodationDatesAreChronological("", "2026-08-24"))
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

    @Test
    fun overviewBlockNormalizationUsesDefaultsForLegacyTrips() {
        assertEquals(listOf("photo", "map"), normalizedOverviewBlocks(listOf("photo", "map")))
        assertEquals(listOf("photo", "map", "weather"), normalizedOverviewBlocks(emptyList()))
    }

    @Test
    fun weatherUsesSharedTripCitiesBeforeMapOnlySelection() {
        assertEquals(
            listOf("Инцелль", "Верона", "Рим"),
            weatherCitiesForOverview(
                overviewWeatherCities = emptyList(),
                tripCities = listOf(" Инцелль ", "Верона", "Рим"),
                fallbackCities = listOf("Верона", "Пиза"),
            ),
        )
        assertEquals(
            listOf("Пиза"),
            weatherCitiesForOverview(
                overviewWeatherCities = listOf("Пиза"),
                tripCities = listOf("Рим"),
                fallbackCities = listOf("Верона"),
            ),
        )
    }

    @Test
    fun weatherTripDatesIncludesBothEndsOfTheTrip() {
        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 25),
                LocalDate.of(2026, 9, 26),
                LocalDate.of(2026, 9, 27),
            ),
            weatherTripDates("2026-09-25 – 2026-09-27"),
        )
    }

    @Test
    fun weatherTripDatesCapsVeryLongRanges() {
        assertEquals(
            listOf(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26)),
            weatherTripDates("2026-09-25 – 2027-09-25", maxDays = 2),
        )
    }
}
