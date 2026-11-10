package com.namhs.cashnote.presentation.home

import androidx.lifecycle.viewModelScope
import com.namhs.cashnote.domain.model.TransactionWithCategory
import com.namhs.cashnote.domain.usecase.GetMonthlyTransactionsUseCase
import com.namhs.cashnote.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMonthlyTransactions: GetMonthlyTransactionsUseCase
) : MviViewModel<HomeUiState, HomeIntent, HomeSideEffect>(HomeUiState(month = YearMonth.now())) {

    init {
        observeTransactions()
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.AddClicked -> postSideEffect(HomeSideEffect.NavigateToEdit(0))

            is HomeIntent.TransactionClicked ->
                postSideEffect(HomeSideEffect.NavigateToEdit(intent.transactionId))
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
}

// DB가 이미 날짜 최신순으로 정렬해 주므로, 처음 나온 순서를 유지하는 groupBy로 묶으면 된다
private fun List<TransactionWithCategory>.groupByDate(): List<DailyTransactions> =
    groupBy { it.transaction.date }.map { (date, items) -> DailyTransactions(date, items) }
