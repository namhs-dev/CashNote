package com.namhs.cashnote.testing

import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * 메모리에 카테고리를 담는 가짜 Repository.
 *
 * [failure]가 있으면 추가·수정·삭제에서 이 예외를 던진다.
 */
class FakeCategoryRepository(initial: List<Category> = emptyList()) : CategoryRepository {
    /** 테스트에서 값을 바꿔 카테고리 변경을 흉내 낸다 */
    val categories = MutableStateFlow(initial)

    var failure: Exception? = null

    override fun observeCategories(): Flow<List<Category>> = categories

    override fun observeCategories(type: TransactionType): Flow<List<Category>> =
        categories.map { list -> list.filter { it.type == type } }

    override suspend fun add(category: Category): Long {
        failure?.let { throw it }
        val id = (categories.value.maxOfOrNull { it.id } ?: 0) + 1
        categories.value = categories.value + category.copy(id = id)
        return id
    }

    override suspend fun update(category: Category) {
        failure?.let { throw it }
        categories.value = categories.value.map { if (it.id == category.id) category else it }
    }

    override suspend fun delete(category: Category) {
        failure?.let { throw it }
        categories.value = categories.value.filterNot { it.id == category.id }
    }
}
