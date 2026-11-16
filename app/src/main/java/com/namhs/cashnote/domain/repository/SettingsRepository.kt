package com.namhs.cashnote.domain.repository

import com.namhs.cashnote.domain.model.BudgetLevel
import com.namhs.cashnote.domain.model.NotificationSettings
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

/** 사용자 설정 */
interface SettingsRepository {
    /** 새 거래 입력 화면이 처음 고를 통화 코드를 구독한다. null이면 원화 */
    fun observeDefaultCurrency(): Flow<String?>

    /** 기본 통화를 바꾼다. null이면 원화 */
    suspend fun setDefaultCurrency(currencyCode: String?)

    /** 알림 설정을 구독한다. 저장한 적이 없으면 기본값([NotificationSettings])이다 */
    fun observeNotificationSettings(): Flow<NotificationSettings>

    suspend fun setReminderEnabled(enabled: Boolean)

    suspend fun setReminderTime(time: LocalTime)

    suspend fun setBudgetAlertEnabled(enabled: Boolean)

    /** [month]에 이미 보낸 가장 높은 예산 알림 단계. 그 달에 보낸 적이 없으면 null */
    suspend fun getBudgetAlertedLevel(month: YearMonth): BudgetLevel?

    /** [month]에 [level] 단계 알림을 보냈다고 기록한다. 다른 달의 기록은 덮어쓴다 */
    suspend fun setBudgetAlertedLevel(month: YearMonth, level: BudgetLevel)
}
