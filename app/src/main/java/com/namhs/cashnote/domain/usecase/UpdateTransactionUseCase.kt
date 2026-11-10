package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.repository.TransactionRepository
import javax.inject.Inject

/** 기존 거래를 수정한다. */
class UpdateTransactionUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.update(transaction)
    }
}
