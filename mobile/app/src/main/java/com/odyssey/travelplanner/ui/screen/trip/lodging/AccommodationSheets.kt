package com.odyssey.travelplanner.ui.screen.trip.lodging

import android.net.Uri
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.jan.supabase.auth.providers.Google
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.cancellation.CancellationException
import java.util.Locale
import java.net.URL
import com.odyssey.travelplanner.data.AccommodationCatalogEntry
import com.odyssey.travelplanner.data.AccommodationCatalogRepository
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.ui.common.FullScreenPhotoViewer
import com.odyssey.travelplanner.ui.domain.accommodationBookingSearchUrl
import com.odyssey.travelplanner.ui.domain.accommodationDateRange
import com.odyssey.travelplanner.ui.domain.accommodationPriceLevelLabel
import com.odyssey.travelplanner.ui.domain.cityFilterKey
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.icons.OdysseyPlusIcon
import com.odyssey.travelplanner.ui.screen.trip.route.accommodationDateParts
import com.odyssey.travelplanner.ui.screen.trip.sights.FastCatalogImage
import com.odyssey.travelplanner.ui.screen.trip.sights.accommodationPhotoLoadGate
import com.odyssey.travelplanner.ui.screen.trip.sights.catalogRatingCountLabel
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyError
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.OdysseyPurpleGradientEnd
import com.odyssey.travelplanner.ui.theme.OdysseyPurpleShadow
import com.odyssey.travelplanner.ui.theme.OdysseyScrimSoft
import com.odyssey.travelplanner.ui.theme.OdysseyWarningDeep
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondarySurfaceColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.surfaceVariantColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor

@Composable
internal fun AccommodationAddChoiceSheet(
    onManual: () -> Unit,
    onFromGoogle: () -> Unit,
    onClose: () -> Unit,
) {
    val language = LocalLanguage.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    localized("Добавить жильё", "Add lodging", "Añadir alojamiento", "Unterkunft hinzufügen"),
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 23.sp,
                )
                Text(
                    localized("Выберите способ добавления", "Choose how to add it", "Elige cómo añadirlo", "Hinzufügemethode wählen"),
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                    fontSize = 12.sp,
                )
            }
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(secondarySurfaceColor()).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = secondaryTextColor(), modifier = Modifier.size(18.dp))
            }
        }
        AccommodationAddChoiceOption(
            icon = { Icon(Icons.Outlined.Hotel, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(24.dp)) },
            title = localized("Найти в Google Places", "Find with Google Places", "Buscar con Google Places", "Mit Google Places suchen"),
            subtitle = localized("Отель, апартаменты, хостел или другой вариант проживания", "Hotel, apartment, hostel, or another stay", "Hotel, apartamento, hostal u otro alojamiento", "Hotel, Apartment, Hostel oder andere Unterkunft"),
            onClick = onFromGoogle,
        )
        AccommodationAddChoiceOption(
            icon = { Text("+", color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 28.sp) },
            title = localized("Добавить вручную", "Add manually", "Añadir manualmente", "Manuell hinzufügen"),
            subtitle = localized("Сохраните жильё и свою ссылку на бронирование", "Save a stay and your own booking link", "Guarda el alojamiento y tu enlace de reserva", "Unterkunft und eigenen Buchungslink speichern"),
            onClick = onManual,
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
internal fun AccommodationAddChoiceOption(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(secondarySurfaceColor())
            .border(1.dp, contentBorderColor(), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(tintedSurfaceColor()),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 15.sp)
            Text(subtitle, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.5.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(19.dp))
    }
}

@Composable
internal fun AccommodationCatalogSheet(
    city: String,
    cityOptions: List<String>,
    onCityChange: (String) -> Unit,
    onSelect: (AccommodationCatalogEntry) -> Unit,
    onClose: () -> Unit,
) {
    val language = LocalLanguage.current
    val repository = remember { AccommodationCatalogRepository(SupabaseProvider.clientForCurrentAuthFlow()) }
    var query by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<AccommodationCatalogEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var fullScreenPhoto by remember { mutableStateOf<Pair<String, String>?>(null) }
    val visibleCities = (cityOptions + city).filter(String::isNotBlank).distinctBy(::cityFilterKey)
    val sortedEntries = entries.sortedWith(
        compareByDescending<AccommodationCatalogEntry> { it.rating ?: -1.0 }
            .thenByDescending { it.reviewCount ?: 0 }
            .thenBy { it.name.lowercase(Locale.ROOT) },
    )

    LaunchedEffect(city, query, language) {
        loading = true
        message = null
        delay(180)
        runCatching { repository.search(city = city, query = query, language = language) }
            .onSuccess { entries = it }
            .onFailure { error ->
                if (error is CancellationException) throw error
                entries = emptyList()
                message = error.message ?: localized(language, "Не удалось загрузить жильё", "Could not load lodging", "No se pudo cargar el alojamiento", "Unterkünfte konnten nicht geladen werden")
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
                Text(localized("Жильё", "Lodging", "Alojamiento", "Unterkünfte"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 22.sp)
                Text(localized("Поиск в выбранном городе", "Search in the selected city", "Buscar en la ciudad seleccionada", "In der ausgewählten Stadt suchen"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp)
            }
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(secondarySurfaceColor()).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = secondaryTextColor(), modifier = Modifier.size(18.dp))
            }
        }
        if (visibleCities.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                visibleCities.forEach { option ->
                    val active = cityFilterKey(option) == cityFilterKey(city)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (active) primaryColor() else tintedSurfaceColor())
                            .clickable { onCityChange(option) }
                            .padding(horizontal = 13.dp, vertical = 8.dp),
                    ) {
                        Text(localizedCityName(option), color = if (active) primaryContentColor() else primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.5.sp)
                    }
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text(localized("Название, район или тип жилья", "Name, area, or lodging type", "Nombre, zona o tipo de alojamiento", "Name, Gegend oder Unterkunftstyp"), color = secondaryTextColor(), fontFamily = Manrope, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Outlined.Hotel, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(20.dp)) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor(), unfocusedBorderColor = contentBorderColor()),
        )
        Text(localized("Рейтинг и фотографии из Google Places", "Ratings and photos from Google Places", "Valoraciones y fotos de Google Places", "Bewertungen und Fotos aus Google Places"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 10.sp)
        if (message != null) {
            Text(message!!, color = OdysseyError, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        }
        if (loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor(), strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
            }
        } else if (sortedEntries.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(localized("В этом городе жильё не найдено", "No lodging found in this city", "No se encontró alojamiento en esta ciudad", "Keine Unterkunft in dieser Stadt gefunden"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sortedEntries, key = { it.id }) { entry ->
                    if (entry.photoUrl.isNullOrBlank() && entry.photoName.isNotBlank()) {
                        LaunchedEffect(entry.id, entry.photoName) {
                            val photo = try {
                                accommodationPhotoLoadGate.withPermit { repository.resolvePhoto(entry.photoName) }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Throwable) {
                                null
                            }
                            if (!photo?.photoUrl.isNullOrBlank()) {
                                entries = entries.map { current ->
                                    if (current.id == entry.id && current.photoUrl.isNullOrBlank()) current.copy(photoUrl = photo?.photoUrl, photoAttribution = photo?.photoAttribution ?: current.photoAttribution) else current
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
                            .clickable { onSelect(entry) }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.size(74.dp).clip(RoundedCornerShape(11.dp)).background(if (entry.photoUrl.isNullOrBlank()) tintedSurfaceColor() else Color.Transparent), contentAlignment = Alignment.Center) {
                            if (!entry.photoUrl.isNullOrBlank()) {
                                FastCatalogImage(entry.photoUrl!!, entry.name, androidx.compose.ui.layout.ContentScale.Crop, Modifier.fillMaxSize())
                            } else {
                                Icon(Icons.Outlined.Hotel, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.name, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            val addressAndType = listOf(entry.type, entry.address).filter(String::isNotBlank).joinToString(" · ")
                            if (addressAndType.isNotBlank()) Text(addressAndType, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.5.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                            if (entry.rating != null || entry.reviewCount != null) {
                                Row(modifier = Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    entry.rating?.let { Text("★ ${it.toString().removeSuffix(".0")}", color = OdysseyWarningDeep, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.5.sp) }
                                    entry.reviewCount?.let { Text(catalogRatingCountLabel(it, language), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 10.5.sp) }
                                }
                            }
                        }
                        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
        Text("Google Places", color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        fullScreenPhoto?.let { (photoUrl, title) ->
            FullScreenPhotoViewer(photos = listOf(photoUrl), initialIndex = 0, accommodationName = title, onDismiss = { fullScreenPhoto = null })
        }
    }
}

@Composable
internal fun AccommodationPlaceDetailsSheet(
    place: AccommodationCatalogEntry,
    tripId: String,
    tripCityId: String,
    tripDates: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val language = LocalLanguage.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val repository = remember { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()) }
    val catalogRepository = remember { AccommodationCatalogRepository(SupabaseProvider.clientForCurrentAuthFlow()) }
    var bookingUrl by remember(place.id) { mutableStateOf("") }
    var checkIn by remember(place.id) { mutableStateOf(accommodationDateParts(tripDates).first) }
    var checkOut by remember(place.id) { mutableStateOf(accommodationDateParts(tripDates).second) }
    var datePickerTarget by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var photoUrls by remember(place.id) { mutableStateOf(listOfNotNull(place.photoUrl)) }
    var fullScreenPhotoIndex by remember { mutableStateOf<Int?>(null) }
    val photoNames = place.photoNames.ifEmpty { listOfNotNull(place.photoName.takeIf(String::isNotBlank)) }.distinct().take(5)

    LaunchedEffect(place.id, photoNames) {
        photoNames.forEach { photoName ->
            if (photoUrls.size >= 5) return@forEach
            val photo = try {
                accommodationPhotoLoadGate.withPermit { catalogRepository.resolvePhoto(photoName) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                null
            }
            photo?.photoUrl?.takeIf(String::isNotBlank)?.let { url ->
                photoUrls = (photoUrls + url).distinct().take(5)
            }
        }
    }
    val bookingTarget = bookingUrl.trim().takeIf(String::isNotBlank)
        ?: place.website.trim().takeIf(String::isNotBlank)
        ?: accommodationBookingSearchUrl(place.name, place.city)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.94f)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(localized("Карточка жилья", "Lodging details", "Detalles del alojamiento", "Unterkunftsdetails"), color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp)
                Text(place.name, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 23.sp, lineHeight = 27.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(secondarySurfaceColor()).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = secondaryTextColor(), modifier = Modifier.size(18.dp))
            }
        }
        if (photoUrls.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                photoUrls.forEachIndexed { index, url ->
                    AsyncImage(model = url, contentDescription = place.name, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(142.dp, 112.dp).clip(RoundedCornerShape(15.dp)).clickable { fullScreenPhotoIndex = index })
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(15.dp)).background(tintedSurfaceColor()), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Hotel, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(34.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            place.rating?.let { Text("★ ${it.toString().removeSuffix(".0")}", color = OdysseyWarningDeep, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp) }
            place.reviewCount?.let { Text(catalogRatingCountLabel(it, language), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp) }
            if (place.type.isNotBlank()) Text(place.type, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (place.address.isNotBlank()) AccommodationInfoRow(localized("Адрес", "Address", "Dirección", "Adresse"), place.address)
        place.latitude?.let { latitude ->
            val coordinates = listOfNotNull(latitude, place.longitude).joinToString(", ")
            if (coordinates.isNotBlank()) AccommodationInfoRow(localized("Координаты", "Coordinates", "Coordenadas", "Koordinaten"), coordinates)
        }
        if (place.phone.isNotBlank()) AccommodationInfoRow(localized("Телефон", "Phone", "Teléfono", "Telefon"), place.phone, onClick = { uriHandler.openUri("tel:${place.phone}") })
        if (place.website.isNotBlank()) AccommodationInfoRow(localized("Сайт объекта", "Property website", "Sitio del alojamiento", "Website der Unterkunft"), place.website, onClick = { uriHandler.openUri(place.website) })
        if (place.googleMapsUrl.isNotBlank()) AccommodationInfoRow(localized("Google Maps", "Google Maps", "Google Maps", "Google Maps"), localized("Открыть карту", "Open map", "Abrir mapa", "Karte öffnen"), onClick = { uriHandler.openUri(place.googleMapsUrl) })

        Text(localized("Бронирование", "Booking", "Reserva", "Buchung"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 15.sp, modifier = Modifier.padding(top = 3.dp))
        Text(localized("Можно открыть сайт объекта или сохранить свою ссылку Booking, Airbnb и другого сервиса.", "Open the property site or save your own Booking, Airbnb, or other link.", "Abre el sitio del alojamiento o guarda tu propio enlace de Booking, Airbnb u otro servicio.", "Öffnen Sie die Website der Unterkunft oder speichern Sie Ihren eigenen Booking-, Airbnb- oder anderen Link."), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.5.sp, lineHeight = 15.sp)
        OutlinedTextField(
            value = bookingUrl,
            onValueChange = { bookingUrl = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            label = { Text(localized("Ссылка на бронирование", "Booking URL", "Enlace de reserva", "Buchungslink"), fontFamily = Manrope) },
            placeholder = { Text("https://...", color = secondaryTextColor()) },
            leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(19.dp)) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor(), unfocusedBorderColor = contentBorderColor()),
        )
        Button(onClick = { uriHandler.openUri(bookingTarget) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor())) {
            Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(localized("Посмотреть цены / Забронировать", "View prices / Book", "Ver precios / Reservar", "Preise ansehen / Buchen"), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp)
        }
        Text(localized("Даты проживания", "Stay dates", "Fechas de estancia", "Aufenthaltsdaten"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 15.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AccommodationEditDateField(label = localized("Заезд", "Check-in", "Entrada", "Anreise"), value = checkIn, scale = 1f, modifier = Modifier.weight(1f), onClick = { datePickerTarget = "checkIn" })
            AccommodationEditDateField(label = localized("Выезд", "Check-out", "Salida", "Abreise"), value = checkOut, scale = 1f, modifier = Modifier.weight(1f), onClick = { datePickerTarget = "checkOut" })
        }
        if (message != null) Text(message!!, color = OdysseyError, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        Button(
            onClick = {
                scope.launch {
                    saving = true
                    message = null
                    runCatching {
                        repository.addAccommodationDetails(
                            com.odyssey.travelplanner.data.AccommodationInput(
                                name = place.name,
                                city = place.city,
                                dates = accommodationDateRange(checkIn, checkOut, tripDates),
                                price = accommodationPriceLevelLabel(place.priceLevel),
                                status = "хочу",
                                details = place.address,
                                bookingUrl = bookingUrl.trim(),
                                source = place.source,
                                googlePlaceId = place.placeId,
                                externalUrl = bookingUrl.trim(),
                                address = place.address,
                                latitude = place.latitude,
                                longitude = place.longitude,
                                rating = place.rating,
                                reviewCount = place.reviewCount,
                                photoReference = place.photoName,
                                website = place.website,
                                phone = place.phone,
                                type = place.type,
                                tripCityId = tripCityId.trim(),
                            ),
                            tripId,
                        )
                    }.onSuccess {
                        onSaved()
                    }.onFailure { error ->
                        message = error.message ?: localized(language, "Не удалось сохранить жильё", "Could not save lodging", "No se pudo guardar el alojamiento", "Unterkunft konnte nicht gespeichert werden")
                    }
                    saving = false
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()),
        ) {
            Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Добавить в поездку", "Add to trip", "Añadir al viaje", "Zur Reise hinzufügen"), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
        }
        if (place.photoAttribution?.isNotBlank() == true) Text(place.photoAttribution!!, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 9.5.sp)
    }
    fullScreenPhotoIndex?.let { initialIndex ->
        FullScreenPhotoViewer(photos = photoUrls, initialIndex = initialIndex, accommodationName = place.name, onDismiss = { fullScreenPhotoIndex = null })
    }
    datePickerTarget?.let { target ->
        AccommodationCalendarDialog(
            initialValue = if (target == "checkIn") checkIn else checkOut,
            onDismiss = { datePickerTarget = null },
            onConfirm = { selected ->
                if (target == "checkIn") checkIn = selected else checkOut = selected
                datePickerTarget = null
            },
        )
    }
}

@Composable
internal fun AccommodationInfoRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(secondarySurfaceColor())
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp, modifier = Modifier.width(92.dp))
        Text(value, color = if (onClick != null) primaryColor() else contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun AccommodationAddSheet(
    name: String,
    city: String,
    checkIn: String,
    checkOut: String,
    deadline: String,
    price: String,
    bookingUrl: String,
    details: String,
    status: String,
    photoUri: Uri?,
    saving: Boolean,
    message: String?,
    onNameChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    onDeadlineClick: () -> Unit,
    onPriceChange: (String) -> Unit,
    onBookingUrlChange: (String) -> Unit,
    onDetailsChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = maxWidth.value / 368f
        fun d(value: Float) = (value * scale).dp
        fun s(value: Float) = (value * scale).sp
        val scrollState = rememberScrollState()
        val labelStyle = androidx.compose.ui.text.TextStyle(color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = s(13f), lineHeight = s(18f), platformStyle = OdysseyNoFontPadding)
        val photoBorderColor = contentBorderColor()
        val photoScrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d(720f))
                .verticalScroll(scrollState),
        ) {
            Box(Modifier.fillMaxWidth().height(d(1102f))) {
                Box(
                    modifier = Modifier
                        .offset(x = d(164f), y = d(12f))
                        .size(d(40f), d(4f))
                        .clip(RoundedCornerShape(d(2f)))
                        .background(contentBorderColor()),
                )
                Text(
                    text = localized("Новое жильё", "New lodging", "Nuevo alojamiento", "Neue Unterkunft"),
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = s(24f),
                    lineHeight = s(33f),
                    style = androidx.compose.ui.text.TextStyle(
                        letterSpacing = s(-0.24f),
                        platformStyle = OdysseyNoFontPadding,
                    ),
                    modifier = Modifier.offset(x = d(16f), y = d(30f)).width(d(260f)).height(d(34f)),
                    maxLines = 1,
                    softWrap = false,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(x = d(303f), y = d(30f))
                        .size(d(34f))
                        .clip(CircleShape)
                        .background(surfaceVariantColor())
                        .clickable(onClick = onClose),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"),
                        tint = secondaryTextColor(),
                        modifier = Modifier.size(d(16f)),
                    )
                }

                Text(text = localized("Фотографии", "Photos", "Fotos", "Fotos"), style = labelStyle, modifier = Modifier.offset(x = d(16f), y = d(82f)).width(d(321f)).height(d(18f)))
                Box(
                    modifier = Modifier
                        .offset(x = d(16f), y = d(108f))
                        .width(d(321f))
                        .height(d(172f))
                        .horizontalScroll(photoScrollState),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(d(10f)), modifier = Modifier.width(d(674f)).height(d(168f))) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(d(240f))
                                .height(d(168f))
                                .clip(RoundedCornerShape(d(16f)))
                                .background(surfaceVariantColor())
                                .drawBehind {
                                    val stroke = d(1f).toPx()
                                    drawRoundRect(color = photoBorderColor, topLeft = Offset(stroke / 2f, stroke / 2f), size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke), cornerRadius = CornerRadius(d(16f).toPx() - stroke / 2f), style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(d(6f).toPx(), d(4f).toPx()))))
                                }
                                .clickable(onClick = onPickPhoto),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Image, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(d(26f)))
                                Text(text = localized("Обложка — перетащите фото\nили выберите файл", "Cover — drag a photo\nor choose a file", "Portada — arrastre una foto\no elija un archivo", "Cover — Foto ziehen\noder Datei auswählen"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = s(11.5f), lineHeight = s(17f), textAlign = TextAlign.Center, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.padding(top = d(6f)))
                            }
                        }
                        Box(modifier = Modifier.width(d(128f)).height(d(168f)).clip(RoundedCornerShape(d(14f))).background(secondarySurfaceColor())) {
                            if (photoUri != null) AsyncImage(model = photoUri, contentDescription = localized("Обложка жилья", "Accommodation cover", "Portada del alojamiento", "Unterkunft-Titelbild"), contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Text(text = localized("Обложка", "Cover", "Portada", "Cover"), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = s(10f), lineHeight = s(14f), style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.align(Alignment.TopStart).padding(start = d(8f), top = d(8f)).background(OdysseyScrimSoft, RoundedCornerShape(d(20f))).padding(horizontal = d(7f), vertical = d(3f)))
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(d(128f))
                                .height(d(168f))
                                .clip(RoundedCornerShape(d(14f)))
                                .background(surfaceVariantColor())
                                .drawBehind {
                                    val stroke = d(1f).toPx()
                                    drawRoundRect(color = photoBorderColor, topLeft = Offset(stroke / 2f, stroke / 2f), size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke), cornerRadius = CornerRadius(d(14f).toPx() - stroke / 2f), style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(d(6f).toPx(), d(4f).toPx()))))
                                }
                                .clickable(onClick = onPickPhoto),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OdysseyPlusIcon(d(18f))
                                Text(text = localized("Добавить", "Add", "Añadir", "Hinzufügen"), color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = s(11.5f), lineHeight = s(15f), style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.padding(top = d(5f)))
                            }
                        }
                    }
                }

                Text(text = localized("Статус", "Status", "Estado", "Status"), style = labelStyle, modifier = Modifier.offset(x = d(16f), y = d(298f)).width(d(321f)).height(d(18f)))
                @Composable
                fun AddStatusChip(label: String, value: String, width: Float, modifier: Modifier = Modifier) {
                    Box(contentAlignment = Alignment.Center, modifier = modifier.width(d(width)).height(d(41f)).clip(RoundedCornerShape(d(12f))).background(if (status == value) primaryColor() else cardSurfaceColor()).border(d(1f), if (status == value) primaryColor() else contentBorderColor(), RoundedCornerShape(d(12f))).clickable { onStatusChange(value) }) {
                        Text(text = label, color = if (status == value) primaryContentColor() else secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = s(12f), lineHeight = s(16f), style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(d(9f)), modifier = Modifier.offset(x = d(16f), y = d(324f)).height(d(41f))) {
                    AddStatusChip(localized("хочу", "want", "quiero", "möchte"), "хочу", 70.6f)
                    AddStatusChip(localized("бронь", "reserved", "reserva", "Reservierung"), "бронь", 81f)
                    AddStatusChip(localized("оплачено", "paid", "pagado", "bezahlt"), "оплачено", 106.6f)
                }
                AddStatusChip(localized("пожили", "stayed", "alojado", "übernachtet"), "пожили", 92.2f, Modifier.offset(x = d(16f), y = d(374f)))

                AccommodationEditTextField(label = localized("Название", "Name", "Nombre", "Name"), value = name, placeholder = localized("Название жилья", "Accommodation name", "Nombre del alojamiento", "Name der Unterkunft"), valueWeight = FontWeight.W600, valueColor = contentTextColor(), scale = scale, modifier = Modifier.offset(x = d(16f), y = d(431f)).width(d(321f)), onValueChange = onNameChange)
                Row(horizontalArrangement = Arrangement.spacedBy(d(12f)), modifier = Modifier.offset(x = d(16f), y = d(524f)).width(d(321f))) {
                    AccommodationEditTextField(label = localized("Город", "City", "Ciudad", "Stadt"), value = city, placeholder = localized("Город", "City", "Ciudad", "Stadt"), valueWeight = FontWeight.W600, valueColor = contentTextColor(), scale = scale, modifier = Modifier.width(d(154.5f)), onValueChange = onCityChange)
                    AccommodationEditTextField(label = localized("Цена", "Price", "Precio", "Preis"), value = price, placeholder = "€120", valueWeight = FontWeight.W700, valueColor = contentTextColor(), scale = scale, modifier = Modifier.width(d(154.5f)), onValueChange = onPriceChange)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(d(12f)), modifier = Modifier.offset(x = d(16f), y = d(617f)).width(d(321f))) {
                    AccommodationEditDateField(label = localized("Заезд", "Check-in", "Entrada", "Anreise"), value = checkIn, scale = scale, modifier = Modifier.width(d(154.5f)), onClick = onCheckInClick)
                    AccommodationEditDateField(label = localized("Выезд", "Check-out", "Salida", "Abreise"), value = checkOut, scale = scale, modifier = Modifier.width(d(154.5f)), onClick = onCheckOutClick)
                }
                AccommodationEditDateField(label = localized("Бесплатная отмена до", "Free cancellation until", "Cancelación gratuita hasta", "Kostenlose Stornierung bis"), value = deadline, scale = scale, modifier = Modifier.offset(x = d(16f), y = d(710f)).width(d(321f)), onClick = onDeadlineClick)
                AccommodationEditTextField(label = localized("Ссылка на жильё", "Accommodation link", "Enlace del alojamiento", "Unterkunftslink"), value = bookingUrl, placeholder = "https://example.com/...", valueWeight = FontWeight.W600, valueColor = primaryColor(), scale = scale, modifier = Modifier.offset(x = d(16f), y = d(803f)).width(d(321f)), onValueChange = onBookingUrlChange)
                AccommodationEditTextField(label = localized("Адрес / заметка", "Address / note", "Dirección / nota", "Adresse / Notiz"), value = details, placeholder = localized("Дополнительные детали", "Additional details", "Detalles adicionales", "Zusätzliche Details"), valueWeight = FontWeight.W600, valueColor = contentTextColor(), scale = scale, modifier = Modifier.offset(x = d(16f), y = d(896f)).width(d(321f)), onValueChange = onDetailsChange)

                message?.let {
                    Text(text = it, color = OdysseyError, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = s(11f), lineHeight = s(15f), style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.offset(x = d(16f), y = d(984f)).width(d(336f)))
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.offset(x = d(16f), y = d(1031f)).width(d(135.3f)).height(d(53f)).clip(RoundedCornerShape(d(15f))).background(cardSurfaceColor()).border(d(1f), contentBorderColor(), RoundedCornerShape(d(15f))).clickable(onClick = onClose)) {
                    Text(text = localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = s(15f), lineHeight = s(20f), style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.offset(x = d(162.3f), y = d(1031f)).width(d(174.7f)).height(d(53f)).shadow(d(8f), RoundedCornerShape(d(15f)), clip = false, ambientColor = OdysseyPurpleShadow, spotColor = OdysseyPurpleShadow).clip(RoundedCornerShape(d(15f))).background(Brush.linearGradient(listOf(primaryColor(), OdysseyPurpleGradientEnd))).clickable(enabled = !saving, onClick = onSave)) {
                    Text(text = if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"), color = primaryContentColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = s(15f), lineHeight = s(20f), style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                }
            }
        }
    }
}

