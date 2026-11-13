package com.namhs.cashnote.data.remote.api

import com.namhs.cashnote.data.remote.dto.toRateOrNull
import com.namhs.cashnote.di.NetworkModule
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

/**
 * MockWebServer로 실제 HTTP 응답을 흉내 내 Retrofit·OkHttp·kotlinx.serialization 경로를 검증한다.
 * Retrofit 구성은 앱과 같은 NetworkModule 설정을 쓴다.
 */
class ExchangeRateApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ExchangeRateApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val retrofit = NetworkModule.createRetrofit(
            baseUrl = server.url("/").toString(),
            okHttpClient = NetworkModule.provideOkHttpClient(),
            json = NetworkModule.provideJson()
        )
        api = retrofit.create(ExchangeRateApi::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueueJson(body: String, code: Int = 200) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun request_hasPathAndQueryParameters() = runBlocking {
        enqueueJson("[]")

        api.getExchangeRates(authKey = "test-key", searchDate = "20261113")

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/site/program/financial/exchangeJSON", request.url.encodedPath)
        assertEquals("test-key", request.url.queryParameter("authkey"))
        assertEquals("20261113", request.url.queryParameter("searchdate"))
        assertEquals("AP01", request.url.queryParameter("data"))
    }

    @Test
    fun request_withoutSearchDate_omitsParameter() = runBlocking {
        enqueueJson("[]")

        api.getExchangeRates(authKey = "test-key")

        assertNull(server.takeRequest().url.queryParameter("searchdate"))
    }

    @Test
    fun successResponse_parsesRealFormat() = runBlocking {
        // 실제 API 응답 형식(2026-11-13): 쉼표가 든 숫자 문자열, 한글 통화명, 쓰지 않는 필드
        enqueueJson(
            """
            [{"result":1,"cur_unit":"USD","ttb":"1,436.0","tts":"1,465.0","deal_bas_r":"1,450.5",
              "bkpr":"1,450","yy_efee_r":"0","ten_dd_efee_r":"0","kftc_bkpr":"1,450",
              "kftc_deal_bas_r":"1,450.5","cur_nm":"미국 달러"},
             {"result":1,"cur_unit":"JPY(100)","ttb":"925.76","tts":"944.47","deal_bas_r":"935.12",
              "bkpr":"935","yy_efee_r":"0","ten_dd_efee_r":"0","kftc_bkpr":"935",
              "kftc_deal_bas_r":"935.12","cur_nm":"일본 옌"}]
            """.trimIndent()
        )

        val rates = api.getExchangeRates(authKey = "test-key", searchDate = "20261113")

        assertEquals(listOf("USD", "JPY(100)"), rates.map { it.currencyUnit })
        assertEquals(listOf("미국 달러", "일본 옌"), rates.map { it.currencyName })
        assertEquals(BigDecimal("1450.5"), rates[0].dealBaseRate?.toRateOrNull())
        assertEquals(BigDecimal("935.12"), rates[1].dealBaseRate?.toRateOrNull())
    }

    @Test
    fun emptyResponse_returnsEmptyList() = runBlocking {
        // 영업일이 아니거나 오전 11시 이전
        enqueueJson("[]")

        assertTrue(api.getExchangeRates(authKey = "test-key", searchDate = "20261115").isEmpty())
    }

    @Test
    fun errorResult_parsesResultCodeWithoutOtherFields() = runBlocking {
        // 인증 오류도 HTTP 200에 result != 1인 항목으로 온다
        enqueueJson("""[{"result":3}]""")

        val rate = api.getExchangeRates(authKey = "wrong-key").single()

        assertEquals(3, rate.result)
        assertNull(rate.currencyUnit)
        assertNull(rate.dealBaseRate)
    }

    @Test
    fun httpError_throwsHttpException() = runBlocking {
        enqueueJson("Internal Server Error", code = 500)

        val error = runCatching { api.getExchangeRates(authKey = "test-key") }.exceptionOrNull()

        assertTrue(error is HttpException)
        assertEquals(500, (error as HttpException).code())
    }

    @Test
    fun malformedJson_throwsSerializationException() = runBlocking {
        // 점검 중 HTML 페이지 등 JSON이 아닌 응답
        enqueueJson("<html>점검 중</html>")

        val error = runCatching { api.getExchangeRates(authKey = "test-key") }.exceptionOrNull()

        assertTrue(error is SerializationException)
    }
}
