package com.namhs.cashnote.presentation.transaction

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.namhs.cashnote.domain.model.ExchangeRate
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.presentation.theme.CashNoteTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CurrencySelector(
                selectedCode = state.currencyCode,
                options = state.foreignCurrencyOptions,
                exchangeRates = state.exchangeRates,
                onSelect = { onIntent(TransactionEditIntent.CurrencySelected(it)) },
                modifier = Modifier.width(CURRENCY_FIELD_WIDTH)
            )
            AmountField(
                state = state,
                onAmountChange = { onIntent(TransactionEditIntent.AmountChanged(it)) },
                modifier = Modifier.weight(1f)
            )
        }

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

/** 원화와 저장된 환율이 있는 외화 중에서 고른다. null은 원화 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencySelector(
    selectedCode: String?,
    options: List<String>,
    exchangeRates: List<ExchangeRate>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val krwLabel = stringResource(R.string.transaction_currency_krw)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCode ?: krwLabel,
            onValueChange = {},
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            label = { Text(stringResource(R.string.transaction_currency_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(krwLabel) },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            options.forEach { code ->
                val name = exchangeRates.find { it.currencyCode == code }?.currencyName
                DropdownMenuItem(
                    text = { Text(if (name == null) code else "$code · $name") },
                    onClick = {
                        onSelect(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** 금액 입력. 외화면 소수 입력 키보드를 쓰고 아래에 원화 환산 금액과 적용 환율을 보여 준다 */
@Composable
private fun AmountField(
    state: TransactionEditUiState,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val code = state.currencyCode
    val supportingText = when {
        state.isAmountError -> stringResource(R.string.transaction_amount_error)

        state.isRateUnavailable -> stringResource(R.string.transaction_rate_unavailable)

        code != null && state.appliedRate != null ->
            conversionText(code, state.appliedRate, state.rateDate, state.convertedKrw)

        else -> null
    }

    OutlinedTextField(
        value = state.amountInput,
        onValueChange = onAmountChange,
        modifier = modifier,
        label = { Text(stringResource(R.string.transaction_amount_label)) },
        suffix = { Text(code ?: stringResource(R.string.transaction_amount_suffix)) },
        isError = state.isAmountError || state.isRateUnavailable,
        supportingText = supportingText?.let { text -> { Text(text) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (code == null) KeyboardType.Number else KeyboardType.Decimal
        ),
        singleLine = true
    )
}

/** "≈ 18,131원 · 1 USD = 1,450.5원 · 2026. 11. 13. 기준" 형식의 안내 문구 */
@Composable
private fun conversionText(
    code: String,
    rate: BigDecimal,
    rateDate: LocalDate?,
    convertedKrw: Long?
): String {
    val rateText = remember(rate) { rateFormat().format(rate.stripTrailingZeros()) }
    val rateLine = if (rateDate == null) {
        stringResource(R.string.transaction_rate_saved, code, rateText)
    } else {
        val dateText = rateDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        stringResource(R.string.transaction_rate_latest, code, rateText, dateText)
    }
    if (convertedKrw == null) return rateLine
    val krwText = NumberFormat.getNumberInstance(Locale.KOREA).format(convertedKrw)
    return stringResource(R.string.transaction_converted_krw, krwText, rateLine)
}

// 환율은 외화 1단위당 원화라 JPY(9.3512)처럼 소수 넷째 자리까지 보여 준다
private fun rateFormat(): NumberFormat = NumberFormat.getNumberInstance(Locale.KOREA).apply {
    maximumFractionDigits = 4
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

private val CURRENCY_FIELD_WIDTH = 128.dp

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

private val previewUsd = ExchangeRate(
    currencyCode = "USD",
    currencyName = "미국 달러",
    unit = 1,
    baseRate = BigDecimal("1450.5"),
    rateDate = LocalDate.of(2026, 11, 13),
    updatedAt = Instant.parse("2026-11-13T03:00:00Z")
)

@Preview(name = "외화 입력", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun TransactionEditScreenForeignPreview() {
    CashNoteTheme {
        TransactionEditScreen(
            state = TransactionEditUiState(
                amountInput = "12.5",
                currencyCode = "USD",
                exchangeRates = listOf(previewUsd),
                appliedRate = previewUsd.ratePerUnit,
                rateDate = previewUsd.rateDate,
                categories = previewCategories,
                selectedCategoryId = 1,
                date = LocalDate.of(2026, 11, 13),
                memo = "커피"
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "환율 없음", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransactionEditScreenRateUnavailablePreview() {
    CashNoteTheme {
        TransactionEditScreen(
            state = TransactionEditUiState(
                amountInput = "20",
                currencyCode = "EUR",
                categories = previewCategories,
                date = LocalDate.of(2026, 11, 13)
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
