package com.namhs.cashnote.domain.usecase

import app.cash.turbine.test
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.CategoryExpense
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.testing.FakeCategoryRepository
import com.namhs.cashnote.testing.FakeTransactionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetCategoryExpensesUseCaseTest {
    private val food = Category(1, "식비", TransactionType.EXPENSE, "food", 0xFFE57373)
    private val transport = Category(2, "교통", TransactionType.EXPENSE, "transport", 0xFF64B5F6)
    private val salary = Category(3, "급여", TransactionType.INCOME, "salary", 0xFF81C784)

    private val november = YearMonth.of(2026, 11)

    @Test
    fun invoke_calculatesRatioOfEachCategory() = runTest {
        val useCase = useCase(
            transaction(id = 1, category = food, amount = 30_000),
            transaction(id = 2, category = food, amount = 45_000),
            transaction(id = 3, category = transport, amount = 25_000)
        )

        useCase(november).test {
            assertEquals(
                listOf(
                    CategoryExpense(food, amount = 75_000, ratio = 0.75f),
                    CategoryExpense(transport, amount = 25_000, ratio = 0.25f)
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun invoke_ratiosSumToOne() = runTest {
        // 1/3처럼 나누어떨어지지 않는 비율이어도 합은 1에 가깝다
        val useCase = useCase(
            transaction(id = 1, category = food, amount = 10_000),
            transaction(id = 2, category = transport, amount = 20_000)
        )

        useCase(november).test {
            assertEquals(1f, awaitItem().sumOf { it.ratio.toDouble() }.toFloat(), 0.0001f)
        }
    }

    @Test
    fun invoke_sortsByAmountDescending() = runTest {
        val useCase = useCase(
            transaction(id = 1, category = food, amount = 5_000),
            transaction(id = 2, category = transport, amount = 50_000)
        )

        useCase(november).test {
            assertEquals(listOf(transport, food), awaitItem().map { it.category })
        }
    }

    @Test
    fun invoke_excludesIncome() = runTest {
        val useCase = useCase(
            transaction(id = 1, category = food, amount = 10_000),
            transaction(id = 2, category = salary, amount = 3_000_000)
        )

        useCase(november).test {
            assertEquals(listOf(CategoryExpense(food, 10_000, 1f)), awaitItem())
        }
    }

    @Test
    fun invoke_noExpenses_isEmpty() = runTest {
        val useCase = useCase(transaction(id = 1, category = salary, amount = 3_000_000))

        useCase(november).test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun invoke_excludesUnknownCategoryFromItemsAndRatio() = runTest {
        val unknown = food.copy(id = 99)
        val useCase = useCase(
            transaction(id = 1, category = food, amount = 10_000),
            transaction(id = 2, category = unknown, amount = 90_000)
        )

        useCase(november).test {
            // 찾지 못한 카테고리는 빠지고, 남은 항목의 비율로 다시 계산한다
            assertEquals(listOf(CategoryExpense(food, 10_000, 1f)), awaitItem())
        }
    }

    // 카테고리 저장소에는 food, transport, salary만 있다
    private fun useCase(vararg transactions: Transaction) = GetCategoryExpensesUseCase(
        FakeTransactionRepository(transactions.toList()),
        FakeCategoryRepository(listOf(food, transport, salary))
    )

    private fun transaction(id: Long, category: Category, amount: Long) = Transaction(
        id = id,
        amount = amount,
        type = category.type,
        categoryId = category.id,
        date = LocalDate.of(2026, 11, 9),
        createdAt = Instant.parse("2026-11-09T03:00:00Z")
    )
}
