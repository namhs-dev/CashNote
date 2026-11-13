package com.namhs.cashnote.data.repository

import com.namhs.cashnote.data.local.dao.ExchangeRateDao
import com.namhs.cashnote.data.local.entity.ExchangeRateEntity
import com.namhs.cashnote.data.remote.ExchangeRateApiException
import com.namhs.cashnote.data.remote.ExchangeRateRemoteDataSource
import com.namhs.cashnote.data.remote.api.ExchangeRateApi
import com.namhs.cashnote.data.remote.dto.ExchangeRateDto
import com.namhs.cashnote.domain.model.ExchangeRate
import java.io.IOException
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExchangeRateRepositoryImplTest {
    // 한국 시간 2026-11-15(일) 12:00
    private val now = Instant.parse("2026-11-15T03:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("Asia/Seoul"))

    private val friday = LocalDate.of(2026, 11, 13)
    private val oldUpdatedAt = Instant.parse("2026-11-12T03:00:00Z")
    private val cachedUsd =
        ExchangeRateEntity("USD", "미국 달러", 1, BigDecimal("1440"), friday.minusDays(1), oldUpdatedAt)

    private val dao = FakeExchangeRateDao(listOf(cachedUsd))
    private val api = FakeExchangeRateApi()
    private val repository = ExchangeRateRepositoryImpl(
        dao,
        ExchangeRateRemoteDataSource(api, authKey = "key"),
        clock
    )

    private fun dto(unit: String, rate: String, name: String = unit) =
        ExchangeRateDto(result = 1, currencyUnit = unit, currencyName = name, dealBaseRate = rate)

    @Test
    fun observeExchangeRates_mapsEntitiesToDomain() = runBlocking {
        val expected =
            ExchangeRate("USD", "미국 달러", 1, BigDecimal("1440"), friday.minusDays(1), oldUpdatedAt)

        assertEquals(listOf(expected), repository.observeExchangeRates().first())
    }

    @Test
    fun refresh_replacesCacheWithLatestBusinessDay() = runBlocking {
        // 일요일 기준 → 토요일 빈 응답 → 금요일 데이터
        api.responses["20261113"] = listOf(dto("USD", "1,450.5"), dto("JPY(100)", "935.12"))

        repository.refresh()

        assertEquals(listOf("20261115", "20261114", "20261113"), api.requestedDates)
        val stored = repository.observeExchangeRates().first()
        assertEquals(listOf("USD", "JPY"), stored.map { it.currencyCode })
        assertTrue(stored.all { it.rateDate == friday && it.updatedAt == now })
        assertEquals(BigDecimal("1450.5"), stored[0].baseRate)
        assertEquals(100, stored[1].unit)
    }

    @Test
    fun refresh_skipsInvalidItems() = runBlocking {
        api.responses["20261115"] = listOf(dto("USD", "1450.5"), dto("???", "1"), dto("EUR", "-"))

        repository.refresh()

        assertEquals(listOf("USD"), dao.rates.value.map { it.currencyCode })
        assertEquals(BigDecimal("1450.5"), dao.rates.value.single().dealBaseRate)
    }

    @Test
    fun refresh_noDataWithinLookback_keepsCache() = runBlocking {
        repository.refresh()

        assertEquals(listOf(cachedUsd), dao.rates.value)
        assertEquals(0, dao.replaceCount)
    }

    @Test
    fun refresh_onlyInvalidItems_keepsCache() = runBlocking {
        api.responses["20261115"] = listOf(dto("USD", "-"))

        repository.refresh()

        assertEquals(listOf(cachedUsd), dao.rates.value)
        assertEquals(0, dao.replaceCount)
    }

    @Test
    fun refresh_apiError_throwsAndKeepsCache() = runBlocking {
        api.responses["20261115"] = listOf(ExchangeRateDto(result = 4))

        try {
            repository.refresh()
            fail("예외가 발생해야 한다")
        } catch (e: ExchangeRateApiException) {
            assertEquals(4, e.resultCode)
        }
        assertEquals(listOf(cachedUsd), dao.rates.value)
    }

    @Test
    fun refresh_networkError_throwsAndKeepsCache() = runBlocking {
        api.failure = IOException("offline")

        try {
            repository.refresh()
            fail("예외가 발생해야 한다")
        } catch (e: IOException) {
            assertEquals("offline", e.message)
        }
        assertEquals(listOf(cachedUsd), dao.rates.value)
    }
}

/** 메모리에 환율 목록을 두는 가짜 DAO */
private class FakeExchangeRateDao(initial: List<ExchangeRateEntity>) : ExchangeRateDao {
    val rates = MutableStateFlow(initial)
    var replaceCount = 0

    override fun observeAll(): Flow<List<ExchangeRateEntity>> = rates

    override suspend fun replaceAll(rates: List<ExchangeRateEntity>) {
        replaceCount++
        this.rates.value = rates
    }

    override suspend fun deleteAll() {
        rates.value = emptyList()
    }

    override suspend fun insertAll(rates: List<ExchangeRateEntity>) {
        this.rates.value += rates
    }
}

/** 날짜(yyyyMMdd)별 응답을 돌려주는 가짜 API. 없는 날짜는 빈 배열 */
private class FakeExchangeRateApi : ExchangeRateApi {
    val responses = mutableMapOf<String, List<ExchangeRateDto>>()
    val requestedDates = mutableListOf<String?>()
    var failure: Exception? = null

    override suspend fun getExchangeRates(
        authKey: String,
        searchDate: String?,
        dataType: String
    ): List<ExchangeRateDto> {
        failure?.let { throw it }
        requestedDates += searchDate
        return responses[searchDate].orEmpty()
    }
}
