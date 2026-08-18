package com.odyssey.travelplanner.ui.screen.trip.route

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.util.Locale
import java.util.Calendar
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.normalizeLanguage

internal fun formatAccommodationDates(value: String, language: String): String {
    val raw = value.trim()
    if (raw.isBlank()) return localized(language, "Даты не указаны", "Dates not specified", "Fechas no indicadas", "Keine Daten angegeben")
    val parts = raw.split(Regex("\\s+[–-]\\s+"))
    if (parts.size != 2) return localizeLegacyAccommodationDateText(raw, language)
    fun parseIso(source: String): Calendar? {
        val match = Regex("(\\d{4})-(\\d{2})-(\\d{2})").matchEntire(source.trim()) ?: return null
        return Calendar.getInstance().apply {
            clear()
            set(match.groupValues[1].toInt(), match.groupValues[2].toInt() - 1, match.groupValues[3].toInt())
        }
    }
    val start = parseIso(parts[0]) ?: return localizeLegacyAccommodationDateText(raw, language)
    val end = parseIso(parts[1]) ?: return localizeLegacyAccommodationDateText(raw, language)
    val months = when (normalizeLanguage(language)) {
        "EN" -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "ES" -> listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        "DE" -> listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")
        else -> listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
    }
    val startDay = start.get(Calendar.DAY_OF_MONTH)
    val endDay = end.get(Calendar.DAY_OF_MONTH)
    val startMonth = months[start.get(Calendar.MONTH)]
    val endMonth = months[end.get(Calendar.MONTH)]
    val range = if (start.get(Calendar.MONTH) == end.get(Calendar.MONTH)) {
        "$startDay–$endDay $endMonth"
    } else {
        "$startDay $startMonth – $endDay $endMonth"
    }
    return range
}

internal fun localizeLegacyAccommodationDateText(value: String, language: String): String {
    val monthNames = when (normalizeLanguage(language)) {
        "EN" -> mapOf("янв" to "Jan", "фев" to "Feb", "мар" to "Mar", "апр" to "Apr", "май" to "May", "июн" to "Jun", "июл" to "Jul", "авг" to "Aug", "сен" to "Sep", "окт" to "Oct", "ноя" to "Nov", "дек" to "Dec")
        "ES" -> mapOf("янв" to "ene", "фев" to "feb", "мар" to "mar", "апр" to "abr", "май" to "may", "июн" to "jun", "июл" to "jul", "авг" to "ago", "сен" to "sep", "окт" to "oct", "ноя" to "nov", "дек" to "dic")
        "DE" -> mapOf("янв" to "Jan", "фев" to "Feb", "мар" to "Mär", "апр" to "Apr", "май" to "Mai", "июн" to "Jun", "июл" to "Jul", "авг" to "Aug", "сен" to "Sep", "окт" to "Okt", "ноя" to "Nov", "дек" to "Dez")
        else -> mapOf("янв" to "янв", "фев" to "фев", "мар" to "мар", "апр" to "апр", "май" to "май", "июн" to "июн", "июл" to "июл", "авг" to "авг", "сен" to "сен", "окт" to "окт", "ноя" to "ноя", "дек" to "дек")
    }
    var result = Regex("(?i)(?<![\\p{L}])(янв|фев|мар|апр|май|июн|июл|авг|сен|окт|ноя|дек)(?![\\p{L}])").replace(value) { match ->
        monthNames[match.value.lowercase(Locale.ROOT)] ?: match.value
    }
    val nightPattern = Regex("(?i)(\\d+)\\s+(ночь|ночи|ночей)")
    result = nightPattern.replace(result) { match ->
        val count = match.groupValues[1].toIntOrNull() ?: 0
        val word = when (normalizeLanguage(language)) {
            "EN" -> if (count == 1) "night" else "nights"
            "ES" -> if (count == 1) "noche" else "noches"
            "DE" -> if (count == 1) "Nacht" else "Nächte"
            else -> when {
                count % 10 == 1 && count % 100 != 11 -> "ночь"
                count % 10 in 2..4 && count % 100 !in 12..14 -> "ночи"
                else -> "ночей"
            }
        }
        "${match.groupValues[1]} $word"
    }
    return result
}

internal fun accommodationDateParts(value: String): Pair<String, String> {
    val matches = Regex("\\d{4}-\\d{2}-\\d{2}").findAll(value).map { it.value }.toList()
    if (matches.size >= 2) return matches[0] to matches[1]
    val parts = value.trim().split(Regex("\\s+[–-]\\s+"))
    return (matches.firstOrNull() ?: parts.getOrNull(0).orEmpty()) to parts.getOrNull(1).orEmpty()
}

