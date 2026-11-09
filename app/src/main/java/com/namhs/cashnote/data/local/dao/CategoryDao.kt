package com.namhs.cashnote.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.namhs.cashnote.data.local.entity.CategoryEntity
import com.namhs.cashnote.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    /** 전체 카테고리를 id 순으로 구독한다. */
    @Query("SELECT * FROM categories ORDER BY id")
    fun observeAll(): Flow<List<CategoryEntity>>

    /** 거래 유형(수입/지출)에 해당하는 카테고리를 id 순으로 구독한다. */
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY id")
    fun observeByType(type: TransactionType): Flow<List<CategoryEntity>>
}
