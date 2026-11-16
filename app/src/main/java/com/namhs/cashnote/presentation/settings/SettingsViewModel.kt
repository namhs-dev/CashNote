package com.namhs.cashnote.presentation.settings

import androidx.lifecycle.viewModelScope
import com.namhs.cashnote.domain.usecase.GetDefaultCurrencyUseCase
import com.namhs.cashnote.domain.usecase.GetExchangeRatesUseCase
import com.namhs.cashnote.domain.usecase.GetNotificationSettingsUseCase
import com.namhs.cashnote.domain.usecase.SetBudgetAlertEnabledUseCase
import com.namhs.cashnote.domain.usecase.SetDefaultCurrencyUseCase
import com.namhs.cashnote.domain.usecase.SetReminderUseCase
import com.namhs.cashnote.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getDefaultCurrency: GetDefaultCurrencyUseCase,
    private val setDefaultCurrency: SetDefaultCurrencyUseCase,
    private val getExchangeRates: GetExchangeRatesUseCase,
    private val getNotificationSettings: GetNotificationSettingsUseCase,
    private val setReminder: SetReminderUseCase,
    private val setBudgetAlertEnabled: SetBudgetAlertEnabledUseCase
) : MviViewModel<SettingsUiState, SettingsIntent, SettingsSideEffect>(SettingsUiState()) {

    init {
        viewModelScope.launch {
            getDefaultCurrency().collect { code -> setState { copy(defaultCurrency = code) } }
        }
        viewModelScope.launch {
            getExchangeRates().collect { rates -> setState { copy(exchangeRates = rates) } }
        }
        viewModelScope.launch {
            getNotificationSettings().collect { settings ->
                setState { copy(notification = settings) }
            }
        }
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            // 저장하면 구독 중인 기본 통화가 바뀌어 State에 반영된다
            is SettingsIntent.DefaultCurrencySelected ->
                viewModelScope.launch { setDefaultCurrency(intent.currencyCode) }

            SettingsIntent.CategoryManageClicked ->
                postSideEffect(SettingsSideEffect.NavigateToCategoryManage)

            // 알림 설정도 저장하면 구독 중인 설정이 바뀌어 State에 반영된다
            is SettingsIntent.ReminderToggled -> {
                val time = currentState.notification.reminderTime
                viewModelScope.launch { setReminder(intent.enabled, time) }
            }

            is SettingsIntent.ReminderTimeSelected -> {
                val enabled = currentState.notification.reminderEnabled
                viewModelScope.launch { setReminder(enabled, intent.time) }
            }

            is SettingsIntent.BudgetAlertToggled ->
                viewModelScope.launch { setBudgetAlertEnabled(intent.enabled) }
        }
    }
}
