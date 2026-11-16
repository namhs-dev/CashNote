package com.namhs.cashnote.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
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

    /** 새 카테고리를 넣고 만들어진 id를 반환한다. */
    @Insert
    suspend fun insert(category: CategoryEntity): Long

    /** id가 같은 카테고리를 바꾼다. */
    @Update
    suspend fun update(category: CategoryEntity)

    /** 카테고리를 지운다. 이 카테고리를 쓰는 거래가 있으면 외래 키(RESTRICT) 때문에 예외가 난다. */
    @Delete
    suspend fun delete(category: CategoryEntity)
}
