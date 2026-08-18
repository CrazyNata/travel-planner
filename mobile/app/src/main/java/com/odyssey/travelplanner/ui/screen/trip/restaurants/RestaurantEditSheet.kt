package com.odyssey.travelplanner.ui.screen.trip.restaurants

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.screen.trip.lodging.AccommodationCalendarDialog
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyDangerBright
import com.odyssey.travelplanner.ui.theme.OdysseyError
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.OdysseyPurpleGradientEnd
import com.odyssey.travelplanner.ui.theme.OdysseyPurpleShadow
import com.odyssey.travelplanner.ui.theme.OdysseySheetScrim
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.dangerSurfaceColor
import com.odyssey.travelplanner.ui.theme.labelColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.surfaceVariantColor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun RestaurantEditSheet(
    restaurant: com.odyssey.travelplanner.data.Restaurant,
    tripId: String,
    cityOptions: List<String>,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val language = LocalLanguage.current
    var name by remember(restaurant.id) { mutableStateOf(restaurant.name) }
    var city by remember(restaurant.id) { mutableStateOf(restaurant.city) }
    var cuisine by remember(restaurant.id) { mutableStateOf(restaurant.note) }
    var dateTime by remember(restaurant.id) { mutableStateOf(restaurant.date) }
    var price by remember(restaurant.id) { mutableStateOf(restaurant.price) }
    var address by remember(restaurant.id) { mutableStateOf(restaurant.link) }
    var status by remember(restaurant.id) { mutableStateOf(restaurant.status.ifBlank { "хочу" }) }
    var priority by remember(restaurant.id) { mutableStateOf(restaurant.priority) }
    var saving by remember { mutableStateOf(false) }
    var uploadingPhoto by remember(restaurant.id) { mutableStateOf(false) }
    var deleting by remember(restaurant.id) { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var cityPickerOpen by remember(restaurant.id) { mutableStateOf(false) }
    var datePickerOpen by remember(restaurant.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            uploadingPhoto = true
            message = null
            runCatching {
                val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                uris.forEachIndexed { index, uri ->
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0447\u0438\u0442\u0430\u0442\u044c \u0438\u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u0435")
                    if (index == 0) {
                        repository.replaceRestaurantCoverPhoto(tripId, restaurant.id, bytes)
                    } else {
                        repository.addRestaurantPhoto(tripId, restaurant.id, bytes)
                    }
                }
            }.onSuccess {
                uploadingPhoto = false
                onSaved()
            }.onFailure {
                uploadingPhoto = false
                message = it.message ?: localized(
                    language,
                    "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0444\u043e\u0442\u043e",
                    "Could not upload photo",
                    "No se pudo subir la foto",
                    "Foto konnte nicht hochgeladen werden",
                )
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = maxWidth.value / 368f
        fun d(value: Float) = (value * scale).dp
        fun s(value: Float) = (value * scale).sp
        val busy = saving || uploadingPhoto || deleting

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d(704f))
                .verticalScroll(rememberScrollState()),
        ) {
            Box(Modifier.fillMaxWidth().height(d(830f))) {
            Box(
                modifier = Modifier
                    .offset(x = d(164f), y = d(12f))
                    .size(d(40f), d(4f))
                    .clip(RoundedCornerShape(d(2f)))
                    .background(Color(0xFFE6E6EC)),
            )
            Text(
                text = localized("Редактировать ресторан", "Edit restaurant", "Editar restaurante", "Restaurant bearbeiten"),
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
                modifier = Modifier
                    .offset(x = d(16f), y = d(32f))
                    .width(d(292f))
                    .height(d(34f)),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = d(318f), y = d(48f))
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

            RestaurantAddField(
                label = localized("Название ресторана", "Restaurant name", "Nombre del restaurante", "Name des Restaurants"),
                value = name,
                placeholder = localized("Название ресторана", "Restaurant name", "Nombre del restaurante", "Name des Restaurants"),
                scale = scale,
                valueWeight = FontWeight.W700,
                modifier = Modifier
                    .offset(x = d(16f), y = d(118f))
                    .width(d(336f)),
                onValueChange = { name = it },
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(d(12f)),
                modifier = Modifier
                    .offset(x = d(16f), y = d(211f))
                    .width(d(336f)),
            ) {
                RestaurantAddField(
                    label = localized("Город", "City", "Ciudad", "Stadt"),
                    value = if (city.isBlank()) "" else localizedCityName(city),
                    placeholder = localized("Выберите город", "Choose a city", "Elija una ciudad", "Stadt auswählen"),
                    scale = scale,
                    trailingChevron = true,
                    readOnly = true,
                    modifier = Modifier.width(d(162f)),
                    onClick = { cityPickerOpen = true },
                    onValueChange = { city = it },
                )
                RestaurantAddField(
                    label = localized("Кухня", "Cuisine", "Cocina", "Küche"),
                    value = cuisine,
                    placeholder = localized("Например, итальянская", "For example, Italian", "Por ejemplo, italiana", "Zum Beispiel italienisch"),
                    scale = scale,
                    modifier = Modifier.width(d(162f)),
                    onValueChange = { cuisine = it },
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(12f)),
                modifier = Modifier
                    .offset(x = d(16f), y = d(304f))
                    .width(d(336f)),
            ) {
                RestaurantAddField(
                    label = localized("Дата и время", "Date and time", "Fecha y hora", "Datum und Uhrzeit"),
                    value = dateTime,
                    placeholder = localized("Выберите дату", "Choose date", "Elija fecha", "Datum auswählen"),
                    scale = scale,
                    trailingChevron = true,
                    readOnly = true,
                    modifier = Modifier.width(d(162f)),
                    onClick = { datePickerOpen = true },
                    onValueChange = { dateTime = it },
                )
                RestaurantAddPriceField(
                    selected = price,
                    scale = scale,
                    modifier = Modifier.width(d(162f)),
                    onSelect = { price = it },
                )
            }
            RestaurantAddField(
                label = localized("Адрес", "Address", "Dirección", "Adresse"),
                value = address,
                placeholder = localized("Адрес ресторана", "Restaurant address", "Dirección del restaurante", "Adresse des Lokals"),
                scale = scale,
                valueWeight = FontWeight.W600,
                modifier = Modifier
                    .offset(x = d(16f), y = d(397f))
                    .width(d(336f)),
                onValueChange = { address = it },
            )
            Text(
                text = localized("Статус", "Status", "Estado", "Status"),
                color = labelColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = s(13f),
                lineHeight = s(18f),
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                modifier = Modifier
                    .offset(x = d(16f), y = d(490f))
                    .width(d(336f))
                    .height(d(18f)),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(9f)),
                modifier = Modifier
                    .offset(x = d(16f), y = d(516f))
                    .width(d(336f))
                    .height(d(45f)),
            ) {
                RestaurantEditStatusChip(
                    label = localized("хочу", "want", "quiero", "möchte"),
                    value = "хочу",
                    selected = status == "хочу",
                    scale = scale,
                    modifier = Modifier.weight(1f),
                    onClick = { status = it },
                )
                RestaurantEditStatusChip(
                    label = localized("бронь", "reserved", "reserva", "Reservierung"),
                    value = "бронь",
                    selected = status == "бронь",
                    scale = scale,
                    modifier = Modifier.weight(1f),
                    onClick = { status = it },
                )
                RestaurantEditStatusChip(
                    label = localized("были", "visited", "visitado", "besucht"),
                    value = "были",
                    selected = status == "были",
                    scale = scale,
                    modifier = Modifier.weight(1f),
                    onClick = { status = it },
                )
            }
            RestaurantAddStatusChip(
                label = localized("🔥 Приоритет", "🔥 Priority", "🔥 Prioridad", "🔥 Priorität"),
                value = "priority",
                selected = priority,
                width = 124.1f,
                scale = scale,
                onClick = { priority = !priority },
                modifier = Modifier.offset(x = d(16f), y = d(563f)),
            )
            RestaurantAddField(
                label = localized("\u0424\u043e\u0442\u043e", "Photos", "Fotos", "Fotos"),
                value = if (restaurant.photos.isEmpty()) {
                    ""
                } else {
                    localized(
                        "${restaurant.photos.size} \u0444\u043e\u0442\u043e",
                        "${restaurant.photos.size} photos",
                        "${restaurant.photos.size} fotos",
                        "${restaurant.photos.size} Fotos",
                    )
                },
                placeholder = localized("\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0444\u043e\u0442\u043e", "Add photo", "A\u00f1adir foto", "Foto hinzuf\u00fcgen"),
                scale = scale,
                trailingChevron = true,
                readOnly = true,
                modifier = Modifier
                    .offset(x = d(16f), y = d(610f))
                    .width(d(336f)),
                onClick = { if (!busy) photoPicker.launch("image/*") },
                onValueChange = {},
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(d(11f)),
                modifier = Modifier
                    .offset(x = d(16f), y = d(703f))
                    .width(d(336f))
                    .height(d(53f)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(d(46f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(d(11f)))
                        .background(dangerSurfaceColor())
                        .clickable(enabled = !busy) {
                            scope.launch {
                                deleting = true
                                message = null
                                runCatching {
                                    SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteTripItem(tripId, "restaurants", restaurant.id)
                                }.onSuccess {
                                    deleting = false
                                    onSaved()
                                }.onFailure {
                                    deleting = false
                                    message = it.message ?: localized(
                                        language,
                                        "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0443\u0434\u0430\u043b\u0438\u0442\u044c \u0440\u0435\u0441\u0442\u043e\u0440\u0430\u043d",
                                        "Could not delete restaurant",
                                        "No se pudo eliminar el restaurante",
                                        "Restaurant konnte nicht gel\u00f6scht werden",
                                    )
                                }
                            }
                        },
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = localized("\u0423\u0434\u0430\u043b\u0438\u0442\u044c", "Delete", "Eliminar", "L\u00f6schen"),
                        tint = OdysseyDangerBright,
                        modifier = Modifier.size(d(19f)),
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
                        .weight(1.3f)
                        .fillMaxHeight()
                        .shadow(
                            d(8f),
                            RoundedCornerShape(d(15f)),
                            clip = false,
                            ambientColor = OdysseyPurpleShadow,
                            spotColor = OdysseyPurpleShadow,
                        )
                        .clip(RoundedCornerShape(d(15f)))
                        .background(Brush.linearGradient(listOf(primaryColor(), OdysseyPurpleGradientEnd)))
                        .clickable(enabled = !busy) {
                            scope.launch {
                                saving = true
                                message = null
                                runCatching {
                                    SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateRestaurantDetailsRich(
                                        tripId = tripId,
                                        restaurantId = restaurant.id,
                                        input = com.odyssey.travelplanner.data.RestaurantInput(
                                            name = name,
                                            city = city,
                                            status = status,
                                            note = cuisine,
                                            price = price,
                                            link = address,
                                            date = dateTime,
                                            priority = priority,
                                        ),
                                    )
                                }.onSuccess {
                                    onSaved()
                                }.onFailure {
                                    message = it.message ?: localized(language, "Не удалось сохранить ресторан", "Could not save restaurant", "No se pudo guardar el restaurante", "Restaurant konnte nicht gespeichert werden")
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
                    color = OdysseyError,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = s(11f),
                    lineHeight = s(15f),
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        modifier = Modifier
                        .offset(x = d(16f), y = d(765f))
                        .width(d(336f)),
                )
            }
        }
    }
    }
    if (datePickerOpen) {
        AccommodationCalendarDialog(
            initialValue = dateTime,
            onDismiss = { datePickerOpen = false },
            onConfirm = { selectedDate ->
                dateTime = selectedDate
                datePickerOpen = false
            },
        )
    }
    if (cityPickerOpen) {
        ModalBottomSheet(
            onDismissRequest = { cityPickerOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = OdysseySheetScrim,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            dragHandle = null,
        ) {
            RestaurantAddCitySheet(
                options = cityOptions,
                selectedCity = city,
                onSelect = { option ->
                    city = option
                    cityPickerOpen = false
                },
                onClose = { cityPickerOpen = false },
            )
        }
    }
}

@Composable
internal fun RestaurantEditStatusChip(
    label: String,
    value: String,
    selected: Boolean,
    scale: Float,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    fun s(value: Float) = (value * scale).sp
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(d(12f)))
            .background(if (selected) primaryColor() else cardSurfaceColor())
            .border(d(1.5f), if (selected) primaryColor() else contentBorderColor(), RoundedCornerShape(d(12f)))
            .clickable { onClick(value) },
    ) {
        Text(
            text = label,
            color = if (selected) primaryContentColor() else secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(14f),
            lineHeight = s(19f),
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

