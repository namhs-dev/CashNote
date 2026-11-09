package com.namhs.cashnote.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * 거래 한 건.
 *
 * 카테고리는 id만 가진다. 화면에 필요한 카테고리 정보는 UseCase에서 붙인다.
 */
data class Transaction(
    /** 0이면 아직 저장되지 않은 거래 */
    val id: Long = 0,
    /** 금액. 원 단위 정수 */
    val amount: Long,
    val type: TransactionType,
    val categoryId: Long,
    /** 메모. 입력하지 않으면 빈 문자열 */
    val memo: String = "",
    /** 거래일 */
    val date: LocalDate,
    /** 생성 시각 */
    val createdAt: Instant
)
