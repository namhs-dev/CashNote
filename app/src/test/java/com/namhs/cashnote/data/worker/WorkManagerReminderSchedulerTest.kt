package com.namhs.cashnote.data.worker

import com.namhs.cashnote.data.worker.WorkManagerReminderScheduler.Companion.delayUntilNext
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkManagerReminderSchedulerTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private val nine = LocalTime.of(21, 0)

    private fun at(hour: Int, minute: Int, second: Int = 0) =
        ZonedDateTime.of(2026, 11, 16, hour, minute, second, 0, seoul)

    @Test
    fun beforeTime_waitsUntilTodayTime() {
        assertEquals(Duration.ofHours(3), delayUntilNext(at(18, 0), nine))
    }

    @Test
    fun afterTime_waitsUntilTomorrowTime() {
        assertEquals(Duration.ofHours(23), delayUntilNext(at(22, 0), nine))
    }

    @Test
    fun exactlyAtTime_waitsFullDay() {
        // 방금 실행된 Worker가 다시 예약할 때 같은 시각에 두 번 울리지 않게 한다
        assertEquals(Duration.ofDays(1), delayUntilNext(at(21, 0), nine))
    }

    @Test
    fun secondsPastTime_waitsUntilTomorrow() {
        assertEquals(Duration.ofHours(24).minusSeconds(5), delayUntilNext(at(21, 0, 5), nine))
    }

    @Test
    fun earlyMorningTime_beforeMidnight_isTomorrowMorning() {
        // 23:30에 07:00 리마인더를 예약하면 자정을 넘어 7시간 30분 뒤다
        assertEquals(
            Duration.ofHours(7).plusMinutes(30),
            delayUntilNext(at(23, 30), LocalTime.of(7, 0))
        )
    }

    @Test
    fun daylightSavingDay_usesWallClockTime() {
        // 미국 서머타임 시작일(2026-03-08)은 하루가 23시간이다. 다음 날 같은 벽시계 시각을 지킨다
        val newYork = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 7, 22, 0, 0, 0, newYork)

        assertEquals(Duration.ofHours(22), delayUntilNext(now, nine))
    }
}
