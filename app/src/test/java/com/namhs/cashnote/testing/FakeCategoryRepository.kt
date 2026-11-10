package com.namhs.cashnote.testing

import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** 메모리에 카테고리를 담는 가짜 Repository */
class FakeCategoryRepository(initial: List<Category> = emptyList()) : CategoryRepository {
    /** 테스트에서 값을 바꿔 카테고리 변경을 흉내 낸다 */
    val categories = MutableStateFlow(initial)

    override fun observeCategories(): Flow<List<Category>> = categories

    override fun observeCategories(type: TransactionType): Flow<List<Category>> =
        categories.map { list -> list.filter { it.type == type } }
}
