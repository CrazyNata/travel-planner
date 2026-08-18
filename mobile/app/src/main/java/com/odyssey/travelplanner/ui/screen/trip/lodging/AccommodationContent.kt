package com.odyssey.travelplanner.ui.screen.trip.lodging

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.cancellation.CancellationException
import com.odyssey.travelplanner.data.AccommodationCatalogEntry
import com.odyssey.travelplanner.data.AccommodationCatalogRepository
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.TripOverview
import com.odyssey.travelplanner.data.cityFlag
import com.odyssey.travelplanner.ui.common.FullScreenPhotoViewer
import com.odyssey.travelplanner.ui.domain.accommodationBookingSearchUrl
import com.odyssey.travelplanner.ui.domain.accommodationDateRange
import com.odyssey.travelplanner.ui.domain.cityFilterKey
import com.odyssey.travelplanner.ui.domain.formatAccommodationDeadline
import com.odyssey.travelplanner.ui.domain.formatAccommodationPrice
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.i18n.splitStoredCityList
import com.odyssey.travelplanner.ui.icons.OdysseyCalendarIcon
import com.odyssey.travelplanner.ui.icons.OdysseyEditIcon
import com.odyssey.travelplanner.ui.icons.OdysseyExternalLinkIcon
import com.odyssey.travelplanner.ui.icons.OdysseyPlusIcon
import com.odyssey.travelplanner.ui.screen.trip.route.formatAccommodationDates
import com.odyssey.travelplanner.ui.screen.trip.sights.accommodationPhotoLoadGate
import com.odyssey.travelplanner.ui.screen.trip.sights.catalogRatingCountLabel
import com.odyssey.travelplanner.ui.theme.LocalDarkTheme
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBorder
import com.odyssey.travelplanner.ui.theme.OdysseyError
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.OdysseyScrimStrong
import com.odyssey.travelplanner.ui.theme.OdysseySheetScrim
import com.odyssey.travelplanner.ui.theme.OdysseySuccess
import com.odyssey.travelplanner.ui.theme.OdysseyWarning
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.labelColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.secondarySurfaceColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccommodationContent(tripId: String, overview: TripOverview, canEdit: Boolean = true, onStatusUpdated: () -> Unit) {
    val context = LocalContext.current
    val language = LocalLanguage.current
    var savingAccommodationId by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var dates by remember { mutableStateOf("") }
    var checkIn by remember { mutableStateOf("") }
    var checkOut by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("хочу") }
    var bookingUrl by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var newAccommodationPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var datePickerTarget by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var editingAccommodation by remember { mutableStateOf<com.odyssey.travelplanner.data.Accommodation?>(null) }
    var accommodationAddChoiceOpen by remember { mutableStateOf(false) }
    var accommodationCatalogOpen by remember { mutableStateOf(false) }
    var accommodationCatalogCity by remember { mutableStateOf("") }
    var selectedAccommodationPlace by remember { mutableStateOf<AccommodationCatalogEntry?>(null) }
    var accommodationAddedName by remember { mutableStateOf<String?>(null) }
    var uploadingAccommodationId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val tripCityOptions = remember(overview.cities, overview.routeLegs, overview.sights, overview.accommodations, overview.restaurants) {
        (
            overview.cities +
                overview.routeLegs.flatMap { listOf(it.from, it.to) } +
                overview.sights.map { it.city } +
                overview.accommodations.map { it.city } +
                overview.restaurants.map { it.city }
            ).flatMap(::splitStoredCityList).map(String::trim).filter(String::isNotBlank).distinctBy(::cityFilterKey)
    }
    val defaultAccommodationCity = accommodationCatalogCity.ifBlank { tripCityOptions.firstOrNull().orEmpty() }

    fun resetAccommodationForm() {
        name = ""
        city = ""
        dates = ""
        checkIn = ""
        checkOut = ""
        deadline = ""
        price = ""
        status = "хочу"
        bookingUrl = ""
        details = ""
        newAccommodationPhotoUri = null
        message = null
        datePickerTarget = null
    }

    fun closeAccommodationForm() {
        adding = false
        resetAccommodationForm()
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val accommodationId = uploadingAccommodationId ?: return@rememberLauncherForActivityResult
        if (uri == null) {
            uploadingAccommodationId = null
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Не удалось прочитать изображение")
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).addAccommodationPhoto(tripId, accommodationId, bytes)
            }.onSuccess {
                actionMessage = null
                onStatusUpdated()
            }.onFailure {
                actionMessage = it.message ?: localized(language, "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0444\u043e\u0442\u043e. \u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442 \u0438 \u043f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u0435 \u043f\u043e\u043f\u044b\u0442\u043a\u0443.", "Could not upload the photo. Check your connection and try again.", "No se pudo subir la foto. Comprueba la conexi\u00f3n e int\u00e9ntalo de nuevo.", "Foto konnte nicht hochgeladen werden. Pr\u00fcfen Sie die Verbindung und versuchen Sie es erneut.")
            }
            uploadingAccommodationId = null
        }
    }
    val newAccommodationPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        newAccommodationPhotoUri = uri
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            top = 18.dp,
            end = 18.dp,
            bottom = 30.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (actionMessage != null) {
            item {
                Text(actionMessage!!, color = OdysseyError, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
            }
        }
        if (overview.accommodations.isEmpty()) {
            item { Text(localized("Жильё пока не добавлено", "No lodging added yet", "Aún no se ha añadido alojamiento", "Noch keine Unterkunft hinzugefügt"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp) }
        } else {
            items(overview.accommodations, key = { it.id }) { accommodation ->
                AccommodationCard(
                    accommodation,
                    savingAccommodationId == accommodation.id,
                    uploadingAccommodationId == accommodation.id,
                    canEdit = canEdit,
                    onEdit = { if (canEdit) editingAccommodation = accommodation },
                    onAddPhoto = { uploadingAccommodationId = accommodation.id; photoPicker.launch("image/*") },
                    onMovePhoto = { index, direction ->
                        scope.launch {
                            savingAccommodationId = accommodation.id
                            runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).moveAccommodationPhoto(tripId, accommodation.id, index, direction) }
                                .onSuccess {
                                    actionMessage = null
                                    onStatusUpdated()
                                }
                                .onFailure {
                                    actionMessage = it.message ?: localized(language, "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0438\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043f\u043e\u0440\u044f\u0434\u043e\u043a \u0444\u043e\u0442\u043e. \u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442 \u0438 \u043f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u0435 \u043f\u043e\u043f\u044b\u0442\u043a\u0443.", "Could not change the photo order. Check your connection and try again.", "No se pudo cambiar el orden de las fotos. Comprueba la conexi\u00f3n e int\u00e9ntalo de nuevo.", "Die Fotoreihenfolge konnte nicht geändert werden. Prüfen Sie die Verbindung und versuchen Sie es erneut.")
                                }
                            savingAccommodationId = null
                        }
                    },
                ) { status ->
                    if (canEdit) scope.launch {
                        savingAccommodationId = accommodation.id
                        runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateAccommodationStatus(tripId, accommodation.id, status) }
                            .onSuccess {
                                actionMessage = null
                                onStatusUpdated()
                            }
                            .onFailure {
                                actionMessage = it.message ?: localized(language, "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0441\u0442\u0430\u0442\u0443\u0441. \u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442 \u0438 \u043f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u0435 \u043f\u043e\u043f\u044b\u0442\u043a\u0443.", "Could not save the status. Check your connection and try again.", "No se pudo guardar el estado. Comprueba la conexi\u00f3n e int\u00e9ntalo de nuevo.", "Status konnte nicht gespeichert werden. Pr\u00fcfen Sie die Verbindung und versuchen Sie es erneut.")
                            }
                        savingAccommodationId = null
                    }
                }
            }
        }
        if (canEdit) item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (LocalDarkTheme.current) secondarySurfaceColor() else Color.White.copy(alpha = 0.4f))
                    .border(androidx.compose.foundation.BorderStroke(2.dp, if (LocalDarkTheme.current) OdysseyDarkBorder else Color(0xFFD3D3DB)), RoundedCornerShape(18.dp))
                    .clickable { accommodationAddChoiceOpen = true; actionMessage = null },
            ) {
                OdysseyPlusIcon(18.dp, primaryColor())
                Text(localized("Добавить жильё", "Add lodging", "Añadir alojamiento", "Unterkunft hinzufügen"), color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, lineHeight = 18.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    if (canEdit && editingAccommodation != null) {
        ModalBottomSheet(
            onDismissRequest = { editingAccommodation = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = OdysseySheetScrim,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            dragHandle = null,
        ) {
            AccommodationEditSheet(
                accommodation = editingAccommodation!!,
                tripId = tripId,
                onClose = { editingAccommodation = null },
                onDeleted = {
                    editingAccommodation = null
                    onStatusUpdated()
                },
                onSaved = {
                    editingAccommodation = null
                    onStatusUpdated()
                },
                onPhotosChanged = { photos ->
                    editingAccommodation = editingAccommodation?.copy(photos = photos)
                    onStatusUpdated()
                },
            )
        }
    }
    if (canEdit && adding) {
        ModalBottomSheet(
            onDismissRequest = ::closeAccommodationForm,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = OdysseySheetScrim,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            dragHandle = null,
        ) {
            AccommodationAddSheet(
                name = name,
                city = city,
                checkIn = checkIn,
                checkOut = checkOut,
                deadline = deadline,
                price = price,
                bookingUrl = bookingUrl,
                details = details,
                status = status,
                photoUri = newAccommodationPhotoUri,
                saving = saving,
                message = message,
                onNameChange = { name = it },
                onCityChange = { city = it },
                onCheckInClick = { datePickerTarget = "checkIn" },
                onCheckOutClick = { datePickerTarget = "checkOut" },
                onDeadlineClick = { datePickerTarget = "deadline" },
                onPriceChange = { price = it },
                onBookingUrlChange = { bookingUrl = it },
                onDetailsChange = { details = it },
                onStatusChange = { status = it },
                onPickPhoto = { newAccommodationPhotoPicker.launch("image/*") },
                onClose = ::closeAccommodationForm,
                onSave = {
                    scope.launch {
                        saving = true
                        var createdAccommodationId: String? = null
                        runCatching {
                            val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                            val accommodationId = repository.addAccommodationDetails(
                                com.odyssey.travelplanner.data.AccommodationInput(
                                    name = name,
                                    city = city,
                                    dates = accommodationDateRange(checkIn, checkOut, dates),
                                    price = price,
                                    status = status,
                                    details = details,
                                    bookingUrl = bookingUrl,
                                    deadline = deadline,
                                    source = "manual",
                                    externalUrl = bookingUrl,
                                    address = details,
                                    tripCityId = city.trim(),
                                ),
                                tripId,
                            )
                            createdAccommodationId = accommodationId
                            newAccommodationPhotoUri?.let { uri ->
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                    ?: error("Не удалось прочитать изображение")
                                repository.addAccommodationPhoto(tripId, accommodationId, bytes)
                            }
                        }.onSuccess {
                                closeAccommodationForm()
                                onStatusUpdated()
                        }.onFailure {
                            createdAccommodationId?.let { accommodationId ->
                                runCatching {
                                    SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                                        .deleteAccommodation(tripId, accommodationId)
                                }
                            }
                            message = it.message ?: localized(language, "Не удалось сохранить жильё", "Could not save lodging", "No se pudo guardar el alojamiento", "Unterkunft konnte nicht gespeichert werden")
                        }
                        saving = false
                    }
                },
            )
        }
    }
    if (canEdit && accommodationAddChoiceOpen) {
        ModalBottomSheet(
            onDismissRequest = { accommodationAddChoiceOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = OdysseySheetScrim,
            shape = RoundedCornerShape(topStart = 29.dp, topEnd = 29.dp),
            dragHandle = null,
        ) {
            AccommodationAddChoiceSheet(
                onManual = {
                    accommodationAddChoiceOpen = false
                    resetAccommodationForm()
                    city = defaultAccommodationCity
                    adding = true
                },
                onFromGoogle = {
                    accommodationAddChoiceOpen = false
                    accommodationCatalogCity = defaultAccommodationCity
                    accommodationCatalogOpen = true
                },
                onClose = { accommodationAddChoiceOpen = false },
            )
        }
    }
    if (canEdit && accommodationCatalogOpen) {
        ModalBottomSheet(
            onDismissRequest = { accommodationCatalogOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = OdysseySheetScrim,
            shape = RoundedCornerShape(topStart = 29.dp, topEnd = 29.dp),
            dragHandle = null,
        ) {
            AccommodationCatalogSheet(
                city = defaultAccommodationCity,
                cityOptions = tripCityOptions,
                onCityChange = { accommodationCatalogCity = it },
                onSelect = { entry ->
                    accommodationCatalogOpen = false
                    selectedAccommodationPlace = entry
                },
                onClose = { accommodationCatalogOpen = false },
            )
        }
    }
    selectedAccommodationPlace?.let { place ->
        ModalBottomSheet(
            onDismissRequest = { selectedAccommodationPlace = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = OdysseySheetScrim,
            shape = RoundedCornerShape(topStart = 29.dp, topEnd = 29.dp),
            dragHandle = null,
        ) {
            AccommodationPlaceDetailsSheet(
                place = place,
                tripId = tripId,
                tripCityId = defaultAccommodationCity,
                tripDates = overview.dates,
                onClose = { selectedAccommodationPlace = null },
                onSaved = {
                    selectedAccommodationPlace = null
                    accommodationAddedName = place.name
                    actionMessage = null
                    onStatusUpdated()
                },
            )
        }
    }
    accommodationAddedName?.let { addedName ->
        AlertDialog(
            onDismissRequest = { accommodationAddedName = null },
            title = { Text(localized("Добавлено в поездку", "Added to trip", "Añadido al viaje", "Zur Reise hinzugefügt"), fontFamily = Manrope, fontWeight = FontWeight.W800) },
            text = { Text(addedName, fontFamily = Manrope, fontWeight = FontWeight.W700) },
            confirmButton = {
                TextButton(onClick = { accommodationAddedName = null }) {
                    Text(localized("Готово", "Done", "Listo", "Fertig"), color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800)
                }
            },
        )
    }
    datePickerTarget?.let { target ->
        AccommodationCalendarDialog(
            initialValue = when (target) {
                "checkIn" -> checkIn
                "checkOut" -> checkOut
                else -> deadline
            },
            onDismiss = { datePickerTarget = null },
            onConfirm = { selected ->
                when (target) {
                    "checkIn" -> checkIn = selected
                    "checkOut" -> checkOut = selected
                    else -> deadline = selected
                }
                datePickerTarget = null
            },
        )
    }
}

@Composable
internal fun AccommodationCard(accommodation: com.odyssey.travelplanner.data.Accommodation, saving: Boolean, uploading: Boolean, canEdit: Boolean = true, onEdit: () -> Unit, onAddPhoto: () -> Unit, onMovePhoto: (Int, Int) -> Unit, onStatusChange: (String) -> Unit) {
    val uriHandler = LocalUriHandler.current
    val language = LocalLanguage.current
    val surface = cardSurfaceColor()
    val city = accommodation.city.trim()
    val cityPrefix = cityFlag(city).takeUnless { it == "📍" }.orEmpty()
    val cityLabel = listOf(cityPrefix, localizedCityName(city)).filter(String::isNotBlank).joinToString(" ")
    val dates = formatAccommodationDates(accommodation.dates, language)
    val price = formatAccommodationPrice(accommodation.price)
    val bookingTarget = accommodation.bookingUrl.trim().takeIf(String::isNotBlank)
        ?: accommodation.website.trim().takeIf(String::isNotBlank)
        ?: accommodationBookingSearchUrl(accommodation.name, accommodation.city)
    val bookingLabel = when {
        accommodation.bookingUrl.isNotBlank() -> localized("Открыть ссылку", "Open link", "Abrir enlace", "Link öffnen")
        accommodation.website.isNotBlank() -> localized("Открыть сайт", "Open website", "Abrir sitio", "Website öffnen")
        else -> localized("Забронировать", "Book", "Reservar", "Buchen")
    }
    val uploadedPhotos = accommodation.photos
    val catalogRepository = remember { AccommodationCatalogRepository(SupabaseProvider.clientForCurrentAuthFlow()) }
    var googlePhotoUrl by remember(accommodation.id, accommodation.photoReference) { mutableStateOf<String?>(null) }
    LaunchedEffect(accommodation.id, accommodation.photoReference, uploadedPhotos) {
        if (uploadedPhotos.isEmpty() && accommodation.photoReference.isNotBlank()) {
            googlePhotoUrl = try {
                accommodationPhotoLoadGate.withPermit { catalogRepository.resolvePhoto(accommodation.photoReference)?.photoUrl }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                null
            }
        }
    }
    val photos = uploadedPhotos.ifEmpty { listOfNotNull(googlePhotoUrl) }
    var photoIndex by remember(accommodation.id, photos) { mutableStateOf(0) }
    var fullScreenPhotoIndex by remember(accommodation.id, photos) { mutableStateOf<Int?>(null) }
    val activePhotoIndex = photoIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surface)
            .shadow(8.dp, RoundedCornerShape(20.dp), clip = false, ambientColor = Color(0x12141428), spotColor = Color(0x12141428)),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(210.dp).background(secondarySurfaceColor())) {
            photos.getOrNull(activePhotoIndex)?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = accommodation.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clickable { fullScreenPhotoIndex = activePhotoIndex },
                )
            }
            if (photos.size > 1) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(OdysseyScrimStrong)
                        .clickable {
                            photoIndex = (activePhotoIndex - 1 + photos.size) % photos.size
                        },
                ) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = localized("Предыдущее фото", "Previous photo", "Foto anterior", "Vorheriges Foto"),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(OdysseyScrimStrong)
                        .clickable {
                            photoIndex = (activePhotoIndex + 1) % photos.size
                        },
                ) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = localized("Следующее фото", "Next photo", "Foto siguiente", "Nächstes Foto"),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp).graphicsLayer(rotationZ = 180f),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(OdysseyScrimStrong)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "${activePhotoIndex + 1}/${photos.size}",
                        color = Color.White,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(start = 15.dp, top = 13.dp, end = 15.dp, bottom = 15.dp)) {
            Row(modifier = Modifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(accommodation.name, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 16.sp, lineHeight = 22.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (price.isNotBlank()) {
                    Text(price, color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 15.sp, lineHeight = 21.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text(cityLabel, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 7.dp).height(17.dp)) {
                OdysseyCalendarIcon(14.dp, primaryColor())
                Text(dates, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.5.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            accommodation.rating?.let { rating ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 11.5.dp).height(17.dp)) {
                    Text("★", color = OdysseyWarning, fontFamily = Manrope, fontWeight = FontWeight.W400, fontSize = 12.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                    Text("· ${rating.toString().removeSuffix(".0")} / 10", color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.sp, lineHeight = 15.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                    accommodation.reviewCount?.let { count ->
                        Text("· ${catalogRatingCountLabel(count, language)}", color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 10.5.sp, lineHeight = 15.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (accommodation.rating == null) {
                accommodation.reviewCount?.let { count ->
                    Text(catalogRatingCountLabel(count, language), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 10.5.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            if (accommodation.deadline.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 10.dp).height(17.dp)) {
                    Text("✓", color = OdysseySuccess, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.width(14.dp))
                    Text(localized("Бесплатная отмена до ${formatAccommodationDeadline(accommodation.deadline, language)}", "Free cancellation until ${formatAccommodationDeadline(accommodation.deadline, language)}", "Cancelación gratuita hasta ${formatAccommodationDeadline(accommodation.deadline, language)}", "Kostenlose Stornierung bis ${formatAccommodationDeadline(accommodation.deadline, language)}"), color = OdysseySuccess, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(top = if (accommodation.deadline.isNotBlank()) 15.5.dp else 12.dp).height(42.dp)) {
                if (canEdit) {
                    Box(modifier = (if (bookingTarget.isNotBlank()) Modifier.width(150.234.dp) else Modifier.weight(1f)).fillMaxHeight().clip(RoundedCornerShape(12.dp)).border(1.dp, contentBorderColor(), RoundedCornerShape(12.dp)).clickable { onEdit() }, contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            OdysseyEditIcon(15.dp, primaryColor())
                            Text(localized("Редактировать", "Edit", "Editar", "Bearbeiten"), color = labelColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.5.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1)
                        }
                    }
                }
                if (bookingTarget.isNotBlank()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).border(1.dp, contentBorderColor(), RoundedCornerShape(12.dp)).clickable { uriHandler.openUri(bookingTarget) }, contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            OdysseyExternalLinkIcon(15.dp, primaryColor())
                            Text(bookingLabel, color = labelColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.5.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1)
                        }
                    }
                }
            }
        }
        fullScreenPhotoIndex?.let { initialIndex ->
            if (photos.isNotEmpty()) {
                FullScreenPhotoViewer(
                    photos = photos,
                    initialIndex = initialIndex,
                    accommodationName = accommodation.name,
                    onDismiss = { selectedIndex ->
                        photoIndex = selectedIndex
                        fullScreenPhotoIndex = null
                    },
                )
            }
        }
    }
}

