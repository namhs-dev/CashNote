package com.namhs.cashnote.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.BudgetLevel
import com.namhs.cashnote.domain.model.BudgetStatus
import com.namhs.cashnote.presentation.component.formatAmount
import com.namhs.cashnote.presentation.theme.CashNoteTheme
import java.time.YearMonth
import kotlin.math.roundToInt

/**
 * 요약 카드 안의 예산 영역. 예산이 없으면 설정 버튼만 보인다.
 * 예산이 있으면 사용률 막대와 경고 문구를 보여 주고, 누르면 예산을 바꿀 수 있다.
 */
@Composable
internal fun BudgetSection(
    budget: BudgetStatus?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (budget == null) {
        TextButton(onClick = onClick, modifier = modifier) {
            Text(stringResource(R.string.home_budget_set))
        }
        return
    }

    val color = budget.level.color()
    val formatted = remember(budget.budget) { formatAmount(budget.budget) }
    val percent = (budget.usageRatio * 100).roundToInt()
    // clickable이 자식 의미를 하나로 묶어 "예산 500,000원, 70% 사용, …" 버튼으로 읽힌다
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.home_budget_edit),
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.home_budget_amount, formatted),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.home_budget_usage, percent),
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
        // 100%를 넘으면 막대는 가득 찬 채로 두고 초과는 문구로 알린다
        LinearProgressIndicator(
            progress = { budget.usageRatio.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = color
        )
        val messageRes = when (budget.level) {
            BudgetLevel.NORMAL -> null
            BudgetLevel.WARNING -> R.string.home_budget_warning
            BudgetLevel.EXCEEDED -> R.string.home_budget_exceeded
        }
        if (messageRes != null) {
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
private fun BudgetLevel.color(): Color = when (this) {
    BudgetLevel.NORMAL -> MaterialTheme.colorScheme.primary
    BudgetLevel.WARNING -> CashNoteTheme.colors.warning
    BudgetLevel.EXCEEDED -> MaterialTheme.colorScheme.error
}

/**
 * 월 예산 입력 다이얼로그. 입력 중인 글자는 다이얼로그 안에서만 들고 있다가 저장할 때 넘긴다.
 * 빈 값으로 저장하면 [month]부터 예산을 없앤다.
 */
@Composable
internal fun BudgetDialog(
    month: YearMonth,
    currentBudget: Long?,
    onSave: (input: String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf(currentBudget?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_budget_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        R.string.home_budget_dialog_applies_from,
                        month.year,
                        month.monthValue
                    )
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { value -> input = value.filter { it.isDigit() }.take(12) },
                    label = { Text(stringResource(R.string.home_budget_dialog_title)) },
                    suffix = { Text(stringResource(R.string.transaction_amount_suffix)) },
                    supportingText = { Text(stringResource(R.string.home_budget_dialog_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(input) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
