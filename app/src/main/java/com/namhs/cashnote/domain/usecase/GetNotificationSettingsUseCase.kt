package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.NotificationSettings
import com.namhs.cashnote.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 알림 설정(리마인더 켜기·시각, 예산 알림 켜기)을 구독한다 */
class GetNotificationSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<NotificationSettings> = repository.observeNotificationSettings()
}
