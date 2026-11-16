package com.namhs.cashnote.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 글자색이 그 위에 놓일 수 있는 모든 표면에서 WCAG AA 일반 글자 기준(4.5:1)을 넘는지 검사한다.
 *
 * 색을 바꿨을 때 대비가 떨어지면 여기서 잡힌다.
 */
class ColorContrastTest {
    @Test
    fun lightTheme_textColorsMeetAa() {
        assertTextColorsReadable("라이트", LightColorScheme, LightCashNoteColors)
    }

    @Test
    fun darkTheme_textColorsMeetAa() {
        assertTextColorsReadable("다크", DarkColorScheme, DarkCashNoteColors)
    }

    @Test
    fun contrastRatio_matchesWcagReference() {
        // WCAG 정의상 검정·흰색은 21:1, 같은 색은 1:1이다
        assertNear(21.0, contrastRatio(Color.Black, Color.White))
        assertNear(1.0, contrastRatio(Color.White, Color.White))
    }

    private fun assertTextColorsReadable(
        theme: String,
        scheme: ColorScheme,
        colors: CashNoteColors
    ) {
        val textColors = mapOf(
            "income" to colors.income,
            "expense" to colors.expense,
            "warning" to colors.warning,
            "onSurface" to scheme.onSurface,
            "onSurfaceVariant" to scheme.onSurfaceVariant,
            "primary" to scheme.primary,
            "error" to scheme.error
        )
        // 목록·화면 배경과 카드·하단 바·다이얼로그 같은 표면 컨테이너
        val surfaces = mapOf(
            "background" to scheme.background,
            "surface" to scheme.surface,
            "surfaceContainerLowest" to scheme.surfaceContainerLowest,
            "surfaceContainerLow" to scheme.surfaceContainerLow,
            "surfaceContainer" to scheme.surfaceContainer,
            "surfaceContainerHigh" to scheme.surfaceContainerHigh,
            "surfaceContainerHighest" to scheme.surfaceContainerHighest
        )
        val failures = textColors.flatMap { (textName, text) ->
            surfaces.mapNotNull { (surfaceName, surface) ->
                val ratio = contrastRatio(text, surface)
                if (ratio < MIN_TEXT_CONTRAST) {
                    "$textName on $surfaceName = ${"%.2f".format(ratio)}"
                } else {
                    null
                }
            }
        }
        assertTrue("$theme 테마 대비 부족: $failures", failures.isEmpty())
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val lighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun assertNear(expected: Double, actual: Double) {
        assertTrue("expected $expected but was $actual", kotlin.math.abs(expected - actual) < 0.01)
    }

    private companion object {
        /** WCAG 2.1 AA, 일반 크기 글자 */
        const val MIN_TEXT_CONTRAST = 4.5
    }
}
