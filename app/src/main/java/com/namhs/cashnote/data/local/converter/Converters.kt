package com.namhs.cashnote.data.local.converter

import androidx.room.TypeConverter
import com.namhs.cashnote.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate

/**
 * Room이 직접 저장하지 못하는 타입을 변환한다.
 *
 * - LocalDate: epochDay(1970-01-01부터의 일 수) Long
 * - Instant: epoch millis Long (밀리초 미만은 버린다)
 * - TransactionType: enum 이름 String
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
}
