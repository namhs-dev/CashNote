package com.namhs.cashnote.presentation.settings

import com.namhs.cashnote.domain.model.ExchangeRate

/** 설정 화면 상태 */
data class SettingsUiState(
    /** 새 거래 입력 화면이 처음 고를 통화 코드. null이면 원화 */
    val defaultCurrency: String? = null,
    /** 저장된 환율 목록. 기본 통화 선택지와 통화 이름 표시에 쓴다 */
    val exchangeRates: List<ExchangeRate> = emptyList()
) {
    /** 기본 통화로 고를 수 있는 외화. 저장된 환율 통화에, 목록에 없는 현재 기본 통화를 더한다 */
    val foreignCurrencyOptions: List<String>
        get() = (exchangeRates.map { it.currencyCode } + listOfNotNull(defaultCurrency)).distinct()
}

/** 설정 화면에서 발생하는 사용자 의도 */
sealed interface SettingsIntent {
    /** 기본 통화 선택. null이면 원화 */
    data class DefaultCurrencySelected(val currencyCode: String?) : SettingsIntent

    data object CategoryManageClicked : SettingsIntent
}

/** 설정 화면의 일회성 이벤트 */
sealed interface SettingsSideEffect {
    data object NavigateToCategoryManage : SettingsSideEffect
}
