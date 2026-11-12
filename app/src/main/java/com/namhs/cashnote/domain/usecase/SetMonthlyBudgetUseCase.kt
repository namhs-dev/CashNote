package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.repository.BudgetRepository
import java.time.YearMonth
import javax.inject.Inject

/** [month]부터 적용할 월 예산을 저장한다. 0이면 그 달부터 예산을 없앤다 */
class SetMonthlyBudgetUseCase @Inject constructor(private val repository: BudgetRepository) {
    suspend operator fun invoke(month: YearMonth, amount: Long) {
        require(amount >= 0) { "예산은 음수일 수 없다: $amount" }
        repository.setBudget(month, amount)
    }
}
