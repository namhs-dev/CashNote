package com.namhs.cashnote.presentation.home

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.MonthlySummary
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.model.TransactionWithCategory
import com.namhs.cashnote.presentation.theme.CashNoteTheme
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/** ViewModel과 [HomeScreen]을 연결한다 */
@Composable
fun HomeRoute(
    onNavigateToEdit: (transactionId: Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is HomeSideEffect.NavigateToEdit -> onNavigateToEdit(effect.transactionId)

                // 스낵바가 닫힐 때까지 다음 이벤트 처리가 막히지 않도록 따로 띄운다
                HomeSideEffect.ShowDeleteUndo -> launch {
                    // 연속 삭제 시 이전 스낵바를 닫는다. 실행 취소는 마지막 삭제에만 적용된다
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.home_deleted_message),
                        actionLabel = resources.getString(R.string.action_undo),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(HomeIntent.UndoDeleteClicked)
                    }
                }

                is HomeSideEffect.ShowMessage -> launch {
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
                }
            }
        }
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    MonthSelector(
                        month = state.month,
                        onPrevious = { onIntent(HomeIntent.PreviousMonthClicked) },
                        onNext = { onIntent(HomeIntent.NextMonthClicked) }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onIntent(HomeIntent.AddClicked) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.action_add_transaction)
                )
            }
        },
        // 하단 내비게이션 바가 이미 시스템 바 여백을 처리하므로 중복으로 넣지 않는다
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        // 요약 카드는 고정하고 아래 목록만 스크롤한다
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            SummaryCard(
                summary = state.summary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            val contentModifier = Modifier
                .weight(1f)
                .fillMaxWidth()
            if (state.isEmpty) {
                EmptyContent(contentModifier)
            } else {
                TransactionList(
                    groups = state.dailyGroups,
                    onItemClick = { onIntent(HomeIntent.TransactionClicked(it)) },
                    onItemDelete = { onIntent(HomeIntent.DeleteRequested(it)) },
                    modifier = contentModifier
                )
            }
        }
    }
}

// 표시 중인 월의 수입·지출·합계
@Composable
private fun SummaryCard(summary: MonthlySummary, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryRow(label = stringResource(R.string.home_summary_income)) {
                AmountText(amount = summary.income, type = TransactionType.INCOME)
            }
            SummaryRow(label = stringResource(R.string.home_summary_expense)) {
                AmountText(amount = summary.expense, type = TransactionType.EXPENSE)
            }
            HorizontalDivider()
            SummaryRow(
                label = stringResource(R.string.home_summary_balance),
                labelStyle = MaterialTheme.typography.titleMedium
            ) {
                val formatted = remember(summary.balance) { formatAmount(summary.balance) }
                Text(
                    text = stringResource(R.string.home_amount, formatted),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

// 왼쪽 라벨, 오른쪽 금액. 스크린리더가 "수입, +3,000,000원"처럼 한 번에 읽도록 묶는다
@Composable
private fun SummaryRow(
    label: String,
    labelStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    amount: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = labelStyle)
        amount()
    }
}

// 상단 바 제목 자리: ‹ 2026년 11월 ›
@Composable
private fun MonthSelector(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionList(
    groups: List<DailyTransactions>,
    onItemClick: (Long) -> Unit,
    onItemDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val headerPattern = stringResource(R.string.home_date_header_pattern)
    val headerFormatter = remember(headerPattern) { DateTimeFormatter.ofPattern(headerPattern) }

    // 마지막 항목이 FAB에 가려지지 않도록 아래 여백을 둔다
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 88.dp)) {
        groups.forEach { group ->
            stickyHeader(key = group.date) {
                Text(
                    text = group.date.format(headerFormatter),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(items = group.items, key = { it.transaction.id }) { item ->
                SwipeToDeleteBox(onDelete = { onItemDelete(item.transaction.id) }) {
                    TransactionRow(item = item, onClick = { onItemClick(item.transaction.id) })
                }
            }
        }
    }
}

/** 오른쪽에서 왼쪽으로 끝까지 밀면 [onDelete]를 호출한다 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteBox(onDelete: () -> Unit, content: @Composable () -> Unit) {
    SwipeToDismissBox(
        state = rememberSwipeToDismissBoxState(),
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        enableDismissFromStartToEnd = false,
        onDismiss = { onDelete() }
    ) {
        content()
    }
}

@Composable
private fun TransactionRow(item: TransactionWithCategory, onClick: () -> Unit) {
    val transaction = item.transaction
    Row(
        // 밀 때 뒤의 삭제 배경이 비치지 않도록 앞면에 배경을 깐다
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color(item.category.color), CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.category.name, style = MaterialTheme.typography.bodyLarge)
            if (transaction.memo.isNotEmpty()) {
                Text(
                    text = transaction.memo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        AmountText(amount = transaction.amount, type = transaction.type)
    }
}

@Composable
private fun AmountText(amount: Long, type: TransactionType) {
    val formatted = remember(amount) { formatAmount(amount) }
    val (formatRes, color) = when (type) {
        TransactionType.INCOME -> R.string.home_amount_income to CashNoteTheme.colors.income
        TransactionType.EXPENSE -> R.string.home_amount_expense to CashNoteTheme.colors.expense
    }
    Text(
        text = stringResource(formatRes, formatted),
        style = MaterialTheme.typography.bodyLarge,
        color = color
    )
}

// 천 단위 구분 기호를 넣는다. 음수면 앞에 - 부호가 붙는다
private fun formatAmount(amount: Long): String = NumberFormat.getNumberInstance().format(amount)

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.home_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val previewFood = Category(1, "식비", TransactionType.EXPENSE, "food", 0xFFE57373)
private val previewSalary = Category(2, "급여", TransactionType.INCOME, "salary", 0xFF64B5F6)

private fun previewItem(id: Long, amount: Long, category: Category, memo: String, day: Int) =
    TransactionWithCategory(
        transaction = Transaction(
            id = id,
            amount = amount,
            type = category.type,
            categoryId = category.id,
            memo = memo,
            date = LocalDate.of(2026, 11, day),
            createdAt = Instant.EPOCH
        ),
        category = category
    )

@Preview(name = "목록", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun HomeScreenPreview() {
    CashNoteTheme {
        HomeScreen(
            state = HomeUiState(
                month = YearMonth.of(2026, 11),
                isLoading = false,
                dailyGroups = listOf(
                    DailyTransactions(
                        LocalDate.of(2026, 11, 10),
                        listOf(
                            previewItem(1, 12_500, previewFood, "점심", 10),
                            previewItem(2, 3_000_000, previewSalary, "", 10)
                        )
                    ),
                    DailyTransactions(
                        LocalDate.of(2026, 11, 9),
                        listOf(previewItem(3, 8_000, previewFood, "", 9))
                    )
                ),
                summary = MonthlySummary(income = 3_000_000, expense = 20_500)
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "빈 상태", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenEmptyPreview() {
    CashNoteTheme {
        HomeScreen(
            state = HomeUiState(month = YearMonth.of(2026, 11), isLoading = false),
            onIntent = {}
        )
    }
}
