package com.namhs.cashnote.presentation.navigation

import kotlinx.serialization.Serializable

// type-safe navigation 라우트. 화면 이동 인자는 이 객체의 프로퍼티로 넘긴다
sealed interface CashNoteDestination {
    // 하단 탭: 홈(월별 내역)
    @Serializable
    data object Home : CashNoteDestination

    // 하단 탭: 통계
    @Serializable
    data object Statistics : CashNoteDestination

    // 홈 FAB에서 여는 내역 입력 화면 (탭 아님)
    @Serializable
    data object TransactionEdit : CashNoteDestination
}
