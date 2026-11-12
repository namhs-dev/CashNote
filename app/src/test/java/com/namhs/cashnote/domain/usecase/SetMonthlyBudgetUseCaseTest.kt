package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.testing.FakeBudgetRepository
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetMonthlyBudgetUseCaseTest {
    private val repository = FakeBudgetRepository()
    private val useCase = SetMonthlyBudgetUseCase(repository)

    private val november = YearMonth.of(2026, 11)

    @Test
    fun invoke_savesBudgetFromMonth() = runTest {
        useCase(november, 500_000)

        assertEquals(listOf(november to 500_000L), repository.saved)
    }

    @Test
    fun invoke_zero_savesToRemoveBudget() = runTest {
        useCase(november, 0)

        assertEquals(listOf(november to 0L), repository.saved)
    }

    @Test
    fun invoke_negative_throwsAndDoesNotSave() = runTest {
        val result = runCatching { useCase(november, -1) }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(repository.saved.isEmpty())
    }
}
