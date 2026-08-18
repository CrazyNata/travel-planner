package com.odyssey.travelplanner.ui.screen.trip.sights

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.github.jan.supabase.auth.providers.Google
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.cancellation.CancellationException
import java.util.Locale
import com.odyssey.travelplanner.data.Sight
import com.odyssey.travelplanner.data.SightCatalogEntry
import com.odyssey.travelplanner.data.SightCatalogRepository
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyError
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondarySurfaceColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SightCatalogSheet(
    tripId: String,
    city: String,
    day: Int,
    existingSights: List<Sight>,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val language = LocalLanguage.current
    val scope = rememberCoroutineScope()
    val catalogRepository = remember { SightCatalogRepository(SupabaseProvider.clientForCurrentAuthFlow()) }
    var query by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<SightCatalogEntry>>(emptyList()) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var liveRatingsAvailable by remember { mutableStateOf(false) }
    val selectedEntries = entries.filter { it.id in selectedIds && !catalogSightAlreadyAdded(it, city, existingSights) }
    val liveRatingsUnavailableMessage = localized(
        "Фото и рейтинг достопримечательностей временно недоступны — показан базовый каталог",
        "Sight photos and ratings are temporarily unavailable — showing the base catalog",
        "Las fotos y valoraciones no están disponibles temporalmente — se muestra el catálogo base",
        "Fotos und Bewertungen sind vorübergehend nicht verfügbar — der Basiskatalog wird angezeigt",
    )

    LaunchedEffect(city, query, language) {
        loading = true
        message = null
        delay(180)
        runCatching { catalogRepository.searchWithLiveRatings(city = city, query = query, language = language) }
            .onSuccess { result ->
                entries = result.entries
                liveRatingsAvailable = result.liveRatingsAvailable
                if (!result.liveRatingsAvailable && result.entries.isNotEmpty()) {
                    message = liveRatingsUnavailableMessage
                }
                selectedIds = selectedIds.intersect(result.entries.map(SightCatalogEntry::id).toSet())
                if (result.liveRatingsAvailable) {
                    val photoNames = result.entries
                        .filter { it.photoUrl.isNullOrBlank() && it.photoName.isNotBlank() }
                        .map(SightCatalogEntry::photoName)
                        .take(12)
                    if (photoNames.isNotEmpty()) {
                        val photos = runCatching { catalogRepository.resolveSightPhotos(photoNames) }
                            .getOrElse { error ->
                                if (error is CancellationException) throw error
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
                liveRatingsAvailable = false
                message = error.message ?: localized(language, "Не удалось загрузить каталог", "Could not load the catalog", "No se pudo cargar el catálogo", "Katalog konnte nicht geladen werden")
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
                    localized("Достопримечательности", "Sights", "Lugares", "Sehenswürdigkeiten"),
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
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(secondarySurfaceColor()).clickable { onClose() },
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
            placeholder = { Text(localized("Поиск по названию", "Search by name", "Buscar por nombre", "Nach Namen suchen"), color = secondaryTextColor(), fontFamily = Manrope, fontSize = 13.sp) },
            leadingIcon = { Text("⌕", color = primaryColor(), fontSize = 23.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor(), unfocusedBorderColor = contentBorderColor()),
        )
        if (liveRatingsAvailable) {
            Text(
                localized("Фото и рейтинг из Google Maps", "Photos and ratings from Google Maps", "Fotos y valoraciones de Google Maps", "Fotos und Bewertungen aus Google Maps"),
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 10.sp,
            )
        }
        if (!loading && entries.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    localized("Популярные места", "Popular places", "Lugares populares", "Beliebte Orte"),
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 14.sp,
                )
                Text(
                    localized("${entries.size} мест", "${entries.size} places", "${entries.size} lugares", "${entries.size} Orte"),
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = 11.sp,
                )
            }
        }
        if (message != null) {
            Text(message!!, color = OdysseyError, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        }
        if (loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor(), strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
            }
        } else if (entries.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    localized("Для этого города пока нет мест в каталоге", "There are no catalog sights for this city yet", "Todavía no hay lugares para esta ciudad", "Für diese Stadt gibt es noch keine Orte"),
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
                items(entries, key = { it.id }) { entry ->
                    if (entry.photoUrl.isNullOrBlank() && entry.photoName.isNotBlank()) {
                        LaunchedEffect(entry.id, entry.photoName) {
                            val photo = try {
                                sightPhotoLoadGate.withPermit {
                                    catalogRepository.resolveSightPhoto(entry.photoName)
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
                    val alreadyAdded = catalogSightAlreadyAdded(entry, city, existingSights)
                    val selected = entry.id in selectedIds
                    val cardShape = RoundedCornerShape(18.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(if (selected) primaryColor().copy(alpha = 0.08f) else secondarySurfaceColor())
                            .border(1.dp, if (selected) primaryColor().copy(alpha = 0.52f) else contentBorderColor(), cardShape)
                            .clickable(enabled = !alreadyAdded && !saving) {
                                selectedIds = if (selected) selectedIds - entry.id else selectedIds + entry.id
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 96.dp, height = 112.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(tintedSurfaceColor()),
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
                                    Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = primaryColor().copy(alpha = 0.72f),
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                entry.category.ifBlank { localized("Достопримечательность", "Attraction", "Atracción", "Sehenswürdigkeit") }.uppercase(Locale.ROOT),
                                color = primaryColor(),
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W800,
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                entry.name(language),
                                color = contentTextColor(),
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W800,
                                fontSize = 15.sp,
                                lineHeight = 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            if (entry.rating != null || entry.ratingCount != null) {
                                Row(
                                    modifier = Modifier.padding(top = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text("★", color = Color(0xFFFFB52E), fontSize = 14.sp, fontWeight = FontWeight.W800)
                                    entry.rating?.let {
                                        Text(
                                            String.format(Locale.ROOT, "%.1f", it),
                                            color = contentTextColor(),
                                            fontFamily = Manrope,
                                            fontWeight = FontWeight.W800,
                                            fontSize = 11.5.sp,
                                        )
                                    }
                                    entry.ratingCount?.let {
                                        Text(
                                            "· ${catalogRatingCountLabel(it, language)}",
                                            color = secondaryTextColor(),
                                            fontFamily = Manrope,
                                            fontWeight = FontWeight.W600,
                                            fontSize = 10.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                            entry.description(language).takeIf(String::isNotBlank)?.let { description ->
                                Text(
                                    description,
                                    color = secondaryTextColor(),
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W600,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (alreadyAdded || selected) primaryColor() else tintedSurfaceColor()),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (alreadyAdded) {
                                Icon(Icons.Filled.Check, contentDescription = localized("Уже добавлено", "Already added", "Ya añadido", "Bereits hinzugefügt"), tint = primaryContentColor(), modifier = Modifier.size(18.dp))
                            } else {
                                Text(if (selected) "✓" else "+", color = if (selected) primaryContentColor() else primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 20.sp)
                            }
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
        Button(
            onClick = {
                scope.launch {
                    saving = true
                    message = null
                    runCatching {
                        SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).addCatalogSights(
                            id = tripId,
                            city = city,
                            language = language,
                            walkDay = day,
                            entries = selectedEntries,
                        )
                    }.onSuccess {
                        onSaved()
                        onClose()
                    }.onFailure { error ->
                        message = error.message ?: localized(language, "Не удалось добавить места", "Could not add sights", "No se pudieron añadir los lugares", "Orte konnten nicht hinzugefügt werden")
                    }
                    saving = false
                }
            },
            enabled = selectedEntries.isNotEmpty() && !saving,
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(bottom = 5.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                if (saving) localized("Добавляем…", "Adding…", "Añadiendo…", "Wird hinzugefügt…")
                else localized("Добавить выбранные", "Add selected", "Añadir seleccionados", "Auswahl hinzufügen") + if (selectedEntries.isNotEmpty()) " (${selectedEntries.size})" else "",
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
            )
        }
    }
}

internal fun catalogRatingCountLabel(count: Int, language: String): String {
    val formatted = String.format(Locale.ROOT, "%,d", count).replace(',', ' ')
    return when (language.trim().uppercase(Locale.ROOT).substringBefore('-')) {
        "EN" -> "$formatted reviews"
        "ES" -> "$formatted reseñas"
        "DE" -> "$formatted Bewertungen"
        else -> "$formatted отзывов"
    }
}

@Composable
internal fun FastCatalogImage(
    url: String,
    contentDescription: String?,
    contentScale: androidx.compose.ui.layout.ContentScale,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val request = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .size(480, 360)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}

