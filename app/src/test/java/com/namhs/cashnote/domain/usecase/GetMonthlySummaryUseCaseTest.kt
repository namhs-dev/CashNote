package com.namhs.cashnote.domain.usecase

import app.cash.turbine.test
import com.namhs.cashnote.domain.model.MonthlySummary
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.testing.FakeTransactionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetMonthlySummaryUseCaseTest {
    private val november = YearMonth.of(2026, 11)

    @Test
    fun invoke_sumsIncomeAndExpenseOfMonth() = runTest {
        val useCase = useCase(
            transaction(id = 1, amount = 12_500, type = TransactionType.EXPENSE),
            transaction(id = 2, amount = 7_500, type = TransactionType.EXPENSE),
            transaction(id = 3, amount = 3_000_000, type = TransactionType.INCOME)
        )

        useCase(november).test {
            assertEquals(MonthlySummary(income = 3_000_000, expense = 20_000), awaitItem())
        }
    }

    @Test
    fun invoke_excludesOtherMonths() = runTest {
        val useCase = useCase(
            transaction(id = 1, amount = 10_000, date = LocalDate.of(2026, 10, 31)),
            transaction(id = 2, amount = 20_000, date = LocalDate.of(2026, 11, 1)),
            transaction(id = 3, amount = 40_000, date = LocalDate.of(2026, 12, 1))
        )

        useCase(november).test {
            assertEquals(MonthlySummary(expense = 20_000), awaitItem())
        }
    }

    private fun useCase(vararg transactions: Transaction) =
        GetMonthlySummaryUseCase(FakeTransactionRepository(transactions.toList()))

    private fun transaction(
        id: Long,
        amount: Long,
        type: TransactionType = TransactionType.EXPENSE,
        date: LocalDate = LocalDate.of(2026, 11, 9)
    ) = Transaction(
        id = id,
        amount = amount,
        type = type,
        categoryId = 1,
        date = date,
        createdAt = Instant.parse("2026-11-09T03:00:00Z")
    )
}
