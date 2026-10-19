package com.odyssey.travelplanner.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import java.time.LocalDateTime

class AlbumPhotosTest {
    @Test fun parsesWebAndNativeCaptureDatesAndRejectsInvalidDates() {
        val expected = LocalDateTime.of(2026, 10, 14, 18, 15, 22)
        assertEquals(expected, albumCaptureDate("2026-10-14T18:15:22+02:00"))
        assertEquals(expected, albumCaptureDate("2026-10-14 18:15:22"))
        assertEquals(expected, albumCaptureDate("2026.10.14 · 18:15:22"))
        assertEquals(expected, albumCaptureDate("2026:10:14 18:15:22"))
        assertEquals(LocalDateTime.of(2026, 1, 4, 0, 0), albumCaptureDate("2026/1/4"))
        assertNull(albumCaptureDate("2026-02-31T12:00"))
        assertNull(albumCaptureDate(""))
    }
    @Test fun includesBothWebAndNativeAlbumsWithoutCatalogPhotos() {
        val payload = Json.parseToJsonElement("""{"photos":[{"id":"web","image":"https://p.supabase.co/storage/v1/object/public/trip-photos/u/t/album/a.jpg"}],"coverPhotos":[{"id":"native","image":"storage://trip-photos/u/t/covers/b.jpg"},{"id":"catalog","image":"https://example.com/city.jpg"}]}""").jsonObject
        assertEquals(2, albumPhotoItems(payload).size)
    }

    @Test fun duplicateIsRecognizedAcrossUploadersAndFilenames() {
        val hash = albumContentHash("original".toByteArray())
        val photo = Json.parseToJsonElement("""{"image":"storage://trip-photos/other/trip/album/legacy.jpg","contentHash":"$hash"}""").jsonObject
        assertNotNull(duplicateAlbumPhoto(listOf(photo), hash, albumContentHash("prepared".toByteArray())))
        assertNull(duplicateAlbumPhoto(listOf(photo), albumContentHash("different".toByteArray()), albumContentHash("another".toByteArray())))
    }

    @Test fun hashFilenameMatchesWithoutStoredMetadata() {
        val hash = albumContentHash("photo".toByteArray())
        val photo = Json.parseToJsonElement("""{"image":"storage://trip-photos/u/t/album/$hash.jpg"}""").jsonObject
        assertEquals(hash, albumHash(photo))
    }

    @Test fun legacyAndSignedReferencesAreOnePhoto() {
        val payload = Json.parseToJsonElement("""{"photos":[{"image":"https://p.supabase.co/storage/v1/object/sign/trip-photos/u/t/album/a.jpg?token=x"}],"coverPhotos":[{"image":"storage://trip-photos/u/t/album/a.jpg"}]}""").jsonObject
        assertEquals(1, albumPhotoItems(payload).size)
    }

    @Test fun deletionRemovesOnlyTheSelectedImageAcrossLegacyRepresentations() {
        val payload = Json.parseToJsonElement("""{"photos":[{"id":"selected","image":"storage://trip-photos/u/t/album/a.jpg"},{"id":"keep","image":"storage://trip-photos/u/t/album/b.jpg"}],"coverPhotos":[{"id":"alias","image":"https://p.supabase.co/storage/v1/object/public/trip-photos/u/t/album/a.jpg"}],"title":"User title","days":[{"note":"User note"}]}""").jsonObject
        val patch = removedAlbumPhotoPatch(payload, "selected", "storage://trip-photos/u/t/album/a.jpg")
        val result = kotlinx.serialization.json.JsonObject(payload.toMutableMap().apply { putAll(patch) })
        assertEquals(payload["title"], result["title"])
        assertEquals(payload["days"], result["days"])
        assertEquals(1, albumPhotoItems(result).size)
        assertFailsWith<IllegalArgumentException> { removedAlbumPhotoPatch(payload, "unknown", "storage://trip-photos/u/t/album/a.jpg") }
    }
}
