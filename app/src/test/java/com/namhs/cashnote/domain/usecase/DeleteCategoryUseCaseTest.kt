package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.CategoryDeleteResult
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.testing.FakeCategoryRepository
import com.namhs.cashnote.testing.FakeTransactionRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteCategoryUseCaseTest {
    private val food = Category(1, "식비", TransactionType.EXPENSE, "food", 0xFFE57373)
    private val travel = Category(2, "여행", TransactionType.EXPENSE, "etc", 0xFF4FC3F7)

    private fun transaction(id: Long, categoryId: Long) = Transaction(
        id = id,
        amount = 10_000,
        type = TransactionType.EXPENSE,
        categoryId = categoryId,
        memo = "",
        date = LocalDate.of(2026, 11, 16),
        createdAt = Instant.parse("2026-11-16T03:00:00Z")
    )

    private val categoryRepository = FakeCategoryRepository(listOf(food, travel))
    private val transactionRepository = FakeTransactionRepository(
        listOf(transaction(1, food.id), transaction(2, food.id))
    )
    private val deleteCategory = DeleteCategoryUseCase(categoryRepository, transactionRepository)

    @Test
    fun unusedCategory_isDeleted() = runTest {
        assertEquals(CategoryDeleteResult.Deleted, deleteCategory(travel))
        assertEquals(listOf(food), categoryRepository.categories.value)
    }

    @Test
    fun usedCategory_isKeptAndReturnsCount() = runTest {
        assertEquals(CategoryDeleteResult.InUse(2), deleteCategory(food))
        assertEquals(listOf(food, travel), categoryRepository.categories.value)
    }
}
