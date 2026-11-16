package com.namhs.cashnote.testing

import com.namhs.cashnote.domain.repository.ReminderScheduler
import java.time.LocalTime

/** 예약 상태를 메모리에 두는 가짜 스케줄러 */
class FakeReminderScheduler : ReminderScheduler {
    /** 지금 예약된 시각. 없으면 null */
    var scheduledTime: LocalTime? = null
        private set

    /** [schedule]·[ensureScheduled]·[cancel] 호출 기록 */
    val calls = mutableListOf<String>()

    override fun schedule(time: LocalTime) {
        calls += "schedule $time"
        scheduledTime = time
    }

    override fun ensureScheduled(time: LocalTime) {
        calls += "ensure $time"
        if (scheduledTime == null) scheduledTime = time
    }

    override fun cancel() {
        calls += "cancel"
        scheduledTime = null
    }
}
