package com.odyssey.travelplanner.data

import java.time.LocalDate
import java.util.Locale
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private data class RouteDayEntry(
    val index: Int,
    val id: String,
    val day: JsonObject,
    val date: LocalDate?,
)

private val routeMonthIndices = mapOf(
    "января" to 1, "январь" to 1, "янв" to 1,
    "февраля" to 2, "февраль" to 2, "фев" to 2,
    "марта" to 3, "март" to 3, "мар" to 3,
    "апреля" to 4, "апрель" to 4, "апр" to 4,
    "мая" to 5, "май" to 5,
    "июня" to 6, "июнь" to 6, "июн" to 6,
    "июля" to 7, "июль" to 7, "июл" to 7,
    "августа" to 8, "август" to 8, "авг" to 8,
    "сентября" to 9, "сентябрь" to 9, "сен" to 9,
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
    "septiembre" to 9,
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

private fun text(element: JsonElement?): String =
    runCatching { element?.jsonPrimitive?.contentOrNull?.trim().orEmpty() }.getOrDefault("")

private fun parseRouteDate(value: String, fallbackYear: Int?): LocalDate? {
    val source = value.trim()
    if (source.isBlank()) return null

    Regex("(\\d{4})-(\\d{2})-(\\d{2})").find(source)?.let { match ->
        return runCatching {
            LocalDate.of(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
        }.getOrNull()
    }
    Regex("(\\d{1,2})[./](\\d{1,2})[./](\\d{4})").find(source)?.let { match ->
        return runCatching {
            LocalDate.of(match.groupValues[3].toInt(), match.groupValues[2].toInt(), match.groupValues[1].toInt())
        }.getOrNull()
    }

    val monthPattern = routeMonthIndices.keys.sortedByDescending(String::length).joinToString("|") { Regex.escape(it) }
    Regex("(\\d{1,2})\\s+($monthPattern)(?:\\s+(\\d{4}))?", RegexOption.IGNORE_CASE).find(source)?.let { match ->
        val month = routeMonthIndices[match.groupValues[2].lowercase(Locale.ROOT).removeSuffix(".")] ?: return null
        val year = match.groupValues[3].toIntOrNull() ?: fallbackYear ?: return null
        return runCatching { LocalDate.of(year, month, match.groupValues[1].toInt()) }.getOrNull()
    }
    return null
}

internal fun tripStartDate(payload: JsonObject): LocalDate? {
    val explicitStart = text(payload["startDate"])
    parseRouteDate(explicitStart, null)?.let { return it }
    val dates = text(payload["dates"])
    val year = Regex("\\d{4}").find(dates)?.value?.toIntOrNull()
    Regex("\\d{4}-\\d{2}-\\d{2}|\\d{1,2}[./]\\d{1,2}[./]\\d{4}|\\d{1,2}\\s+[\\p{L}.]+(?:\\s+\\d{4})?", RegexOption.IGNORE_CASE)
        .find(dates)
        ?.value
        ?.let { parseRouteDate(it, year) }
        ?.let { return it }
    return null
}

private fun routeDayEntries(days: JsonArray, startDate: LocalDate?): List<RouteDayEntry> =
    days.mapIndexedNotNull { index, element ->
        val day = element as? JsonObject ?: return@mapIndexedNotNull null
        val roadLeg = day["roadLeg"] as? JsonObject ?: return@mapIndexedNotNull null
        val id = text(day["id"]).ifBlank { "legacy-route-$index" }
        val legacyDay = text(roadLeg["dateDay"])
            .ifBlank { text(day["dateDay"]) }
            .ifBlank { text(roadLeg["day"]) }
        val legacyMonth = text(roadLeg["dateMonth"])
            .ifBlank { text(day["dateMonth"]) }
            .ifBlank { text(roadLeg["month"]) }
        val legacyDate = listOf(legacyDay, legacyMonth)
            .filter(String::isNotBlank)
            .joinToString(" ")
        val date = parseRouteDate(text(day["date"]), startDate?.year)
            ?: parseRouteDate(text(roadLeg["date"]), startDate?.year)
            ?: parseRouteDate(legacyDate, startDate?.year)
        RouteDayEntry(index = index, id = id, day = day, date = date)
    }

private fun RouteDayEntry.sortDate(startDate: LocalDate?): LocalDate? =
    date ?: startDate?.plusDays(index.toLong())

private fun displayOrder(entries: List<RouteDayEntry>, startDate: LocalDate?): List<RouteDayEntry> =
    entries.sortedWith(
        compareBy<RouteDayEntry> { it.sortDate(startDate) == null }
            .thenBy { it.sortDate(startDate) ?: LocalDate.MAX }
            .thenBy { it.index },
    )

internal fun routeDayIdsInDateOrder(days: JsonArray, startDate: LocalDate?): List<String> =
    displayOrder(routeDayEntries(days, startDate), startDate).map { it.id }

internal fun routeDayObjectsInDateOrder(days: JsonArray, startDate: LocalDate?): List<JsonObject> =
    displayOrder(routeDayEntries(days, startDate), startDate).map { it.day }

private fun normalizedRouteDay(day: JsonObject, date: LocalDate?, routePosition: Int): JsonObject {
    val roadLeg = day["roadLeg"] as? JsonObject ?: return day
    val nextRoadLeg = JsonObject(roadLeg.toMutableMap().apply {
        if (date != null) {
            // These labels are derived from the ISO date by each client and may
            // otherwise remain in the language/date of the device that saved it.
            remove("dateDay")
            remove("dateMonth")
            remove("weekday")
        }
    })
    return JsonObject(day.toMutableMap().apply {
        put("dayNumber", JsonPrimitive(routePosition + 1))
        date?.let { put("date", JsonPrimitive(it.toString())) }
        text(nextRoadLeg["to"]).takeIf(String::isNotBlank)?.let { put("city", JsonPrimitive(it)) }
        put("roadLeg", nextRoadLeg)
    })
}

/**
 * Rewrites only route-bearing day slots. Sight-only days and unknown payload
 * fields stay untouched. Date slots follow the current chronological order;
 * moving a card therefore moves its cities into the date where it was dropped.
 */
internal fun synchronizeRouteDayOrder(
    days: JsonArray,
    orderedRouteDayIds: List<String>,
    startDate: LocalDate?,
): JsonArray {
    val entries = routeDayEntries(days, startDate)
    if (entries.isEmpty()) return days

    val currentOrder = displayOrder(entries, startDate)
    val dateSlots = currentOrder.mapIndexed { index, entry ->
        entry.sortDate(startDate) ?: startDate?.plusDays(index.toLong())
    }
    val byId = entries.associateBy { it.id }
    val requested: List<RouteDayEntry> = buildList {
        orderedRouteDayIds.asSequence()
            .mapNotNull { byId[it] }
            .distinctBy { it.id }
            .forEach(::add)
        currentOrder.filterNot { entry -> this.contains(entry) }.forEach(::add)
    }
    if (requested.size != entries.size) return days

    val routeSlots = entries.map { it.index }.sorted()
    val nextDays = days.toMutableList()
    requested.forEachIndexed { routePosition, entry ->
        nextDays[routeSlots[routePosition]] = normalizedRouteDay(
            day = entry.day,
            date = dateSlots[routePosition],
            routePosition = routePosition,
        )
    }
    return JsonArray(nextDays)
}

internal fun insertRouteDayInDateOrder(
    days: JsonArray,
    newDay: JsonObject,
    startDate: LocalDate?,
): JsonArray {
    val nextDays = JsonArray(days + newDay)
    val orderedIds = routeDayIdsInDateOrder(nextDays, startDate)
    return synchronizeRouteDayOrder(nextDays, orderedIds, startDate)
}
