package com.namhs.cashnote.data.local.dao

/** 카테고리별 합계 쿼리 결과. 테이블이 아니라 [TransactionDao.observeTotalsByCategory]의 결과 모양이다 */
data class CategoryTotal(val categoryId: Long, val total: Long)
