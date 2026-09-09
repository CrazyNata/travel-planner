package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BudgetExpenseCurrencyTest {
    private fun accommodation(price: String) = Accommodation(
        id = "stay-1",
        city = "Рим",
        name = "Hotel Test",
        dates = "12–15 сен",
        price = price,
        status = "бронь",
        details = "",
        photos = emptyList(),
        bookingUrl = "",
    )

    private val expense = BudgetExpense(
        id = "expense-1",
        name = "Билеты",
        amount = 9_784.735812133073,
        category = "Активности и билеты",
        scope = "общий",
        paidBy = "Общее",
        inputCurrency = "EUR",
        inputCurrencyRate = 1.0 / 97.84735812133073,
    )

    @Test
    fun storedRateKeepsAmountStableInInputCurrency() {
        assertEquals(100.0, expense.amountIn("EUR", currentRate = 1.0 / 50.0), absoluteTolerance = 0.000_001)
    }

    @Test
    fun currentRateIsUsedWhenDisplayingAnotherCurrency() {
        assertEquals(
            2_446.183953033268,
            expense.amountIn("CZK", currentRate = 1.0 / 4.0),
            absoluteTolerance = 0.000_001,
        )
    }

    @Test
    fun legacyExpenseWithoutSnapshotKeepsPreviousBehavior() {
        val legacyExpense = expense.copy(inputCurrency = "", inputCurrencyRate = null)

        assertEquals(
            195.69471624266146,
            legacyExpense.amountIn("EUR", currentRate = 1.0 / 50.0),
            absoluteTolerance = 0.000_001,
        )
    }

    @Test
    fun accommodationPriceBecomesAutomaticBudgetExpense() {
        val automatic = automaticAccommodationBudgetExpense(accommodation("€434")) { code ->
            when (code) {
                "EUR" -> 1.0 / 100.0
                else -> 1.0
            }
        }

        requireNotNull(automatic)
        assertEquals("accommodation:stay-1", automatic.id)
        assertEquals("Жильё", automatic.category)
        assertEquals(434.0, automatic.amountIn("EUR", 1.0 / 50.0), absoluteTolerance = 0.000_001)
        assertEquals(true, isAutomaticBudgetExpense(automatic))
    }

    @Test
    fun unmarkedAccommodationPriceDefaultsToEuro() {
        val automatic = automaticAccommodationBudgetExpense(accommodation("120")) { code ->
            if (code == "EUR") 1.0 / 100.0 else 1.0
        }

        requireNotNull(automatic)
        assertEquals("EUR", automatic.inputCurrency)
        assertEquals(120.0, automatic.amountIn("EUR", 1.0 / 50.0), absoluteTolerance = 0.000_001)
    }

    @Test
    fun unsupportedAccommodationCurrencyIsNotAddedToBudget() {
        assertNull(
            automaticAccommodationBudgetExpense(accommodation("$500")) { 1.0 },
        )
        assertNull(
            automaticAccommodationBudgetExpense(accommodation("500 USD")) { 1.0 },
        )
    }
}
