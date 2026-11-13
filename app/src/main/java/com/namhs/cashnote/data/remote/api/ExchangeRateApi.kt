package com.namhs.cashnote.data.remote.api

import com.namhs.cashnote.data.remote.dto.ExchangeRateDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 한국수출입은행 현재환율 API.
 *
 * 영업일이 아니거나 당일 오전 11시 이전이면 빈 배열이 온다.
 * 인증 오류 등은 HTTP 200에 result != 1인 항목으로 온다.
 */
interface ExchangeRateApi {
    /**
     * @param authKey 발급받은 인증키
     * @param searchDate 조회 날짜(yyyyMMdd). null이면 오늘
     * @param dataType 조회 종류. AP01 = 환율
     */
    @GET("site/program/financial/exchangeJSON")
    suspend fun getExchangeRates(
        @Query("authkey") authKey: String,
        @Query("searchdate") searchDate: String? = null,
        @Query("data") dataType: String = DATA_TYPE_EXCHANGE_RATE
    ): List<ExchangeRateDto>

    companion object {
        const val DATA_TYPE_EXCHANGE_RATE = "AP01"
    }
}
