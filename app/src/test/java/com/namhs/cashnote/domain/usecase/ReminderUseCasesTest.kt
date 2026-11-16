package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.NotificationSettings
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.testing.FakeNotifier
import com.namhs.cashnote.testing.FakeReminderScheduler
import com.namhs.cashnote.testing.FakeSettingsRepository
import com.namhs.cashnote.testing.FakeTransactionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderUseCasesTest {
    private val today = LocalDate.of(2026, 11, 16)

    private val settingsRepository = FakeSettingsRepository()
    private val scheduler = FakeReminderScheduler()
    private val notifier = FakeNotifier()

    private fun transactionOn(date: LocalDate) = Transaction(
        id = 1,
        amount = 10_000,
        type = TransactionType.EXPENSE,
        categoryId = 1,
        date = date,
        createdAt = Instant.parse("2026-11-01T00:00:00Z")
    )

    private fun sendDailyReminder(vararg transactions: Transaction) = SendDailyReminderUseCase(
        settingsRepository,
        FakeTransactionRepository(transactions.toList()),
        notifier
    )

    private fun enableReminder() {
        settingsRepository.notificationSettings.value = NotificationSettings(reminderEnabled = true)
    }

    // 리마인더 보내기

    @Test
    fun send_enabledAndNoRecordToday_sendsReminder() = runTest {
        enableReminder()

        // 어제 기록은 오늘 기록이 아니다
        assertTrue(sendDailyReminder(transactionOn(today.minusDays(1)))(today))
        assertEquals(1, notifier.reminderCount)
    }

    @Test
    fun send_recordedToday_staysQuiet() = runTest {
        enableReminder()

        assertFalse(sendDailyReminder(transactionOn(today))(today))
        assertEquals(0, notifier.reminderCount)
    }

    @Test
    fun send_reminderDisabled_staysQuiet() = runTest {
        assertFalse(sendDailyReminder()(today))
        assertEquals(0, notifier.reminderCount)
    }

    // 켜기·끄기

    @Test
    fun setReminder_on_savesAndSchedules() = runTest {
        SetReminderUseCase(settingsRepository, scheduler)(true, LocalTime.of(8, 30))

        assertEquals(
            NotificationSettings(reminderEnabled = true, reminderTime = LocalTime.of(8, 30)),
            settingsRepository.notificationSettings.value
        )
        assertEquals(LocalTime.of(8, 30), scheduler.scheduledTime)
    }

    @Test
    fun setReminder_off_savesTimeAndCancels() = runTest {
        val setReminder = SetReminderUseCase(settingsRepository, scheduler)
        setReminder(true, LocalTime.of(21, 0))

        setReminder(false, LocalTime.of(22, 0))

        // 끈 상태에서도 고른 시각은 기억해 다음에 켤 때 쓴다
        assertFalse(settingsRepository.notificationSettings.value.reminderEnabled)
        assertEquals(
            LocalTime.of(22, 0),
            settingsRepository.notificationSettings.value.reminderTime
        )
        assertNull(scheduler.scheduledTime)
    }

    // 앱 시작 시 예약 확인

    @Test
    fun ensure_enabled_ensuresSavedTime() = runTest {
        settingsRepository.notificationSettings.value =
            NotificationSettings(reminderEnabled = true, reminderTime = LocalTime.of(7, 0))

        EnsureReminderScheduledUseCase(settingsRepository, scheduler)()

        assertEquals(listOf("ensure 07:00"), scheduler.calls)
    }

    @Test
    fun ensure_disabled_doesNothing() = runTest {
        EnsureReminderScheduledUseCase(settingsRepository, scheduler)()

        assertTrue(scheduler.calls.isEmpty())
    }
}
