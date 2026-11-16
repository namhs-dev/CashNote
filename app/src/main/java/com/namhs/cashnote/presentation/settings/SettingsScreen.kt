package com.namhs.cashnote.presentation.settings

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.ExchangeRate
import com.namhs.cashnote.domain.model.NotificationSettings
import com.namhs.cashnote.presentation.theme.CashNoteTheme
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsRoute(
    onNavigateToCategoryManage: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                SettingsSideEffect.NavigateToCategoryManage -> onNavigateToCategoryManage()
            }
        }
    }

    SettingsScreen(state = state, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    // 다이얼로그 표시 여부는 화면 안에서만 쓰는 UI 상태라 ViewModel에 두지 않는다
    var showCurrencyDialog by rememberSaveable { mutableStateOf(false) }
    var showTimeDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
        }
    ) { innerPadding ->
        // 설정 항목(기본 통화, 카테고리 관리 등)을 위에서부터 쌓는다
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_default_currency)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_default_currency_description))
                },
                trailingContent = {
                    Text(
                        text = state.defaultCurrency
                            ?: stringResource(R.string.transaction_currency_krw),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                modifier = Modifier.clickable { showCurrencyDialog = true }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.category_manage_title)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_category_manage_description))
                },
                modifier = Modifier.clickable {
                    onIntent(SettingsIntent.CategoryManageClicked)
                }
            )
            NotificationSection(
                settings = state.notification,
                onIntent = onIntent,
                onTimeClick = { showTimeDialog = true }
            )
        }
    }

    if (showTimeDialog) {
        ReminderTimeDialog(
            initialTime = state.notification.reminderTime,
            onConfirm = { time ->
                onIntent(SettingsIntent.ReminderTimeSelected(time))
                showTimeDialog = false
            },
            onDismiss = { showTimeDialog = false }
        )
    }

    if (showCurrencyDialog) {
        DefaultCurrencyDialog(
            selected = state.defaultCurrency,
            options = state.foreignCurrencyOptions,
            exchangeRates = state.exchangeRates,
            onSelect = { code ->
                onIntent(SettingsIntent.DefaultCurrencySelected(code))
                showCurrencyDialog = false
            },
            onDismiss = { showCurrencyDialog = false }
        )
    }
}

/** 기록 리마인더·알림 시간·예산 알림. 행 전체를 눌러도 스위치가 바뀐다 */
@Composable
private fun NotificationSection(
    settings: NotificationSettings,
    onIntent: (SettingsIntent) -> Unit,
    onTimeClick: () -> Unit
) {
    Text(
        text = stringResource(R.string.settings_section_notification),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
    SwitchItem(
        title = stringResource(R.string.settings_reminder),
        description = stringResource(R.string.settings_reminder_description),
        checked = settings.reminderEnabled,
        onCheckedChange = { onIntent(SettingsIntent.ReminderToggled(it)) }
    )
    val timeText = remember(settings.reminderTime) {
        settings.reminderTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }
    // 리마인더가 꺼져 있으면 시간은 의미가 없으므로 흐리게 두고 누를 수 없게 한다
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_reminder_time)) },
        trailingContent = { Text(text = timeText, style = MaterialTheme.typography.bodyLarge) },
        modifier = Modifier
            .clickable(enabled = settings.reminderEnabled, onClick = onTimeClick)
            .alpha(if (settings.reminderEnabled) 1f else DISABLED_ALPHA)
    )
    SwitchItem(
        title = stringResource(R.string.settings_budget_alert),
        description = stringResource(R.string.settings_budget_alert_description),
        checked = settings.budgetAlertEnabled,
        onCheckedChange = { onIntent(SettingsIntent.BudgetAlertToggled(it)) }
    )
}

@Composable
private fun SwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        // 행 전체가 클릭을 받으므로 Switch 자체의 클릭은 끈다
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(
            value = checked,
            onValueChange = onCheckedChange,
            role = Role.Switch
        )
    )
}

/** 시·분을 고르는 다이얼로그. 12/24시간 표시는 기기 설정을 따른다 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = DateFormat.is24HourFormat(context)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_reminder_time)) },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

private const val DISABLED_ALPHA = 0.38f

/** 원화와 저장된 환율 통화 중 하나를 고르는 다이얼로그. 고르면 바로 적용하고 닫는다 */
@Composable
private fun DefaultCurrencyDialog(
    selected: String?,
    options: List<String>,
    exchangeRates: List<ExchangeRate>,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val krwLabel = stringResource(R.string.transaction_currency_krw)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_default_currency)) },
        text = {
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .verticalScroll(rememberScrollState())
            ) {
                CurrencyOption(label = krwLabel, selected = selected == null) { onSelect(null) }
                options.forEach { code ->
                    val name = exchangeRates.find { it.currencyCode == code }?.currencyName
                    CurrencyOption(
                        label = if (name == null) code else "$code · $name",
                        selected = code == selected
                    ) { onSelect(code) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun CurrencyOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 행 전체가 클릭을 받으므로 RadioButton 자체의 클릭은 끈다
        RadioButton(selected = selected, onClick = null)
        Text(text = label, modifier = Modifier.padding(start = 12.dp))
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

@Preview(name = "설정", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SettingsScreenPreview() {
    CashNoteTheme {
        SettingsScreen(
            state = SettingsUiState(
                defaultCurrency = "USD",
                exchangeRates = listOf(previewUsd),
                notification = NotificationSettings(
                    reminderEnabled = true,
                    reminderTime = LocalTime.of(21, 30)
                )
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "알림 시간 다이얼로그", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ReminderTimeDialogPreview() {
    CashNoteTheme {
        ReminderTimeDialog(initialTime = LocalTime.of(21, 0), onConfirm = {}, onDismiss = {})
    }
}

@Preview(name = "기본 통화 다이얼로그", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultCurrencyDialogPreview() {
    CashNoteTheme {
        DefaultCurrencyDialog(
            selected = "USD",
            options = listOf("JPY", "USD"),
            exchangeRates = listOf(previewUsd),
            onSelect = {},
            onDismiss = {}
        )
    }
}
