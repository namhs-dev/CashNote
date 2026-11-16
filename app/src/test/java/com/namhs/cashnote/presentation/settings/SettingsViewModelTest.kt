package com.namhs.cashnote.presentation.settings

import app.cash.turbine.test
import com.namhs.cashnote.BuildConfig
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.ExchangeRate
import com.namhs.cashnote.domain.model.NotificationSettings
import com.namhs.cashnote.domain.usecase.GetDefaultCurrencyUseCase
import com.namhs.cashnote.domain.usecase.GetExchangeRatesUseCase
import com.namhs.cashnote.domain.usecase.GetNotificationSettingsUseCase
import com.namhs.cashnote.domain.usecase.SetBudgetAlertEnabledUseCase
import com.namhs.cashnote.domain.usecase.SetDefaultCurrencyUseCase
import com.namhs.cashnote.domain.usecase.SetReminderUseCase
import com.namhs.cashnote.testing.FakeExchangeRateRepository
import com.namhs.cashnote.testing.FakeReminderScheduler
import com.namhs.cashnote.testing.FakeSettingsRepository
import com.namhs.cashnote.testing.MainDispatcherRule
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val friday = LocalDate.of(2026, 11, 13)
    private val updatedAt = Instant.parse("2026-11-13T03:00:00Z")
    private val usd = ExchangeRate("USD", "미국 달러", 1, BigDecimal("1450.5"), friday, updatedAt)
    private val jpy = ExchangeRate("JPY", "일본 옌", 100, BigDecimal("935.12"), friday, updatedAt)

    private val settingsRepository = FakeSettingsRepository()
    private val exchangeRateRepository = FakeExchangeRateRepository()
    private val reminderScheduler = FakeReminderScheduler()

    @Test
    fun init_defaultCurrencyIsKrw() {
        assertNull(viewModel().uiState.value.defaultCurrency)
    }

    @Test
    fun init_loadsSavedDefaultCurrencyAndOptions() {
        settingsRepository.defaultCurrency.value = "USD"
        exchangeRateRepository.rates.value = listOf(jpy, usd)

        val state = viewModel().uiState.value

        assertEquals("USD", state.defaultCurrency)
        assertEquals(listOf("JPY", "USD"), state.foreignCurrencyOptions)
    }

    @Test
    fun foreignCurrencyOptions_keepsSavedCurrencyMissingFromRates() {
        // 환율을 아직 못 받았어도 저장된 기본 통화는 선택지에 남긴다
        settingsRepository.defaultCurrency.value = "EUR"
        exchangeRateRepository.rates.value = listOf(usd)

        assertEquals(listOf("USD", "EUR"), viewModel().uiState.value.foreignCurrencyOptions)
    }

    @Test
    fun defaultCurrencySelected_savesAndUpdatesState() {
        exchangeRateRepository.rates.value = listOf(usd)
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.DefaultCurrencySelected("USD"))

        assertEquals("USD", settingsRepository.defaultCurrency.value)
        assertEquals("USD", viewModel.uiState.value.defaultCurrency)
    }

    @Test
    fun defaultCurrencySelected_krw_clearsDefault() {
        settingsRepository.defaultCurrency.value = "USD"
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.DefaultCurrencySelected(null))

        assertNull(settingsRepository.defaultCurrency.value)
        assertNull(viewModel.uiState.value.defaultCurrency)
    }

    @Test
    fun categoryManageClicked_navigatesToCategoryManage() = runTest {
        val viewModel = viewModel()

        viewModel.sideEffect.test {
            viewModel.onIntent(SettingsIntent.CategoryManageClicked)

            assertEquals(SettingsSideEffect.NavigateToCategoryManage, awaitItem())
        }
    }

    @Test
    fun init_appVersionIsBuildVersionName() {
        val version = viewModel().uiState.value.appVersion

        assertEquals(BuildConfig.VERSION_NAME, version)
        assertTrue(version.isNotBlank())
    }

    @Test
    fun contactClicked_opensContactEmail() = runTest {
        val viewModel = viewModel()

        viewModel.sideEffect.test {
            viewModel.onIntent(SettingsIntent.ContactClicked)

            assertEquals(SettingsSideEffect.OpenContactEmail, awaitItem())
        }
    }

    @Test
    fun contactEmailUnavailable_showsAddress() = runTest {
        val viewModel = viewModel()

        viewModel.sideEffect.test {
            viewModel.onIntent(SettingsIntent.ContactEmailUnavailable)

            assertEquals(SettingsSideEffect.ShowContactEmailUnavailable, awaitItem())
        }
    }

    @Test
    fun privacyPolicyClicked_opensPrivacyPolicy() = runTest {
        val viewModel = viewModel()

        viewModel.sideEffect.test {
            viewModel.onIntent(SettingsIntent.PrivacyPolicyClicked)

            assertEquals(SettingsSideEffect.OpenPrivacyPolicy, awaitItem())
        }
    }

    @Test
    fun browserUnavailable_showsUrl() = runTest {
        val viewModel = viewModel()

        viewModel.sideEffect.test {
            viewModel.onIntent(SettingsIntent.BrowserUnavailable)

            assertEquals(SettingsSideEffect.ShowBrowserUnavailable, awaitItem())
        }
    }

    @Test
    fun ossLicensesClicked_navigatesToOssLicenses() = runTest {
        val viewModel = viewModel()

        viewModel.sideEffect.test {
            viewModel.onIntent(SettingsIntent.OssLicensesClicked)

            assertEquals(SettingsSideEffect.NavigateToOssLicenses, awaitItem())
        }
    }

    // 알림

    @Test
    fun init_notificationDefaults() {
        assertEquals(NotificationSettings(), viewModel().uiState.value.notification)
    }

    @Test
    fun reminderToggledOn_savesAndSchedulesAtSavedTime() {
        settingsRepository.notificationSettings.value =
            NotificationSettings(reminderTime = LocalTime.of(8, 0))
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.ReminderToggled(true))

        assertTrue(viewModel.uiState.value.notification.reminderEnabled)
        assertEquals(LocalTime.of(8, 0), reminderScheduler.scheduledTime)
    }

    @Test
    fun reminderToggledOff_cancelsSchedule() {
        val viewModel = viewModel()
        viewModel.onIntent(SettingsIntent.ReminderToggled(true))

        viewModel.onIntent(SettingsIntent.ReminderToggled(false))

        assertFalse(viewModel.uiState.value.notification.reminderEnabled)
        assertNull(reminderScheduler.scheduledTime)
    }

    @Test
    fun reminderTimeSelected_whileOn_reschedules() {
        val viewModel = viewModel()
        viewModel.onIntent(SettingsIntent.ReminderToggled(true))

        viewModel.onIntent(SettingsIntent.ReminderTimeSelected(LocalTime.of(22, 30)))

        assertEquals(LocalTime.of(22, 30), viewModel.uiState.value.notification.reminderTime)
        assertEquals(LocalTime.of(22, 30), reminderScheduler.scheduledTime)
    }

    @Test
    fun reminderTimeSelected_whileOff_savesWithoutScheduling() {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.ReminderTimeSelected(LocalTime.of(7, 15)))

        val notification = viewModel.uiState.value.notification
        assertFalse(notification.reminderEnabled)
        assertEquals(LocalTime.of(7, 15), notification.reminderTime)
        assertNull(reminderScheduler.scheduledTime)
    }

    @Test
    fun budgetAlertToggled_savesWithoutTouchingReminder() {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.BudgetAlertToggled(true))

        assertTrue(viewModel.uiState.value.notification.budgetAlertEnabled)
        assertFalse(viewModel.uiState.value.notification.reminderEnabled)
        assertTrue(reminderScheduler.calls.isEmpty())
    }

    // 알림 권한

    /** 알림을 표시할 수 없는 상태(권한 없음)로 시작한다 */
    private fun blockedViewModel() = viewModel().apply {
        onIntent(SettingsIntent.NotificationPermissionChecked(canPost = false))
    }

    @Test
    fun toggleOn_withoutPermission_requestsPermissionWithoutSaving() = runTest {
        val viewModel = blockedViewModel()

        viewModel.sideEffect.test {
            viewModel.onIntent(SettingsIntent.ReminderToggled(true))

            assertEquals(
                SettingsSideEffect.RequestNotificationPermission(NotificationTarget.REMINDER),
                awaitItem()
            )
        }
        assertFalse(settingsRepository.notificationSettings.value.reminderEnabled)
        assertNull(reminderScheduler.scheduledTime)
    }

    @Test
    fun toggleOff_withoutPermission_turnsOffDirectly() = runTest {
        settingsRepository.notificationSettings.value =
            NotificationSettings(budgetAlertEnabled = true)
        val viewModel = blockedViewModel()

        viewModel.sideEffect.test {
            viewModel.onIntent(SettingsIntent.BudgetAlertToggled(false))

            expectNoEvents()
        }
        assertFalse(settingsRepository.notificationSettings.value.budgetAlertEnabled)
    }

    @Test
    fun permissionGranted_turnsOnRequestedTarget() {
        val viewModel = blockedViewModel()

        viewModel.onIntent(
            SettingsIntent.NotificationPermissionResult(
                NotificationTarget.BUDGET_ALERT,
                granted = true,
                canAskAgain = true
            )
        )

        val state = viewModel.uiState.value
        assertTrue(state.canPostNotifications)
        assertTrue(state.notification.budgetAlertEnabled)
        assertFalse(state.notification.reminderEnabled)
    }

    @Test
    fun permissionDenied_canAskAgain_staysOffAndShowsMessage() = runTest {
        val viewModel = blockedViewModel()

        viewModel.sideEffect.test {
            viewModel.onIntent(
                SettingsIntent.NotificationPermissionResult(
                    NotificationTarget.REMINDER,
                    granted = false,
                    canAskAgain = true
                )
            )

            assertEquals(
                SettingsSideEffect.ShowMessage(R.string.settings_notification_permission_denied),
                awaitItem()
            )
        }
        assertFalse(viewModel.uiState.value.notification.reminderEnabled)
        assertFalse(viewModel.uiState.value.isPermissionSettingsDialogVisible)
    }

    @Test
    fun permissionDeniedPermanently_showsSettingsDialog() {
        val viewModel = blockedViewModel()

        viewModel.onIntent(
            SettingsIntent.NotificationPermissionResult(
                NotificationTarget.REMINDER,
                granted = false,
                canAskAgain = false
            )
        )

        assertTrue(viewModel.uiState.value.isPermissionSettingsDialogVisible)
        assertFalse(viewModel.uiState.value.notification.reminderEnabled)
    }

    @Test
    fun openSettings_thenAllowedInSettings_turnsOnPendingTarget() = runTest {
        val viewModel = blockedViewModel()
        viewModel.onIntent(
            SettingsIntent.NotificationPermissionResult(
                NotificationTarget.REMINDER,
                granted = false,
                canAskAgain = false
            )
        )

        viewModel.sideEffect.test {
            viewModel.onIntent(SettingsIntent.OpenNotificationSettingsClicked)

            assertEquals(SettingsSideEffect.OpenNotificationSettings, awaitItem())
        }
        assertFalse(viewModel.uiState.value.isPermissionSettingsDialogVisible)

        // 설정 앱에서 허용하고 돌아왔다
        viewModel.onIntent(SettingsIntent.NotificationPermissionChecked(canPost = true))

        assertTrue(viewModel.uiState.value.notification.reminderEnabled)
        assertEquals(LocalTime.of(21, 0), reminderScheduler.scheduledTime)
    }

    @Test
    fun settingsDialogDismissed_forgetsPendingTarget() {
        val viewModel = blockedViewModel()
        viewModel.onIntent(
            SettingsIntent.NotificationPermissionResult(
                NotificationTarget.REMINDER,
                granted = false,
                canAskAgain = false
            )
        )

        viewModel.onIntent(SettingsIntent.PermissionSettingsDialogDismissed)
        // 나중에 다른 이유로 권한이 생겨도 사용자가 취소한 알림을 켜지 않는다
        viewModel.onIntent(SettingsIntent.NotificationPermissionChecked(canPost = true))

        assertFalse(viewModel.uiState.value.isPermissionSettingsDialogVisible)
        assertFalse(viewModel.uiState.value.notification.reminderEnabled)
    }

    @Test
    fun notificationBlocked_onlyWhenSomethingIsOn() {
        val viewModel = blockedViewModel()
        assertFalse(viewModel.uiState.value.isNotificationBlocked)

        settingsRepository.notificationSettings.value =
            NotificationSettings(reminderEnabled = true)

        assertTrue(viewModel.uiState.value.isNotificationBlocked)
    }

    private fun viewModel() = SettingsViewModel(
        getDefaultCurrency = GetDefaultCurrencyUseCase(settingsRepository),
        setDefaultCurrency = SetDefaultCurrencyUseCase(settingsRepository),
        getExchangeRates = GetExchangeRatesUseCase(exchangeRateRepository),
        getNotificationSettings = GetNotificationSettingsUseCase(settingsRepository),
        setReminder = SetReminderUseCase(settingsRepository, reminderScheduler),
        setBudgetAlertEnabled = SetBudgetAlertEnabledUseCase(settingsRepository)
    )
}
