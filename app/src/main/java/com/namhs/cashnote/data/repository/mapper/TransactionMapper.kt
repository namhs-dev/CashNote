package com.namhs.cashnote.data.repository.mapper

import com.namhs.cashnote.data.local.dao.CategoryTotal
import com.namhs.cashnote.data.local.dao.TypeTotal
import com.namhs.cashnote.data.local.entity.TransactionEntity
import com.namhs.cashnote.domain.model.CategoryAmount
import com.namhs.cashnote.domain.model.ForeignAmount
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
    createdAt = createdAt,
    foreign = toForeignAmount()
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amount = amount,
    type = type,
    categoryId = categoryId,
    memo = memo,
    date = date,
    createdAt = createdAt,
    currencyCode = foreign?.currencyCode,
    foreignAmount = foreign?.amount,
    appliedRate = foreign?.appliedRate
)

// 외화 컬럼 셋이 모두 있어야 외화 거래로 본다. 하나라도 비면 원화 거래로 취급한다
private fun TransactionEntity.toForeignAmount(): ForeignAmount? {
    val code = currencyCode ?: return null
    val amount = foreignAmount ?: return null
    val rate = appliedRate ?: return null
    return ForeignAmount(currencyCode = code, amount = amount, appliedRate = rate)
}

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
