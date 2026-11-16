package com.namhs.cashnote.presentation.settings

import app.cash.turbine.test
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

    private fun viewModel() = SettingsViewModel(
        getDefaultCurrency = GetDefaultCurrencyUseCase(settingsRepository),
        setDefaultCurrency = SetDefaultCurrencyUseCase(settingsRepository),
        getExchangeRates = GetExchangeRatesUseCase(exchangeRateRepository),
        getNotificationSettings = GetNotificationSettingsUseCase(settingsRepository),
        setReminder = SetReminderUseCase(settingsRepository, reminderScheduler),
        setBudgetAlertEnabled = SetBudgetAlertEnabledUseCase(settingsRepository)
    )
}
