package com.namhs.cashnote.presentation.statistics

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.CategoryExpense
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.presentation.component.MonthSelector
import com.namhs.cashnote.presentation.component.formatAmount
import com.namhs.cashnote.presentation.theme.CashNoteTheme
import java.time.YearMonth
import kotlin.math.roundToInt

/** ViewModel과 [StatisticsScreen]을 연결한다. 아직 일회성 이벤트가 없어 SideEffect는 수집하지 않는다 */
@Composable
fun StatisticsRoute(viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    StatisticsScreen(state = state, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    onIntent: (StatisticsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    MonthSelector(
                        month = state.month,
                        onPrevious = { onIntent(StatisticsIntent.PreviousMonthClicked) },
                        onNext = { onIntent(StatisticsIntent.NextMonthClicked) }
                    )
                }
            )
        },
        // 하단 내비게이션 바가 이미 시스템 바 여백을 처리하므로 중복으로 넣지 않는다
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        when {
            state.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.isEmpty -> Box(contentModifier, contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.statistics_empty),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            else -> ExpenseContent(state, contentModifier)
        }
    }
}

// 위에 도넛 차트, 아래에 카테고리별 범례
@Composable
private fun ExpenseContent(state: StatisticsUiState, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                DonutChart(
                    slices = state.expenses.map { DonutSlice(Color(it.category.color), it.ratio) },
                    contentDescription = stringResource(R.string.statistics_chart_description),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    TotalExpense(state.totalExpense)
                }
            }
        }
        items(state.expenses, key = { it.category.id }) { expense ->
            LegendRow(expense)
        }
    }
}

// 도넛 가운데: 총 지출
@Composable
private fun TotalExpense(total: Long) {
    val formatted = remember(total) { formatAmount(total) }
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.statistics_total_expense),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.home_amount, formatted),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

// ● 식비  75%  30,000원. 스크린리더가 한 줄을 한 번에 읽도록 묶는다
@Composable
private fun LegendRow(expense: CategoryExpense) {
    val formatted = remember(expense.amount) { formatAmount(expense.amount) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color(expense.category.color), CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = expense.category.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        // 반올림한 값이라 모두 더해도 정확히 100%가 아닐 수 있다
        Text(
            text = stringResource(R.string.statistics_percent, (expense.ratio * 100).roundToInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.home_amount, formatted),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun previewExpense(id: Long, name: String, color: Long, amount: Long, ratio: Float) =
    CategoryExpense(
        category = Category(id, name, TransactionType.EXPENSE, "icon", color),
        amount = amount,
        ratio = ratio
    )

@Preview(name = "차트와 범례", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun StatisticsScreenPreview() {
    CashNoteTheme {
        StatisticsScreen(
            state = StatisticsUiState(
                month = YearMonth.of(2026, 11),
                isLoading = false,
                expenses = listOf(
                    previewExpense(1, "식비", 0xFFE57373, 300_000, 0.5f),
                    previewExpense(2, "교통", 0xFF64B5F6, 180_000, 0.3f),
                    previewExpense(3, "쇼핑", 0xFF81C784, 90_000, 0.15f),
                    previewExpense(4, "문화", 0xFFFFB74D, 30_000, 0.05f)
                )
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "빈 상태", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatisticsScreenEmptyPreview() {
    CashNoteTheme {
        StatisticsScreen(
            state = StatisticsUiState(month = YearMonth.of(2026, 11), isLoading = false),
            onIntent = {}
        )
    }
}
