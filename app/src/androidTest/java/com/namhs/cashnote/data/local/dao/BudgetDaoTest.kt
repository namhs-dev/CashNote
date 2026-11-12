package com.namhs.cashnote.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.namhs.cashnote.data.local.database.CashNoteDatabase
import com.namhs.cashnote.data.local.entity.BudgetEntity
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetDaoTest {
    private lateinit var database: CashNoteDatabase
    private lateinit var dao: BudgetDao

    private val october = YearMonth.of(2026, 10)
    private val november = YearMonth.of(2026, 11)
    private val december = YearMonth.of(2026, 12)

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, CashNoteDatabase::class.java).build()
        dao = database.budgetDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeEffective_noBudget_isNull() = runBlocking {
        assertNull(dao.observeEffective(november).first())
    }

    @Test
    fun observeEffective_budgetOfSameMonth() = runBlocking {
        dao.upsert(BudgetEntity(november, 500_000))

        assertEquals(BudgetEntity(november, 500_000), dao.observeEffective(november).first())
    }

    @Test
    fun observeEffective_carriesOverFromEarlierMonth() = runBlocking {
        dao.upsert(BudgetEntity(october, 400_000))

        // 11월에 따로 정하지 않았으면 10월 예산이 이어진다
        assertEquals(BudgetEntity(october, 400_000), dao.observeEffective(november).first())
    }

    @Test
    fun observeEffective_usesMostRecentAndIgnoresLaterMonths() = runBlocking {
        dao.upsert(BudgetEntity(october, 400_000))
        dao.upsert(BudgetEntity(november, 500_000))
        dao.upsert(BudgetEntity(december, 600_000))

        // 11월에는 10월보다 최근인 11월 예산, 이후인 12월 예산은 적용되지 않는다
        assertEquals(BudgetEntity(november, 500_000), dao.observeEffective(november).first())
        assertEquals(BudgetEntity(october, 400_000), dao.observeEffective(october).first())
    }

    @Test
    fun upsert_sameMonth_overwrites() = runBlocking {
        dao.upsert(BudgetEntity(november, 500_000))

        dao.upsert(BudgetEntity(november, 700_000))

        assertEquals(BudgetEntity(november, 700_000), dao.observeEffective(november).first())
    }
}
