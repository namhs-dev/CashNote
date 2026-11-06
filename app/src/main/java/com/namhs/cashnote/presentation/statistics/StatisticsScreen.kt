package com.namhs.cashnote.presentation.statistics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.namhs.cashnote.R

// 월 합계·카테고리 통계는 M3에서 구현한다
@Composable
fun StatisticsScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.statistics_title),
            style = MaterialTheme.typography.titleLarge
        )
    }
}
