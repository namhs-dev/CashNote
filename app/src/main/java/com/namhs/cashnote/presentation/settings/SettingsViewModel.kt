package com.namhs.cashnote.presentation.settings

import androidx.lifecycle.viewModelScope
import com.namhs.cashnote.BuildConfig
import com.namhs.cashnote.R
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
) : MviViewModel<SettingsUiState, SettingsIntent, SettingsSideEffect>(
    // 앱 버전은 실행 중에 바뀌지 않으므로 구독하지 않고 처음에 한 번 넣는다
    SettingsUiState(appVersion = BuildConfig.VERSION_NAME)
) {

    // 설정 앱으로 보낸 뒤 허용하고 돌아오면 켜 줄 알림. 다이얼로그를 닫으면 잊는다
    private var pendingTarget: NotificationTarget? = null

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

            SettingsIntent.OssLicensesClicked ->
                postSideEffect(SettingsSideEffect.NavigateToOssLicenses)

            SettingsIntent.ContactClicked -> postSideEffect(SettingsSideEffect.OpenContactEmail)

            SettingsIntent.ContactEmailUnavailable ->
                postSideEffect(SettingsSideEffect.ShowContactEmailUnavailable)

            SettingsIntent.PrivacyPolicyClicked -> postSideEffect(
                SettingsSideEffect.OpenPrivacyPolicy
            )

            SettingsIntent.BrowserUnavailable ->
                postSideEffect(SettingsSideEffect.ShowBrowserUnavailable)

            // 알림 설정도 저장하면 구독 중인 설정이 바뀌어 State에 반영된다
            is SettingsIntent.ReminderToggled ->
                toggle(NotificationTarget.REMINDER, intent.enabled)

            is SettingsIntent.ReminderTimeSelected -> {
                val enabled = currentState.notification.reminderEnabled
                viewModelScope.launch { setReminder(enabled, intent.time) }
            }

            is SettingsIntent.BudgetAlertToggled ->
                toggle(NotificationTarget.BUDGET_ALERT, intent.enabled)

            is SettingsIntent.NotificationPermissionChecked -> {
                setState { copy(canPostNotifications = intent.canPost) }
                // 설정 앱에서 허용하고 돌아왔으면 켜려던 알림을 마저 켠다
                val target = pendingTarget
                if (intent.canPost && target != null) {
                    pendingTarget = null
                    setEnabled(target, true)
                }
            }

            is SettingsIntent.NotificationPermissionResult -> onPermissionResult(intent)

            SettingsIntent.OpenNotificationSettingsClicked -> {
                setState { copy(isPermissionSettingsDialogVisible = false) }
                postSideEffect(SettingsSideEffect.OpenNotificationSettings)
            }

            SettingsIntent.PermissionSettingsDialogDismissed -> {
                pendingTarget = null
                setState { copy(isPermissionSettingsDialogVisible = false) }
            }
        }
    }

    // 끄기는 바로 반영한다. 켜기는 알림을 표시할 수 없으면 권한부터 받는다
    private fun toggle(target: NotificationTarget, enabled: Boolean) {
        if (enabled && !currentState.canPostNotifications) {
            postSideEffect(SettingsSideEffect.RequestNotificationPermission(target))
        } else {
            setEnabled(target, enabled)
        }
    }

    private fun onPermissionResult(result: SettingsIntent.NotificationPermissionResult) {
        when {
            result.granted -> {
                setState { copy(canPostNotifications = true) }
                setEnabled(result.target, true)
            }

            // 시스템 창을 다시 띄울 수 있으니 이번에는 꺼 둔 채 알리기만 한다
            result.canAskAgain ->
                postSideEffect(
                    SettingsSideEffect.ShowMessage(R.string.settings_notification_permission_denied)
                )

            // "다시 묻지 않기"라 시스템 창이 뜨지 않는다. 설정 앱에서 허용하도록 안내한다
            else -> {
                pendingTarget = result.target
                setState { copy(isPermissionSettingsDialogVisible = true) }
            }
        }
    }

    private fun setEnabled(target: NotificationTarget, enabled: Boolean) {
        val settings = currentState.notification
        viewModelScope.launch {
            when (target) {
                NotificationTarget.REMINDER -> setReminder(enabled, settings.reminderTime)
                NotificationTarget.BUDGET_ALERT -> setBudgetAlertEnabled(enabled)
            }
        }
    }
}
