package com.odyssey.travelplanner.ui

/** The two supported entry points into the shared onboarding flow. */
enum class OnboardingMode {
    FIRST_RUN,
    REPLAY,
}

/** Actions available when the first-run onboarding is completed. */
enum class OnboardingExitAction {
    SKIP,
    CREATE_FIRST_TRIP,
    EXPLORE,
}

const val onboardingPageCount: Int = 3

fun normalizeOnboardingPage(index: Int): Int =
    index.coerceIn(0, onboardingPageCount - 1)

fun shouldShowFirstRunOnboarding(onboardingCompleted: Boolean): Boolean =
    !onboardingCompleted

fun onboardingDestination(
    action: OnboardingExitAction,
    pendingTripId: String? = null,
    pendingPasswordReset: Boolean = false,
): String = when {
    pendingPasswordReset -> "reset-password"
    !pendingTripId.isNullOrBlank() -> "trip/$pendingTripId"
    action == OnboardingExitAction.CREATE_FIRST_TRIP -> "create-trip"
    else -> "trips"
}

/**
 * A stored account profile or any existing trip is a server-side signal that
 * the account predates onboarding. This lets an upgrade bypass the new-user
 * flow without introducing another persistent store or a schema change.
 */
fun shouldMigrateExistingUserOnboarding(
    hasStoredAccountProfile: Boolean,
    hasAnyTrip: Boolean,
): Boolean = hasStoredAccountProfile || hasAnyTrip

fun shouldShowCreateTripHint(
    onboardingCompleted: Boolean,
    hasAnyTrip: Boolean,
    createTripHintSeen: Boolean,
    anotherHintVisible: Boolean = false,
): Boolean =
    onboardingCompleted &&
        !hasAnyTrip &&
        !createTripHintSeen &&
        !anotherHintVisible

fun shouldShowAddPlaceHint(
    onboardingCompleted: Boolean,
    hasTrip: Boolean,
    hasPlaces: Boolean,
    addPlaceHintSeen: Boolean,
    anotherHintVisible: Boolean = false,
): Boolean =
    onboardingCompleted &&
        hasTrip &&
        !hasPlaces &&
        !addPlaceHintSeen &&
        !anotherHintVisible
