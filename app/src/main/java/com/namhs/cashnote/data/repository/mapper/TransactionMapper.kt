package com.namhs.cashnote.data.repository.mapper

import com.namhs.cashnote.data.local.entity.TransactionEntity
import com.namhs.cashnote.domain.model.Transaction

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
