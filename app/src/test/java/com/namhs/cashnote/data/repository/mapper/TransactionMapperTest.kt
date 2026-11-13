package com.namhs.cashnote.data.repository.mapper

import com.namhs.cashnote.data.local.entity.TransactionEntity
import com.namhs.cashnote.domain.model.ForeignAmount
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionMapperTest {
    // 필드가 뒤바뀌면 잡히도록 모든 값을 서로 다르게 둔다.
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

    @Test
    fun toDomain_mapsAllFields() {
        assertEquals(domain, entity.toDomain())
    }

    @Test
    fun toEntity_mapsAllFields() {
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun domain_roundTrip() {
        assertEquals(domain, domain.toEntity().toDomain())
    }

    @Test
    fun toEntity_keepsZeroIdForNewTransaction() {
        // id 0은 Room autoGenerate에서 새 행으로 처리된다
        val newTransaction = domain.copy(id = 0)

        assertEquals(0L, newTransaction.toEntity().id)
    }

    // 외화 거래

    private val usd = ForeignAmount("USD", BigDecimal("12.50"), BigDecimal("1450.5"))

    @Test
    fun krwTransaction_hasNoForeignColumns() {
        val converted = domain.toEntity()

        assertNull(converted.currencyCode)
        assertNull(converted.foreignAmount)
        assertNull(converted.appliedRate)
    }

    @Test
    fun foreignTransaction_roundTrip() {
        val foreignTransaction = domain.copy(amount = 18_131, foreign = usd)

        val converted = foreignTransaction.toEntity()

        assertEquals("USD", converted.currencyCode)
        assertEquals(BigDecimal("12.50"), converted.foreignAmount)
        assertEquals(BigDecimal("1450.5"), converted.appliedRate)
        assertEquals(foreignTransaction, converted.toDomain())
    }

    @Test
    fun partialForeignColumns_treatedAsKrw() {
        val partial = entity.copy(currencyCode = "USD", foreignAmount = null, appliedRate = null)

        assertNull(partial.toDomain().foreign)
    }
}
