package com.namhs.cashnote.presentation.component

import java.text.NumberFormat

// 천 단위 구분 기호를 넣는다. 음수면 앞에 - 부호가 붙는다
internal fun formatAmount(amount: Long): String = NumberFormat.getNumberInstance().format(amount)
