package com.odyssey.travelplanner.ui

internal enum class CreateTripValidationError {
    TITLE_REQUIRED,
    DATES_REQUIRED,
    CITIES_REQUIRED,
}

internal fun validateCreateTripRequiredFields(
    title: String,
    startDate: String,
    endDate: String,
    cities: List<String>,
): CreateTripValidationError? = when {
    title.isBlank() -> CreateTripValidationError.TITLE_REQUIRED
    startDate.isBlank() || endDate.isBlank() -> CreateTripValidationError.DATES_REQUIRED
    cities.isEmpty() -> CreateTripValidationError.CITIES_REQUIRED
    else -> null
}
