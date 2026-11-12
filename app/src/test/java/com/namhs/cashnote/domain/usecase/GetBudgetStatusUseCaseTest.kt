package com.namhs.cashnote.domain.usecase

import app.cash.turbine.test
import com.namhs.cashnote.domain.model.BudgetStatus
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.testing.FakeBudgetRepository
import com.namhs.cashnote.testing.FakeTransactionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetBudgetStatusUseCaseTest {
    private val october = YearMonth.of(2026, 10)
    private val november = YearMonth.of(2026, 11)
    private val december = YearMonth.of(2026, 12)

    private val transactionRepository = FakeTransactionRepository(
        listOf(
            transaction(id = 1, amount = 300_000, type = TransactionType.EXPENSE),
            transaction(id = 2, amount = 3_000_000, type = TransactionType.INCOME)
        )
    )

    @Test
    fun invoke_noBudget_isNull() = runTest {
        useCase(FakeBudgetRepository())(november).test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun invoke_comparesBudgetWithExpenseOnly() = runTest {
        useCase(FakeBudgetRepository(mapOf(november to 500_000L)))(november).test {
            // 수입은 사용액에 넣지 않는다
            assertEquals(BudgetStatus(budget = 500_000, spent = 300_000), awaitItem())
        }
    }

    @Test
    fun invoke_carriesOverBudgetFromEarlierMonth() = runTest {
        useCase(FakeBudgetRepository(mapOf(october to 400_000L)))(november).test {
            assertEquals(BudgetStatus(budget = 400_000, spent = 300_000), awaitItem())
        }
    }

    @Test
    fun invoke_budgetRemovedFromMonth_isNull() = runTest {
        val budgets = FakeBudgetRepository(mapOf(october to 400_000L, december to 0L))

        useCase(budgets)(december).test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun invoke_updatesWhenBudgetChanges() = runTest {
        val budgets = FakeBudgetRepository()

        useCase(budgets)(november).test {
            assertNull(awaitItem())

            budgets.setBudget(november, 500_000)

            assertEquals(BudgetStatus(budget = 500_000, spent = 300_000), awaitItem())
        }
    }

    private fun useCase(budgetRepository: FakeBudgetRepository) =
        GetBudgetStatusUseCase(budgetRepository, transactionRepository)

    private fun transaction(id: Long, amount: Long, type: TransactionType) = Transaction(
        id = id,
        amount = amount,
        type = type,
        categoryId = 1,
        date = LocalDate.of(2026, 11, 9),
        createdAt = Instant.parse("2026-11-09T03:00:00Z")
    )
}
