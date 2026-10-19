package com.odyssey.travelplanner.data

import java.time.LocalDate

/** Changes presentation only; stored day numbers, dates and order remain untouched. */
internal fun routeDisplayIndices(dates: List<LocalDate?>, today: LocalDate): List<Int> =
    dates.indices.filter { dates[it]?.isBefore(today) != true } +
        dates.indices.filter { dates[it]?.isBefore(today) == true }

internal fun isDraftTripStatus(status: String): Boolean =
    listOf("чернов", "draft", "borrador", "entwurf").any { status.contains(it, true) }

internal fun tripStatusForDates(status: String, start: LocalDate?, end: LocalDate?, today: LocalDate): String {
    if (isDraftTripStatus(status)) return status
    if (start == null || end == null) return status
    return when {
        end.isBefore(today) -> "Завершено"
        start.isAfter(today) -> "Предстоящее"
        else -> "Текущее"
    }
}
