package com.namhs.cashnote.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.namhs.cashnote.data.local.database.CashNoteDatabase
import com.namhs.cashnote.data.local.database.DefaultCategoryCallback
import com.namhs.cashnote.data.local.entity.TransactionEntity
import com.namhs.cashnote.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var database: CashNoteDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        // 실제 앱과 같이 기본 카테고리 콜백을 붙여, 카테고리 1~10이 있는 상태에서 테스트한다.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, CashNoteDatabase::class.java)
            .addCallback(DefaultCategoryCallback())
            .build()
        dao = database.transactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // 월별 조회 경계값

    @Test
    fun observeByDateRange_includesFirstAndLastDayOfMonthOnly() = runBlocking {
        listOf(
            LocalDate.of(2026, 10, 31),
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30),
            LocalDate.of(2026, 12, 1)
        ).forEach { dao.insert(transaction(date = it)) }

        val result = dao.observeByDateRange(
            start = LocalDate.of(2026, 11, 1),
            endExclusive = LocalDate.of(2026, 12, 1)
        ).first()

        assertEquals(
            listOf(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1)),
            result.map { it.date }
        )
    }

    @Test
    fun observeByDateRange_emptyWhenNoTransactionInRange() = runBlocking {
        dao.insert(transaction(date = LocalDate.of(2026, 10, 31)))

        val result = dao.observeByDateRange(
            start = LocalDate.of(2026, 11, 1),
            endExclusive = LocalDate.of(2026, 12, 1)
        ).first()

        assertTrue(result.isEmpty())
    }

    // 정렬

    @Test
    fun observeByDateRange_sortsByDateThenCreatedAtThenIdDescending() = runBlocking {
        val day = LocalDate.of(2026, 11, 9)
        val earlier = Instant.parse("2026-11-09T01:00:00Z")
        val later = Instant.parse("2026-11-09T02:00:00Z")
        val previousDayId = dao.insert(transaction(date = day.minusDays(1), createdAt = later))
        val earlierId = dao.insert(transaction(date = day, createdAt = earlier))
        val sameTimeFirstId = dao.insert(transaction(date = day, createdAt = later))
        val sameTimeSecondId = dao.insert(transaction(date = day, createdAt = later))

        val result = dao.observeByDateRange(
            start = LocalDate.of(2026, 11, 1),
            endExclusive = LocalDate.of(2026, 12, 1)
        ).first()

        assertEquals(
            listOf(sameTimeSecondId, sameTimeFirstId, earlierId, previousDayId),
            result.map { it.id }
        )
    }

    // 추가·수정·삭제

    @Test
    fun insert_returnsGeneratedIdAndStoresAllFields() = runBlocking {
        val original = transaction(date = LocalDate.of(2026, 11, 9))

        val id = dao.insert(original)

        assertEquals(listOf(original.copy(id = id)), observeNovember())
    }

    @Test
    fun update_changesStoredTransaction() = runBlocking {
        val id = dao.insert(transaction(date = LocalDate.of(2026, 11, 9)))
        val updated = transaction(date = LocalDate.of(2026, 11, 10)).copy(
            id = id,
            amount = 30_000,
            memo = "수정"
        )

        dao.update(updated)

        assertEquals(listOf(updated), observeNovember())
    }

    @Test
    fun delete_removesTransaction() = runBlocking {
        val id = dao.insert(transaction(date = LocalDate.of(2026, 11, 9)))

        dao.delete(transaction(date = LocalDate.of(2026, 11, 9)).copy(id = id))

        assertTrue(observeNovember().isEmpty())
    }

    // 유형별 합계

    @Test
    fun observeTotalsByType_sumsAmountsPerType() = runBlocking {
        val day = LocalDate.of(2026, 11, 9)
        dao.insert(transaction(date = day, amount = 12_500))
        dao.insert(transaction(date = day, amount = 7_500))
        dao.insert(transaction(date = day, amount = 3_000_000, type = TransactionType.INCOME))

        val result = observeNovemberTotals()

        // GROUP BY 결과 순서는 보장되지 않으므로 Set으로 비교한다
        assertEquals(
            setOf(
                TypeTotal(TransactionType.EXPENSE, 20_000),
                TypeTotal(TransactionType.INCOME, 3_000_000)
            ),
            result.toSet()
        )
    }

    @Test
    fun observeTotalsByType_includesFirstAndLastDayOfMonthOnly() = runBlocking {
        listOf(
            LocalDate.of(2026, 10, 31) to 1L,
            LocalDate.of(2026, 11, 1) to 10L,
            LocalDate.of(2026, 11, 30) to 100L,
            LocalDate.of(2026, 12, 1) to 1_000L
        ).forEach { (date, amount) -> dao.insert(transaction(date = date, amount = amount)) }

        assertEquals(listOf(TypeTotal(TransactionType.EXPENSE, 110)), observeNovemberTotals())
    }

    @Test
    fun observeTotalsByType_emptyWhenNoTransactionInRange() = runBlocking {
        dao.insert(transaction(date = LocalDate.of(2026, 10, 31)))

        assertTrue(observeNovemberTotals().isEmpty())
    }

    // 외래 키

    @Test(expected = SQLiteConstraintException::class)
    fun insert_withUnknownCategory_fails() {
        runBlocking {
            dao.insert(transaction(date = LocalDate.of(2026, 11, 9), categoryId = 999))
        }
    }

    private suspend fun observeNovember(): List<TransactionEntity> = dao.observeByDateRange(
        start = LocalDate.of(2026, 11, 1),
        endExclusive = LocalDate.of(2026, 12, 1)
    ).first()

    private suspend fun observeNovemberTotals(): List<TypeTotal> = dao.observeTotalsByType(
        start = LocalDate.of(2026, 11, 1),
        endExclusive = LocalDate.of(2026, 12, 1)
    ).first()

    private fun transaction(
        date: LocalDate,
        createdAt: Instant = Instant.parse("2026-11-09T03:00:00Z"),
        categoryId: Long = 1,
        amount: Long = 12_500,
        type: TransactionType = TransactionType.EXPENSE
    ) = TransactionEntity(
        amount = amount,
        type = type,
        categoryId = categoryId,
        memo = "점심",
        date = date,
        createdAt = createdAt
    )
}
