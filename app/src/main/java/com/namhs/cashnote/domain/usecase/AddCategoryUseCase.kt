package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.repository.CategoryRepository
import javax.inject.Inject

/** 카테고리를 추가하고 저장된 id를 반환한다. */
class AddCategoryUseCase @Inject constructor(private val repository: CategoryRepository) {
    suspend operator fun invoke(category: Category): Long = repository.add(category)
}
