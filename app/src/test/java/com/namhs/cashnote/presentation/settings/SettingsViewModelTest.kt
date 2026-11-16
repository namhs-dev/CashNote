package com.namhs.cashnote.presentation.settings

import com.namhs.cashnote.domain.model.ExchangeRate
import com.namhs.cashnote.domain.usecase.GetDefaultCurrencyUseCase
import com.namhs.cashnote.domain.usecase.GetExchangeRatesUseCase
import com.namhs.cashnote.domain.usecase.SetDefaultCurrencyUseCase
import com.namhs.cashnote.testing.FakeExchangeRateRepository
import com.namhs.cashnote.testing.FakeSettingsRepository
import com.namhs.cashnote.testing.MainDispatcherRule
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private fun viewModel() = SettingsViewModel(
        getDefaultCurrency = GetDefaultCurrencyUseCase(settingsRepository),
        setDefaultCurrency = SetDefaultCurrencyUseCase(settingsRepository),
        getExchangeRates = GetExchangeRatesUseCase(exchangeRateRepository)
    )
}
