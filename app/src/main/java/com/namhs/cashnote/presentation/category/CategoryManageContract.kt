package com.namhs.cashnote.presentation.category

import androidx.annotation.StringRes
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.TransactionType

/** 카테고리 관리 화면 상태 */
data class CategoryManageUiState(
    /** 보고 있는 유형(지출/수입) 탭 */
    val type: TransactionType = TransactionType.EXPENSE,
    /** [type]에 해당하는 카테고리 목록 */
    val categories: List<Category> = emptyList(),
    /** 추가·수정 다이얼로그. null이면 닫혀 있다 */
    val editor: CategoryEditorState? = null
)

/**
 * 카테고리 추가·수정 다이얼로그 상태.
 *
 * 저장 시 검증하므로 입력 내용과 함께 ViewModel이 들고 있는다.
 */
data class CategoryEditorState(
    /** 수정할 카테고리 id. null이면 새 카테고리 */
    val categoryId: Long? = null,
    val name: String = "",
    /** 표시 색상. 0xAARRGGBB */
    val color: Long,
    val nameError: CategoryNameError? = null,
    val isSaving: Boolean = false
) {
    val isNew: Boolean
        get() = categoryId == null
}

/** 카테고리 이름 검증 오류 */
enum class CategoryNameError {
    /** 비었거나 공백뿐이다 */
    EMPTY,

    /** 같은 유형에 같은 이름이 이미 있다 */
    DUPLICATE
}

/** 카테고리 관리 화면에서 발생하는 사용자 의도 */
sealed interface CategoryManageIntent {
    data class TypeChanged(val type: TransactionType) : CategoryManageIntent

    data object BackClicked : CategoryManageIntent

    /** 새 카테고리 추가 버튼 */
    data object AddClicked : CategoryManageIntent

    /** 목록의 카테고리를 눌러 수정 */
    data class CategoryClicked(val category: Category) : CategoryManageIntent

    data class EditorNameChanged(val name: String) : CategoryManageIntent

    data class EditorColorSelected(val color: Long) : CategoryManageIntent

    data object EditorSaveClicked : CategoryManageIntent

    data object EditorDismissed : CategoryManageIntent
}

/** 카테고리 관리 화면의 일회성 이벤트 */
sealed interface CategoryManageSideEffect {
    data object NavigateBack : CategoryManageSideEffect

    /** 저장 실패 등 사용자에게 알릴 메시지 */
    data class ShowMessage(@StringRes val messageRes: Int) : CategoryManageSideEffect
}
