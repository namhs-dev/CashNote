package com.namhs.cashnote.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.namhs.cashnote.domain.model.BudgetLevel
import com.namhs.cashnote.domain.model.NotificationSettings
import java.io.File
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** 실제 DataStore를 임시 파일에 만들어 읽고 쓴다 */
class SettingsRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun repository(file: File = File(temporaryFolder.root, "settings.preferences_pb")) =
        SettingsRepositoryImpl(
            PreferenceDataStoreFactory.create(scope = dataStoreScope, produceFile = { file })
        )

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun defaultCurrency_initiallyKrw() = runTest {
        assertNull(repository().observeDefaultCurrency().first())
    }

    @Test
    fun setDefaultCurrency_emitsSavedCode() = runTest {
        val repository = repository()

        repository.setDefaultCurrency("USD")

        assertEquals("USD", repository.observeDefaultCurrency().first())
    }

    @Test
    fun setDefaultCurrency_null_backToKrw() = runTest {
        val repository = repository()
        repository.setDefaultCurrency("JPY")

        repository.setDefaultCurrency(null)

        assertNull(repository.observeDefaultCurrency().first())
    }

    // 알림 설정

    @Test
    fun notificationSettings_initiallyAllOffAt2100() = runTest {
        assertEquals(
            NotificationSettings(
                reminderEnabled = false,
                reminderTime = LocalTime.of(21, 0),
                budgetAlertEnabled = false
            ),
            repository().observeNotificationSettings().first()
        )
    }

    @Test
    fun notificationSettings_savesEachValue() = runTest {
        val repository = repository()

        repository.setReminderEnabled(true)
        repository.setReminderTime(LocalTime.of(8, 30, 45))
        repository.setBudgetAlertEnabled(true)

        // 초는 버리고 분까지만 저장한다
        assertEquals(
            NotificationSettings(true, LocalTime.of(8, 30), true),
            repository.observeNotificationSettings().first()
        )
    }

    // 예산 알림 기록

    @Test
    fun budgetAlertedLevel_initiallyNull() = runTest {
        assertNull(repository().getBudgetAlertedLevel(YearMonth.of(2026, 11)))
    }

    @Test
    fun budgetAlertedLevel_returnsLevelForSameMonthOnly() = runTest {
        val repository = repository()

        repository.setBudgetAlertedLevel(YearMonth.of(2026, 11), BudgetLevel.WARNING)

        assertEquals(BudgetLevel.WARNING, repository.getBudgetAlertedLevel(YearMonth.of(2026, 11)))
        // 다음 달에는 다시 처음부터 알린다
        assertNull(repository.getBudgetAlertedLevel(YearMonth.of(2026, 12)))
    }

    @Test
    fun budgetAlertedLevel_newMonthOverwritesOldRecord() = runTest {
        val repository = repository()
        repository.setBudgetAlertedLevel(YearMonth.of(2026, 11), BudgetLevel.EXCEEDED)

        repository.setBudgetAlertedLevel(YearMonth.of(2026, 12), BudgetLevel.WARNING)

        assertNull(repository.getBudgetAlertedLevel(YearMonth.of(2026, 11)))
        assertEquals(BudgetLevel.WARNING, repository.getBudgetAlertedLevel(YearMonth.of(2026, 12)))
    }
}
