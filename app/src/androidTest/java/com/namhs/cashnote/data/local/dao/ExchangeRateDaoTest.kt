package com.namhs.cashnote.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.namhs.cashnote.data.local.database.CashNoteDatabase
import com.namhs.cashnote.data.local.entity.ExchangeRateEntity
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExchangeRateDaoTest {
    private lateinit var database: CashNoteDatabase
    private lateinit var dao: ExchangeRateDao

    private val friday = LocalDate.of(2026, 11, 13)
    private val updatedAt = Instant.parse("2026-11-13T03:00:00Z")

    private fun rate(code: String, rate: String, unit: Int = 1, date: LocalDate = friday) =
        ExchangeRateEntity(code, "$code 통화", unit, BigDecimal(rate), date, updatedAt)

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, CashNoteDatabase::class.java).build()
        dao = database.exchangeRateDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeAll_empty() = runBlocking {
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun replaceAll_storesAndSortsByCurrencyCode() = runBlocking {
        dao.replaceAll(listOf(rate("USD", "1450.5"), rate("JPY", "935.12", unit = 100)))

        val stored = dao.observeAll().first()

        assertEquals(listOf("JPY", "USD"), stored.map { it.currencyCode })
        assertEquals(100, stored.first().unit)
    }

    @Test
    fun replaceAll_removesPreviousRows() = runBlocking {
        dao.replaceAll(listOf(rate("USD", "1450.5"), rate("EUR", "1580.2")))

        val nextDay = friday.plusDays(3)
        dao.replaceAll(listOf(rate("USD", "1460", date = nextDay)))

        assertEquals(listOf(rate("USD", "1460", date = nextDay)), dao.observeAll().first())
    }

    @Test
    fun bigDecimal_keepsScale() = runBlocking {
        dao.replaceAll(listOf(rate("USD", "1450.10")))

        assertEquals(BigDecimal("1450.10"), dao.observeAll().first().single().dealBaseRate)
    }
}
