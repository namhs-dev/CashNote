package com.namhs.cashnote.presentation.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(
    onNavigateToCategoryManage: () -> Unit,
    onNavigateToOssLicenses: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = LocalActivity.current

    // 권한 창이 떠 있는 동안 화면이 다시 만들어져도 무엇을 켜려 했는지 잃지 않게 저장한다
    var permissionTarget by rememberSaveable { mutableStateOf<NotificationTarget?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = permissionTarget ?: return@rememberLauncherForActivityResult
        permissionTarget = null
        // 거부 직후 rationale이 false면 "다시 묻지 않기" 상태라 시스템 창을 더 띄울 수 없다
        val canAskAgain = activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity, POST_NOTIFICATIONS)
        viewModel.onIntent(
            SettingsIntent.NotificationPermissionResult(target, granted, canAskAgain)
        )
    }

    // 설정 앱에서 알림을 바꾸고 돌아올 수 있으므로 화면이 보일 때마다 다시 확인한다
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(
            SettingsIntent.NotificationPermissionChecked(canPostNotifications(context))
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                SettingsSideEffect.NavigateToCategoryManage -> onNavigateToCategoryManage()

                SettingsSideEffect.NavigateToOssLicenses -> onNavigateToOssLicenses()

                is SettingsSideEffect.RequestNotificationPermission -> when {
                    // 그새 허용됐으면 바로 켠다
                    canPostNotifications(context) -> viewModel.onIntent(
                        SettingsIntent.NotificationPermissionResult(effect.target, true, true)
                    )

                    needsRuntimePermission(context) -> {
                        permissionTarget = effect.target
                        permissionLauncher.launch(POST_NOTIFICATIONS)
                    }

                    // 권한은 있는데 앱 알림이 꺼져 있다(또는 Android 12 이하). 설정 앱에서만 켤 수 있다
                    else -> viewModel.onIntent(
                        SettingsIntent.NotificationPermissionResult(effect.target, false, false)
                    )
                }

                SettingsSideEffect.OpenNotificationSettings -> openNotificationSettings(context)

                is SettingsSideEffect.ShowMessage -> launch {
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
                }
            }
        }
    }

    SettingsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState
    )
}

/** Android 13+ 권한이 있고 사용자가 앱 알림을 꺼 두지 않았다 */
private fun canPostNotifications(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

/** Android 13+에서 아직 알림 권한을 받지 않았다 */
private fun needsRuntimePermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

/** 이 앱의 시스템 알림 설정 화면 */
private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    context.startActivity(intent)
}

private const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    // 다이얼로그 표시 여부는 화면 안에서만 쓰는 UI 상태라 ViewModel에 두지 않는다
    var showCurrencyDialog by rememberSaveable { mutableStateOf(false) }
    var showTimeDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                isBlocked = state.isNotificationBlocked,
                onIntent = onIntent,
                onTimeClick = { showTimeDialog = true }
            )
            SectionHeader(stringResource(R.string.settings_section_app_info))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_app_version)) },
                trailingContent = {
                    Text(text = state.appVersion, style = MaterialTheme.typography.bodyLarge)
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.oss_licenses_title)) },
                modifier = Modifier.clickable { onIntent(SettingsIntent.OssLicensesClicked) }
            )
        }
    }

    if (state.isPermissionSettingsDialogVisible) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.PermissionSettingsDialogDismissed) },
            title = { Text(stringResource(R.string.settings_notification_permission_title)) },
            text = { Text(stringResource(R.string.settings_notification_permission_text)) },
            confirmButton = {
                TextButton(onClick = { onIntent(SettingsIntent.OpenNotificationSettingsClicked) }) {
                    Text(stringResource(R.string.settings_notification_open_settings))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onIntent(SettingsIntent.PermissionSettingsDialogDismissed) }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
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

/** 설정 묶음 제목 */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            // 스크린리더가 묶음 단위로 건너뛸 수 있게 제목으로 표시한다
            .semantics { heading() }
    )
}

/** 기록 리마인더·알림 시간·예산 알림. 행 전체를 눌러도 스위치가 바뀐다 */
@Composable
private fun NotificationSection(
    settings: NotificationSettings,
    isBlocked: Boolean,
    onIntent: (SettingsIntent) -> Unit,
    onTimeClick: () -> Unit
) {
    SectionHeader(stringResource(R.string.settings_section_notification))
    // 켜 두었는데 시스템에서 막혀 있으면 눌러서 설정 앱으로 갈 수 있게 알린다
    if (isBlocked) {
        Text(
            text = stringResource(R.string.settings_notification_blocked),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) {
                    onIntent(SettingsIntent.OpenNotificationSettingsClicked)
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
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
@Preview(name = "설정 · 글꼴 2배", fontScale = 2f)
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
                ),
                appVersion = "1.0"
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
