package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.ExchangeRate
import com.namhs.cashnote.testing.FakeExchangeRateRepository
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExchangeRateUseCasesTest {
    private val usd = ExchangeRate(
        currencyCode = "USD",
        currencyName = "미국 달러",
        unit = 1,
        baseRate = BigDecimal("1450.5"),
        rateDate = LocalDate.of(2026, 11, 13),
        updatedAt = Instant.parse("2026-11-13T03:00:00Z")
    )

    private val repository = FakeExchangeRateRepository()
    private val getExchangeRates = GetExchangeRatesUseCase(repository)
    private val refreshExchangeRates = RefreshExchangeRatesUseCase(repository)

    @Test
    fun get_emitsStoredRates() = runTest {
        assertTrue(getExchangeRates().first().isEmpty())

        repository.rates.value = listOf(usd)

        assertEquals(listOf(usd), getExchangeRates().first())
    }

    @Test
    fun refresh_updatesObservedRates() = runTest {
        repository.refreshResult = listOf(usd)

        refreshExchangeRates()

        assertEquals(1, repository.refreshCount)
        assertEquals(listOf(usd), getExchangeRates().first())
    }

    @Test
    fun refresh_failure_propagatesAndKeepsRates() = runTest {
        repository.failure = IOException("offline")

        val result = runCatching { refreshExchangeRates() }

        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(getExchangeRates().first().isEmpty())
    }
}
