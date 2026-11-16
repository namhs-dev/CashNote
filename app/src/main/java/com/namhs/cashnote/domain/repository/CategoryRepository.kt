package com.namhs.cashnote.domain.repository

import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    /** 전체 카테고리를 구독한다. */
    fun observeCategories(): Flow<List<Category>>

    /** 거래 유형(수입/지출)에 해당하는 카테고리를 구독한다. */
    fun observeCategories(type: TransactionType): Flow<List<Category>>

    /** 카테고리를 추가하고 저장된 id를 반환한다. [Category.id]는 무시한다. */
    suspend fun add(category: Category): Long

    /** id가 같은 카테고리의 이름·색상 등을 바꾼다. */
    suspend fun update(category: Category)
}
