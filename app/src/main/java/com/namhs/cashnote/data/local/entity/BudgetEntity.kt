package com.namhs.cashnote.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.YearMonth

/**
 * 월 예산을 저장하는 Room Entity.
 *
 * [startMonth]부터 다음 설정이 나올 때까지 이 예산이 적용된다.
 * 예산을 바꿔도 이전 달 행은 남아 있어 지난달 사용률은 그때 예산 기준으로 계산된다.
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    /** 적용 시작 월. 같은 달에 다시 설정하면 덮어쓴다 */
    @PrimaryKey
    @ColumnInfo(name = "start_month")
    val startMonth: YearMonth,
    /** 월 예산. 원 단위. 0이면 이 달부터 예산 없음 */
    val amount: Long
)
