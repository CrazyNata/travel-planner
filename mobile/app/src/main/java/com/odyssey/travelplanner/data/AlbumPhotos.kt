package com.odyssey.travelplanner.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.time.LocalDateTime

internal fun albumCaptureDate(value: String): LocalDateTime? {
    val match = Regex("^(\\d{4})[.\\-/:](\\d{1,2})[.\\-/:](\\d{1,2})(?:[T\\s·]+(\\d{1,2}):(\\d{2})(?::(\\d{2}))?)?").find(value.trim()) ?: return null
    return runCatching { LocalDateTime.of(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt(), match.groupValues[4].toIntOrNull() ?: 0, match.groupValues[5].toIntOrNull() ?: 0, match.groupValues[6].toIntOrNull() ?: 0) }.getOrNull()
}

internal suspend fun <T> albumAttempt(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (error: Exception) {
    if (error is kotlinx.coroutines.CancellationException) throw error
    Result.failure(error)
}

internal fun albumContentHash(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

internal fun albumPhotoItems(payload: JsonObject): List<JsonObject> =
    (payload["photos"]?.jsonArray.orEmpty() + payload["coverPhotos"]?.jsonArray.orEmpty())
        .mapNotNull { it as? JsonObject }
        .filter { photo -> tripPhotoPath(photo["image"]?.jsonPrimitive?.contentOrNull)
            ?.let { it.contains("/album/") || it.contains("/covers/") } == true }
        .distinctBy { canonicalTripPhotoReference(it["image"]?.jsonPrimitive?.contentOrNull) }

internal fun albumHash(photo: JsonObject): String? =
    photo["contentHash"]?.jsonPrimitive?.contentOrNull?.takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }
        ?: tripPhotoPath(photo["image"]?.jsonPrimitive?.contentOrNull)?.substringAfterLast('/')
            ?.substringBefore('.')?.takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }

internal fun duplicateAlbumPhoto(photos: List<JsonObject>, sourceHash: String, contentHash: String): JsonObject? {
    val hashes = setOf(sourceHash.lowercase(), contentHash.lowercase())
    return photos.firstOrNull { photo ->
        listOfNotNull(albumHash(photo), photo["sourceHash"]?.jsonPrimitive?.contentOrNull)
            .any { it.lowercase() in hashes }
    }
}

internal fun removedAlbumPhotoPatch(payload: JsonObject, photoId: String, reference: String): JsonObject {
    val target = canonicalTripPhotoReference(reference)
    require(target != null && tripPhotoPath(reference)?.let { it.contains("/album/") || it.contains("/covers/") } == true)
    require(albumPhotoItems(payload).any { photo ->
        canonicalTripPhotoReference(photo["image"]?.jsonPrimitive?.contentOrNull) == target &&
            (photo["id"]?.jsonPrimitive?.contentOrNull ?: photo["image"]?.jsonPrimitive?.contentOrNull) == photoId
    }) { "Фотография не найдена. Обновите альбом." }
    return buildJsonObject {
        for (section in listOf("photos", "coverPhotos")) {
            val photos = payload[section] as? JsonArray ?: continue
            val next = JsonArray(photos.filterNot { item ->
                val photo = item as? JsonObject ?: return@filterNot false
                canonicalTripPhotoReference(photo["image"]?.jsonPrimitive?.contentOrNull) == target
            })
            if (next != photos) put(section, next)
        }
    }
}

data class AlbumPhotoLinks(val thumbnail: String? = null, val original: String? = null)

data class PreparedAlbumPhoto(val bytes: ByteArray, val mimeType: String, val sourceHash: String)

private fun orientedBitmap(bitmap: Bitmap, bytes: ByteArray): Bitmap {
    val orientation = runCatching { ExifInterface(ByteArrayInputStream(bytes))
        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(1)
    val matrix = Matrix()
    when (orientation) {
        2 -> matrix.setScale(-1f, 1f)
        3 -> matrix.setRotate(180f)
        4 -> matrix.setScale(1f, -1f)
        5 -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
        6 -> matrix.setRotate(90f)
        7 -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
        8 -> matrix.setRotate(-90f)
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/** Preserve supported originals; convert HEIC/oversized images for all clients. */
suspend fun prepareAlbumPhoto(bytes: ByteArray, mimeType: String?): PreparedAlbumPhoto = withContext(Dispatchers.IO) {
    require(bytes.isNotEmpty()) { "Не удалось прочитать изображение" }
    val hash = albumContentHash(bytes)
    val type = when {
        bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() -> "image/jpeg"
        bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) -> "image/png"
        bytes.size >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
        else -> mimeType.orEmpty()
    }
    if (type in setOf("image/jpeg", "image/png", "image/webp") && bytes.size <= 10 * 1024 * 1024) {
        return@withContext PreparedAlbumPhoto(bytes, type, hash)
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Формат изображения не поддерживается" }
    bounds.inJustDecodeBounds = false
    bounds.inSampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / bounds.inSampleSize > 4096) bounds.inSampleSize *= 2
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds) ?: error("Не удалось прочитать изображение")
    val oriented = orientedBitmap(decoded, bytes)
    val output = ByteArrayOutputStream()
    try { check(oriented.compress(Bitmap.CompressFormat.JPEG, 88, output)) }
    finally { if (oriented !== decoded) oriented.recycle(); decoded.recycle() }
    val prepared = output.toByteArray()
    require(prepared.size <= 10 * 1024 * 1024) { "Фотография должна быть не больше 10 МБ" }
    PreparedAlbumPhoto(prepared, "image/jpeg", hash)
}

/** Uses only the current user's token; the Worker enforces trip access. */
class AlbumPhotoService(private val client: SupabaseClient) {
    private val endpoint = "https://ramingo-photos.road-guide-natasha7261.workers.dev"
    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS).callTimeout(120, TimeUnit.SECONDS).build()
    private val lookupHttp = http.newBuilder().readTimeout(15, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS).build()
    private val downloadHttp = http.newBuilder().readTimeout(30, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()

    suspend fun city(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        val lat = "%.3f".format(java.util.Locale.US, latitude)
        val lon = "%.3f".format(java.util.Locale.US, longitude)
        val request = Request.Builder().url("https://photon.komoot.io/reverse?lat=$lat&lon=$lon&limit=10&lang=en&layer=city&layer=locality&layer=county").header("Accept", "application/json").build()
        lookupHttp.newCall(request).execute().use {
            if (!it.isSuccessful) return@withContext null
            val root = Json.parseToJsonElement(it.body!!.string()).jsonObject
            val places = root["features"]?.jsonArray.orEmpty().mapNotNull { feature ->
                feature.jsonObject["properties"]?.jsonObject
            }
            places.firstNotNullOfOrNull { it["city"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank) }
                ?: places.firstNotNullOfOrNull { properties ->
                    listOf("locality", "town", "village", "municipality", "name", "county")
                        .firstNotNullOfOrNull { key -> properties[key]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank) }
                }
        }
    }

    suspend fun resolve(keys: List<String>): Map<String, AlbumPhotoLinks> = withContext(Dispatchers.IO) {
        if (keys.isEmpty()) return@withContext emptyMap()
        require(keys.size <= 12)
        val body = buildJsonObject { put("keys", buildJsonArray { keys.forEach { add(JsonPrimitive(it)) } }) }
        val response = request("$endpoint/resolve").post(body.toString().toRequestBody("application/json".toMediaType())).build()
        lookupHttp.newCall(response).execute().use {
            check(it.isSuccessful) { "Не удалось получить миниатюры (${it.code})" }
            Json.parseToJsonElement(it.body!!.string()).jsonObject.mapValues { (_, value) ->
                val photo = value.jsonObject
                AlbumPhotoLinks(photo["thumbnail"]?.jsonPrimitive?.contentOrNull, photo["original"]?.jsonPrimitive?.contentOrNull)
            }
        }
    }

    suspend fun mirror(key: String, bytes: ByteArray, mimeType: String) = withContext(Dispatchers.IO) {
        require(key.contains("/album/"))
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        require(options.outWidth > 0 && options.outHeight > 0) { "Не удалось прочитать изображение" }
        options.inJustDecodeBounds = false
        options.inSampleSize = 1
        while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 1440) options.inSampleSize *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: error("Не удалось создать миниатюру")
        val oriented = orientedBitmap(decoded, bytes)
        val scale = minOf(1f, 720f / maxOf(oriented.width, oriented.height))
        val preview = Bitmap.createScaledBitmap(oriented, (oriented.width * scale).toInt().coerceAtLeast(1), (oriented.height * scale).toInt().coerceAtLeast(1), true)
        val output = ByteArrayOutputStream()
        try { check(preview.compress(Bitmap.CompressFormat.JPEG, 72, output)) }
        finally { if (preview !== oriented) preview.recycle(); if (oriented !== decoded) oriented.recycle(); decoded.recycle() }
        val thumbnail = output.toByteArray()
        require(thumbnail.size <= 512 * 1024)
        put(key, "thumbnail", thumbnail, "image/jpeg")
        put(key, "original", bytes, mimeType)
    }

    suspend fun download(reference: String): ByteArray = withContext(Dispatchers.IO) {
        val url = client.resolveTripPhotoReference(reference) ?: error("Фотография недоступна")
        downloadHttp.newCall(Request.Builder().url(url).build()).execute().use {
            check(it.isSuccessful) { "Не удалось проверить ранее загруженное фото" }
            it.body?.bytes() ?: error("Фотография недоступна")
        }
    }

    private fun request(url: String): Request.Builder {
        val token = client.auth.currentAccessTokenOrNull() ?: throw AuthSessionRequiredException()
        return Request.Builder().url(url).header("Authorization", "Bearer $token")
    }

    private fun put(key: String, variant: String, bytes: ByteArray, mimeType: String) {
        val request = request("$endpoint/object/$variant/$key").put(bytes.toRequestBody(mimeType.toMediaType())).build()
        http.newCall(request).execute().use { check(it.isSuccessful) { "Не удалось сохранить копию фото (${it.code})" } }
    }
}
