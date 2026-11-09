package com.namhs.cashnote.data.repository.mapper

import com.namhs.cashnote.data.local.entity.TransactionEntity
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
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
}
