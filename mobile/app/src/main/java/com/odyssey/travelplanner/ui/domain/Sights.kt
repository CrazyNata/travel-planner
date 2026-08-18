package com.odyssey.travelplanner.ui.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal fun sightRouteDay(walkDay: Int): Int = walkDay.coerceAtLeast(1)

internal fun routeLegDayNumber(
    leg: com.odyssey.travelplanner.data.RouteLeg,
    legs: List<com.odyssey.travelplanner.data.RouteLeg>,
): Int = leg.dayNumber.takeIf { it > 0 } ?: (legs.indexOf(leg) + 1)

internal fun daySightNamesToSave(placeNames: List<String>, draftName: String): List<String> = buildList {
    placeNames
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .forEach { add(it) }
    draftName.trim().takeIf { it.isNotBlank() }?.let { add(it) }
}

internal fun isAlreadyRegisteredAuthError(error: Throwable): Boolean =
    generateSequence(error) { it.cause }.any { candidate ->
        val message = candidate.message?.lowercase().orEmpty()
        message.contains("already registered") ||
            message.contains("user_already_exists") ||
            message.contains("already exists")
    }

