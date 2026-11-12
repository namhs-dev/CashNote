package com.namhs.cashnote.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.namhs.cashnote.data.local.entity.TransactionEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    /**
     * [start] 이상 [endExclusive] 미만 날짜의 거래를 최신순으로 구독한다.
     * 월 → 날짜 범위 변환은 Repository에서 한다.
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE date >= :start AND date < :endExclusive
        ORDER BY date DESC, created_at DESC, id DESC
        """
    )
    fun observeByDateRange(start: LocalDate, endExclusive: LocalDate): Flow<List<TransactionEntity>>

    /**
     * [start] 이상 [endExclusive] 미만 날짜의 거래를 유형별로 합산해 구독한다.
     * 거래가 없는 유형은 결과에 나오지 않는다.
     */
    @Query(
        """
        SELECT type, SUM(amount) AS total FROM transactions
        WHERE date >= :start AND date < :endExclusive
        GROUP BY type
        """
    )
    fun observeTotalsByType(start: LocalDate, endExclusive: LocalDate): Flow<List<TypeTotal>>

    /** id로 거래 하나를 조회한다. 없으면 null */
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    /** 거래를 추가하고 생성된 id를 반환한다. */
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    /** 실행 취소 시 다시 넣을 수 있도록 Entity 전체를 받는다. */
    @Delete
    suspend fun delete(transaction: TransactionEntity)
}
