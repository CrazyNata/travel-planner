package com.odyssey.travelplanner.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingLogicTest {
    @Test
    fun onboardingUsesExactlyThreePagesAndClampsNavigation() {
        assertEquals(3, onboardingPageCount)
        assertEquals(0, normalizeOnboardingPage(-1))
        assertEquals(0, normalizeOnboardingPage(0))
        assertEquals(2, normalizeOnboardingPage(2))
        assertEquals(2, normalizeOnboardingPage(3))
    }

    @Test
    fun firstRunOnboardingOnlyShowsUntilItIsCompleted() {
        assertTrue(shouldShowFirstRunOnboarding(onboardingCompleted = false))
        assertFalse(shouldShowFirstRunOnboarding(onboardingCompleted = true))
    }

    @Test
    fun completionActionsUseHomeOrTheExistingCreateTripFlow() {
        assertEquals("trips", onboardingDestination(OnboardingExitAction.SKIP))
        assertEquals("trips", onboardingDestination(OnboardingExitAction.EXPLORE))
        assertEquals("create-trip", onboardingDestination(OnboardingExitAction.CREATE_FIRST_TRIP))
        assertEquals(
            "trip/trip-42",
            onboardingDestination(OnboardingExitAction.EXPLORE, pendingTripId = "trip-42"),
        )
        assertEquals(
            "reset-password",
            onboardingDestination(OnboardingExitAction.SKIP, pendingPasswordReset = true),
        )
    }

    @Test
    fun existingProfileOrTripMigratesAwayFromFirstRunOnboarding() {
        assertTrue(shouldMigrateExistingUserOnboarding(hasStoredAccountProfile = true, hasAnyTrip = false))
        assertTrue(shouldMigrateExistingUserOnboarding(hasStoredAccountProfile = false, hasAnyTrip = true))
        assertFalse(shouldMigrateExistingUserOnboarding(hasStoredAccountProfile = false, hasAnyTrip = false))
    }

    @Test
    fun createTripHintRequiresCompletedOnboardingAndAnEmptyHome() {
        assertTrue(
            shouldShowCreateTripHint(
                onboardingCompleted = true,
                hasAnyTrip = false,
                createTripHintSeen = false,
            ),
        )
        assertFalse(shouldShowCreateTripHint(false, false, false))
        assertFalse(shouldShowCreateTripHint(true, true, false))
        assertFalse(shouldShowCreateTripHint(true, false, true))
        assertFalse(shouldShowCreateTripHint(true, false, false, anotherHintVisible = true))
    }

    @Test
    fun addPlaceHintRequiresAnEmptyTripAndNoOtherHint() {
        assertTrue(
            shouldShowAddPlaceHint(
                onboardingCompleted = true,
                hasTrip = true,
                hasPlaces = false,
                addPlaceHintSeen = false,
            ),
        )
        assertFalse(shouldShowAddPlaceHint(false, true, false, false))
        assertFalse(shouldShowAddPlaceHint(true, false, false, false))
        assertFalse(shouldShowAddPlaceHint(true, true, true, false))
        assertFalse(shouldShowAddPlaceHint(true, true, false, true))
        assertFalse(shouldShowAddPlaceHint(true, true, false, false, anotherHintVisible = true))
    }
}
