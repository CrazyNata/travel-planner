package com.odyssey.travelplanner.ui.domain

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.odyssey.travelplanner.data.TripOverview
import com.odyssey.travelplanner.data.CoverPhoto
import com.odyssey.travelplanner.data.cityCatalogEntry
import java.util.Locale
import java.time.LocalDate
import com.odyssey.travelplanner.ui.i18n.normalizeLanguage
import com.odyssey.travelplanner.ui.screen.tripedit.parseTripDateRange

internal data class PhotoDateRange(val start: LocalDate, val end: LocalDate)

internal data class NewTripPhoto(
    val uri: Uri,
    val city: String = "",
)

internal val PhotoMonthNames = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")

internal fun photoCityKey(city: String): String = city.substringBefore(',').trim().lowercase(Locale.ROOT)

internal fun samePhotoCity(left: String, right: String): Boolean = photoCityKey(left) == photoCityKey(right)

internal fun coverPhotoForCity(photos: List<CoverPhoto>, city: String): CoverPhoto? {
    val targetKey = cityCatalogEntry(city)?.key
    return photos.firstOrNull { photo ->
        photo.city.isNotBlank() && (
            samePhotoCity(photo.city, city) ||
                (targetKey != null && cityCatalogEntry(photo.city)?.key == targetKey)
            )
    }
}

internal fun parsePhotoDateRange(value: String): PhotoDateRange? {
    return parseTripDateRange(value)?.let { (start, end) -> PhotoDateRange(start, end) }
}

internal fun parsePhotoTripStart(value: String): LocalDate? = parseTripDateRange(value)?.first

internal fun photoGroupDay(city: String, overview: TripOverview, fallback: Int): Int {
    val route = overview.routeLegs.withIndex().firstOrNull { (_, leg) ->
        samePhotoCity(leg.from, city) || samePhotoCity(leg.to, city)
    }
    val routeDay = route?.value?.dayId?.filter { it in '0'..'9' }?.toIntOrNull()
        ?.takeIf { it in 1..99 }
        ?: route?.index?.plus(1)
    val sightDay = overview.sights.filter { samePhotoCity(it.city, city) && it.walkDay > 0 }
        .minOfOrNull { it.walkDay }
    return routeDay ?: sightDay ?: fallback
}

internal fun photoGroupDateRange(city: String, overview: TripOverview, fallbackDay: Int): PhotoDateRange? {
    val stayRanges = overview.accommodations
        .filter { samePhotoCity(it.city, city) }
        .mapNotNull { parsePhotoDateRange(it.dates) }
    if (stayRanges.isNotEmpty()) {
        return PhotoDateRange(stayRanges.minOf { it.start }, stayRanges.maxOf { it.end })
    }
    val tripStart = parsePhotoTripStart(overview.dates) ?: return null
    val day = photoGroupDay(city, overview, fallbackDay).coerceAtLeast(1)
    val date = tripStart.plusDays((day - 1).toLong())
    return PhotoDateRange(date, date)
}

internal fun formatPhotoDateRange(range: PhotoDateRange, language: String): String {
    val monthNames = when (normalizeLanguage(language)) {
        "EN" -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "ES" -> listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        "DE" -> listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")
        else -> PhotoMonthNames
    }
    val startMonth = monthNames[range.start.monthValue - 1]
    val endMonth = monthNames[range.end.monthValue - 1]
    return when {
        range.start == range.end -> "${range.start.dayOfMonth} $startMonth"
        range.start.year == range.end.year && range.start.monthValue == range.end.monthValue -> "${range.start.dayOfMonth}–${range.end.dayOfMonth} $startMonth"
        else -> "${range.start.dayOfMonth} $startMonth – ${range.end.dayOfMonth} $endMonth"
    }
}

