package com.namhs.cashnote.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ExchangeRateTest {
    private fun rate(unit: Int, baseRate: String) = ExchangeRate(
        currencyCode = "XXX",
        currencyName = "테스트",
        unit = unit,
        baseRate = BigDecimal(baseRate),
        rateDate = LocalDate.of(2026, 11, 13),
        updatedAt = Instant.parse("2026-11-13T03:00:00Z")
    )

    @Test
    fun ratePerUnit_unitOne_isBaseRate() {
        assertEquals(0, BigDecimal("1450.5").compareTo(rate(1, "1450.5").ratePerUnit))
    }

    @Test
    fun ratePerUnit_unitHundred_dividesByHundred() {
        assertEquals(0, BigDecimal("9.3512").compareTo(rate(100, "935.12").ratePerUnit))
        assertEquals(0, BigDecimal("0.0912").compareTo(rate(100, "9.12").ratePerUnit))
    }
}
