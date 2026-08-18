package com.odyssey.travelplanner.ui.screen.trip.restaurants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityFilter
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
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
internal fun RestaurantCityFilterSheet(
    options: List<String>,
    counts: Map<String, Int>,
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
                    .clickable { onClose() },
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
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                if (option == options.first()) {
                    Box(
                        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(tintedSurfaceColor()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Explore, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    text = localizedCityFilter(option),
                    color = if (active) primaryColor() else contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 15.5.sp,
                    lineHeight = 20.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    modifier = Modifier.weight(1f),
                )
                if (option != options.first()) {
                    Text(
                        text = (counts[option] ?: 0).toString(),
                        color = secondaryTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W700,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
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

@Composable
internal fun RestaurantFilterSheet(
    type: String,
    features: Set<String>,
    price: String,
    rating: String,
    onTypeChange: (String) -> Unit,
    onFeatureToggle: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onRatingChange: (String) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Keep the reference proportions on wider Android devices; only shrink on narrower ones.
        val scale = minOf(maxWidth.value / 368f, 1f)
        fun d(value: Float) = (value * scale).dp
        fun s(value: Float) = (value * scale).sp
        val contentWidth = (maxWidth.value - 32f).coerceAtLeast(0f).dp
        val resetX = (maxWidth.value - 97f).coerceAtLeast(16f).dp
        val sectionStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(11f),
            lineHeight = s(15f),
            color = secondaryTextColor(),
            platformStyle = OdysseyNoFontPadding,
        )
        val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Box(Modifier.fillMaxWidth().height(d(604f) + navigationBarInset)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = d(12f))
                    .size(d(40f), d(4f))
                    .clip(RoundedCornerShape(d(2f)))
                    .background(Color(0xFFE6E6EC)),
            )
            Text(
                text = localized("Фильтры", "Filters", "Filtros", "Filter"),
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = s(22f),
                lineHeight = s(30f),
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                modifier = Modifier.offset(x = d(16f), y = d(30f)).width(d(190f)).height(d(30f)),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = resetX, y = d(34.5f))
                    .width(d(81f))
                    .height(d(21f))
                    .clickable(onClick = onReset),
            ) {
                Text(
                    text = localized("Сбросить", "Reset", "Restablecer", "Zurücksetzen"),
                    color = primaryColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = s(14f),
                    lineHeight = s(21f),
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                )
            }

            Text(
                text = localized("ТИП ЗАВЕДЕНИЯ", "VENUE TYPE", "TIPO DE LOCAL", "ART DES LOKALS"),
                style = sectionStyle,
                modifier = Modifier.offset(x = d(16f), y = d(78f)).width(contentWidth).height(d(15f)),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(10f)),
                modifier = Modifier.offset(x = d(16f), y = d(103f)).width(contentWidth).height(d(75f)),
            ) {
                listOf(
                    "Ресторан" to "restaurant",
                    "Бар" to "bar",
                    "Кафе" to "cafe",
                ).forEach { (label, kind) ->
                    RestaurantFilterTypeButton(
                        label = localized(label, when (label) { "Бар" -> "Bar"; "Кафе" -> "Cafe"; else -> "Restaurant" }, when (label) { "Бар" -> "Bar"; "Кафе" -> "Café"; else -> "Restaurante" }, when (label) { "Бар" -> "Bar"; "Кафе" -> "Café"; else -> "Restaurant" }),
                        kind = kind,
                        selected = type == label,
                        scale = scale,
                        modifier = Modifier.weight(1f),
                        onClick = { onTypeChange(label) },
                    )
                }
            }

            Text(
                text = localized("ОСОБЕННОСТИ", "FEATURES", "CARACTERÍSTICAS", "MERKMALE"),
                style = sectionStyle,
                modifier = Modifier.offset(x = d(16f), y = d(198f)).width(contentWidth).height(d(15f)),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(9f)),
                modifier = Modifier.offset(x = d(16f), y = d(223f)).height(d(38f)),
            ) {
                RestaurantFilterFeatureChip(localized("Приоритет", "Priority", "Prioridad", "Priorität"), "priority", "priority" in features, scale, onFeatureToggle)
                RestaurantFilterFeatureChip(localized("С собакой", "Dog-friendly", "Con perro", "Hundefreundlich"), "dog", "dog" in features, scale, onFeatureToggle)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(9f)),
                modifier = Modifier.offset(x = d(16f), y = d(270f)).height(d(38f)),
            ) {
                RestaurantFilterFeatureChip(localized("Есть бронь", "Has reservation", "Tiene reserva", "Reservierung vorhanden"), "reservation", "reservation" in features, scale, onFeatureToggle)
                RestaurantFilterFeatureChip(localized("Веган", "Vegan", "Vegano", "Vegan"), "vegan", "vegan" in features, scale, onFeatureToggle)
            }

            Text(
                text = localized("СРЕДНИЙ ЧЕК", "AVERAGE PRICE", "PRECIO MEDIO", "DURCHSCHNITTSPREIS"),
                style = sectionStyle,
                modifier = Modifier.offset(x = d(16f), y = d(330f)).width(contentWidth).height(d(15f)),
            )
            RestaurantFilterSegmentedRow(
                // Product requirement: keep all four restaurant price levels.
                options = listOf("€", "€€", "€€€", "€€€€"),
                selected = price,
                onSelect = onPriceChange,
                scale = scale,
                itemFontSize = 12f,
                modifier = Modifier.offset(x = d(16f), y = d(355f)).width(contentWidth),
            )

            Text(
                text = localized("РЕЙТИНГ ОТ", "RATING FROM", "VALORACIÓN DESDE", "BEWERTUNG AB"),
                style = sectionStyle,
                modifier = Modifier.offset(x = d(16f), y = d(430f)).width(contentWidth).height(d(15f)),
            )
            RestaurantFilterSegmentedRow(
                options = listOf("4.0+", "4.5+", "4.8+"),
                selected = rating,
                onSelect = onRatingChange,
                scale = scale,
                modifier = Modifier.offset(x = d(16f), y = d(455f)).width(contentWidth),
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = d(16f), y = d(532f))
                    .width(contentWidth)
                    .height(d(54f))
                    .shadow(d(8f), RoundedCornerShape(d(15f)), clip = false, ambientColor = Color(0x4D6C5CE7), spotColor = Color(0x4D6C5CE7))
                    .clip(RoundedCornerShape(d(15f)))
                    .background(Brush.linearGradient(listOf(primaryColor(), Color(0xFF7D6CF0))))
                    .clickable(onClick = onApply),
            ) {
                Text(
                    text = localized("Показать результаты", "Show results", "Mostrar resultados", "Ergebnisse anzeigen"),
                    color = primaryContentColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = s(16f),
                    lineHeight = s(22f),
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                )
            }
        }
    }
}

@Composable
internal fun RestaurantFilterTypeButton(
    label: String,
    kind: String,
    selected: Boolean,
    scale: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d(7f)),
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(d(15f)))
            .background(if (selected) Brush.linearGradient(listOf(primaryColor(), Color(0xFF7D6CF0))) else Brush.linearGradient(listOf(cardSurfaceColor(), cardSurfaceColor())))
            .border(d(1.6f), if (selected) primaryColor() else contentBorderColor(), RoundedCornerShape(d(15f)))
            .clickable(onClick = onClick)
            .padding(top = d(14f), bottom = d(14f)),
    ) {
        RestaurantFilterTypeIcon(kind, d(20f), if (selected) primaryContentColor() else primaryColor())
        Text(
            text = label,
            color = if (selected) primaryContentColor() else contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = d(13.5f).value.sp,
            lineHeight = d(18f).value.sp,
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            maxLines = 1,
        )
    }
}

@Composable
internal fun RestaurantFilterFeatureChip(
    label: String,
    kind: String,
    selected: Boolean,
    scale: Float,
    onToggle: (String) -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d(5f)),
        modifier = Modifier
            .height(d(38f))
            .clip(RoundedCornerShape(d(12f)))
            .background(if (selected) primaryColor() else cardSurfaceColor())
            .border(d(1.6f), if (selected) primaryColor() else contentBorderColor(), RoundedCornerShape(d(12f)))
            .clickable { onToggle(kind) }
            .padding(horizontal = d(13f)),
    ) {
        RestaurantFilterFeatureIcon(kind, d(14f), if (selected) primaryContentColor() else primaryColor())
        Text(
            text = label,
            color = if (selected) primaryContentColor() else labelColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = d(13.5f).value.sp,
            lineHeight = d(18f).value.sp,
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
internal fun RestaurantFilterSegmentedRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    scale: Float,
    itemFontSize: Float = 14f,
    modifier: Modifier = Modifier,
) {
    fun d(value: Float) = (value * scale).dp
    Row(
        horizontalArrangement = Arrangement.spacedBy(d(5f)),
        modifier = modifier
            .height(d(53f))
            .clip(RoundedCornerShape(d(14f)))
            .background(secondarySurfaceColor())
            .padding(d(5f)),
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(d(43f))
                    .shadow(if (active) d(2f) else 0.dp, RoundedCornerShape(d(11f)), clip = false, ambientColor = Color(0x1A000000), spotColor = Color(0x1A000000))
                    .clip(RoundedCornerShape(d(11f)))
                    .background(if (active) cardSurfaceColor() else Color.Transparent)
                    .clickable { onSelect(option) },
            ) {
                Text(
                    text = option,
                    color = if (active) contentTextColor() else secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = d(itemFontSize).value.sp,
                    lineHeight = d(itemFontSize * 1.35f).value.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
internal fun RestaurantFilterTypeIcon(kind: String, iconSize: Dp, color: Color) {
    Canvas(Modifier.size(iconSize)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = 2.dp.toPx()
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        when (kind) {
            "restaurant" -> {
                drawLine(color, p(3f, 2f), p(3f, 9f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color, p(5f, 2f), p(5f, 22f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color, p(7f, 2f), p(7f, 9f), strokeWidth = stroke, cap = StrokeCap.Round)
                val fork = Path().apply {
                    moveTo(3f * sx, 9f * sy)
                    cubicTo(3f * sx, 10.1f * sy, 3.9f * sx, 11f * sy, 5f * sx, 11f * sy)
                    cubicTo(6.1f * sx, 11f * sy, 7f * sx, 10.1f * sy, 7f * sx, 9f * sy)
                }
                drawPath(fork, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
                val spoon = Path().apply {
                    moveTo(17f * sx, 2f * sy)
                    lineTo(17f * sx, 12f * sy)
                    cubicTo(19f * sx, 12f * sy, 21f * sx, 10.5f * sy, 21f * sx, 7f * sy)
                    cubicTo(21f * sx, 3.5f * sy, 19f * sx, 2f * sy, 17f * sx, 2f * sy)
                    close()
                }
                drawPath(spoon, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
                drawLine(color, p(17f, 12f), p(17f, 22f), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            "bar" -> {
                drawLine(color, p(8f, 22f), p(16f, 22f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color, p(12f, 11f), p(12f, 22f), strokeWidth = stroke, cap = StrokeCap.Round)
                val glass = Path().apply { moveTo(3f * sx, 5f * sy); lineTo(21f * sx, 5f * sy); lineTo(12f * sx, 11f * sy); close() }
                drawPath(glass, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            else -> {
                val cup = Path().apply {
                    moveTo(2f * sx, 8f * sy)
                    lineTo(18f * sx, 8f * sy)
                    lineTo(18f * sx, 17f * sy)
                    cubicTo(18f * sx, 19.2f * sy, 16.2f * sx, 21f * sy, 14f * sx, 21f * sy)
                    lineTo(6f * sx, 21f * sy)
                    cubicTo(3.8f * sx, 21f * sy, 2f * sx, 19.2f * sy, 2f * sx, 17f * sy)
                    close()
                }
                drawPath(cup, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                val handle = Path().apply { moveTo(18f * sx, 8f * sy); lineTo(19f * sx, 8f * sy); cubicTo(24f * sx, 8f * sy, 24f * sx, 16f * sy, 19f * sx, 16f * sy); lineTo(18f * sx, 16f * sy) }
                drawPath(handle, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
internal fun RestaurantFilterFeatureIcon(kind: String, iconSize: Dp, color: Color) {
    Canvas(Modifier.size(iconSize)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        when (kind) {
            "priority" -> {
                val star = Path().apply {
                    moveTo(12f * sx, 2f * sy)
                    lineTo(15f * sx, 8.5f * sy)
                    lineTo(22f * sx, 9.1f * sy)
                    lineTo(16.7f * sx, 13.7f * sy)
                    lineTo(18.3f * sx, 20.5f * sy)
                    lineTo(12f * sx, 17.3f * sy)
                    lineTo(5.1f * sx, 20.5f * sy)
                    lineTo(6.7f * sx, 13.7f * sy)
                    lineTo(1.4f * sx, 9.1f * sy)
                    lineTo(8.4f * sx, 8.5f * sy)
                    close()
                }
                drawPath(star, color)
            }
            "dog" -> {
                drawCircle(color, radius = 2f * sx, center = p(5f, 9f))
                drawCircle(color, radius = 2f * sx, center = p(19f, 9f))
                drawCircle(color, radius = 2f * sx, center = p(9f, 5f))
                drawCircle(color, radius = 2f * sx, center = p(15f, 5f))
                val dog = Path().apply {
                    moveTo(12f * sx, 11f * sy)
                    cubicTo(9f * sx, 11f * sy, 7f * sx, 13.5f * sy, 7f * sx, 16f * sy)
                    cubicTo(7f * sx, 18f * sy, 8.5f * sx, 19f * sy, 10f * sx, 19f * sy)
                    cubicTo(11f * sx, 19f * sy, 11.5f * sx, 18.5f * sy, 12f * sx, 18.5f * sy)
                    cubicTo(12.5f * sx, 18.5f * sy, 13f * sx, 19f * sy, 14f * sx, 19f * sy)
                    cubicTo(15.5f * sx, 19f * sy, 17f * sx, 18f * sy, 17f * sx, 16f * sy)
                    cubicTo(17f * sx, 13.5f * sy, 15f * sx, 11f * sy, 12f * sx, 11f * sy)
                    close()
                }
                drawPath(dog, color)
            }
            "reservation" -> {
                val calendar = Path().apply {
                    moveTo(5f * sx, 4f * sy)
                    lineTo(19f * sx, 4f * sy)
                    cubicTo(20.1f * sx, 4f * sy, 21f * sx, 4.9f * sy, 21f * sx, 6f * sy)
                    lineTo(21f * sx, 20f * sy)
                    cubicTo(21f * sx, 21.1f * sy, 20.1f * sx, 22f * sy, 19f * sx, 22f * sy)
                    lineTo(5f * sx, 22f * sy)
                    cubicTo(3.9f * sx, 22f * sy, 3f * sx, 21.1f * sy, 3f * sx, 20f * sy)
                    lineTo(3f * sx, 6f * sy)
                    cubicTo(3f * sx, 4.9f * sy, 3.9f * sx, 4f * sy, 5f * sx, 4f * sy)
                }
                drawPath(calendar, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                drawLine(color, p(8f, 2f), p(8f, 6f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color, p(16f, 2f), p(16f, 6f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color, p(3f, 10f), p(21f, 10f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
            else -> {
                val leaf = Path().apply {
                    moveTo(11f * sx, 20f * sy)
                    cubicTo(7f * sx, 20f * sy, 4f * sx, 17f * sy, 4f * sx, 13f * sy)
                    cubicTo(4f * sx, 7f * sy, 10f * sx, 3f * sy, 20f * sx, 3f * sy)
                    cubicTo(19f * sx, 11f * sy, 15f * sx, 18f * sy, 11f * sx, 20f * sy)
                    close()
                }
                drawPath(leaf, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                drawLine(color, p(11f, 20f), p(17f, 12f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

