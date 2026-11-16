package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.repository.Notifier
import com.namhs.cashnote.domain.repository.SettingsRepository
import com.namhs.cashnote.domain.repository.TransactionRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * 리마인더가 켜져 있고 [today]에 기록한 거래가 없으면 리마인더를 보낸다.
 *
 * 이미 기록한 날에는 조용히 넘어간다.
 *
 * @return 리마인더를 보냈으면 true
 */
class SendDailyReminderUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val notifier: Notifier
) {
    suspend operator fun invoke(today: LocalDate): Boolean {
        // 끈 직후 이미 예약된 작업이 실행돼도 보내지 않는다
        if (!settingsRepository.observeNotificationSettings().first().reminderEnabled) return false

        val hasRecordToday = transactionRepository.observeTransactions(YearMonth.from(today))
            .first()
            .any { it.date == today }
        if (hasRecordToday) return false

        notifier.showReminder()
        return true
    }
}
