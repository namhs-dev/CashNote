package com.namhs.cashnote.domain.repository

import kotlinx.coroutines.flow.Flow

/** 사용자 설정 */
interface SettingsRepository {
    /** 새 거래 입력 화면이 처음 고를 통화 코드를 구독한다. null이면 원화 */
    fun observeDefaultCurrency(): Flow<String?>

    /** 기본 통화를 바꾼다. null이면 원화 */
    suspend fun setDefaultCurrency(currencyCode: String?)
}
