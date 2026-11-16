package com.namhs.cashnote.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 카테고리 색 점. 홈 목록·통계 범례·카테고리 관리가 함께 쓴다.
 *
 * 노랑·연두처럼 밝은 색은 라이트 배경과 대비가 낮아 얇은 외곽선을 둘러 모양이 보이게 한다.
 * 색만으로 정보를 전하지 않도록 늘 카테고리 이름과 함께 쓴다.
 */
@Composable
fun CategoryColorDot(color: Long, modifier: Modifier = Modifier, size: Dp = 12.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(color), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
    )
}
