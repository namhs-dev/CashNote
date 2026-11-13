package com.namhs.cashnote.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * 통화별 최신 환율을 저장하는 Room Entity.
 *
 * 환율 API로 받은 통화 전체 목록으로 테이블을 통째로 교체한다. 통화마다 한 행만 둔다.
 */
@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    /** 통화 코드. "USD", "JPY" 등. 기준 단위 표기("(100)")는 떼고 저장한다 */
    @PrimaryKey
    @ColumnInfo(name = "currency_code")
    val currencyCode: String,
    /** 국가/통화명 */
    @ColumnInfo(name = "currency_name")
    val currencyName: String,
    /** 환율 기준 단위. JPY·IDR은 100, 그 외 1 */
    val unit: Int,
    /** 매매 기준율. [unit]만큼의 외화가 몇 원인지 */
    @ColumnInfo(name = "deal_base_rate")
    val dealBaseRate: BigDecimal,
    /** 환율 기준 영업일 */
    @ColumnInfo(name = "rate_date")
    val rateDate: LocalDate,
    /** 이 환율을 받아 저장한 시각 */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
