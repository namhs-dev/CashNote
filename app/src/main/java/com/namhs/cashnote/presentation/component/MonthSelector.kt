package com.namhs.cashnote.presentation.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.namhs.cashnote.R
import java.time.YearMonth

// 상단 바 제목 자리: ‹ 2026년 11월 ›. 홈과 통계 화면이 함께 쓴다
@Composable
internal fun MonthSelector(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = stringResource(R.string.home_previous_month)
            )
        }
        Text(stringResource(R.string.home_month_title, month.year, month.monthValue))
        IconButton(onClick = onNext) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.home_next_month)
            )
        }
    }
}
