package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.TransactionWithCategory
import com.namhs.cashnote.domain.repository.CategoryRepository
import com.namhs.cashnote.domain.repository.TransactionRepository
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** [month]의 거래 목록을 카테고리 정보와 함께 최신순으로 구독한다. */
class GetMonthlyTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(month: YearMonth): Flow<List<TransactionWithCategory>> = combine(
        transactionRepository.observeTransactions(month),
        categoryRepository.observeCategories()
    ) { transactions, categories ->
        val categoryById = categories.associateBy { it.id }
        // 두 Flow 갱신 시점이 어긋나 카테고리가 아직 없는 거래는 건너뛴다
        transactions.mapNotNull { transaction ->
            categoryById[transaction.categoryId]?.let { category ->
                TransactionWithCategory(transaction, category)
            }
        }
    }
}
