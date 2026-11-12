package com.namhs.cashnote.data.local.converter

import androidx.room.TypeConverter
import com.namhs.cashnote.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * Room이 직접 저장하지 못하는 타입을 변환한다.
 *
 * - LocalDate: epochDay(1970-01-01부터의 일 수) Long
 * - Instant: epoch millis Long (밀리초 미만은 버린다)
 * - TransactionType: enum 이름 String
 * - YearMonth: epoch month(year × 12 + month − 1) Int. 크기 비교가 곧 월 순서다
 */
class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @TypeConverter
    fun fromInstant(instant: Instant): Long = instant.toEpochMilli()

    @TypeConverter
    fun toInstant(epochMilli: Long): Instant = Instant.ofEpochMilli(epochMilli)

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(name: String): TransactionType = TransactionType.valueOf(name)

    @TypeConverter
    fun fromYearMonth(month: YearMonth): Int = month.year * 12 + month.monthValue - 1

    @TypeConverter
    fun toYearMonth(epochMonth: Int): YearMonth =
        YearMonth.of(Math.floorDiv(epochMonth, 12), Math.floorMod(epochMonth, 12) + 1)
}
