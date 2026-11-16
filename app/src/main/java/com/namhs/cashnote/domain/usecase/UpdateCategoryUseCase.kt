package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.repository.CategoryRepository
import javax.inject.Inject

/** 카테고리의 이름·색상을 바꾼다. */
class UpdateCategoryUseCase @Inject constructor(private val repository: CategoryRepository) {
    suspend operator fun invoke(category: Category) = repository.update(category)
}
