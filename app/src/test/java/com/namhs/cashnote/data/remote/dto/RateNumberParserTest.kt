package com.namhs.cashnote.data.remote.dto

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RateNumberParserTest {
    @Test
    fun plainDecimal() {
        assertEquals(BigDecimal("907.1"), "907.1".toRateOrNull())
    }

    @Test
    fun removesThousandsSeparator() {
        assertEquals(BigDecimal("3533.28"), "3,533.28".toRateOrNull())
        assertEquals(BigDecimal("1234567.89"), "1,234,567.89".toRateOrNull())
    }

    @Test
    fun integerWithoutDecimalPoint() {
        assertEquals(BigDecimal("3533"), "3,533".toRateOrNull())
    }

    @Test
    fun trimsWhitespace() {
        assertEquals(BigDecimal("1450.5"), " 1,450.5 ".toRateOrNull())
    }

    @Test
    fun keepsScaleWithoutRounding() {
        // Double을 거치지 않으므로 소수 자릿수가 그대로 유지된다
        assertEquals("1450.10", "1,450.10".toRateOrNull()?.toPlainString())
    }

    @Test
    fun emptyOrBlank_returnsNull() {
        assertNull("".toRateOrNull())
        assertNull("  ".toRateOrNull())
    }

    @Test
    fun notANumber_returnsNull() {
        assertNull("-".toRateOrNull())
        assertNull("abc".toRateOrNull())
    }
}
