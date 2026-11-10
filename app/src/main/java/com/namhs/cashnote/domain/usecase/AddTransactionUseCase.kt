package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.repository.TransactionRepository
import javax.inject.Inject

/** 거래를 추가하고 저장된 id를 반환한다. */
class AddTransactionUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(transaction: Transaction): Long = repository.add(transaction)
}
