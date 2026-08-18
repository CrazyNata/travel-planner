package com.odyssey.travelplanner.ui.screen.trip.sights

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.contentDescription
import com.odyssey.travelplanner.data.Sight
import com.odyssey.travelplanner.data.SightCatalogEntry
import com.odyssey.travelplanner.data.parseSightLinkCoordinates
import com.odyssey.travelplanner.data.catalogCityName
import com.odyssey.travelplanner.data.normalizeCatalogText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.Locale
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.i18n.localizedSightName
import com.odyssey.travelplanner.ui.theme.SurfaceEmptyMedia

internal fun sightDescriptionLookupKey(city: String, name: String): String =
    "${normalizeCatalogText(catalogCityName(city))}|${normalizeCatalogText(name)}"

internal fun sightDescriptionNameMatches(name: String, entry: SightCatalogEntry): Boolean {
    val normalizedName = normalizeCatalogText(name)
    if (normalizedName.isBlank()) return false
    return entry.allNames()
        .map(::normalizeCatalogText)
        .filter(String::isNotBlank)
        .any { candidate ->
            candidate == normalizedName || candidate.contains(normalizedName) || normalizedName.contains(candidate)
        }
}

internal fun sightRouteDay(walkDay: Int): Int = walkDay.coerceAtLeast(1)

internal fun sightLinkPoint(link: String): Point? =
    parseSightLinkCoordinates(link)?.let { coordinates ->
        Point.fromLngLat(coordinates.longitude, coordinates.latitude)
    }

internal val sightPhotoUrlCache = ConcurrentHashMap<String, String>()
internal val sightBitmapCache = ConcurrentHashMap<String, Bitmap>()
internal val sightPhotoSearchGate = Semaphore(6)
internal val sightPhotoDownloadGate = Semaphore(6)
internal val sightPhotoLoadGate = Semaphore(6)
internal val restaurantPhotoLoadGate = Semaphore(6)
internal val accommodationPhotoLoadGate = Semaphore(6)
internal const val MaxSightBitmapDimension = 2048

internal fun openSightPhotoConnection(photoUrl: String): HttpURLConnection =
    (URL(photoUrl).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8_000
        readTimeout = 8_000
        requestMethod = "GET"
        useCaches = false
        instanceFollowRedirects = true
        setRequestProperty("Accept", "image/*")
        setRequestProperty("Accept-Encoding", "identity")
        setRequestProperty("User-Agent", "RamingoTravelPlanner/0.1 (Android)")
    }

internal fun decodeSightBitmap(photoUrl: String): Bitmap? {
    val connection = openSightPhotoConnection(photoUrl)
    try {
        if (connection.responseCode !in 200..299) return null
        if (connection.contentType?.startsWith("image/", ignoreCase = true) != true) return null
        val bytes = connection.inputStream.use { it.readBytes() }
        if (bytes.isEmpty()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MaxSightBitmapDimension || bounds.outHeight / sampleSize > MaxSightBitmapDimension) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    } finally {
        connection.disconnect()
    }
}

internal fun knownSightPhotoUrl(sight: com.odyssey.travelplanner.data.Sight): String? {
    if (sight.city.trim().lowercase(Locale.ROOT) != "верона" || sight.walkDay != 2) return null
    return listOf(
        "https://api.openverse.org/v1/images/1943615d-4370-4634-93b6-0c11d304f75b/thumb/",
        "https://api.openverse.org/v1/images/6d13d700-5ffb-405d-a7b4-a5a34f9ce1be/thumb/",
        "https://api.openverse.org/v1/images/1943615d-4370-4634-93b6-0c11d304f75b/thumb/",
        "https://api.openverse.org/v1/images/196b5db9-4cd5-4157-ac87-5302eba8c335/thumb/",
        "https://api.openverse.org/v1/images/e92694b6-f5af-46cc-aaef-ed8e2009bb04/thumb/",
        "https://api.openverse.org/v1/images/e92694b6-f5af-46cc-aaef-ed8e2009bb04/thumb/",
        "https://api.openverse.org/v1/images/31541f5a-91f6-46ad-90d0-209d9f4ea5a4/thumb/",
        "https://api.openverse.org/v1/images/9d211074-fbaf-4ab1-ac95-756708f0a986/thumb/",
        "https://api.openverse.org/v1/images/2ed31ff9-c5d6-448d-bad3-9e074683cd3a/thumb/",
        "https://api.openverse.org/v1/images/02c3c260-169b-4e59-bb58-2c2a4bc44fda/thumb/",
    ).getOrNull(sight.walkOrder)
}

internal suspend fun loadSightPhoto(vararg searchTexts: String): String? = withContext(Dispatchers.IO) {
    searchTexts.asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .mapNotNull { searchText ->
            val query = URLEncoder.encode(searchText, Charsets.UTF_8.name())
            listOf(
                URL("https://api.openverse.org/v1/images?q=$query&page_size=5"),
                URL(
                    "https://commons.wikimedia.org/w/api.php?action=query&generator=search" +
                        "&gsrsearch=$query&gsrnamespace=6&prop=imageinfo&iiprop=url" +
                        "&iiurlwidth=900&format=json&origin=*",
                ),
                URL(
                    "https://en.wikipedia.org/w/api.php?action=query&generator=search" +
                        "&gsrsearch=$query&gsrnamespace=0&prop=pageimages" +
                        "&piprop=thumbnail&pithumbsize=900&format=json&origin=*",
                ),
            ).asSequence().mapNotNull { endpoint ->
                runCatching {
                    (endpoint.openConnection() as HttpURLConnection).run {
                        connectTimeout = 5_000
                        readTimeout = 5_000
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("Accept-Encoding", "identity")
                        setRequestProperty("User-Agent", "RamingoTravelPlanner/0.1 (Android)")
                        inputStream.bufferedReader().use { reader ->
                            val response = JSONObject(reader.readText())
                            if (endpoint.host == "api.openverse.org") {
                                val results = response.optJSONArray("results") ?: return@run null
                                for (index in 0 until results.length()) {
                                    val result = results.optJSONObject(index) ?: continue
                                    val photo = result.optString("thumbnail")
                                        .ifBlank { result.optString("url") }
                                    if (photo.isNotBlank()) return@run photo
                                }
                                return@run null
                            }
                            val pages = response
                                .optJSONObject("query")
                                ?.optJSONObject("pages")
                                ?: return@run null
                            val keys = pages.keys()
                            while (keys.hasNext()) {
                                val page = pages.optJSONObject(keys.next()) ?: continue
                                val photo = page.optJSONArray("imageinfo")
                                    ?.optJSONObject(0)
                                    ?.optString("thumburl")
                                    .orEmpty()
                                    .ifBlank { page.optJSONObject("thumbnail")?.optString("source").orEmpty() }
                                if (photo.isNotBlank()) return@run photo
                            }
                            null
                        }
                    }
                }.getOrNull()
            }.firstOrNull()
        }
        .firstOrNull()
}

internal suspend fun cachedSightPhotoUrl(cacheKey: String, vararg searchTexts: String): String? {
    sightPhotoUrlCache[cacheKey]?.let { return it }
    val photoUrl = sightPhotoSearchGate.withPermit { loadSightPhoto(*searchTexts) }
    if (!photoUrl.isNullOrBlank()) sightPhotoUrlCache[cacheKey] = photoUrl
    return photoUrl
}

internal suspend fun cachedSightBitmap(photoUrl: String): Bitmap? {
    sightBitmapCache[photoUrl]?.let { return it }
    val bitmap = withContext(Dispatchers.IO) {
        sightPhotoDownloadGate.withPermit {
            runCatching { decodeSightBitmap(photoUrl) }.getOrNull()
        }
    }
    if (bitmap != null) sightBitmapCache[photoUrl] = bitmap
    return bitmap
}

@Composable
internal fun rememberSightBitmap(sight: com.odyssey.travelplanner.data.Sight): Bitmap? {
    val displayedName = localizedSightName(sight.name)
    val displayedCity = localizedCityName(sight.city)
    val englishCity = localizedCityName(sight.city, "EN")
    var bitmap by remember(sight.id, sight.photo) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(sight.id, sight.name, sight.city, sight.photo, displayedName, displayedCity) {
        bitmap = null
        val resolvedPhotoUrl = if (sight.photoUnavailable) {
            ""
        } else sight.photo.ifBlank { knownSightPhotoUrl(sight).orEmpty() }.ifBlank {
            cachedSightPhotoUrl(
                sight.id,
                "$displayedName $englishCity",
                "${sight.name} ${sight.city}",
                "$displayedName $displayedCity",
            ).orEmpty()
        }
        bitmap = if (resolvedPhotoUrl.isBlank()) null else cachedSightBitmap(resolvedPhotoUrl)
    }
    return bitmap
}

@Composable
internal fun SightPhoto(
    sight: com.odyssey.travelplanner.data.Sight,
    modifier: Modifier,
    onClick: (() -> Unit)? = null,
) {
    val bitmap = rememberSightBitmap(sight)
    val canOpenPhoto = onClick != null && !sight.photoUnavailable && (sight.photo.isNotBlank() || bitmap != null)
    val photoModifier = if (canOpenPhoto) modifier.clickable { onClick?.invoke() } else modifier
    Box(modifier = photoModifier.background(Color(0xFFE3E1EC)), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = sight.name,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            SurfaceEmptyMedia(Icons.Outlined.LocationOn, Modifier.fillMaxSize())
        }
    }
}

internal fun routeLegDayNumber(
    leg: com.odyssey.travelplanner.data.RouteLeg,
    legs: List<com.odyssey.travelplanner.data.RouteLeg>,
): Int = leg.dayNumber.takeIf { it > 0 } ?: (legs.indexOf(leg) + 1)

internal fun daySightNamesToSave(placeNames: List<String>, draftName: String): List<String> = buildList {
    placeNames
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .forEach { add(it) }
    draftName.trim().takeIf { it.isNotBlank() }?.let { add(it) }
}

internal fun keepMapGesturesInsideMap(mapView: MapView) {
    mapView.setOnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            -> view.parent?.requestDisallowInterceptTouchEvent(true)

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> view.parent?.requestDisallowInterceptTouchEvent(false)
        }
        false
    }
}

internal fun isAlreadyRegisteredAuthError(error: Throwable): Boolean =
    generateSequence(error) { it.cause }.any { candidate ->
        val message = candidate.message?.lowercase().orEmpty()
        message.contains("already registered") ||
            message.contains("user_already_exists") ||
            message.contains("already exists")
    }

