package com.odyssey.travelplanner.ui.screen.createtrip

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException
import java.util.Locale
import java.util.Calendar
import com.odyssey.travelplanner.data.AuthSessionRequiredException
import com.odyssey.travelplanner.data.CityCatalogEntry
import com.odyssey.travelplanner.data.CityCatalogRepository
import com.odyssey.travelplanner.data.CityLocation
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.TripCard
import com.odyssey.travelplanner.data.cityCatalog
import com.odyssey.travelplanner.data.cityCatalogEntry
import com.odyssey.travelplanner.data.cityFlag
import com.odyssey.travelplanner.data.countryFlag
import com.odyssey.travelplanner.data.normalizeCityAlias
import com.odyssey.travelplanner.ui.domain.NewTripPhoto
import com.odyssey.travelplanner.ui.domain.parseRouteDate
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.normalizeLanguage
import com.odyssey.travelplanner.ui.icons.OdysseyCalendarIcon
import com.odyssey.travelplanner.ui.screen.auth.RamingoBrand
import com.odyssey.travelplanner.ui.theme.LocalDarkTheme
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyBackground
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBackground
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBorder
import com.odyssey.travelplanner.ui.theme.OdysseyDarkMuted
import com.odyssey.travelplanner.ui.theme.OdysseyDarkSurface
import com.odyssey.travelplanner.ui.theme.OdysseyDarkTint
import com.odyssey.travelplanner.ui.theme.OdysseyError
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
@OptIn(ExperimentalMaterial3Api::class)
internal fun CreateTripScreen(
    onBack: () -> Unit,
    onCreated: (TripCard) -> Unit,
    onAuthRequired: () -> Unit,
) {
    val darkTheme = LocalDarkTheme.current
    val language = LocalLanguage.current
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var cities by remember { mutableStateOf("") }
    var cityDialogOpen by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf<String?>(null) }
    var citySearch by remember { mutableStateOf("") }
    var citySearchResults by remember { mutableStateOf(cityCatalog) }
    var citySearchLoading by remember { mutableStateOf(false) }
    var selectedCatalogEntries by remember { mutableStateOf<Map<String, CityCatalogEntry>>(emptyMap()) }
    val cityCatalogContext = LocalContext.current
    val cityCatalogRepository = remember(cityCatalogContext) { CityCatalogRepository(cityCatalogContext.assets) }
    val cityList = remember(cities) {
        cities.split(",").map(String::trim).filter(String::isNotBlank)
    }
    val selectedCityKeys = remember(cityList, selectedCatalogEntries) {
        (selectedCatalogEntries.keys + cityList.mapNotNull { cityCatalogEntry(it)?.key }).toSet()
    }
    val normalizedCitySearch = citySearch
        .trim()
        .lowercase(Locale.ROOT)
        .replace('ё', 'е')
    val filteredCatalogCities = remember(citySearchResults, selectedCityKeys) {
        citySearchResults.sortedByDescending { entry -> entry.key in selectedCityKeys }
    }
    val duplicateCityNames = remember(filteredCatalogCities, language) {
        filteredCatalogCities
            .groupingBy { normalizeCityAlias(it.localized(language)) }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
    }
    val duplicateCityCountryNames = remember(filteredCatalogCities, language) {
        filteredCatalogCities
            .groupingBy { entry ->
                "${normalizeCityAlias(entry.localized(language))}|${entry.countryCode.uppercase(Locale.ROOT)}"
            }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
    }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var newTripPhotos by remember { mutableStateOf<List<NewTripPhoto>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val newTripPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val knownUris = newTripPhotos.mapTo(mutableSetOf()) { it.uri.toString() }
        newTripPhotos = newTripPhotos + uris
            .filter { knownUris.add(it.toString()) }
            .map(::NewTripPhoto)
        message = null
    }

    LaunchedEffect(cityDialogOpen, normalizedCitySearch, language) {
        if (!cityDialogOpen) return@LaunchedEffect
        delay(220)
        citySearchLoading = true
        try {
            citySearchResults = cityCatalogRepository.search(citySearch, language)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            citySearchResults = emptyList()
        } finally {
            citySearchLoading = false
        }
    }

    fun storedValueFor(entry: CityCatalogEntry): String = entry.selectionValue(language)

    fun entryForStoredCity(value: String): CityCatalogEntry? = selectedCatalogEntries.values.firstOrNull { entry ->
        val cityPart = value.substringBefore(" — ").trim()
        entry.aliases.contains(normalizeCityAlias(cityPart)) ||
            listOf(entry.russian, entry.english, entry.spanish, entry.german).any { name ->
                name.equals(cityPart, ignoreCase = true)
            }
    } ?: cityCatalogEntry(value)

    fun toggleCatalogCity(entry: CityCatalogEntry) {
        val existingCity = cityList.firstOrNull { entryForStoredCity(it)?.key == entry.key }
        cities = if (existingCity != null) {
            selectedCatalogEntries = selectedCatalogEntries - entry.key
            cityList.filterNot { entryForStoredCity(it)?.key == entry.key }.joinToString(", ")
        } else {
            selectedCatalogEntries = selectedCatalogEntries + (entry.key to entry)
            (cityList + storedValueFor(entry)).joinToString(", ")
        }
        message = null
    }

    fun openCityPicker() {
        citySearch = ""
        message = null
        cityDialogOpen = true
    }

    fun save() {
        if (title.isBlank()) title = "\u0411\u0435\u0437 \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u044f"
        val unsupportedCity = cityList.firstOrNull { entryForStoredCity(it) == null }
        if (unsupportedCity != null) {
            message = localized(
                language,
                "Выберите город из каталога",
                "Choose a city from the catalog",
                "Elija una ciudad del catálogo",
                "Wählen Sie eine Stadt aus dem Katalog",
            )
            return
        }
        if (
            startDate.isNotBlank() &&
            endDate.isNotBlank() &&
            parseRouteDate(startDate)?.let { start ->
                parseRouteDate(endDate)?.let { end -> end.timeInMillis < start.timeInMillis }
            } == true
        ) {
            message = localized(language, "Дата окончания не может быть раньше даты начала", "The end date cannot be before the start date", "La fecha de finalización no puede ser anterior a la de inicio", "Das Enddatum darf nicht vor dem Startdatum liegen")
            return
        }
        val selectedPhotoItems = newTripPhotos
        scope.launch {
            saving = true
            message = null
            val cityCoordinates = cityList.mapNotNull { city ->
                entryForStoredCity(city)?.let { entry ->
                    city to CityLocation(entry.latitude, entry.longitude)
                }
            }.toMap()
            if (!SupabaseProvider.ensureActiveSession()) {
                onAuthRequired()
                saving = false
                return@launch
            }
            runCatching {
                val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                val created = repository.createTrip(title, startDate, endDate, cities, cityCoordinates)
                try {
                    selectedPhotoItems.forEach { photo ->
                        val bytes = context.contentResolver.openInputStream(photo.uri)?.use { it.readBytes() }
                            ?: error(localized(language, "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0447\u0438\u0442\u0430\u0442\u044c \u0438\u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u0435", "Could not read the image", "No se pudo leer la imagen", "Das Bild konnte nicht gelesen werden"))
                        repository.addCoverPhoto(created.id, bytes, photo.city)
                    }
                } catch (photoError: Throwable) {
                    runCatching { repository.deleteTrip(created.id) }
                    throw photoError
                }
                created
            }.onSuccess { onCreated(it) }.onFailure { error ->
                if (error is AuthSessionRequiredException) {
                    onAuthRequired()
                } else {
                    message = error.message ?: localized(language, "Не удалось создать путешествие", "Could not create trip", "No se pudo crear el viaje", "Reise konnte nicht erstellt werden")
                }
            }
            saving = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (darkTheme) OdysseyDarkBackground else OdysseyBackground)
            .padding(WindowInsets.statusBars.asPaddingValues()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clickable { onBack() },
            ) {
                Icon(Icons.Outlined.Menu, contentDescription = localized("Назад", "Back", "Atrás", "Zurück"), tint = contentTextColor(), modifier = Modifier.size(22.dp))
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                RamingoBrand()
            }
            Spacer(Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 30.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        ) {
            Text(
                localized("Новое путешествие", "New trip", "Nuevo viaje", "Neue Reise"),
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 30.sp,
                lineHeight = 31.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                localized("Заполните данные путешествия", "Fill in your trip details", "Complete los datos del viaje", "Füllen Sie die Reisedaten aus"),
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W500,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 8.dp),
            )

            CreateTripCoverPhoto(
                language = language,
                photoUris = newTripPhotos.map { it.uri },
                onPickPhoto = { if (!saving) newTripPhotoPicker.launch("image/*") },
                onRemovePhoto = { uri -> newTripPhotos = newTripPhotos.filterNot { it.uri == uri } },
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 20.dp),
            ) {
                TripCreateField(
                    label = localized("Название поездки", "Trip name", "Nombre del viaje", "Name der Reise"),
                    placeholder = localized("Введите название", "Enter trip name", "Escriba el nombre del viaje", "Reisename eingeben"),
                    value = title,
                    onValueChange = { title = it },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    TripCreateDateField(
                        label = localized("Начало", "Start", "Inicio", "Beginn"),
                        placeholder = localized("Выберите дату", "Choose date", "Elija una fecha", "Datum auswählen"),
                        value = startDate,
                        onClick = { datePickerTarget = "start" },
                        modifier = Modifier.weight(1f),
                    )
                    TripCreateDateField(
                        label = localized("Конец", "End", "Fin", "Ende"),
                        placeholder = localized("Выберите дату", "Choose date", "Elija una fecha", "Datum auswählen"),
                        value = endDate,
                        onClick = { datePickerTarget = "end" },
                        modifier = Modifier.weight(1f),
                    )
                }
                Column {
                    Text(
                        localized("Города", "Cities", "Ciudades", "Städte"),
                        color = secondaryTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W700,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 7.dp),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 49.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .border(1.5.dp, if (darkTheme) OdysseyDarkBorder else contentBorderColor(), RoundedCornerShape(13.dp))
                            .clickable(onClick = ::openCityPicker),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(11.dp)
                                .horizontalScroll(rememberScrollState()),
                        ) {
                            cityList.forEach { city ->
                                TripCityChip(city) {
                                    val removedCityKey = entryForStoredCity(city)?.key
                                    removedCityKey?.let { key ->
                                        selectedCatalogEntries = selectedCatalogEntries - key
                                    }
                                    cities = cityList.filterNot { it == city }.joinToString(", ")
                                    newTripPhotos = newTripPhotos.map { photo ->
                                        if (photo.city == city || (removedCityKey != null && entryForStoredCity(photo.city)?.key == removedCityKey)) {
                                            photo.copy(city = "")
                                        } else {
                                            photo
                                        }
                                    }
                                }
                            }
                            Text(
                                localized("+ добавить…", "+ add…", "+ add…", "+ hinzufügen…"),
                                color = secondaryTextColor(),
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W600,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }

            CreateTripPhotoCityAssignments(
                language = language,
                photos = newTripPhotos,
                cityOptions = cityList,
                onCityChange = { uri, city ->
                    newTripPhotos = newTripPhotos.map { photo ->
                        if (photo.uri == uri) photo.copy(city = city) else photo
                    }
                },
            )

            if (message != null) {
                Text(message!!, color = OdysseyError, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            }
            Button(
                onClick = ::save,
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()),
                shape = RoundedCornerShape(15.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp).height(54.dp),
            ) {
                Text(if (saving) localized("Создаём…", "Creating…", "Creando…", "Wird erstellt…") else localized("Создать путешествие", "Create trip", "Crear viaje", "Reise erstellen"), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 16.sp)
            }
        }
    }

    if (cityDialogOpen) {
        ModalBottomSheet(
            onDismissRequest = {
                cityDialogOpen = false
                citySearch = ""
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(43.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (darkTheme) OdysseyDarkMuted else Color(0xFF9996A5)),
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            localized("Выберите города", "Choose cities", "Elige ciudades", "Städte auswählen"),
                            color = contentTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 22.sp,
                        )
                        Text(
                            localized("Добавьте один или несколько городов", "Add one or more cities", "Añade una o varias ciudades", "Fügen Sie eine oder mehrere Städte hinzu"),
                            color = secondaryTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W500,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (darkTheme) OdysseyDarkTint else Color(0xFFEEEDF4))
                            .clickable {
                                cityDialogOpen = false
                                citySearch = ""
                            },
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"),
                            tint = secondaryTextColor(),
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                OutlinedTextField(
                    value = citySearch,
                    onValueChange = { citySearch = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            localized("Поиск города", "Search city", "Buscar ciudad", "Stadt suchen"),
                            color = secondaryTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W500,
                        )
                    },
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor(),
                        unfocusedBorderColor = contentBorderColor(),
                        focusedContainerColor = cardSurfaceColor(),
                        unfocusedContainerColor = cardSurfaceColor(),
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )

                if (citySearchLoading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                    ) {
                        CircularProgressIndicator(color = primaryColor(), strokeWidth = 2.5.dp)
                    }
                } else if (filteredCatalogCities.isEmpty()) {
                    Text(
                        localized("Ничего не найдено", "No cities found", "No se encontraron ciudades", "Keine Städte gefunden"),
                        color = secondaryTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W600,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 28.dp),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 14.dp),
                    ) {
                        items(filteredCatalogCities, key = { it.key }) { entry ->
                            val selected = entry.key in selectedCityKeys
                            val localizedName = entry.localized(language)
                            val normalizedName = normalizeCityAlias(localizedName)
                            val countryLabel = entry.countryName.ifBlank { entry.countryCode }
                            val sameName = normalizedName in duplicateCityNames
                            val sameNameAndCountry = "${normalizedName}|${entry.countryCode.uppercase(Locale.ROOT)}" in duplicateCityCountryNames
                            val displayName = if (sameName && countryLabel.isNotBlank()) {
                                "$localizedName — $countryLabel"
                            } else {
                                localizedName
                            }
                            val secondaryLabel = if (sameNameAndCountry) {
                                val coordinates = String.format(Locale.US, "%.3f, %.3f", entry.latitude, entry.longitude)
                                listOf(countryLabel, coordinates).filter(String::isNotBlank).joinToString(" · ")
                            } else if (!sameName) {
                                countryLabel
                            } else {
                                ""
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(11.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(if (selected) tintedSurfaceColor() else cardSurfaceColor())
                                    .border(1.dp, if (selected) primaryColor().copy(alpha = 0.45f) else contentBorderColor(), RoundedCornerShape(13.dp))
                                    .clickable { toggleCatalogCity(entry) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Text(countryFlag(entry.countryCode) ?: cityFlag(entry.russian), fontSize = 20.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        displayName,
                                        color = contentTextColor(),
                                        fontFamily = Manrope,
                                        fontWeight = FontWeight.W700,
                                        fontSize = 14.sp,
                                    )
                                    if (secondaryLabel.isNotBlank()) {
                                        Text(
                                            secondaryLabel,
                                            color = secondaryTextColor(),
                                            fontFamily = Manrope,
                                            fontWeight = FontWeight.W500,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                if (selected) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(23.dp)
                                            .clip(CircleShape)
                                            .background(primaryColor()),
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = primaryContentColor(), modifier = Modifier.size(15.dp))
                                    }
                                } else {
                                    Text("+", color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        cityDialogOpen = false
                        citySearch = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(52.dp),
                ) {
                    Text(
                        localized("Готово", "Done", "Listo", "Fertig"),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 15.sp,
                    )
                }
                Text(
                    "Данные городов: Countries States Cities Database · ODbL 1.0",
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W500,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
    datePickerTarget?.let { target ->
        val pickingStart = target == "start"
        TripCreationCalendarDialog(
            title = localized("Выберите дату", "Choose date", "Elige una fecha", "Datum auswählen"),
            initialValue = if (pickingStart) startDate else endDate.ifBlank { startDate },
            minimumDate = if (pickingStart) null else parseRouteDate(startDate),
            onDismiss = { datePickerTarget = null },
            onConfirm = { selectedDate ->
                if (pickingStart) startDate = selectedDate else endDate = selectedDate
                datePickerTarget = null
            },
        )
    }
}

@Composable
internal fun CreateTripPhotoCityAssignments(
    language: String,
    photos: List<NewTripPhoto>,
    cityOptions: List<String>,
    onCityChange: (Uri, String) -> Unit,
) {
    if (photos.isEmpty()) return

    val darkTheme = LocalDarkTheme.current
    var expandedPhoto by remember { mutableStateOf<Uri?>(null) }
    val rowShape = RoundedCornerShape(14.dp)

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            localized(language, "Фото по городам", "Photos by city", "Fotos por ciudad", "Fotos nach Stadt"),
            color = secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W700,
            fontSize = 12.sp,
        )
        Text(
            localized(
                language,
                "Укажите город для фото — оно появится в карточке погоды этого города",
                "Choose a city for each photo — it will appear on that city's weather card",
                "Elige una ciudad para cada foto: aparecerá en la tarjeta del tiempo de esa ciudad",
                "Wähle für jedes Foto eine Stadt — es erscheint auf der Wetterkarte dieser Stadt",
            ),
            color = secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W500,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        photos.forEachIndexed { index, photo ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(rowShape)
                    .background(if (darkTheme) OdysseyDarkSurface else cardSurfaceColor())
                    .border(1.dp, if (darkTheme) OdysseyDarkBorder else contentBorderColor(), rowShape)
                    .padding(9.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = photo.uri,
                        contentDescription = localized(language, "Фото путешествия", "Trip photo", "Foto del viaje", "Reisefoto"),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(
                            if (index == 0) {
                                localized(language, "Обложка путешествия", "Trip cover", "Portada del viaje", "Reisetitelbild")
                            } else {
                                localized(language, "Фото ${index + 1}", "Photo ${index + 1}", "Foto ${index + 1}", "Foto ${index + 1}")
                            },
                            color = contentTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W700,
                            fontSize = 12.sp,
                        )
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(top = 5.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (darkTheme) OdysseyDarkTint else tintedSurfaceColor())
                                    .clickable(enabled = cityOptions.isNotEmpty()) { expandedPhoto = photo.uri }
                                    .padding(horizontal = 9.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    photo.city.ifBlank {
                                        if (cityOptions.isEmpty()) {
                                            localized(language, "Сначала добавьте города", "Add cities first", "Añade ciudades primero", "Füge zuerst Städte hinzu")
                                        } else {
                                            localized(language, "Выбрать город", "Choose a city", "Elegir ciudad", "Stadt auswählen")
                                        }
                                    },
                                    color = if (photo.city.isBlank()) secondaryTextColor() else primaryColor(),
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W700,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (cityOptions.isNotEmpty()) {
                                    Icon(
                                        Icons.Outlined.KeyboardArrowDown,
                                        contentDescription = localized(language, "Выбрать город", "Choose a city", "Elegir ciudad", "Stadt auswählen"),
                                        tint = primaryColor(),
                                        modifier = Modifier.size(16.dp).padding(start = 2.dp),
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = expandedPhoto == photo.uri,
                                onDismissRequest = { expandedPhoto = null },
                            ) {
                                cityOptions.forEach { city ->
                                    DropdownMenuItem(
                                        text = { Text(city, fontFamily = Manrope, fontWeight = FontWeight.W600) },
                                        onClick = {
                                            onCityChange(photo.uri, city)
                                            expandedPhoto = null
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (index < photos.lastIndex) Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
internal fun CreateTripCoverPhoto(
    language: String,
    photoUris: List<Uri>,
    onPickPhoto: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
) {
    val darkTheme = LocalDarkTheme.current
    val coverShape = RoundedCornerShape(15.dp)
    val thumbnailShape = RoundedCornerShape(11.dp)

    Column(modifier = Modifier.padding(top = 18.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
        ) {
            Text(
                localized(language, "Фото путешествия", "Trip photo", "Foto del viaje", "Reisefoto"),
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                fontSize = 12.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                localized(language, "необязательно", "optional", "opcional", "optional"),
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W500,
                fontSize = 10.sp,
            )
        }

        if (photoUris.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(coverShape)
                    .background(if (darkTheme) OdysseyDarkTint else tintedSurfaceColor())
                    .border(1.5.dp, primaryColor().copy(alpha = 0.5f), coverShape)
                    .clickable(onClick = onPickPhoto),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("+", color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 29.sp, lineHeight = 29.sp)
                    Text(
                        localized(language, "Добавить фото обложки", "Add cover photo", "Añadir foto de portada", "Titelbild hinzufügen"),
                        color = primaryColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W700,
                        fontSize = 12.sp,
                    )
                    Text(
                        localized(language, "По желанию", "Optional", "Opcional", "Optional"),
                        color = secondaryTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W500,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(coverShape)
                    .border(1.dp, if (darkTheme) OdysseyDarkBorder else contentBorderColor(), coverShape),
            ) {
                AsyncImage(
                    model = photoUris.first(),
                    contentDescription = localized(language, "Обложка путешествия", "Trip cover", "Portada del viaje", "Reisetitelbild"),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.58f))
                        .padding(vertical = 6.dp, horizontal = 9.dp),
                ) {
                    Text(
                        localized(language, "Обложка путешествия", "Trip cover", "Portada del viaje", "Reisetitelbild"),
                        color = Color.White,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W700,
                        fontSize = 11.sp,
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(25.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.62f))
                        .clickable { onRemovePhoto(photoUris.first()) },
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = localized(language, "Удалить обложку", "Remove cover", "Eliminar portada", "Titelbild entfernen"),
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(thumbnailShape)
                        .background(if (darkTheme) OdysseyDarkTint else tintedSurfaceColor())
                        .border(1.5.dp, primaryColor().copy(alpha = 0.5f), thumbnailShape)
                        .clickable(onClick = onPickPhoto),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("+", color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 20.sp, lineHeight = 20.sp)
                        Text(
                            localized(language, "Ещё", "More", "Más", "Mehr"),
                            color = primaryColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W700,
                            fontSize = 9.sp,
                        )
                    }
                }
                photoUris.drop(1).forEach { uri ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(thumbnailShape)
                            .border(1.dp, if (darkTheme) OdysseyDarkBorder else contentBorderColor(), thumbnailShape),
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = localized(language, "Дополнительное фото", "Additional photo", "Foto adicional", "Zusätzliches Foto"),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.62f))
                                .clickable { onRemovePhoto(uri) },
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = localized(language, "Удалить фото", "Remove photo", "Eliminar foto", "Foto entfernen"),
                                tint = Color.White,
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TripCreateDateField(
    label: String,
    placeholder: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val darkTheme = LocalDarkTheme.current
    val shape = RoundedCornerShape(13.dp)
    val displayedValue = displayTripCreationDate(value)
    Column(modifier = modifier) {
        Text(
            label,
            color = secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W700,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(47.dp)
                .clip(shape)
                .background(cardSurfaceColor())
                .border(1.5.dp, if (darkTheme) OdysseyDarkBorder else contentBorderColor(), shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = displayedValue.ifBlank { placeholder },
                color = if (displayedValue.isBlank()) secondaryTextColor() else contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(start = 14.dp, end = 42.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
            ) {
                OdysseyCalendarIcon(16.dp, if (displayedValue.isBlank()) secondaryTextColor() else primaryColor())
            }
        }
    }
}

internal fun displayTripCreationDate(value: String): String {
    val match = Regex("(\\d{4})-(\\d{2})-(\\d{2})").matchEntire(value.trim()) ?: return value
    return "${match.groupValues[3]}.${match.groupValues[2]}.${match.groupValues[1]}"
}

@Composable
internal fun TripCreationCalendarDialog(
    title: String,
    initialValue: String,
    minimumDate: Calendar?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val language = normalizeLanguage(LocalLanguage.current)
    val minimumMillis = minimumDate?.timeInMillis
    val initial = remember(initialValue, minimumMillis) {
        val candidate = parseRouteDate(initialValue) ?: minimumDate ?: Calendar.getInstance()
        if (minimumDate != null && candidate.timeInMillis < minimumDate.timeInMillis) {
            Calendar.getInstance().apply { timeInMillis = minimumDate.timeInMillis }
        } else {
            candidate
        }
    }
    var displayedYear by remember(initialValue, minimumMillis) { mutableStateOf(initial.get(Calendar.YEAR)) }
    var displayedMonth by remember(initialValue, minimumMillis) { mutableStateOf(initial.get(Calendar.MONTH)) }
    var selectedYear by remember(initialValue, minimumMillis) { mutableStateOf(initial.get(Calendar.YEAR)) }
    var selectedMonth by remember(initialValue, minimumMillis) { mutableStateOf(initial.get(Calendar.MONTH)) }
    var selectedDay by remember(initialValue, minimumMillis) { mutableStateOf(initial.get(Calendar.DAY_OF_MONTH)) }

    val monthNames = when (language) {
        "EN" -> listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        "ES" -> listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
        "DE" -> listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")
        else -> listOf("январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь")
    }
    val weekDays = when (language) {
        "EN" -> listOf("M", "T", "W", "T", "F", "S", "S")
        "ES" -> listOf("L", "M", "X", "J", "V", "S", "D")
        "DE" -> listOf("M", "D", "M", "D", "F", "S", "S")
        else -> listOf("П", "В", "С", "Ч", "П", "С", "В")
    }
    val daysInMonth = Calendar.getInstance().apply {
        clear()
        set(displayedYear, displayedMonth + 1, 0)
    }.get(Calendar.DAY_OF_MONTH)
    val firstDay = Calendar.getInstance().apply {
        clear()
        set(displayedYear, displayedMonth, 1)
    }.get(Calendar.DAY_OF_WEEK)
    val leadingEmpty = (firstDay - Calendar.MONDAY + 7) % 7
    val displayedMonthIndex = displayedYear * 12 + displayedMonth
    val minimumMonthIndex = minimumDate?.let { it.get(Calendar.YEAR) * 12 + it.get(Calendar.MONTH) }
    val canGoPrevious = minimumMonthIndex == null || displayedMonthIndex > minimumMonthIndex

    fun dateCalendar(day: Int): Calendar = Calendar.getInstance().apply {
        clear()
        set(displayedYear, displayedMonth, day)
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x660F0F19))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(352.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(cardSurfaceColor())
                    .border(1.dp, contentBorderColor(), RoundedCornerShape(28.dp))
                    .padding(horizontal = 18.dp, vertical = 17.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            color = contentTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 19.sp,
                            lineHeight = 24.sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                        Text(
                            text = "${selectedDay.toString().padStart(2, '0')}.${(selectedMonth + 1).toString().padStart(2, '0')}.$selectedYear",
                            color = secondaryTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W600,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 3.dp),
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(tintedSurfaceColor())
                            .clickable(onClick = onDismiss),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = primaryColor(), modifier = Modifier.size(17.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (canGoPrevious) secondarySurfaceColor() else secondarySurfaceColor().copy(alpha = 0.55f))
                            .clickable(enabled = canGoPrevious) {
                                if (displayedMonth == 0) {
                                    displayedMonth = 11
                                    displayedYear -= 1
                                } else {
                                    displayedMonth -= 1
                                }
                            },
                    ) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = localized("Предыдущий месяц", "Previous month", "Mes anterior", "Vorheriger Monat"), tint = if (canGoPrevious) primaryColor() else secondaryTextColor().copy(alpha = 0.45f), modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "${monthNames[displayedMonth].replaceFirstChar { it.uppercase() }} $displayedYear",
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(secondarySurfaceColor())
                            .clickable {
                                if (displayedMonth == 11) {
                                    displayedMonth = 0
                                    displayedYear += 1
                                } else {
                                    displayedMonth += 1
                                }
                            },
                    ) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = localized("Следующий месяц", "Next month", "Mes siguiente", "Nächster Monat"), tint = primaryColor(), modifier = Modifier.size(18.dp).graphicsLayer { rotationY = 180f })
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth().height(22.dp)) {
                    weekDays.forEach { day ->
                        Text(
                            text = day,
                            color = secondaryTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    (0 until 6).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth().height(43.dp)) {
                            (0 until 7).forEach { weekday ->
                                val dayIndex = week * 7 + weekday - leadingEmpty + 1
                                val validDay = dayIndex in 1..daysInMonth
                                val selectable = validDay && (minimumDate == null || dateCalendar(dayIndex).timeInMillis >= minimumDate.timeInMillis)
                                val selected = dayIndex == selectedDay && displayedYear == selectedYear && displayedMonth == selectedMonth
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                ) {
                                    if (validDay) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (selected) primaryColor() else Color.Transparent)
                                                .clickable(enabled = selectable) {
                                                    selectedYear = displayedYear
                                                    selectedMonth = displayedMonth
                                                    selectedDay = dayIndex
                                                },
                                        ) {
                                            Text(
                                                text = dayIndex.toString(),
                                                color = when {
                                                    selected -> primaryContentColor()
                                                    selectable -> contentTextColor()
                                                    else -> secondaryTextColor().copy(alpha = 0.45f)
                                                },
                                                fontFamily = Manrope,
                                                fontWeight = if (selected) FontWeight.W800 else FontWeight.W600,
                                                fontSize = 14.sp,
                                                lineHeight = 18.sp,
                                                textAlign = TextAlign.Center,
                                                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(15.dp))
                            .background(cardSurfaceColor())
                            .border(1.dp, contentBorderColor(), RoundedCornerShape(15.dp))
                            .clickable(onClick = onDismiss),
                    ) {
                        Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(15.dp))
                            .background(primaryColor())
                            .clickable {
                                onConfirm(String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay))
                            },
                    ) {
                        Text(localized("Готово", "Done", "Listo", "Fertig"), color = primaryContentColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                    }
                }
            }
        }
    }
}

@Composable
internal fun TripCreateField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val darkTheme = LocalDarkTheme.current
    val shape = RoundedCornerShape(13.dp)
    Column(modifier = modifier) {
        Text(
            label,
            color = secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W700,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                platformStyle = OdysseyNoFontPadding,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(47.dp)
                .clip(shape)
                .background(cardSurfaceColor())
                .border(1.5.dp, if (darkTheme) OdysseyDarkBorder else contentBorderColor(), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text(placeholder, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 15.sp, lineHeight = 20.sp)
                }
                innerTextField()
            },
        )
    }
}

@Composable
internal fun TripCityChip(city: String, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(tintedSurfaceColor())
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(city, color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp, lineHeight = 17.sp)
        Text("×", color = primaryColor().copy(alpha = 0.6f), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onRemove))
    }
}

