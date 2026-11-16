package com.namhs.cashnote.domain.repository

import com.namhs.cashnote.domain.model.BudgetLevel

/**
 * 사용자에게 알림을 보낸다.
 *
 * 보낼지 말지는 UseCase가 정하고, 여기서는 표시만 한다. 알림 권한이 없으면 아무것도 하지 않는다.
 */
interface Notifier {
    /** 오늘 기록을 남기라는 리마인더 */
    fun showReminder()

    /** 이번 달 지출이 예산의 [level] 단계에 이르렀다. [BudgetLevel.NORMAL]이면 보내지 않는다 */
    fun showBudgetAlert(level: BudgetLevel, budget: Long, spent: Long)
}
