package com.odyssey.travelplanner.ui.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal val DefaultOverviewBlocks = listOf("photo", "map", "weather")

internal fun normalizedOverviewBlocks(value: List<String>): List<String> =
    (value.filter { it in DefaultOverviewBlocks } + DefaultOverviewBlocks.filterNot(value::contains)).distinct()

internal fun List<String>.toggleOverviewCity(city: String): List<String> =
    if (any { cityFilterKey(it) == cityFilterKey(city) }) {
        filterNot { cityFilterKey(it) == cityFilterKey(city) }
    } else {
        this + city
    }

