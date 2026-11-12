package com.namhs.cashnote.data.repository

import com.namhs.cashnote.data.local.dao.TransactionDao
import com.namhs.cashnote.data.repository.mapper.toDomain
import com.namhs.cashnote.data.repository.mapper.toEntity
import com.namhs.cashnote.data.repository.mapper.toMonthlySummary
import com.namhs.cashnote.domain.model.MonthlySummary
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.repository.TransactionRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl @Inject constructor(private val transactionDao: TransactionDao) :
    TransactionRepository {
    override fun observeTransactions(month: YearMonth): Flow<List<Transaction>> {
        val (start, endExclusive) = month.toDateRange()
        return transactionDao.observeByDateRange(start, endExclusive)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeMonthlySummary(month: YearMonth): Flow<MonthlySummary> {
        val (start, endExclusive) = month.toDateRange()
        return transactionDao.observeTotalsByType(start, endExclusive)
            .map { totals -> totals.toMonthlySummary() }
    }

    override suspend fun getTransaction(id: Long): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override suspend fun add(transaction: Transaction): Long =
        transactionDao.insert(transaction.toEntity())

    override suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity())
    }
}

// 해당 월 1일 이상, 다음 달 1일 미만
private fun YearMonth.toDateRange(): Pair<LocalDate, LocalDate> = atDay(1) to plusMonths(1).atDay(1)
