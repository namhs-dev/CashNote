package com.namhs.cashnote.testing

import com.namhs.cashnote.domain.model.BudgetLevel
import com.namhs.cashnote.domain.model.NotificationSettings
import com.namhs.cashnote.domain.repository.SettingsRepository
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** 설정을 메모리에 두는 가짜 Repository. 기본 통화는 원화(null), 알림은 기본값(모두 꺼짐)으로 시작한다 */
class FakeSettingsRepository(initialDefaultCurrency: String? = null) : SettingsRepository {
    val defaultCurrency = MutableStateFlow(initialDefaultCurrency)
    val notificationSettings = MutableStateFlow(NotificationSettings())

    /** 마지막으로 기록한 예산 알림 (월, 단계) */
    var budgetAlerted: Pair<YearMonth, BudgetLevel>? = null

    override fun observeDefaultCurrency(): Flow<String?> = defaultCurrency

    override suspend fun setDefaultCurrency(currencyCode: String?) {
        defaultCurrency.value = currencyCode
    }

    override fun observeNotificationSettings(): Flow<NotificationSettings> = notificationSettings

    override suspend fun setReminderEnabled(enabled: Boolean) {
        notificationSettings.update { it.copy(reminderEnabled = enabled) }
    }

    override suspend fun setReminderTime(time: LocalTime) {
        notificationSettings.update { it.copy(reminderTime = time) }
    }

    override suspend fun setBudgetAlertEnabled(enabled: Boolean) {
        notificationSettings.update { it.copy(budgetAlertEnabled = enabled) }
    }

    override suspend fun getBudgetAlertedLevel(month: YearMonth): BudgetLevel? =
        budgetAlerted?.takeIf { it.first == month }?.second

    override suspend fun setBudgetAlertedLevel(month: YearMonth, level: BudgetLevel) {
        budgetAlerted = month to level
    }
}
