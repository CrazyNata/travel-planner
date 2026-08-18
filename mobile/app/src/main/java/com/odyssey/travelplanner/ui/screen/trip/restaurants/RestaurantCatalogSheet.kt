package com.odyssey.travelplanner.ui.screen.trip.restaurants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.RestaurantCatalogEntry
import com.odyssey.travelplanner.data.RestaurantCatalogRepository
import io.github.jan.supabase.auth.providers.Google
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.cancellation.CancellationException
import java.util.Locale
import com.odyssey.travelplanner.ui.common.FullScreenPhotoViewer
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.i18n.localizedRestaurantCuisine
import com.odyssey.travelplanner.ui.screen.trip.sights.FastCatalogImage
import com.odyssey.travelplanner.ui.screen.trip.sights.restaurantPhotoLoadGate
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.secondarySurfaceColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun RestaurantCatalogSheet(
    city: String,
    onSelect: (RestaurantCatalogEntry) -> Unit,
    onClose: () -> Unit,
) {
    val language = LocalLanguage.current
    val uriHandler = LocalUriHandler.current
    val catalogRepository = remember { RestaurantCatalogRepository(SupabaseProvider.clientForCurrentAuthFlow()) }
    var query by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<RestaurantCatalogEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var liveRatingsAvailable by remember { mutableStateOf(false) }
    var fullScreenRestaurantPhoto by remember { mutableStateOf<Pair<String, String>?>(null) }
    var sortMenuOpen by remember(city) { mutableStateOf(false) }
    var sortMode by remember(city) { mutableStateOf("rating") }
    val sortRatingLabel = localized("По рейтингу", "By rating", "Por valoración", "Nach Bewertung")
    val sortPriceLabel = localized("По цене", "By price", "Por precio", "Nach Preis")
    val sortButtonLabel = localized("Сортировать", "Sort", "Ordenar", "Sortieren")
    val sortedEntries = when (sortMode) {
        "price" -> entries.sortedWith(
            compareBy<RestaurantCatalogEntry> { it.priceLevel ?: Int.MAX_VALUE }
                .thenByDescending { it.rating ?: -1.0 }
                .thenBy { it.sortOrder },
        )
        else -> entries.sortedWith(
            compareByDescending<RestaurantCatalogEntry> { it.rating ?: -1.0 }
                .thenByDescending { it.ratingCount ?: 0 }
                .thenBy { it.sortOrder },
        )
    }
    val liveRatingsUnavailableMessage = localized(
        "Рейтинг и фото Google временно недоступны — показан каталог ресторанов",
        "Google ratings and photos are temporarily unavailable — showing the restaurant catalog",
        "Las valoraciones y fotos de Google no están disponibles temporalmente — se muestra el catálogo",
        "Google-Bewertungen und Fotos sind vorübergehend nicht verfügbar — der Katalog wird angezeigt",
    )

    LaunchedEffect(city, query, language) {
        loading = true
        message = null
        delay(180)
        runCatching { catalogRepository.search(city = city, query = query) }
            .onSuccess { localEntries ->
                entries = localEntries
                liveRatingsAvailable = false
                loading = false
                val liveEntries = try {
                    catalogRepository.searchLiveRatings(city = city, query = query, language = language)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    message = liveRatingsUnavailableMessage
                    emptyList()
                }
                if (liveEntries.isNotEmpty()) {
                    entries = liveEntries
                    liveRatingsAvailable = true
                    val photoNames = liveEntries
                        .filter { it.photoUrl.isNullOrBlank() && it.photoName.isNotBlank() }
                        .map(RestaurantCatalogEntry::photoName)
                        .take(12)
                    if (photoNames.isNotEmpty()) {
                        val photos = try {
                            catalogRepository.resolveRestaurantPhotos(photoNames)
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Throwable) {
                            emptyMap()
                        }
                        if (photos.isNotEmpty()) {
                            entries = entries.map { entry ->
                                val photo = photos[entry.photoName]
                                if (photo == null) entry else entry.copy(
                                    photoUrl = photo.photoUrl ?: entry.photoUrl,
                                    photoAttribution = photo.photoAttribution ?: entry.photoAttribution,
                                )
                            }
                        }
                    }
                }
            }
            .onFailure { error ->
                if (error is CancellationException) throw error
                entries = emptyList()
                message = error.message ?: localized(language, "Не удалось загрузить каталог", "Could not load the restaurant catalog", "No se pudo cargar el catálogo", "Restaurantkatalog konnte nicht geladen werden")
            }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.94f)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    localized("Рестораны", "Restaurants", "Restaurantes", "Restaurants"),
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 22.sp,
                )
                Text(
                    localizedCityName(city),
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                    fontSize = 13.sp,
                )
            }
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(tintedSurfaceColor())
                        .clickable { sortMenuOpen = !sortMenuOpen }
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OdysseySortIcon()
                    Text(
                        text = sortButtonLabel,
                        color = primaryColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 12.sp,
                    )
                }
                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = { sortMenuOpen = false },
                    modifier = Modifier.width(228.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = cardSurfaceColor(),
                    shadowElevation = 14.dp,
                ) {
                    Text(
                        text = sortButtonLabel.uppercase(Locale.ROOT),
                        color = secondaryTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(sortRatingLabel, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp)
                                Text(localized("Сначала лучшие", "Best first", "Mejor valorados", "Beste zuerst"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 10.5.sp)
                            }
                        },
                        leadingIcon = { Text("★", color = Color(0xFFE29B32), fontSize = 18.sp) },
                        trailingIcon = if (sortMode == "rating") {
                            { Icon(Icons.Filled.Check, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(18.dp)) }
                        } else null,
                        onClick = {
                            sortMode = "rating"
                            sortMenuOpen = false
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(sortPriceLabel, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp)
                                Text(localized("Сначала доступные", "Most affordable first", "Más económicos primero", "Günstigste zuerst"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 10.5.sp)
                            }
                        },
                        leadingIcon = { Text("€", color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp) },
                        trailingIcon = if (sortMode == "price") {
                            { Icon(Icons.Filled.Check, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(18.dp)) }
                        } else null,
                        onClick = {
                            sortMode = "price"
                            sortMenuOpen = false
                        },
                    )
                }
            }
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(secondarySurfaceColor()).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = secondaryTextColor(), modifier = Modifier.size(18.dp))
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text(localized("Поиск по названию или кухне", "Search by name or cuisine", "Buscar por nombre o cocina", "Nach Name oder Küche suchen"), color = secondaryTextColor(), fontFamily = Manrope, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(20.dp)) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor(), unfocusedBorderColor = contentBorderColor()),
        )
        if (liveRatingsAvailable) {
            Text(
                text = localized("Рейтинг и фото из Google Maps", "Ratings and photos from Google Maps", "Valoraciones y fotos de Google Maps", "Bewertungen und Fotos aus Google Maps"),
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 10.sp,
            )
        }
        if (message != null) {
            Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        }
        if (loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor(), strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
            }
        } else if (entries.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    localized("Для этого города ресторанов пока нет", "No catalog restaurants for this city yet", "Todavía no hay restaurantes para esta ciudad", "Für diese Stadt gibt es noch keine Restaurants"),
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 22.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sortedEntries, key = { it.id }) { entry ->
                    if (entry.photoUrl.isNullOrBlank() && entry.photoName.isNotBlank()) {
                        LaunchedEffect(entry.id, entry.photoName) {
                            val photo = try {
                                restaurantPhotoLoadGate.withPermit {
                                    catalogRepository.resolveRestaurantPhoto(entry.photoName)
                                }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Throwable) {
                                null
                            }
                            val photoUrl = photo?.photoUrl
                            if (!photoUrl.isNullOrBlank()) {
                                entries = entries.map { current ->
                                    if (current.id == entry.id &&
                                        current.photoName == entry.photoName &&
                                        current.photoUrl.isNullOrBlank()
                                    ) {
                                        current.copy(
                                            photoUrl = photoUrl,
                                            photoAttribution = photo?.photoAttribution ?: current.photoAttribution,
                                        )
                                    } else {
                                        current
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(secondarySurfaceColor())
                            .border(1.dp, contentBorderColor(), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (entry.photoUrl.isNullOrBlank()) tintedSurfaceColor() else Color.Transparent)
                                .clickable(enabled = !entry.photoUrl.isNullOrBlank()) {
                                    entry.photoUrl?.let { photoUrl ->
                                        fullScreenRestaurantPhoto = photoUrl to entry.name(language)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!entry.photoUrl.isNullOrBlank()) {
                                FastCatalogImage(
                                    url = entry.photoUrl,
                                    contentDescription = entry.name(language),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Restaurant,
                                    contentDescription = null,
                                    tint = primaryColor(),
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.name(language), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            val details = listOf(localizedRestaurantCuisine(entry.cuisine), entry.address).filter(String::isNotBlank).joinToString(" · ")
                            if (details.isNotBlank()) {
                                Text(details, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.5.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                            }
                            if (entry.rating != null || entry.ratingCount != null || entry.ratingPlaceUrl.isNotBlank()) {
                                Row(
                                    modifier = Modifier.padding(top = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                if (entry.rating != null) {
                                    Text(
                                        text = "★ ${entry.rating.toString().removeSuffix(".0")}",
                                        color = Color(0xFFE29B32),
                                        fontFamily = Manrope,
                                        fontWeight = FontWeight.W800,
                                        fontSize = 11.5.sp,
                                    )
                                }
                                if (entry.ratingCount != null) {
                                    Text(
                                        text = localized("${entry.ratingCount} отзывов", "${entry.ratingCount} reviews", "${entry.ratingCount} reseñas", "${entry.ratingCount} Bewertungen"),
                                        color = secondaryTextColor(),
                                        fontFamily = Manrope,
                                        fontWeight = FontWeight.W600,
                                        fontSize = 10.5.sp,
                                    )
                                }
                                if (entry.ratingPlaceUrl.isNotBlank()) {
                                    Text(
                                        text = "Google Maps",
                                        color = primaryColor(),
                                        fontFamily = Manrope,
                                        fontWeight = FontWeight.W800,
                                        fontSize = 10.sp,
                                        modifier = Modifier.clickable { uriHandler.openUri(entry.ratingPlaceUrl) },
                                    )
                                }
                            }
                            }
                            if (!entry.photoAttribution.isNullOrBlank()) {
                                Text(
                                    text = "Google photo",
                                    color = secondaryTextColor(),
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W600,
                                    fontSize = 9.5.sp,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(secondarySurfaceColor())
                                .border(1.dp, contentBorderColor(), RoundedCornerShape(12.dp))
                                .clickable { onSelect(entry) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "+",
                                color = primaryColor(),
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W800,
                                fontSize = 24.sp,
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = "© OpenStreetMap contributors",
            color = secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W600,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        fullScreenRestaurantPhoto?.let { (photoUrl, title) ->
            FullScreenPhotoViewer(
                photos = listOf(photoUrl),
                initialIndex = 0,
                accommodationName = title,
                onDismiss = { fullScreenRestaurantPhoto = null },
            )
        }
    }
}

