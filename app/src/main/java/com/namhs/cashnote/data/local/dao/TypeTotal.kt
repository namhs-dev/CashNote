package com.namhs.cashnote.data.local.dao

import com.namhs.cashnote.domain.model.TransactionType

/** 유형별 합계 쿼리 결과. 테이블이 아니라 [TransactionDao.observeTotalsByType]의 결과 모양이다 */
data class TypeTotal(val type: TransactionType, val total: Long)
