package com.namhs.cashnote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.namhs.cashnote.domain.model.BudgetLevel
import com.namhs.cashnote.domain.model.NotificationSettings
import com.namhs.cashnote.domain.repository.SettingsRepository
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DataStore Preferences에 사용자 설정을 저장한다.
 *
 * 설정은 거래 데이터와 달리 값 몇 개뿐이라 Room 테이블 대신 DataStore에 둔다.
 */
class SettingsRepositoryImpl @Inject constructor(private val dataStore: DataStore<Preferences>) :
    SettingsRepository {
    override fun observeDefaultCurrency(): Flow<String?> =
        dataStore.data.map { it[DEFAULT_CURRENCY] }.distinctUntilChanged()

    override suspend fun setDefaultCurrency(currencyCode: String?) {
        dataStore.edit { preferences ->
            // 원화는 키를 지워 기본값(null)으로 되돌린다
            if (currencyCode == null) {
                preferences.remove(DEFAULT_CURRENCY)
            } else {
                preferences[DEFAULT_CURRENCY] = currencyCode
            }
        }
    }

    override fun observeNotificationSettings(): Flow<NotificationSettings> =
        dataStore.data.map { preferences ->
            NotificationSettings(
                reminderEnabled = preferences[REMINDER_ENABLED] ?: false,
                reminderTime = preferences[REMINDER_TIME]?.toLocalTimeOrNull()
                    ?: NotificationSettings.DEFAULT_REMINDER_TIME,
                budgetAlertEnabled = preferences[BUDGET_ALERT_ENABLED] ?: false
            )
        }.distinctUntilChanged()

    override suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[REMINDER_ENABLED] = enabled }
    }

    override suspend fun setReminderTime(time: LocalTime) {
        // "21:00" 형식(ISO-8601). 초는 쓰지 않는다
        dataStore.edit { it[REMINDER_TIME] = time.withSecond(0).withNano(0).toString() }
    }

    override suspend fun setBudgetAlertEnabled(enabled: Boolean) {
        dataStore.edit { it[BUDGET_ALERT_ENABLED] = enabled }
    }

    override suspend fun getBudgetAlertedLevel(month: YearMonth): BudgetLevel? {
        val preferences = dataStore.data.first()
        // 기록이 다른 달 것이면 이번 달에는 아직 보낸 적이 없다
        if (preferences[BUDGET_ALERTED_MONTH] != month.toString()) return null
        return preferences[BUDGET_ALERTED_LEVEL]?.let { name ->
            BudgetLevel.entries.firstOrNull { it.name == name }
        }
    }

    override suspend fun setBudgetAlertedLevel(month: YearMonth, level: BudgetLevel) {
        // 월과 단계는 짝이 맞아야 하므로 한 번에 쓴다
        dataStore.edit {
            it[BUDGET_ALERTED_MONTH] = month.toString()
            it[BUDGET_ALERTED_LEVEL] = level.name
        }
    }

    private fun String.toLocalTimeOrNull(): LocalTime? = try {
        LocalTime.parse(this)
    } catch (e: DateTimeParseException) {
        null
    }

    private companion object {
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val BUDGET_ALERT_ENABLED = booleanPreferencesKey("budget_alert_enabled")
        val BUDGET_ALERTED_MONTH = stringPreferencesKey("budget_alerted_month")
        val BUDGET_ALERTED_LEVEL = stringPreferencesKey("budget_alerted_level")
    }
}
