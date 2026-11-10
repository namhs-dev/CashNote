package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.repository.TransactionRepository
import javax.inject.Inject

/** id로 거래를 조회한다. 없으면 null */
class GetTransactionUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(id: Long): Transaction? = repository.getTransaction(id)
}
