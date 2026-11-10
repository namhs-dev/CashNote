package com.namhs.cashnote.presentation.transaction

import androidx.annotation.StringRes
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.TransactionType
import java.time.LocalDate

/** 거래 입력·수정 화면 상태 */
data class TransactionEditUiState(
    /** true면 기존 거래 수정, false면 새 거래 입력 */
    val isEditMode: Boolean = false,
    /** 수정할 거래를 불러오는 중 */
    val isLoading: Boolean = false,
    /** 금액 입력값. 숫자만 담는다 */
    val amountInput: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    /** 현재 유형에 해당하는 카테고리 목록 */
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    /** 거래일. 초기값은 ViewModel이 오늘 날짜로 정한다 */
    val date: LocalDate,
    val memo: String = "",
    /** 저장 시 금액이 비었거나 0 이하였다 */
    val isAmountError: Boolean = false,
    /** 저장 시 카테고리를 고르지 않았다 */
    val isCategoryError: Boolean = false,
    val isSaving: Boolean = false
) {
    /** 불러오거나 저장하는 중에는 저장 버튼을 막는다 */
    val isSaveEnabled: Boolean
        get() = !isLoading && !isSaving
}

/** 거래 입력·수정 화면에서 발생하는 사용자 의도 */
sealed interface TransactionEditIntent {
    data class AmountChanged(val input: String) : TransactionEditIntent

    data class TypeChanged(val type: TransactionType) : TransactionEditIntent

    data class CategorySelected(val categoryId: Long) : TransactionEditIntent

    data class DateSelected(val date: LocalDate) : TransactionEditIntent

    data class MemoChanged(val memo: String) : TransactionEditIntent

    data object SaveClicked : TransactionEditIntent

    data object BackClicked : TransactionEditIntent
}

/** 거래 입력·수정 화면의 일회성 이벤트 */
sealed interface TransactionEditSideEffect {
    /** 저장 완료, 뒤로 가기, 수정할 거래가 없을 때 이전 화면으로 돌아간다 */
    data object NavigateBack : TransactionEditSideEffect

    /** 저장 실패 등 사용자에게 알릴 메시지 */
    data class ShowMessage(@StringRes val messageRes: Int) : TransactionEditSideEffect
}
