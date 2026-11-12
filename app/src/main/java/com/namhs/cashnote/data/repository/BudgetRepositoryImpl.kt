package com.namhs.cashnote.data.repository

import com.namhs.cashnote.data.local.dao.BudgetDao
import com.namhs.cashnote.data.repository.mapper.budgetEntityOf
import com.namhs.cashnote.data.repository.mapper.toBudgetAmount
import com.namhs.cashnote.domain.repository.BudgetRepository
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl @Inject constructor(private val budgetDao: BudgetDao) :
    BudgetRepository {
    override fun observeBudget(month: YearMonth): Flow<Long?> =
        budgetDao.observeEffective(month).map { it.toBudgetAmount() }

    override suspend fun setBudget(startMonth: YearMonth, amount: Long) {
        budgetDao.upsert(budgetEntityOf(startMonth, amount))
    }
}
