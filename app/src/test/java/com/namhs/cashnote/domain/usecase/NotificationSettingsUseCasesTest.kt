package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.NotificationSettings
import com.namhs.cashnote.testing.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSettingsUseCasesTest {
    private val repository = FakeSettingsRepository()
    private val getNotificationSettings = GetNotificationSettingsUseCase(repository)
    private val setBudgetAlertEnabled = SetBudgetAlertEnabledUseCase(repository)

    @Test
    fun get_initiallyDefaults() = runTest {
        assertEquals(NotificationSettings(), getNotificationSettings().first())
    }

    @Test
    fun setBudgetAlertEnabled_updatesObservedSettings() = runTest {
        setBudgetAlertEnabled(true)

        assertTrue(getNotificationSettings().first().budgetAlertEnabled)
    }
}
