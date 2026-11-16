package com.namhs.cashnote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.namhs.cashnote.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private companion object {
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
    }
}
