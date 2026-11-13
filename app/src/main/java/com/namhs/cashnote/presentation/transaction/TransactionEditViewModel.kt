package com.namhs.cashnote.presentation.transaction

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.ForeignAmount
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.usecase.AddTransactionUseCase
import com.namhs.cashnote.domain.usecase.GetCategoriesUseCase
import com.namhs.cashnote.domain.usecase.GetExchangeRatesUseCase
import com.namhs.cashnote.domain.usecase.GetTransactionUseCase
import com.namhs.cashnote.domain.usecase.RefreshExchangeRatesUseCase
import com.namhs.cashnote.domain.usecase.UpdateTransactionUseCase
import com.namhs.cashnote.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class TransactionEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTransaction: GetTransactionUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val getExchangeRates: GetExchangeRatesUseCase,
    private val refreshExchangeRates: RefreshExchangeRatesUseCase
) : MviViewModel<TransactionEditUiState, TransactionEditIntent, TransactionEditSideEffect>(
    TransactionEditUiState(date = LocalDate.now())
) {
    // toRoute()는 내부에서 Android Bundle을 써서 JVM 단위 테스트에서 동작하지 않으므로 키로 직접 읽는다
    private val transactionId: Long = savedStateHandle.get<Long>(ARG_TRANSACTION_ID) ?: 0L

    // 수정 모드에서 불러온 원본. 저장 시 id와 createdAt, 같은 통화의 적용 환율을 유지하는 데 쓴다
    private var original: Transaction? = null

    init {
        observeCategories()
        observeExchangeRates()
        if (transactionId != 0L) loadTransaction(transactionId)
    }

    override fun onIntent(intent: TransactionEditIntent) {
        when (intent) {
            is TransactionEditIntent.AmountChanged -> {
                val isForeign = currentState.currencyCode != null
                val amount = sanitizeAmount(intent.input, isForeign)
                setState { copy(amountInput = amount, isAmountError = false) }
            }

            is TransactionEditIntent.CurrencySelected -> {
                if (intent.currencyCode == currentState.currencyCode) return
                setState {
                    copy(
                        currencyCode = intent.currencyCode,
                        // 외화 → 원화로 바꾸면 소수 부분은 버린다
                        amountInput = sanitizeAmount(amountInput, intent.currencyCode != null),
                        isAmountError = false
                    ).withResolvedRate()
                }
            }

            is TransactionEditIntent.TypeChanged -> {
                if (intent.type == currentState.type) return
                // 카테고리는 유형별로 다르므로 선택을 초기화한다
                setState {
                    copy(type = intent.type, selectedCategoryId = null, isCategoryError = false)
                }
            }

            is TransactionEditIntent.CategorySelected ->
                setState { copy(selectedCategoryId = intent.categoryId, isCategoryError = false) }

            is TransactionEditIntent.DateSelected -> setState { copy(date = intent.date) }

            is TransactionEditIntent.MemoChanged -> setState { copy(memo = intent.memo) }

            TransactionEditIntent.SaveClicked -> save()

            TransactionEditIntent.BackClicked ->
                postSideEffect(TransactionEditSideEffect.NavigateBack)
        }
    }

    // 유형이 바뀔 때마다 해당 유형의 카테고리를 다시 구독한다
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeCategories() {
        viewModelScope.launch {
            uiState.map { it.type }
                .distinctUntilChanged()
                .flatMapLatest { type -> getCategories(type) }
                .collect { categories -> setState { copy(categories = categories) } }
        }
    }

    // 환율 캐시를 구독한다. 처음 화면에 들어왔을 때 비어 있으면 한 번 받아 온다(실패는 무시)
    private fun observeExchangeRates() {
        viewModelScope.launch {
            var refreshTried = false
            getExchangeRates().collect { rates ->
                setState { copy(exchangeRates = rates).withResolvedRate() }
                if (rates.isEmpty() && !refreshTried) {
                    refreshTried = true
                    launch { refreshExchangeRatesQuietly() }
                }
            }
        }
    }

    private suspend fun refreshExchangeRatesQuietly() {
        try {
            refreshExchangeRates()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 환율을 못 받아도 원화 입력은 할 수 있으므로 알리지 않는다
        }
    }

    /**
     * 선택한 통화에 맞는 적용 환율을 정한다.
     * 수정 중인 거래와 같은 통화면 그때 적용한 환율을 그대로 쓰고, 아니면 저장된 최신 환율을 쓴다.
     */
    private fun TransactionEditUiState.withResolvedRate(): TransactionEditUiState {
        val code = currencyCode ?: return copy(appliedRate = null, rateDate = null)
        original?.foreign?.takeIf { it.currencyCode == code }?.let {
            return copy(appliedRate = it.appliedRate, rateDate = null)
        }
        val rate = exchangeRates.find { it.currencyCode == code }
        return copy(appliedRate = rate?.ratePerUnit, rateDate = rate?.rateDate)
    }

    private fun loadTransaction(id: Long) {
        setState { copy(isEditMode = true, isLoading = true) }
        viewModelScope.launch {
            val transaction = getTransaction(id)
            if (transaction == null) {
                showMessage(R.string.transaction_not_found)
                postSideEffect(TransactionEditSideEffect.NavigateBack)
                return@launch
            }
            original = transaction
            val foreign = transaction.foreign
            setState {
                copy(
                    isLoading = false,
                    amountInput = foreign?.amount?.toPlainString() ?: transaction.amount.toString(),
                    currencyCode = foreign?.currencyCode,
                    type = transaction.type,
                    selectedCategoryId = transaction.categoryId,
                    date = transaction.date,
                    memo = transaction.memo
                ).withResolvedRate()
            }
        }
    }

    private fun save() {
        val state = currentState
        if (!state.isSaveEnabled) return

        val code = state.currencyCode
        val rate = state.appliedRate
        if (code != null && rate == null) {
            showMessage(R.string.transaction_rate_unavailable)
            return
        }

        // 외화는 원금액과 적용 환율을 함께 남기고, 원화 금액은 저장 시점 환율로 환산한다
        val foreign = if (code != null && rate != null) {
            state.amountInput.toBigDecimalOrNull()
                ?.takeIf { it.signum() > 0 }
                ?.let { ForeignAmount(code, it, rate) }
        } else {
            null
        }
        val amount = if (code == null) state.amountInput.toLongOrNull() else foreign?.toKrw()
        val validAmount = amount?.takeIf { it > 0 }
        val categoryId = state.selectedCategoryId
        if (validAmount == null || categoryId == null) {
            setState {
                copy(isAmountError = validAmount == null, isCategoryError = categoryId == null)
            }
            return
        }

        setState { copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val base = original
                if (base == null) {
                    addTransaction(
                        Transaction(
                            amount = validAmount,
                            type = state.type,
                            categoryId = categoryId,
                            memo = state.memo,
                            date = state.date,
                            createdAt = Instant.now(),
                            foreign = foreign
                        )
                    )
                } else {
                    updateTransaction(
                        base.copy(
                            amount = validAmount,
                            type = state.type,
                            categoryId = categoryId,
                            memo = state.memo,
                            date = state.date,
                            foreign = foreign
                        )
                    )
                }
                postSideEffect(TransactionEditSideEffect.NavigateBack)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setState { copy(isSaving = false) }
                showMessage(R.string.transaction_save_failed)
            }
        }
    }

    private fun showMessage(@StringRes messageRes: Int) {
        postSideEffect(TransactionEditSideEffect.ShowMessage(messageRes))
    }

    companion object {
        /** 이동 인자 키. CashNoteDestination.TransactionEdit의 프로퍼티 이름과 같아야 한다 */
        internal const val ARG_TRANSACTION_ID = "transactionId"

        // 999,999,999,999원까지 입력할 수 있다
        private const val MAX_AMOUNT_DIGITS = 12

        // 외화는 정수 10자리, 소수 2자리까지 입력할 수 있다
        private const val MAX_FOREIGN_INTEGER_DIGITS = 10
        private const val MAX_FOREIGN_FRACTION_DIGITS = 2

        /**
         * 금액 입력값을 정리한다.
         * 원화는 숫자만 남겨 Long 범위를 넘지 않게 자르고, 외화는 소수점 하나와 소수 둘째 자리까지 허용한다.
         */
        internal fun sanitizeAmount(input: String, isForeign: Boolean): String {
            if (!isForeign) {
                return input.substringBefore('.').filter { it.isDigit() }.take(MAX_AMOUNT_DIGITS)
            }
            val integer = input.substringBefore('.').filter { it.isDigit() }
                .take(MAX_FOREIGN_INTEGER_DIGITS)
            if ('.' !in input) return integer
            val fraction = input.substringAfter('.').filter { it.isDigit() }
                .take(MAX_FOREIGN_FRACTION_DIGITS)
            return "$integer.$fraction"
        }
    }
}
