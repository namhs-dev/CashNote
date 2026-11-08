package com.namhs.cashnote.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.namhs.cashnote.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate

/**
 * 거래 한 건을 저장하는 Room Entity.
 *
 * 외화 관련 필드는 M4에서 Room migration으로 추가한다.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 금액. 원 단위 정수로 저장하며 부동소수점은 사용하지 않는다. */
    val amount: Long,
    /** 수입/지출 구분 */
    val type: TransactionType,
    /** 카테고리 id. 외래 키와 인덱스는 Category Entity와 함께 추가한다. */
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    /** 메모. 입력하지 않으면 빈 문자열 */
    val memo: String = "",
    /** 거래일 */
    val date: LocalDate,
    /** 생성 시각 */
    @ColumnInfo(name = "created_at")
    val createdAt: Instant
)
