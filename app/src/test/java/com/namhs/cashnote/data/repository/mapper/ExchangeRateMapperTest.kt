package com.namhs.cashnote.data.repository.mapper

import com.namhs.cashnote.data.local.entity.ExchangeRateEntity
import com.namhs.cashnote.data.remote.dto.ExchangeRateDto
import com.namhs.cashnote.domain.model.ExchangeRate
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExchangeRateMapperTest {
    private val friday = LocalDate.of(2026, 11, 13)
    private val updatedAt = Instant.parse("2026-11-13T03:00:00Z")

    private fun dto(unit: String?, rate: String?, name: String? = "미국 달러") =
        ExchangeRateDto(result = 1, currencyUnit = unit, currencyName = name, dealBaseRate = rate)

    @Test
    fun toEntity_plainCurrency() {
        val entity = dto("USD", "1,450.5").toEntityOrNull(friday, updatedAt)

        val expected = ExchangeRateEntity(
            currencyCode = "USD",
            currencyName = "미국 달러",
            unit = 1,
            dealBaseRate = BigDecimal("1450.5"),
            rateDate = friday,
            updatedAt = updatedAt
        )
        assertEquals(expected, entity)
    }

    @Test
    fun toEntity_unitInParentheses() {
        val entity = dto("JPY(100)", "935.12", name = "일본 옌").toEntityOrNull(friday, updatedAt)

        assertEquals("JPY", entity?.currencyCode)
        assertEquals(100, entity?.unit)
        assertEquals(BigDecimal("935.12"), entity?.dealBaseRate)
    }

    @Test
    fun toEntity_trimsCurrencyUnit() {
        assertEquals("USD", dto(" USD ", "1450.5").toEntityOrNull(friday, updatedAt)?.currencyCode)
    }

    @Test
    fun toEntity_missingName_usesCode() {
        listOf(null, " ").forEach { name ->
            val entity = dto("USD", "1450.5", name = name).toEntityOrNull(friday, updatedAt)

            assertEquals("USD", entity?.currencyName)
        }
    }

    @Test
    fun toEntity_invalidCurrencyUnit_isNull() {
        assertNull(dto(null, "1450.5").toEntityOrNull(friday, updatedAt))
        assertNull(dto("", "1450.5").toEntityOrNull(friday, updatedAt))
        assertNull(dto("JPY(abc)", "935.12").toEntityOrNull(friday, updatedAt))
    }

    @Test
    fun toEntity_invalidRate_isNull() {
        assertNull(dto("USD", null).toEntityOrNull(friday, updatedAt))
        assertNull(dto("USD", "-").toEntityOrNull(friday, updatedAt))
    }

    @Test
    fun toDomain_mapsAllFields() {
        val entity = ExchangeRateEntity("JPY", "일본 옌", 100, BigDecimal("935.12"), friday, updatedAt)

        val expected = ExchangeRate("JPY", "일본 옌", 100, BigDecimal("935.12"), friday, updatedAt)

        assertEquals(expected, entity.toDomain())
    }
}
