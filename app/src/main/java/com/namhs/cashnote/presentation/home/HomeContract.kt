package com.namhs.cashnote.presentation.home

import com.namhs.cashnote.domain.model.TransactionWithCategory
import java.time.LocalDate
import java.time.YearMonth

/** 홈 화면 상태 */
data class HomeUiState(
    /** 표시 중인 월. 월 이동은 #30에서 추가한다 */
    val month: YearMonth,
    val isLoading: Boolean = true,
    /** 날짜별로 묶은 거래. 날짜 최신순 */
    val dailyGroups: List<DailyTransactions> = emptyList()
) {
    /** 불러오기가 끝났는데 거래가 하나도 없다 */
    val isEmpty: Boolean
        get() = !isLoading && dailyGroups.isEmpty()
}

/** 같은 날짜의 거래 묶음 */
data class DailyTransactions(val date: LocalDate, val items: List<TransactionWithCategory>)

/** 홈 화면에서 발생하는 사용자 의도 */
sealed interface HomeIntent {
    data object AddClicked : HomeIntent

    data class TransactionClicked(val transactionId: Long) : HomeIntent
}

/** 홈 화면의 일회성 이벤트 */
sealed interface HomeSideEffect {
    /** 입력·수정 화면으로 이동한다. 0이면 새 거래 */
    data class NavigateToEdit(val transactionId: Long) : HomeSideEffect
}
