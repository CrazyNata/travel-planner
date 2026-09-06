package com.odyssey.travelplanner.ui

import com.odyssey.travelplanner.notifications.ReminderPlanner
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

internal data class AccommodationDeadlineStatus(
    val deadline: LocalDate,
    val daysRemaining: Long,
)

internal fun accommodationDeadlineStatus(
    value: String,
    today: LocalDate = LocalDate.now(),
): AccommodationDeadlineStatus? {
    val deadline = ReminderPlanner.parseSingleDate(value) ?: return null
    return AccommodationDeadlineStatus(
        deadline = deadline,
        daysRemaining = ChronoUnit.DAYS.between(today, deadline),
    )
}

internal fun accommodationDeadlineCountdownLabel(daysRemaining: Long, language: String): String {
    if (daysRemaining < 0L) {
        return when (languageKey(language)) {
            "EN" -> "Cancellation ended"
            "ES" -> "Cancelación finalizada"
            "DE" -> "Stornierung beendet"
            else -> "Отмена уже недоступна"
        }
    }
    if (daysRemaining == 0L) {
        return when (languageKey(language)) {
            "EN" -> "Today"
            "ES" -> "Hoy"
            "DE" -> "Heute"
            else -> "Сегодня"
        }
    }
    val count = daysRemaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return when (languageKey(language)) {
        "EN" -> "$count ${if (count == 1) "day" else "days"} left"
        "ES" -> "Quedan $count ${if (count == 1) "día" else "días"}"
        "DE" -> "Noch $count ${if (count == 1) "Tag" else "Tage"}"
        else -> "Осталось $count ${russianDayWord(count)}"
    }
}

internal fun formatAccommodationDeadlineDetail(value: String, language: String): String {
    val deadline = ReminderPlanner.parseSingleDate(value) ?: return value.trim()
    val months = when (languageKey(language)) {
        "EN" -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "ES" -> listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        "DE" -> listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")
        else -> listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
    }
    val day = deadline.dayOfMonth
    val month = months[deadline.monthValue - 1]
    return when (languageKey(language)) {
        "EN" -> "$month $day, ${deadline.year}"
        "ES" -> "$day de $month de ${deadline.year}"
        "DE" -> "$day. $month ${deadline.year}"
        else -> "$day $month ${deadline.year}"
    }
}

internal fun accommodationDeadlineTimeLabel(language: String): String = when (languageKey(language)) {
    "EN" -> "23:59 local time"
    "ES" -> "23:59 hora local"
    "DE" -> "23:59 Ortszeit"
    else -> "23:59 по местному времени"
}

private fun languageKey(value: String): String = when (value.trim().uppercase(Locale.ROOT).substringBefore('-')) {
    "EN", "ENGLISH" -> "EN"
    "ES", "SPANISH" -> "ES"
    "DE", "GERMAN" -> "DE"
    else -> "RU"
}

private fun russianDayWord(value: Int): String = when {
    value % 100 in 11..14 -> "дней"
    value % 10 == 1 -> "день"
    value % 10 in 2..4 -> "дня"
    else -> "дней"
}
