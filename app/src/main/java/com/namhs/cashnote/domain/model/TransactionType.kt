package com.namhs.cashnote.domain.model

/**
 * 거래 유형. 거래와 카테고리가 함께 사용한다.
 */
enum class TransactionType {
    /** 수입 */
    INCOME,

    /** 지출 */
    EXPENSE
}
