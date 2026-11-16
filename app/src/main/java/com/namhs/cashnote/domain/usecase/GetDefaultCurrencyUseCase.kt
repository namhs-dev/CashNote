package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 기본 통화 코드를 구독한다. null이면 원화 */
class GetDefaultCurrencyUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<String?> = repository.observeDefaultCurrency()
}
