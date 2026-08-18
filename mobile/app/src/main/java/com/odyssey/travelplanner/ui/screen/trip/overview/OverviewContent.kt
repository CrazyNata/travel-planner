package com.odyssey.travelplanner.ui.screen.trip.overview

import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Check
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.extension.localization.localizeLabels
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.JsonPrimitive
import com.odyssey.travelplanner.data.CityLocation
import com.odyssey.travelplanner.data.CoverPhoto
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.TripOverview
import com.odyssey.travelplanner.data.WeatherSnapshot
import com.odyssey.travelplanner.data.cityFlag
import com.odyssey.travelplanner.ui.common.EmptyStateCard
import com.odyssey.travelplanner.ui.common.WeatherPlaceholder
import com.odyssey.travelplanner.ui.domain.cityFilterKey
import com.odyssey.travelplanner.ui.domain.coverPhotoForCity
import com.odyssey.travelplanner.ui.domain.mapCoordinate
import com.odyssey.travelplanner.ui.domain.normalizedOverviewBlocks
import com.odyssey.travelplanner.ui.domain.toggleOverviewCity
import com.odyssey.travelplanner.ui.i18n.labelMapboxAccessibility
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.i18n.localizedLegsAndCitiesSummary
import com.odyssey.travelplanner.ui.i18n.mapLocale
import com.odyssey.travelplanner.ui.screen.trip.sights.keepMapGesturesInsideMap
import com.odyssey.travelplanner.ui.theme.LocalDarkTheme
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyCardShadow
import com.odyssey.travelplanner.ui.theme.OdysseyDarkSurface2
import com.odyssey.travelplanner.ui.theme.OdysseyError
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor

internal enum class OverviewEditSheet { MAP, WEATHER }

@Composable
internal fun OverviewContent(
    tripId: String,
    overview: TripOverview,
    weather: Map<String, WeatherSnapshot>,
    editMode: Boolean,
    onChanged: () -> Unit,
) {
    val language = LocalLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    var photoIndex by remember { mutableStateOf(0) }
    var tripDatesWeather by remember { mutableStateOf(false) }
    var orderedBlocks by remember(overview.id, overview.overviewBlocks) {
        mutableStateOf(normalizedOverviewBlocks(overview.overviewBlocks))
    }
    var selectedMapCities by remember(overview.id, overview.overviewMapPoints, overview.routeLegs) {
        mutableStateOf<List<String>>(emptyList())
    }
    var selectedWeatherCities by remember(overview.id, overview.overviewWeatherCities, overview.overviewMapPoints, overview.routeLegs) {
        mutableStateOf<List<String>>(emptyList())
    }
    var editSheet by remember { mutableStateOf<OverviewEditSheet?>(null) }
    var savingSettings by remember { mutableStateOf(false) }
    var uploadingPhoto by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var draggedBlock by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }
    var dragInitialOrder by remember { mutableStateOf<List<String>>(emptyList()) }

    val photos = overview.coverPhotos
    val routeCities = remember(overview.routeLegs, overview.cities, overview.overviewMapPoints) {
        (overview.cities + overview.overviewMapPoints + overview.routeLegs.flatMap { listOf(it.from, it.to) })
            .filter(String::isNotBlank)
            .distinctBy { cityFilterKey(it) }
    }
    val defaultMapCities = overview.overviewMapPoints.ifEmpty { routeCities }
    val weatherCities = overview.overviewWeatherCities.ifEmpty { defaultMapCities }
        .distinctBy { cityFilterKey(it) }
    val selectableWeatherCities = routeCities.ifEmpty { defaultMapCities }
    val weatherEditorCities = (selectableWeatherCities + selectedWeatherCities)
        .distinctBy { cityFilterKey(it) }

    LaunchedEffect(overview.id, overview.overviewMapPoints, overview.routeLegs, overview.cities) {
        selectedMapCities = overview.overviewMapPoints.ifEmpty { routeCities }
    }
    LaunchedEffect(overview.id, overview.overviewWeatherCities, overview.overviewMapPoints, overview.routeLegs, overview.cities) {
        selectedWeatherCities = overview.overviewWeatherCities.ifEmpty { defaultMapCities }
    }

    fun updateBlockDrag(dragAmount: Float) {
        val draggedId = draggedBlock ?: return
        dragOffsetPx += dragAmount
        val draggedInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "overview-block:$draggedId" } ?: return
        val draggedCenter = draggedInfo.offset + draggedInfo.size / 2f + dragOffsetPx
        val viewport = listState.layoutInfo
        if (draggedCenter < viewport.viewportStartOffset + 80f) scope.launch { listState.scrollBy(-24f) }
        if (draggedCenter > viewport.viewportEndOffset - 80f) scope.launch { listState.scrollBy(24f) }

        val currentIndex = orderedBlocks.indexOf(draggedId)
        if (currentIndex < 0) return
        val targetIndex = when {
            dragAmount > 0f -> currentIndex + 1
            dragAmount < 0f -> currentIndex - 1
            else -> return
        }
        if (targetIndex !in orderedBlocks.indices) return
        val targetId = orderedBlocks[targetIndex]
        val targetInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "overview-block:$targetId" } ?: return
        val targetCenter = targetInfo.offset + targetInfo.size / 2f
        val crossedTarget = if (dragAmount > 0f) draggedCenter > targetCenter else draggedCenter < targetCenter
        if (!crossedTarget) return

        dragOffsetPx -= (targetInfo.offset - draggedInfo.offset).toFloat()
        orderedBlocks = orderedBlocks.toMutableList().apply {
            removeAt(currentIndex)
            add(targetIndex, draggedId)
        }
    }

    fun finishBlockDrag() {
        val draggedId = draggedBlock
        val finalOrder = orderedBlocks
        val initialOrder = dragInitialOrder
        val changed = draggedId != null && finalOrder != initialOrder
        draggedBlock = null
        dragOffsetPx = 0f
        dragInitialOrder = emptyList()
        if (!changed) return

        scope.launch {
            savingSettings = true
            runCatching {
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateTripSection(
                    tripId,
                    "overviewBlocks",
                    buildJsonArray { finalOrder.forEach { add(JsonPrimitive(it)) } },
                )
            }.onSuccess {
                actionMessage = null
                onChanged()
            }.onFailure {
                orderedBlocks = initialOrder.ifEmpty { normalizedOverviewBlocks(overview.overviewBlocks) }
                actionMessage = it.message ?: localized(
                    language,
                    "Не удалось сохранить порядок блоков. Проверьте интернет и повторите попытку.",
                    "Could not save the block order. Check your connection and try again.",
                    "No se pudo guardar el orden de los bloques. Comprueba la conexión e inténtalo de nuevo.",
                    "Die Reihenfolge der Blöcke konnte nicht gespeichert werden. Prüfen Sie die Verbindung und versuchen Sie es erneut.",
                )
            }
            savingSettings = false
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            uploadingPhoto = true
            actionMessage = null
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Не удалось прочитать изображение")
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).addCoverPhoto(
                    id = tripId,
                    bytes = bytes,
                    city = photos.getOrNull(photoIndex)?.city.orEmpty().ifBlank { routeCities.firstOrNull().orEmpty() },
                )
            }.onSuccess {
                photoIndex = overview.coverPhotos.size
                onChanged()
            }.onFailure {
                actionMessage = it.message ?: localized(language, "Не удалось загрузить фото", "Could not upload the photo", "No se pudo subir la foto", "Foto konnte nicht hochgeladen werden")
            }
            uploadingPhoto = false
        }
    }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        item(key = "overview-summary") {
            if (editMode) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(tintedSurfaceColor()).padding(horizontal = 12.dp, vertical = 9.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(17.dp))
                    Text(localized("Зажмите блок за ⋮⋮ и перенесите его", "Hold ⋮⋮ to move a block", "Mantén ⋮⋮ para mover un bloque", "Halten Sie ⋮⋮ zum Verschieben gedrückt"), color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.sp, modifier = Modifier.padding(start = 7.dp))
                }
            }
            if (actionMessage != null) {
                Text(actionMessage!!, color = OdysseyError, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        orderedBlocks.forEach { block ->
            item(key = "overview-block:$block") {
                val dragModifier = if (editMode && overview.canEdit && !savingSettings && !uploadingPhoto) {
                    Modifier.pointerInput(block) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedBlock = block
                                dragInitialOrder = orderedBlocks
                                dragOffsetPx = 0f
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { _, dragAmount -> updateBlockDrag(dragAmount.y) },
                            onDragEnd = ::finishBlockDrag,
                            onDragCancel = ::finishBlockDrag,
                        )
                    }
                } else Modifier
                OverviewEditableBlock(
                    block = block,
                    editMode = editMode,
                    isDragging = draggedBlock == block,
                    dragOffsetPx = if (draggedBlock == block) dragOffsetPx else 0f,
                    modifier = dragModifier,
                    actionLabel = when (block) {
                        "photo" -> if (uploadingPhoto) localized("Загружаем…", "Uploading…", "Subiendo…", "Wird hochgeladen…") else localized("Изменить фото", "Change photo", "Cambiar foto", "Foto ändern")
                        "map" -> localized("Изменить карту", "Edit map", "Editar mapa", "Karte bearbeiten")
                        else -> localized("Настроить погоду", "Configure weather", "Configurar el tiempo", "Wetter einstellen")
                    },
                    onAction = {
                        when (block) {
                            "photo" -> if (!uploadingPhoto) photoPicker.launch("image/*")
                            "map" -> {
                                selectedMapCities = overview.overviewMapPoints.ifEmpty { routeCities }
                                editSheet = OverviewEditSheet.MAP
                            }
                            "weather" -> {
                                selectedWeatherCities = overview.overviewWeatherCities.ifEmpty { defaultMapCities }
                                editSheet = OverviewEditSheet.WEATHER
                            }
                        }
                    },
                ) {
                    when (block) {
                        "photo" -> OverviewPhotoBlock(photos, photoIndex, { photoIndex = (photoIndex - 1 + photos.size) % photos.size }, { photoIndex = (photoIndex + 1) % photos.size })
                        "map" -> OverviewMapCard(overview.routeLegs, defaultMapCities, cityCoordinates = overview.cityCoordinates)
                        "weather" -> OverviewWeatherBlock(weatherCities, photos, weather, tripDatesWeather) { tripDatesWeather = it }
                    }
                }
            }
        }
    }

    if (editSheet == OverviewEditSheet.MAP) {
        OverviewCitySelectionSheet(
            title = localized("Создать карту", "Create map", "Crear mapa", "Karte erstellen"),
            body = localized("Выберите города, которые должны быть на карте маршрута.", "Choose the cities that should appear on the route map.", "Elige las ciudades que deben aparecer en el mapa.", "Wählen Sie die Städte für die Routenkarte aus."),
            cities = routeCities,
            selectedCities = selectedMapCities,
            saving = savingSettings,
            onToggle = { city -> selectedMapCities = selectedMapCities.toggleOverviewCity(city) },
            onDismiss = { editSheet = null },
            onSave = {
                scope.launch {
                    savingSettings = true
                    runCatching {
                        SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateTripSection(tripId, "overviewMapPoints", buildJsonArray { selectedMapCities.forEach { add(JsonPrimitive(it)) } })
                    }.onSuccess { editSheet = null; actionMessage = null; onChanged() }
                        .onFailure { actionMessage = it.message ?: localized(language, "Не удалось сохранить карту", "Could not save the map", "No se pudo guardar el mapa", "Die Karte konnte nicht gespeichert werden") }
                    savingSettings = false
                }
            },
        )
    }
    if (editSheet == OverviewEditSheet.WEATHER) {
        OverviewCitySelectionSheet(
            title = localized("Погода по городам", "Weather by city", "Tiempo por ciudad", "Wetter nach Stadt"),
            body = localized("Добавьте или уберите города в блоке погоды на главном экране.", "Add or remove cities from the weather block on the overview.", "Añade o quita ciudades del bloque del tiempo.", "Fügen Sie Städte im Wetterblock hinzu oder entfernen Sie sie."),
            cities = weatherEditorCities,
            selectedCities = selectedWeatherCities,
            saving = savingSettings,
            onToggle = { city -> selectedWeatherCities = selectedWeatherCities.toggleOverviewCity(city) },
            onDismiss = { editSheet = null },
            onSave = {
                scope.launch {
                    savingSettings = true
                    runCatching {
                        SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateTripSection(tripId, "overviewWeatherCities", buildJsonArray { selectedWeatherCities.forEach { add(JsonPrimitive(it)) } })
                    }.onSuccess { editSheet = null; actionMessage = null; onChanged() }
                        .onFailure { actionMessage = it.message ?: localized(language, "Не удалось сохранить города погоды", "Could not save weather cities", "No se pudieron guardar las ciudades del tiempo", "Wetterstädte konnten nicht gespeichert werden") }
                    savingSettings = false
                }
            },
            allowAddingCities = true,
            onAddCity = { city ->
                val cleanedCity = city.trim()
                if (selectedWeatherCities.any { cityFilterKey(it) == cityFilterKey(cleanedCity) }) {
                    false
                } else {
                    selectedWeatherCities = selectedWeatherCities + cleanedCity
                    true
                }
            },
        )
    }
}

@Composable
internal fun OverviewEditableBlock(
    block: String,
    editMode: Boolean,
    isDragging: Boolean,
    dragOffsetPx: Float,
    modifier: Modifier,
    actionLabel: String,
    onAction: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().graphicsLayer {
            translationY = if (isDragging) dragOffsetPx else 0f
            scaleX = if (isDragging) 1.015f else 1f
            scaleY = if (isDragging) 1.015f else 1f
            shadowElevation = if (isDragging) 18.dp.toPx() else 0f
            alpha = if (isDragging) 0.96f else 1f
        }.then(if (editMode) Modifier.border(1.dp, primaryColor(), RoundedCornerShape(21.dp)).padding(7.dp) else Modifier),
    ) {
        if (editMode) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Text("⋮⋮", color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
                Text(when (block) { "photo" -> localized("Фото", "Photo", "Foto", "Foto"); "map" -> localized("Карта", "Map", "Mapa", "Karte"); else -> localized("Погода", "Weather", "Tiempo", "Wetter") }, color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp, modifier = Modifier.padding(start = 5.dp))
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onAction, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text(actionLabel, color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.sp)
                }
            }
        }
        content()
    }
}

@Composable
internal fun OverviewPhotoBlock(photos: List<CoverPhoto>, photoIndex: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    val activePhoto = photos.getOrNull(photoIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)))
    Box(modifier = Modifier.fillMaxWidth().height(270.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFCAC7D9))) {
        if (activePhoto != null) AsyncImage(model = activePhoto.imageUrl, contentDescription = activePhoto.city, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Text(localizedCityName(activePhoto?.city.orEmpty()), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 24.sp, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))
        if (photos.size > 1) {
            Text("‹", color = Color.White, fontSize = 31.sp, modifier = Modifier.align(Alignment.CenterStart).padding(12.dp).clickable(onClick = onPrevious))
            Text("›", color = Color.White, fontSize = 31.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp).clickable(onClick = onNext))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)) {
                photos.forEachIndexed { index, _ -> Spacer(Modifier.height(6.dp).width(if (index == photoIndex) 18.dp else 6.dp).background(if (index == photoIndex) Color.White else Color(0x99FFFFFF), RoundedCornerShape(3.dp))) }
            }
        }
    }
}

@Composable
internal fun OverviewWeatherBlock(weatherCities: List<String>, photos: List<CoverPhoto>, weather: Map<String, WeatherSnapshot>, tripDatesWeather: Boolean, onTripDatesWeatherChange: (Boolean) -> Unit) {
    Text(localized("Погода по маршруту", "Weather along the route", "Tiempo en la ruta", "Wetter entlang der Route"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 20.sp, modifier = Modifier.padding(top = 2.dp))
    Row(modifier = Modifier.background(if (LocalDarkTheme.current) OdysseyDarkSurface2 else Color(0xFFEEEEF2), RoundedCornerShape(12.dp)).padding(4.dp)) {
        Text(localized("Сейчас", "Now", "Ahora", "Jetzt"), color = if (!tripDatesWeather) contentTextColor() else secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp, modifier = Modifier.background(if (!tripDatesWeather) cardSurfaceColor() else Color.Transparent, RoundedCornerShape(9.dp)).clickable { onTripDatesWeatherChange(false) }.padding(horizontal = 14.dp, vertical = 8.dp))
        Text(localized("На даты поездки", "Trip dates", "Fechas del viaje", "Reisedaten"), color = if (tripDatesWeather) contentTextColor() else secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp, modifier = Modifier.background(if (tripDatesWeather) cardSurfaceColor() else Color.Transparent, RoundedCornerShape(9.dp)).clickable { onTripDatesWeatherChange(true) }.padding(horizontal = 14.dp, vertical = 8.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
        weatherCities.forEach { city -> WeatherPlaceholder(city, coverPhotoForCity(photos, city), weather[city], tripDatesWeather) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun OverviewCitySelectionSheet(
    title: String,
    body: String,
    cities: List<String>,
    selectedCities: List<String>,
    saving: Boolean,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    allowAddingCities: Boolean = false,
    onAddCity: ((String) -> Boolean)? = null,
) {
    var newCity by remember { mutableStateOf("") }
    var addCityMessage by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = cardSurfaceColor()) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(start = 20.dp, end = 20.dp, bottom = 18.dp)) {
            Text(title, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 20.sp)
            Text(body, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
            if (allowAddingCities && onAddCity != null) {
                val invalidCityMessage = localized("Введите название города", "Enter a city name", "Escribe el nombre de una ciudad", "Geben Sie einen Stadtnamen ein")
                val duplicateCityMessage = localized("Этот город уже добавлен", "This city is already added", "Esta ciudad ya está añadida", "Diese Stadt wurde bereits hinzugefügt")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newCity,
                        onValueChange = {
                            newCity = it
                            addCityMessage = null
                        },
                        label = { Text(localized("Новый город", "New city", "Nueva ciudad", "Neue Stadt"), fontFamily = Manrope, fontSize = 12.sp) },
                        placeholder = { Text(localized("Например, Париж", "For example, Paris", "Por ejemplo, París", "Zum Beispiel Paris"), color = secondaryTextColor(), fontFamily = Manrope, fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(13.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor(),
                            unfocusedBorderColor = contentBorderColor(),
                            focusedLabelColor = primaryColor(),
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            val cleanedCity = newCity.trim()
                            if (cleanedCity.length < 2) {
                                addCityMessage = invalidCityMessage
                            } else if (onAddCity(cleanedCity)) {
                                newCity = ""
                                addCityMessage = null
                            } else {
                                addCityMessage = duplicateCityMessage
                            }
                        },
                        enabled = !saving && newCity.trim().length >= 2,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp),
                    ) {
                        Text(localized("Добавить", "Add", "Añadir", "Hinzufügen"), color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
                    }
                }
                Text(
                    localized("Можно добавить город, которого нет в маршруте.", "You can add a city that is not on the route.", "Puedes añadir una ciudad que no esté en la ruta.", "Sie können eine Stadt hinzufügen, die nicht auf der Route liegt."),
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
                addCityMessage?.let { message ->
                    Text(message, color = OdysseyError, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            if (cities.isEmpty()) {
                Text(
                    if (allowAddingCities) localized("Добавьте город через поле выше.", "Add a city using the field above.", "Añade una ciudad usando el campo de arriba.", "Fügen Sie eine Stadt über das Feld oben hinzu.")
                    else localized("Добавьте города в маршрут, чтобы выбрать их здесь.", "Add cities to the route to choose them here.", "Añade ciudades a la ruta para elegirlas aquí.", "Fügen Sie der Route Städte hinzu, um sie hier auszuwählen."),
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                cities.forEach { city ->
                    val selected = selectedCities.any { cityFilterKey(it) == cityFilterKey(city) }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(if (selected) tintedSurfaceColor() else Color.Transparent).clickable { onToggle(city) }.padding(horizontal = 10.dp, vertical = 10.dp)) {
                        Icon(if (selected) Icons.Filled.Check else Icons.Outlined.LocationOn, contentDescription = null, tint = if (selected) primaryColor() else secondaryTextColor(), modifier = Modifier.size(20.dp))
                        Text(localizedCityName(city), color = if (selected) primaryColor() else contentTextColor(), fontFamily = Manrope, fontWeight = if (selected) FontWeight.W800 else FontWeight.W700, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
                        Spacer(Modifier.weight(1f))
                        Text(cityFlag(city), fontSize = 17.sp)
                    }
                }
            }
            Button(onClick = onSave, enabled = !saving && selectedCities.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()), shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth().padding(top = 13.dp)) {
                Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Готово", "Done", "Listo", "Fertig"), fontFamily = Manrope, fontWeight = FontWeight.W800)
            }
        }
    }
}

@Composable
internal fun OverviewMapCard(
    legs: List<com.odyssey.travelplanner.data.RouteLeg>,
    cities: List<String>,
    cityCoordinates: Map<String, CityLocation> = emptyMap(),
    mapHeight: Dp = 200.dp,
    footer: @Composable (() -> Unit)? = null,
    routePoints: List<Point> = emptyList(),
    markerPoints: List<Point> = routePoints,
    selectedPointIndex: Int? = null,
    cardShape: RoundedCornerShape = RoundedCornerShape(20.dp),
    cardShadow: Dp? = null,
) {
    val context = LocalContext.current
    val language = LocalLanguage.current
    val attributionDescription = localized("Информация об источниках карты", "Map attribution information", "Información de atribución del mapa", "Informationen zur Kartenquelle")
    val cityCount = cities.distinctBy { cityFilterKey(it) }.size
    val cityMapCoordinates = cities.mapNotNull { mapCoordinate(it, cityCoordinates) }
    // The overview map has no separate sight geometry. Use the ordered city
    // coordinates as its route and markers so the map shows the same journey
    // that is summarized below it. Detailed sight maps still provide their
    // own explicit routePoints/markerPoints.
    val effectiveRoutePoints = routePoints.ifEmpty { cityMapCoordinates }
    val effectiveMarkerPoints = markerPoints.ifEmpty {
        if (routePoints.isNotEmpty()) routePoints else cityMapCoordinates
    }
    val coordinates = (effectiveRoutePoints + effectiveMarkerPoints)
        .ifEmpty { cityMapCoordinates }
        .distinctBy { "${it.longitude()},${it.latitude()}" }
    var mapStyleReady by remember { mutableStateOf(false) }
    val mapView = remember(context, attributionDescription) {
        MapView(
            context,
            MapInitOptions(
                context = context,
                textureView = true,
                styleUri = null,
            ),
        ).also {
            it.scalebar.enabled = false
            keepMapGesturesInsideMap(it)
            it.post { labelMapboxAccessibility(it, attributionDescription) }
        }
    }
    val routeAnnotationManager = remember(mapView) { mapView.annotations.createPolylineAnnotationManager() }
    val sightAnnotationManager = remember(mapView) { mapView.annotations.createCircleAnnotationManager() }
    val sightNumberAnnotationManager = remember(mapView) { mapView.annotations.createPointAnnotationManager() }

    LaunchedEffect(mapView) {
        mapStyleReady = false
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            style.localizeLabels(mapLocale(language))
            mapStyleReady = true
        }
    }

    DisposableEffect(mapView, routeAnnotationManager, sightAnnotationManager, sightNumberAnnotationManager) {
        onDispose {
            routeAnnotationManager.deleteAll()
            sightAnnotationManager.deleteAll()
            sightNumberAnnotationManager.deleteAll()
        }
    }

    LaunchedEffect(mapStyleReady, language) {
        if (mapStyleReady) mapView.mapboxMap.style?.localizeLabels(mapLocale(language))
    }

    LaunchedEffect(mapStyleReady, coordinates, effectiveRoutePoints, effectiveMarkerPoints, selectedPointIndex) {
        if (mapStyleReady && coordinates.isNotEmpty()) {
            routeAnnotationManager.deleteAll()
            sightAnnotationManager.deleteAll()
            sightNumberAnnotationManager.deleteAll()
            if (effectiveRoutePoints.size > 1) {
                routeAnnotationManager.create(
                    PolylineAnnotationOptions()
                        .withPoints(effectiveRoutePoints)
                        .withLineColor("#6C5CE7")
                        .withLineWidth(5.0),
                )
            }
            if (effectiveMarkerPoints.isNotEmpty()) {
                effectiveMarkerPoints.forEachIndexed { index, point ->
                    val selected = index == selectedPointIndex
                    sightAnnotationManager.create(
                        CircleAnnotationOptions()
                            .withPoint(point)
                            .withCircleRadius(if (selected) 14.0 else 9.0)
                            .withCircleColor(if (selected) "#FF6B65" else "#6C5CE7")
                            .withCircleStrokeColor("#FFFFFF")
                            .withCircleStrokeWidth(if (selected) 4.0 else 3.0),
                    )
                    sightNumberAnnotationManager.create(
                        PointAnnotationOptions()
                            .withPoint(point)
                            .withTextField((index + 1).toString())
                            .withTextColor("#FFFFFF")
                            .withTextSize(if (selected) 13.5 else 12.0)
                            .withTextAnchor(TextAnchor.CENTER),
                    )
                }
            }
        }
    }

    LaunchedEffect(mapStyleReady, coordinates) {
        if (mapStyleReady && coordinates.isNotEmpty()) {
            mapView.post {
                val camera = if (coordinates.size > 1) {
                    mapView.mapboxMap.cameraForCoordinates(
                        coordinates,
                        EdgeInsets(34.0, 34.0, 34.0, 34.0),
                        null,
                        null,
                    )
                } else {
                    CameraOptions.Builder()
                        .center(coordinates.first())
                        .zoom(9.0)
                        .build()
                }
                mapView.mapboxMap.setCamera(camera)
            }
        }
    }

    LaunchedEffect(mapStyleReady, selectedPointIndex, effectiveMarkerPoints) {
        if (mapStyleReady) {
            effectiveMarkerPoints.getOrNull(selectedPointIndex ?: -1)?.let { point ->
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(14.0)
                        .build(),
                )
            }
        }
    }

    val cardModifier = if (cardShadow != null) {
        Modifier.fillMaxWidth().shadow(cardShadow, cardShape, clip = false, ambientColor = OdysseyCardShadow, spotColor = OdysseyCardShadow)
    } else {
        Modifier.fillMaxWidth()
    }
    Column(
        modifier = cardModifier.clip(cardShape).background(cardSurfaceColor()),
    ) {
        if (coordinates.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Outlined.LocationOn,
                title = localized("Карта появится после добавления городов", "The map appears after adding cities", "El mapa aparecerá al añadir ciudades", "Die Karte erscheint nach dem Hinzufügen von Städten"),
                body = localized("Добавьте города или координаты мест", "Add cities or place coordinates", "Añada ciudades o coordenadas", "Fügen Sie Städte oder Koordinaten hinzu"),
                action = null,
                onAction = null,
            )
        } else {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxWidth().height(mapHeight))
        }
        if (footer != null) footer() else Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(localized("Общий маршрут", "Full route", "Ruta completa", "Gesamtroute"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp)
            Text(text = localizedLegsAndCitiesSummary(legs.size, cityCount, language), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
        }
    }
}

