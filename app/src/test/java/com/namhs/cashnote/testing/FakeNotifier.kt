package com.namhs.cashnote.testing

import com.namhs.cashnote.domain.model.BudgetLevel
import com.namhs.cashnote.domain.repository.Notifier

/** 보낸 알림을 기록하는 가짜 Notifier */
class FakeNotifier : Notifier {
    var reminderCount = 0
        private set

    /** 보낸 예산 알림 (단계, 예산, 지출) */
    val budgetAlerts = mutableListOf<Triple<BudgetLevel, Long, Long>>()

    override fun showReminder() {
        reminderCount++
    }

    override fun showBudgetAlert(level: BudgetLevel, budget: Long, spent: Long) {
        budgetAlerts += Triple(level, budget, spent)
    }
}
