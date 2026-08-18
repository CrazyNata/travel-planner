package com.odyssey.travelplanner.ui.screen.trip.photos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.TripOverview
import com.odyssey.travelplanner.ui.domain.formatPhotoDateRange
import com.odyssey.travelplanner.ui.domain.photoGroupDateRange
import com.odyssey.travelplanner.ui.domain.photoGroupDay
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyError
import com.odyssey.travelplanner.ui.theme.OdysseyPurpleGradientEnd
import com.odyssey.travelplanner.ui.theme.OdysseyWarning
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor

@Composable
internal fun PhotosContent(tripId: String, overview: TripOverview, canEdit: Boolean = true, onPhotoAdded: () -> Unit) {
    val language = LocalLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            uploading = true
            message = null
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Не удалось прочитать изображение")
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).addCoverPhoto(tripId, bytes)
            }.onSuccess { onPhotoAdded() }.onFailure {
                message = it.message ?: localized(language, "Не удалось загрузить фото", "Could not upload photo", "No se pudo subir la foto", "Foto konnte nicht hochgeladen werden")
            }
            uploading = false
        }
    }
    val photos = buildList {
        overview.coverPhotos.forEach { add(it.imageUrl to it.city) }
        overview.accommodations.forEach { accommodation ->
            accommodation.photos.forEach { add(it to accommodation.city) }
        }
        overview.sights.filter { it.photo.isNotBlank() }.forEach { sight -> add(sight.photo to sight.city) }
        overview.restaurants.forEach { restaurant -> restaurant.photos.forEach { add(it to restaurant.city) } }
    }.filter { it.first.isNotBlank() }.distinctBy { it.first }
    val groupedPhotos = photos.groupBy { (_, city) -> city.ifBlank { localized(language, "Поездка", "Trip", "Viaje", "Reise") } }.toList()

    fun groupMeta(city: String, count: Int): String {
        val date = photoGroupDateRange(city, overview, groupedPhotos.indexOfFirst { it.first == city } + 1)
            ?.let { formatPhotoDateRange(it, language) }
        return listOfNotNull(date, "$count ${localized(language, "фото", "photos", "fotos", "Fotos")}").joinToString(" · ")
    }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text(localized("Фото", "Photos", "Fotos", "Fotos"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
                if (canEdit) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(Brush.linearGradient(listOf(primaryColor(), OdysseyPurpleGradientEnd)))
                            .shadow(5.dp, RoundedCornerShape(11.dp), clip = false, ambientColor = Color(0x426C5CE7), spotColor = Color(0x426C5CE7))
                            .clickable(enabled = !uploading) { picker.launch("image/*") }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            if (uploading) localized("Загружаем…", "Uploading…", "Subiendo…", "Wird hochgeladen…") else localized("↑  Загрузить", "↑  Upload", "↑  Subir", "↑  Hochladen"),
                            color = primaryContentColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 12.5.sp,
                        )
                    }
                }
            }
        }
        if (message != null) item { Text(message!!, color = OdysseyError, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp) }
        if (groupedPhotos.isEmpty()) {
            item { Text(localized("Фотографии пока не добавлены", "No photos added yet", "Aún no se han añadido fotos", "Noch keine Fotos hinzugefügt"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp) }
        } else {
            itemsIndexed(groupedPhotos, key = { _, group -> group.first }) { index, (city, cityPhotos) ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(26.dp).background(Brush.linearGradient(listOf(OdysseyWarning, Color(0xFFF77F4B))), CircleShape)) {
                            Text(photoGroupDay(city, overview, index + 1).toString(), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
                        }
                    Text(localizedCityName(city), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 18.sp, modifier = Modifier.padding(start = 10.dp))
                        Spacer(Modifier.weight(1f))
                        Text(groupMeta(city, cityPhotos.size), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.5.sp)
                    }

                    if (cityPhotos.size >= 3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            PhotoTile(cityPhotos[0].first, Modifier.weight(1.7f).height(216.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                PhotoTile(cityPhotos[1].first, Modifier.fillMaxWidth().height(104.dp))
                                PhotoTile(cityPhotos[2].first, Modifier.fillMaxWidth().height(104.dp))
                            }
                        }
                        cityPhotos.drop(3).chunked(3).forEach { row ->
                            PhotoTileRow(row)
                        }
                    } else {
                        cityPhotos.chunked(3).forEach { row ->
                            PhotoTileRow(row)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PhotoTileRow(photos: List<Pair<String, String>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        photos.forEach { (imageUrl, _) ->
            PhotoTile(imageUrl, Modifier.weight(1f).height(112.dp))
        }
        repeat(3 - photos.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
internal fun PhotoTile(imageUrl: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFD9D6E1))) {
        AsyncImage(model = imageUrl, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

