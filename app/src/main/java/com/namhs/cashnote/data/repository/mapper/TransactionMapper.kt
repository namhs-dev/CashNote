package com.namhs.cashnote.data.repository.mapper

import com.namhs.cashnote.data.local.dao.CategoryTotal
import com.namhs.cashnote.data.local.dao.TypeTotal
import com.namhs.cashnote.data.local.entity.TransactionEntity
import com.namhs.cashnote.domain.model.CategoryAmount
import com.namhs.cashnote.domain.model.MonthlySummary
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType

// Entity ↔ Domain 변환. 거래는 저장·수정·삭제에 모두 쓰이므로 양방향으로 둔다.

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amount = amount,
    type = type,
    categoryId = categoryId,
    memo = memo,
    date = date,
    createdAt = createdAt
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amount = amount,
    type = type,
    categoryId = categoryId,
    memo = memo,
    date = date,
    createdAt = createdAt
)

// 유형별 합계 → 월 요약. 쿼리 결과에 없는 유형은 거래가 없다는 뜻이므로 0이다
fun List<TypeTotal>.toMonthlySummary(): MonthlySummary {
    val totalByType = associate { it.type to it.total }
    return MonthlySummary(
        income = totalByType[TransactionType.INCOME] ?: 0,
        expense = totalByType[TransactionType.EXPENSE] ?: 0
    )
}

fun CategoryTotal.toDomain(): CategoryAmount =
    CategoryAmount(categoryId = categoryId, amount = total)
