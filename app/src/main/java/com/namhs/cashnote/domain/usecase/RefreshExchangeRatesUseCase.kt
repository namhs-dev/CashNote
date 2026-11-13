package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.repository.ExchangeRateRepository
import javax.inject.Inject

/**
 * 최신 환율을 받아 저장된 환율을 교체한다.
 *
 * 네트워크·API 오류는 예외로 던진다. 실패를 무시할지(입력 화면), 재시도할지(주기 동기화)는 호출하는 쪽이 정한다.
 */
class RefreshExchangeRatesUseCase @Inject constructor(
    private val repository: ExchangeRateRepository
) {
    suspend operator fun invoke() = repository.refresh()
}
