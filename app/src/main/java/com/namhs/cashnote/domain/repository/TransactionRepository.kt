package com.namhs.cashnote.domain.repository

import com.namhs.cashnote.domain.model.MonthlySummary
import com.namhs.cashnote.domain.model.Transaction
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    /** [month]에 해당하는 거래를 최신순으로 구독한다. */
    fun observeTransactions(month: YearMonth): Flow<List<Transaction>>

    /** [month]의 수입·지출 합계를 구독한다. 거래가 없으면 0이다. */
    fun observeMonthlySummary(month: YearMonth): Flow<MonthlySummary>

    /** id로 거래를 조회한다. 없으면 null */
    suspend fun getTransaction(id: Long): Transaction?

    /**
     * 거래를 추가하고 저장된 id를 반환한다.
     * id가 0이면 새 id가 생성되고, 0이 아니면 그 id로 저장한다(삭제 실행 취소 시 사용).
     */
    suspend fun add(transaction: Transaction): Long

    suspend fun update(transaction: Transaction)

    suspend fun delete(transaction: Transaction)
}
