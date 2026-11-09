package com.namhs.cashnote.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.namhs.cashnote.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate

/**
 * 거래 한 건을 저장하는 Room Entity.
 *
 * 외화 관련 필드는 M4에서 Room migration으로 추가한다.
 * 거래가 남아 있는 카테고리는 삭제할 수 없다(RESTRICT).
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("category_id")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 금액. 원 단위 정수로 저장하며 부동소수점은 사용하지 않는다. */
    val amount: Long,
    /** 수입/지출 구분 */
    val type: TransactionType,
    /** 카테고리 id. categories.id를 참조하는 외래 키 */
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
