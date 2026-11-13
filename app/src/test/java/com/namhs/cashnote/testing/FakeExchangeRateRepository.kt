package com.namhs.cashnote.testing

import com.namhs.cashnote.domain.model.ExchangeRate
import com.namhs.cashnote.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 환율 목록을 메모리에 두는 가짜 Repository.
 *
 * refresh하면 [refreshResult]로 목록을 교체한다. [failure]가 있으면 목록을 그대로 두고 예외를 던진다.
 */
class FakeExchangeRateRepository(initial: List<ExchangeRate> = emptyList()) :
    ExchangeRateRepository {
    val rates = MutableStateFlow(initial)

    /** refresh 성공 시 저장될 목록 */
    var refreshResult: List<ExchangeRate> = initial

    /** null이 아니면 refresh에서 이 예외를 던진다 */
    var failure: Exception? = null

    /** refresh 호출 횟수 */
    var refreshCount = 0
        private set

    override fun observeExchangeRates(): Flow<List<ExchangeRate>> = rates

    override suspend fun refresh() {
        refreshCount++
        failure?.let { throw it }
        rates.value = refreshResult
    }
}
