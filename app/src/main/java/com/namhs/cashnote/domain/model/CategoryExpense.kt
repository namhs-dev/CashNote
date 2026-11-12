package com.namhs.cashnote.domain.model

/** 한 카테고리의 월 지출과 전체 지출 중 비율 */
data class CategoryExpense(
    val category: Category,
    /** 지출 합계. 원 단위 */
    val amount: Long,
    /** 전체 지출 중 비율. 0~1, 한 달 목록의 비율을 모두 더하면 1이다 */
    val ratio: Float
)
