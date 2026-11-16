package com.namhs.cashnote.presentation.component

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

// 천 단위 구분 기호를 넣는다. 음수면 앞에 - 부호가 붙는다
internal fun formatAmount(amount: Long): String = NumberFormat.getNumberInstance().format(amount)

/**
 * 외화 금액을 "USD 12.50"처럼 통화 코드와 함께 표시한다.
 *
 * 소수 자릿수는 그 통화의 표준을 따른다(USD 2자리, JPY 0자리). 저장된 값에 표준보다 많은 소수가 있으면
 * 정보를 잃지 않도록 그 자릿수까지 보여 준다. Java가 모르는 통화 코드면 2자리로 둔다.
 */
internal fun formatForeignAmount(
    currencyCode: String,
    amount: BigDecimal,
    locale: Locale = Locale.getDefault()
): String {
    val standardDigits = standardFractionDigits(currencyCode)
    // 1000은 stripTrailingZeros 후 scale이 음수(1E+3)가 되므로 0 아래로 내려가지 않게 한다
    val actualDigits = amount.stripTrailingZeros().scale().coerceAtLeast(0)
    val format = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = standardDigits
        maximumFractionDigits = maxOf(standardDigits, actualDigits)
    }
    return "$currencyCode ${format.format(amount)}"
}

private const val UNKNOWN_CURRENCY_FRACTION_DIGITS = 2

private fun standardFractionDigits(currencyCode: String): Int {
    val digits = try {
        Currency.getInstance(currencyCode).defaultFractionDigits
    } catch (e: IllegalArgumentException) {
        UNKNOWN_CURRENCY_FRACTION_DIGITS
    }
    // 금(XAU) 같은 특수 코드는 -1을 돌려준다
    return if (digits < 0) UNKNOWN_CURRENCY_FRACTION_DIGITS else digits
}
