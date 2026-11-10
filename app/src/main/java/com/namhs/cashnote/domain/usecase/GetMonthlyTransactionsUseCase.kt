package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.repository.TransactionRepository
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** [month]의 거래 목록을 최신순으로 구독한다. */
class GetMonthlyTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(month: YearMonth): Flow<List<Transaction>> =
        repository.observeTransactions(month)
}
