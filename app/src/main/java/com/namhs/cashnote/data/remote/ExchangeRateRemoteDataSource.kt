package com.namhs.cashnote.data.remote

import com.namhs.cashnote.data.remote.api.ExchangeRateApi
import com.namhs.cashnote.data.remote.dto.ExchangeRateDto
import com.namhs.cashnote.di.ExchangeRateApiKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 한국수출입은행 환율 API를 호출해 가장 최근 영업일의 환율을 가져온다.
 *
 * 주말·공휴일이거나 당일 오전 11시 이전이면 API가 빈 배열을 돌려주므로,
 * 기준일부터 하루씩 거슬러 올라가며 데이터가 있는 첫 날짜를 찾는다.
 */
class ExchangeRateRemoteDataSource @Inject constructor(
    private val api: ExchangeRateApi,
    @ExchangeRateApiKey private val authKey: String
) {
    /**
     * [baseDate]부터 최대 [MAX_LOOKBACK_DAYS]일 전까지 조회한다.
     * 기준일은 호출하는 쪽에서 한국 시간(Asia/Seoul) 기준으로 넘긴다.
     *
     * @return 데이터가 있는 첫 날짜와 그날의 환율. 기간 안에 데이터가 없으면 null
     * @throws ExchangeRateApiException 인증키가 없거나 API가 result != 1을 돌려준 경우
     */
    suspend fun fetchLatest(baseDate: LocalDate): FetchedExchangeRates? {
        if (authKey.isBlank()) throw ExchangeRateApiException(RESULT_INVALID_AUTH)

        repeat(MAX_LOOKBACK_DAYS) { daysBack ->
            val date = baseDate.minusDays(daysBack.toLong())
            val rates = api.getExchangeRates(
                authKey = authKey,
                searchDate = date.format(DateTimeFormatter.BASIC_ISO_DATE)
            )
            rates.firstOrNull { it.result != RESULT_SUCCESS }?.let {
                throw ExchangeRateApiException(it.result)
            }
            if (rates.isNotEmpty()) return FetchedExchangeRates(date, rates)
        }
        return null
    }

    companion object {
        /** 추석·설 연휴(주말 포함 최대 7~9일)에도 직전 영업일에 닿을 만큼 넉넉히 둔다 */
        const val MAX_LOOKBACK_DAYS = 10
        const val RESULT_SUCCESS = 1
        const val RESULT_INVALID_AUTH = 3
    }
}

/** 실제로 환율 데이터가 있었던 날짜와 그날의 환율 */
data class FetchedExchangeRates(val date: LocalDate, val rates: List<ExchangeRateDto>)

/**
 * 환율 API가 실패 결과를 돌려준 경우.
 *
 * @property resultCode 2: DATA 코드 오류, 3: 인증코드 오류(키 없음 포함), 4: 일일 제한 횟수 마감
 */
class ExchangeRateApiException(val resultCode: Int) :
    Exception("환율 API 오류 (result=$resultCode)")
