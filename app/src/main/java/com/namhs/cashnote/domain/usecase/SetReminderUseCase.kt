package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.repository.ReminderScheduler
import com.namhs.cashnote.domain.repository.SettingsRepository
import java.time.LocalTime
import javax.inject.Inject

/**
 * 기록 리마인더 설정을 저장하고, 켜면 [time]으로 예약하고 끄면 예약을 지운다.
 *
 * 알림 권한 확인은 화면에서 먼저 한다.
 */
class SetReminderUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(enabled: Boolean, time: LocalTime) {
        settingsRepository.setReminderTime(time)
        settingsRepository.setReminderEnabled(enabled)
        if (enabled) reminderScheduler.schedule(time) else reminderScheduler.cancel()
    }
}
