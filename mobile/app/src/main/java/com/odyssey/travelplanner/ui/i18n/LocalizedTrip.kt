package com.odyssey.travelplanner.ui.i18n

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import com.odyssey.travelplanner.data.localizedCityCatalogName
import java.util.Locale
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.odyssey.travelplanner.ui.theme.LocalLanguage

@Composable
internal fun localizedBudgetScope(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
    "общий", "общее" -> localized("общий", "shared", "compartido", "gemeinsam")
    "семья" -> localized("семья", "family", "familia", "Familie")
    "личный" -> localized("личный", "personal", "personal", "privat")
    else -> value
}

@Composable
internal fun localizedCityFilter(value: String): String = if (value.trim().equals("Все города", ignoreCase = true)) {
    localized("Все города", "All cities", "Todas las ciudades", "Alle Städte")
} else {
    localizedCityName(value)
}

internal fun localizedCityName(value: String, language: String): String {
    val selectionParts = value.trim().split(" — ", limit = 2)
    if (selectionParts.size == 2) {
        val city = localizedCityCatalogName(selectionParts[0], language) ?: selectionParts[0]
        return "$city — ${selectionParts[1].trim()}"
    }
    val parts = value.trim().split(Regex("\\s*,\\s*"), limit = 2)
    val cityValue = parts.firstOrNull().orEmpty()
    val city = localizedCityCatalogName(cityValue, language) ?: cityValue
    if (parts.size == 1) return city
    val country = when (parts[1].trim().lowercase(Locale.ROOT)) {
        "италия", "italy", "italia", "italien" -> localized(language, "Италия", "Italy", "Italia", "Italien")
        "германия", "germany", "alemania", "deutschland" -> localized(language, "Германия", "Germany", "Alemania", "Deutschland")
        "австрия", "austria", "österreich", "osterreich" -> localized(language, "Австрия", "Austria", "Austria", "Österreich")
        "чехия", "czechia", "chequia", "tschechien" -> localized(language, "Чехия", "Czechia", "Chequia", "Tschechien")
        "латвия", "latvia", "letonia", "lettland" -> localized(language, "Латвия", "Latvia", "Letonia", "Lettland")
        "литва", "lithuania", "lituania", "litauen" -> localized(language, "Литва", "Lithuania", "Lituania", "Litauen")
        "эстония", "estonia", "estland" -> localized(language, "Эстония", "Estonia", "Estonia", "Estland")
        else -> parts[1].trim()
    }
    return "$city, $country"
}

@Composable
internal fun localizedCityName(value: String): String = localizedCityName(value, LocalLanguage.current)

internal fun localizedCityList(value: String, language: String): String {
    val separator = when {
        value.contains(" → ") -> " → "
        value.contains(" · ") -> " · "
        value.contains(",") -> ", "
        else -> return localizedCityName(value, language)
    }
    return value.split(separator).joinToString(separator) { localizedCityName(it, language) }
}

internal fun splitStoredCityList(value: String): List<String> {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return emptyList()
    return when {
        trimmed.contains(" · ") -> trimmed.split(" · ")
        trimmed.contains(" → ") -> trimmed.split(" → ")
        trimmed.count { it == ',' } >= 2 -> trimmed.split(",")
        else -> listOf(trimmed)
    }
}

@Composable
internal fun localizedTripStatus(value: String): String = when {
    value.contains("чернов", ignoreCase = true) -> localized("Черновик", "Draft", "Borrador", "Entwurf")
    value.contains("предст", ignoreCase = true) -> localized("Предстоящее", "Upcoming", "Próximo", "Bevorstehend")
    value.contains("заверш", ignoreCase = true) -> localized("Завершено", "Completed", "Completado", "Abgeschlossen")
    value.contains("прошед", ignoreCase = true) -> localized("Прошедшее", "Past", "Pasado", "Vergangen")
    else -> value
}

internal fun localizedTripDateText(value: String, language: String, multilineDuration: Boolean = false): String {
    if (value.isBlank()) return value
    val monthNames = when (normalizeLanguage(language)) {
        "EN" -> mapOf("января" to "Jan", "январь" to "Jan", "февраля" to "Feb", "февраль" to "Feb", "марта" to "Mar", "март" to "Mar", "апреля" to "Apr", "апрель" to "Apr", "мая" to "May", "май" to "May", "июня" to "Jun", "июнь" to "Jun", "июля" to "Jul", "июль" to "Jul", "августа" to "Aug", "август" to "Aug", "сентября" to "Sep", "сентябрь" to "Sep", "октября" to "Oct", "октябрь" to "Oct", "ноября" to "Nov", "ноябрь" to "Nov", "декабря" to "Dec", "декабрь" to "Dec")
        "ES" -> mapOf("января" to "ene", "январь" to "ene", "февраля" to "feb", "февраль" to "feb", "марта" to "mar", "март" to "mar", "апреля" to "abr", "апрель" to "abr", "мая" to "may", "май" to "may", "июня" to "jun", "июнь" to "jun", "июля" to "jul", "июль" to "jul", "августа" to "ago", "август" to "ago", "сентября" to "sep", "сентябрь" to "sep", "октября" to "oct", "октябрь" to "oct", "ноября" to "nov", "ноябрь" to "nov", "декабря" to "dic", "декабрь" to "dic")
        "DE" -> mapOf("января" to "Jan", "январь" to "Jan", "февраля" to "Feb", "февраль" to "Feb", "марта" to "Mär", "март" to "Mär", "апреля" to "Apr", "апрель" to "Apr", "мая" to "Mai", "май" to "Mai", "июня" to "Jun", "июнь" to "Jun", "июля" to "Jul", "июль" to "Jul", "августа" to "Aug", "август" to "Aug", "сентября" to "Sep", "сентябрь" to "Sep", "октября" to "Okt", "октябрь" to "Okt", "ноября" to "Nov", "ноябрь" to "Nov", "декабря" to "Dez", "декабрь" to "Dez")
        else -> mapOf("января" to "янв", "январь" to "янв", "февраля" to "фев", "февраль" to "фев", "марта" to "мар", "март" to "мар", "апреля" to "апр", "апрель" to "апр", "мая" to "май", "май" to "май", "июня" to "июн", "июнь" to "июн", "июля" to "июл", "июль" to "июл", "августа" to "авг", "август" to "авг", "сентября" to "сен", "сентябрь" to "сен", "октября" to "окт", "октябрь" to "окт", "ноября" to "ноя", "ноябрь" to "ноя", "декабря" to "дек", "декабрь" to "дек")
    }
    var result = value
    val monthPattern = Regex("(?i)(января|январь|февраля|февраль|марта|март|апреля|апрель|мая|май|июня|июнь|июля|июль|августа|август|сентября|сентябрь|октября|октябрь|ноября|ноябрь|декабря|декабрь)")
    result = monthPattern.replace(result) { match ->
        monthNames[match.value.lowercase(Locale.ROOT)] ?: match.value
    }
    fun durationWord(count: Int) = localizedCountWord(
        count,
        language,
        "день",
        "дня",
        "дней",
        "day",
        "days",
        "día",
        "días",
        "Tag",
        "Tage",
    )
    val isoDates = Regex("""\d{4}-\d{2}-\d{2}""").findAll(value)
        .mapNotNull { match -> runCatching { LocalDate.parse(match.value) }.getOrNull() }
        .toList()
    if (isoDates.isNotEmpty()) {
        val shortMonths = monthNames.values.toList().distinct()
        if (shortMonths.size >= 12) {
            fun formatIsoDate(date: LocalDate) = "${date.dayOfMonth} ${shortMonths[date.monthValue - 1]} ${date.year}"
            val formattedRange = isoDates.take(2).joinToString(" – ", transform = ::formatIsoDate)
            val duration = isoDates.takeIf { it.size >= 2 }?.let { dates ->
                ChronoUnit.DAYS.between(dates[0], dates[1]).toInt() + 1
            }
            val formatted = if (duration != null && duration > 0) {
                "$formattedRange · $duration ${durationWord(duration)}"
            } else {
                formattedRange
            }
            return if (multilineDuration) {
                formatted.replace(Regex("\\s+·\\s+"), " ·\n")
            } else {
                formatted
            }
        }
    }
    result = result.replace(Regex("(\\d+)\\s+дн(?:ей|я|ень)", RegexOption.IGNORE_CASE)) {
        val count = it.groupValues[1].toIntOrNull() ?: 0
        "${it.groupValues[1]} ${durationWord(count)}"
    }
    val dateJoiner = when (normalizeLanguage(language)) {
        "EN" -> " and "
        "ES" -> " y "
        "DE" -> " und "
        else -> " и "
    }
    result = result.replace(" и ", dateJoiner)
    return if (multilineDuration) {
        result.replace(Regex("\\s+·\\s+(\\d+\\s+\\S+)"), " ·\n$1")
    } else {
        result
    }
}

@Composable
internal fun localizedWeatherCondition(value: String): String {
    val normalized = value.trim().lowercase(Locale.ROOT)
    return when {
        normalized.contains("ясно") || normalized.contains("clear") -> localized("Ясно", "Clear", "Despejado", "Klar")
        normalized.contains("облачно") || normalized.contains("cloud") -> localized("Облачно", "Cloudy", "Nublado", "Bewölkt")
        normalized.contains("туман") || normalized.contains("fog") -> localized("Туман", "Fog", "Niebla", "Nebel")
        normalized.contains("морось") || normalized.contains("drizzle") -> localized("Морось", "Drizzle", "Llovizna", "Nieselregen")
        normalized.contains("дождь") || normalized.contains("rain") -> localized("Дождь", "Rain", "Lluvia", "Regen")
        normalized.contains("снег") || normalized.contains("snow") -> localized("Снег", "Snow", "Nieve", "Schnee")
        normalized.contains("гроза") || normalized.contains("thunder") -> localized("Гроза", "Thunderstorm", "Tormenta", "Gewitter")
        else -> value
    }
}

@Composable
internal fun localizedTripTitle(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
    "рождественская италия" -> localized("Рождественская Италия", "Christmas Italy", "Italia navideña", "Weihnachtliches Italien")
    "италия с семьёй", "италия с семьей" -> localized("Италия с семьёй", "Italy with family", "Italia en familia", "Italien mit Familie")
    else -> value
}

@Composable
internal fun localizedSightCategory(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
    "достопримечательности", "достопримечательность", "места", "место" -> localized("Достопримечательности", "Sights", "Lugares", "Sehenswürdigkeiten")
    "главная достопримечательность" -> localized("Главная достопримечательность", "Main sight", "Lugar principal", "Hauptsehenswürdigkeit")
    "природа" -> localized("Природа", "Nature", "Naturaleza", "Natur")
    else -> value
}

