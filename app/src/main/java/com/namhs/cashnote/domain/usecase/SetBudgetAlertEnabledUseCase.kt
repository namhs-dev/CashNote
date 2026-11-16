package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.repository.SettingsRepository
import javax.inject.Inject

/** 예산 알림을 켜거나 끈다. 알림 권한 확인은 화면에서 먼저 한다 */
class SetBudgetAlertEnabledUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setBudgetAlertEnabled(enabled)
}
