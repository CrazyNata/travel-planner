package com.odyssey.travelplanner.ui.screen.trip.route

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.util.Locale
import java.net.URLEncoder
import java.util.Calendar
import java.time.temporal.ChronoUnit
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.normalizeLanguage
import com.odyssey.travelplanner.ui.screen.tripedit.calendarForTripDate
import com.odyssey.travelplanner.ui.screen.tripedit.parseTripDateRange

internal data class RouteEditorDateValues(val day: String, val month: String, val weekday: String)

internal val routeEditorMonthIndices = mapOf(
    "января" to 0, "январь" to 0, "янв" to 0,
    "февраля" to 1, "февраль" to 1, "фев" to 1,
    "марта" to 2, "март" to 2, "мар" to 2,
    "апреля" to 3, "апрель" to 3, "апр" to 3,
    "мая" to 4, "май" to 4, "май" to 4,
    "июня" to 5, "июнь" to 5, "июн" to 5,
    "июля" to 6, "июль" to 6, "июл" to 6,
    "августа" to 7, "август" to 7, "авг" to 7,
    "сентября" to 8, "сентябрь" to 8, "сен" to 8,
    "октября" to 9, "октябрь" to 9, "окт" to 9,
    "ноября" to 10, "ноябрь" to 10, "ноя" to 10,
    "декабря" to 11, "декабрь" to 11, "дек" to 11,
    "january" to 0, "jan" to 0, "february" to 1, "feb" to 1, "march" to 2, "mar" to 2,
    "april" to 3, "apr" to 3, "may" to 4, "june" to 5, "jun" to 5, "july" to 6, "jul" to 6,
    "august" to 7, "aug" to 7, "september" to 8, "sep" to 8, "october" to 9, "oct" to 9,
    "november" to 10, "nov" to 10, "december" to 11, "dec" to 11,
    "enero" to 0, "ene" to 0, "febrero" to 1, "marzo" to 2, "abril" to 3, "mayo" to 4,
    "junio" to 5, "julio" to 6, "agosto" to 7, "septiembre" to 8, "octubre" to 9,
    "noviembre" to 10, "diciembre" to 11,
    "januar" to 0, "märz" to 2, "maerz" to 2, "juni" to 5, "juli" to 6, "august" to 7,
    "september" to 8, "oktober" to 9, "dezember" to 11,
)

internal fun routeEditorMonthIndex(value: String): Int? {
    val normalized = value.trim().lowercase(Locale.ROOT).removeSuffix(".")
    return normalized.toIntOrNull()?.minus(1)?.takeIf { it in 0..11 } ?: routeEditorMonthIndices[normalized]
}

internal fun parseRouteDate(source: String): Calendar? {
    val iso = Regex("(\\d{4})-(\\d{2})-(\\d{2})").find(source)
    val dotted = Regex("(\\d{1,2})[./](\\d{1,2})[./](\\d{4})").find(source)
    val monthPattern = routeEditorMonthIndices.keys.sortedByDescending(String::length).joinToString("|") { Regex.escape(it) }
    val named = Regex("(\\d{1,2})\\s+($monthPattern)\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(source)
    return when {
        iso != null -> Calendar.getInstance().apply { clear(); set(iso.groupValues[1].toInt(), iso.groupValues[2].toInt() - 1, iso.groupValues[3].toInt()) }
        dotted != null -> Calendar.getInstance().apply { clear(); set(dotted.groupValues[3].toInt(), dotted.groupValues[2].toInt() - 1, dotted.groupValues[1].toInt()) }
        named != null -> Calendar.getInstance().apply { clear(); set(named.groupValues[3].toInt(), routeEditorMonthIndex(named.groupValues[2]) ?: 0, named.groupValues[1].toInt()) }
        else -> null
    }
}

internal fun routeEditorDateIso(date: String, tripDates: String, dayIndex: Int, storedDay: String, storedMonth: String): String {
    val legDate = parseRouteDate(date)
    val calendar = (legDate ?: parseRouteDate(tripDates))?.also {
        if (legDate == null) it.add(Calendar.DAY_OF_YEAR, dayIndex)
    } ?: if (storedDay.toIntOrNull() != null && routeEditorMonthIndex(storedMonth) != null) {
        Calendar.getInstance()
    } else {
        return ""
    }
    val day = storedDay.toIntOrNull()
    val month = routeEditorMonthIndex(storedMonth)
    if (day != null && month != null) {
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)))
    }
    return accommodationDateIso(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
}

internal fun routeEditorDateLabel(date: String, language: String): String {
    val calendar = parseRouteDate(date) ?: return ""
    val months = when (normalizeLanguage(language)) {
        "EN" -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "ES" -> listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        "DE" -> listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")
        else -> listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
    }
    return "${calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')} ${months[calendar.get(Calendar.MONTH)]} ${calendar.get(Calendar.YEAR)}"
}

internal fun routeEditorDateValues(date: String, tripDates: String, dayIndex: Int, language: String): RouteEditorDateValues {
    val legDate = parseRouteDate(date)
    val calendar = (legDate ?: parseRouteDate(tripDates)) ?: return RouteEditorDateValues("", "", "")
    if (legDate == null) calendar.add(Calendar.DAY_OF_YEAR, dayIndex)
    val months = when (normalizeLanguage(language)) {
        "EN" -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "ES" -> listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        "DE" -> listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")
        else -> listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
    }
    val weekdays = when (normalizeLanguage(language)) {
        "EN" -> listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        "ES" -> listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        "DE" -> listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
        else -> listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")
    }
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val weekdayIndex = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
    return RouteEditorDateValues(
        day = calendar.get(Calendar.DAY_OF_MONTH).toString(),
        month = months[calendar.get(Calendar.MONTH)],
        weekday = weekdays[weekdayIndex],
    )
}

internal fun routeDateParts(date: String, tripDates: String, dayIndex: Int, language: String): Pair<String, String> {
    val months = when (normalizeLanguage(language)) {
        "EN" -> listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        "ES" -> listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")
        "DE" -> listOf("JAN", "FEB", "MÄR", "APR", "MAI", "JUN", "JUL", "AUG", "SEP", "OKT", "NOV", "DEZ")
        else -> listOf("ЯНВ", "ФЕВ", "МАР", "АПР", "МАЙ", "ИЮН", "ИЮЛ", "АВГ", "СЕН", "ОКТ", "НОЯ", "ДЕК")
    }
    val russianMonths = mapOf("января" to 0, "январь" to 0, "февраля" to 1, "февраль" to 1, "марта" to 2, "март" to 2, "апреля" to 3, "апрель" to 3, "мая" to 4, "май" to 4, "июня" to 5, "июнь" to 5, "июля" to 6, "июль" to 6, "августа" to 7, "август" to 7, "сентября" to 8, "сентябрь" to 8, "октября" to 9, "октябрь" to 9, "ноября" to 10, "ноябрь" to 10, "декабря" to 11, "декабрь" to 11)
    fun parse(source: String): Calendar? {
        val parsed = parseTripDateRange(source)?.first
        if (parsed != null) return calendarForTripDate(parsed)
        val iso = Regex("(\\d{4})-(\\d{2})-(\\d{2})").find(source)
        val russian = Regex("(\\d{1,2})\\s+(${russianMonths.keys.joinToString("|")})\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(source)
        return when {
            iso != null -> Calendar.getInstance().apply { clear(); set(iso.groupValues[1].toInt(), iso.groupValues[2].toInt() - 1, iso.groupValues[3].toInt()) }
            russian != null -> Calendar.getInstance().apply { clear(); set(russian.groupValues[3].toInt(), russianMonths[russian.groupValues[2].lowercase()] ?: 0, russian.groupValues[1].toInt()) }
            else -> null
        }
    }
    val legDate = parse(date)
    val calendar = legDate ?: parse(tripDates) ?: return "" to ""
    if (legDate == null) calendar.add(Calendar.DAY_OF_YEAR, dayIndex)
    return calendar.get(Calendar.DAY_OF_MONTH).toString() to months[calendar.get(Calendar.MONTH)]
}

internal fun routeDurationDays(dates: String): Int? {
    parseTripDateRange(dates)?.let { (start, end) ->
        return (ChronoUnit.DAYS.between(start, end).toInt() + 1).takeIf { it > 0 }
    }
    val matches = Regex("(\\d{4})-(\\d{2})-(\\d{2})").findAll(dates).toList()
    if (matches.size < 2) {
        return Regex("·\\s*(\\d+)\\s+дн", RegexOption.IGNORE_CASE).find(dates)?.groupValues?.get(1)?.toIntOrNull()
    }
    fun day(match: MatchResult): Long = Calendar.getInstance().apply { clear(); set(match.groupValues[1].toInt(), match.groupValues[2].toInt() - 1, match.groupValues[3].toInt()) }.timeInMillis / 86_400_000
    return (day(matches[1]) - day(matches[0]) + 1).toInt().takeIf { it > 0 }
}

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

internal fun accommodationDateParts(value: String): Pair<String, String> {
    val matches = Regex("\\d{4}-\\d{2}-\\d{2}").findAll(value).map { it.value }.toList()
    if (matches.size >= 2) return matches[0] to matches[1]
    val parts = value.trim().split(Regex("\\s+[–-]\\s+"))
    return (matches.firstOrNull() ?: parts.getOrNull(0).orEmpty()) to parts.getOrNull(1).orEmpty()
}

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

