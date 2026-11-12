package com.namhs.cashnote.presentation.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionWithCategory
import com.namhs.cashnote.domain.usecase.AddTransactionUseCase
import com.namhs.cashnote.domain.usecase.DeleteTransactionUseCase
import com.namhs.cashnote.domain.usecase.GetMonthlySummaryUseCase
import com.namhs.cashnote.domain.usecase.GetMonthlyTransactionsUseCase
import com.namhs.cashnote.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getMonthlyTransactions: GetMonthlyTransactionsUseCase,
    private val getMonthlySummary: GetMonthlySummaryUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val addTransaction: AddTransactionUseCase
) : MviViewModel<HomeUiState, HomeIntent, HomeSideEffect>(
    // 프로세스가 정리됐다가 복원되면 보던 월에서 다시 시작한다
    HomeUiState(
        month = savedStateHandle.get<String>(KEY_MONTH)?.let(YearMonth::parse) ?: YearMonth.now()
    )
) {

    // 실행 취소용으로 마지막에 삭제한 거래를 보관한다. 새로 삭제하면 덮어쓴다
    private var recentlyDeleted: Transaction? = null

    init {
        observeMonth()
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.AddClicked -> postSideEffect(HomeSideEffect.NavigateToEdit(0))

            is HomeIntent.TransactionClicked ->
                postSideEffect(HomeSideEffect.NavigateToEdit(intent.transactionId))

            HomeIntent.PreviousMonthClicked -> moveMonth(-1)

            HomeIntent.NextMonthClicked -> moveMonth(1)

            is HomeIntent.DeleteRequested -> delete(intent.transactionId)

            HomeIntent.UndoDeleteClicked -> undoDelete()
        }
    }

    // 월이 바뀔 때마다 그 달의 거래 목록과 합계를 함께 다시 구독한다.
    // 이전 달 구독은 flatMapLatest가 둘 다 취소하므로 다른 달의 목록과 합계가 섞이지 않는다.
    // 새 달의 데이터가 올 때까지 기존 화면을 그대로 두어 빈 화면이 깜빡이지 않게 한다
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeMonth() {
        viewModelScope.launch {
            uiState.map { it.month }
                .distinctUntilChanged()
                .flatMapLatest { month ->
                    combine(getMonthlyTransactions(month), getMonthlySummary(month), ::Pair)
                }
                .collect { (items, summary) ->
                    setState {
                        copy(
                            isLoading = false,
                            dailyGroups = items.groupByDate(),
                            summary = summary
                        )
                    }
                }
        }
    }

    private fun moveMonth(offset: Long) {
        val month = currentState.month.plusMonths(offset)
        savedStateHandle[KEY_MONTH] = month.toString()
        setState { copy(month = month) }
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

    companion object {
        // 이동 인자가 아니라 이 화면이 직접 저장하는 값. "2026-11" 형식 문자열
        internal const val KEY_MONTH = "selectedMonth"
    }
}

// DB가 이미 날짜 최신순으로 정렬해 주므로, 처음 나온 순서를 유지하는 groupBy로 묶으면 된다
private fun List<TransactionWithCategory>.groupByDate(): List<DailyTransactions> =
    groupBy { it.transaction.date }.map { (date, items) -> DailyTransactions(date, items) }
