package com.namhs.cashnote.domain.model

import java.time.LocalTime

/** 알림 설정. 처음에는 모두 꺼져 있다 */
data class NotificationSettings(
    /** 매일 [reminderTime]에 기록 리마인더를 보낸다 */
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = DEFAULT_REMINDER_TIME,
    /** 거래 저장으로 이번 달 지출이 예산의 80%·100%에 이르면 알린다 */
    val budgetAlertEnabled: Boolean = false
) {
    companion object {
        /** 하루를 마무리하며 기록하기 좋은 시각 */
        val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(21, 0)
    }
}
