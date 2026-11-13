package com.namhs.cashnote.domain.repository

import com.namhs.cashnote.domain.model.ExchangeRate
import kotlinx.coroutines.flow.Flow

interface ExchangeRateRepository {
    /** 저장된 환율 전체를 통화 코드 순으로 구독한다. 아직 받은 적이 없으면 빈 목록이다 */
    fun observeExchangeRates(): Flow<List<ExchangeRate>>

    /**
     * 최신 환율을 받아 저장된 환율을 교체한다. 구독 중인 Flow는 저장 후 새 값을 받는다.
     *
     * 네트워크·API 오류는 예외로 던지며, 이때 저장된 환율은 바뀌지 않는다.
     */
    suspend fun refresh()
}
