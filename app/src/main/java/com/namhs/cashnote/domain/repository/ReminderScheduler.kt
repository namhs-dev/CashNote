package com.namhs.cashnote.domain.repository

import java.time.LocalTime

/** 매일 [LocalTime]에 기록 리마인더가 실행되도록 예약한다. 시각은 기기 시간대 기준이다 */
interface ReminderScheduler {
    /** 이미 예약이 있으면 지우고 [time]으로 새로 예약한다. 시각을 바꿀 때 쓴다 */
    fun schedule(time: LocalTime)

    /** 예약이 없을 때만 [time]으로 예약한다. 앱 시작 시 예약이 살아 있는지 확인할 때 쓴다 */
    fun ensureScheduled(time: LocalTime)

    fun cancel()
}
