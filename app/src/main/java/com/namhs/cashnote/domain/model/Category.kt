package com.namhs.cashnote.domain.model

/**
 * 거래 카테고리.
 */
data class Category(
    val id: Long,
    val name: String,
    /** 이 카테고리를 쓰는 거래 유형(수입/지출) */
    val type: TransactionType,
    /** 아이콘 키. presentation 계층에서 실제 아이콘으로 매핑한다. */
    val icon: String,
    /** 표시 색상. 0xAARRGGBB 형식의 ARGB 값 */
    val color: Long
)
