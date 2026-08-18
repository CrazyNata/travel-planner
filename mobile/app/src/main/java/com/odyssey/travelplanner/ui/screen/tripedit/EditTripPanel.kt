package com.odyssey.travelplanner.ui.screen.tripedit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import java.util.Locale
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDate
import java.time.YearMonth
import com.odyssey.travelplanner.R
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.TripCard
import com.odyssey.travelplanner.ui.domain.tripCalendarMonthLabel
import com.odyssey.travelplanner.ui.domain.tripCalendarWeekdays
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityList
import com.odyssey.travelplanner.ui.i18n.localizedTripDateText
import com.odyssey.travelplanner.ui.i18n.localizedTripTitle
import com.odyssey.travelplanner.ui.theme.LocalDarkTheme
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBorder
import com.odyssey.travelplanner.ui.theme.OdysseyDarkText
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondarySurfaceColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor

@Composable
internal fun CompactTripEditField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onValueChange: (String) -> Unit,
) {
    val darkTheme = LocalDarkTheme.current
    val surface = cardSurfaceColor()
    val border = if (darkTheme) OdysseyDarkBorder else contentBorderColor()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            color = if (darkTheme) OdysseyDarkText else Color(0xFF3A3A42),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 12.sp,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            leadingIcon = icon?.let { iconValue ->
                {
                    Icon(iconValue, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(18.dp))
                }
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                fontSize = 14.sp,
            ),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor(),
                unfocusedBorderColor = border,
                focusedContainerColor = surface,
                unfocusedContainerColor = surface,
                cursorColor = primaryColor(),
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        )
    }
}

@Composable
internal fun CompactTripDateField(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    val darkTheme = LocalDarkTheme.current
    val surface = cardSurfaceColor()
    val border = if (darkTheme) OdysseyDarkBorder else contentBorderColor()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            color = if (darkTheme) OdysseyDarkText else Color(0xFF3A3A42),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 12.sp,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.5.dp, border, RoundedCornerShape(14.dp))
                .background(surface)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
        ) {
            Icon(
                Icons.Outlined.DateRange,
                contentDescription = localized("Открыть календарь", "Open calendar", "Abrir calendario", "Kalender öffnen"),
                tint = primaryColor(),
                modifier = Modifier.size(19.dp),
            )
            Text(
                value,
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

internal fun parseTripDateRange(value: String): Pair<LocalDate, LocalDate>? {
    val dottedDates = Regex("""\d{1,2}\.\d{1,2}\.\d{4}""").findAll(value)
        .mapNotNull { match ->
            val parts = match.value.split('.')
            runCatching { LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt()) }.getOrNull()
        }
        .toList()
    if (dottedDates.isNotEmpty()) {
        val start = dottedDates.first()
        return start to dottedDates.getOrElse(1) { start }
    }

    val isoDates = Regex("""\d{4}-\d{2}-\d{2}""").findAll(value)
        .mapNotNull { match -> runCatching { LocalDate.parse(match.value) }.getOrNull() }
        .toList()
    if (isoDates.isNotEmpty()) {
        val start = isoDates.first()
        return start to isoDates.getOrElse(1) { start }
    }

    val humanDates = Regex("""(\d{1,2})\s+([A-Za-zА-Яа-яЁёÄÖÜäöüß]+)\s+(\d{4})""").findAll(value)
        .mapNotNull { match ->
            val month = when (match.groupValues[2].lowercase(Locale.ROOT).take(4)) {
                "янва", "janu", "jan", "ene" -> 1
                "февр", "febr", "feb" -> 2
                "мар", "mär", "mar", "march" -> 3
                "апре", "apr" -> 4
                "мая", "май", "may", "mai" -> 5
                "июн", "june", "jun" -> 6
                "июл", "july", "jul" -> 7
                "авгу", "aug", "ago" -> 8
                "сент", "сен", "sept", "sep" -> 9
                "октя", "окт", "oct", "okt" -> 10
                "нояб", "nov" -> 11
                "дека", "дек", "dec", "dez" -> 12
                else -> null
            } ?: return@mapNotNull null
            runCatching { LocalDate.of(match.groupValues[3].toInt(), month, match.groupValues[1].toInt()) }.getOrNull()
        }
        .toList()
    val start = humanDates.firstOrNull() ?: return null
    return start to humanDates.getOrElse(1) { start }
}

@Composable
internal fun TripCalendarRangeChip(
    label: String,
    date: LocalDate?,
    selected: Boolean,
    language: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) tintedSurfaceColor() else secondarySurfaceColor())
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(label, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp)
        Text(
            date?.let { localizedTripDateText(it.toString(), language) }
                ?: localized("Выбрать", "Choose", "Elegir", "Auswählen"),
            color = if (date == null) secondaryTextColor() else contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
internal fun TripDateCalendarDialog(
    language: String,
    month: YearMonth,
    startDate: LocalDate?,
    endDate: LocalDate?,
    selectingEnd: Boolean,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val calendarDays = buildList<LocalDate?> {
        repeat(month.atDay(1).dayOfWeek.value - 1) { add(null) }
        for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
    }
    val weekdays = tripCalendarWeekdays(language)
    val canConfirm = startDate != null && endDate != null && !endDate.isBefore(startDate)

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val dialogWindow = (LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        DisposableEffect(dialogWindow) {
            if (dialogWindow == null) {
                return@DisposableEffect onDispose { }
            }
            val previousStatusBarColor = dialogWindow.statusBarColor
            val previousNavigationBarColor = dialogWindow.navigationBarColor
            val previousDimAmount = dialogWindow.attributes.dimAmount
            val insetsController = WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView)
            val previousLightStatusBars = insetsController.isAppearanceLightStatusBars
            val previousLightNavigationBars = insetsController.isAppearanceLightNavigationBars

            dialogWindow.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialogWindow.setDimAmount(1f)
            dialogWindow.statusBarColor = android.graphics.Color.BLACK
            dialogWindow.navigationBarColor = android.graphics.Color.BLACK
            WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false

            onDispose {
                dialogWindow.setDimAmount(previousDimAmount)
                dialogWindow.statusBarColor = previousStatusBarColor
                dialogWindow.navigationBarColor = previousNavigationBarColor
                WindowCompat.setDecorFitsSystemWindows(dialogWindow, true)
                insetsController.isAppearanceLightStatusBars = previousLightStatusBars
                insetsController.isAppearanceLightNavigationBars = previousLightNavigationBars
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x660F0F19)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 22.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardSurfaceColor())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            localized("ДАТЫ ПОЕЗДКИ", "TRIP DATES", "FECHAS DEL VIAJE", "REISEDATEN"),
                            color = secondaryTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                        )
                        Text(
                            localized("Выбрать даты", "Choose dates", "Elegir fechas", "Daten auswählen"),
                            color = contentTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 21.sp,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(secondarySurfaceColor())
                            .clickable(onClick = onDismiss),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = secondaryTextColor(), modifier = Modifier.size(17.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TripCalendarRangeChip(
                        label = localized("Начало", "Start", "Inicio", "Beginn"),
                        date = startDate,
                        selected = !selectingEnd,
                        language = language,
                        modifier = Modifier.weight(1f),
                        onClick = onStartClick,
                    )
                    TripCalendarRangeChip(
                        label = localized("Окончание", "End", "Fin", "Ende"),
                        date = endDate,
                        selected = selectingEnd,
                        language = language,
                        modifier = Modifier.weight(1f),
                        onClick = onEndClick,
                    )
                }
                Text(
                    if (selectingEnd) localized("Теперь выберите дату окончания", "Now choose the end date", "Ahora elija la fecha de fin", "Wählen Sie jetzt das Ende")
                    else localized("Выберите дату начала поездки", "Choose the start date", "Elija la fecha de inicio", "Wählen Sie den Beginn"),
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = 12.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(secondarySurfaceColor())
                            .clickable { onMonthChange(month.minusMonths(1)) },
                    ) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = localized("Предыдущий месяц", "Previous month", "Mes anterior", "Vorheriger Monat"), tint = contentTextColor(), modifier = Modifier.size(20.dp))
                    }
                    Text(tripCalendarMonthLabel(month, language), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 15.sp)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(secondarySurfaceColor())
                            .clickable { onMonthChange(month.plusMonths(1)) },
                    ) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = localized("Следующий месяц", "Next month", "Mes siguiente", "Nächster Monat"), tint = contentTextColor(), modifier = Modifier.size(20.dp).graphicsLayer(rotationZ = 180f))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekdays.forEach { weekday ->
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).height(24.dp)) {
                            Text(weekday, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.sp)
                        }
                    }
                }
                calendarDays.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            val isStart = date != null && date == startDate
                            val isEnd = date != null && date == endDate
                            val inRange = date != null && startDate != null && endDate != null && date.isAfter(startDate) && date.isBefore(endDate)
                            val enabled = date != null && (!selectingEnd || startDate == null || !date.isBefore(startDate))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isStart || isEnd -> primaryColor()
                                            inRange -> tintedSurfaceColor()
                                            else -> Color.Transparent
                                        },
                                    )
                                    .clickable(enabled = enabled) { date?.let(onDateSelected) },
                            ) {
                                date?.let {
                                    Text(
                                        it.dayOfMonth.toString(),
                                        color = when {
                                            isStart || isEnd -> primaryContentColor()
                                            !enabled -> secondaryTextColor().copy(alpha = 0.35f)
                                            else -> contentTextColor()
                                        },
                                        fontFamily = Manrope,
                                        fontWeight = if (isStart || isEnd) FontWeight.W800 else FontWeight.W700,
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                        repeat(7 - week.size) {
                            Spacer(Modifier.weight(1f).height(42.dp))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = secondarySurfaceColor(), contentColor = contentTextColor()),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(46.dp),
                    ) { Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp) }
                    Button(
                        onClick = onConfirm,
                        enabled = canConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor(), disabledContainerColor = secondarySurfaceColor(), disabledContentColor = secondaryTextColor()),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(46.dp),
                    ) { Text(localized("Готово", "Done", "Listo", "Fertig"), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp) }
                }
            }
        }
    }
}

@Composable
internal fun EditTripPanel(
    trip: TripCard,
    onClose: () -> Unit,
    onSaved: (TripCard) -> Unit,
    onDeleted: (String) -> Unit,
) {
    val language = LocalLanguage.current
    val displayedTitle = localizedTripTitle(trip.title)
    val displayedCities = localizedCityList(trip.cities, language)
    val displayedDates = localizedTripDateText(trip.dates, language)
    var title by remember(trip.id, language) { mutableStateOf(displayedTitle) }
    var cities by remember(trip.id, language) { mutableStateOf(displayedCities) }
    var dates by remember(trip.id, language) { mutableStateOf(displayedDates) }
    var status by remember(trip.id) { mutableStateOf(trip.status) }
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    var deleteDialogOpen by remember { mutableStateOf(false) }
    var leaveDialogOpen by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var tripCalendarOpen by remember { mutableStateOf(false) }
    var tripCalendarStep by remember { mutableStateOf("start") }
    var tripCalendarMonth by remember { mutableStateOf(YearMonth.now()) }
    var tripCalendarStart by remember { mutableStateOf<LocalDate?>(null) }
    var tripCalendarEnd by remember { mutableStateOf<LocalDate?>(null) }

    fun openTripDatePicker() {
        val fallbackStart = LocalDate.now()
        val currentRange = parseTripDateRange(dates) ?: parseTripDateRange(trip.dates)
        val currentStart = currentRange?.first ?: fallbackStart
        tripCalendarStart = currentStart
        tripCalendarEnd = currentRange?.second ?: currentStart.plusDays(1)
        tripCalendarMonth = YearMonth.from(currentStart)
        tripCalendarStep = "start"
        tripCalendarOpen = true
    }

    fun deleteTrip() {
        deleteDialogOpen = false
        scope.launch {
            deleting = true
            message = null
            runCatching {
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteTrip(trip.id)
            }
                .onSuccess { onDeleted(trip.id) }
                .onFailure {
                    message = it.message ?: localized(
                        language,
                        "Не удалось удалить путешествие",
                        "Could not delete trip",
                        "No se pudo eliminar el viaje",
                        "Reise konnte nicht gelöscht werden",
                    )
                }
            deleting = false
        }
    }

    fun leaveTrip() {
        leaveDialogOpen = false
        scope.launch {
            leaving = true
            message = null
            runCatching {
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).leaveTrip(trip.id)
            }
                .onSuccess { onDeleted(trip.id) }
                .onFailure {
                    message = it.message ?: localized(
                        language,
                        "Не удалось выйти из путешествия",
                        "Could not leave trip",
                        "No se pudo salir del viaje",
                        "Die Reise konnte nicht verlassen werden",
                    )
                }
            leaving = false
        }
    }

    val summaryTitle = when {
        status.contains("чернов", ignoreCase = true) || status.equals("draft", ignoreCase = true) -> localized("Черновик путешествия", "Draft trip", "Borrador de viaje", "Reiseentwurf")
        status.contains("прошед", ignoreCase = true) || status.contains("заверш", ignoreCase = true) || status.equals("past", ignoreCase = true) || status.equals("completed", ignoreCase = true) -> localized("Завершённое путешествие", "Completed trip", "Viaje completado", "Abgeschlossene Reise")
        else -> localized("Предстоящее путешествие", "Upcoming trip", "Próximo viaje", "Bevorstehende Reise")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardSurfaceColor())
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    localized("НАСТРОЙКИ ПОЕЗДКИ", "TRIP SETTINGS", "AJUSTES DEL VIAJE", "REISEEINSTELLUNGEN"),
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    displayedTitle,
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 21.sp,
                    lineHeight = 24.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(secondarySurfaceColor())
                    .clickable(onClick = onClose),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"),
                    tint = secondaryTextColor(),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(contentBorderColor()))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(tintedSurfaceColor())
                .padding(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(primaryColor()),
            ) {
                Text("R", color = primaryContentColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(summaryTitle, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(localizedTripDateText(dates, language), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            }
        }
        Text(
            localized("ОСНОВНОЕ", "MAIN DETAILS", "DATOS PRINCIPALES", "GRUNDDATEN"),
            color = secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(top = 1.dp),
        )
        CompactTripEditField(
            localized("Название путешествия", "Trip name", "Nombre del viaje", "Name der Reise"),
            title,
        ) { title = it }
        CompactTripEditField(
            localized("Маршрут", "Route", "Ruta", "Route"),
            cities,
            Icons.Outlined.LocationOn,
        ) { cities = it }
        CompactTripDateField(
            localized("Даты поездки", "Trip dates", "Fechas del viaje", "Reisedaten"),
            localizedTripDateText(dates, language),
            ::openTripDatePicker,
        )
        Text(
            localized("Статус", "Status", "Estado", "Status"),
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 1.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(secondarySurfaceColor())
                .padding(4.dp),
        ) {
            listOf(
                "Предстоящее" to localized("Предстоящие", "Upcoming", "Próximos", "Bevorstehend"),
                "Черновик" to localized("Черновики", "Drafts", "Borradores", "Entwürfe"),
                "Прошедшее" to localized("Прошедшие", "Past", "Pasados", "Vergangen"),
            ).forEach { (value, label) ->
                val selected = when (value) {
                    "Предстоящее" -> status.contains("предст", ignoreCase = true) || status.equals("upcoming", ignoreCase = true)
                    "Черновик" -> status.contains("чернов", ignoreCase = true) || status.equals("draft", ignoreCase = true)
                    else -> status.contains("прошед", ignoreCase = true) || status.contains("заверш", ignoreCase = true) || status.equals("past", ignoreCase = true) || status.equals("completed", ignoreCase = true)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) cardSurfaceColor() else Color.Transparent)
                        .clickable { status = value },
                ) {
                    Text(label, color = if (selected) contentTextColor() else secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = secondarySurfaceColor(), contentColor = contentTextColor()), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(46.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) { Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp) }
            Button(onClick = {
                scope.launch {
                    saving = true
                    val savedTitle = title.trim().takeUnless { it == displayedTitle } ?: trip.title.trim()
                    val savedCities = cities.trim().takeUnless { it == displayedCities } ?: trip.cities.trim()
                    val savedDates = dates.trim().takeUnless { it == displayedDates } ?: trip.dates.trim()
                    runCatching {
                        val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                        repository.updateTripDetails(trip.id, savedTitle, savedDates, savedCities)
                        repository.updateTripSection(trip.id, "status", JsonPrimitive(status))
                    }
                        .onSuccess { onSaved(trip.copy(title = savedTitle, cities = savedCities, dates = savedDates, status = status)) }
                        .onFailure { message = it.message ?: localized(language, "Не удалось сохранить изменения", "Could not save changes", "No se pudieron guardar los cambios", "Änderungen konnten nicht gespeichert werden") }
                    saving = false
                }
            }, enabled = !saving && !deleting && !leaving, colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(46.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) { Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp) }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFFF1D0CF), RoundedCornerShape(14.dp))
                .background(Color(0xFFFFF8F7))
                .clickable(enabled = !saving && !deleting && !leaving) {
                    if (trip.isOwner) deleteDialogOpen = true else leaveDialogOpen = true
                }
                .padding(11.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFFFFE9E7)),
            ) {
                Icon(if (trip.isOwner) Icons.Outlined.Delete else Icons.Outlined.Logout, contentDescription = null, tint = Color(0xFFD9534F), modifier = Modifier.size(17.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (trip.isOwner) localized("Удалить путешествие", "Delete trip", "Eliminar viaje", "Reise löschen")
                    else localized("Выйти из путешествия", "Leave trip", "Salir del viaje", "Reise verlassen"),
                    color = Color(0xFFD9534F),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 13.sp,
                )
                Text(
                    if (trip.isOwner) localized("Удаление нельзя отменить", "This cannot be undone", "No se puede deshacer", "Das kann nicht rückgängig gemacht werden")
                    else localized("Поездка останется у остальных участников", "The trip will remain for the other members", "El viaje permanecerá para los demás participantes", "Die Reise bleibt für die anderen Mitglieder erhalten"),
                    color = Color(0xFFB78380),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
    if (tripCalendarOpen) {
        TripDateCalendarDialog(
            language = language,
            month = tripCalendarMonth,
            startDate = tripCalendarStart,
            endDate = tripCalendarEnd,
            selectingEnd = tripCalendarStep == "end",
            onMonthChange = { tripCalendarMonth = it },
            onDateSelected = { selectedDate ->
                if (tripCalendarStep == "start") {
                    tripCalendarStart = selectedDate
                    tripCalendarEnd = tripCalendarEnd?.takeIf { !it.isBefore(selectedDate) }
                    tripCalendarMonth = YearMonth.from(tripCalendarEnd ?: selectedDate)
                    tripCalendarStep = "end"
                } else if (tripCalendarStart == null || !selectedDate.isBefore(tripCalendarStart)) {
                    tripCalendarEnd = selectedDate
                }
            },
            onStartClick = {
                tripCalendarStep = "start"
                tripCalendarStart?.let { tripCalendarMonth = YearMonth.from(it) }
            },
            onEndClick = {
                tripCalendarStep = "end"
                tripCalendarEnd?.let { tripCalendarMonth = YearMonth.from(it) }
            },
            onDismiss = { tripCalendarOpen = false },
            onConfirm = {
                val selectedStart = tripCalendarStart
                val selectedEnd = tripCalendarEnd
                if (selectedStart != null && selectedEnd != null && !selectedEnd.isBefore(selectedStart)) {
                    dates = "$selectedStart — $selectedEnd"
                    tripCalendarOpen = false
                }
            },
        )
    }
    if (deleteDialogOpen) {
        AlertDialog(
            onDismissRequest = { if (!deleting) deleteDialogOpen = false },
            title = {
                Text(localized("Удалить путешествие?", "Delete trip?", "¿Eliminar viaje?", "Reise löschen?"), fontFamily = Manrope, fontWeight = FontWeight.W800)
            },
            text = {
                Text(
                    localized(
                        "Путешествие, его фотографии и данные будут удалены без возможности восстановления.",
                        "This trip, its photos, and all of its data will be permanently deleted.",
                        "Este viaje, sus fotos y todos sus datos se eliminarán de forma permanente.",
                        "Diese Reise, ihre Fotos und alle Daten werden dauerhaft gelöscht.",
                    ),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                )
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogOpen = false }, enabled = !deleting) {
                    Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = ::deleteTrip,
                    enabled = !deleting,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD9534F)),
                ) {
                    Text(
                        if (deleting) localized("Удаляем…", "Deleting…", "Eliminando…", "Wird gelöscht…")
                        else localized("Удалить", "Delete", "Eliminar", "Löschen"),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                    )
                }
            },
        )
    }
    if (leaveDialogOpen) {
        AlertDialog(
            onDismissRequest = { if (!leaving) leaveDialogOpen = false },
            title = {
                Text(localized("Выйти из путешествия?", "Leave trip?", "¿Salir del viaje?", "Reise verlassen?"), fontFamily = Manrope, fontWeight = FontWeight.W800)
            },
            text = {
                Text(
                    localized(
                        "Вы перестанете видеть это путешествие. У создателя и остальных участников оно останется.",
                        "You will stop seeing this trip. It will remain with the owner and other members.",
                        "Dejarás de ver este viaje. Permanecerá para el creador y los demás participantes.",
                        "Sie sehen diese Reise danach nicht mehr. Für den Ersteller und die anderen Mitglieder bleibt sie erhalten.",
                    ),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                )
            },
            dismissButton = {
                TextButton(onClick = { leaveDialogOpen = false }, enabled = !leaving) {
                    Text(localized("Остаться", "Stay", "Quedarse", "Bleiben"), fontFamily = Manrope, fontWeight = FontWeight.W800)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = ::leaveTrip,
                    enabled = !leaving,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD9534F)),
                ) {
                    Text(
                        if (leaving) localized("Выходим…", "Leaving…", "Saliendo…", "Wird verlassen…")
                        else localized("Выйти", "Leave", "Salir", "Verlassen"),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                    )
                }
            },
        )
    }
}

