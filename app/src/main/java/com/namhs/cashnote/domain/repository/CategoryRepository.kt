package com.namhs.cashnote.domain.repository

import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    /** 전체 카테고리를 구독한다. */
    fun observeCategories(): Flow<List<Category>>

    /** 거래 유형(수입/지출)에 해당하는 카테고리를 구독한다. */
    fun observeCategories(type: TransactionType): Flow<List<Category>>
}
