package com.odyssey.travelplanner.ui.screen.trip.restaurants

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.odyssey.travelplanner.ui.common.FullScreenPhotoViewer
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.icons.OdysseyChevronDown
import com.odyssey.travelplanner.ui.icons.OdysseyPlusIcon
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.labelColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.surfaceVariantColor
import com.odyssey.travelplanner.ui.theme.trackColor

@Composable
internal fun RestaurantAddSheet(
    name: String,
    city: String,
    cuisine: String,
    dateTime: String,
    price: String,
    address: String,
    status: String,
    priority: Boolean,
    photoUris: List<Uri>,
    saving: Boolean,
    message: String?,
    onNameChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onCityPickerOpen: () -> Unit,
    onDatePickerOpen: () -> Unit,
    onCuisineChange: (String) -> Unit,
    onDateTimeChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onPriorityChange: () -> Unit,
    onPickPhoto: () -> Unit,
    onCatalogOpen: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = maxWidth.value / 368f
        fun d(value: Float) = (value * scale).dp
        fun s(value: Float) = (value * scale).sp
        val inputTextStyle = androidx.compose.ui.text.TextStyle(
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W600,
            fontSize = s(15f),
            lineHeight = s(20f),
            platformStyle = OdysseyNoFontPadding,
        )
        val labelStyle = androidx.compose.ui.text.TextStyle(
            color = labelColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13f),
            lineHeight = s(18f),
            platformStyle = OdysseyNoFontPadding,
        )
        val photoBorderColor = contentBorderColor()
        val scrollState = rememberScrollState()
        val photoScrollState = rememberScrollState()
        var fullScreenPhotoIndex by remember(photoUris) { mutableStateOf<Int?>(null) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d(704f))
                .verticalScroll(scrollState),
        ) {
            Box(Modifier.fillMaxWidth().height(d(926f))) {
                Box(
                    modifier = Modifier
                        .offset(x = d(156.5f), y = d(12f))
                        .size(d(40f), d(4f))
                        .clip(RoundedCornerShape(d(2f)))
                        .background(contentBorderColor()),
                )

                Text(
                    text = localized("Новый ресторан", "New restaurant", "Nuevo restaurante", "Neues Restaurant"),
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = s(24f),
                    lineHeight = s(33f),
                    letterSpacing = (-0.24f * scale).sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
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
                    Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = secondaryTextColor(), modifier = Modifier.size(d(16f)))
                }

                Text(
                    text = localized("Фотографии", "Photos", "Fotos", "Fotos"),
                    style = labelStyle,
                    modifier = Modifier.offset(x = d(16f), y = d(82f)).width(d(321f)).height(d(18f)),
                )
                Box(
                    modifier = Modifier
                        .offset(x = d(16f), y = d(108f))
                        .width(d(321f))
                        .height(d(172f))
                        .horizontalScroll(photoScrollState),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(d(10f)),
                        modifier = Modifier.height(d(168f)),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(d(240f))
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
                                        style = Stroke(
                                            width = stroke,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(d(6f).toPx(), d(4f).toPx())),
                                        ),
                                    )
                                }
                                .clickable(onClick = onPickPhoto),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Image, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(d(26f)))
                                Text(
                                    text = localized("Обложка — перетащите фото\nили выберите файл", "Cover — drag a photo\nor choose a file", "Portada — arrastre una foto\no elija un archivo", "Cover — Foto ziehen\noder Datei auswählen"),
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

                        Box(
                            modifier = Modifier
                                .width(d(128f))
                                .height(d(168f))
                                .clip(RoundedCornerShape(d(14f)))
                                .background(Color(0xFFE9E7F4))
                                .clickable(enabled = !saving && photoUris.isNotEmpty()) { fullScreenPhotoIndex = 0 },
                        ) {
                            photoUris.firstOrNull()?.let { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = localized("Обложка ресторана", "Restaurant cover", "Portada del restaurante", "Restaurant-Titelbild"),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Text(
                                text = localized("Обложка", "Cover", "Portada", "Cover"),
                                color = Color.White,
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W800,
                                fontSize = s(10f),
                                lineHeight = s(14f),
                                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = d(8f), top = d(8f))
                                    .background(Color(0x8C141419), RoundedCornerShape(d(20f)))
                                    .padding(horizontal = d(7f), vertical = d(3f)),
                            )
                        }

                        photoUris.drop(1).forEachIndexed { index, uri ->
                            Box(
                                modifier = Modifier
                                    .width(d(128f))
                                    .height(d(168f))
                                    .clip(RoundedCornerShape(d(14f)))
                                    .background(Color(0xFFE9E7F4))
                                    .clickable(enabled = !saving) { fullScreenPhotoIndex = index + 1 },
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = localized("Фото ресторана ${index + 2}", "Restaurant photo ${index + 2}", "Foto del restaurante ${index + 2}", "Restaurantfoto ${index + 2}"),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                Text(
                                    text = "${index + 2}",
                                    color = Color.White,
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W800,
                                    fontSize = s(10f),
                                    lineHeight = s(14f),
                                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = d(8f), top = d(8f))
                                        .background(Color(0x8C141419), RoundedCornerShape(d(20f)))
                                        .padding(horizontal = d(7f), vertical = d(3f)),
                                )
                            }
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
                                        style = Stroke(
                                            width = stroke,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(d(6f).toPx(), d(4f).toPx())),
                                        ),
                                    )
                                }
                                .clickable(onClick = onPickPhoto),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OdysseyPlusIcon(d(18f))
                                Text(
                                    text = localized("Добавить", "Add", "Añadir", "Hinzufügen"),
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
                    }
                }

                Box(
                    modifier = Modifier
                        .offset(x = d(16f), y = d(288f))
                        .width(d(321f))
                        .height(d(42f))
                        .clip(RoundedCornerShape(d(12f)))
                        .border(d(1f), primaryColor().copy(alpha = 0.45f), RoundedCornerShape(d(12f)))
                        .background(primaryColor().copy(alpha = 0.08f))
                        .clickable(onClick = onCatalogOpen),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d(7f))) {
                        Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(d(17f)))
                        Text(
                            localized("Выбрать из каталога", "Choose from catalog", "Elegir del catálogo", "Aus Katalog wählen"),
                            color = primaryColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = s(12f),
                            lineHeight = s(16f),
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                    }
                }

                RestaurantAddField(
                    label = localized("Название", "Name", "Nombre", "Name"),
                    value = name,
                    placeholder = localized("Название места", "Restaurant name", "Nombre del lugar", "Name des Lokals"),
                    scale = scale,
                    modifier = Modifier.offset(x = d(16f), y = d(346f)).width(d(321f)),
                    onValueChange = onNameChange,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d(12f)),
                    modifier = Modifier.offset(x = d(16f), y = d(439f)).width(d(321f)),
                ) {
                    RestaurantAddField(
                        label = localized("Город", "City", "Ciudad", "Stadt"),
                        value = if (city.isBlank()) "" else localizedCityName(city),
                        placeholder = localized("Выберите город", "Choose a city", "Elija una ciudad", "Stadt auswählen"),
                        scale = scale,
                        trailingChevron = true,
                        readOnly = true,
                        onClick = onCityPickerOpen,
                        modifier = Modifier.width(d(154.5f)),
                        onValueChange = { onCityChange(it) },
                    )
                    RestaurantAddField(
                        label = localized("Кухня", "Cuisine", "Cocina", "Küche"),
                        value = cuisine,
                        placeholder = localized("Например, итальянская", "For example, Italian", "Por ejemplo, italiana", "Zum Beispiel italienisch"),
                        scale = scale,
                        modifier = Modifier.width(d(154.5f)),
                        onValueChange = onCuisineChange,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d(12f)),
                    modifier = Modifier.offset(x = d(16f), y = d(532f)).width(d(321f)),
                ) {
                    RestaurantAddField(
                        label = localized("Дата и время", "Date and time", "Fecha y hora", "Datum und Uhrzeit"),
                        value = dateTime,
                        placeholder = localized("Выберите дату", "Choose date", "Elija una fecha", "Datum auswählen"),
                        scale = scale,
                        trailingChevron = true,
                        readOnly = true,
                        onClick = onDatePickerOpen,
                        modifier = Modifier.width(d(154.5f)),
                        onValueChange = { onDateTimeChange(it) },
                    )
                    RestaurantAddPriceField(
                        selected = price,
                        scale = scale,
                        modifier = Modifier.width(d(154.5f)),
                        onSelect = onPriceChange,
                    )
                }
                RestaurantAddField(
                    label = localized("Адрес", "Address", "Dirección", "Adresse"),
                    value = address,
                    placeholder = localized("Адрес ресторана", "Restaurant address", "Dirección del restaurante", "Adresse des Lokals"),
                    scale = scale,
                    modifier = Modifier.offset(x = d(16f), y = d(627f)).width(d(321f)),
                    onValueChange = onAddressChange,
                )

                Text(
                    text = localized("Статус", "Status", "Estado", "Status"),
                    style = labelStyle,
                    modifier = Modifier.offset(x = d(16f), y = d(720f)).width(d(321f)).height(d(18f)),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d(9f)),
                    modifier = Modifier.offset(x = d(16f), y = d(746f)).height(d(38f)),
                ) {
                    RestaurantAddStatusChip(localized("хочу", "want", "quiero", "möchte"), "хочу", status == "хочу", 61.4f, scale, onStatusChange)
                    RestaurantAddStatusChip(localized("бронь", "reserved", "reserva", "Reservierung"), "бронь", status == "бронь", 71.4f, scale, onStatusChange)
                    RestaurantAddStatusChip(localized("были", "visited", "visitado", "besucht"), "были", status == "были", 65.1f, scale, onStatusChange)
                }
                RestaurantAddStatusChip(
                    label = localized("🔥 Приоритет", "🔥 Priority", "🔥 Prioridad", "🔥 Priorität"),
                    value = "priority",
                    selected = priority,
                    width = 124.1f,
                    scale = scale,
                    onClick = { onPriorityChange() },
                    modifier = Modifier.offset(x = d(16f), y = d(793f)),
                )

                if (message != null) {
                    Text(
                        text = message,
                        color = Color(0xFFE0524B),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W700,
                        fontSize = s(11f),
                        lineHeight = s(15f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        modifier = Modifier.offset(x = d(16f), y = d(834f)).width(d(336f)),
                    )
                }

                Box(
                    modifier = Modifier
                        .offset(x = d(16f), y = d(855f))
                        .width(d(135.3f))
                        .height(d(53f))
                        .clip(RoundedCornerShape(d(15f)))
                        .border(d(1f), contentBorderColor(), RoundedCornerShape(d(15f)))
                        .background(cardSurfaceColor())
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
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
                    modifier = Modifier
                        .offset(x = d(162.3f), y = d(855f))
                        .width(d(174.7f))
                        .height(d(53f))
                        .shadow(d(8f), RoundedCornerShape(d(15f)), clip = false, ambientColor = Color(0x4D6C5CE7), spotColor = Color(0x4D6C5CE7))
                        .clip(RoundedCornerShape(d(15f)))
                        .background(Brush.linearGradient(listOf(primaryColor(), Color(0xFF7D6CF0))))
                        .clickable(enabled = !saving, onClick = onSave),
                    contentAlignment = Alignment.Center,
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
        fullScreenPhotoIndex?.let { initialIndex ->
            if (photoUris.isNotEmpty()) {
                FullScreenPhotoViewer(
                    photos = photoUris,
                    initialIndex = initialIndex,
                    accommodationName = name.ifBlank { localized("Ресторан", "Restaurant", "Restaurante", "Restaurant") },
                    onDismiss = { fullScreenPhotoIndex = null },
                )
            }
        }
    }
}

@Composable
internal fun RestaurantAddField(
    label: String,
    value: String,
    placeholder: String,
    scale: Float,
    modifier: Modifier = Modifier,
    trailingChevron: Boolean = false,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    valueWeight: FontWeight = FontWeight.W600,
    onValueChange: (String) -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    fun s(value: Float) = (value * scale).sp
    val textStyle = androidx.compose.ui.text.TextStyle(
        color = contentTextColor(),
        fontFamily = Manrope,
        fontWeight = valueWeight,
        fontSize = s(15f),
        lineHeight = s(20f),
        platformStyle = OdysseyNoFontPadding,
    )
    Column(modifier = modifier.height(d(77f))) {
        Text(
            text = label,
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13f),
            lineHeight = s(18f),
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.height(d(18f)),
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(d(8f)))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d(51f))
                .clip(RoundedCornerShape(d(14f)))
                .background(cardSurfaceColor())
                .border(d(1f), contentBorderColor(), RoundedCornerShape(d(14f))),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = readOnly,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(primaryColor()),
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = d(15f), end = if (trailingChevron) d(34f) else d(15f)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                 color = secondaryTextColor(),
                                 fontFamily = Manrope,
                                 fontWeight = FontWeight.W600,
                                 fontSize = s(13f),
                                 lineHeight = s(15f),
                                 style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                 maxLines = 2,
                                 softWrap = true,
                                 overflow = TextOverflow.Clip,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (trailingChevron) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(end = d(12f))
                        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    OdysseyChevronDown(d(16f), secondaryTextColor())
                }
            }
        }
    }
}

@Composable
internal fun RestaurantAddPriceField(
    selected: String,
    scale: Float,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    fun s(value: Float) = (value * scale).sp
    Column(modifier = modifier.height(d(79f))) {
        Text(
            text = localized("Средний чек", "Average price", "Precio medio", "Durchschnittspreis"),
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13f),
            lineHeight = s(18f),
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.height(d(18f)),
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(d(8f)))
        Row(
            horizontalArrangement = Arrangement.spacedBy(d(3.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(d(51f))
                .clip(RoundedCornerShape(d(14f)))
                .background(trackColor())
                .padding(d(4f)),
        ) {
            listOf("€", "€€", "€€€", "€€€€").forEach { option ->
                val active = option == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(d(43f))
                        .shadow(if (active) d(2f) else 0.dp, RoundedCornerShape(d(11f)), clip = false, ambientColor = Color(0x1A000000), spotColor = Color(0x1A000000))
                        .clip(RoundedCornerShape(d(11f)))
                        .background(if (active) cardSurfaceColor() else Color.Transparent)
                        .clickable { onSelect(option) },
                ) {
                    Text(
                        text = option,
                        color = if (active) contentTextColor() else secondaryTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(12f),
                        lineHeight = s(16f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
internal fun RestaurantAddStatusChip(
    label: String,
    value: String,
    selected: Boolean,
    width: Float,
    scale: Float,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun d(value: Float) = (value * scale).dp
    fun s(value: Float) = (value * scale).sp
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(d(width))
            .height(d(38f))
            .clip(RoundedCornerShape(d(12f)))
            .background(if (selected) primaryColor() else cardSurfaceColor())
            .border(d(1f), if (selected) primaryColor() else contentBorderColor(), RoundedCornerShape(d(12f)))
            .clickable { onClick(value) },
    ) {
        Text(
            text = label,
            color = if (selected) primaryContentColor() else secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13.5f),
            lineHeight = s(18f),
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            maxLines = 1,
            softWrap = false,
        )
    }
}

