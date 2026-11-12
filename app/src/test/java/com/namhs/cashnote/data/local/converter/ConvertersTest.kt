package com.namhs.cashnote.data.local.converter

import com.namhs.cashnote.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    // LocalDate

    @Test
    fun localDate_roundTrip() {
        val date = LocalDate.of(2026, 11, 9)

        val restored = converters.toLocalDate(converters.fromLocalDate(date))

        assertEquals(date, restored)
    }

    @Test
    fun localDate_storedAsEpochDay() {
        assertEquals(0L, converters.fromLocalDate(LocalDate.of(1970, 1, 1)))
        assertEquals(1L, converters.fromLocalDate(LocalDate.of(1970, 1, 2)))
    }

    // Instant

    @Test
    fun instant_roundTrip() {
        val instant = Instant.parse("2026-11-09T03:15:30.123Z")

        val restored = converters.toInstant(converters.fromInstant(instant))

        assertEquals(instant, restored)
    }

    @Test
    fun instant_storedAsEpochMilli() {
        assertEquals(1_000L, converters.fromInstant(Instant.parse("1970-01-01T00:00:01Z")))
    }

    @Test
    fun instant_dropsSubMillisecondPrecision() {
        // 밀리초 미만(나노초)은 저장되지 않는다
        val instant = Instant.parse("2026-11-09T03:15:30.123456789Z")

        val restored = converters.toInstant(converters.fromInstant(instant))

        assertEquals(Instant.parse("2026-11-09T03:15:30.123Z"), restored)
    }

    // TransactionType

    @Test
    fun transactionType_roundTrip() {
        TransactionType.entries.forEach { type ->
            val restored = converters.toTransactionType(converters.fromTransactionType(type))

            assertEquals(type, restored)
        }
    }

    @Test
    fun transactionType_storedAsName() {
        assertEquals("INCOME", converters.fromTransactionType(TransactionType.INCOME))
        assertEquals("EXPENSE", converters.fromTransactionType(TransactionType.EXPENSE))
    }

    // YearMonth

    @Test
    fun yearMonth_roundTrip() {
        listOf(YearMonth.of(2026, 1), YearMonth.of(2026, 11), YearMonth.of(2026, 12)).forEach {
            assertEquals(it, converters.toYearMonth(converters.fromYearMonth(it)))
        }
    }

    @Test
    fun yearMonth_storedValueKeepsMonthOrder() {
        // DB에서 start_month <= :month 비교와 정렬이 월 순서와 같아야 한다
        val december = converters.fromYearMonth(YearMonth.of(2026, 12))
        val nextJanuary = converters.fromYearMonth(YearMonth.of(2027, 1))

        assertEquals(1, nextJanuary - december)
    }
}
