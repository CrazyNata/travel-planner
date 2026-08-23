package com.odyssey.travelplanner.data

import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetExpenseCurrencyTest {
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
}
