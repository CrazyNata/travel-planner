package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationEmailTest {
    @Test
    fun normalizesRecipientEmailBeforeSaving() {
        assertEquals("person@example.com", normalizeNotificationEmail("  Person@Example.com "))
    }

    @Test
    fun rejectsInvalidRecipientEmail() {
        assertNull(normalizeNotificationEmail("not-an-email"))
        assertNull(normalizeNotificationEmail("person@example"))
        assertNull(normalizeNotificationEmail("person @example.com"))
    }
}
