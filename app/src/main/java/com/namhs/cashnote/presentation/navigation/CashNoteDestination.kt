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

    // 내역 입력·수정 화면 (탭 아님). transactionId가 0이면 새 거래, 아니면 그 거래를 수정한다
    @Serializable
    data class TransactionEdit(val transactionId: Long = 0) : CashNoteDestination
}
