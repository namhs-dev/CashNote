package com.namhs.cashnote.domain.model

/** 한 달의 수입·지출 합계. 원 단위 */
data class MonthlySummary(val income: Long = 0, val expense: Long = 0) {
    /** 수입 − 지출. 지출이 더 많으면 음수 */
    val balance: Long
        get() = income - expense
}
