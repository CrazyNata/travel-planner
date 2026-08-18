package com.odyssey.travelplanner.ui.screen.trip.sights

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.filled.Check
import com.mapbox.geojson.Point
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.Sight
import com.odyssey.travelplanner.data.SightCatalogEntry
import com.odyssey.travelplanner.data.SightCatalogRepository
import com.odyssey.travelplanner.data.TripOverview
import com.odyssey.travelplanner.data.catalogCityName
import com.odyssey.travelplanner.data.isPlaceholderSightDescription
import kotlinx.coroutines.launch
import com.odyssey.travelplanner.ui.common.FullScreenSightPhotoViewer
import com.odyssey.travelplanner.ui.common.mapCoordinate
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.i18n.localizedCountWord
import com.odyssey.travelplanner.ui.icons.OdysseyChevronDown
import com.odyssey.travelplanner.ui.icons.OdysseyChevronUp
import com.odyssey.travelplanner.ui.screen.trip.overview.OverviewMapCard
import com.odyssey.travelplanner.ui.theme.LocalDarkTheme
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SightsContent(tripId: String, overview: TripOverview, canEdit: Boolean = true, onSightUpdated: () -> Unit) {
    val context = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val darkTheme = LocalDarkTheme.current
    val language = LocalLanguage.current
    val sights = overview.sights.sortedWith(compareBy<com.odyssey.travelplanner.data.Sight> { sightRouteDay(it.walkDay) }.thenBy { it.walkOrder })
    val initialRouteCity = listOf(
        sights.firstOrNull()?.city,
        overview.routeLegs.firstOrNull()?.to,
        overview.cities.firstOrNull(),
        overview.overviewMapPoints.firstOrNull(),
    ).firstOrNull { !it.isNullOrBlank() }.orEmpty()
    var routeDay by remember(tripId) { mutableStateOf(sights.firstOrNull()?.walkDay?.let(::sightRouteDay) ?: 1) }
    var dayMenuOpen by remember { mutableStateOf(false) }
    var creatingDay by remember { mutableStateOf(false) }
    val dayCities = remember(sights, overview.routeLegs, initialRouteCity) {
        val totalDays = maxOf(
            sights.maxOfOrNull { sightRouteDay(it.walkDay) } ?: 1,
            overview.routeDayCount,
            overview.routeLegs.maxOfOrNull { routeLegDayNumber(it, overview.routeLegs) } ?: overview.routeLegs.size,
        )
        (1..totalDays).map { day ->
            sights.firstOrNull { sightRouteDay(it.walkDay) == day }?.city?.takeIf(String::isNotBlank)
                ?: overview.routeLegs.firstOrNull { routeLegDayNumber(it, overview.routeLegs) == day }?.to
                ?: initialRouteCity
        }
    }
    val selectedDayCity = dayCities.getOrNull(routeDay - 1).orEmpty().ifBlank { initialRouteCity }
    val visibleSights = sights.filter { sightRouteDay(it.walkDay) == routeDay }
    var selectedSightId by remember(tripId, routeDay) { mutableStateOf<String?>(null) }
    val selectedLeg = overview.routeLegs.firstOrNull { routeLegDayNumber(it, overview.routeLegs) == routeDay }
    val mapCities = selectedLeg?.let { listOf(it.from, it.to) } ?: listOf(selectedDayCity)
    val sightRoutePoints = visibleSights.mapNotNull { sight -> sight.longitude?.let { longitude -> sight.latitude?.let { latitude -> Point.fromLngLat(longitude, latitude) } } }
    val sightMapEntries = visibleSights.mapNotNull { sight ->
        val point = sight.longitude?.let { longitude -> sight.latitude?.let { latitude -> Point.fromLngLat(longitude, latitude) } }
            ?: mapCoordinate(sight.city)
        point?.let { sight.id to it }
    }
    val sightMapPoints = sightMapEntries.map { it.second }
    val selectedSightMapIndex = sightMapEntries.indexOfFirst { it.first == selectedSightId }.takeIf { it >= 0 }
    val routeShareUrl = if (sightRoutePoints.size > 1) {
        val stops = sightRoutePoints.map { "${it.latitude()},${it.longitude()}" }
        "https://www.google.com/maps/dir/?api=1&origin=${stops.first()}&destination=${stops.last()}&waypoints=${stops.drop(1).dropLast(1).joinToString("|")}" 
    } else "https://www.google.com/maps/search/?api=1&query=${Uri.encode(selectedDayCity)}"
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("достопримечательности") }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var editingSight by remember { mutableStateOf<com.odyssey.travelplanner.data.Sight?>(null) }
    var uploadingSightId by remember { mutableStateOf<String?>(null) }
    var fullScreenSight by remember { mutableStateOf<com.odyssey.travelplanner.data.Sight?>(null) }
    var editingDay by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val missingSightDescriptionKey = remember(sights) {
        sights
            .filter { isPlaceholderSightDescription(it.description) }
            .map { sightDescriptionLookupKey(it.city, it.name) }
            .sorted()
            .joinToString("|")
    }
    var sightDescriptionOverrides by remember(tripId, language) { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(tripId, language, missingSightDescriptionKey) {
        sightDescriptionOverrides = emptyMap()
        if (missingSightDescriptionKey.isBlank()) return@LaunchedEffect

        val missingSights = sights.filter { isPlaceholderSightDescription(it.description) }
        val repository = SightCatalogRepository(SupabaseProvider.clientForCurrentAuthFlow())
        val resolved = mutableMapOf<String, String>()
        missingSights
            .groupBy { catalogCityName(it.city) }
            .filterKeys(String::isNotBlank)
            .forEach { (cityName, citySights) ->
                val entries = runCatching {
                    repository.searchWithLiveRatings(city = cityName, query = "", language = language, limit = 60).entries
                }.getOrElse { emptyList() }
                entries.forEach { entry ->
                    val description = entry.description(language).trim()
                    if (isPlaceholderSightDescription(description)) return@forEach
                    citySights.firstOrNull { sight -> sightDescriptionNameMatches(sight.name, entry) }
                        ?.let { sight -> resolved[sightDescriptionLookupKey(sight.city, sight.name)] = description }
                }
            }
        sightDescriptionOverrides = resolved
    }
    val visibleSightCatalogKey = visibleSights.joinToString("|") { "${it.id}:${it.name}:${it.city}" }
    var sightCatalogEntries by remember(tripId, language) { mutableStateOf<List<SightCatalogEntry>>(emptyList()) }
    LaunchedEffect(tripId, language, selectedDayCity, visibleSightCatalogKey) {
        sightCatalogEntries = emptyList()
        if (selectedDayCity.isBlank() || visibleSights.isEmpty()) return@LaunchedEffect

        val repository = SightCatalogRepository(SupabaseProvider.clientForCurrentAuthFlow())
        sightCatalogEntries = runCatching {
            repository.searchWithLiveRatings(
                city = selectedDayCity,
                query = "",
                language = language,
                limit = 60,
            ).entries
        }.getOrElse { emptyList() }
    }
    val visibleSightsWithDescriptions = visibleSights.map { sight ->
        val override = sightDescriptionOverrides[sightDescriptionLookupKey(sight.city, sight.name)]
        if (isPlaceholderSightDescription(sight.description) && !override.isNullOrBlank()) {
            sight.copy(description = override)
        } else {
            sight
        }
    }
    val sightCatalogById = visibleSightsWithDescriptions.associate { sight ->
        sight.id to sightCatalogEntries.firstOrNull { entry -> sightDescriptionNameMatches(sight.name, entry) }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val sightId = uploadingSightId ?: return@rememberLauncherForActivityResult
        if (uri == null) { uploadingSightId = null; return@rememberLauncherForActivityResult }
        scope.launch {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Не удалось прочитать изображение")
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).addSightPhoto(tripId, sightId, bytes)
            }.onSuccess {
                message = null
                onSightUpdated()
            }.onFailure {
                message = it.message ?: localized(language, "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0444\u043e\u0442\u043e. \u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442 \u0438 \u043f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u0435 \u043f\u043e\u043f\u044b\u0442\u043a\u0443.", "Could not upload the photo. Check your connection and try again.", "No se pudo subir la foto. Comprueba la conexi\u00f3n e int\u00e9ntalo de nuevo.", "Foto konnte nicht hochgeladen werden. Pr\u00fcfen Sie die Verbindung und versuchen Sie es erneut.")
            }
            uploadingSightId = null
        }
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            top = 18.dp,
            end = 18.dp,
            bottom = 30.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                "${localizedCityName(selectedDayCity).uppercase()} · ${localized("ДЕНЬ", "DAY", "DÍA", "TAG")} $routeDay",
                color = primaryColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.66.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            )
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .shadow(4.dp, RoundedCornerShape(17.dp), clip = false, ambientColor = Color(0x0D141428), spotColor = Color(0x0D141428))
                    .clip(RoundedCornerShape(17.dp))
                    .background(cardSurfaceColor())
                    .border(1.dp, contentBorderColor(), RoundedCornerShape(17.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Box(modifier = Modifier.size(46.dp).shadow(4.dp, RoundedCornerShape(13.dp), clip = false, ambientColor = Color(0x4D6C5CE7), spotColor = Color(0x4D6C5CE7)).clip(RoundedCornerShape(13.dp)).background(primaryColor()), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(
                            routeDay.toString(),
                            color = primaryContentColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 17.sp,
                            lineHeight = 17.sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                        Text(
                            localized("ДЕНЬ", "DAY", "DÍA", "TAG"),
                            color = primaryContentColor().copy(alpha = 0.8f),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 7.sp,
                            lineHeight = 9.sp,
                            letterSpacing = 0.7.sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        localized("ВЫБЕРИТЕ ДЕНЬ", "SELECT DAY", "ELIGE UN DÍA", "TAG WÄHLEN"),
                        color = secondaryTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        letterSpacing = 0.8.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 1.dp),
                    ) {
                        Text(
                            localizedCityName(selectedDayCity),
                            color = contentTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 17.sp,
                            lineHeight = 23.sp,
                            letterSpacing = (-0.17).sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (canEdit) {
                            Box(modifier = Modifier.padding(start = 7.dp).size(22.dp).clip(RoundedCornerShape(7.dp)).background(tintedSurfaceColor()).clickable { editingDay = true }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Edit, contentDescription = localized("Изменить", "Edit", "Editar", "Bearbeiten"), tint = primaryColor(), modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(tintedSurfaceColor()).clickable { dayMenuOpen = !dayMenuOpen }, contentAlignment = Alignment.Center) {
                    if (dayMenuOpen) {
                        OdysseyChevronUp(17.dp, color = primaryColor())
                    } else {
                        OdysseyChevronDown(17.dp, color = primaryColor())
                    }
                }
            }
            DropdownMenu(
                expanded = dayMenuOpen,
                onDismissRequest = { dayMenuOpen = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                scrollState = rememberScrollState(),
                shape = RoundedCornerShape(18.dp),
                containerColor = cardSurfaceColor(),
                shadowElevation = 16.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp)) {
                    dayCities.forEachIndexed { index, dayCity ->
                        val selected = index + 1 == routeDay
                        Row(modifier = Modifier.fillMaxWidth().height(43.dp).padding(horizontal = 12.dp).clip(RoundedCornerShape(11.dp)).background(if (selected) tintedSurfaceColor() else Color.Transparent).clickable { routeDay = index + 1; dayMenuOpen = false }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${localized("ДЕНЬ", "DAY", "DÍA", "TAG")} ${index + 1}", color = if (selected) primaryColor() else secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp, modifier = Modifier.width(64.dp))
                            Text(localizedCityName(dayCity), color = if (selected) primaryColor() else contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
                            Spacer(Modifier.weight(1f))
                            if (selected) Text("✓", color = primaryColor(), fontSize = 18.sp, fontWeight = FontWeight.W800)
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(contentBorderColor()).padding(horizontal = 12.dp))
                    if (canEdit) {
                        Row(modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 22.dp).clickable { creatingDay = true; dayMenuOpen = false }, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(tintedSurfaceColor()), contentAlignment = Alignment.Center) { Text("+", color = primaryColor(), fontSize = 23.sp, fontWeight = FontWeight.W500) }
                            Text(localized("Добавить день", "Add day", "Añadir día", "Tag hinzufügen"), color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
        }
        }
        item {
            Box(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                OverviewMapCard(
                    legs = overview.routeLegs,
                    cities = mapCities,
                    cityCoordinates = overview.cityCoordinates,
                    mapHeight = 220.dp,
                    cardShape = RoundedCornerShape(22.dp),
                    cardShadow = 10.dp,
                    routePoints = sightRoutePoints,
                    markerPoints = sightMapPoints,
                    selectedPointIndex = selectedSightMapIndex,
                    footer = {
                        Row(modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${localizedCityName(selectedDayCity).uppercase()} · ${localized("ДЕНЬ", "DAY", "DÍA", "TAG")} $routeDay",
                                    color = primaryColor(),
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W800,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    letterSpacing = 0.66.sp,
                                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                )
                                Text(
                                    "${localizedCityName(selectedDayCity)} · ${visibleSights.size} ${localizedCountWord(visibleSights.size, language, "место", "места", "мест", "place", "places", "lugar", "lugares", "Ort", "Orte")}",
                                    color = secondaryTextColor(),
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W700,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(primaryColor())
                                    .clickable { clipboard.setText(AnnotatedString(routeShareUrl)) }
                                    .padding(horizontal = 13.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = localized("Копировать", "Copy", "Copiar", "Kopieren"), tint = primaryContentColor(), modifier = Modifier.size(14.dp))
                                Text(
                                    localized("Копировать", "Copy", "Copiar", "Kopieren"),
                                    color = primaryContentColor(),
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W700,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    },
                )
            }
        }
        if (message != null) {
            item {
                Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
            }
        }
        if (visibleSights.isEmpty()) {
            item { Text(localized("Достопримечательности пока не добавлены", "No sights added yet", "Aún no se han añadido lugares", "Noch keine Orte hinzugefügt"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp) }
        } else {
            items(visibleSightsWithDescriptions, key = { it.id }) { sight ->
                SightCard(
                    sight = sight,
                    catalogEntry = sightCatalogById[sight.id],
                    uploading = uploadingSightId == sight.id,
                    selected = sight.id == selectedSightId,
                    onSelect = { selectedSightId = sight.id },
                    onOpenPhoto = { fullScreenSight = sight },
                    canEdit = canEdit,
                    onEdit = { if (canEdit) editingSight = sight },
                )
            }
        }
    }
    if (canEdit && editingSight != null) {
        ModalBottomSheet(
            onDismissRequest = { editingSight = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = Color(0x730F0F19),
            shape = RoundedCornerShape(topStart = 29.dp, topEnd = 29.dp),
            dragHandle = null,
        ) {
            EditSightPanel(
                sight = editingSight!!,
                tripId = tripId,
                dayCities = dayCities,
                onClose = { editingSight = null },
                onSaved = {
                    editingSight = null
                    onSightUpdated()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding(),
            )
        }
    }
    fullScreenSight?.let { sight ->
        FullScreenSightPhotoViewer(
            sight = sight,
            onDismiss = { fullScreenSight = null },
        )
    }
    if (canEdit && editingDay) {
        ModalBottomSheet(onDismissRequest = { editingDay = false }, containerColor = cardSurfaceColor()) {
            EditDaySheet(
                tripId = tripId,
                day = routeDay,
                city = selectedDayCity,
                sights = visibleSightsWithDescriptions,
                allSights = sights,
                darkTheme = darkTheme,
                onClose = { editingDay = false },
                onSaved = onSightUpdated,
                onDeleted = {
                    editingDay = false
                    routeDay = (routeDay - 1).coerceAtLeast(1)
                    onSightUpdated()
                },
            )
        }
    }
    if (canEdit && creatingDay) {
        ModalBottomSheet(onDismissRequest = { creatingDay = false }, containerColor = cardSurfaceColor()) {
            val nextDayNumber = maxOf(
                routeDay,
                overview.routeDayCount,
                sights.maxOfOrNull { sightRouteDay(it.walkDay) } ?: 0,
            ) + 1
            CreateDaySheet(tripId = tripId, city = selectedDayCity, day = nextDayNumber, onClose = { creatingDay = false }, onSaved = onSightUpdated)
        }
    }
}

