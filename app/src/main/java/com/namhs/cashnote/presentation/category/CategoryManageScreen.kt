package com.namhs.cashnote.presentation.category

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.presentation.theme.CashNoteTheme

@Composable
fun CategoryManageRoute(onBack: () -> Unit, viewModel: CategoryManageViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                CategoryManageSideEffect.NavigateBack -> onBack()
            }
        }
    }

    CategoryManageScreen(state = state, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(
    state: CategoryManageUiState,
    onIntent: (CategoryManageIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_manage_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(CategoryManageIntent.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TypeTabs(
                selected = state.type,
                onSelect = { onIntent(CategoryManageIntent.TypeChanged(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.categories, key = { it.id }) { category ->
                    CategoryRow(category)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeTabs(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    // 거래 입력 화면과 같은 순서로 지출을 앞에 둔다
    val options = listOf(
        TransactionType.EXPENSE to R.string.transaction_type_expense,
        TransactionType.INCOME to R.string.transaction_type_income
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (type, labelRes) ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(stringResource(labelRes))
            }
        }
    }
}

@Composable
private fun CategoryRow(category: Category) {
    ListItem(
        headlineContent = { Text(category.name) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(category.color), CircleShape)
            )
        }
    )
}

@Preview(name = "카테고리 관리", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun CategoryManageScreenPreview() {
    CashNoteTheme {
        CategoryManageScreen(
            state = CategoryManageUiState(
                categories = listOf(
                    Category(1, "식비", TransactionType.EXPENSE, "food", 0xFFFF8A65),
                    Category(2, "교통", TransactionType.EXPENSE, "transport", 0xFF4FC3F7),
                    Category(3, "쇼핑", TransactionType.EXPENSE, "shopping", 0xFFBA68C8)
                )
            ),
            onIntent = {}
        )
    }
}
