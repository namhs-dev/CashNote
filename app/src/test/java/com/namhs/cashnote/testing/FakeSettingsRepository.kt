package com.namhs.cashnote.testing

import com.namhs.cashnote.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** 설정을 메모리에 두는 가짜 Repository. 기본 통화의 초기값은 원화(null)다 */
class FakeSettingsRepository(initialDefaultCurrency: String? = null) : SettingsRepository {
    val defaultCurrency = MutableStateFlow(initialDefaultCurrency)

    override fun observeDefaultCurrency(): Flow<String?> = defaultCurrency

    override suspend fun setDefaultCurrency(currencyCode: String?) {
        defaultCurrency.value = currencyCode
    }
}
