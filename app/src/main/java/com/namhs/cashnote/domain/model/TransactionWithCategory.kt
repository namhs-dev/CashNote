package com.namhs.cashnote.domain.model

/** 목록 표시용: 거래와 그 카테고리 */
data class TransactionWithCategory(val transaction: Transaction, val category: Category)
