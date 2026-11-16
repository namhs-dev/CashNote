package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.CategoryDeleteResult
import com.namhs.cashnote.domain.repository.CategoryRepository
import com.namhs.cashnote.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * 카테고리를 지운다. 이 카테고리를 쓰는 거래가 있으면 지우지 않고 그 건수를 돌려준다.
 *
 * 거래를 다른 카테고리로 옮기지 않는 이유: 사용자가 모르게 거래 분류가 바뀌지 않도록 한다.
 */
class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(category: Category): CategoryDeleteResult {
        val count = transactionRepository.countByCategory(category.id)
        if (count > 0) return CategoryDeleteResult.InUse(count)
        categoryRepository.delete(category)
        return CategoryDeleteResult.Deleted
    }
}
