package com.odyssey.travelplanner.ui.screen.trip.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.TripOverview
import com.odyssey.travelplanner.data.ExchangeRateRepository
import kotlinx.coroutines.launch
import java.util.Locale
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive
import java.time.temporal.ChronoUnit
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.screen.auth.AuthField
import com.odyssey.travelplanner.ui.screen.trip.lodging.AccommodationCalendarDialog
import com.odyssey.travelplanner.ui.screen.tripedit.parseTripDateRange
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondarySurfaceColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun BudgetContent(
    tripId: String,
    overview: TripOverview,
    canEdit: Boolean = true,
    onExpenseAdded: () -> Unit,
    onCurrencyChanged: (String) -> Unit,
) {
    val language = LocalLanguage.current
    val scope = rememberCoroutineScope()
    val expenses = overview.budgetExpenses
    val total = expenses.sumOf { it.amount }
    val categoryStyles = listOf(
        BudgetCategoryStyle("Жильё", "Жильё", Color(0xFF6C5CE7), setOf("жильё", "жилье", "проживание")),
        BudgetCategoryStyle("Транспорт", "Транспорт", Color(0xFFF5A623), setOf("транспорт")),
        BudgetCategoryStyle("Еда и рестораны", "Питание", Color(0xFF22B07D), setOf("еда и рестораны", "питание", "еда")),
        BudgetCategoryStyle("Активности и билеты", "Развлечения", Color(0xFF4AA3F0), setOf("активности и билеты", "развлечения", "активности")),
        BudgetCategoryStyle("Прочее", "Прочее", Color(0xFFEE6C8A), setOf("прочее")),
    )
    val currencyOptions = listOf(
        BudgetCurrencyStyle("RUB", "₽"),
        BudgetCurrencyStyle("EUR", "€"),
        BudgetCurrencyStyle("CZK", "Kč"),
    )
    val storedCurrencyCode = budgetCurrencyCode(overview.budgetCurrency)
    var selectedCurrencyCode by remember(tripId, storedCurrencyCode) { mutableStateOf(storedCurrencyCode) }
    val currencySymbol = currencyOptions.firstOrNull { it.code == selectedCurrencyCode }?.symbol ?: "₽"
    val exchangeRateRepository = remember { ExchangeRateRepository() }
    DisposableEffect(exchangeRateRepository) {
        onDispose { exchangeRateRepository.close() }
    }
    val storedManualRates = overview.budgetManualRates
    var manualRates by remember(tripId, storedManualRates) { mutableStateOf(storedManualRates) }
    var onlineRates by remember(tripId) { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var onlineRateDate by remember(tripId) { mutableStateOf("") }
    var loadingRates by remember(tripId) { mutableStateOf(false) }
    var ratesMessage by remember(tripId) { mutableStateOf<String?>(null) }
    val budgetScrollState = rememberScrollState()
    LaunchedEffect(Unit) { budgetScrollState.scrollTo(0) }

    var adding by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<com.odyssey.travelplanner.data.BudgetExpense?>(null) }
    var editMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Еда и рестораны") }
    var scopeName by remember { mutableStateOf("общий") }
    var paidBy by remember { mutableStateOf("Общее") }
    var date by remember { mutableStateOf("") }
    var datePickerOpen by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var savingCurrency by remember { mutableStateOf(false) }
    var deletingExpenseId by remember { mutableStateOf<String?>(null) }
    var deleteExpense by remember { mutableStateOf<com.odyssey.travelplanner.data.BudgetExpense?>(null) }
    var deleteExpenseError by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var rateEditorCode by remember { mutableStateOf<String?>(null) }
    var rateReferenceCode by remember { mutableStateOf("RUB") }
    var rateInput by remember { mutableStateOf("") }
    var rateError by remember { mutableStateOf<String?>(null) }
    var savingRate by remember { mutableStateOf(false) }

    fun fallbackCurrencyRate(code: String): Double = when (code) {
        "EUR" -> 1.0 / 100.0
        "CZK" -> 1.0 / 4.0
        else -> 1.0
    }

    fun effectiveCurrencyRate(code: String): Double = when {
        code == "RUB" -> 1.0
        manualRates[code]?.takeIf { it > 0.0 } != null -> manualRates.getValue(code)
        onlineRates[code]?.takeIf { it > 0.0 } != null -> onlineRates.getValue(code)
        else -> fallbackCurrencyRate(code)
    }

    val hasReliableCurrencyRate = selectedCurrencyCode == "RUB" ||
        manualRates[selectedCurrencyCode]?.let { it > 0.0 } == true ||
        onlineRates[selectedCurrencyCode]?.let { it > 0.0 } == true
    val missingCurrencyRateMessage = localized(
        "Курс валюты ещё не загружен. Обновите курс или задайте его вручную.",
        "The exchange rate is not loaded yet. Refresh it or set it manually.",
        "El tipo de cambio aún no está cargado. Actualízalo o establécelo manualmente.",
        "Der Wechselkurs ist noch nicht geladen. Aktualisieren Sie ihn oder geben Sie ihn manuell ein.",
    )

    fun rubPerCurrencyUnit(code: String): Double = 1.0 / effectiveCurrencyRate(code)

    fun rateInReferenceCurrency(code: String, referenceCode: String): Double =
        rubPerCurrencyUnit(code) * effectiveCurrencyRate(referenceCode)

    val peopleCount = (overview.budgetGroups.sumOf { it.people }.takeIf { it > 0 } ?: overview.members.size).coerceAtLeast(1)
    val dayCount = budgetTripDayCount(overview.dates)
    val currencyRate = effectiveCurrencyRate(selectedCurrencyCode)

    suspend fun refreshOnlineRates() {
        loadingRates = true
        ratesMessage = null
        runCatching { exchangeRateRepository.loadRubRates(currencyOptions.map { it.code }.toSet()) }
            .onSuccess {
                onlineRates = it.rates
                onlineRateDate = it.date
            }
            .onFailure {
                ratesMessage = localized(language, "Не удалось обновить онлайн-курс", "Could not refresh the online rate", "No se pudo actualizar el tipo online", "Online-Kurs konnte nicht aktualisiert werden")
            }
        loadingRates = false
    }

    LaunchedEffect(tripId) { refreshOnlineRates() }

    fun manualRatesJson(rates: Map<String, Double>) = buildJsonObject {
        rates.toSortedMap().forEach { (code, rate) -> put(code, rate) }
    }

    fun saveManualRate() {
        val code = rateEditorCode ?: return
        val referenceRate = effectiveCurrencyRate(rateReferenceCode)
        val referenceUnitsPerUnit = rateInput.replace(',', '.').toDoubleOrNull()
        val rubPerUnit = referenceUnitsPerUnit?.let { it / referenceRate }
        if (rubPerUnit == null || rubPerUnit <= 0.0) {
            rateError = localized(language, "Введите курс больше нуля", "Enter a rate greater than zero", "Introduce un tipo mayor que cero", "Geben Sie einen Kurs größer als null ein")
            return
        }
        val previousRates = manualRates
        val previousAmountInput = amountInput
        val previousRate = effectiveCurrencyRate(code)
        val updatedRates = manualRates.toMutableMap().apply { put(code, 1.0 / rubPerUnit) }
        manualRates = updatedRates
        if (code == selectedCurrencyCode) {
            previousAmountInput.replace(',', '.').toDoubleOrNull()?.let { enteredAmount ->
                amountInput = formatBudgetInput(enteredAmount / previousRate, effectiveCurrencyRate(code))
            }
        }
        scope.launch {
            savingRate = true
            runCatching {
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateTripSection(
                    tripId,
                    "budgetManualRates",
                    manualRatesJson(updatedRates),
                )
            }.onSuccess {
                rateEditorCode = null
                rateError = null
            }.onFailure {
                manualRates = previousRates
                amountInput = previousAmountInput
                rateError = it.message ?: localized(language, "Не удалось сохранить курс", "Could not save the rate", "No se pudo guardar el tipo", "Kurs konnte nicht gespeichert werden")
            }
            savingRate = false
        }
    }

    fun changeRateReference(referenceCode: String) {
        val code = rateEditorCode ?: return
        if (referenceCode == rateReferenceCode) return
        val previousReferenceCode = rateReferenceCode
        val enteredValue = rateInput.replace(',', '.').toDoubleOrNull()
        rateReferenceCode = referenceCode
        if (enteredValue != null && enteredValue > 0.0) {
            val rubPerUnit = enteredValue / effectiveCurrencyRate(previousReferenceCode)
            rateInput = formatBudgetRateInput(rubPerUnit * effectiveCurrencyRate(referenceCode))
        } else {
            rateInput = formatBudgetRateInput(rateInReferenceCurrency(code, referenceCode))
        }
    }

    fun resetManualRate() {
        val code = rateEditorCode ?: return
        val previousRates = manualRates
        val previousAmountInput = amountInput
        val previousRate = effectiveCurrencyRate(code)
        val updatedRates = manualRates.toMutableMap().apply { remove(code) }
        manualRates = updatedRates
        if (code == selectedCurrencyCode) {
            previousAmountInput.replace(',', '.').toDoubleOrNull()?.let { enteredAmount ->
                amountInput = formatBudgetInput(enteredAmount / previousRate, effectiveCurrencyRate(code))
            }
        }
        scope.launch {
            savingRate = true
            runCatching {
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateTripSection(
                    tripId,
                    "budgetManualRates",
                    manualRatesJson(updatedRates),
                )
            }.onSuccess {
                rateEditorCode = null
                rateError = null
            }.onFailure {
                manualRates = previousRates
                amountInput = previousAmountInput
                rateError = it.message ?: localized(language, "Не удалось сбросить курс", "Could not reset the rate", "No se pudo restablecer el tipo", "Kurs konnte nicht zurückgesetzt werden")
            }
            savingRate = false
        }
    }

    fun closeExpenseSheet() {
        adding = false
        editingExpense = null
        datePickerOpen = false
        message = null
    }

    fun openNewExpense() {
        name = ""
        amountInput = ""
        category = "Еда и рестораны"
        scopeName = "общий"
        paidBy = "Общее"
        date = ""
        message = null
        editingExpense = null
        adding = true
    }

    fun openEditExpense(expense: com.odyssey.travelplanner.data.BudgetExpense) {
        name = expense.name
        amountInput = formatBudgetInput(expense.amount, currencyRate)
        category = categoryStyles.firstOrNull { it.aliases.contains(expense.category.trim().lowercase(java.util.Locale.ROOT)) }?.key ?: "Прочее"
        scopeName = budgetScopeValue(expense.scope)
        paidBy = expense.paidBy.ifBlank { "Общее" }
        date = expense.date
        message = null
        adding = false
        editingExpense = expense
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(budgetScrollState)
            .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 30.dp),
    ) {
        BudgetSummaryCard(total = total, currencySymbol = currencySymbol, conversionRate = currencyRate)
        Spacer(Modifier.height(14.dp))
        BudgetCurrencySelector(
            selectedCode = selectedCurrencyCode,
            options = currencyOptions,
            saving = savingCurrency,
            editable = canEdit,
            onSelect = { selected ->
                if (selected != selectedCurrencyCode && !savingCurrency) {
                    val previousCurrencyCode = selectedCurrencyCode
                    val previousAmountInput = amountInput
                    selectedCurrencyCode = selected
                    previousAmountInput.replace(',', '.').toDoubleOrNull()?.let { enteredAmount ->
                        val baseAmount = enteredAmount / effectiveCurrencyRate(previousCurrencyCode)
                        amountInput = formatBudgetInput(baseAmount, effectiveCurrencyRate(selected))
                    }
                    scope.launch {
                        savingCurrency = true
                        runCatching {
                            SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateTripSection(
                                tripId,
                                "budgetCurrency",
                                JsonPrimitive(selected),
                            )
                        }.onSuccess { onCurrencyChanged(selected) }
                            .onFailure {
                                selectedCurrencyCode = previousCurrencyCode
                                amountInput = previousAmountInput
                                message = it.message ?: localized(language, "Не удалось изменить валюту", "Could not change currency", "No se pudo cambiar la moneda", "Währung konnte nicht geändert werden")
                            }
                        savingCurrency = false
                    }
                }
            },
        )
        Spacer(Modifier.height(10.dp))
        BudgetExchangeRateCard(
            currencyCode = selectedCurrencyCode,
            rubPerUnit = rubPerCurrencyUnit(selectedCurrencyCode),
            isManual = manualRates.containsKey(selectedCurrencyCode),
            loading = loadingRates,
            onlineRateDate = onlineRateDate,
            hasOnlineRate = selectedCurrencyCode == "RUB" || onlineRates.containsKey(selectedCurrencyCode),
            message = ratesMessage,
            onEdit = {
                if (canEdit && selectedCurrencyCode != "RUB") {
                    rateEditorCode = selectedCurrencyCode
                    rateReferenceCode = "RUB"
                    rateInput = formatBudgetRateInput(rubPerCurrencyUnit(selectedCurrencyCode))
                    rateError = null
                }
            },
            onRefresh = { scope.launch { refreshOnlineRates() } },
            editable = canEdit,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(71.dp)) {
            BudgetMetricCard(
                label = localized("НА ЧЕЛОВЕКА", "PER PERSON", "POR PERSONA", "PRO PERSON"),
                value = formatBudgetAmount(if (peopleCount == 0) 0.0 else total / peopleCount, currencySymbol, currencyRate),
                modifier = Modifier.weight(1f),
            )
            BudgetMetricCard(
                label = localized("В ДЕНЬ", "PER DAY", "POR DÍA", "PRO TAG"),
                value = formatBudgetAmount(if (dayCount == 0) 0.0 else total / dayCount, currencySymbol, currencyRate),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = localized("По категориям", "By category", "Por categorías", "Nach Kategorien"),
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.height(22.dp),
        )
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            categoryStyles.forEach { categoryStyle ->
                val categoryTotal = expenses.filter { expense ->
                    categoryStyle.aliases.contains(expense.category.trim().lowercase(java.util.Locale.ROOT))
                }.sumOf { it.amount }
                BudgetCategoryRow(
                    style = categoryStyle,
                    amount = categoryTotal,
                    total = total,
                    currencySymbol = currencySymbol,
                    conversionRate = currencyRate,
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        BudgetExpensesCard(
            expenses = expenses,
            currencySymbol = currencySymbol,
            conversionRate = currencyRate,
            editMode = editMode,
            editable = canEdit,
            deletingExpenseId = deletingExpenseId,
            onToggleEditMode = { editMode = !editMode },
            onAdd = {
                if (hasReliableCurrencyRate) openNewExpense() else ratesMessage = missingCurrencyRateMessage
            },
            onEdit = { expense ->
                if (hasReliableCurrencyRate) openEditExpense(expense) else ratesMessage = missingCurrencyRateMessage
            },
            onDelete = { expense ->
                if (canEdit) {
                    deleteExpense = expense
                    deleteExpenseError = null
                }
            },
        )
    }

    if (canEdit && (adding || editingExpense != null)) {
        ModalBottomSheet(
            onDismissRequest = ::closeExpenseSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = Color(0x730F0F19),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            dragHandle = null,
        ) {
            BudgetExpenseSheet(
                title = if (editingExpense == null) localized("Новая трата", "New expense", "Nuevo gasto", "Neue Ausgabe") else localized("Редактировать трату", "Edit expense", "Editar gasto", "Ausgabe bearbeiten"),
                currencySymbol = currencySymbol,
                name = name,
                amount = amountInput,
                payer = paidBy,
                date = date,
                category = category,
                scopeName = scopeName,
                editing = editingExpense != null,
                saving = saving,
                message = message,
                onNameChange = { name = it },
                onAmountChange = { amountInput = it },
                onPayerChange = { paidBy = it },
                onDateClick = { datePickerOpen = true },
                onCategoryChange = { category = it },
                onScopeChange = { scopeName = it },
                onClose = ::closeExpenseSheet,
                onSave = {
                    if (!hasReliableCurrencyRate) {
                        message = missingCurrencyRateMessage
                    } else {
                        scope.launch {
                            saving = true
                            message = null
                            val value = amountInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                            val baseValue = value / currencyRate
                            val expenseName = name.trim().ifBlank { category }
                            val input = com.odyssey.travelplanner.data.ExpenseInput(
                                name = expenseName,
                                amount = baseValue,
                                category = category,
                                scope = scopeName,
                                paidBy = paidBy,
                                date = date,
                            )
                            runCatching {
                                val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                                editingExpense?.let { expense ->
                                    repository.updateBudgetExpenseDetails(tripId, expense.id, input)
                                } ?: repository.addBudgetExpenseDetails(tripId, input)
                            }.onSuccess {
                                closeExpenseSheet()
                                onExpenseAdded()
                            }.onFailure {
                                message = it.message ?: localized(language, "Не удалось сохранить трату", "Could not save expense", "No se pudo guardar el gasto", "Ausgabe konnte nicht gespeichert werden")
                            }
                            saving = false
                        }
                    }
                },
            )
        }
    }
    if (datePickerOpen) {
        AccommodationCalendarDialog(
            initialValue = date,
            onDismiss = { datePickerOpen = false },
            onConfirm = {
                date = it
                datePickerOpen = false
            },
        )
    }
    if (canEdit) deleteExpense?.let { expense ->
        val deleting = deletingExpenseId == expense.id
        AlertDialog(
            onDismissRequest = {
                if (!deleting) {
                    deleteExpense = null
                    deleteExpenseError = null
                }
            },
            title = {
                Text(
                    localized("Удалить расход?", "Delete expense?", "¿Eliminar gasto?", "Ausgabe löschen?"),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        localized(
                            "Расход «${expense.name}» будет удалён без возможности восстановления.",
                            "Expense “${expense.name}” will be permanently deleted.",
                            "El gasto «${expense.name}» se eliminará de forma permanente.",
                            "Die Ausgabe „${expense.name}“ wird dauerhaft gelöscht.",
                        ),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W600,
                    )
                    deleteExpenseError?.let { error ->
                        Text(error, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteExpense = null
                        deleteExpenseError = null
                    },
                    enabled = !deleting,
                ) {
                    Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            deletingExpenseId = expense.id
                            deleteExpenseError = null
                            runCatching {
                                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteTripItem(tripId, "budgetExpenses", expense.id)
                            }.onSuccess {
                                deleteExpense = null
                                deleteExpenseError = null
                                onExpenseAdded()
                            }.onFailure {
                                deleteExpenseError = it.message ?: localized(language, "Не удалось удалить расход", "Could not delete expense", "No se pudo eliminar el gasto", "Ausgabe konnte nicht gelöscht werden")
                            }
                            deletingExpenseId = null
                        }
                    },
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
    if (canEdit) rateEditorCode?.let { code ->
        AlertDialog(
            onDismissRequest = { if (!savingRate) rateEditorCode = null },
            title = {
                Text(
                    localized("Курс $code", "${code} exchange rate", "Tipo $code", "$code-Wechselkurs"),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        localized("Считать курс в", "Quote the rate in", "Expresar el tipo en", "Kurs angeben in"),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W700,
                        fontSize = 12.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        currencyOptions.filter { it.code != code }.forEach { option ->
                            val selected = option.code == rateReferenceCode
                            Text(
                                "${option.code} ${option.symbol}",
                                color = if (selected) primaryContentColor() else secondaryTextColor(),
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W800,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) primaryColor() else secondarySurfaceColor())
                                    .clickable(enabled = !savingRate) { changeRateReference(option.code) }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                            )
                        }
                    }
                    Text(
                        localized(
                            "Укажите, сколько $rateReferenceCode стоит 1 $code",
                            "Enter how many $rateReferenceCode 1 $code costs",
                            "Indica cuántos $rateReferenceCode cuesta 1 $code",
                            "Geben Sie an, wie viele $rateReferenceCode 1 $code kostet",
                        ),
                        fontFamily = Manrope,
                    )
                    AuthField(
                        label = localized("Курс в $rateReferenceCode", "Rate in $rateReferenceCode", "Tipo en $rateReferenceCode", "Kurs in $rateReferenceCode"),
                        placeholder = "100,00",
                        value = rateInput,
                        onValueChange = { rateInput = it },
                    )
                    rateError?.let {
                        Text(it, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
                    }
                    if (manualRates.containsKey(code)) {
                        TextButton(onClick = ::resetManualRate, enabled = !savingRate) {
                            Text(
                                localized("Сбросить на онлайн-курс", "Use online rate", "Usar tipo online", "Online-Kurs verwenden"),
                                color = primaryColor(),
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W800,
                            )
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { rateEditorCode = null }, enabled = !savingRate) {
                    Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800)
                }
            },
            confirmButton = {
                TextButton(onClick = ::saveManualRate, enabled = !savingRate) {
                    Text(
                        if (savingRate) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…")
                        else localized("Сохранить", "Save", "Guardar", "Speichern"),
                        color = primaryColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                    )
                }
            },
        )
    }
}

internal data class BudgetCategoryStyle(
    val key: String,
    val label: String,
    val color: Color,
    val aliases: Set<String>,
)

internal data class BudgetCurrencyStyle(val code: String, val symbol: String)

internal fun budgetCurrencyCode(value: String): String = when (value.trim().uppercase(java.util.Locale.ROOT)) {
    "RUB", "₽" -> "RUB"
    "EUR", "€" -> "EUR"
    "CZK", "KČ", "Kč" -> "CZK"
    else -> "RUB"
}

@Composable
internal fun localizedBudgetExpenseName(value: String): String {
    val normalized = value.trim().lowercase(Locale.ROOT)
    return when {
        normalized == "жильё" || normalized == "жилье" || normalized == "проживание" -> localized("Жильё", "Lodging", "Alojamiento", "Unterkunft")
        normalized == "аренда машины" -> localized("Аренда машины", "Car rental", "Alquiler de coche", "Mietwagen")
        normalized == "бензин" -> localized("Бензин", "Fuel", "Combustible", "Kraftstoff")
        normalized.startsWith("дневные траты") -> localized(
            "Дневные траты · 28 сентября – 13 октября",
            "Daily expenses · 28 Sep – 13 Oct",
            "Gastos diarios · 28 sep – 13 oct",
            "Tagesausgaben · 28. Sep. – 13. Okt.",
        )
        else -> value
    }
}

internal fun budgetScopeValue(value: String): String = when (value.trim().lowercase(java.util.Locale.ROOT)) {
    "семья", "family" -> "семья"
    "личный", "личное", "personal" -> "личный"
    else -> "общий"
}

internal fun budgetTripDayCount(value: String): Int {
    parseTripDateRange(value)?.let { (start, end) ->
        return (ChronoUnit.DAYS.between(start, end).toInt() + 1).coerceAtLeast(1)
    }
    val match = Regex("""(\d+)\s*(?:дн\w*|day\w*|día\w*|tag\w*)""", RegexOption.IGNORE_CASE).find(value)
    return match?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
}

internal fun formatBudgetRate(value: Double): String {
    val symbols = java.text.DecimalFormatSymbols(java.util.Locale("ru", "RU")).apply {
        groupingSeparator = '\u00A0'
        decimalSeparator = ','
    }
    return java.text.DecimalFormat("#,##0.####", symbols).format(value)
}

internal fun formatBudgetRateInput(value: Double): String =
    java.text.DecimalFormat("0.####", java.text.DecimalFormatSymbols(java.util.Locale.US)).format(value)

internal fun formatBudgetInput(value: Double, conversionRate: Double): String =
    java.text.DecimalFormat("0.##", java.text.DecimalFormatSymbols(java.util.Locale.US)).format(value * conversionRate)

internal fun formatBudgetAmount(value: Double, currencySymbol: String, conversionRate: Double): String {
    val symbols = java.text.DecimalFormatSymbols(java.util.Locale("ru", "RU")).apply {
        groupingSeparator = '\u00A0'
        decimalSeparator = ','
    }
    val displayValue = value * conversionRate
    val pattern = if (displayValue % 1.0 == 0.0) "#,##0" else "#,##0.##"
    val formattedValue = java.text.DecimalFormat(pattern, symbols).format(displayValue)
    return if (budgetCurrencyCode(currencySymbol) == "RUB") "$formattedValue $currencySymbol" else "$currencySymbol $formattedValue"
}

