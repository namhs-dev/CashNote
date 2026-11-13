package com.namhs.cashnote.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.namhs.cashnote.data.local.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {
    /** 저장된 환율 전체를 통화 코드 순으로 구독한다 */
    @Query("SELECT * FROM exchange_rates ORDER BY currency_code")
    fun observeAll(): Flow<List<ExchangeRateEntity>>

    /** 기존 환율을 모두 지우고 [rates]로 바꾼다. 한 트랜잭션이라 구독자는 중간 상태를 보지 않는다 */
    @Transaction
    suspend fun replaceAll(rates: List<ExchangeRateEntity>) {
        deleteAll()
        insertAll(rates)
    }

    @Query("DELETE FROM exchange_rates")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(rates: List<ExchangeRateEntity>)
}
