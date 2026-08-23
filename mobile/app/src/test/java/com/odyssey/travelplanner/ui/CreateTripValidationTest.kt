package com.odyssey.travelplanner.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CreateTripValidationTest {
    @Test
    fun emptyFormRequiresTitleBeforeAnythingElse() {
        assertEquals(
            CreateTripValidationError.TITLE_REQUIRED,
            validateCreateTripRequiredFields(" ", "", "", emptyList()),
        )
    }

    @Test
    fun titleWithoutBothDatesIsRejected() {
        assertEquals(
            CreateTripValidationError.DATES_REQUIRED,
            validateCreateTripRequiredFields("Италия", "2026-09-01", "", listOf("Рим")),
        )
    }

    @Test
    fun datesWithoutCitiesAreRejected() {
        assertEquals(
            CreateTripValidationError.CITIES_REQUIRED,
            validateCreateTripRequiredFields("Италия", "2026-09-01", "2026-09-10", emptyList()),
        )
    }

    @Test
    fun completeRequiredFieldsAreAccepted() {
        assertNull(
            validateCreateTripRequiredFields("Италия", "2026-09-01", "2026-09-10", listOf("Рим")),
        )
    }
}
