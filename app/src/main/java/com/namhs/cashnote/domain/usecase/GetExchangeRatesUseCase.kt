package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.ExchangeRate
import com.namhs.cashnote.domain.repository.ExchangeRateRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 저장된 환율 전체를 통화 코드 순으로 구독한다. 아직 받은 적이 없으면 빈 목록이다 */
class GetExchangeRatesUseCase @Inject constructor(private val repository: ExchangeRateRepository) {
    operator fun invoke(): Flow<List<ExchangeRate>> = repository.observeExchangeRates()
}
