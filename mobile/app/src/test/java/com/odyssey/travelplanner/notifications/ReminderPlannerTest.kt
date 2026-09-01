package com.odyssey.travelplanner.notifications

import com.odyssey.travelplanner.data.Accommodation
import com.odyssey.travelplanner.data.TripCard
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReminderPlannerTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun plansTripAndFreeCancellationReminders() {
        val trip = TripCard(
            id = "trip-1",
            title = "Италия",
            dates = "2026-01-31 – 2026-02-07",
            status = "Черновик",
            progress = 10,
            cities = "Рим",
            coverImage = null,
            isOwner = true,
            accommodations = listOf(
                accommodation(
                    id = "stay-1",
                    name = "Casa Roma",
                    city = "Рим",
                    deadline = "2026-01-08",
                ),
            ),
        )

        val events = ReminderPlanner.plan(
            trips = listOf(trip),
            language = "RU",
            accountId = "user-1",
            now = ZonedDateTime.of(2026, 1, 1, 8, 0, 0, 0, zone).toInstant(),
            zone = zone,
        )

        assertEquals(9, events.size)
        assertEquals(5, events.count { it.kind == ReminderKind.TRIP })
        assertEquals(4, events.count { it.kind == ReminderKind.FREE_CANCELLATION })
        assertTrue(events.any { it.notificationText.contains("Италия") && it.notificationText.contains("30 дней") })
        assertTrue(events.any { it.notificationText.contains("Casa Roma") && it.notificationText.contains("7 дней") })
        assertTrue(events.all { it.accountId == "user-1" })
    }

    @Test
    fun skipsFinishedTripsAndStayedLodging() {
        val trip = TripCard(
            id = "trip-2",
            title = "Прошлая поездка",
            dates = "2026-02-01 – 2026-02-05",
            status = "Завершено",
            progress = 100,
            cities = "Рим",
            coverImage = null,
            isOwner = true,
            accommodations = listOf(accommodation("stay-2", "Отель", "Рим", "2026-01-08", "пожили")),
        )

        val events = ReminderPlanner.plan(
            trips = listOf(trip),
            language = "RU",
            now = Instant.parse("2026-01-01T08:00:00Z"),
            zone = zone,
        )

        assertTrue(events.isEmpty())
    }

    @Test
    fun supportsLegacyDateFormatsAndDropsPastTriggers() {
        assertEquals(
            java.time.LocalDate.of(2026, 9, 27),
            ReminderPlanner.parseSingleDate("27 сентября 2026"),
        )
        assertEquals(
            java.time.LocalDate.of(2026, 9, 27),
            ReminderPlanner.parseDateRange("27.09.2026 — 30.09.2026")?.first,
        )

        val trip = TripCard(
            id = "trip-3",
            title = "Сентябрь",
            dates = "2026-01-31 – 2026-02-02",
            status = "Предстоящее",
            progress = 0,
            cities = "Рим",
            coverImage = null,
            isOwner = true,
        )
        val events = ReminderPlanner.plan(
            trips = listOf(trip),
            language = "EN",
            now = Instant.parse("2026-01-01T10:00:00Z"),
            zone = zone,
        )

        assertFalse(events.any { it.daysRemaining == 30L })
        assertTrue(events.all { it.triggerAtMillis > Instant.parse("2026-01-01T10:00:00Z").toEpochMilli() })
    }

    private fun accommodation(
        id: String,
        name: String,
        city: String,
        deadline: String,
        status: String = "бронь",
    ) = Accommodation(
        id = id,
        city = city,
        name = name,
        dates = "",
        price = "",
        status = status,
        details = "",
        photos = emptyList(),
        bookingUrl = "",
        deadline = deadline,
    )
}
