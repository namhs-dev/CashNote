package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.repository.ReminderScheduler
import com.namhs.cashnote.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * 리마인더가 켜져 있으면 예약이 살아 있는지 확인하고, 없으면 다시 예약한다.
 *
 * 예약은 실행할 때마다 다음 날로 이어지는데, 어떤 이유로 끊겼을 때 앱을 열면 되살아나게 한다.
 */
class EnsureReminderScheduledUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke() {
        val settings = settingsRepository.observeNotificationSettings().first()
        if (settings.reminderEnabled) reminderScheduler.ensureScheduled(settings.reminderTime)
    }
}
