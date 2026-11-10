package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.repository.TransactionRepository
import javax.inject.Inject

/** 거래를 삭제한다. 실행 취소는 같은 거래를 [AddTransactionUseCase]로 다시 추가한다. */
class DeleteTransactionUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.delete(transaction)
    }
}
