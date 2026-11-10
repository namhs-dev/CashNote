package com.namhs.cashnote.domain.usecase

import app.cash.turbine.test
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.model.TransactionWithCategory
import com.namhs.cashnote.testing.FakeCategoryRepository
import com.namhs.cashnote.testing.FakeTransactionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetMonthlyTransactionsUseCaseTest {
    private val food = Category(1, "식비", TransactionType.EXPENSE, "food", 0xFFE57373)
    private val salary = Category(2, "급여", TransactionType.INCOME, "salary", 0xFF64B5F6)

    private val lunch = transaction(id = 1, categoryId = food.id, date = LocalDate.of(2026, 11, 9))
    private val pay = transaction(id = 2, categoryId = salary.id, date = LocalDate.of(2026, 11, 10))

    private val november = YearMonth.of(2026, 11)

    @Test
    fun invoke_combinesTransactionsWithCategory() = runTest {
        val useCase = useCase(listOf(lunch, pay), listOf(food, salary))

        useCase(november).test {
            // 날짜 최신순
            assertEquals(
                listOf(TransactionWithCategory(pay, salary), TransactionWithCategory(lunch, food)),
                awaitItem()
            )
        }
    }

    @Test
    fun invoke_onlyIncludesRequestedMonth() = runTest {
        val october = transaction(id = 3, categoryId = food.id, date = LocalDate.of(2026, 10, 31))
        val useCase = useCase(listOf(lunch, october), listOf(food))

        useCase(november).test {
            assertEquals(listOf(TransactionWithCategory(lunch, food)), awaitItem())
        }
    }

    @Test
    fun invoke_skipsTransactionWithoutCategory() = runTest {
        // salary 카테고리가 아직 없다
        val useCase = useCase(listOf(lunch, pay), listOf(food))

        useCase(november).test {
            assertEquals(listOf(TransactionWithCategory(lunch, food)), awaitItem())
        }
    }

    @Test
    fun invoke_emitsAgainWhenCategoryChanges() = runTest {
        val categoryRepository = FakeCategoryRepository(listOf(food))
        val useCase = GetMonthlyTransactionsUseCase(
            FakeTransactionRepository(listOf(lunch)),
            categoryRepository
        )

        useCase(november).test {
            assertEquals(listOf(TransactionWithCategory(lunch, food)), awaitItem())

            val renamed = food.copy(name = "외식")
            categoryRepository.categories.value = listOf(renamed)

            assertEquals(listOf(TransactionWithCategory(lunch, renamed)), awaitItem())
        }
    }

    @Test
    fun invoke_emitsAgainWhenTransactionAdded() = runTest {
        val transactionRepository = FakeTransactionRepository(listOf(lunch))
        val useCase = GetMonthlyTransactionsUseCase(
            transactionRepository,
            FakeCategoryRepository(listOf(food, salary))
        )

        useCase(november).test {
            assertEquals(1, awaitItem().size)

            transactionRepository.add(pay)

            assertEquals(
                listOf(TransactionWithCategory(pay, salary), TransactionWithCategory(lunch, food)),
                awaitItem()
            )
        }
    }

    private fun useCase(transactions: List<Transaction>, categories: List<Category>) =
        GetMonthlyTransactionsUseCase(
            FakeTransactionRepository(transactions),
            FakeCategoryRepository(categories)
        )

    private fun transaction(id: Long, categoryId: Long, date: LocalDate) = Transaction(
        id = id,
        amount = 10_000,
        type = TransactionType.EXPENSE,
        categoryId = categoryId,
        date = date,
        createdAt = Instant.parse("2026-11-01T00:00:00Z")
    )
}
