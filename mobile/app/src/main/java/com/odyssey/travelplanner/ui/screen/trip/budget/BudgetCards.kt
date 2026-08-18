package com.odyssey.travelplanner.ui.screen.trip.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedBudgetCategory
import com.odyssey.travelplanner.ui.icons.OdysseyEditIcon
import com.odyssey.travelplanner.ui.icons.OdysseyPlusIcon
import com.odyssey.travelplanner.ui.theme.LocalDarkTheme
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyDarkSurface2
import com.odyssey.travelplanner.ui.theme.OdysseyDarkTint
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondarySurfaceColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor
import com.odyssey.travelplanner.ui.theme.trackColor

@Composable
internal fun BudgetSummaryCard(total: Double, currencySymbol: String, conversionRate: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(103.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(primaryColor())
            .padding(start = 22.dp, top = 22.dp),
    ) {
        Text(
            text = localized("ОБЩАЯ СУММА", "TOTAL", "TOTAL", "GESAMTSUMME"),
            color = primaryContentColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 1.1.sp,
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.height(15.dp),
        )
        Text(
            text = formatBudgetAmount(total, currencySymbol, conversionRate),
            color = primaryContentColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 38.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.76).sp,
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
internal fun BudgetCurrencySelector(
    selectedCode: String,
    options: List<BudgetCurrencyStyle>,
    saving: Boolean,
    editable: Boolean = true,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor())
            .padding(5.dp),
    ) {
        options.forEach { option ->
            val selected = option.code == selectedCode
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) cardSurfaceColor() else Color.Transparent)
                    .clickable(enabled = editable && !saving && !selected) { onSelect(option.code) },
            ) {
                Text(
                    text = option.symbol,
                    color = if (selected) contentTextColor() else secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                )
            }
        }
    }
}

@Composable
internal fun BudgetExchangeRateCard(
    currencyCode: String,
    rubPerUnit: Double,
    isManual: Boolean,
    loading: Boolean,
    onlineRateDate: String,
    hasOnlineRate: Boolean,
    message: String?,
    editable: Boolean = true,
    onEdit: () -> Unit,
    onRefresh: () -> Unit,
) {
    val rateText = if (currencyCode == "RUB") {
        localized("Базовая валюта: RUB", "Base currency: RUB", "Moneda base: RUB", "Basiswährung: RUB")
    } else {
        localized(
            "1 $currencyCode = ${formatBudgetRate(rubPerUnit)} ₽",
            "1 $currencyCode = ${formatBudgetRate(rubPerUnit)} RUB",
            "1 $currencyCode = ${formatBudgetRate(rubPerUnit)} RUB",
            "1 $currencyCode = ${formatBudgetRate(rubPerUnit)} RUB",
        )
    }
    val sourceText = when {
        currencyCode == "RUB" -> localized("Расходы хранятся в рублях", "Expenses are stored in RUB", "Los gastos se guardan en RUB", "Ausgaben werden in RUB gespeichert")
        isManual -> localized("Задано вручную", "Set manually", "Definido manualmente", "Manuell festgelegt")
        loading -> localized("Обновляем онлайн-курс…", "Refreshing online rate…", "Actualizando el tipo online…", "Online-Kurs wird aktualisiert…")
        hasOnlineRate -> localized("Frankfurter · ${onlineRateDate.ifBlank { "сегодня" }}", "Frankfurter · ${onlineRateDate.ifBlank { "today" }}", "Frankfurter · ${onlineRateDate.ifBlank { "hoy" }}", "Frankfurter · ${onlineRateDate.ifBlank { "heute" }}")
        else -> localized("Нет онлайн-курса — задайте вручную", "No online rate — set it manually", "Sin tipo online: establécelo manualmente", "Kein Online-Kurs — manuell festlegen")
    }
    val statusText = when {
        loading -> localized("обновляем", "refreshing", "actualizando", "aktualisiert")
        hasOnlineRate -> localized("онлайн", "online", "online", "online")
        isManual -> localized("вручную", "manual", "manual", "manuell")
        else -> localized("нет курса", "no rate", "sin tipo", "kein Kurs")
    }
    val editRateDescription = localized("Изменить курс", "Edit rate", "Editar tipo", "Kurs ändern")
    val refreshRateDescription = localized("Обновить курс", "Refresh rate", "Actualizar tipo", "Kurs aktualisieren")
    val darkTheme = LocalDarkTheme.current
    val exchangeBrush = if (darkTheme) {
        Brush.linearGradient(listOf(OdysseyDarkSurface2, OdysseyDarkTint))
    } else {
        Brush.linearGradient(listOf(Color(0xFF604BD7), primaryColor(), Color(0xFF9588F0)))
    }
    val exchangeTextColor = if (darkTheme) contentTextColor() else Color.White
    val exchangeSecondaryTextColor = if (darkTheme) secondaryTextColor() else Color.White.copy(alpha = 0.72f)
    val exchangeChipColor = if (darkTheme) OdysseyDarkTint else Color.White.copy(alpha = 0.16f)
    val detailText = message ?: if (currencyCode == "RUB") {
        sourceText
    } else {
        localized(
            "за 1 $currencyCode · $sourceText",
            "per 1 $currencyCode · $sourceText",
            "por 1 $currencyCode · $sourceText",
            "pro 1 $currencyCode · $sourceText",
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                clip = false,
                ambientColor = Color(0x40604BD7),
                spotColor = Color(0x40604BD7),
            )
            .clip(RoundedCornerShape(18.dp))
            .background(exchangeBrush)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                localized("Курс валюты", "Exchange rate", "Tipo de cambio", "Wechselkurs"),
                color = exchangeSecondaryTextColor,
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                fontSize = 10.sp,
                letterSpacing = 0.4.sp,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(exchangeChipColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        statusText,
                        color = exchangeTextColor,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W700,
                        fontSize = 10.sp,
                    )
                }
                if (editable && currencyCode != "RUB") {
                    Text(
                        text = localized("Изменить", "Edit", "Editar", "Ändern"),
                        color = exchangeTextColor,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onEdit)
                            .semantics {
                                role = Role.Button
                                contentDescription = editRateDescription
                            }
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Text(
            rateText,
            color = exchangeTextColor,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 22.sp,
            lineHeight = 25.sp,
            letterSpacing = (-0.45).sp,
            modifier = Modifier.padding(top = 9.dp),
            maxLines = 1,
            softWrap = false,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp),
        ) {
            Text(
                detailText,
                color = if (message != null) Color(0xFFFFD2D0) else if (darkTheme) secondaryTextColor() else Color.White.copy(alpha = 0.76f),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 9.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (currencyCode != "RUB") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(exchangeChipColor)
                        .clickable(enabled = !loading, onClick = onRefresh)
                        .semantics {
                            role = Role.Button
                            contentDescription = refreshRateDescription
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        if (loading) localized("Обновляем…", "Refreshing…", "Actualizando…", "Aktualisierung…")
                        else localized("↻  Обновить", "↻  Refresh", "↻  Actualizar", "↻  Aktualisieren"),
                        color = Color.White,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 9.sp,
                    )
                }
            }
        }
    }
}

@Composable
internal fun BudgetMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(cardSurfaceColor())
            .border(1.dp, contentBorderColor(), RoundedCornerShape(16.dp))
            .padding(start = 12.dp, top = 13.dp, end = 12.dp),
    ) {
        Text(
            text = label,
            color = secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.6.sp,
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.height(14.dp),
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = value,
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
internal fun BudgetCategoryRow(style: BudgetCategoryStyle, amount: Double, total: Double, currencySymbol: String, conversionRate: Double) {
    val fraction = if (total <= 0.0) 0f else (amount / total).toFloat().coerceIn(0f, 1f)
    val percent = if (total <= 0.0) 0 else (amount / total * 100.0).toInt()
    Column(modifier = Modifier.fillMaxWidth().height(35.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(19.dp)) {
            Box(modifier = Modifier.size(11.dp).clip(RoundedCornerShape(4.dp)).background(style.color))
            Text(
                text = localizedBudgetCategory(style.label),
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                modifier = Modifier.padding(start = 9.dp),
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = " $percent%",
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                modifier = Modifier.padding(start = 5.dp),
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatBudgetAmount(amount, currencySymbol, conversionRate),
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(5.dp)).background(secondarySurfaceColor())) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(RoundedCornerShape(5.dp)).background(style.color))
        }
    }
}

@Composable
internal fun BudgetExpensesCard(
    expenses: List<com.odyssey.travelplanner.data.BudgetExpense>,
    currencySymbol: String,
    conversionRate: Double,
    editMode: Boolean,
    editable: Boolean = true,
    deletingExpenseId: String?,
    onToggleEditMode: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (com.odyssey.travelplanner.data.BudgetExpense) -> Unit,
    onDelete: (com.odyssey.travelplanner.data.BudgetExpense) -> Unit,
) {
    val dividerColor = contentBorderColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardSurfaceColor())
            .border(1.dp, contentBorderColor(), RoundedCornerShape(20.dp))
            .padding(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .drawBehind {
                    drawLine(dividerColor, Offset(0f, size.height - 0.5.dp.toPx()), Offset(size.width, size.height - 0.5.dp.toPx()), strokeWidth = 1.dp.toPx())
                },
        ) {
            Text(
                text = localized("Расходы", "Expenses", "Gastos", "Ausgaben"),
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            )
            Spacer(Modifier.weight(1f))
            if (editable) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(tintedSurfaceColor())
                        .clickable(onClick = onToggleEditMode),
                ) {
                    OdysseyEditIcon(16.dp, primaryColor())
                }
            }
        }
        expenses.forEachIndexed { index, expense ->
            BudgetExpenseRow(
                expense = expense,
                currencySymbol = currencySymbol,
                conversionRate = conversionRate,
                editMode = editable && editMode,
                deleting = deletingExpenseId == expense.id,
                showDivider = index < expenses.lastIndex,
                onEdit = { onEdit(expense) },
                onDelete = { onDelete(expense) },
            )
        }
        Spacer(Modifier.height(5.dp))
        if (editable) BudgetDashedButton(onClick = onAdd)
    }
}

@Composable
internal fun BudgetExpenseRow(
    expense: com.odyssey.travelplanner.data.BudgetExpense,
    currencySymbol: String,
    conversionRate: Double,
    editMode: Boolean,
    deleting: Boolean,
    showDivider: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val categoryStyle = when (expense.category.trim().lowercase(java.util.Locale.ROOT)) {
        "проживание" -> BudgetCategoryStyle("Жильё", "Проживание", Color(0xFF6C5CE7), emptySet())
        "жильё", "жилье" -> BudgetCategoryStyle("Жильё", "Проживание", Color(0xFF6C5CE7), emptySet())
        "транспорт" -> BudgetCategoryStyle("Транспорт", "Транспорт", Color(0xFFF5A623), emptySet())
        "еда и рестораны", "еда", "питание" -> BudgetCategoryStyle("Еда и рестораны", "Питание", Color(0xFF22B07D), emptySet())
        "активности и билеты", "активности", "развлечения" -> BudgetCategoryStyle("Активности и билеты", "Развлечения", Color(0xFF4AA3F0), emptySet())
        else -> BudgetCategoryStyle("Прочее", "Прочее", Color(0xFFEE6C8A), emptySet())
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(categoryStyle.color))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedBudgetExpenseName(expense.name),
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 14.5.sp,
                    lineHeight = 19.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = localizedBudgetCategory(categoryStyle.label),
                    color = categoryStyle.color,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(categoryStyle.color.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            if (editMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BudgetExpenseActionButton(
                        background = tintedSurfaceColor(),
                        onClick = onEdit,
                        enabled = !deleting,
                    ) { OdysseyEditIcon(14.dp, primaryColor()) }
                    BudgetExpenseActionButton(
                        background = Color(0xFFFFE9E8),
                        onClick = onDelete,
                        enabled = !deleting,
                    ) { Icon(Icons.Outlined.Delete, contentDescription = localized("Удалить", "Delete", "Eliminar", "Löschen"), tint = Color(0xFFFF6B65), modifier = Modifier.size(16.dp)) }
                }
            } else {
                Text(
                    text = formatBudgetAmount(expense.amount, currencySymbol, conversionRate),
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(secondarySurfaceColor()),
            )
        }
    }
}

@Composable
internal fun BudgetExpenseActionButton(background: Color, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        content()
    }
}

@Composable
internal fun BudgetDashedButton(onClick: () -> Unit) {
    val dashedBorderColor = contentBorderColor()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(47.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tintedSurfaceColor())
            .drawBehind {
                val stroke = 1.6.dp.toPx()
                drawRoundRect(
                    color = dashedBorderColor,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(14.dp.toPx() - stroke / 2f),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OdysseyPlusIcon(17.dp, primaryColor())
            Text(
                text = localized("Добавить трату", "Add expense", "Añadir gasto", "Ausgabe hinzufügen"),
                color = primaryColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                modifier = Modifier.padding(start = 7.dp),
            )
        }
    }
}

@Composable
internal fun BudgetChoiceChip(
    label: String,
    selected: Boolean,
    width: Float,
    scale: Float,
    onClick: () -> Unit,
) {
    val d = { value: Float -> (value * scale).dp }
    val s = { value: Float -> (value * scale).sp }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(d(width))
            .height(d(40f))
            .clip(RoundedCornerShape(d(20f)))
            .background(if (selected) primaryColor() else cardSurfaceColor())
            .border(d(1f), if (selected) primaryColor() else contentBorderColor(), RoundedCornerShape(d(20f)))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            color = if (selected) primaryContentColor() else secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13.5f),
            lineHeight = s(18f),
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            maxLines = 1,
            softWrap = false,
        )
    }
}

