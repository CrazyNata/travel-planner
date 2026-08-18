package com.odyssey.travelplanner.ui.screen.trip.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import kotlinx.coroutines.launch
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.screen.auth.AuthField
import com.odyssey.travelplanner.ui.screen.trip.lodging.AccommodationEditDateField
import com.odyssey.travelplanner.ui.screen.trip.lodging.AccommodationEditTextField
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.dangerSurfaceColor
import com.odyssey.travelplanner.ui.theme.labelColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondarySurfaceColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.surfaceVariantColor

@Composable
internal fun BudgetExpenseSheet(
    title: String,
    currencySymbol: String,
    name: String,
    amount: String,
    payer: String,
    date: String,
    category: String,
    scopeName: String,
    editing: Boolean,
    saving: Boolean,
    message: String?,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onPayerChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onCategoryChange: (String) -> Unit,
    onScopeChange: (String) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = maxWidth.value / 368f
        fun d(value: Float) = (value * scale).dp
        fun s(value: Float) = (value * scale).sp
        val labelStyle = androidx.compose.ui.text.TextStyle(
            color = labelColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13f),
            lineHeight = s(18f),
            platformStyle = OdysseyNoFontPadding,
        )
        Box(modifier = Modifier.fillMaxWidth().height(d(700f))) {
            Box(
                modifier = Modifier
                    .offset(x = d(164f), y = d(12f))
                    .size(d(40f), d(4f))
                    .clip(RoundedCornerShape(d(2f)))
                    .background(contentBorderColor()),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.offset(x = d(16f), y = d(30f)).width(d(336f)).height(d(34f)),
            ) {
                Text(
                    text = title,
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = s(24f),
                    lineHeight = s(33f),
                    letterSpacing = s(-0.24f),
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    maxLines = 1,
                    softWrap = false,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(d(34f)).clip(CircleShape).background(surfaceVariantColor()).clickable(onClick = onClose),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = secondaryTextColor(), modifier = Modifier.size(d(16f)))
                }
            }
            AccommodationEditTextField(
                label = localized("Название", "Name", "Nombre", "Name"),
                value = name,
                placeholder = localized("Например, билеты", "E.g. tickets", "P. ej. billetes", "Z. B. Tickets"),
                valueWeight = FontWeight.W600,
                valueColor = contentTextColor(),
                scale = scale,
                modifier = Modifier.offset(x = d(16f), y = d(78f)).width(d(336f)),
                onValueChange = onNameChange,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(12f)),
                modifier = Modifier.offset(x = d(16f), y = d(165f)).width(d(336f)).height(d(77f)),
            ) {
                AccommodationEditTextField(
                    label = localized("Сумма, $currencySymbol", "Amount, $currencySymbol", "Importe, $currencySymbol", "Betrag, $currencySymbol"),
                    value = amount,
                    placeholder = "0",
                    valueWeight = FontWeight.W800,
                    valueColor = contentTextColor(),
                    scale = scale,
                    modifier = Modifier.width(d(162f)),
                    onValueChange = onAmountChange,
                )
                AccommodationEditTextField(
                    label = localized("Кто платил", "Paid by", "Quién pagó", "Bezahlt von"),
                    value = payer,
                    placeholder = localized("Общее", "Shared", "Común", "Gemeinsam"),
                    valueWeight = FontWeight.W600,
                    valueColor = contentTextColor(),
                    scale = scale,
                    modifier = Modifier.width(d(162f)),
                    onValueChange = onPayerChange,
                )
            }
            AccommodationEditDateField(
                label = localized("Дата", "Date", "Fecha", "Datum"),
                value = date,
                scale = scale,
                modifier = Modifier.offset(x = d(16f), y = d(258f)).width(d(336f)),
                onClick = onDateClick,
            )
            Text(
                text = localized("Категория", "Category", "Categoría", "Kategorie"),
                style = labelStyle,
                modifier = Modifier.offset(x = d(16f), y = d(351f)).width(d(336f)).height(d(18f)),
            )
            Column(modifier = Modifier.offset(x = d(16f), y = d(377f)).width(d(336f))) {
                Row(horizontalArrangement = Arrangement.spacedBy(d(9f))) {
                    BudgetChoiceChip(localized("Жильё", "Lodging", "Alojamiento", "Unterkunft"), category == "Жильё", 79.2f, scale) { onCategoryChange("Жильё") }
                    BudgetChoiceChip(localized("Транспорт", "Transport", "Transporte", "Transport"), category == "Транспорт", 106.8f, scale) { onCategoryChange("Транспорт") }
                }
                Spacer(Modifier.height(d(9f)))
                Row(horizontalArrangement = Arrangement.spacedBy(d(9f))) {
                    BudgetChoiceChip(localized("Еда и рестораны", "Food & restaurants", "Comida y restaurantes", "Essen & Restaurants"), category == "Еда и рестораны", 147.6f, scale) { onCategoryChange("Еда и рестораны") }
                    BudgetChoiceChip(localized("Активности и билеты", "Activities & tickets", "Actividades y entradas", "Aktivitäten & Tickets"), category == "Активности и билеты", 178.7f, scale) { onCategoryChange("Активности и билеты") }
                }
                Spacer(Modifier.height(d(9f)))
                BudgetChoiceChip(localized("Прочее", "Other", "Otros", "Sonstiges"), category == "Прочее", 85.1f, scale) { onCategoryChange("Прочее") }
            }
            Text(
                text = localized("Тип бюджета", "Budget type", "Tipo de presupuesto", "Budgettyp"),
                style = labelStyle,
                modifier = Modifier.offset(x = d(16f), y = d(531f)).width(d(336f)).height(d(18f)),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(9f)),
                modifier = Modifier.offset(x = d(16f), y = d(557f)).height(d(40f)),
            ) {
                BudgetChoiceChip(localized("Общий", "Shared", "Común", "Gemeinsam"), scopeName == "общий", 79.8f, scale) { onScopeChange("общий") }
                BudgetChoiceChip(localized("Семья", "Family", "Familia", "Familie"), scopeName == "семья", 77.9f, scale) { onScopeChange("семья") }
                BudgetChoiceChip(localized("Личный", "Personal", "Personal", "Privat"), scopeName == "личный", 87.4f, scale) { onScopeChange("личный") }
            }
            message?.let {
                Text(
                    text = it,
                    color = Color(0xFFE0524B),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = s(11f),
                    lineHeight = s(15f),
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    modifier = Modifier.offset(x = d(16f), y = d(591f)).width(d(336f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(11f)),
                modifier = Modifier.offset(x = d(16f), y = d(613f)).width(d(336f)).height(d(53f)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(d(141.578f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(d(15f)))
                        .background(cardSurfaceColor())
                        .border(d(1f), contentBorderColor(), RoundedCornerShape(d(15f)))
                        .clickable(onClick = onClose),
                ) {
                    Text(
                        text = localized("Отмена", "Cancel", "Cancelar", "Abbrechen"),
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(15f),
                        lineHeight = s(20f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(d(183.422f))
                        .fillMaxHeight()
                        .shadow(d(8f), RoundedCornerShape(d(15f)), clip = false, ambientColor = Color(0x4D6C5CE7), spotColor = Color(0x4D6C5CE7))
                        .clip(RoundedCornerShape(d(15f)))
                        .background(Brush.linearGradient(listOf(primaryColor(), Color(0xFF7D6CF0))))
                        .clickable(enabled = !saving, onClick = onSave),
                ) {
                    Text(
                        text = if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else if (editing) localized("Сохранить", "Save", "Guardar", "Speichern") else localized("Добавить", "Add", "Añadir", "Hinzufügen"),
                        color = primaryContentColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(15f),
                        lineHeight = s(20f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
            }
        }
    }
}

@Composable
internal fun EditExpensePanel(expense: com.odyssey.travelplanner.data.BudgetExpense, tripId: String, onClose: () -> Unit, onDeleted: () -> Unit, onSaved: () -> Unit) {
    val language = LocalLanguage.current
    var name by remember(expense.id) { mutableStateOf(expense.name) }
    var amount by remember(expense.id) { mutableStateOf(expense.amount.toString()) }
    var category by remember(expense.id) { mutableStateOf(expense.category) }
    var scopeName by remember(expense.id) { mutableStateOf(expense.scope) }
    var paidBy by remember(expense.id) { mutableStateOf(expense.paidBy) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cardSurfaceColor()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(localized("Редактировать трату", "Edit expense", "Editar gasto", "Ausgabe bearbeiten"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
        AuthField(localized("Название", "Name", "Nombre", "Name"), localized("Название", "Name", "Nombre", "Name"), name) { name = it }
        AuthField(localized("Сумма", "Amount", "Importe", "Betrag"), "0", amount) { amount = it }
        AuthField(localized("Категория", "Category", "Categoría", "Kategorie"), localized("Категория", "Category", "Categoría", "Kategorie"), category) { category = it }
        AuthField(localized("Кто оплатил", "Paid by", "Pagado por", "Bezahlt von"), localized("Имя", "Name", "Nombre", "Name"), paidBy) { paidBy = it }
        AuthField(localized("Тип бюджета", "Budget type", "Tipo de presupuesto", "Budgettyp"), localized("общий / семья / личный", "shared / family / personal", "compartido / familiar / personal", "gemeinsam / Familie / privat"), scopeName) { scopeName = it }
        if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(11.dp)).background(dangerSurfaceColor()).clickable {
                scope.launch {
                    saving = true
                    runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteTripItem(tripId, "budgetExpenses", expense.id) }
                        .onSuccess { onDeleted() }
                        .onFailure { message = it.message ?: localized(language, "Не удалось удалить трату", "Could not delete expense", "No se pudo eliminar el gasto", "Ausgabe konnte nicht gelöscht werden") }
                    saving = false
                }
            }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Delete, contentDescription = localized("Удалить", "Delete", "Eliminar", "Löschen"), tint = Color(0xFFFF6B65), modifier = Modifier.size(19.dp))
            }
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = secondarySurfaceColor(), contentColor = contentTextColor()), shape = RoundedCornerShape(11.dp)) { Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
            Button(onClick = {
                scope.launch {
                    saving = true
                    val value = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
                    runCatching {
                        SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateBudgetExpenseDetails(
                            tripId,
                            expense.id,
                            com.odyssey.travelplanner.data.ExpenseInput(
                                name = name,
                                amount = value,
                                category = category,
                                scope = scopeName,
                                paidBy = paidBy,
                            ),
                        )
                    }
                        .onSuccess { onSaved() }
                        .onFailure { message = it.message ?: localized(language, "Не удалось сохранить трату", "Could not save expense", "No se pudo guardar el gasto", "Ausgabe konnte nicht gespeichert werden") }
                    saving = false
                }
            }, enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()), shape = RoundedCornerShape(11.dp)) { Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
        }
    }
}

