package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.CategoryExpense
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.repository.CategoryRepository
import com.namhs.cashnote.domain.repository.TransactionRepository
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** [month]의 카테고리별 지출과 비율을 금액이 큰 순서로 구독한다. 지출이 없으면 빈 리스트 */
class GetCategoryExpensesUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(month: YearMonth): Flow<List<CategoryExpense>> = combine(
        transactionRepository.observeCategoryTotals(month, TransactionType.EXPENSE),
        categoryRepository.observeCategories()
    ) { totals, categories ->
        val categoryById = categories.associateBy { it.id }
        // 카테고리를 찾은 항목만 남겨, 그 합계를 분모로 써야 비율의 합이 1이 된다
        val matched = totals.mapNotNull { total ->
            categoryById[total.categoryId]?.let { category -> category to total.amount }
        }
        val sum = matched.sumOf { (_, amount) -> amount }
        if (sum == 0L) return@combine emptyList()
        matched.map { (category, amount) ->
            CategoryExpense(category, amount, ratio = amount.toFloat() / sum)
        }
    }
}
