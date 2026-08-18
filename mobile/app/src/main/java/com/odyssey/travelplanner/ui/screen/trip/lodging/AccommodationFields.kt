package com.odyssey.travelplanner.ui.screen.trip.lodging

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import com.odyssey.travelplanner.ui.domain.accommodationDateCalendar
import com.odyssey.travelplanner.ui.domain.accommodationDateIso
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.icons.OdysseyCalendarIcon
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.OdysseyPurpleGradientEnd
import com.odyssey.travelplanner.ui.theme.OdysseySheetScrim
import com.odyssey.travelplanner.ui.theme.OdysseyTightText
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.surfaceVariantColor

@Composable
internal fun AccommodationEditTextField(
    label: String,
    value: String,
    placeholder: String,
    valueWeight: FontWeight,
    valueColor: Color,
    scale: Float,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    fun s(value: Float) = (value * scale).sp
    Column(modifier = modifier.height(d(77f))) {
        Text(
            text = label,
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13f),
            lineHeight = s(18f),
            style = OdysseyTightText,
            modifier = Modifier.height(d(18f)),
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(d(8f)))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d(51f))
                .clip(RoundedCornerShape(d(14f)))
                .background(cardSurfaceColor())
                .border(d(1f), contentBorderColor(), RoundedCornerShape(d(14f))),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = valueColor,
                    fontFamily = Manrope,
                    fontWeight = valueWeight,
                    fontSize = s(15f),
                    lineHeight = s(20f),
                    platformStyle = OdysseyNoFontPadding,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(primaryColor()),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = d(15f)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                color = secondaryTextColor(),
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W600,
                                fontSize = s(15f),
                                lineHeight = s(20f),
                                style = OdysseyTightText,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Composable
internal fun AccommodationEditDateField(
    label: String,
    value: String,
    scale: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    fun s(value: Float) = (value * scale).sp
    Column(modifier = modifier.height(d(77f))) {
        Text(
            text = label,
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13f),
            lineHeight = s(18f),
            style = OdysseyTightText,
            modifier = Modifier.height(d(18f)),
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(d(8f)))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d(51f))
                .clip(RoundedCornerShape(d(14f)))
                .background(cardSurfaceColor())
                .border(d(1f), contentBorderColor(), RoundedCornerShape(d(14f)))
                .clickable(onClick = onClick),
        ) {
            BasicTextField(
                value = value,
                onValueChange = {},
                enabled = false,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                    fontSize = s(15f),
                    lineHeight = s(20f),
                    platformStyle = OdysseyNoFontPadding,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = d(32f)),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = d(12f)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        innerTextField()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = d(12f)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                OdysseyCalendarIcon(d(14f), contentTextColor())
            }
        }
    }
}

@Composable
internal fun AccommodationCalendarDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val language = LocalLanguage.current
    val initialCalendar = remember(initialValue) { accommodationDateCalendar(initialValue) }
    var displayedYear by remember(initialValue) { mutableStateOf(initialCalendar.get(Calendar.YEAR)) }
    var displayedMonth by remember(initialValue) { mutableStateOf(initialCalendar.get(Calendar.MONTH)) }
    var selectedYear by remember(initialValue) { mutableStateOf(initialCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember(initialValue) { mutableStateOf(initialCalendar.get(Calendar.MONTH)) }
    var selectedDay by remember(initialValue) { mutableStateOf(initialCalendar.get(Calendar.DAY_OF_MONTH)) }
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

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OdysseySheetScrim),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(336.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(cardSurfaceColor())
                    .padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                ) {
                    Text(
                        text = localized("Выберите дату", "Choose date", "Elige una fecha", "Datum auswählen"),
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        style = OdysseyTightText,
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(surfaceVariantColor())
                            .clickable(onClick = onDismiss),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = secondaryTextColor(), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(32.dp).clip(CircleShape).clickable {
                            if (displayedMonth == 0) {
                                displayedMonth = 11
                                displayedYear -= 1
                            } else {
                                displayedMonth -= 1
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = localized("Предыдущий месяц", "Previous month", "Mes anterior", "Vorheriger Monat"), tint = primaryColor(), modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "${monthNames[displayedMonth]} $displayedYear",
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        style = OdysseyTightText,
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(32.dp).clip(CircleShape).clickable {
                            if (displayedMonth == 11) {
                                displayedMonth = 0
                                displayedYear += 1
                            } else {
                                displayedMonth += 1
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = localized("Следующий месяц", "Next month", "Mes siguiente", "Nächster Monat"), tint = primaryColor(), modifier = Modifier.size(20.dp).graphicsLayer { rotationY = 180f })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                    weekDays.forEach { day ->
                        Text(
                            text = day,
                            color = secondaryTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center,
                            style = OdysseyTightText,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.fillMaxWidth()) {
                    (0 until 6).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth().height(42.dp)) {
                            (0 until 7).forEach { weekday ->
                                val dayIndex = week * 7 + weekday - leadingEmpty + 1
                                val validDay = dayIndex in 1..daysInMonth
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    if (validDay) {
                                        val selected = dayIndex == selectedDay && displayedYear == selectedYear && displayedMonth == selectedMonth
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (selected) primaryColor() else Color.Transparent)
                                                .clickable {
                                                    selectedYear = displayedYear
                                                    selectedMonth = displayedMonth
                                                    selectedDay = dayIndex
                                                },
                                        ) {
                                            Text(
                                                text = dayIndex.toString(),
                                                color = if (selected) primaryContentColor() else contentTextColor(),
                                                fontFamily = Manrope,
                                                fontWeight = if (selected) FontWeight.W800 else FontWeight.W600,
                                                fontSize = 14.sp,
                                                lineHeight = 18.sp,
                                                textAlign = TextAlign.Center,
                                                style = OdysseyTightText,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(11.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
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
                        Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, style = OdysseyTightText)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(15.dp))
                            .background(Brush.linearGradient(listOf(primaryColor(), OdysseyPurpleGradientEnd)))
                            .clickable { onConfirm(accommodationDateIso(selectedYear, selectedMonth, selectedDay)) },
                    ) {
                        Text(localized("Готово", "Done", "Listo", "Fertig"), color = primaryContentColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, style = OdysseyTightText)
                    }
                }
            }
        }
    }
}

