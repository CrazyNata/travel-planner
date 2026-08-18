package com.odyssey.travelplanner.ui.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.util.Locale
import java.time.temporal.ChronoUnit
import com.odyssey.travelplanner.ui.screen.tripedit.parseTripDateRange

internal fun budgetCurrencyCode(value: String): String = when (value.trim().uppercase(java.util.Locale.ROOT)) {
    "RUB", "₽" -> "RUB"
    "EUR", "€" -> "EUR"
    "CZK", "KČ", "Kč" -> "CZK"
    else -> "RUB"
}

internal fun budgetScopeValue(value: String): String = when (value.trim().lowercase(java.util.Locale.ROOT)) {
    "семья", "family" -> "семья"
    "личный", "личное", "personal" -> "личный"
    else -> "общий"
}

internal fun budgetTripDayCount(value: String): Int {
    parseTripDateRange(value)?.let { (start, end) ->
        return (ChronoUnit.DAYS.between(start, end).toInt() + 1).coerceAtLeast(1)
    }
    val match = Regex("""(\d+)\s*(?:дн\w*|day\w*|día\w*|tag\w*)""", RegexOption.IGNORE_CASE).find(value)
    return match?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
}

internal fun formatBudgetRate(value: Double): String {
    val symbols = java.text.DecimalFormatSymbols(java.util.Locale("ru", "RU")).apply {
        groupingSeparator = '\u00A0'
        decimalSeparator = ','
    }
    return java.text.DecimalFormat("#,##0.####", symbols).format(value)
}

internal fun formatBudgetRateInput(value: Double): String =
    java.text.DecimalFormat("0.####", java.text.DecimalFormatSymbols(java.util.Locale.US)).format(value)

internal fun formatBudgetInput(value: Double, conversionRate: Double): String =
    java.text.DecimalFormat("0.##", java.text.DecimalFormatSymbols(java.util.Locale.US)).format(value * conversionRate)

