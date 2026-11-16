package com.namhs.cashnote.domain.model

/** 카테고리 삭제 결과 */
sealed interface CategoryDeleteResult {
    data object Deleted : CategoryDeleteResult

    /** 이 카테고리를 쓰는 거래가 있어 지우지 않았다 */
    data class InUse(val transactionCount: Int) : CategoryDeleteResult
}
