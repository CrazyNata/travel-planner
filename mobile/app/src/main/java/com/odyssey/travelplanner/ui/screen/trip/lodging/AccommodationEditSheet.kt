package com.odyssey.travelplanner.ui.screen.trip.lodging

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.resolveTripPhotoReference
import com.odyssey.travelplanner.ui.domain.accommodationDateRange
import com.odyssey.travelplanner.ui.domain.formatAccommodationPrice
import com.odyssey.travelplanner.ui.domain.normalizeAccommodationStatus
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.icons.OdysseyPlusIcon
import com.odyssey.travelplanner.ui.screen.trip.route.accommodationDateParts
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.dangerSurfaceColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.surfaceVariantColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccommodationEditSheet(
    accommodation: com.odyssey.travelplanner.data.Accommodation,
    tripId: String,
    onClose: () -> Unit,
    onDeleted: () -> Unit,
    onSaved: () -> Unit,
    onPhotosChanged: (List<String>) -> Unit,
) {
    val context = LocalContext.current
    val language = LocalLanguage.current
    val initialDates = remember(accommodation.id) { accommodationDateParts(accommodation.dates) }
    var name by remember(accommodation.id) { mutableStateOf(accommodation.name) }
    var city by remember(accommodation.id) { mutableStateOf(accommodation.city) }
    var status by remember(accommodation.id) { mutableStateOf(normalizeAccommodationStatus(accommodation.status)) }
    var checkIn by remember(accommodation.id) { mutableStateOf(initialDates.first) }
    var checkOut by remember(accommodation.id) { mutableStateOf(initialDates.second) }
    var deadline by remember(accommodation.id) { mutableStateOf(accommodation.deadline) }
    var price by remember(accommodation.id) { mutableStateOf(formatAccommodationPrice(accommodation.price)) }
    var bookingUrl by remember(accommodation.id) { mutableStateOf(accommodation.bookingUrl) }
    var details by remember(accommodation.id) { mutableStateOf(accommodation.details) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var datePickerTarget by remember { mutableStateOf<String?>(null) }
    var deleteDialogOpen by remember { mutableStateOf(false) }
    var deletingAccommodation by remember { mutableStateOf(false) }
    var photos by remember(accommodation.id) { mutableStateOf(accommodation.photos) }
    var uploadingPhoto by remember { mutableStateOf(false) }
    var deletingPhotoIndex by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            uploadingPhoto = true
            message = null
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Не удалось прочитать изображение")
                val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                val reference = repository.replaceAccommodationCoverPhoto(tripId, accommodation.id, bytes)
                val imageUrl = SupabaseProvider.clientForCurrentAuthFlow().resolveTripPhotoReference(reference) ?: reference
                listOf(imageUrl) + photos.drop(1)
            }.onSuccess { updatedPhotos ->
                photos = updatedPhotos
                onPhotosChanged(updatedPhotos)
            }.onFailure {
                message = it.message ?: localized(
                    language,
                    "Не удалось загрузить фото. Проверьте интернет и повторите попытку.",
                    "Could not upload the photo. Check your connection and try again.",
                    "No se pudo subir la foto. Comprueba la conexión e inténtalo de nuevo.",
                    "Foto konnte nicht hochgeladen werden. Prüfen Sie die Verbindung und versuchen Sie es erneut.",
                )
            }
            uploadingPhoto = false
        }
    }
    val photoBusy = uploadingPhoto || deletingPhotoIndex != null
    val formBusy = saving || photoBusy || deletingAccommodation
    fun saveAccommodation() {
        scope.launch {
            saving = true
            message = null
            runCatching {
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateAccommodationDetailsRich(
                    tripId = tripId,
                    accommodationId = accommodation.id,
                    input = com.odyssey.travelplanner.data.AccommodationInput(
                        name = name,
                        city = city,
                        dates = accommodationDateRange(checkIn, checkOut, accommodation.dates),
                        price = price,
                        status = status,
                        details = details,
                        bookingUrl = bookingUrl,
                        deadline = deadline,
                        externalUrl = bookingUrl,
                        address = details,
                    ),
                )
            }.onSuccess {
                onSaved()
            }.onFailure {
                message = it.message ?: localized(
                    language,
                    "Не удалось сохранить жильё",
                    "Could not save lodging",
                    "No se pudo guardar el alojamiento",
                    "Unterkunft konnte nicht gespeichert werden",
                )
            }
            saving = false
        }
    }
    fun deleteAccommodation() {
        deleteDialogOpen = false
        scope.launch {
            deletingAccommodation = true
            message = null
            runCatching {
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                    .deleteAccommodation(tripId, accommodation.id)
            }.onSuccess {
                onDeleted()
            }.onFailure {
                message = it.message ?: localized(
                    language,
                    "Не удалось удалить жильё",
                    "Could not delete lodging",
                    "No se pudo eliminar el alojamiento",
                    "Unterkunft konnte nicht gelöscht werden",
                )
            }
            deletingAccommodation = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = maxWidth.value / 368f
        fun d(value: Float) = (value * scale).dp
        fun s(value: Float) = (value * scale).sp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d(720f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(d(650f))
                    .verticalScroll(rememberScrollState()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(d(1000f)),
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = d(164f), y = d(12f))
                            .size(d(40f), d(4f))
                            .clip(RoundedCornerShape(d(2f)))
                            .background(contentBorderColor()),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .offset(x = d(16f), y = d(32f))
                            .width(d(336f))
                            .height(d(34f)),
                    ) {
                Text(
                    text = localized("Редактировать жильё", "Edit lodging", "Editar alojamiento", "Unterkunft bearbeiten"),
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = s(24f),
                    lineHeight = s(33f),
                    style = androidx.compose.ui.text.TextStyle(
                        letterSpacing = s(-0.24f),
                        platformStyle = OdysseyNoFontPadding,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
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
            }

            Text(
                text = localized("Фотографии", "Photos", "Fotos", "Fotos"),
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = s(13f),
                lineHeight = s(18f),
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                modifier = Modifier
                    .offset(x = d(16f), y = d(82f))
                    .width(d(321f))
                    .height(d(18f)),
            )
            AccommodationEditPhotoStrip(
                photos = photos,
                scale = scale,
                uploading = uploadingPhoto,
                deletingIndex = deletingPhotoIndex,
                onPickPhoto = { if (!photoBusy) photoPicker.launch("image/*") },
                onDeletePhoto = { index ->
                    if (!photoBusy) {
                        scope.launch {
                            deletingPhotoIndex = index
                            message = null
                            runCatching {
                                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                                    .deleteAccommodationPhoto(tripId, accommodation.id, index)
                            }.onSuccess {
                                val updatedPhotos = photos.toMutableList().apply { removeAt(index) }
                                photos = updatedPhotos
                                onPhotosChanged(updatedPhotos)
                            }.onFailure {
                                message = it.message ?: localized(
                                    language,
                                    "Не удалось удалить фото. Проверьте интернет и повторите попытку.",
                                    "Could not delete the photo. Check your connection and try again.",
                                    "No se pudo eliminar la foto. Comprueba la conexión e inténtalo de nuevo.",
                                    "Foto konnte nicht gelöscht werden. Prüfen Sie die Verbindung und versuchen Sie es erneut.",
                                )
                            }
                            deletingPhotoIndex = null
                        }
                    }
                },
                modifier = Modifier
                    .offset(x = d(16f), y = d(108f))
                .width(d(321f))
                .height(d(172f)),
            )
            Text(
                text = localized("Статус", "Status", "Estado", "Status"),
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = s(13f),
                lineHeight = s(18f),
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                modifier = Modifier
                    .offset(x = d(16f), y = d(298f))
                    .width(d(336f))
                    .height(d(18f)),
            )
            @Composable
            fun EditStatusChip(label: String, value: String, width: Float, modifier: Modifier = Modifier) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = modifier
                        .width(d(width))
                        .height(d(41f))
                        .clip(RoundedCornerShape(d(12f)))
                        .background(if (status == value) primaryColor() else cardSurfaceColor())
                        .border(d(1f), if (status == value) primaryColor() else contentBorderColor(), RoundedCornerShape(d(12f)))
                        .clickable { status = value },
                ) {
                    Text(
                        text = label,
                        color = if (status == value) primaryContentColor() else secondaryTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(12f),
                        lineHeight = s(16f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(9f)),
                modifier = Modifier
                    .offset(x = d(16f), y = d(324f))
                    .height(d(41f)),
            ) {
                EditStatusChip(localized("хочу", "want", "quiero", "möchte"), "хочу", 70.6f)
                EditStatusChip(localized("бронь", "reserved", "reserva", "Reservierung"), "бронь", 81f)
                EditStatusChip(localized("оплачено", "paid", "pagado", "bezahlt"), "оплачено", 106.6f)
            }
            EditStatusChip(
                localized("пожили", "stayed", "alojado", "übernachtet"),
                "пожили",
                92.2f,
                Modifier.offset(x = d(16f), y = d(374f)),
            )
            AccommodationEditTextField(
                label = localized("Название жилья", "Accommodation name", "Nombre del alojamiento", "Name der Unterkunft"),
                value = name,
                placeholder = localized("Название жилья", "Accommodation name", "Nombre del alojamiento", "Name der Unterkunft"),
                valueWeight = FontWeight.W700,
                valueColor = contentTextColor(),
                scale = scale,
                modifier = Modifier.offset(x = d(16f), y = d(431f)).width(d(336f)),
                onValueChange = { name = it },
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(12f)),
                modifier = Modifier
                    .offset(x = d(16f), y = d(524f))
                    .width(d(336f))
                    .height(d(77f)),
            ) {
                AccommodationEditTextField(
                    label = localized("Город", "City", "Ciudad", "Stadt"),
                    value = city,
                    placeholder = localized("Город", "City", "Ciudad", "Stadt"),
                    valueWeight = FontWeight.W600,
                    valueColor = contentTextColor(),
                    scale = scale,
                    modifier = Modifier.weight(1f),
                    onValueChange = { city = it },
                )
                AccommodationEditTextField(
                    label = localized("Цена", "Price", "Precio", "Preis"),
                    value = price,
                    placeholder = "€0",
                    valueWeight = FontWeight.W700,
                    valueColor = contentTextColor(),
                    scale = scale,
                    modifier = Modifier.weight(1f),
                    onValueChange = { price = it },
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(12f)),
                modifier = Modifier
                    .offset(x = d(16f), y = d(617f))
                    .width(d(336f))
                    .height(d(77f)),
            ) {
                AccommodationEditDateField(
                    label = localized("Заезд", "Check-in", "Entrada", "Anreise"),
                    value = checkIn,
                    scale = scale,
                    modifier = Modifier.weight(1f),
                    onClick = { datePickerTarget = "checkIn" },
                )
                AccommodationEditDateField(
                    label = localized("Выезд", "Check-out", "Salida", "Abreise"),
                    value = checkOut,
                    scale = scale,
                    modifier = Modifier.weight(1f),
                    onClick = { datePickerTarget = "checkOut" },
                )
            }
            AccommodationEditDateField(
                label = localized("Бесплатная отмена до", "Free cancellation until", "Cancelación gratuita hasta", "Kostenlose Stornierung bis"),
                value = deadline,
                scale = scale,
                modifier = Modifier.offset(x = d(16f), y = d(710f)).width(d(336f)),
                onClick = { datePickerTarget = "deadline" },
            )
            AccommodationEditTextField(
                label = localized("Ссылка на жильё", "Accommodation link", "Enlace del alojamiento", "Unterkunftslink"),
                value = bookingUrl,
                placeholder = "https://example.com/...",
                valueWeight = FontWeight.W600,
                valueColor = primaryColor(),
                scale = scale,
                modifier = Modifier.offset(x = d(16f), y = d(803f)).width(d(336f)),
                onValueChange = { bookingUrl = it },
            )
            AccommodationEditTextField(
                label = localized("Адрес / заметка", "Address / note", "Dirección / nota", "Adresse / Notiz"),
                value = details,
                placeholder = localized("Дополнительные детали", "Additional details", "Detalles adicionales", "Zusätzliche Details"),
                valueWeight = FontWeight.W600,
                valueColor = contentTextColor(),
                scale = scale,
                modifier = Modifier.offset(x = d(16f), y = d(896f)).width(d(336f)),
                onValueChange = { details = it },
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(d(11f)),
                modifier = Modifier
                    .offset(x = d(16f), y = d(795f))
                    .width(d(336f))
                    .height(0.dp)
                    .graphicsLayer { alpha = 0f },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(d(141.578f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(d(15f)))
                        .background(cardSurfaceColor())
                        .border(d(1f), contentBorderColor(), RoundedCornerShape(d(15f)))
                        .clickable(onClick = onClose),
                ) {
                    Text(
                        text = localized("Отмена", "Cancel", "Cancelar", "Abbrechen"),
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(15f),
                        lineHeight = s(20f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(d(183.422f))
                        .fillMaxHeight()
                        .shadow(
                            d(8f),
                            RoundedCornerShape(d(15f)),
                            clip = false,
                            ambientColor = Color(0x4D6C5CE7),
                            spotColor = Color(0x4D6C5CE7),
                        )
                        .clip(RoundedCornerShape(d(15f)))
                        .background(Brush.linearGradient(listOf(primaryColor(), Color(0xFF7D6CF0))))
                        .clickable(enabled = !saving && !photoBusy) {
                            scope.launch {
                                saving = true
                                message = null
                                runCatching {
                                    SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateAccommodationDetailsRich(
                                        tripId = tripId,
                                        accommodationId = accommodation.id,
                                        input = com.odyssey.travelplanner.data.AccommodationInput(
                                            name = name,
                                            city = city,
                                            dates = accommodationDateRange(checkIn, checkOut, accommodation.dates),
                                            price = price,
                                            status = status,
                                            details = details,
                                            bookingUrl = bookingUrl,
                                            deadline = deadline,
                                            externalUrl = bookingUrl,
                                            address = details,
                                        ),
                                    )
                                }.onSuccess {
                                    onSaved()
                                }.onFailure {
                                    message = it.message ?: localized(language, "Не удалось сохранить жильё", "Could not save lodging", "No se pudo guardar el alojamiento", "Unterkunft konnte nicht gespeichert werden")
                                }
                                saving = false
                            }
                        },
                ) {
                    Text(
                        text = if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"),
                        color = primaryContentColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(15f),
                        lineHeight = s(20f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
            }
            message?.let {
                Text(
                    text = it,
                    color = Color(0xFFE0524B),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = s(11f),
                    lineHeight = s(15f),
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    modifier = Modifier
                        .offset(x = d(16f), y = d(754f))
                        .width(d(336f))
                        .graphicsLayer { alpha = 0f },
                )
            }
        }
    }
            Column(
                verticalArrangement = Arrangement.spacedBy(d(8f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(cardSurfaceColor())
                    .padding(start = d(16f), end = d(16f), top = d(8f), bottom = d(12f)),
            ) {
                message?.let {
                    Text(
                        text = it,
                        color = Color(0xFFE0524B),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W700,
                        fontSize = s(11f),
                        lineHeight = s(15f),
                        maxLines = 2,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d(11f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(d(53f)),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(d(54f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(d(15f)))
                            .background(dangerSurfaceColor())
                            .clickable(enabled = !formBusy) { deleteDialogOpen = true },
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = localized("Удалить жильё", "Delete lodging", "Eliminar alojamiento", "Unterkunft löschen"),
                            tint = Color(0xFFFF6B65),
                            modifier = Modifier.size(d(20f)),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(d(15f)))
                            .background(cardSurfaceColor())
                            .border(d(1f), contentBorderColor(), RoundedCornerShape(d(15f)))
                            .clickable(enabled = !formBusy, onClick = onClose),
                    ) {
                        Text(
                            text = localized("Отмена", "Cancel", "Cancelar", "Abbrechen"),
                            color = contentTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = s(15f),
                            lineHeight = s(20f),
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxHeight()
                            .shadow(
                                d(8f),
                                RoundedCornerShape(d(15f)),
                                clip = false,
                                ambientColor = Color(0x4D6C5CE7),
                                spotColor = Color(0x4D6C5CE7),
                            )
                            .clip(RoundedCornerShape(d(15f)))
                            .background(Brush.linearGradient(listOf(primaryColor(), Color(0xFF7D6CF0))))
                            .clickable(enabled = !formBusy, onClick = ::saveAccommodation),
                    ) {
                        Text(
                            text = if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"),
                            color = primaryContentColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = s(15f),
                            lineHeight = s(20f),
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                    }
                }
            }
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
    if (deleteDialogOpen) {
        AlertDialog(
            onDismissRequest = { if (!deletingAccommodation) deleteDialogOpen = false },
            title = {
                Text(
                    localized("Удалить жильё?", "Delete lodging?", "¿Eliminar alojamiento?", "Unterkunft löschen?"),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                )
            },
            text = {
                Text(
                    localized(
                        "Жильё и его фотографии будут удалены из маршрута.",
                        "The lodging and its photos will be removed from the trip.",
                        "El alojamiento y sus fotos se eliminarán del viaje.",
                        "Die Unterkunft und ihre Fotos werden aus der Reise entfernt.",
                    ),
                    fontFamily = Manrope,
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteDialogOpen = false },
                    enabled = !deletingAccommodation,
                ) {
                    Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = ::deleteAccommodation,
                    enabled = !deletingAccommodation,
                ) {
                    Text(
                        if (deletingAccommodation) localized("Удаляем…", "Deleting…", "Eliminando…", "Wird gelöscht…")
                        else localized("Удалить", "Delete", "Eliminar", "Löschen"),
                        color = Color(0xFFE0524B),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                    )
                }
            },
        )
    }
}
}

@Composable
internal fun AccommodationEditPhotoStrip(
    photos: List<String>,
    scale: Float,
    uploading: Boolean,
    deletingIndex: Int?,
    onPickPhoto: () -> Unit,
    onDeletePhoto: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun d(value: Float) = (value * scale).dp
    fun s(value: Float) = (value * scale).sp
    val busy = uploading || deletingIndex != null
    val photoBorderColor = contentBorderColor()
    val photoScrollState = rememberScrollState()

    Box(modifier = modifier.horizontalScroll(photoScrollState)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(d(10f)),
            modifier = Modifier.height(d(168f)),
        ) {
            if (photos.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(d(180f))
                        .height(d(168f))
                        .clip(RoundedCornerShape(d(16f)))
                        .background(surfaceVariantColor())
                        .drawBehind {
                            val stroke = d(1f).toPx()
                            drawRoundRect(
                                color = photoBorderColor,
                                topLeft = Offset(stroke / 2f, stroke / 2f),
                                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                                cornerRadius = CornerRadius(d(16f).toPx() - stroke / 2f),
                                style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(d(6f).toPx(), d(4f).toPx()))),
                            )
                        }
                        .clickable(enabled = !busy, onClick = onPickPhoto),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Image, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(d(26f)))
                        Text(
                            text = localized(
                                "Обложка — выберите фото",
                                "Cover — choose a photo",
                                "Portada — elige una foto",
                                "Cover — Foto auswählen",
                            ),
                            color = secondaryTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = s(11.5f),
                            lineHeight = s(17f),
                            textAlign = TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                            modifier = Modifier.padding(top = d(6f)),
                        )
                    }
                }
            } else {
                AccommodationEditPhotoTile(
                    photo = photos.first(),
                    photoIndex = 0,
                    width = 180f,
                    label = localized("Обложка", "Cover", "Portada", "Cover"),
                    scale = scale,
                    deletingIndex = deletingIndex,
                    busy = busy,
                    onDeletePhoto = onDeletePhoto,
                )
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
                        drawRoundRect(
                            color = photoBorderColor,
                            topLeft = Offset(stroke / 2f, stroke / 2f),
                            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                            cornerRadius = CornerRadius(d(14f).toPx() - stroke / 2f),
                            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(d(6f).toPx(), d(4f).toPx()))),
                        )
                    }
                    .clickable(enabled = !busy, onClick = onPickPhoto),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (uploading) {
                        CircularProgressIndicator(
                            color = primaryColor(),
                            strokeWidth = d(2f),
                            modifier = Modifier.size(d(20f)),
                        )
                    } else {
                        OdysseyPlusIcon(d(18f))
                    }
                    Text(
                        text = if (uploading) localized("Загрузка…", "Uploading…", "Subiendo…", "Wird hochgeladen…") else localized("Добавить", "Add", "Añadir", "Hinzufügen"),
                        color = primaryColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(11.5f),
                        lineHeight = s(15f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        modifier = Modifier.padding(top = d(5f)),
                    )
                }
            }
            if (photos.isNotEmpty()) {
                photos.drop(1).forEachIndexed { index, photo ->
                    AccommodationEditPhotoTile(
                        photo = photo,
                        photoIndex = index + 1,
                        width = 128f,
                        label = null,
                        scale = scale,
                        deletingIndex = deletingIndex,
                        busy = busy,
                        onDeletePhoto = onDeletePhoto,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AccommodationEditPhotoTile(
    photo: String,
    photoIndex: Int,
    width: Float,
    label: String?,
    scale: Float,
    deletingIndex: Int?,
    busy: Boolean,
    onDeletePhoto: (Int) -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    Box(
        modifier = Modifier
            .width(d(width))
            .height(d(168f))
            .clip(RoundedCornerShape(d(14f)))
            .background(surfaceVariantColor()),
    ) {
        AsyncImage(
            model = photo,
            contentDescription = localized("Фото жилья", "Lodging photo", "Foto del alojamiento", "Unterkunftsfoto"),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        label?.let {
            Text(
                text = it,
                color = Color.White,
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = d(10f).value.sp,
                lineHeight = d(14f).value.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = d(8f), top = d(8f))
                    .background(Color(0x8C141419), RoundedCornerShape(d(20f)))
                    .padding(horizontal = d(7f), vertical = d(3f)),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(d(8f))
                .size(d(32f))
                .clip(CircleShape)
                .background(Color(0xFFFDE8E7))
                .clickable(enabled = !busy, onClick = { onDeletePhoto(photoIndex) }),
        ) {
            if (deletingIndex == photoIndex) {
                CircularProgressIndicator(
                    color = Color(0xFFE0524B),
                    strokeWidth = d(2f),
                    modifier = Modifier.size(d(17f)),
                )
            } else {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = localized("Удалить фото", "Delete photo", "Eliminar foto", "Foto löschen"),
                    tint = Color(0xFFE0524B),
                    modifier = Modifier.size(d(17f)),
                )
            }
        }
    }
}

