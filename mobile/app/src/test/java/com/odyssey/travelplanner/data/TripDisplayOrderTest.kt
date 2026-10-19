package com.odyssey.travelplanner.data

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TripDisplayOrderTest {
    private val today = LocalDate.of(2026, 10, 14)
    @Test fun currentAndFutureDaysPrecedePastDays() {
        assertEquals(listOf(1, 3, 0, 2), routeDisplayIndices(listOf(today.minusDays(1), today, today.minusDays(2), today.plusDays(1)), today))
    }
    @Test fun finishedTripsKeepTheirChosenOrder() {
        assertEquals(listOf(0, 1), routeDisplayIndices(listOf(today.minusDays(2), today.minusDays(1)), today))
    }
    @Test fun missingDatesDoNotHideDays() {
        assertEquals(listOf(1, 2, 0), routeDisplayIndices(listOf(today.minusDays(1), null, today), today))
    }
    @Test fun tripIsCurrentOnBothBoundaryDates() {
        assertEquals("Текущее", tripStatusForDates("Предстоящее", today, today.plusDays(1), today))
        assertEquals("Текущее", tripStatusForDates("Предстоящее", today.minusDays(1), today, today))
        assertEquals("Завершено", tripStatusForDates("Текущее", today.minusDays(2), today.minusDays(1), today))
        assertEquals("Предстоящее", tripStatusForDates("Завершено", today.plusDays(1), today.plusDays(2), today))
    }
    @Test fun draftsAndUndatedTripsArePreserved() {
        for (draft in listOf("Draft", "Borrador", "Entwurf", "Черновик")) {
            assertEquals(true, isDraftTripStatus(draft))
            assertEquals(draft, tripStatusForDates(draft, today, today, today))
        }
        assertEquals("Draft", tripStatusForDates("Draft", today.minusDays(1), today, today))
        assertEquals("Черновик", tripStatusForDates("Черновик", today.minusDays(1), today, today))
        assertEquals("custom", tripStatusForDates("custom", null, null, today))
    }
}
