package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.repository.SettingsRepository
import javax.inject.Inject

/** 기본 통화를 바꾼다. null이면 원화 */
class SetDefaultCurrencyUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(currencyCode: String?) = repository.setDefaultCurrency(currencyCode)
}
