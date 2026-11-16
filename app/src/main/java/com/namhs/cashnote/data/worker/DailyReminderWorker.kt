package com.namhs.cashnote.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.namhs.cashnote.domain.usecase.GetNotificationSettingsUseCase
import com.namhs.cashnote.domain.usecase.SendDailyReminderUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * 지정 시각에 실행돼 오늘 기록이 없으면 리마인더를 보내고, 다음 날 같은 시각으로 다시 예약한다.
 *
 * 알림을 보내지 못해도 다음 날 예약은 반드시 이어 간다.
 */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sendDailyReminder: SendDailyReminderUseCase,
    private val getNotificationSettings: GetNotificationSettingsUseCase,
    private val scheduler: WorkManagerReminderScheduler
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        try {
            sendDailyReminder(LocalDate.now())
        } finally {
            val settings = getNotificationSettings().first()
            if (settings.reminderEnabled) scheduler.scheduleNext(settings.reminderTime)
        }
        return Result.success()
    }
}
