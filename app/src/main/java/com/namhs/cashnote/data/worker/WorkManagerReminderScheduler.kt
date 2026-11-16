package com.namhs.cashnote.data.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.namhs.cashnote.domain.repository.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 다음 지정 시각까지 기다렸다 한 번 실행하는 작업으로 리마인더를 예약한다.
 *
 * 주기 작업(24시간)은 실행 시각이 조금씩 밀리므로 쓰지 않는다.
 * 대신 [DailyReminderWorker]가 실행될 때마다 [scheduleNext]로 다음 날을 다시 예약한다.
 */
@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderScheduler {
    override fun schedule(time: LocalTime) = enqueue(time, ExistingWorkPolicy.REPLACE)

    override fun ensureScheduled(time: LocalTime) = enqueue(time, ExistingWorkPolicy.KEEP)

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * 실행 중인 Worker가 다음 날을 예약한다.
     * REPLACE는 같은 이름의 실행 중인 자신을 취소하므로, 지금 작업 뒤에 이어 붙인다.
     */
    fun scheduleNext(time: LocalTime) = enqueue(time, ExistingWorkPolicy.APPEND_OR_REPLACE)

    private fun enqueue(time: LocalTime, policy: ExistingWorkPolicy) {
        val delay = delayUntilNext(ZonedDateTime.now(), time)
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
    }

    companion object {
        const val WORK_NAME = "daily_reminder"

        /**
         * [now]부터 다음 [time]까지 남은 시간. 오늘 그 시각이 지났거나 지금이면 내일 그 시각이다.
         * 시간대 규칙(서머타임 등)은 [now]의 시간대를 따른다.
         */
        internal fun delayUntilNext(now: ZonedDateTime, time: LocalTime): Duration {
            val today = now.toLocalDate()
            val todayAtTime = today.atTime(time).atZone(now.zone)
            val next = if (todayAtTime.isAfter(now)) {
                todayAtTime
            } else {
                today.plusDays(1).atTime(time).atZone(now.zone)
            }
            return Duration.between(now, next)
        }
    }
}
