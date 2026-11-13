package com.namhs.cashnote.data.remote

import com.namhs.cashnote.data.remote.api.ExchangeRateApi
import com.namhs.cashnote.di.NetworkModule
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

/**
 * 빈 응답·오류 응답 처리를 실제 HTTP(MockWebServer)를 거쳐 검증한다.
 * 서버는 searchdate별로 정해 둔 응답을 돌려주고, 정하지 않은 날짜는 빈 배열(비영업일)을 돌려준다.
 */
class ExchangeRateRemoteDataSourceHttpTest {
    private lateinit var server: MockWebServer
    private lateinit var dataSource: ExchangeRateRemoteDataSource

    /** searchdate(yyyyMMdd) → 응답 */
    private val responses = mutableMapOf<String, MockResponse>()

    private val sunday = LocalDate.of(2026, 11, 15)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                responses[request.url.queryParameter("searchdate")] ?: json("[]")
        }
        server.start()
        val retrofit = NetworkModule.createRetrofit(
            baseUrl = server.url("/").toString(),
            okHttpClient = NetworkModule.provideOkHttpClient(),
            json = NetworkModule.provideJson()
        )
        dataSource = ExchangeRateRemoteDataSource(
            api = retrofit.create(ExchangeRateApi::class.java),
            authKey = "test-key"
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun json(body: String, code: Int = 200) =
        MockResponse.Builder().code(code).body(body).build()

    private fun requestedDates(): List<String?> =
        List(server.requestCount) { server.takeRequest().url.queryParameter("searchdate") }

    @Test
    fun weekend_fallsBackToFridayOverHttp() = runBlocking {
        responses["20261113"] = json(
            """[{"result":1,"cur_unit":"USD","deal_bas_r":"1,450.5","cur_nm":"미국 달러"}]"""
        )

        val result = dataSource.fetchLatest(sunday)

        assertEquals(LocalDate.of(2026, 11, 13), result?.date)
        assertEquals("USD", result?.rates?.single()?.currencyUnit)
        assertEquals(listOf("20261115", "20261114", "20261113"), requestedDates())
    }

    @Test
    fun allEmptyWithinLookback_returnsNull() = runBlocking {
        val result = dataSource.fetchLatest(sunday)

        assertNull(result)
        assertEquals(ExchangeRateRemoteDataSource.MAX_LOOKBACK_DAYS, server.requestCount)
    }

    @Test
    fun errorResult_throwsAndStopsLookback() = runBlocking {
        // 토요일 빈 응답 뒤 금요일에 일일 제한 초과(result=4)
        responses["20261113"] = json("""[{"result":4}]""")

        val error = runCatching { dataSource.fetchLatest(sunday) }.exceptionOrNull()

        assertTrue(error is ExchangeRateApiException)
        assertEquals(4, (error as ExchangeRateApiException).resultCode)
        // 오류가 나면 더 이전 날짜는 조회하지 않는다
        assertEquals(listOf("20261115", "20261114", "20261113"), requestedDates())
    }

    @Test
    fun invalidAuthKey_throwsResultThree() = runBlocking {
        responses["20261115"] = json("""[{"result":3}]""")

        val error = runCatching { dataSource.fetchLatest(sunday) }.exceptionOrNull()

        assertEquals(3, (error as ExchangeRateApiException).resultCode)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun httpError_propagatesHttpException() = runBlocking {
        responses["20261114"] = json("Service Unavailable", code = 503)

        val error = runCatching { dataSource.fetchLatest(sunday) }.exceptionOrNull()

        assertTrue(error is HttpException)
        assertEquals(503, (error as HttpException).code())
        assertEquals(listOf("20261115", "20261114"), requestedDates())
    }
}
