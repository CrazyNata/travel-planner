package com.odyssey.travelplanner.data

import java.time.LocalDate
import java.util.Locale

/**
 * Parses the date formats shared by the web and Android clients.
 *
 * The web client stores both ISO ranges and human-readable ranges such as
 * `25–27 сентября 2026 · 3 дня`. Some of those strings contain non-breaking
 * spaces, so normalize Unicode separators before applying the date patterns.
 */
internal fun parseTripDateRange(value: String): Pair<LocalDate, LocalDate>? {
    val normalizedValue = value
        .replace(Regex("[\\p{Z}\\p{Cf}]+"), " ")
        .trim()
    if (normalizedValue.isBlank()) return null

    val isoDates = Regex("(?<!\\d)(\\d{4})-(\\d{2})-(\\d{2})(?!\\d)")
        .findAll(normalizedValue)
        .mapNotNull { match ->
            runCatching {
                LocalDate.of(
                    match.groupValues[1].toInt(),
                    match.groupValues[2].toInt(),
                    match.groupValues[3].toInt(),
                )
            }.getOrNull()
        }
        .toList()
    if (isoDates.isNotEmpty()) {
        val start = isoDates.first()
        return start to isoDates.getOrElse(1) { start }
    }

    val dottedDates = Regex("(?<!\\d)(\\d{1,2})[./](\\d{1,2})[./](\\d{4})(?!\\d)")
        .findAll(normalizedValue)
        .mapNotNull { match ->
            runCatching {
                LocalDate.of(
                    match.groupValues[3].toInt(),
                    match.groupValues[2].toInt(),
                    match.groupValues[1].toInt(),
                )
            }.getOrNull()
        }
        .toList()
    if (dottedDates.isNotEmpty()) {
        val start = dottedDates.first()
        return start to dottedDates.getOrElse(1) { start }
    }

    val monthPattern = tripMonthNames.keys
        .sortedByDescending(String::length)
        .joinToString("|") { Regex.escape(it) }
    val compactHumanRange = Regex(
        "(?<!\\d)(\\d{1,2})\\s*[–—-]\\s*(\\d{1,2})\\s+($monthPattern)\\s+(\\d{4})(?!\\d)",
        RegexOption.IGNORE_CASE,
    ).find(normalizedValue)
    if (compactHumanRange != null) {
        val startDay = compactHumanRange.groupValues[1].toIntOrNull() ?: return null
        val endDay = compactHumanRange.groupValues[2].toIntOrNull() ?: return null
        val month = tripMonthNames[compactHumanRange.groupValues[3].lowercase(Locale.ROOT)] ?: return null
        val year = compactHumanRange.groupValues[4].toIntOrNull() ?: return null
        val start = runCatching { LocalDate.of(year, month, startDay) }.getOrNull() ?: return null
        val end = runCatching { LocalDate.of(year, month, endDay) }.getOrNull() ?: return null
        return start to end
    }

    val humanDates = Regex(
        "(?<!\\d)(\\d{1,2})\\s+($monthPattern)\\s+(\\d{4})(?!\\d)",
        RegexOption.IGNORE_CASE,
    ).findAll(normalizedValue)
        .mapNotNull { match ->
            val month = tripMonthNames[match.groupValues[2].lowercase(Locale.ROOT)] ?: return@mapNotNull null
            runCatching {
                LocalDate.of(match.groupValues[3].toInt(), month, match.groupValues[1].toInt())
            }.getOrNull()
        }
        .toList()
    val start = humanDates.firstOrNull() ?: return null
    return start to humanDates.getOrElse(1) { start }
}

private val tripMonthNames = mapOf(
    "января" to 1, "январь" to 1, "янв" to 1,
    "февраля" to 2, "февраль" to 2, "фев" to 2,
    "марта" to 3, "март" to 3, "мар" to 3,
    "апреля" to 4, "апрель" to 4, "апр" to 4,
    "мая" to 5, "май" to 5,
    "июня" to 6, "июнь" to 6, "июн" to 6,
    "июля" to 7, "июль" to 7, "июл" to 7,
    "августа" to 8, "август" to 8, "авг" to 8,
    "сентября" to 9, "сентябрь" to 9, "сен" to 9, "сент" to 9,
    "октября" to 10, "октябрь" to 10, "окт" to 10,
    "ноября" to 11, "ноябрь" to 11, "ноя" to 11,
    "декабря" to 12, "декабрь" to 12, "дек" to 12,
    "january" to 1, "jan" to 1,
    "february" to 2, "feb" to 2,
    "march" to 3, "mar" to 3,
    "april" to 4, "apr" to 4,
    "may" to 5,
    "june" to 6, "jun" to 6,
    "july" to 7, "jul" to 7,
    "august" to 8, "aug" to 8,
    "september" to 9, "sep" to 9,
    "october" to 10, "oct" to 10,
    "november" to 11, "nov" to 11,
    "december" to 12, "dec" to 12,
    "enero" to 1, "ene" to 1,
    "febrero" to 2,
    "marzo" to 3,
    "abril" to 4,
    "mayo" to 5,
    "junio" to 6,
    "julio" to 7,
    "agosto" to 8,
    "septiembre" to 9, "setiembre" to 9,
    "octubre" to 10,
    "noviembre" to 11,
    "diciembre" to 12,
    "januar" to 1,
    "februar" to 2,
    "märz" to 3, "maerz" to 3,
    "april" to 4,
    "mai" to 5,
    "juni" to 6,
    "juli" to 7,
    "august" to 8,
    "september" to 9,
    "oktober" to 10,
    "november" to 11,
    "dezember" to 12,
)
