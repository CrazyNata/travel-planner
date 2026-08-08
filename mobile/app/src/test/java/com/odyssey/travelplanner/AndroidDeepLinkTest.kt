package com.odyssey.travelplanner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AndroidDeepLinkTest {
    @Test
    fun `invite link carries trip id`() {
        assertEquals(
            AndroidDeepLink.Invite("trip-123"),
            parseAndroidDeepLink("https://travelplanner.muntim.ru/mobile/invite?tripId=trip-123"),
        )
    }

    @Test
    fun `reset link opens password flow`() {
        assertEquals(
            AndroidDeepLink.PasswordReset,
            parseAndroidDeepLink("https://travelplanner.muntim.ru/mobile/reset"),
        )
    }

    @Test
    fun `foreign hosts and missing trip ids are rejected`() {
        assertNull(parseAndroidDeepLink("https://example.com/mobile/reset"))
        assertNull(parseAndroidDeepLink("https://travelplanner.muntim.ru/mobile/invite"))
    }
}
