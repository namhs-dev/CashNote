package com.namhs.cashnote.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = GreenOnPrimaryLight,
    primaryContainer = GreenPrimaryContainerLight,
    onPrimaryContainer = GreenOnPrimaryContainerLight,
    secondary = SecondaryLight,
    tertiary = TertiaryLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = BackgroundLight,
    onSurface = OnBackgroundLight
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = GreenOnPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    onPrimaryContainer = GreenOnPrimaryContainerDark,
    secondary = SecondaryDark,
    tertiary = TertiaryDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = BackgroundDark,
    onSurface = OnBackgroundDark
)

/** Material3 ColorScheme에 없는 CashNote 전용 색 */
@Immutable
data class CashNoteColors(val income: Color, val expense: Color)

private val LightCashNoteColors = CashNoteColors(income = IncomeLight, expense = ExpenseLight)
private val DarkCashNoteColors = CashNoteColors(income = IncomeDark, expense = ExpenseDark)

private val LocalCashNoteColors = staticCompositionLocalOf { LightCashNoteColors }

@Composable
fun CashNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 브랜드 색을 일관되게 보여주기 위해 기본값은 끈다 (Android 12+에서만 동작)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme

        else -> LightColorScheme
    }
    val cashNoteColors = if (darkTheme) DarkCashNoteColors else LightCashNoteColors

    CompositionLocalProvider(LocalCashNoteColors provides cashNoteColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/** 사용 예: `CashNoteTheme.colors.income` */
object CashNoteTheme {
    val colors: CashNoteColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCashNoteColors.current
}
