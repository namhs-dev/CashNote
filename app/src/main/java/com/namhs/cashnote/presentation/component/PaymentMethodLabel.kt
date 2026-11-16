package com.namhs.cashnote.presentation.component

import androidx.annotation.StringRes
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.PaymentMethod

/** 결제수단의 화면 표시 이름. 입력 화면과 홈 목록이 함께 쓴다 */
@StringRes
fun PaymentMethod.labelRes(): Int = when (this) {
    PaymentMethod.CASH -> R.string.payment_method_cash
    PaymentMethod.CARD -> R.string.payment_method_card
    PaymentMethod.OTHER -> R.string.payment_method_other
}
