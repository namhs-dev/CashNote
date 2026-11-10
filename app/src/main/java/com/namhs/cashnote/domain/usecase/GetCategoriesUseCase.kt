package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.repository.CategoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 거래 유형(수입/지출)에 해당하는 카테고리 목록을 구독한다. */
class GetCategoriesUseCase @Inject constructor(private val repository: CategoryRepository) {
    operator fun invoke(type: TransactionType): Flow<List<Category>> =
        repository.observeCategories(type)
}
