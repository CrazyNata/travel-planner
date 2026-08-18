package com.odyssey.travelplanner.ui.screen.trip.sights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.Sight
import com.odyssey.travelplanner.data.SightCatalogEntry
import com.odyssey.travelplanner.data.catalogCityName
import com.odyssey.travelplanner.data.normalizeCatalogText
import kotlinx.coroutines.launch
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityName
import com.odyssey.travelplanner.ui.i18n.localizedSightInfo
import com.odyssey.travelplanner.ui.i18n.localizedSightName
import com.odyssey.travelplanner.ui.screen.trip.route.RouteEditorField
import com.odyssey.travelplanner.ui.screen.trip.route.RouteOrderButton
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyDarkSubtext
import com.odyssey.travelplanner.ui.theme.OdysseyDarkSurface2
import com.odyssey.travelplanner.ui.theme.OdysseyDarkText
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.OdysseySubtext
import com.odyssey.travelplanner.ui.theme.OdysseySurface2
import com.odyssey.travelplanner.ui.theme.OdysseyText
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
internal fun CreateDaySheet(tripId: String, city: String, day: Int, onClose: () -> Unit, onSaved: () -> Unit) {
    val language = LocalLanguage.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var dayNumber by remember { mutableStateOf(day.toString()) }
    var placeName by remember { mutableStateOf("") }
    var placeNames by remember { mutableStateOf(emptyList<String>()) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) { Text(localized("СОЗДАТЬ ДЕНЬ", "CREATE DAY", "CREAR DÍA", "TAG ERSTELLEN"), color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp); Text(localized("Места и маршрут дня", "Places and day route", "Lugares y ruta del día", "Orte und Tagesroute"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 22.sp) }
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(secondarySurfaceColor()).clickable { onClose() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = secondaryTextColor(), modifier = Modifier.size(18.dp)) }
        }
        RouteEditorField(localized("День", "Day", "Día", "Tag"), dayNumber, { dayNumber = it }, Modifier.fillMaxWidth())
        Text(localized("ДОСТОПРИМЕЧАТЕЛЬНОСТИ · ${placeNames.size}", "SIGHTS · ${placeNames.size}", "LUGARES · ${placeNames.size}", "ORTE · ${placeNames.size}"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = placeName,
                onValueChange = { placeName = it },
                placeholder = { Text(localized("Напр. Хофбройхаус", "E.g. Hofbräuhaus", "P. ej. Hofbräuhaus", "Z. B. Hofbräuhaus"), color = secondaryTextColor(), fontFamily = Manrope, fontSize = 13.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.sp, lineHeight = 20.sp, color = contentTextColor(), platformStyle = OdysseyNoFontPadding),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(54.dp),
            )
            Button(onClick = { if (placeName.isNotBlank()) { placeNames = placeNames + placeName.trim(); placeName = "" } }, modifier = Modifier.height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()), shape = RoundedCornerShape(12.dp)) { Text(localized("＋ Добавить", "＋ Add", "＋ Añadir", "＋ Hinzufügen"), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp) }
        }
        placeNames.forEachIndexed { index, pendingName ->
            Row(modifier = Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(13.dp)).background(tintedSurfaceColor()).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text((index + 1).toString(), color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 16.sp, modifier = Modifier.size(34.dp).clip(CircleShape).border(2.dp, Color(0xFFCFC6FF), CircleShape).padding(start = 11.dp, top = 5.dp))
                Text(pendingName, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, lineHeight = 18.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.weight(1f).padding(start = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RouteOrderButton(Icons.Outlined.KeyboardArrowUp, index > 0, localized("Переместить вверх", "Move up", "Mover arriba", "Nach oben")) {
                        val reordered = placeNames.toMutableList()
                        val moved = reordered.removeAt(index)
                        reordered.add(index - 1, moved)
                        placeNames = reordered
                    }
                    RouteOrderButton(Icons.Outlined.KeyboardArrowDown, index < placeNames.lastIndex, localized("Переместить вниз", "Move down", "Mover abajo", "Nach unten")) {
                        val reordered = placeNames.toMutableList()
                        val moved = reordered.removeAt(index)
                        reordered.add(index + 1, moved)
                        placeNames = reordered
                    }
                    Text("×", color = Color(0xFFFF6B65), fontSize = 22.sp, modifier = Modifier.padding(start = 4.dp).clickable { placeNames = placeNames.filterIndexed { itemIndex, _ -> itemIndex != index } })
                }
            }
        }
        if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        Button(onClick = {
            scope.launch {
                saving = true
                runCatching {
                    val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                    val namesToAdd = daySightNamesToSave(placeNames, placeName)
                    require(namesToAdd.isNotEmpty()) {
                        localized(language, "Добавьте хотя бы одну достопримечательность", "Add at least one sight", "Añada al menos un lugar", "Fügen Sie mindestens einen Ort hinzu")
                    }
                    val targetDay = dayNumber.toIntOrNull()?.takeIf { it > 0 } ?: day
                    namesToAdd.forEach { sightName ->
                        repository.addSightDetails(tripId, sightName, city, "достопримечательности", "", targetDay, link = "")
                    }
                }.onSuccess { onSaved(); onClose() }.onFailure {
                    message = it.message ?: localized(language, "Не удалось сохранить день", "Could not save day", "No se pudo guardar el día", "Tag konnte nicht gespeichert werden")
                }
                saving = false
            }
        }, enabled = !saving && city.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp).padding(bottom = 5.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()), shape = RoundedCornerShape(14.dp)) { Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить день", "Save day", "Guardar día", "Tag speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun EditDaySheet(
    tripId: String,
    day: Int,
    city: String,
    sights: List<Sight>,
    allSights: List<Sight>,
    darkTheme: Boolean,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
) {
    var addingSight by remember { mutableStateOf(false) }
    var catalogOpen by remember { mutableStateOf(false) }
    var deletingSightId by remember { mutableStateOf<String?>(null) }
    var deleteDayDialogOpen by remember { mutableStateOf(false) }
    var deletingDay by remember { mutableStateOf(false) }
    var deleteDayError by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val language = LocalLanguage.current
    val scope = rememberCoroutineScope()
    val sheetContentTextColor = if (darkTheme) OdysseyDarkText else OdysseyText
    val sheetSecondaryTextColor = if (darkTheme) OdysseyDarkSubtext else OdysseySubtext
    val sheetSecondarySurfaceColor = if (darkTheme) OdysseyDarkSurface2 else OdysseySurface2
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized("Редактировать день", "Edit day", "Editar día", "Tag bearbeiten"), color = sheetContentTextColor, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 22.5.sp)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(sheetSecondarySurfaceColor).clickable { onClose() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = sheetSecondaryTextColor, modifier = Modifier.size(18.dp)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RouteEditorField(localized("День №", "Day no.", "Día nº", "Tag Nr."), day.toString(), {}, Modifier.width(74.dp))
            RouteEditorField(localized("Город", "City", "Ciudad", "Stadt"), localizedCityName(city), {}, Modifier.weight(1f))
        }
        Text(localized("ДОСТОПРИМЕЧАТЕЛЬНОСТИ", "SIGHTS", "LUGARES", "SEHENSWÜRDIGKEITEN"), color = sheetSecondaryTextColor, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.7.sp, modifier = Modifier.padding(top = 6.dp))
        if (message != null) {
            Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        }
        sights.forEach { sight ->
            Row(modifier = Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(14.dp)).background(sheetSecondarySurfaceColor).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                SightPhoto(sight, Modifier.size(52.dp).clip(RoundedCornerShape(11.dp)))
                Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) { Text(localizedSightName(sight.name), color = sheetContentTextColor, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(localizedSightInfo(sight.name, sight.description, sight.category), color = sheetSecondaryTextColor, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.8.sp, maxLines = 1) }
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(dangerSurfaceColor()).clickable(enabled = deletingSightId == null) {
                    deletingSightId = sight.id
                    message = null
                    scope.launch {
                        runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteTripItem(tripId, "sights", sight.id) }
                            .onSuccess { onSaved() }
                            .onFailure {
                                message = it.message ?: localized(language, "Не удалось удалить достопримечательность", "Could not delete sight", "No se pudo eliminar el lugar", "Ort konnte nicht gelöscht werden")
                            }
                        deletingSightId = null
                    }
                }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Delete, contentDescription = localized("Удалить", "Delete", "Eliminar", "Löschen"), tint = Color(0xFFFF6B65), modifier = Modifier.size(18.dp))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                localized("＋  Из каталога", "＋  From catalog", "＋  Del catálogo", "＋  Aus Katalog"),
                color = primaryColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, primaryColor().copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .background(primaryColor().copy(alpha = 0.08f))
                    .clickable { catalogOpen = true }
                    .padding(top = 15.dp),
            )
            Text(
                localized("＋  Вручную", "＋  Manually", "＋  Manualmente", "＋  Manuell"),
                color = primaryColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, contentBorderColor(), RoundedCornerShape(14.dp))
                    .background(tintedSurfaceColor())
                    .clickable { addingSight = true }
                    .padding(top = 15.dp),
            )
        }
        if (sights.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(dangerSurfaceColor())
                    .clickable(enabled = deletingSightId == null && !deletingDay) {
                        deleteDayError = null
                        deleteDayDialogOpen = true
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Delete, contentDescription = localized("Удалить день", "Delete day", "Eliminar día", "Tag löschen"), tint = Color(0xFFFF6B65), modifier = Modifier.size(18.dp))
                    Text(localized("Удалить день", "Delete day", "Eliminar día", "Tag löschen"), color = Color(0xFFFF6B65), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp)
                }
            }
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(54.dp).padding(bottom = 5.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()), shape = RoundedCornerShape(14.dp)) { Text(localized("Сохранить", "Save", "Guardar", "Speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
    }
    if (catalogOpen) {
        ModalBottomSheet(
            onDismissRequest = { catalogOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            dragHandle = null,
        ) {
            SightCatalogSheet(
                tripId = tripId,
                city = city,
                day = day,
                existingSights = allSights,
                onClose = { catalogOpen = false },
                onSaved = onSaved,
            )
        }
    }
    if (addingSight) {
        ModalBottomSheet(onDismissRequest = { addingSight = false }, containerColor = cardSurfaceColor()) {
            AddSightSheet(tripId, city, day, onClose = { addingSight = false }, onSaved = onSaved)
        }
    }
    if (deleteDayDialogOpen) {
        AlertDialog(
            onDismissRequest = { if (!deletingDay) deleteDayDialogOpen = false },
            title = {
                Text(
                    localized("Удалить день?", "Delete day?", "¿Eliminar día?", "Tag löschen?"),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        localized(
                            "Все достопримечательности этого дня будут удалены из поездки.",
                            "All sights from this day will be removed from the trip.",
                            "Todos los lugares de este día se eliminarán del viaje.",
                            "Alle Sehenswürdigkeiten dieses Tages werden aus der Reise entfernt.",
                        ),
                        fontFamily = Manrope,
                    )
                    deleteDayError?.let { error ->
                        Text(error, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDayDialogOpen = false }, enabled = !deletingDay) {
                    Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            deletingDay = true
                            deleteDayError = null
                            runCatching {
                                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteSightDay(tripId, day)
                            }.onSuccess {
                                deleteDayDialogOpen = false
                                onDeleted()
                            }.onFailure {
                                deleteDayError = it.message ?: localized(language, "Не удалось удалить день", "Could not delete day", "No se pudo eliminar el día", "Tag konnte nicht gelöscht werden")
                            }
                            deletingDay = false
                        }
                    },
                    enabled = !deletingDay,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD9534F)),
                ) {
                    Text(
                        if (deletingDay) localized("Удаляем…", "Deleting…", "Eliminando…", "Wird gelöscht…")
                        else localized("Удалить", "Delete", "Eliminar", "Löschen"),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                    )
                }
            },
        )
    }
}

internal fun catalogSightAlreadyAdded(
    entry: SightCatalogEntry,
    city: String,
    existingSights: List<Sight>,
): Boolean {
    val cityKey = normalizeCatalogText(catalogCityName(city))
    val entryNames = entry.allNames().map(::normalizeCatalogText).filter(String::isNotBlank).toSet()
    return existingSights.any { sight ->
        val sightCity = normalizeCatalogText(catalogCityName(sight.city))
        val sightName = normalizeCatalogText(sight.name)
        sightCity == cityKey && sightName in entryNames
    }
}

