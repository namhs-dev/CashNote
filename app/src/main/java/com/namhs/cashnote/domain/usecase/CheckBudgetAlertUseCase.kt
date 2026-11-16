package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.BudgetLevel
import com.namhs.cashnote.domain.repository.Notifier
import com.namhs.cashnote.domain.repository.SettingsRepository
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * [month]의 지출이 예산의 80%·100%에 처음 이르렀으면 예산 알림을 보낸다. 거래를 저장한 뒤 부른다.
 *
 * 같은 달에는 단계마다 한 번만 보낸다. 80% 알림 뒤 100%에 이르면 한 번 더 보내고,
 * 지출이 줄었다 다시 늘어도 이미 보낸 단계는 다시 보내지 않는다. 다음 달에는 처음부터 센다.
 *
 * @return 알림을 보냈으면 true
 */
class CheckBudgetAlertUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val getBudgetStatus: GetBudgetStatusUseCase,
    private val notifier: Notifier
) {
    suspend operator fun invoke(month: YearMonth): Boolean {
        val settings = settingsRepository.observeNotificationSettings().first()
        if (!settings.budgetAlertEnabled) return false

        // 예산이 없는 달은 알릴 기준이 없다
        val status = getBudgetStatus(month).first() ?: return false
        val level = status.level
        if (level == BudgetLevel.NORMAL) return false

        val alerted = settingsRepository.getBudgetAlertedLevel(month)
        if (alerted != null && level <= alerted) return false

        notifier.showBudgetAlert(level, status.budget, status.spent)
        settingsRepository.setBudgetAlertedLevel(month, level)
        return true
    }
}
