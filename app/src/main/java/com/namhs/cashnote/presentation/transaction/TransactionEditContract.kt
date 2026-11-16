package com.namhs.cashnote.presentation.transaction

import androidx.annotation.StringRes
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.ExchangeRate
import com.namhs.cashnote.domain.model.ForeignAmount
import com.namhs.cashnote.domain.model.PaymentMethod
import com.namhs.cashnote.domain.model.TransactionType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** 거래 입력·수정 화면 상태 */
data class TransactionEditUiState(
    /** true면 기존 거래 수정, false면 새 거래 입력 */
    val isEditMode: Boolean = false,
    /** 수정할 거래를 불러오는 중 */
    val isLoading: Boolean = false,
    /** 금액 입력값. 원화는 숫자만, 외화는 소수점 둘째 자리까지 담는다 */
    val amountInput: String = "",
    /** 선택한 통화 코드. null이면 원화 */
    val currencyCode: String? = null,
    /** 저장된 환율 목록. 통화 선택지와 환산에 쓴다 */
    val exchangeRates: List<ExchangeRate> = emptyList(),
    /** 외화일 때 환산에 쓸 환율(외화 1단위당 원화). 환율 정보가 없으면 null */
    val appliedRate: BigDecimal? = null,
    /** [appliedRate]의 기준 영업일. 기존 거래의 환율을 그대로 쓰면 null */
    val rateDate: LocalDate? = null,
    /** [appliedRate]를 받아 저장한 시각. 기존 거래의 환율을 그대로 쓰면 null */
    val rateUpdatedAt: Instant? = null,
    /** 인터넷에 연결돼 있지 않다 */
    val isOffline: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    /** 현재 유형에 해당하는 카테고리 목록 */
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    /** 거래일. 초기값은 ViewModel이 오늘 날짜로 정한다 */
    val date: LocalDate,
    val memo: String = "",
    /** 결제수단. 새 거래는 가장 흔한 카드로 시작하고, 수정할 때는 거래의 값을 쓴다 */
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    /** 저장 시 금액이 비었거나 0 이하였다 */
    val isAmountError: Boolean = false,
    /** 저장 시 카테고리를 고르지 않았다 */
    val isCategoryError: Boolean = false,
    val isSaving: Boolean = false
) {
    /** 불러오거나 저장하는 중에는 저장 버튼을 막는다 */
    val isSaveEnabled: Boolean
        get() = !isLoading && !isSaving

    /** 원화 외에 고를 수 있는 통화. 저장된 환율 통화에, 목록에 없는 현재 선택 통화를 더한다 */
    val foreignCurrencyOptions: List<String>
        get() = (exchangeRates.map { it.currencyCode } + listOfNotNull(currencyCode)).distinct()

    /** 외화를 골랐지만 환산할 환율이 없다 */
    val isRateUnavailable: Boolean
        get() = currencyCode != null && appliedRate == null

    /** 오프라인이라 새 환율을 받지 못하고, 전에 받아 둔 최신 환율로 환산한다. 기존 거래의 저장 당시 환율은 해당하지 않는다 */
    val isUsingCachedRateOffline: Boolean
        get() = isOffline && rateUpdatedAt != null

    /** 외화를 골랐는데 받아 둔 환율이 없고, 오프라인이라 받을 수도 없다 */
    val isRateUnavailableOffline: Boolean
        get() = isOffline && isRateUnavailable

    /** 외화 입력값을 원화로 환산한 금액. 원화이거나 입력·환율이 없으면 null */
    val convertedKrw: Long?
        get() {
            val code = currencyCode ?: return null
            val rate = appliedRate ?: return null
            val amount = amountInput.toBigDecimalOrNull() ?: return null
            return ForeignAmount(code, amount, rate).toKrw()
        }
}

/** 거래 입력·수정 화면에서 발생하는 사용자 의도 */
sealed interface TransactionEditIntent {
    data class AmountChanged(val input: String) : TransactionEditIntent

    /** 통화 선택. null이면 원화 */
    data class CurrencySelected(val currencyCode: String?) : TransactionEditIntent

    data class TypeChanged(val type: TransactionType) : TransactionEditIntent

    data class CategorySelected(val categoryId: Long) : TransactionEditIntent

    data class DateSelected(val date: LocalDate) : TransactionEditIntent

    data class MemoChanged(val memo: String) : TransactionEditIntent

    data class PaymentMethodSelected(val paymentMethod: PaymentMethod) : TransactionEditIntent

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
