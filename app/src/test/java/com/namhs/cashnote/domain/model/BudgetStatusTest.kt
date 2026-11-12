package com.namhs.cashnote.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetStatusTest {
    @Test
    fun level_below80Percent_isNormal() {
        assertEquals(BudgetLevel.NORMAL, BudgetStatus(budget = 500_000, spent = 399_999).level)
    }

    @Test
    fun level_exactly80Percent_isWarning() {
        assertEquals(BudgetLevel.WARNING, BudgetStatus(budget = 500_000, spent = 400_000).level)
    }

    @Test
    fun level_justBelow100Percent_isWarning() {
        assertEquals(BudgetLevel.WARNING, BudgetStatus(budget = 500_000, spent = 499_999).level)
    }

    @Test
    fun level_exactly100Percent_isExceeded() {
        assertEquals(BudgetLevel.EXCEEDED, BudgetStatus(budget = 500_000, spent = 500_000).level)
    }

    @Test
    fun level_over100Percent_isExceeded() {
        assertEquals(BudgetLevel.EXCEEDED, BudgetStatus(budget = 500_000, spent = 750_000).level)
    }

    @Test
    fun usageRatio_isSpentDividedByBudget() {
        assertEquals(1.5f, BudgetStatus(budget = 500_000, spent = 750_000).usageRatio, 0.0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroBudget_isNotAllowed() {
        BudgetStatus(budget = 0, spent = 10_000)
    }
}
