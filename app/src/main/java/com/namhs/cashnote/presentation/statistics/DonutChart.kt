package com.namhs.cashnote.presentation.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.namhs.cashnote.R
import com.namhs.cashnote.presentation.theme.CashNoteTheme

/** 도넛 차트 조각 하나. [ratio]는 0~1, 한 차트의 비율을 모두 더하면 1이다 */
data class DonutSlice(val color: Color, val ratio: Float)

/**
 * 비율대로 조각을 그리는 도넛 차트. 12시 방향에서 시작해 시계 방향으로 [slices] 순서대로 그린다.
 * 차트 자체는 [contentDescription] 하나로 읽히므로, 실제 값은 범례 등 텍스트로 따로 보여 준다.
 */
@Composable
internal fun DonutChart(
    slices: List<DonutSlice>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 32.dp,
    centerContent: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics { this.contentDescription = contentDescription }
        ) {
            val stroke = strokeWidth.toPx()
            // 선 두께의 절반만큼 안쪽에 그려야 바깥 테두리가 잘리지 않는다
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            var startAngle = -90f
            slices.forEach { slice ->
                val sweepAngle = slice.ratio * 360f
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke)
                )
                startAngle += sweepAngle
            }
        }
        centerContent()
    }
}

@Preview
@Composable
private fun DonutChartPreview() {
    CashNoteTheme {
        Surface {
            DonutChart(
                slices = listOf(
                    DonutSlice(Color(0xFFE57373), 0.5f),
                    DonutSlice(Color(0xFF64B5F6), 0.3f),
                    DonutSlice(Color(0xFF81C784), 0.15f),
                    DonutSlice(Color(0xFFFFB74D), 0.05f)
                ),
                contentDescription = stringResource(R.string.statistics_chart_description),
                modifier = Modifier
                    .padding(16.dp)
                    .size(200.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("총 지출", style = MaterialTheme.typography.bodyMedium)
                    Text("40,000원", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
