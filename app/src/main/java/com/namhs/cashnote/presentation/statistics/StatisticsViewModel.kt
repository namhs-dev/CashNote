package com.namhs.cashnote.presentation.statistics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.namhs.cashnote.domain.usecase.GetCategoryExpensesUseCase
import com.namhs.cashnote.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getCategoryExpenses: GetCategoryExpensesUseCase
) : MviViewModel<StatisticsUiState, StatisticsIntent, StatisticsSideEffect>(
    // 프로세스가 정리됐다가 복원되면 보던 월에서 다시 시작한다
    StatisticsUiState(
        month = savedStateHandle.get<String>(KEY_MONTH)?.let(YearMonth::parse) ?: YearMonth.now()
    )
) {

    init {
        observeMonth()
    }

    override fun onIntent(intent: StatisticsIntent) {
        when (intent) {
            StatisticsIntent.PreviousMonthClicked -> moveMonth(-1)
            StatisticsIntent.NextMonthClicked -> moveMonth(1)
        }
    }

    // 월이 바뀔 때마다 그 달의 카테고리별 지출을 다시 구독한다. 이전 달 구독은 flatMapLatest가 취소한다
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeMonth() {
        viewModelScope.launch {
            uiState.map { it.month }
                .distinctUntilChanged()
                .flatMapLatest { month -> getCategoryExpenses(month) }
                .collect { expenses -> setState { copy(isLoading = false, expenses = expenses) } }
        }
    }

    private fun moveMonth(offset: Long) {
        val month = currentState.month.plusMonths(offset)
        savedStateHandle[KEY_MONTH] = month.toString()
        setState { copy(month = month) }
    }

    companion object {
        // 홈과 따로 기억하도록 다른 키를 쓴다. "2026-11" 형식 문자열
        internal const val KEY_MONTH = "statisticsMonth"
    }
}
