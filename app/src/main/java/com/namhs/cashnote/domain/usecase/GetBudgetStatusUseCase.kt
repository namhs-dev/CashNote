package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.BudgetStatus
import com.namhs.cashnote.domain.repository.BudgetRepository
import com.namhs.cashnote.domain.repository.TransactionRepository
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** [month]의 예산 대비 지출을 구독한다. 예산이 없으면 null */
class GetBudgetStatusUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(month: YearMonth): Flow<BudgetStatus?> = combine(
        budgetRepository.observeBudget(month),
        transactionRepository.observeMonthlySummary(month)
    ) { budget, summary ->
        budget?.let { BudgetStatus(budget = it, spent = summary.expense) }
    }
}
