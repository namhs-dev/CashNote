package com.namhs.cashnote.presentation.transaction

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.usecase.AddTransactionUseCase
import com.namhs.cashnote.domain.usecase.GetCategoriesUseCase
import com.namhs.cashnote.domain.usecase.GetTransactionUseCase
import com.namhs.cashnote.domain.usecase.UpdateTransactionUseCase
import com.namhs.cashnote.presentation.base.MviViewModel
import com.namhs.cashnote.presentation.navigation.CashNoteDestination
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
    private val updateTransaction: UpdateTransactionUseCase
) : MviViewModel<TransactionEditUiState, TransactionEditIntent, TransactionEditSideEffect>(
    TransactionEditUiState(date = LocalDate.now())
) {
    private val transactionId =
        savedStateHandle.toRoute<CashNoteDestination.TransactionEdit>().transactionId

    // 수정 모드에서 불러온 원본. 저장 시 id와 createdAt을 유지하는 데 쓴다
    private var original: Transaction? = null

    init {
        observeCategories()
        if (transactionId != 0L) loadTransaction(transactionId)
    }

    override fun onIntent(intent: TransactionEditIntent) {
        when (intent) {
            is TransactionEditIntent.AmountChanged -> {
                // 숫자만 남기고 Long 범위를 넘지 않도록 자릿수를 제한한다
                val digits = intent.input.filter { it.isDigit() }.take(MAX_AMOUNT_DIGITS)
                setState { copy(amountInput = digits, isAmountError = false) }
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
            setState {
                copy(
                    isLoading = false,
                    amountInput = transaction.amount.toString(),
                    type = transaction.type,
                    selectedCategoryId = transaction.categoryId,
                    date = transaction.date,
                    memo = transaction.memo
                )
            }
        }
    }

    private fun save() {
        val state = currentState
        if (!state.isSaveEnabled) return

        val amount = state.amountInput.toLongOrNull()?.takeIf { it > 0 }
        val categoryId = state.selectedCategoryId
        if (amount == null || categoryId == null) {
            setState { copy(isAmountError = amount == null, isCategoryError = categoryId == null) }
            return
        }

        setState { copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val base = original
                if (base == null) {
                    addTransaction(
                        Transaction(
                            amount = amount,
                            type = state.type,
                            categoryId = categoryId,
                            memo = state.memo,
                            date = state.date,
                            createdAt = Instant.now()
                        )
                    )
                } else {
                    updateTransaction(
                        base.copy(
                            amount = amount,
                            type = state.type,
                            categoryId = categoryId,
                            memo = state.memo,
                            date = state.date
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

    private companion object {
        // 999,999,999,999원까지 입력할 수 있다
        const val MAX_AMOUNT_DIGITS = 12
    }
}
