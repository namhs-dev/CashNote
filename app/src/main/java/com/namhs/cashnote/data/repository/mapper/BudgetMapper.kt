package com.namhs.cashnote.data.repository.mapper

import com.namhs.cashnote.data.local.entity.BudgetEntity
import java.time.YearMonth

// Entity ↔ 예산 금액 변환. 0원 행은 "이 달부터 예산 없음"을 뜻하므로 null로 바꾼다

fun BudgetEntity?.toBudgetAmount(): Long? = this?.amount?.takeIf { it > 0 }

fun budgetEntityOf(startMonth: YearMonth, amount: Long): BudgetEntity =
    BudgetEntity(startMonth = startMonth, amount = amount)
