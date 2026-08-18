package com.odyssey.travelplanner.ui.screen.trip.route

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Key
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import com.odyssey.travelplanner.data.CityCatalogRepository
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.TripOverview
import com.odyssey.travelplanner.data.cityFlag
import com.odyssey.travelplanner.data.countryFlag
import com.odyssey.travelplanner.ui.common.RouteEditorDateField
import com.odyssey.travelplanner.ui.domain.cityFilterKey
import com.odyssey.travelplanner.ui.domain.routeDateParts
import com.odyssey.travelplanner.ui.domain.routeDurationDays
import com.odyssey.travelplanner.ui.domain.routeEditorDateIso
import com.odyssey.travelplanner.ui.domain.routeEditorDateLabel
import com.odyssey.travelplanner.ui.domain.routeEditorDateValues
import com.odyssey.travelplanner.ui.domain.routeTiming
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.i18n.localizedRouteSummary
import com.odyssey.travelplanner.ui.screen.trip.lodging.AccommodationCalendarDialog
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyFontPadding
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.dangerSurfaceColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondarySurfaceColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun TripRouteContent(tripId: String, overview: TripOverview, canEdit: Boolean = true, onRouteAdded: () -> Unit) {
    val language = LocalLanguage.current
    val cityCatalogContext = LocalContext.current
    val cityCatalogRepository = remember(cityCatalogContext) { CityCatalogRepository(cityCatalogContext.assets) }
    val hapticFeedback = LocalHapticFeedback.current
    val routeListState = rememberLazyListState()
    val routeCities = remember(overview.routeLegs) {
        overview.routeLegs
            .flatMap { listOf(it.from, it.to) }
            .filter(String::isNotBlank)
            .distinct()
    }
    var resolvedRouteFlags by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var adding by remember { mutableStateOf(false) }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var checkIn by remember { mutableStateOf("") }
    var checkOut by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var mapsUrl by remember { mutableStateOf("") }
    var selectedDateIso by remember { mutableStateOf("") }
    var datePickerOpen by remember { mutableStateOf(false) }
    var editingLeg by remember { mutableStateOf<com.odyssey.travelplanner.data.RouteLeg?>(null) }
    var saving by remember { mutableStateOf(false) }
    var savingRouteOrder by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var orderedRouteIds by remember(overview.routeLegs) { mutableStateOf(overview.routeLegs.map { it.dayId }) }
    var draggedRouteId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }
    var dragInitialOrder by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val displayedRouteLegs = orderedRouteIds.mapNotNull { id ->
        overview.routeLegs.firstOrNull { it.dayId == id }
    } + overview.routeLegs.filterNot { leg -> orderedRouteIds.contains(leg.dayId) }

    fun updateRouteDrag(dragAmount: Float) {
        val draggedId = draggedRouteId ?: return
        dragOffsetPx += dragAmount
        val draggedKey = "route:$draggedId"
        val draggedInfo = routeListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggedKey } ?: return
        val draggedCenter = draggedInfo.offset + draggedInfo.size / 2f + dragOffsetPx
        val viewport = routeListState.layoutInfo
        when {
            draggedCenter < viewport.viewportStartOffset + 80f -> scope.launch { routeListState.scrollBy(-24f) }
            draggedCenter > viewport.viewportEndOffset - 80f -> scope.launch { routeListState.scrollBy(24f) }
        }
        val currentIndex = orderedRouteIds.indexOf(draggedId)
        if (currentIndex < 0) return
        val targetIndex = when {
            dragAmount > 0f -> currentIndex + 1
            dragAmount < 0f -> currentIndex - 1
            else -> return
        }
        if (targetIndex !in orderedRouteIds.indices) return
        val targetId = orderedRouteIds[targetIndex]
        val targetInfo = routeListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == "route:$targetId" }
            ?: return
        val targetCenter = targetInfo.offset + targetInfo.size / 2f
        val crossedTarget = if (dragAmount > 0f) {
            draggedCenter > targetCenter
        } else {
            draggedCenter < targetCenter
        }
        if (!crossedTarget) return

        // Keep the card under the finger when its slot changes in the list.
        dragOffsetPx -= (targetInfo.offset - draggedInfo.offset).toFloat()
        orderedRouteIds = orderedRouteIds.toMutableList().apply {
            removeAt(currentIndex)
            add(targetIndex, draggedId)
        }
    }

    fun finishRouteDrag() {
        val draggedId = draggedRouteId
        val finalOrder = orderedRouteIds
        val changed = draggedId != null && finalOrder != dragInitialOrder
        draggedRouteId = null
        dragOffsetPx = 0f
        dragInitialOrder = emptyList()
        if (!changed) return

        scope.launch {
            savingRouteOrder = true
            runCatching {
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                    .reorderRouteLegs(tripId, finalOrder)
            }.onSuccess {
                actionMessage = null
                onRouteAdded()
            }.onFailure {
                orderedRouteIds = overview.routeLegs.map { it.dayId }
                actionMessage = it.message ?: localized(
                    language,
                    "Не удалось сохранить порядок маршрута. Проверьте интернет и повторите попытку.",
                    "Could not save the route order. Check your connection and try again.",
                    "No se pudo guardar el orden de la ruta. Comprueba la conexión e inténtalo de nuevo.",
                    "Die Reihenfolge der Route konnte nicht gespeichert werden. Prüfen Sie die Verbindung und versuchen Sie es erneut.",
                )
            }
            savingRouteOrder = false
        }
    }

    LaunchedEffect(routeCities) {
        val unresolvedCities = routeCities.filter { cityFlag(it) == "📍" }
        resolvedRouteFlags = if (unresolvedCities.isEmpty()) {
            emptyMap()
        } else {
            cityCatalogRepository.findExact(unresolvedCities)
                .mapValues { (_, entry) -> countryFlag(entry.countryCode) ?: cityFlag(entry.russian) }
                .filterValues { it != "📍" }
        }
    }

    val cityCount = overview.overviewMapPoints
        .ifEmpty { overview.routeLegs.flatMap { listOf(it.from, it.to) } }
        .filter(String::isNotBlank)
        .distinctBy { cityFilterKey(it) }
        .size
    val tripDays = routeDurationDays(overview.dates)
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        state = routeListState,
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                localizedRouteSummary(tripDays, cityCount, language),
                color = primaryColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 11.sp,
            )
        }
        if (actionMessage != null) {
            item {
                Text(actionMessage!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
            }
        }
        if (overview.routeLegs.isEmpty()) {
            item {
                Text(localized("Переезды пока не добавлены", "No route legs added yet", "Aún no se han añadido trayectos", "Noch keine Etappen hinzugefügt"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp, modifier = Modifier.padding(vertical = 20.dp))
            }
        } else {
            itemsIndexed(displayedRouteLegs, key = { _, leg -> "route:${leg.dayId}" }) { index, leg ->
                RouteLegCard(
                    leg = leg,
                    dayIndex = index,
                    tripDates = overview.dates,
                    resolvedCityFlags = resolvedRouteFlags,
                    canEdit = canEdit,
                    dragEnabled = canEdit && !adding && !savingRouteOrder,
                    isDragging = draggedRouteId == leg.dayId,
                    dragOffsetPx = if (draggedRouteId == leg.dayId) dragOffsetPx else 0f,
                    onDragStart = {
                        if (canEdit && !adding && !savingRouteOrder) {
                            draggedRouteId = leg.dayId
                            dragInitialOrder = orderedRouteIds
                            dragOffsetPx = 0f
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDrag = ::updateRouteDrag,
                    onDragEnd = ::finishRouteDrag,
                    onEdit = {
                    if (canEdit) {
                        editingLeg = leg
                        from = leg.from
                        to = leg.to
                        checkIn = leg.checkIn
                        checkOut = leg.checkOut
                        notes = leg.notes
                        mapsUrl = leg.mapsUrl
                        selectedDateIso = routeEditorDateIso(leg.date, overview.dates, index, leg.dateDay, leg.dateMonth)
                        adding = true
                    }
                    },
                ) { itemId, completed ->
                    if (canEdit) scope.launch {
                        runCatching {
                            SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                                .updateRouteChecklist(tripId, leg.dayId, itemId, completed)
                        }.onSuccess {
                            actionMessage = null
                            onRouteAdded()
                        }.onFailure {
                            actionMessage = it.message ?: localized(language, "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 checklist. \u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442 \u0438 \u043f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u0435 \u043f\u043e\u043f\u044b\u0442\u043a\u0443.", "Could not save the checklist change. Check your connection and try again.", "No se pudo guardar el cambio del checklist. Comprueba la conexi\u00f3n e int\u00e9ntalo de nuevo.", "Die Checklisten\u00e4nderung konnte nicht gespeichert werden. Pr\u00fcfen Sie die Verbindung und versuchen Sie es erneut.")
                        }
                    }
                }
            }
        }
        if (canEdit) item {
            if (!adding) {
                Text(
                    localized("＋  Добавить день", "＋  Add day", "＋  Añadir día", "＋  Tag hinzufügen"),
                    color = primaryColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().height(55.dp).clip(RoundedCornerShape(16.dp)).background(tintedSurfaceColor()).drawBehind {
                        drawRoundRect(
                            color = Color(0xFFD7D0FF),
                            cornerRadius = CornerRadius(16.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                        )
                    }.clickable {
                        editingLeg = null
                        from = ""
                        to = ""
                        checkIn = ""
                        checkOut = ""
                        notes = ""
                        mapsUrl = ""
                        selectedDateIso = routeEditorDateIso("", overview.dates, overview.routeLegs.size, "", "")
                        adding = true
                    }.padding(top = 17.dp),
                )
            }
        }
    }
    if (canEdit && adding) {
        ModalBottomSheet(
            onDismissRequest = { adding = false; editingLeg = null; message = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
        ) {
            RouteLegEditorSheet(
                from = from,
                to = to,
                date = selectedDateIso,
                checkIn = checkIn,
                checkOut = checkOut,
                mapsUrl = mapsUrl,
                saving = saving,
                message = message,
                onFromChange = { from = it },
                onToChange = { to = it },
                onDateClick = { datePickerOpen = true },
                onCheckInChange = { checkIn = it },
                onCheckOutChange = { checkOut = it },
                onMapsUrlChange = { mapsUrl = it },
                onCancel = { adding = false; editingLeg = null; message = null },
                canDelete = canEdit && editingLeg != null,
                onDelete = {
                    editingLeg?.let { leg ->
                        scope.launch {
                            saving = true
                            runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteTripItem(tripId, "days", leg.dayId) }
                                .onSuccess { adding = false; editingLeg = null; onRouteAdded() }
                                .onFailure { message = it.message ?: localized(language, "Не удалось удалить день", "Could not delete day", "No se pudo eliminar el día", "Tag konnte nicht gelöscht werden") }
                            saving = false
                        }
                    }
                },
                onSave = {
                    scope.launch {
                        saving = true
                        val dateValues = routeEditorDateValues(selectedDateIso, "", 0, language)
                        runCatching {
                            val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                            editingLeg?.let {
                                repository.updateRouteLegDetails(
                                    id = tripId,
                                    dayId = it.dayId,
                                    from = from,
                                    to = to,
                                    checkIn = checkIn,
                                    checkOut = checkOut,
                                    notes = notes,
                                    mapsUrl = mapsUrl,
                                    date = selectedDateIso,
                                    dateDay = dateValues.day,
                                    dateMonth = dateValues.month,
                                    weekday = dateValues.weekday,
                                    distance = it.distance,
                                    travelTime = it.travelTime,
                                )
                            } ?: repository.addRouteLeg(
                                id = tripId,
                                from = from,
                                to = to,
                                checkIn = checkIn,
                                checkOut = checkOut,
                                notes = notes,
                                mapsUrl = mapsUrl,
                                date = selectedDateIso,
                                dateDay = dateValues.day,
                                dateMonth = dateValues.month,
                                weekday = dateValues.weekday,
                            )
                        }.onSuccess { adding = false; editingLeg = null; datePickerOpen = false; from = ""; to = ""; checkIn = ""; checkOut = ""; notes = ""; mapsUrl = ""; selectedDateIso = ""; onRouteAdded() }
                            .onFailure { message = it.message ?: localized(language, "Не удалось сохранить переезд", "Could not save route leg", "No se pudo guardar el trayecto", "Etappe konnte nicht gespeichert werden") }
                        saving = false
                    }
                },
            )
        }
    }
    if (canEdit && datePickerOpen) {
        AccommodationCalendarDialog(
            initialValue = selectedDateIso,
            onDismiss = { datePickerOpen = false },
            onConfirm = {
                selectedDateIso = it
                datePickerOpen = false
            },
        )
    }
}

@Composable
internal fun RouteLegEditorSheet(
    from: String,
    to: String,
    date: String,
    checkIn: String,
    checkOut: String,
    mapsUrl: String,
    saving: Boolean,
    message: String?,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onCheckInChange: (String) -> Unit,
    onCheckOutChange: (String) -> Unit,
    onMapsUrlChange: (String) -> Unit,
    onCancel: () -> Unit,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    val language = LocalLanguage.current
    val displayedFrom = localizedCityName(from)
    val displayedTo = localizedCityName(to)
    var visibleFrom by remember(from, language) { mutableStateOf(displayedFrom) }
    var visibleTo by remember(to, language) { mutableStateOf(displayedTo) }
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .padding(bottom = navigationBarInset),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized("День маршрута", "Route day", "Día de ruta", "Reisetag"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 23.sp)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.size(37.dp).clip(CircleShape).background(secondarySurfaceColor()).clickable { onCancel() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = secondaryTextColor(), modifier = Modifier.size(18.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RouteEditorField(localized("Откуда", "From", "Desde", "Von"), visibleFrom, { visibleFrom = it; onFromChange(it) }, Modifier.weight(1f))
            RouteEditorField(localized("Куда", "To", "A", "Nach"), visibleTo, { visibleTo = it; onToChange(it) }, Modifier.weight(1f))
        }
        RouteEditorDateField(
            label = localized("Дата", "Date", "Fecha", "Datum"),
            value = routeEditorDateLabel(date, language),
            onClick = onDateClick,
            modifier = Modifier.fillMaxWidth(),
        )
        RouteEditorField(localized("Заселение до", "Check-in by", "Entrada antes de", "Check-in bis"), checkIn, onCheckInChange, Modifier.fillMaxWidth(), placeholder = "—")
        RouteEditorField(localized("Выселение до", "Check-out by", "Salida antes de", "Check-out bis"), checkOut, onCheckOutChange, Modifier.fillMaxWidth(), placeholder = "—")
        RouteEditorField(
            localized("Ссылка на карту", "Map link", "Enlace al mapa", "Kartenlink"),
            mapsUrl,
            onMapsUrlChange,
            Modifier.fillMaxWidth(),
            placeholder = "https://maps.app.goo.gl/...",
            includeFontPadding = true,
        )
        if (message != null) Text(message, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp)) {
            if (canDelete) {
                Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(dangerSurfaceColor()).clickable { onDelete() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Delete, contentDescription = localized("Удалить день", "Delete day", "Eliminar día", "Tag löschen"), tint = Color(0xFFFF6B65), modifier = Modifier.size(22.dp))
                }
            }
            Button(onClick = onCancel, modifier = Modifier.height(54.dp).weight(1f), colors = ButtonDefaults.buttonColors(containerColor = cardSurfaceColor(), contentColor = contentTextColor()), shape = RoundedCornerShape(14.dp)) { Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
            Button(onClick = onSave, enabled = !saving, modifier = Modifier.height(54.dp).weight(1.25f), colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()), shape = RoundedCornerShape(14.dp)) { Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
        }
    }
}

@Composable
internal fun RouteOrderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, contentDescription: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) tintedSurfaceColor() else secondarySurfaceColor())
            .border(1.dp, contentBorderColor(), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = if (enabled) primaryColor() else Color(0xFFC2BFCA), modifier = Modifier.size(19.dp))
    }
}

@Composable
internal fun RouteEditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: String = "",
    includeFontPadding: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                if (placeholder.isNotBlank()) {
                    Text(
                        placeholder,
                        color = secondaryTextColor(),
                        fontFamily = Manrope,
                        fontSize = 14.sp,
                        lineHeight = if (includeFontPadding) 22.sp else 20.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = if (includeFontPadding) OdysseyFontPadding else OdysseyNoFontPadding),
                    )
                }
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.sp, lineHeight = if (includeFontPadding) 22.sp else 20.sp, color = contentTextColor(), platformStyle = if (includeFontPadding) OdysseyFontPadding else OdysseyNoFontPadding),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = contentBorderColor(), unfocusedBorderColor = contentBorderColor(), focusedContainerColor = cardSurfaceColor(), unfocusedContainerColor = cardSurfaceColor()),
            modifier = Modifier.fillMaxWidth().height(if (includeFontPadding) 54.dp else 50.dp),
        )
    }
}

@Composable
internal fun RouteLegCard(
    leg: com.odyssey.travelplanner.data.RouteLeg,
    dayIndex: Int,
    tripDates: String,
    resolvedCityFlags: Map<String, String> = emptyMap(),
    canEdit: Boolean = true,
    dragEnabled: Boolean = false,
    isDragging: Boolean = false,
    dragOffsetPx: Float = 0f,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onEdit: () -> Unit,
    onChecklistChange: (String, Boolean) -> Unit,
) {
    val language = LocalLanguage.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val timing = routeTiming(leg.checkIn, leg.checkOut)
    val timingLabel = if (timing.isCheckOut) {
        localized("Выселение", "Check-out", "Salida", "Check-out")
    } else {
        localized("Заселение", "Check-in", "Entrada", "Check-in")
    }
    val mapsUrl = leg.mapsUrl.ifBlank {
        "https://www.google.com/maps/dir/?api=1&origin=${Uri.encode(leg.from)}&destination=${Uri.encode(leg.to)}"
    }
    val longDestination = leg.to.length > 14
    val dateParts = if (leg.date.isNotBlank()) {
        // The ISO day is the source of truth. Legacy display labels can be
        // stale after a trip/date change or may have been saved in another
        // device language.
        routeDateParts(leg.date, tripDates, dayIndex, language)
    } else if (leg.dateDay.isNotBlank() || leg.dateMonth.isNotBlank()) {
        leg.dateDay.ifBlank { "—" } to leg.dateMonth.ifBlank { "—" }
    } else {
        routeDateParts(leg.date, tripDates, dayIndex, language)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 158.dp)
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetPx else 0f
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
            }
            .zIndex(if (isDragging) 1f else 0f)
            .shadow(if (isDragging) 16.dp else 8.dp, RoundedCornerShape(20.dp), clip = false, ambientColor = Color(0x12202040), spotColor = Color(0x12202040))
            .clip(RoundedCornerShape(20.dp))
            .background(cardSurfaceColor())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 14.dp)
            .pointerInput(leg.dayId, dragEnabled) {
                if (!dragEnabled) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = { currentOnDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnDrag(dragAmount.y)
                    },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragEnd() },
                )
            },
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.width(38.dp).padding(top = 1.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(dateParts.first, color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 23.sp)
            Text(dateParts.second, color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 9.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                RouteStop(leg.from, resolvedCityFlags[leg.from] ?: cityFlag(leg.from), isLast = false)
                Spacer(Modifier.height(5.dp))
                RouteStop(leg.to, resolvedCityFlags[leg.to] ?: cityFlag(leg.to), isLast = true, compact = longDestination)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(modifier = Modifier.size(37.dp).clip(RoundedCornerShape(11.dp)).background(tintedSurfaceColor()).clickable { clipboard.setText(AnnotatedString(mapsUrl)) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = localized("Копировать ссылку", "Copy link", "Copiar enlace", "Link kopieren"), tint = primaryColor(), modifier = Modifier.size(18.dp))
                }
                if (canEdit) {
                    Box(modifier = Modifier.size(37.dp).clip(RoundedCornerShape(11.dp)).background(tintedSurfaceColor()).clickable { onEdit() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Edit, contentDescription = localized("Изменить", "Edit", "Editar", "Bearbeiten"), tint = primaryColor(), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(secondarySurfaceColor())
                .border(1.dp, contentBorderColor(), RoundedCornerShape(13.dp))
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Key, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(16.dp))
            Text(timingLabel, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
            Spacer(Modifier.weight(1f))
            Text(timing.value, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp)
        }
    }
}

@Composable
internal fun RouteStop(city: String, flag: String, isLast: Boolean, compact: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(18.dp).height(22.dp).drawBehind {
            if (!isLast) drawLine(Color(0xFFD8D3F8), Offset(size.width / 2, 12.dp.toPx()), Offset(size.width / 2, size.height), strokeWidth = 1.5.dp.toPx())
        }) {
            Box(modifier = Modifier.size(if (isLast) 9.dp else 8.dp).clip(CircleShape).background(if (isLast) primaryColor() else Color.White).border(1.5.dp, if (isLast) primaryColor() else Color(0xFFC6BDF7), CircleShape).align(Alignment.Center))
        }
        Text("$flag ${localizedCityName(city)}", color = if (isLast) contentTextColor() else secondaryTextColor(), fontFamily = Manrope, fontWeight = if (isLast) FontWeight.W800 else FontWeight.W700, fontSize = if (compact) 14.sp else if (isLast) 17.sp else 13.sp, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.padding(start = 4.dp))
    }
}

