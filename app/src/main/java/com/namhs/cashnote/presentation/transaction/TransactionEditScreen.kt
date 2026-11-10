package com.namhs.cashnote.presentation.transaction

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.presentation.theme.CashNoteTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

/** ViewModel과 [TransactionEditScreen]을 연결한다 */
@Composable
fun TransactionEditRoute(
    onBack: () -> Unit,
    viewModel: TransactionEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                TransactionEditSideEffect.NavigateBack -> onBack()

                // 스낵바가 닫힐 때까지 다음 이벤트 처리가 막히지 않도록 따로 띄운다
                is TransactionEditSideEffect.ShowMessage -> launch {
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
                }
            }
        }
    }

    TransactionEditScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    state: TransactionEditUiState,
    onIntent: (TransactionEditIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val titleRes = if (state.isEditMode) {
        R.string.transaction_edit_title_edit
    } else {
        R.string.transaction_edit_title
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(TransactionEditIntent.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_navigate_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
        } else {
            TransactionEditForm(
                state = state,
                onIntent = onIntent,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun TransactionEditForm(
    state: TransactionEditUiState,
    onIntent: (TransactionEditIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TypeSelector(
            selected = state.type,
            onSelect = { onIntent(TransactionEditIntent.TypeChanged(it)) }
        )

        OutlinedTextField(
            value = state.amountInput,
            onValueChange = { onIntent(TransactionEditIntent.AmountChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.transaction_amount_label)) },
            suffix = { Text(stringResource(R.string.transaction_amount_suffix)) },
            isError = state.isAmountError,
            supportingText = if (state.isAmountError) {
                { Text(stringResource(R.string.transaction_amount_error)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        CategorySelector(
            categories = state.categories,
            selectedId = state.selectedCategoryId,
            isError = state.isCategoryError,
            onSelect = { onIntent(TransactionEditIntent.CategorySelected(it)) }
        )

        DateField(
            date = state.date,
            onDateSelect = { onIntent(TransactionEditIntent.DateSelected(it)) }
        )

        OutlinedTextField(
            value = state.memo,
            onValueChange = { onIntent(TransactionEditIntent.MemoChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.transaction_memo_label)) },
            singleLine = true
        )

        Button(
            onClick = { onIntent(TransactionEditIntent.SaveClicked) },
            enabled = state.isSaveEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_save))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSelector(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
    // 지출을 더 자주 입력하므로 앞에 둔다
    val options = listOf(
        TransactionType.EXPENSE to R.string.transaction_type_expense,
        TransactionType.INCOME to R.string.transaction_type_income
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
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
private fun CategorySelector(
    categories: List<Category>,
    selectedId: Long?,
    isError: Boolean,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.transaction_category_label),
            style = MaterialTheme.typography.labelLarge
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                FilterChip(
                    selected = category.id == selectedId,
                    onClick = { onSelect(category.id) },
                    label = { Text(category.name) }
                )
            }
        }
        if (isError) {
            Text(
                text = stringResource(R.string.transaction_category_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(date: LocalDate, onDateSelect: (LocalDate) -> Unit) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }

    // 읽기 전용 입력창은 클릭을 받지 않으므로 위에 투명한 클릭 영역을 덮는다
    Box {
        OutlinedTextField(
            value = date.format(formatter),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text(stringResource(R.string.transaction_date_label)) },
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = pickerState.selectedDateMillis
                        if (millis != null) onDateSelect(utcMillisToLocalDate(millis))
                        showDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// DatePicker는 UTC 자정 기준 밀리초를 쓴다
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun utcMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate()

private val previewCategories = listOf(
    Category(1, "식비", TransactionType.EXPENSE, "food", 0xFFE57373),
    Category(2, "교통", TransactionType.EXPENSE, "transport", 0xFF64B5F6),
    Category(3, "쇼핑", TransactionType.EXPENSE, "shopping", 0xFFBA68C8)
)

@Preview(name = "입력", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun TransactionEditScreenPreview() {
    CashNoteTheme {
        TransactionEditScreen(
            state = TransactionEditUiState(
                amountInput = "12500",
                categories = previewCategories,
                selectedCategoryId = 1,
                date = LocalDate.of(2026, 11, 10),
                memo = "점심"
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "검증 오류", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransactionEditScreenErrorPreview() {
    CashNoteTheme {
        TransactionEditScreen(
            state = TransactionEditUiState(
                categories = previewCategories,
                date = LocalDate.of(2026, 11, 10),
                isAmountError = true,
                isCategoryError = true
            ),
            onIntent = {}
        )
    }
}
