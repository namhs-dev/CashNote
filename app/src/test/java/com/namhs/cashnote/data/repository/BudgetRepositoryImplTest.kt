package com.namhs.cashnote.data.repository

import com.namhs.cashnote.data.local.dao.BudgetDao
import com.namhs.cashnote.data.local.entity.BudgetEntity
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetRepositoryImplTest {
    private val dao = FakeBudgetDao()
    private val repository = BudgetRepositoryImpl(dao)

    private val november = YearMonth.of(2026, 11)

    @Test
    fun observeBudget_passesMonthAndMapsAmount() = runBlocking {
        dao.effective = BudgetEntity(YearMonth.of(2026, 10), 500_000)

        assertEquals(500_000L, repository.observeBudget(november).first())
        assertEquals(november, dao.lastMonth)
    }

    @Test
    fun observeBudget_noBudget_isNull() = runBlocking {
        assertNull(repository.observeBudget(november).first())
    }

    @Test
    fun observeBudget_zeroAmount_isNull() = runBlocking {
        // 0원 행은 "이 달부터 예산 없음"
        dao.effective = BudgetEntity(november, 0)

        assertNull(repository.observeBudget(november).first())
    }

    @Test
    fun setBudget_upsertsEntity() = runBlocking {
        repository.setBudget(november, 700_000)

        assertEquals(listOf(BudgetEntity(november, 700_000)), dao.upserted)
    }
}

/** 호출 인자를 기록하는 가짜 DAO */
private class FakeBudgetDao : BudgetDao {
    var effective: BudgetEntity? = null
    var lastMonth: YearMonth? = null
    val upserted = mutableListOf<BudgetEntity>()

    override fun observeEffective(month: YearMonth): Flow<BudgetEntity?> {
        lastMonth = month
        return flowOf(effective)
    }

    override suspend fun upsert(budget: BudgetEntity) {
        upserted += budget
    }
}
