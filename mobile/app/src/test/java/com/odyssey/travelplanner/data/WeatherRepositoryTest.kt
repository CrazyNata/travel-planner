package com.odyssey.travelplanner.data

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class WeatherRepositoryTest {
    @Test
    fun tripDateRangeParsesSharedWebCompactDateRange() {
        assertEquals(
            LocalDate.of(2026, 9, 25) to LocalDate.of(2026, 9, 27),
            tripDateRangeFrom("25–27 сентября 2026 · 3 дня"),
        )
    }

    @Test
    fun tripDateRangeParsesUnicodeSpacesFromStoredWebDates() {
        assertEquals(
            LocalDate.of(2026, 9, 25) to LocalDate.of(2026, 10, 12),
            tripDateRangeFrom("25\u00A0сентябрь\u00A02026 – 12\u00A0октябрь\u00A02026 · 18 дней"),
        )
    }

    @Test
    fun tripDateRangeParsesEnglishAndGermanHumanFormats() {
        assertEquals(
            LocalDate.of(2026, 9, 25) to LocalDate.of(2026, 9, 27),
            tripDateRangeFrom("25–27 September 2026"),
        )
        assertEquals(
            LocalDate.of(2026, 3, 25) to LocalDate.of(2026, 3, 27),
            tripDateRangeFrom("25–27 März 2026"),
        )
    }
}
