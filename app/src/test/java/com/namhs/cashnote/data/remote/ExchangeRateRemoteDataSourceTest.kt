package com.namhs.cashnote.data.remote

import com.namhs.cashnote.data.remote.api.ExchangeRateApi
import com.namhs.cashnote.data.remote.dto.ExchangeRateDto
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExchangeRateRemoteDataSourceTest {
    private val usd = ExchangeRateDto(result = 1, currencyUnit = "USD", dealBaseRate = "1,450.5")

    /** 날짜(yyyyMMdd)별 응답을 돌려주고 요청 날짜를 기록한다. 없는 날짜는 빈 배열 */
    private class FakeExchangeRateApi(
        private val responses: Map<String, List<ExchangeRateDto>> = emptyMap()
    ) : ExchangeRateApi {
        val requestedDates = mutableListOf<String?>()

        override suspend fun getExchangeRates(
            authKey: String,
            searchDate: String?,
            dataType: String
        ): List<ExchangeRateDto> {
            requestedDates += searchDate
            return responses[searchDate].orEmpty()
        }
    }

    @Test
    fun businessDay_returnsBaseDate() = runTest {
        val api = FakeExchangeRateApi(mapOf("20261113" to listOf(usd)))
        val dataSource = ExchangeRateRemoteDataSource(api, authKey = "key")

        val result = dataSource.fetchLatest(LocalDate.of(2026, 11, 13))

        assertEquals(LocalDate.of(2026, 11, 13), result?.date)
        assertEquals(listOf(usd), result?.rates)
        assertEquals(listOf("20261113"), api.requestedDates)
    }

    @Test
    fun weekend_fallsBackToPreviousBusinessDay() = runTest {
        // 일요일(11/15) → 토요일(11/14) 빈 응답 → 금요일(11/13)
        val api = FakeExchangeRateApi(mapOf("20261113" to listOf(usd)))
        val dataSource = ExchangeRateRemoteDataSource(api, authKey = "key")

        val result = dataSource.fetchLatest(LocalDate.of(2026, 11, 15))

        assertEquals(LocalDate.of(2026, 11, 13), result?.date)
        assertEquals(listOf("20261115", "20261114", "20261113"), api.requestedDates)
    }

    @Test
    fun noDataWithinLookback_returnsNull() = runTest {
        val api = FakeExchangeRateApi()
        val dataSource = ExchangeRateRemoteDataSource(api, authKey = "key")

        val result = dataSource.fetchLatest(LocalDate.of(2026, 11, 13))

        assertNull(result)
        assertEquals(ExchangeRateRemoteDataSource.MAX_LOOKBACK_DAYS, api.requestedDates.size)
    }

    @Test
    fun errorResult_throwsWithResultCode() = runTest {
        val api = FakeExchangeRateApi(mapOf("20261113" to listOf(ExchangeRateDto(result = 4))))
        val dataSource = ExchangeRateRemoteDataSource(api, authKey = "key")

        try {
            dataSource.fetchLatest(LocalDate.of(2026, 11, 13))
            fail("예외가 발생해야 한다")
        } catch (e: ExchangeRateApiException) {
            assertEquals(4, e.resultCode)
        }
    }

    @Test
    fun blankApiKey_throwsWithoutCallingApi() = runTest {
        val api = FakeExchangeRateApi(mapOf("20261113" to listOf(usd)))
        val dataSource = ExchangeRateRemoteDataSource(api, authKey = "")

        try {
            dataSource.fetchLatest(LocalDate.of(2026, 11, 13))
            fail("예외가 발생해야 한다")
        } catch (e: ExchangeRateApiException) {
            assertEquals(ExchangeRateRemoteDataSource.RESULT_INVALID_AUTH, e.resultCode)
        }
        assertTrue(api.requestedDates.isEmpty())
    }
}
