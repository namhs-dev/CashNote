package com.namhs.cashnote.presentation.home

import androidx.lifecycle.viewModelScope
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionWithCategory
import com.namhs.cashnote.domain.usecase.AddTransactionUseCase
import com.namhs.cashnote.domain.usecase.DeleteTransactionUseCase
import com.namhs.cashnote.domain.usecase.GetMonthlyTransactionsUseCase
import com.namhs.cashnote.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMonthlyTransactions: GetMonthlyTransactionsUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val addTransaction: AddTransactionUseCase
) : MviViewModel<HomeUiState, HomeIntent, HomeSideEffect>(HomeUiState(month = YearMonth.now())) {

    // 실행 취소용으로 마지막에 삭제한 거래를 보관한다. 새로 삭제하면 덮어쓴다
    private var recentlyDeleted: Transaction? = null

    init {
        observeTransactions()
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.AddClicked -> postSideEffect(HomeSideEffect.NavigateToEdit(0))

            is HomeIntent.TransactionClicked ->
                postSideEffect(HomeSideEffect.NavigateToEdit(intent.transactionId))

            is HomeIntent.DeleteRequested -> delete(intent.transactionId)

            HomeIntent.UndoDeleteClicked -> undoDelete()
        }
    }

    // 지금은 이번 달만 구독한다. 월 이동(#30)이 생기면 month 변경에 따라 다시 구독한다
    private fun observeTransactions() {
        viewModelScope.launch {
            getMonthlyTransactions(currentState.month).collect { items ->
                setState { copy(isLoading = false, dailyGroups = items.groupByDate()) }
            }
        }
    }

    private fun delete(transactionId: Long) {
        val transaction = currentState.dailyGroups
            .flatMap { it.items }
            .firstOrNull { it.transaction.id == transactionId }
            ?.transaction
            ?: return
        viewModelScope.launch {
            try {
                deleteTransaction(transaction)
                recentlyDeleted = transaction
                postSideEffect(HomeSideEffect.ShowDeleteUndo)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                postSideEffect(HomeSideEffect.ShowMessage(R.string.home_delete_failed))
            }
        }
    }

    private fun undoDelete() {
        val transaction = recentlyDeleted ?: return
        recentlyDeleted = null
        viewModelScope.launch {
            try {
                // 같은 id로 다시 넣어 원래 거래를 그대로 복구한다
                addTransaction(transaction)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                postSideEffect(HomeSideEffect.ShowMessage(R.string.transaction_save_failed))
            }
        }
    }
}

// DB가 이미 날짜 최신순으로 정렬해 주므로, 처음 나온 순서를 유지하는 groupBy로 묶으면 된다
private fun List<TransactionWithCategory>.groupByDate(): List<DailyTransactions> =
    groupBy { it.transaction.date }.map { (date, items) -> DailyTransactions(date, items) }
