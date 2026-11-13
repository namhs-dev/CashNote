package com.namhs.cashnote.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ForeignAmountTest {
    private fun foreign(amount: String, rate: String) =
        ForeignAmount("USD", BigDecimal(amount), BigDecimal(rate))

    @Test
    fun toKrw_multipliesAmountByRate() {
        assertEquals(14_505L, foreign("10", "1450.5").toKrw())
    }

    @Test
    fun toKrw_roundsHalfUp() {
        // 12.5 × 1450.5 = 18131.25 → 18131
        assertEquals(18_131L, foreign("12.5", "1450.5").toKrw())
        // 0.5 × 1.001 = 0.5005 → 1
        assertEquals(1L, foreign("0.5", "1.001").toKrw())
        // 0.3 × 1.5 = 0.45 → 0
        assertEquals(0L, foreign("0.3", "1.5").toKrw())
    }

    @Test
    fun toKrw_withJpyRatePerUnit() {
        val jpy = ExchangeRate(
            currencyCode = "JPY",
            currencyName = "일본 옌",
            unit = 100,
            baseRate = BigDecimal("935.12"),
            rateDate = LocalDate.of(2026, 11, 13),
            updatedAt = Instant.parse("2026-11-13T03:00:00Z")
        )

        // 1,000엔 × 9.3512 = 9351.2 → 9351
        val krw = ForeignAmount("JPY", BigDecimal("1000"), jpy.ratePerUnit).toKrw()

        assertEquals(9_351L, krw)
    }
}
