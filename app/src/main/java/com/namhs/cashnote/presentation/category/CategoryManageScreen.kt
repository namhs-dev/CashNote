package com.namhs.cashnote.presentation.category

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.presentation.component.CategoryColorDot
import com.namhs.cashnote.presentation.component.DialogAdjustResize
import com.namhs.cashnote.presentation.theme.CashNoteTheme
import kotlinx.coroutines.launch

@Composable
fun CategoryManageRoute(onBack: () -> Unit, viewModel: CategoryManageViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                CategoryManageSideEffect.NavigateBack -> onBack()

                // 스낵바가 닫힐 때까지 다음 이벤트 처리가 막히지 않도록 따로 띄운다
                is CategoryManageSideEffect.ShowMessage -> launch {
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
                }

                is CategoryManageSideEffect.ShowCategoryInUse -> launch {
                    snackbarHostState.showSnackbar(
                        resources.getString(
                            R.string.category_delete_in_use,
                            effect.transactionCount
                        )
                    )
                }
            }
        }
    }

    CategoryManageScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(
    state: CategoryManageUiState,
    onIntent: (CategoryManageIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onIntent(CategoryManageIntent.AddClicked) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.category_add)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    CategoryRow(
                        category = category,
                        onClick = { onIntent(CategoryManageIntent.CategoryClicked(category)) }
                    )
                }
            }
        }
    }

    state.editor?.let { editor ->
        CategoryEditorDialog(editor = editor, onIntent = onIntent)
        if (editor.isConfirmingDelete) {
            // 입력 중에 바꾼 이름이 아니라 저장된 이름으로 묻는다
            val savedName = state.categories.firstOrNull { it.id == editor.categoryId }?.name
            DeleteConfirmDialog(name = savedName ?: editor.name, onIntent = onIntent)
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
private fun CategoryRow(category: Category, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(category.name) },
        leadingContent = { CategoryColorDot(color = category.color) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/** 이름과 색상을 입력하는 다이얼로그. 유형은 보고 있는 탭을 따른다 */
@Composable
private fun CategoryEditorDialog(
    editor: CategoryEditorState,
    onIntent: (CategoryManageIntent) -> Unit
) {
    val titleRes = if (editor.isNew) R.string.category_add else R.string.category_edit
    val errorText = when (editor.nameError) {
        CategoryNameError.EMPTY -> stringResource(R.string.category_name_empty)
        CategoryNameError.DUPLICATE -> stringResource(R.string.category_name_duplicate)
        null -> null
    }

    AlertDialog(
        onDismissRequest = { onIntent(CategoryManageIntent.EditorDismissed) },
        title = { Text(stringResource(titleRes)) },
        text = {
            // 키보드가 올라오면 다이얼로그 창을 키보드 위로 줄이고, 본문은 스크롤되게 둔다.
            // 그러면 AlertDialog가 본문만 줄이고 저장·취소 버튼은 키보드 위에 남긴다 (큰 글꼴에서 버튼이 가려지던 문제)
            DialogAdjustResize()
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { onIntent(CategoryManageIntent.EditorNameChanged(it)) },
                    label = { Text(stringResource(R.string.category_name_label)) },
                    isError = errorText != null,
                    supportingText = errorText?.let { text -> { Text(text) } },
                    singleLine = true
                )
                Text(
                    text = stringResource(R.string.category_color_label),
                    style = MaterialTheme.typography.labelLarge
                )
                // 각 색은 48dp 터치 영역 안에 36dp 원으로 그려 원 사이 간격이 따로 필요 없다
                FlowRow {
                    categoryColorPalette.forEach { color ->
                        ColorOption(
                            color = color,
                            selected = color == editor.color,
                            onClick = { onIntent(CategoryManageIntent.EditorColorSelected(color)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onIntent(CategoryManageIntent.EditorSaveClicked) },
                enabled = !editor.isSaving
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                // 새 카테고리는 아직 저장 전이라 지울 것이 없다
                if (!editor.isNew) {
                    TextButton(
                        onClick = { onIntent(CategoryManageIntent.EditorDeleteClicked) },
                        enabled = !editor.isSaving
                    ) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(onClick = { onIntent(CategoryManageIntent.EditorDismissed) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(name: String, onIntent: (CategoryManageIntent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onIntent(CategoryManageIntent.DeleteCancelled) },
        title = { Text(stringResource(R.string.category_delete_confirm_title, name)) },
        text = { Text(stringResource(R.string.category_delete_confirm_message)) },
        confirmButton = {
            TextButton(onClick = { onIntent(CategoryManageIntent.DeleteConfirmed) }) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(CategoryManageIntent.DeleteCancelled) }) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun ColorOption(color: Long, selected: Boolean, onClick: () -> Unit) {
    // 고른 색은 바깥에 테두리를 둘러 표시한다
    val borderColor = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
    val name = categoryColorNameRes(color)?.let { stringResource(it) }
    // 터치 영역은 권장 최소 크기 48dp, 보이는 원은 36dp
    Box(
        modifier = Modifier
            .size(48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { if (name != null) contentDescription = name },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(2.dp, borderColor, CircleShape)
                .padding(4.dp)
                .background(Color(color), CircleShape)
                // 밝은 색도 라이트 배경에서 모양이 보이게 얇은 외곽선을 두른다
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
    }
}

private val previewCategories = listOf(
    Category(1, "식비", TransactionType.EXPENSE, "food", 0xFFFF8A65),
    Category(2, "교통", TransactionType.EXPENSE, "transport", 0xFF4FC3F7),
    Category(3, "쇼핑", TransactionType.EXPENSE, "shopping", 0xFFBA68C8)
)

@Preview(name = "카테고리 관리", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun CategoryManageScreenPreview() {
    CashNoteTheme {
        CategoryManageScreen(
            state = CategoryManageUiState(categories = previewCategories),
            onIntent = {}
        )
    }
}

@Preview(name = "카테고리 편집", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryEditorDialogPreview() {
    CashNoteTheme {
        CategoryEditorDialog(
            editor = CategoryEditorState(
                categoryId = 1,
                name = "식비",
                color = 0xFFFF8A65,
                nameError = CategoryNameError.DUPLICATE
            ),
            onIntent = {}
        )
    }
}
