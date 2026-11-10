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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.model.TransactionWithCategory
import com.namhs.cashnote.presentation.theme.CashNoteTheme
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** ViewModel과 [HomeScreen]을 연결한다 */
@Composable
fun HomeRoute(
    onNavigateToEdit: (transactionId: Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is HomeSideEffect.NavigateToEdit -> onNavigateToEdit(effect.transactionId)
            }
        }
    }

    HomeScreen(state = state, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: HomeUiState, onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            R.string.home_month_title,
                            state.month.year,
                            state.month.monthValue
                        )
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
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        when {
            state.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.isEmpty -> EmptyContent(contentModifier)

            else -> TransactionList(
                groups = state.dailyGroups,
                onItemClick = { onIntent(HomeIntent.TransactionClicked(it)) },
                modifier = contentModifier
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionList(
    groups: List<DailyTransactions>,
    onItemClick: (Long) -> Unit,
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
                TransactionRow(item = item, onClick = { onItemClick(item.transaction.id) })
            }
        }
    }
}

@Composable
private fun TransactionRow(item: TransactionWithCategory, onClick: () -> Unit) {
    val transaction = item.transaction
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
    val formatted = remember(amount) { NumberFormat.getNumberInstance().format(amount) }
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
                )
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
