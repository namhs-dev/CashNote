package com.namhs.cashnote.domain.repository

import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    /**
     * [month]에 적용되는 월 예산을 구독한다.
     * 그 달 이하에서 가장 최근에 설정한 값이며, 설정한 적이 없거나 0이면 null이다.
     */
    fun observeBudget(month: YearMonth): Flow<Long?>

    /** [startMonth]부터 적용할 예산을 저장한다. 0이면 그 달부터 예산 없음 */
    suspend fun setBudget(startMonth: YearMonth, amount: Long)
}
