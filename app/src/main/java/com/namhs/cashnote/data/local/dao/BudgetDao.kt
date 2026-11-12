package com.namhs.cashnote.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.namhs.cashnote.data.local.entity.BudgetEntity
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    /** [month]에 적용되는 예산(그 달 이하에서 가장 최근에 설정한 것)을 구독한다. 없으면 null */
    @Query(
        """
        SELECT * FROM budgets
        WHERE start_month <= :month
        ORDER BY start_month DESC
        LIMIT 1
        """
    )
    fun observeEffective(month: YearMonth): Flow<BudgetEntity?>

    /** 예산을 저장한다. 같은 적용 시작 월이 있으면 덮어쓴다 */
    @Upsert
    suspend fun upsert(budget: BudgetEntity)
}
