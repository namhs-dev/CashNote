package com.namhs.cashnote.domain.model

import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant
import java.time.LocalDate

/**
 * 통화 하나의 환율.
 *
 * [baseRate]는 외화 [unit]만큼이 몇 원인지다. JPY는 unit이 100이라 100엔당 원화 금액이다.
 */
data class ExchangeRate(
    /** 통화 코드. "USD", "JPY" 등 */
    val currencyCode: String,
    /** 국가/통화명 */
    val currencyName: String,
    /** 환율 기준 단위. JPY·IDR은 100, 그 외 1 */
    val unit: Int,
    /** 매매 기준율. [unit]만큼의 외화가 몇 원인지 */
    val baseRate: BigDecimal,
    /** 환율 기준 영업일 */
    val rateDate: LocalDate,
    /** 이 환율을 받아 저장한 시각 */
    val updatedAt: Instant
) {
    /** 외화 1단위가 몇 원인지. JPY는 100엔 기준 환율을 100으로 나눈다 */
    val ratePerUnit: BigDecimal
        get() = baseRate.divide(BigDecimal(unit), MathContext.DECIMAL64)
}
