package com.namhs.cashnote.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.namhs.cashnote.R

// 하단 내비게이션 바의 탭 목록
enum class TopLevelDestination(
    val destination: CashNoteDestination,
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int
) {
    HOME(CashNoteDestination.Home, R.drawable.ic_home, R.string.home_title),
    STATISTICS(CashNoteDestination.Statistics, R.drawable.ic_bar_chart, R.string.statistics_title)
}
