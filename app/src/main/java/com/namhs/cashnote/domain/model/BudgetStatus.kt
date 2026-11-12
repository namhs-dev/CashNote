package com.namhs.cashnote.domain.model

/** 예산 사용 정도에 따른 경고 단계 */
enum class BudgetLevel {
    /** 80% 미만 */
    NORMAL,

    /** 80% 이상 100% 미만 */
    WARNING,

    /** 100% 이상 */
    EXCEEDED
}

/** 한 달의 예산과 지출. 예산이 있을 때만 만든다 */
data class BudgetStatus(
    /** 월 예산. 원 단위, 0보다 크다 */
    val budget: Long,
    /** 이 달 지출 합계. 원 단위 */
    val spent: Long
) {
    init {
        require(budget > 0) { "예산은 0보다 커야 한다: $budget" }
    }

    /** 사용률. 1이면 100%, 초과하면 1보다 크다. 표시용 */
    val usageRatio: Float
        get() = spent.toFloat() / budget

    /** 경계값이 소수 오차에 흔들리지 않도록 정수로 비교한다 */
    val level: BudgetLevel
        get() = when {
            spent >= budget -> BudgetLevel.EXCEEDED
            spent * 10 >= budget * 8 -> BudgetLevel.WARNING
            else -> BudgetLevel.NORMAL
        }
}
