package com.odyssey.travelplanner.ui.screen.trip.sights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.OpenInNew
import com.mapbox.geojson.Point
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.Sight
import com.odyssey.travelplanner.data.SightCatalogEntry
import com.odyssey.travelplanner.data.resolveSightLinkCoordinates
import kotlinx.coroutines.launch
import java.util.Locale
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.i18n.localizedSightCategory
import com.odyssey.travelplanner.ui.i18n.localizedSightDescription
import com.odyssey.travelplanner.ui.i18n.localizedSightInfo
import com.odyssey.travelplanner.ui.i18n.localizedSightName
import com.odyssey.travelplanner.ui.screen.auth.AuthField
import com.odyssey.travelplanner.ui.screen.trip.route.RouteEditorField
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondarySurfaceColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor

@Composable
internal fun SightCard(
    sight: com.odyssey.travelplanner.data.Sight,
    catalogEntry: SightCatalogEntry? = null,
    uploading: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpenPhoto: () -> Unit,
    canEdit: Boolean = true,
    onEdit: () -> Unit,
) {
    val displayedName = localizedSightName(sight.name)
    val displayedCategory = localizedSightCategory(
        sight.category.trim().takeIf { it.isNotBlank() }
            ?: catalogEntry?.category?.trim().orEmpty(),
    ).uppercase(Locale.ROOT)
    val displayedRating = catalogEntry?.rating ?: sight.rating
    val displayedRatingCount = catalogEntry?.ratingCount
    val language = LocalLanguage.current
    val uriHandler = LocalUriHandler.current
    val cardShape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(if (selected) primaryColor().copy(alpha = 0.08f) else secondarySurfaceColor())
            .border(if (selected) 2.dp else 1.dp, if (selected) primaryColor().copy(alpha = 0.52f) else contentBorderColor(), cardShape)
            .padding(8.dp),
    ) {
        Box(modifier = Modifier.size(width = 96.dp, height = 112.dp).clip(RoundedCornerShape(14.dp))) {
            SightPhoto(
                sight = sight,
                modifier = Modifier.fillMaxSize(),
                onClick = onOpenPhoto.takeIf { !uploading },
            )
        }
        Column(modifier = Modifier.weight(1f).clickable { onSelect() }) {
            Text(
                displayedCategory,
                color = primaryColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 9.5.sp,
                lineHeight = 12.sp,
                letterSpacing = 0.2.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                displayedName,
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (displayedRating != null || displayedRatingCount != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 5.dp),
                ) {
                    Text("★", color = Color(0xFFFFB52E), fontSize = 14.sp, fontWeight = FontWeight.W800)
                    displayedRating?.let {
                        Text(
                            String.format(Locale.ROOT, "%.1f", it),
                            color = contentTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    displayedRatingCount?.let {
                        Text(
                            "· ${catalogRatingCountLabel(it, language)}",
                            color = secondaryTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W600,
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Text(
                localizedSightInfo(sight.name, sight.description, sight.category),
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (sight.link.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable { uriHandler.openUri(sight.link) },
                ) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(13.dp))
                    Text(
                        localized("Открыть ссылку", "Open link", "Abrir enlace", "Link öffnen"),
                        color = primaryColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        if (canEdit) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tintedSurfaceColor())
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = localized("Редактировать", "Edit", "Editar", "Bearbeiten"),
                    tint = primaryColor(),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun EditSightPanel(
    sight: com.odyssey.travelplanner.data.Sight,
    tripId: String,
    dayCities: List<String>,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalLanguage.current
    val displayedName = localizedSightName(sight.name)
    val displayedDescription = localizedSightDescription(sight.description)
    var name by remember(sight.id, language) { mutableStateOf(displayedName) }
    var description by remember(sight.id, language) { mutableStateOf(displayedDescription) }
    var link by remember(sight.id, language) { mutableStateOf(sight.link) }
    var selectedDay by remember(sight.id) { mutableStateOf(sight.walkDay.coerceAtLeast(1)) }
    var routeDayMenuOpen by remember(sight.id) { mutableStateOf(false) }
    val routeDayOptions = remember(dayCities) {
        dayCities.mapIndexed { index, dayCity -> index + 1 to dayCity }
    }
    val effectiveSelectedDay = selectedDay.takeIf { it in 1..routeDayOptions.size } ?: sight.walkDay.coerceAtLeast(1)
    val selectedDayCity = routeDayOptions.firstOrNull { it.first == effectiveSelectedDay }?.second
        .orEmpty()
        .ifBlank { sight.city }
    var selectedPoint by remember(sight.id) {
        mutableStateOf(
            sight.longitude?.let { longitude ->
                sight.latitude?.let { latitude -> Point.fromLngLat(longitude, latitude) }
            },
        )
    }
    var locationChanged by remember(sight.id) { mutableStateOf(false) }
    var locationPickerOpen by remember(sight.id) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cardSurfaceColor()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(localized("Редактировать место", "Edit sight", "Editar lugar", "Ort bearbeiten"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
        AuthField(localized("Название", "Name", "Nombre", "Name"), localized("Название", "Name", "Nombre", "Name"), name) { name = it }
        SightRouteDayField(
            selectedDay = effectiveSelectedDay,
            selectedCity = selectedDayCity,
            options = routeDayOptions,
            expanded = routeDayMenuOpen,
            onExpandedChange = { routeDayMenuOpen = it },
            onDaySelected = {
                if (it != effectiveSelectedDay) {
                    selectedPoint = null
                    locationChanged = true
                }
                selectedDay = it
            },
        )
        SightLocationField(
            point = selectedPoint,
            onClick = { locationPickerOpen = true },
            modifier = Modifier.fillMaxWidth(),
        )
        AuthField(localized("Описание", "Description", "Descripción", "Beschreibung"), localized("Что важно увидеть", "What is important to see", "Qué es importante ver", "Was sehenswert ist"), description) { description = it }
        RouteEditorField(
            label = localized("Ссылка на достопримечательность", "Sight link", "Enlace del lugar", "Link zur Sehenswürdigkeit"),
            value = link,
            onValueChange = { value ->
                link = value
                sightLinkPoint(value)?.let {
                    selectedPoint = it
                    locationChanged = true
                }
                if (value.isBlank() && sight.link.isBlank()) selectedPoint = null
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = "https://maps.app.goo.gl/...",
        )
        if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = secondarySurfaceColor(), contentColor = contentTextColor()), shape = RoundedCornerShape(11.dp)) { Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
            Button(onClick = {
                scope.launch {
                    saving = true
                    val savedName = name.trim().takeUnless { it == displayedName } ?: sight.name.trim()
                    val savedCity = selectedDayCity.trim().ifBlank { sight.city.trim() }
                    val savedDescription = description.trim().takeUnless { it == displayedDescription } ?: sight.description.trim()
                    val savedLink = link.trim()
                    val linkChanged = savedLink != sight.link.trim()
                    var savedPoint = selectedPoint
                    if (linkChanged && savedLink.isNotBlank()) {
                        savedPoint = resolveSightLinkCoordinates(savedLink)?.let { coordinates ->
                            Point.fromLngLat(coordinates.longitude, coordinates.latitude)
                        } ?: error(localized(language, "Не удалось определить точку по ссылке. Выберите точку на карте.", "Could not find a map point in this link. Choose a point on the map.", "No se pudo encontrar el punto en el enlace. Elija un punto en el mapa.", "Im Link wurde kein Kartenpunkt gefunden. Wählen Sie einen Punkt auf der Karte."))
                        locationChanged = true
                    }
                    runCatching {
                        SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateSightDetailsRich(
                            id = tripId,
                            sightId = sight.id,
                            name = savedName,
                            city = savedCity,
                            category = sight.category.trim(),
                            description = savedDescription,
                            walkDay = effectiveSelectedDay,
                            longitude = savedPoint?.longitude(),
                            latitude = savedPoint?.latitude(),
                            locationChanged = locationChanged,
                            link = savedLink,
                        )
                    }
                        .onSuccess { onSaved() }
                        .onFailure { message = it.message ?: localized(language, "Не удалось сохранить место", "Could not save sight", "No se pudo guardar el lugar", "Ort konnte nicht gespeichert werden") }
                    saving = false
                }
            }, enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()), shape = RoundedCornerShape(11.dp)) { Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
        }
    }
    if (locationPickerOpen) {
        ModalBottomSheet(
            onDismissRequest = { locationPickerOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            dragHandle = null,
        ) {
            SightLocationPickerSheet(
                city = selectedDayCity.ifBlank { sight.city },
                initialPoint = selectedPoint,
                onClose = { locationPickerOpen = false },
                onConfirm = { point ->
                    selectedPoint = point
                    locationChanged = true
                    locationPickerOpen = false
                },
            )
        }
    }
}

@Composable
internal fun SightRouteDayField(
    selectedDay: Int,
    selectedCity: String,
    options: List<Pair<Int, String>>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDaySelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            localized("Город и день маршрута", "City and route day", "Ciudad y día de ruta", "Stadt und Reisetag"),
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 12.sp,
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, contentBorderColor(), RoundedCornerShape(14.dp))
                    .background(cardSurfaceColor())
                    .clickable(enabled = options.isNotEmpty()) { onExpandedChange(true) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        localized("День $selectedDay", "Day $selectedDay", "Día $selectedDay", "Tag $selectedDay"),
                        color = secondaryTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W700,
                        fontSize = 11.sp,
                    )
                    Text(
                        localizedCityName(selectedCity).ifBlank { localized("Город не указан", "City not set", "Ciudad no indicada", "Stadt nicht festgelegt") },
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = localized("Выбрать день маршрута", "Choose route day", "Elegir día de ruta", "Reisetag auswählen"),
                    tint = if (options.isNotEmpty()) primaryColor() else secondaryTextColor(),
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                containerColor = cardSurfaceColor(),
                shadowElevation = 12.dp,
            ) {
                options.forEach { (day, city) ->
                    val selected = day == selectedDay
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    localized("День $day", "Day $day", "Día $day", "Tag $day"),
                                    color = if (selected) primaryColor() else secondaryTextColor(),
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W800,
                                    fontSize = 11.sp,
                                )
                                Text(
                                    localizedCityName(city).ifBlank { localized("Город не указан", "City not set", "Ciudad no indicada", "Stadt nicht festgelegt") },
                                    color = if (selected) primaryColor() else contentTextColor(),
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W700,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        onClick = {
                            onDaySelected(day)
                            onExpandedChange(false)
                        },
                    )
                }
            }
        }
    }
}

