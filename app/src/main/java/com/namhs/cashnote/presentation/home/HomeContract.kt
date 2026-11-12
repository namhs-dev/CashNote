package com.namhs.cashnote.presentation.home

import androidx.annotation.StringRes
import com.namhs.cashnote.domain.model.BudgetStatus
import com.namhs.cashnote.domain.model.MonthlySummary
import com.namhs.cashnote.domain.model.TransactionWithCategory
import java.time.LocalDate
import java.time.YearMonth

/** 홈 화면 상태 */
data class HomeUiState(
    /** 표시 중인 월. 이전 달·다음 달 버튼으로 바꾼다 */
    val month: YearMonth,
    val isLoading: Boolean = true,
    /** 날짜별로 묶은 거래. 날짜 최신순 */
    val dailyGroups: List<DailyTransactions> = emptyList(),
    /** 표시 중인 월의 수입·지출 합계 */
    val summary: MonthlySummary = MonthlySummary(),
    /** 표시 중인 월의 예산 대비 지출. 예산이 없으면 null */
    val budget: BudgetStatus? = null,
    /** 예산 입력 다이얼로그 표시 여부 */
    val isBudgetDialogVisible: Boolean = false
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

    /** 상단 바의 이전 달 버튼 */
    data object PreviousMonthClicked : HomeIntent

    /** 상단 바의 다음 달 버튼 */
    data object NextMonthClicked : HomeIntent

    /** 항목을 밀어 삭제 */
    data class DeleteRequested(val transactionId: Long) : HomeIntent

    /** 스낵바의 실행 취소. 마지막으로 삭제한 거래를 되돌린다 */
    data object UndoDeleteClicked : HomeIntent

    /** 요약 카드의 예산 영역. 예산 입력 다이얼로그를 연다 */
    data object BudgetClicked : HomeIntent

    /** 예산 입력 확인. 표시 중인 월부터 적용하며, 빈 값·0이면 예산을 없앤다 */
    data class BudgetSaved(val input: String) : HomeIntent

    /** 예산 입력 취소 */
    data object BudgetDialogDismissed : HomeIntent
}

/** 홈 화면의 일회성 이벤트 */
sealed interface HomeSideEffect {
    /** 입력·수정 화면으로 이동한다. 0이면 새 거래 */
    data class NavigateToEdit(val transactionId: Long) : HomeSideEffect

    /** 삭제 완료. 실행 취소 스낵바를 띄운다 */
    data object ShowDeleteUndo : HomeSideEffect

    /** 삭제·복구 실패 등 사용자에게 알릴 메시지 */
    data class ShowMessage(@StringRes val messageRes: Int) : HomeSideEffect
}
