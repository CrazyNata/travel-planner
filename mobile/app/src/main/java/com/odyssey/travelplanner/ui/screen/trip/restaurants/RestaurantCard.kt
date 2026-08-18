package com.odyssey.travelplanner.ui.screen.trip.restaurants

import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Edit
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.extension.localization.localizeLabels
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import java.net.URL
import com.odyssey.travelplanner.data.CityLocation
import com.odyssey.travelplanner.ui.common.FullScreenPhotoViewer
import com.odyssey.travelplanner.ui.domain.mapCoordinate
import com.odyssey.travelplanner.ui.domain.restaurantLinkUri
import com.odyssey.travelplanner.ui.i18n.labelMapboxAccessibility
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.i18n.localizedRestaurantNote
import com.odyssey.travelplanner.ui.i18n.mapLocale
import com.odyssey.travelplanner.ui.icons.OdysseyCalendarIcon
import com.odyssey.travelplanner.ui.icons.OdysseyEditIcon
import com.odyssey.travelplanner.ui.icons.OdysseyExternalLinkIcon
import com.odyssey.travelplanner.ui.screen.trip.sights.keepMapGesturesInsideMap
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.labelColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.secondarySurfaceColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor
import com.odyssey.travelplanner.ui.theme.warningSurfaceColor

@Composable
internal fun RestaurantCard(
    restaurant: com.odyssey.travelplanner.data.Restaurant,
    saving: Boolean,
    uploading: Boolean,
    canEdit: Boolean = true,
    onEdit: () -> Unit,
    onAddPhoto: () -> Unit,
    modifier: Modifier = Modifier,
    onStatusChange: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val cardBorderColor = contentBorderColor()
    // Failed signed-URL resolutions are represented by blank placeholders so
    // photo indexes stay stable in the editor. They must not make a viewer
    // appear to have a usable photo.
    val photos = restaurant.photos.filter(String::isNotBlank)
    var photoIndex by remember(restaurant.id, photos) { mutableStateOf(0) }
    var fullScreenPhotoIndex by remember(restaurant.id, photos) { mutableStateOf<Int?>(null) }
    val activePhotoIndex = photoIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
    val displayedNote = localizedRestaurantNote(restaurant.note)
    val booked = restaurant.status == "бронь"
    val visited = restaurant.status == "были"
    val reviewsLabel = restaurant.reviews.trim().let { raw ->
        when {
            raw.isBlank() -> ""
            raw.contains("отзыв", ignoreCase = true) || raw.contains("review", ignoreCase = true) || raw.contains("reseña", ignoreCase = true) || raw.contains("Bewertung", ignoreCase = true) -> raw
            else -> localized("$raw отзывов", "$raw reviews", "$raw reseñas", "$raw Bewertungen")
        }
    }
    val reservation = when (restaurant.status) {
        "бронь" -> localized("Бронь подтверждена", "Reservation confirmed", "Reserva confirmada", "Reservierung bestätigt")
        "были" -> localized("Посещено", "Visited", "Visitado", "Besucht")
        else -> localized("Запланировать бронь", "Plan reservation", "Planificar reserva", "Reservierung planen")
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardSurfaceColor())
            .shadow(6.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = Color(0x0F141428), spotColor = Color(0x0F141428))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(78.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE4E1EA))
                    .clickable(enabled = !uploading && (canEdit || photos.isNotEmpty())) {
                        if (photos.isEmpty() && canEdit) onAddPhoto() else if (photos.isNotEmpty()) fullScreenPhotoIndex = activePhotoIndex
                    },
            ) {
                photos.getOrNull(activePhotoIndex)?.let { AsyncImage(model = it, contentDescription = restaurant.name, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                if (photos.isEmpty()) Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = Color(0xFFA7A1B2), modifier = Modifier.align(Alignment.Center).size(27.dp))
                // Restaurant thumbnails stay clean; photo controls are available in the fullscreen viewer.
                if (photos.size > 1 && fullScreenPhotoIndex != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x990F0F19))
                            .clickable {
                                photoIndex = (activePhotoIndex - 1 + photos.size) % photos.size
                            },
                    ) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = localized("Предыдущее фото", "Previous photo", "Foto anterior", "Vorheriges Foto"),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x990F0F19))
                            .clickable {
                                photoIndex = (activePhotoIndex + 1) % photos.size
                            },
                    ) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = localized("Следующее фото", "Next photo", "Foto siguiente", "Nächstes Foto"),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp).graphicsLayer(rotationZ = 180f),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x990F0F19))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${activePhotoIndex + 1}/${photos.size}",
                            color = Color.White,
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f).height(78.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                        restaurant.name,
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 15.sp,
                        lineHeight = 17.25.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    restaurantLinkUri(restaurant.link)?.let { link ->
                        OdysseyExternalLinkIcon(
                            17.dp,
                            primaryColor(),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable { runCatching { uriHandler.openUri(link) } },
                        )
                    }
                }
                Text(
                    restaurant.city.takeIf(String::isNotBlank)?.let { localizedCityName(it) }
                        ?: localized("Город не указан", "City not specified", "Ciudad no indicada", "Stadt nicht angegeben"),
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 9.dp).height(25.dp)) {
                    restaurant.rating?.let {
                        Row(
                            modifier = Modifier.height(25.dp).clip(RoundedCornerShape(8.dp)).background(warningSurfaceColor()).padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("★", color = Color(0xFFF5A623), fontFamily = Manrope, fontWeight = FontWeight.W400, fontSize = 11.sp, lineHeight = 15.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                            Text(it.toString(), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                        }
                    }
                    if (restaurant.price.isNotBlank()) {
                        Box(modifier = Modifier.height(25.dp).clip(RoundedCornerShape(8.dp)).background(tintedSurfaceColor()).padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                            Text(restaurant.price, color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                        }
                    }
                    if (displayedNote.isNotBlank() && !restaurant.note.contains("http", ignoreCase = true)) {
                        Box(modifier = Modifier.height(25.dp).clip(RoundedCornerShape(8.dp)).background(secondarySurfaceColor()).padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                            Text(displayedNote, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                        }
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(29.dp)
                .drawBehind { drawLine(cardBorderColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
                .padding(top = 11.dp)
                .clickable(enabled = canEdit && !saving) { onStatusChange(when (restaurant.status) { "хочу" -> "бронь"; "бронь" -> "были"; else -> "хочу" }) },
        ) {
            if (booked || visited) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OdysseyCalendarIcon(14.dp, if (booked) Color(0xFF22B07D) else secondaryTextColor())
                    Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else reservation, color = if (booked) Color(0xFF22B07D) else secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.5.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(
                    if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else reviewsLabel.ifBlank { reservation },
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (restaurant.reviews.isNotBlank()) {
                    Text(localized("Забронировать", "Book", "Reservar", "Buchen"), color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.5.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1)
                }
            }
        }
        if (canEdit) {
            OutlinedButton(
                onClick = onEdit,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = labelColor()),
                border = androidx.compose.foundation.BorderStroke(1.dp, contentBorderColor()),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 11.dp).height(42.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(11.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OdysseyEditIcon(15.dp, primaryColor())
                    Text(localized("Редактировать", "Edit", "Editar", "Bearbeiten"), color = labelColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.5.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1)
                }
            }
        }
        fullScreenPhotoIndex?.let { initialIndex ->
            if (photos.isNotEmpty()) {
                FullScreenPhotoViewer(
                    photos = photos,
                    initialIndex = initialIndex,
                    accommodationName = restaurant.name,
                    onDismiss = { selectedIndex ->
                        photoIndex = selectedIndex
                        fullScreenPhotoIndex = null
                    },
                )
            }
        }
    }
}

@Composable
internal fun RestaurantMapCard(
    restaurants: List<com.odyssey.travelplanner.data.Restaurant>,
    cityCoordinates: Map<String, CityLocation> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val language = LocalLanguage.current
    val attributionDescription = localized("Информация об источниках карты", "Map attribution information", "Información de atribución del mapa", "Informationen zur Kartenquelle")
    val restaurantPoints = remember(restaurants) {
        restaurants
            .mapNotNull { mapCoordinate(it.city, cityCoordinates) }
            .distinctBy { "${it.longitude()},${it.latitude()}" }
    }
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
    val annotationManager = remember(mapView) { mapView.annotations.createCircleAnnotationManager() }
    val numberAnnotationManager = remember(mapView) { mapView.annotations.createPointAnnotationManager() }

    LaunchedEffect(mapView) {
        mapStyleReady = false
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            style.localizeLabels(mapLocale(language))
            mapStyleReady = true
        }
    }

    DisposableEffect(mapView, annotationManager, numberAnnotationManager) {
        onDispose {
            annotationManager.deleteAll()
            numberAnnotationManager.deleteAll()
        }
    }

    LaunchedEffect(mapStyleReady, language) {
        if (mapStyleReady) mapView.mapboxMap.style?.localizeLabels(mapLocale(language))
    }

    LaunchedEffect(mapStyleReady, restaurantPoints) {
        if (!mapStyleReady) return@LaunchedEffect
        annotationManager.deleteAll()
        numberAnnotationManager.deleteAll()
        restaurantPoints.forEachIndexed { index, point ->
            annotationManager.create(
                CircleAnnotationOptions()
                    .withPoint(point)
                    .withCircleRadius(9.0)
                    .withCircleColor("#6C5CE7")
                    .withCircleStrokeColor("#FFFFFF")
                    .withCircleStrokeWidth(3.0),
            )
            numberAnnotationManager.create(
                PointAnnotationOptions()
                    .withPoint(point)
                    .withTextField((index + 1).toString())
                    .withTextColor("#FFFFFF")
                    .withTextSize(12.0)
                    .withTextAnchor(TextAnchor.CENTER),
            )
        }
        val camera = when {
            restaurantPoints.size > 1 -> mapView.mapboxMap.cameraForCoordinates(
                restaurantPoints,
                EdgeInsets(34.0, 34.0, 34.0, 34.0),
                null,
                null,
            )
            restaurantPoints.size == 1 -> CameraOptions.Builder()
                .center(restaurantPoints.first())
                .zoom(9.0)
                .build()
            else -> CameraOptions.Builder()
                .center(Point.fromLngLat(12.4964, 41.9028))
                .zoom(5.0)
                .build()
        }
        mapView.mapboxMap.setCamera(camera)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .shadow(10.dp, RoundedCornerShape(22.dp), clip = false, ambientColor = Color(0x19141428), spotColor = Color(0x19141428))
            .clip(RoundedCornerShape(22.dp))
            .background(cardSurfaceColor()),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

