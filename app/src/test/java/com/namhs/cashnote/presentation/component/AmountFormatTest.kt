package com.namhs.cashnote.presentation.component

import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountFormatTest {
    private val korea = Locale.KOREA

    @Test
    fun usd_showsTwoDecimals() {
        assertEquals("USD 12.50", formatForeignAmount("USD", BigDecimal("12.5"), korea))
        assertEquals("USD 12.00", formatForeignAmount("USD", BigDecimal("12"), korea))
    }

    @Test
    fun jpy_showsNoDecimals() {
        assertEquals("JPY 1,000", formatForeignAmount("JPY", BigDecimal("1000"), korea))
        assertEquals("JPY 1,000", formatForeignAmount("JPY", BigDecimal("1000.00"), korea))
    }

    @Test
    fun moreDecimalsThanStandard_areKept() {
        // 입력 화면은 엔화도 소수 둘째 자리까지 받으므로, 잘라 내면 저장한 값과 달라진다
        assertEquals("JPY 1,000.5", formatForeignAmount("JPY", BigDecimal("1000.50"), korea))
    }

    @Test
    fun largeAmount_hasGroupingSeparators() {
        assertEquals(
            "EUR 1,234,567.89",
            formatForeignAmount("EUR", BigDecimal("1234567.89"), korea)
        )
    }

    @Test
    fun unknownCurrency_usesTwoDecimals() {
        assertEquals("ZZZ 3.10", formatForeignAmount("ZZZ", BigDecimal("3.1"), korea))
    }

    @Test
    fun specialCodeWithoutStandardDigits_usesTwoDecimals() {
        // XAU(금)는 표준 자릿수가 없어 -1을 돌려준다
        assertEquals("XAU 1.00", formatForeignAmount("XAU", BigDecimal("1"), korea))
    }

    @Test
    fun followsLocaleSeparators() {
        // 독일어권은 천 단위에 점, 소수점에 쉼표를 쓴다
        assertEquals(
            "USD 1.234,50",
            formatForeignAmount("USD", BigDecimal("1234.5"), Locale.GERMANY)
        )
    }
}
