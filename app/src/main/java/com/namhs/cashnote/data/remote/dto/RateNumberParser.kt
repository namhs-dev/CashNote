package com.namhs.cashnote.data.remote.dto

import java.math.BigDecimal

/**
 * 환율 API의 숫자 문자열("1,234.56")을 [BigDecimal]로 바꾼다.
 *
 * 천 단위 쉼표와 앞뒤 공백을 지우고 변환한다. 비어 있거나 숫자가 아니면 null을 돌려준다.
 */
fun String.toRateOrNull(): BigDecimal? = replace(",", "").trim().toBigDecimalOrNull()
