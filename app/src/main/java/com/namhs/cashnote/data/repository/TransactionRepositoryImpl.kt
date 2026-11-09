package com.namhs.cashnote.data.repository

import com.namhs.cashnote.data.local.dao.TransactionDao
import com.namhs.cashnote.data.repository.mapper.toDomain
import com.namhs.cashnote.data.repository.mapper.toEntity
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.repository.TransactionRepository
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl @Inject constructor(private val transactionDao: TransactionDao) :
    TransactionRepository {
    override fun observeTransactions(month: YearMonth): Flow<List<Transaction>> {
        // 해당 월 1일 이상, 다음 달 1일 미만
        val start = month.atDay(1)
        val endExclusive = month.plusMonths(1).atDay(1)
        return transactionDao.observeByDateRange(start, endExclusive)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun add(transaction: Transaction): Long =
        transactionDao.insert(transaction.toEntity())

    override suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity())
    }
}
