package com.namhs.cashnote.presentation.category

import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.TransactionType

/** 카테고리 관리 화면 상태 */
data class CategoryManageUiState(
    /** 보고 있는 유형(지출/수입) 탭 */
    val type: TransactionType = TransactionType.EXPENSE,
    /** [type]에 해당하는 카테고리 목록 */
    val categories: List<Category> = emptyList()
)

/** 카테고리 관리 화면에서 발생하는 사용자 의도 */
sealed interface CategoryManageIntent {
    data class TypeChanged(val type: TransactionType) : CategoryManageIntent

    data object BackClicked : CategoryManageIntent
}

/** 카테고리 관리 화면의 일회성 이벤트 */
sealed interface CategoryManageSideEffect {
    data object NavigateBack : CategoryManageSideEffect
}
