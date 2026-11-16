package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.testing.FakeCategoryRepository
import com.namhs.cashnote.testing.FakeExportFileRepository
import com.namhs.cashnote.testing.FakeTransactionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareMonthlyCsvUseCaseTest {
    private val food = Category(1, "식비", TransactionType.EXPENSE, "food", 0xFFE57373)
    private val november = YearMonth.of(2026, 11)

    private val lunch = Transaction(
        id = 1,
        amount = 12_000,
        type = TransactionType.EXPENSE,
        categoryId = food.id,
        memo = "점심",
        date = LocalDate.of(2026, 11, 3),
        createdAt = Instant.parse("2026-11-03T03:00:00Z")
    )

    private val exportFileRepository = FakeExportFileRepository()

    private fun useCase(vararg transactions: Transaction) = ShareMonthlyCsvUseCase(
        ExportMonthlyCsvUseCase(
            FakeTransactionRepository(transactions.toList()),
            FakeCategoryRepository(listOf(food))
        ),
        exportFileRepository
    )

    @Test
    fun monthWithTransactions_savesFileAndReturnsUri() = runTest {
        val uri = useCase(lunch)(november)

        assertEquals("content://test/cashnote-2026-11.csv", uri)
        val saved = exportFileRepository.saved.single()
        assertEquals(1, saved.rowCount)
        assertTrue(saved.content.contains("2026-11-03,지출,식비,12000,KRW,,,점심"))
    }

    @Test
    fun emptyMonth_returnsNullWithoutSaving() = runTest {
        assertNull(useCase()(november))
        assertTrue(exportFileRepository.saved.isEmpty())
    }
}
