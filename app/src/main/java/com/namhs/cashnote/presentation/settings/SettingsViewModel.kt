package com.namhs.cashnote.presentation.settings

import androidx.lifecycle.viewModelScope
import com.namhs.cashnote.domain.usecase.GetDefaultCurrencyUseCase
import com.namhs.cashnote.domain.usecase.GetExchangeRatesUseCase
import com.namhs.cashnote.domain.usecase.SetDefaultCurrencyUseCase
import com.namhs.cashnote.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getDefaultCurrency: GetDefaultCurrencyUseCase,
    private val setDefaultCurrency: SetDefaultCurrencyUseCase,
    private val getExchangeRates: GetExchangeRatesUseCase
) : MviViewModel<SettingsUiState, SettingsIntent, SettingsSideEffect>(SettingsUiState()) {

    init {
        viewModelScope.launch {
            getDefaultCurrency().collect { code -> setState { copy(defaultCurrency = code) } }
        }
        viewModelScope.launch {
            getExchangeRates().collect { rates -> setState { copy(exchangeRates = rates) } }
        }
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            // 저장하면 구독 중인 기본 통화가 바뀌어 State에 반영된다
            is SettingsIntent.DefaultCurrencySelected ->
                viewModelScope.launch { setDefaultCurrency(intent.currencyCode) }

            SettingsIntent.CategoryManageClicked ->
                postSideEffect(SettingsSideEffect.NavigateToCategoryManage)
        }
    }
}
