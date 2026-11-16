package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.testing.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultCurrencyUseCasesTest {
    private val repository = FakeSettingsRepository()
    private val getDefaultCurrency = GetDefaultCurrencyUseCase(repository)
    private val setDefaultCurrency = SetDefaultCurrencyUseCase(repository)

    @Test
    fun get_initiallyKrw() = runTest {
        assertNull(getDefaultCurrency().first())
    }

    @Test
    fun set_updatesObservedCurrency() = runTest {
        setDefaultCurrency("USD")

        assertEquals("USD", getDefaultCurrency().first())
    }
}
