package com.namhs.cashnote.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 외화로 낸 거래의 원래 금액과 그때 적용한 환율.
 *
 * 거래의 원화 금액(Transaction.amount)은 저장 시점에 [toKrw]로 계산해 따로 저장한다.
 */
data class ForeignAmount(
    /** 통화 코드. "USD", "JPY" 등 */
    val currencyCode: String,
    /** 외화 원금액. 12.50달러처럼 소수가 있을 수 있다 */
    val amount: BigDecimal,
    /** 적용 환율. 외화 1단위가 몇 원인지 (JPY는 100엔 기준 환율을 100으로 나눈 값) */
    val appliedRate: BigDecimal
) {
    /** 원화 금액. 원 단위로 반올림한다 */
    fun toKrw(): Long = amount.multiply(appliedRate).setScale(0, RoundingMode.HALF_UP).toLong()
}
