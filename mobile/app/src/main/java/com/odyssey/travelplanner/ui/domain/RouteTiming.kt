package com.odyssey.travelplanner.ui.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal data class RouteTiming(val isCheckOut: Boolean, val value: String)

internal fun routeTiming(checkIn: String, checkOut: String): RouteTiming {
    val normalizedCheckIn = checkIn.trim()
    val normalizedCheckOut = checkOut.trim()
    return when {
        normalizedCheckIn.isNotBlank() -> RouteTiming(isCheckOut = false, value = normalizedCheckIn)
        normalizedCheckOut.isNotBlank() -> RouteTiming(isCheckOut = true, value = normalizedCheckOut)
        else -> RouteTiming(isCheckOut = false, value = "—")
    }
}

