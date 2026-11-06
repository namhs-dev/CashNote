package com.namhs.cashnote.presentation.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// 라이트/다크 팔레트와 금액 표시를 한눈에 확인하기 위한 Preview
@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThemePreview() {
    CashNoteTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val scheme = MaterialTheme.colorScheme
                ColorSwatch("primary", scheme.primary, scheme.onPrimary)
                ColorSwatch("primaryContainer", scheme.primaryContainer, scheme.onPrimaryContainer)
                ColorSwatch("secondary", scheme.secondary, scheme.onSecondary)
                ColorSwatch("tertiary", scheme.tertiary, scheme.onTertiary)
                Text("1,250,000원", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "+3,000,000",
                    color = CashNoteTheme.colors.income,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "-38,900",
                    color = CashNoteTheme.colors.expense,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(name: String, color: Color, contentColor: Color) {
    Surface(
        color = color,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}
