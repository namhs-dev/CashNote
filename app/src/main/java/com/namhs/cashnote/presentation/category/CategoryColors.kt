package com.namhs.cashnote.presentation.category

import androidx.annotation.StringRes
import com.namhs.cashnote.R

/**
 * 카테고리 색상으로 고를 수 있는 팔레트. 0xAARRGGBB와 스크린리더가 읽을 색 이름의 짝.
 *
 * 기본 카테고리(DefaultCategories)에 쓴 색을 포함해 라이트·다크 테마 모두에서 구분되는 중간 톤으로 고른다.
 */
private val paletteEntries: List<Pair<Long, Int>> = listOf(
    0xFFFF8A65 to R.string.color_orange,
    0xFFE57373 to R.string.color_red,
    0xFFF06292 to R.string.color_pink,
    0xFFBA68C8 to R.string.color_purple,
    0xFF9575CD to R.string.color_light_purple,
    0xFF7986CB to R.string.color_indigo,
    0xFF4FC3F7 to R.string.color_sky_blue,
    0xFF4DB6AC to R.string.color_teal,
    0xFF81C784 to R.string.color_green,
    0xFFAED581 to R.string.color_light_green,
    0xFFFFD54F to R.string.color_yellow,
    0xFFFFB74D to R.string.color_tangerine,
    0xFFA1887F to R.string.color_brown,
    0xFF90A4AE to R.string.color_gray
)

val categoryColorPalette: List<Long> = paletteEntries.map { it.first }

/** 팔레트 색의 이름. 팔레트에 없는 색이면 null */
@StringRes
fun categoryColorNameRes(color: Long): Int? =
    paletteEntries.firstOrNull { it.first == color }?.second
