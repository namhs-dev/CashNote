package com.namhs.cashnote.testing

import com.namhs.cashnote.domain.repository.BudgetRepository
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** 적용 시작 월별 예산을 메모리에 담는 가짜 Repository. 실제 쿼리처럼 그 달 이하의 최근 값을 돌려준다 */
class FakeBudgetRepository(initial: Map<YearMonth, Long> = emptyMap()) : BudgetRepository {
    private val budgets = MutableStateFlow(initial)

    /** setBudget 호출 기록 */
    val saved = mutableListOf<Pair<YearMonth, Long>>()

    /** null이 아니면 setBudget이 이 예외를 던진다 */
    var failure: Exception? = null

    override fun observeBudget(month: YearMonth): Flow<Long?> = budgets.map { map ->
        map.filterKeys { it <= month }
            .maxByOrNull { it.key }
            ?.value
            ?.takeIf { it > 0 }
    }

    override suspend fun setBudget(startMonth: YearMonth, amount: Long) {
        failure?.let { throw it }
        saved += startMonth to amount
        budgets.update { it + (startMonth to amount) }
    }
}
