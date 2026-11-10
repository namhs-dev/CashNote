package com.namhs.cashnote.data.repository

import com.namhs.cashnote.data.local.dao.TransactionDao
import com.namhs.cashnote.data.local.entity.TransactionEntity
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionRepositoryImplTest {
    private val dao = FakeTransactionDao()
    private val repository = TransactionRepositoryImpl(dao)

    private val entity = TransactionEntity(
        id = 7,
        amount = 12_500,
        type = TransactionType.EXPENSE,
        categoryId = 3,
        memo = "점심",
        date = LocalDate.of(2026, 11, 9),
        createdAt = Instant.parse("2026-11-09T03:15:30.123Z")
    )

    private val domain = Transaction(
        id = 7,
        amount = 12_500,
        type = TransactionType.EXPENSE,
        categoryId = 3,
        memo = "점심",
        date = LocalDate.of(2026, 11, 9),
        createdAt = Instant.parse("2026-11-09T03:15:30.123Z")
    )

    // 월 → 날짜 범위

    @Test
    fun observeTransactions_queriesFirstDayToNextMonthFirstDay() {
        repository.observeTransactions(YearMonth.of(2026, 11))

        assertEquals(LocalDate.of(2026, 11, 1) to LocalDate.of(2026, 12, 1), dao.lastRange)
    }

    @Test
    fun observeTransactions_december_endsAtNextYearJanuary() {
        repository.observeTransactions(YearMonth.of(2026, 12))

        assertEquals(LocalDate.of(2026, 12, 1) to LocalDate.of(2027, 1, 1), dao.lastRange)
    }

    @Test
    fun observeTransactions_leapFebruary_includesFebruary29() {
        repository.observeTransactions(YearMonth.of(2028, 2))

        // 끝이 3월 1일(미포함)이므로 2월 29일까지 포함된다
        assertEquals(LocalDate.of(2028, 2, 1) to LocalDate.of(2028, 3, 1), dao.lastRange)
    }

    // 변환

    @Test
    fun observeTransactions_mapsEntitiesToDomain() = runBlocking {
        dao.entities = listOf(entity)

        val result = repository.observeTransactions(YearMonth.of(2026, 11)).first()

        assertEquals(listOf(domain), result)
    }

    @Test
    fun getTransaction_mapsEntityToDomain() = runBlocking {
        dao.entities = listOf(entity)

        assertEquals(domain, repository.getTransaction(7))
    }

    @Test
    fun getTransaction_returnsNullWhenMissing() = runBlocking {
        dao.entities = listOf(entity)

        assertNull(repository.getTransaction(99))
    }

    @Test
    fun add_insertsEntityAndReturnsId() = runBlocking {
        dao.nextId = 42

        val id = repository.add(domain.copy(id = 0))

        assertEquals(42L, id)
        assertEquals(listOf(entity.copy(id = 0)), dao.inserted)
    }

    @Test
    fun update_passesEntityToDao() = runBlocking {
        repository.update(domain)

        assertEquals(listOf(entity), dao.updated)
    }

    @Test
    fun delete_passesEntityToDao() = runBlocking {
        repository.delete(domain)

        assertEquals(listOf(entity), dao.deleted)
    }
}

/** 호출 인자를 기록하는 가짜 DAO */
private class FakeTransactionDao : TransactionDao {
    var lastRange: Pair<LocalDate, LocalDate>? = null
    var entities: List<TransactionEntity> = emptyList()
    var nextId: Long = 1
    val inserted = mutableListOf<TransactionEntity>()
    val updated = mutableListOf<TransactionEntity>()
    val deleted = mutableListOf<TransactionEntity>()

    override fun observeByDateRange(
        start: LocalDate,
        endExclusive: LocalDate
    ): Flow<List<TransactionEntity>> {
        lastRange = start to endExclusive
        return flowOf(entities)
    }

    override suspend fun getById(id: Long): TransactionEntity? = entities.find { it.id == id }

    override suspend fun insert(transaction: TransactionEntity): Long {
        inserted += transaction
        return nextId
    }

    override suspend fun update(transaction: TransactionEntity) {
        updated += transaction
    }

    override suspend fun delete(transaction: TransactionEntity) {
        deleted += transaction
    }
}
