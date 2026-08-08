package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TripPhotoStorageTest {
    @Test
    fun parsesCanonicalStorageReference() {
        val path = "user-id/trip-id/covers/photo.jpg"
        assertEquals(path, tripPhotoPath(storedTripPhotoReference(path)))
    }

    @Test
    fun parsesLegacyPublicUrlWithoutPersistingQueryParameters() {
        val url = "https://project.supabase.co/storage/v1/object/public/trip-photos/user/trip/photo%20one.jpg?download=1"
        assertEquals("user/trip/photo one.jpg", tripPhotoPath(url))
        assertEquals(
            "storage://trip-photos/user/trip/photo one.jpg",
            canonicalTripPhotoReference(url),
        )
    }

    @Test
    fun leavesExternalImagesUntouched() {
        val url = "https://images.example.com/city.jpg"
        assertNull(tripPhotoPath(url))
        assertEquals(url, canonicalTripPhotoReference(url))
    }
}
