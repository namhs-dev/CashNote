package com.namhs.cashnote.presentation.statistics

import com.namhs.cashnote.domain.model.CategoryExpense
import java.time.YearMonth

/** 통계 화면 상태 */
data class StatisticsUiState(
    /** 표시 중인 월. 홈과 별개로 이 화면이 기억한다 */
    val month: YearMonth,
    val isLoading: Boolean = true,
    /** 카테고리별 지출. 금액이 큰 순서 */
    val expenses: List<CategoryExpense> = emptyList()
) {
    /** 이 달의 총 지출 */
    val totalExpense: Long
        get() = expenses.sumOf { it.amount }

    /** 불러오기가 끝났는데 지출이 하나도 없다 */
    val isEmpty: Boolean
        get() = !isLoading && expenses.isEmpty()
}

/** 통계 화면에서 발생하는 사용자 의도 */
sealed interface StatisticsIntent {
    /** 상단 바의 이전 달 버튼 */
    data object PreviousMonthClicked : StatisticsIntent

    /** 상단 바의 다음 달 버튼 */
    data object NextMonthClicked : StatisticsIntent
}

/** 통계 화면의 일회성 이벤트. 지금은 없다 */
sealed interface StatisticsSideEffect
