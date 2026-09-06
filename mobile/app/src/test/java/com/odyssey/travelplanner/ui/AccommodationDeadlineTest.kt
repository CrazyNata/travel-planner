package com.odyssey.travelplanner.ui

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccommodationDeadlineTest {
    private val today = LocalDate.of(2026, 12, 10)

    @Test
    fun calculatesDaysRemainingFromStoredIsoDate() {
        val status = accommodationDeadlineStatus("2026-12-17", today)

        assertEquals(7L, status?.daysRemaining)
        assertEquals(LocalDate.of(2026, 12, 17), status?.deadline)
    }

    @Test
    fun supportsLegacyDeadlineFormats() {
        assertEquals(7L, accommodationDeadlineStatus("17.12.2026", today)?.daysRemaining)
        assertEquals(7L, accommodationDeadlineStatus("17 декабря 2026", today)?.daysRemaining)
    }

    @Test
    fun marksTodayAndPastDeadlines() {
        assertEquals(0L, accommodationDeadlineStatus("2026-12-10", today)?.daysRemaining)
        assertEquals(-1L, accommodationDeadlineStatus("2026-12-09", today)?.daysRemaining)
    }

    @Test
    fun keepsInvalidDeadlineSafe() {
        assertNull(accommodationDeadlineStatus("not-a-date", today))
    }

    @Test
    fun localizesCountdownLabels() {
        assertEquals("Осталось 7 дней", accommodationDeadlineCountdownLabel(7, "RU"))
        assertEquals("1 day left", accommodationDeadlineCountdownLabel(1, "EN"))
        assertEquals("Today", accommodationDeadlineCountdownLabel(0, "EN"))
        assertEquals("Cancellation ended", accommodationDeadlineCountdownLabel(-1, "EN"))
    }
}
