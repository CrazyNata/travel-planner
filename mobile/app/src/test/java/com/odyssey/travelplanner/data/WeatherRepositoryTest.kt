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
}
