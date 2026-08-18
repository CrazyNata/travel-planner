package com.odyssey.travelplanner.ui.screen.trip.restaurants

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.TripOverview
import com.odyssey.travelplanner.ui.common.EmptyStateCard
import com.odyssey.travelplanner.ui.domain.cityFilterKey
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityFilter
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.i18n.splitStoredCityList
import com.odyssey.travelplanner.ui.icons.OdysseyChevronDown
import com.odyssey.travelplanner.ui.icons.OdysseyFilterIcon
import com.odyssey.travelplanner.ui.icons.OdysseyLocationIcon
import com.odyssey.travelplanner.ui.icons.OdysseyPlusIcon
import com.odyssey.travelplanner.ui.screen.trip.lodging.AccommodationCalendarDialog
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyError
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.OdysseyPurpleGradientEnd
import com.odyssey.travelplanner.ui.theme.OdysseyPurpleShadowSoft
import com.odyssey.travelplanner.ui.theme.OdysseySheetScrim
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.labelColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondarySurfaceColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun RestaurantsContent(tripId: String, overview: TripOverview, canEdit: Boolean = true, onRestaurantAdded: () -> Unit) {
    val context = LocalContext.current
    val language = LocalLanguage.current
    val dashedBorderColor = contentBorderColor()
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var cuisine by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") }
    var restaurantDatePickerOpen by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("хочу") }
    var priority by remember { mutableStateOf(false) }
    var price by remember { mutableStateOf("€€") }
    var newRestaurantPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var savingRestaurantId by remember { mutableStateOf<String?>(null) }
    var editingRestaurant by remember { mutableStateOf<com.odyssey.travelplanner.data.Restaurant?>(null) }
    var cityPickerOpen by remember { mutableStateOf(false) }
    var restaurantCatalogOpen by remember { mutableStateOf(false) }
    var openCatalogAfterCitySelection by remember { mutableStateOf(false) }
    var uploadingRestaurantId by remember { mutableStateOf<String?>(null) }
    var selectedCity by remember { mutableStateOf("Все города") }
    var cityMenuOpen by remember { mutableStateOf(false) }
    var filterMenuOpen by remember { mutableStateOf(false) }
    var priceFilter by remember { mutableStateOf("") }
    var ratingFilter by remember { mutableStateOf("") }
    var appliedTypeFilter by remember { mutableStateOf("Ресторан") }
    var appliedFeatureFilters by remember { mutableStateOf(setOf<String>()) }
    var draftTypeFilter by remember { mutableStateOf("Ресторан") }
    var draftFeatureFilters by remember { mutableStateOf(emptySet<String>()) }
    var draftPriceFilter by remember { mutableStateOf("") }
    var draftRatingFilter by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun resetRestaurantForm() {
        name = ""
        city = ""
        cuisine = ""
        dateTime = ""
        restaurantDatePickerOpen = false
        address = ""
        status = "хочу"
        priority = false
        price = "€€"
        newRestaurantPhotoUris = emptyList()
        message = null
        cityPickerOpen = false
        restaurantCatalogOpen = false
        openCatalogAfterCitySelection = false
    }

    fun closeRestaurantForm() {
        adding = false
        resetRestaurantForm()
    }

    val tripCityOptions = (
        overview.cities +
            overview.routeLegs.flatMap { listOf(it.from, it.to) } +
        overview.sights.map { it.city } +
            overview.accommodations.map { it.city } +
            overview.restaurants.map { it.city }
        ).flatMap(::splitStoredCityList).map(String::trim).filter(String::isNotBlank).distinctBy(::cityFilterKey)
    val cityOptions = listOf("Все города") + tripCityOptions
    val cityCounts = cityOptions.associateWith { option ->
        if (option == cityOptions.first()) {
            overview.restaurants.size
        } else {
            overview.restaurants.count { restaurant -> cityFilterKey(restaurant.city) == cityFilterKey(option) }
        }
    }
    val visibleRestaurants = overview.restaurants.filter { restaurant ->
        val note = restaurant.note.lowercase()
        val typeMatches = when (appliedTypeFilter) {
            "Бар" -> note.contains("бар") || note.contains("bar")
            "Кафе" -> note.contains("кафе") || note.contains("cafe")
            else -> true
        }
        val featureMatches = appliedFeatureFilters.all { feature ->
            when (feature) {
                "priority" -> restaurant.priority || note.contains("приоритет") || note.contains("priority")
                "dog" -> note.contains("с собакой") || note.contains("dog")
                "reservation" -> restaurant.status == "бронь" || note.contains("бронь") || note.contains("reserv")
                "vegan" -> note.contains("веган") || note.contains("vegan")
                else -> true
            }
        }
        val ratingMatches = ratingFilter.isBlank() || (restaurant.rating ?: 0.0) >= (ratingFilter.removeSuffix("+").toDoubleOrNull() ?: 0.0)
        (selectedCity == "Все города" || cityFilterKey(restaurant.city) == cityFilterKey(selectedCity)) &&
            typeMatches &&
            featureMatches &&
            (priceFilter.isBlank() || restaurant.price == priceFilter) &&
            ratingMatches
    }
    val filterCount = listOf(
        appliedTypeFilter != "Ресторан",
        priceFilter.isNotBlank(),
        ratingFilter.isNotBlank(),
    ).count { it } + appliedFeatureFilters.size
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val restaurantId = uploadingRestaurantId ?: return@rememberLauncherForActivityResult
        if (uris.isEmpty()) {
            uploadingRestaurantId = null
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching {
                val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                uris.forEach { uri ->
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Не удалось прочитать изображение")
                repository.addRestaurantPhoto(tripId, restaurantId, bytes)
                }
            }.onSuccess {
                actionMessage = null
                onRestaurantAdded()
            }.onFailure {
                actionMessage = it.message ?: localized(language, "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0444\u043e\u0442\u043e. \u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442 \u0438 \u043f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u0435 \u043f\u043e\u043f\u044b\u0442\u043a\u0443.", "Could not upload the photo. Check your connection and try again.", "No se pudo subir la foto. Comprueba la conexi\u00f3n e int\u00e9ntalo de nuevo.", "Foto konnte nicht hochgeladen werden. Pr\u00fcfen Sie die Verbindung und versuchen Sie es erneut.")
            }
            uploadingRestaurantId = null
        }
    }
    val newRestaurantPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            newRestaurantPhotoUris = (newRestaurantPhotoUris + uris).distinctBy(Uri::toString)
        }
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            top = 18.dp,
            end = 18.dp,
            bottom = 30.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (canEdit) item {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1.31f)
                        .fillMaxHeight()
                        .shadow(5.dp, RoundedCornerShape(13.dp), clip = false, ambientColor = OdysseyPurpleShadowSoft, spotColor = OdysseyPurpleShadowSoft)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(primaryColor(), OdysseyPurpleGradientEnd)))
                        .clickable { cityMenuOpen = !cityMenuOpen }
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OdysseyLocationIcon(15.dp, primaryContentColor())
                    Text(
                        localizedCityFilter(selectedCity),
                        color = primaryContentColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    OdysseyChevronDown(16.dp, primaryContentColor())
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(13.dp))
                        .background(cardSurfaceColor())
                        .border(1.dp, contentBorderColor(), RoundedCornerShape(13.dp))
                        .clickable {
                            cityMenuOpen = false
                            filterMenuOpen = true
                        }
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    OdysseyFilterIcon(15.dp)
                    Text(
                        localized("Фильтры", "Filters", "Filtros", "Filter"),
                        color = labelColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                    Box(
                        modifier = Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).background(primaryColor()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            filterCount.toString(),
                            color = primaryContentColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(47.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tintedSurfaceColor())
                    .drawBehind {
                        val stroke = 1.5.dp.toPx()
                        drawRoundRect(
                            color = dashedBorderColor,
                            topLeft = Offset(stroke / 2f, stroke / 2f),
                            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                            cornerRadius = CornerRadius(14.dp.toPx() - stroke / 2f),
                            style = Stroke(
                                width = stroke,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                            ),
                        )
                    }
                    .clickable { resetRestaurantForm(); adding = true; actionMessage = null },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                OdysseyPlusIcon(18.dp)
                Text(
                    localized("Добавить ресторан", "Add restaurant", "Añadir restaurante", "Restaurant hinzufügen"),
                    color = primaryColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 14.5.sp,
                    lineHeight = 19.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        if (actionMessage != null) {
            item {
                Text(actionMessage!!, color = OdysseyError, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }
        item {
            RestaurantMapCard(
                restaurants = visibleRestaurants,
                cityCoordinates = overview.cityCoordinates,
                modifier = Modifier.padding(top = 11.dp),
            )
        }
        if (visibleRestaurants.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.Restaurant,
                    title = if (overview.restaurants.isEmpty()) localized("Рестораны пока не добавлены", "No restaurants added yet", "Aún no se han añadido restaurantes", "Noch keine Restaurants hinzugefügt") else localized("Ничего не найдено", "Nothing found", "No se encontró nada", "Nichts gefunden"),
                    body = localized("Добавьте место или измените фильтры", "Add a place or change the filters", "Añada un lugar o cambie los filtros", "Fügen Sie einen Ort hinzu oder ändern Sie die Filter"),
                )
            }
        } else {
            itemsIndexed(visibleRestaurants, key = { _, restaurant -> restaurant.id }) { index, restaurant ->
                RestaurantCard(
                    restaurant,
                    savingRestaurantId == restaurant.id,
                    uploadingRestaurantId == restaurant.id,
                    canEdit = canEdit,
                    onEdit = { if (canEdit) editingRestaurant = restaurant },
                    onAddPhoto = { uploadingRestaurantId = restaurant.id; photoPicker.launch("image/*") },
                    modifier = Modifier.padding(top = if (index == 0) 16.dp else 13.dp),
                ) { status ->
                    scope.launch {
                        savingRestaurantId = restaurant.id
                        runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateRestaurantStatus(tripId, restaurant.id, status) }
                            .onSuccess {
                                actionMessage = null
                                onRestaurantAdded()
                            }
                            .onFailure {
                                actionMessage = it.message ?: localized(language, "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0441\u0442\u0430\u0442\u0443\u0441. \u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442 \u0438 \u043f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u0435 \u043f\u043e\u043f\u044b\u0442\u043a\u0443.", "Could not save the status. Check your connection and try again.", "No se pudo guardar el estado. Comprueba la conexi\u00f3n e int\u00e9ntalo de nuevo.", "Status konnte nicht gespeichert werden. Pr\u00fcfen Sie die Verbindung und versuchen Sie es erneut.")
                            }
                        savingRestaurantId = null
                    }
                }
            }
        }
    }
    if (cityMenuOpen) {
        ModalBottomSheet(
            onDismissRequest = { cityMenuOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = OdysseySheetScrim,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            dragHandle = null,
        ) {
            RestaurantCityFilterSheet(
                options = cityOptions,
                counts = cityCounts,
                selectedCity = selectedCity,
                onSelect = { option -> selectedCity = option; cityMenuOpen = false },
                onClose = { cityMenuOpen = false },
            )
        }
    }
    if (canEdit && editingRestaurant != null) {
        ModalBottomSheet(
            onDismissRequest = { editingRestaurant = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = OdysseySheetScrim,
            shape = RoundedCornerShape(topStart = 29.dp, topEnd = 29.dp),
            dragHandle = null,
        ) {
            RestaurantEditSheet(
                restaurant = editingRestaurant!!,
                tripId = tripId,
                cityOptions = tripCityOptions,
                onClose = { editingRestaurant = null },
                onSaved = {
                    editingRestaurant = null
                    onRestaurantAdded()
                },
            )
        }
    }
    if (filterMenuOpen) {
        ModalBottomSheet(
            onDismissRequest = { filterMenuOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = OdysseySheetScrim,
            shape = RoundedCornerShape(topStart = 29.dp, topEnd = 29.dp),
            dragHandle = null,
        ) {
            RestaurantFilterSheet(
                type = draftTypeFilter,
                features = draftFeatureFilters,
                price = draftPriceFilter,
                rating = draftRatingFilter,
                onTypeChange = { draftTypeFilter = it },
                onFeatureToggle = { feature ->
                    draftFeatureFilters = if (feature in draftFeatureFilters) draftFeatureFilters - feature else draftFeatureFilters + feature
                },
                onPriceChange = { draftPriceFilter = it },
                onRatingChange = { draftRatingFilter = it },
                onReset = {
                    draftTypeFilter = "Ресторан"
                    draftFeatureFilters = emptySet()
                    draftPriceFilter = ""
                    draftRatingFilter = ""
                },
                onApply = {
                    appliedTypeFilter = draftTypeFilter
                    appliedFeatureFilters = draftFeatureFilters
                    priceFilter = draftPriceFilter
                    ratingFilter = draftRatingFilter
                    filterMenuOpen = false
                },
            )
        }
    }
    if (canEdit && adding) {
        ModalBottomSheet(
            onDismissRequest = ::closeRestaurantForm,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = OdysseySheetScrim,
            shape = RoundedCornerShape(topStart = 29.dp, topEnd = 29.dp),
            dragHandle = null,
        ) {
            RestaurantAddSheet(
                name = name,
                city = city,
                cuisine = cuisine,
                dateTime = dateTime,
                price = price,
                address = address,
                status = status,
                priority = priority,
                photoUris = newRestaurantPhotoUris,
                saving = saving,
                message = message,
                onNameChange = { name = it },
                onCityChange = { city = it },
                onCityPickerOpen = {
                    openCatalogAfterCitySelection = false
                    cityPickerOpen = true
                },
                onDatePickerOpen = {
                    restaurantDatePickerOpen = true
                },
                onCuisineChange = { cuisine = it },
                onDateTimeChange = { dateTime = it },
                onPriceChange = { price = it },
                onAddressChange = { address = it },
                onStatusChange = { status = it },
                onPriorityChange = { priority = !priority },
                onPickPhoto = { newRestaurantPhotoPicker.launch("image/*") },
                onCatalogOpen = {
                    if (city.isBlank()) {
                        openCatalogAfterCitySelection = true
                        cityPickerOpen = true
                    } else {
                        openCatalogAfterCitySelection = false
                        restaurantCatalogOpen = true
                    }
                },
                onClose = ::closeRestaurantForm,
                onSave = {
                    scope.launch {
                        saving = true
                        var createdRestaurantId: String? = null
                        runCatching {
                            val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                            val restaurantId = repository.addRestaurantDetails(
                                com.odyssey.travelplanner.data.RestaurantInput(
                                    name = name,
                                    city = city,
                                    status = status,
                                    note = cuisine,
                                    price = price,
                                    link = address,
                                    date = dateTime,
                                    priority = priority,
                                ),
                                tripId,
                            )
                            createdRestaurantId = restaurantId
                            newRestaurantPhotoUris.forEach { uri ->
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                    ?: error("Не удалось прочитать изображение")
                                repository.addRestaurantPhoto(tripId, restaurantId, bytes)
                            }
                        }.onSuccess {
                            closeRestaurantForm()
                            onRestaurantAdded()
                        }.onFailure {
                            createdRestaurantId?.let { restaurantId ->
                                runCatching {
                                    SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                                        .deleteTripItem(tripId, "restaurants", restaurantId)
                                }
                            }
                            message = it.message ?: localized(language, "Не удалось сохранить ресторан", "Could not save restaurant", "No se pudo guardar el restaurante", "Restaurant konnte nicht gespeichert werden")
                        }
                        saving = false
                    }
                },
            )
        }
    }
    if (canEdit && adding && restaurantDatePickerOpen) {
        AccommodationCalendarDialog(
            initialValue = dateTime,
            onDismiss = { restaurantDatePickerOpen = false },
            onConfirm = { selectedDate ->
                dateTime = selectedDate
                restaurantDatePickerOpen = false
            },
        )
    }
    if (canEdit && adding && cityPickerOpen) {
        ModalBottomSheet(
            onDismissRequest = { cityPickerOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = OdysseySheetScrim,
            shape = RoundedCornerShape(topStart = 29.dp, topEnd = 29.dp),
            dragHandle = null,
        ) {
            RestaurantAddCitySheet(
                options = tripCityOptions,
                selectedCity = city,
                onSelect = { option ->
                    city = option
                    cityPickerOpen = false
                    if (openCatalogAfterCitySelection) {
                        openCatalogAfterCitySelection = false
                        restaurantCatalogOpen = true
                    }
                },
                onClose = { cityPickerOpen = false },
            )
        }
    }
    if (canEdit && adding && restaurantCatalogOpen) {
        ModalBottomSheet(
            onDismissRequest = { restaurantCatalogOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = OdysseySheetScrim,
            shape = RoundedCornerShape(topStart = 29.dp, topEnd = 29.dp),
            dragHandle = null,
        ) {
            RestaurantCatalogSheet(
                city = city,
                onSelect = { entry ->
                    name = entry.name(language)
                    cuisine = entry.cuisine
                    address = entry.address
                    restaurantCatalogOpen = false
                    message = null
                },
                onClose = { restaurantCatalogOpen = false },
            )
        }
    }
}

@Composable
internal fun RestaurantAddCitySheet(
    options: List<String>,
    selectedCity: String,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val maxSheetHeight = maxHeight * 0.8f
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 18.dp + navigationBarInset),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(40.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(contentBorderColor()),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = localized("\u0413\u043e\u0440\u043e\u0434", "City", "Ciudad", "Stadt"),
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(secondarySurfaceColor())
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = localized("\u0417\u0430\u043a\u0440\u044b\u0442\u044c", "Close", "Cerrar", "Schlie\u00dfen"),
                        tint = secondaryTextColor(),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            if (options.isEmpty()) {
                Text(
                    text = localized("\u0412 \u043f\u043e\u0435\u0437\u0434\u043a\u0435 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442 \u0433\u043e\u0440\u043e\u0434\u043e\u0432", "No cities in this trip yet", "Aún no hay ciudades en este viaje", "Noch keine Städte in dieser Reise"),
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                options.forEach { option ->
                    val active = option == selectedCity
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) tintedSurfaceColor() else cardSurfaceColor())
                            .border(1.6.dp, if (active) primaryColor() else contentBorderColor(), RoundedCornerShape(14.dp))
                            .clickable { onSelect(option) }
                            .padding(horizontal = 15.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = localizedCityName(option),
                            color = if (active) primaryColor() else contentTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 15.5.sp,
                            lineHeight = 20.sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                            modifier = Modifier.weight(1f),
                        )
                        if (active) {
                            Box(
                                modifier = Modifier.size(22.dp).clip(CircleShape).background(primaryColor()),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = primaryContentColor(), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OdysseySortIcon(tint: Color? = null) {
    val resolvedTint = tint ?: primaryColor()
    Canvas(modifier = Modifier.size(20.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        val left = 2.dp.toPx()
        val right = size.width - 2.dp.toPx()
        val y1 = 4.dp.toPx()
        val y2 = 9.dp.toPx()
        val y3 = 14.dp.toPx()
        drawLine(resolvedTint, Offset(left, y1), Offset(right, y1), strokeWidth, StrokeCap.Round)
        drawLine(resolvedTint, Offset(left, y2), Offset(left + (right - left) * 0.72f, y2), strokeWidth, StrokeCap.Round)
        drawLine(resolvedTint, Offset(left, y3), Offset(left + (right - left) * 0.46f, y3), strokeWidth, StrokeCap.Round)
    }
}

