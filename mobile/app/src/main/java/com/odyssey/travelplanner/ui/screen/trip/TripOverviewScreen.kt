package com.odyssey.travelplanner.ui.screen.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odyssey.travelplanner.data.CityCatalogRepository
import com.odyssey.travelplanner.data.CityLocation
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.TripOverview
import com.odyssey.travelplanner.data.WeatherRepository
import com.odyssey.travelplanner.data.WeatherSnapshot
import com.odyssey.travelplanner.data.cityCatalogEntry
import com.odyssey.travelplanner.ui.common.TripOverviewLoading
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedTripTitle
import com.odyssey.travelplanner.ui.screen.trip.budget.BudgetContent
import com.odyssey.travelplanner.ui.screen.trip.lodging.AccommodationContent
import com.odyssey.travelplanner.ui.screen.trip.members.MembersContent
import com.odyssey.travelplanner.ui.screen.trip.overview.OverviewContent
import com.odyssey.travelplanner.ui.screen.trip.photos.PhotosContent
import com.odyssey.travelplanner.ui.screen.trip.restaurants.RestaurantsContent
import com.odyssey.travelplanner.ui.screen.trip.route.TripRouteContent
import com.odyssey.travelplanner.ui.screen.trip.sights.SightsContent
import com.odyssey.travelplanner.ui.theme.LocalDarkTheme
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyBackground
import com.odyssey.travelplanner.ui.theme.OdysseyBorder
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBackground
import com.odyssey.travelplanner.ui.theme.OdysseyError
import com.odyssey.travelplanner.ui.theme.OdysseySurface2
import com.odyssey.travelplanner.ui.theme.OdysseyTightText
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor

@Composable
internal fun TripOverviewScreen(tripId: String, onBack: () -> Unit, onSettings: () -> Unit) {
    val darkTheme = LocalDarkTheme.current
    val language = LocalLanguage.current
    val cityCatalogContext = LocalContext.current
    val cityCatalogRepository = remember(cityCatalogContext) { CityCatalogRepository(cityCatalogContext.assets) }
    var overview by remember { mutableStateOf<TripOverview?>(null) }
    var weather by remember { mutableStateOf<Map<String, WeatherSnapshot>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf("overview") }
    var overviewEditMode by remember { mutableStateOf(false) }
    var sectionMenuOpen by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val weatherRepository = remember { WeatherRepository() }

    DisposableEffect(weatherRepository) {
        onDispose { weatherRepository.close() }
    }

    LaunchedEffect(tripId, refresh) {
        val hadOverview = overview != null
        if (!hadOverview) loading = true
        loadError = null
        val loadedOverview = runCatching {
            SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).loadTripOverview(tripId)
        }.onFailure { loadError = it.message }.getOrNull()
        if (loadedOverview == null) {
            if (!hadOverview) overview = null
            loading = false
            return@LaunchedEffect
        }

        overview = loadedOverview
        loading = false

        loadedOverview.let { trip ->
            val routeCities = trip.overviewMapPoints.ifEmpty {
                trip.routeLegs.flatMap { listOf(it.from, it.to) }.distinct()
            }.distinct()
            val cities = trip.overviewWeatherCities.ifEmpty { routeCities }.distinct()
            val unresolvedCities = cities.filter { city ->
                trip.cityCoordinates[city] == null && cityCatalogEntry(city) == null
            }
            val catalogCoordinates = runCatching {
                cityCatalogRepository.findExact(unresolvedCities).mapValues { (_, entry) ->
                    CityLocation(entry.latitude, entry.longitude)
                }
            }.getOrDefault(emptyMap())
            weather = runCatching {
                weatherRepository.loadCurrent(cities, trip.dates, catalogCoordinates + trip.cityCoordinates)
            }.getOrDefault(emptyMap())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (darkTheme) OdysseyDarkBackground else OdysseyBackground)
                .padding(WindowInsets.statusBars.asPaddingValues()),
        ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val pixelPerfectTab = tab == "restaurants" || tab == "accommodation" || tab == "budget"
            val pageScale = if (pixelPerfectTab) (maxWidth.value / 368f) else 1f
            val pageWidth = if (pixelPerfectTab) 368.dp else maxWidth
            val pageHeight = if (pixelPerfectTab) maxHeight / pageScale else maxHeight
            Column(
                modifier = Modifier
                    .width(pageWidth)
                    .height(pageHeight)
                    .offset(y = if (pixelPerfectTab) (-2).dp else 0.dp)
                    .graphicsLayer {
                        scaleX = pageScale
                        scaleY = pageScale
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
            ) {
        val menuContentDescription = localized("Открыть меню", "Open menu", "Abrir menú", "Menü öffnen")
        val overviewEditContentDescription = if (overviewEditMode) {
            localized("Завершить редактирование главного экрана", "Finish editing the overview", "Finalizar la edición del inicio", "Bearbeitung der Übersicht beenden")
        } else {
            localized("Редактировать главный экран", "Edit overview", "Editar inicio", "Übersicht bearbeiten")
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .offset(x = (-6).dp)
                    .semantics {
                        contentDescription = menuContentDescription
                        role = Role.Button
                    }
                    .clickable { sectionMenuOpen = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Menu, contentDescription = null, tint = contentTextColor(), modifier = Modifier.size(24.dp))
                }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = localizedTripTitle(overview?.title.orEmpty()),
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    style = OdysseyTightText,
                    maxLines = 1,
                    softWrap = false,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (tab) {
                            "overview" -> localized("Главная", "Overview", "Inicio", "Übersicht")
                            "route" -> localized("Маршрут", "Route", "Ruta", "Route")
                            "sights" -> localized("Достопримечательности", "Sights", "Lugares", "Sehenswürdigkeiten")
                            "restaurants" -> localized("Рестораны", "Restaurants", "Restaurantes", "Restaurants")
                            "accommodation" -> localized("Жильё", "Lodging", "Alojamiento", "Unterkunft")
                            "budget" -> localized("Бюджет", "Budget", "Presupuesto", "Budget")
                            "members" -> localized("Участники", "Members", "Participantes", "Teilnehmer")
                            else -> localized("Фото", "Photos", "Fotos", "Fotos")
                        },
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        style = OdysseyTightText,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Spacer(Modifier.width(5.dp))
                }
            }
            if (tab == "overview" && overview?.canEdit == true) {
                val editButtonShape = RoundedCornerShape(12.dp)
                val editButtonBackground = if (overviewEditMode) {
                    primaryColor()
                } else if (darkTheme) {
                    Color(0xFF2A2D3B)
                } else {
                    OdysseySurface2
                }
                val editButtonBorder = if (overviewEditMode) {
                    primaryColor()
                } else if (darkTheme) {
                    Color(0xFF3A3D4C)
                } else {
                    OdysseyBorder
                }
                val editButtonIcon = if (overviewEditMode) primaryContentColor() else Color(0xFF8E7BF5)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = overviewEditContentDescription
                            role = Role.Button
                        }
                        .clickable { overviewEditMode = !overviewEditMode },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(editButtonShape)
                            .background(editButtonBackground)
                            .border(1.dp, editButtonBorder, editButtonShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (overviewEditMode) Icons.Filled.Check else Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = editButtonIcon,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
        }

        if (loading) {
            TripOverviewLoading()
        } else if (overview == null) {
            Text(
                text = loadError ?: localized("Путешествие не найдено", "Trip not found", "Viaje no encontrado", "Reise nicht gefunden"),
                color = OdysseyError,
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                modifier = Modifier.padding(18.dp),
            )
        } else {
            when (tab) {
                "overview" -> OverviewContent(
                    tripId = tripId,
                    overview = overview!!,
                    weather = weather,
                    editMode = overviewEditMode,
                    onChanged = { refresh++ },
                )
                "route" -> TripRouteContent(tripId, overview!!, canEdit = overview!!.canEdit) { refresh++ }
                "sights" -> SightsContent(tripId, overview!!, canEdit = overview!!.canEdit) { refresh++ }
                "restaurants" -> RestaurantsContent(tripId, overview!!, canEdit = overview!!.canEdit) { refresh++ }
                "accommodation" -> AccommodationContent(tripId, overview!!, canEdit = overview!!.canEdit) { refresh++ }
                "budget" -> BudgetContent(
                    tripId = tripId,
                    overview = overview!!,
                    canEdit = overview!!.canEdit,
                    onExpenseAdded = { refresh++ },
                    onCurrencyChanged = { selectedCurrency -> overview = overview?.copy(budgetCurrency = selectedCurrency) },
                )
                "members" -> MembersContent(tripId, overview!!, canEdit = overview!!.currentUserRole == "Владелец") { refresh++ }
                else -> PhotosContent(tripId, overview!!, canEdit = overview!!.canEdit) { refresh++ }
            }
        }
            }
        }
        }

        val closeSectionMenuDescription = localized("Закрыть меню разделов", "Close sections menu", "Cerrar menú de secciones", "Bereichsmenü schließen")
        if (sectionMenuOpen) {
            val sectionMenuShape = RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp)
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x66000000)).semantics { contentDescription = closeSectionMenuDescription; role = Role.Button }.clickable { sectionMenuOpen = false })
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(310.dp)
                        .shadow(16.dp, sectionMenuShape, clip = false, ambientColor = Color(0x24000000), spotColor = Color(0x24000000))
                        .clip(sectionMenuShape)
                        .background(cardSurfaceColor())
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = 18.dp, top = 22.dp, end = 18.dp, bottom = 32.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp).background(primaryColor(), RoundedCornerShape(13.dp))) {
                            Icon(Icons.Outlined.Explore, contentDescription = null, tint = primaryContentColor(), modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp, top = 7.dp)) {
                            Text(localizedTripTitle(overview?.title.orEmpty()), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.fillMaxWidth().height(1.dp).background(contentBorderColor()))
                    Spacer(Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(
                        Triple("overview", Icons.Outlined.Explore, localized("Главная", "Overview", "Inicio", "Übersicht")),
                        Triple("route", Icons.Outlined.Share, localized("Маршрут", "Route", "Ruta", "Route")),
                        Triple("sights", Icons.Outlined.LocationOn, localized("Достопримечательности", "Sights", "Lugares", "Sehenswürdigkeiten")),
                        Triple("restaurants", Icons.Outlined.Restaurant, localized("Рестораны", "Restaurants", "Restaurantes", "Restaurants")),
                        Triple("accommodation", Icons.Outlined.Hotel, localized("Жильё", "Lodging", "Alojamiento", "Unterkunft")),
                        Triple("budget", Icons.Outlined.AccountBalanceWallet, localized("Бюджет", "Budget", "Presupuesto", "Budget")),
                        Triple("members", Icons.Outlined.Group, localized("Участники", "Members", "Participantes", "Teilnehmer")),
                        Triple("photos", Icons.Outlined.Image, localized("Фото", "Photos", "Fotos", "Fotos")),
                        ).forEach { (entry, icon, label) ->
                        val selected = tab == entry
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(if (selected) tintedSurfaceColor() else Color.Transparent, RoundedCornerShape(14.dp)).clickable { tab = entry; overviewEditMode = false; sectionMenuOpen = false }.padding(horizontal = 14.dp, vertical = 13.dp)) {
                            Icon(icon, contentDescription = null, tint = if (selected) primaryColor() else secondaryTextColor(), modifier = Modifier.size(20.dp))
                            Text(label, color = if (selected) primaryColor() else contentTextColor(), fontFamily = Manrope, fontWeight = if (selected) FontWeight.W800 else FontWeight.W700, fontSize = 14.5.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 14.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(tintedSurfaceColor())
                            .clickable { sectionMenuOpen = false; onBack() }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                    ) {
                        Text("↩", color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 22.sp, lineHeight = 22.sp, modifier = Modifier.width(20.dp))
                        Text(localized("Мои путешествия", "My trips", "Mis viajes", "Meine Reisen"), color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 15.5.sp, modifier = Modifier.padding(start = 14.dp))
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { sectionMenuOpen = false; onSettings() }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = null, tint = secondaryTextColor(), modifier = Modifier.size(20.dp))
                        Text(localized("Настройки", "Settings", "Ajustes", "Einstellungen"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 15.5.sp, modifier = Modifier.padding(start = 14.dp))
                    }
                    }
                }
            }
        }
    }
}

