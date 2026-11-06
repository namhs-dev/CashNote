package com.namhs.cashnote.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

// 숫자 폭을 고정해 금액 자릿수가 세로로 정렬되게 한다
private const val TABULAR_NUMBERS = "tnum"

private val DefaultTypography = Typography()

val Typography = Typography(
    // 월 합계 등 큰 금액
    headlineMedium = DefaultTypography.headlineMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TABULAR_NUMBERS
    ),
    // 화면 제목
    titleLarge = DefaultTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    // 목록 항목 제목, 금액
    titleMedium = DefaultTypography.titleMedium.copy(fontFeatureSettings = TABULAR_NUMBERS),
    // 본문
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFeatureSettings = TABULAR_NUMBERS),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFeatureSettings = TABULAR_NUMBERS)
)
