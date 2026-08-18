package com.odyssey.travelplanner.ui.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.util.Locale
import java.net.URLEncoder
import java.util.Calendar
import com.odyssey.travelplanner.ui.i18n.normalizeLanguage

internal fun formatAccommodationDeadline(value: String, language: String): String {
    val raw = value.trim()
    val match = Regex("(\\d{4})-(\\d{2})-(\\d{2})").matchEntire(raw) ?: return raw
    val months = when (normalizeLanguage(language)) {
        "EN" -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "ES" -> listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        "DE" -> listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")
        else -> listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
    }
    return "${match.groupValues[3].toInt()} ${months[match.groupValues[2].toInt() - 1]}"
}

internal fun formatAccommodationPrice(value: String): String {
    val raw = value.trim()
    if (raw.isBlank()) return ""
    return if (raw.firstOrNull() in listOf('€', '$', '£', '₽') || raw.lastOrNull() in listOf('€', '$', '£', '₽')) raw else "€$raw"
}

internal fun accommodationPriceLevelLabel(priceLevel: Int?): String =
    priceLevel?.coerceIn(1, 4)?.let { "€".repeat(it) }.orEmpty()

internal fun accommodationBookingSearchUrl(name: String, city: String): String {
    val query = URLEncoder.encode(listOf(name.trim(), city.trim()).filter(String::isNotBlank).joinToString(" "), "UTF-8")
    return "https://www.booking.com/searchresults.html?ss=$query"
}

internal fun normalizeAccommodationStatus(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
    "want", "хочу" -> "хочу"
    "reserve", "reserved", "бронь" -> "бронь"
    "paid", "оплачено" -> "оплачено"
    "stayed", "пожили" -> "пожили"
    else -> value.trim().ifBlank { "хочу" }
}

internal fun accommodationDateCalendar(value: String): Calendar {
    val match = Regex("(\\d{4})-(\\d{2})-(\\d{2})").find(value)
    return Calendar.getInstance().apply {
        if (match != null) {
            clear()
            set(match.groupValues[1].toInt(), match.groupValues[2].toInt() - 1, match.groupValues[3].toInt())
        }
    }
}

internal fun accommodationDateIso(year: Int, month: Int, day: Int): String =
    "${year.toString().padStart(4, '0')}-${(month + 1).toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

internal fun accommodationDateRange(start: String, end: String, original: String): String {
    val checkIn = start.trim()
    val checkOut = end.trim()
    return when {
        checkIn.isNotBlank() && checkOut.isNotBlank() -> "$checkIn – $checkOut"
        checkIn.isNotBlank() -> checkIn
        checkOut.isNotBlank() -> checkOut
        else -> original.trim()
    }
}

