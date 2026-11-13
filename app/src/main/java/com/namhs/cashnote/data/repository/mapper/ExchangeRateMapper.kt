package com.namhs.cashnote.data.repository.mapper

import com.namhs.cashnote.data.local.entity.ExchangeRateEntity
import com.namhs.cashnote.data.remote.dto.ExchangeRateDto
import com.namhs.cashnote.data.remote.dto.toRateOrNull
import com.namhs.cashnote.domain.model.ExchangeRate
import java.time.Instant
import java.time.LocalDate

// DTO → Entity, Entity → Domain 변환

/** "USD", "JPY(100)"처럼 통화 코드 뒤에 괄호로 기준 단위가 붙을 수 있다 */
private val CURRENCY_UNIT_PATTERN = Regex("""^([A-Z]{3})(?:\((\d+)\))?$""")

/**
 * API 응답 항목을 Entity로 바꾼다.
 *
 * 통화 코드나 매매 기준율을 읽을 수 없는 항목은 저장해도 쓸 수 없으므로 null을 돌려준다.
 */
fun ExchangeRateDto.toEntityOrNull(rateDate: LocalDate, updatedAt: Instant): ExchangeRateEntity? {
    val match = currencyUnit?.trim()?.let(CURRENCY_UNIT_PATTERN::matchEntire) ?: return null
    val code = match.groupValues[1]
    val unit = match.groupValues[2].toIntOrNull() ?: 1
    val rate = dealBaseRate?.toRateOrNull() ?: return null
    return ExchangeRateEntity(
        currencyCode = code,
        currencyName = currencyName?.trim().orEmpty().ifEmpty { code },
        unit = unit,
        dealBaseRate = rate,
        rateDate = rateDate,
        updatedAt = updatedAt
    )
}

fun ExchangeRateEntity.toDomain(): ExchangeRate = ExchangeRate(
    currencyCode = currencyCode,
    currencyName = currencyName,
    unit = unit,
    baseRate = dealBaseRate,
    rateDate = rateDate,
    updatedAt = updatedAt
)
