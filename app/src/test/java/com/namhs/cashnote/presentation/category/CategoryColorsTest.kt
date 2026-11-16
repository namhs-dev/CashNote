package com.namhs.cashnote.presentation.category

import com.namhs.cashnote.data.local.database.DefaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryColorsTest {
    @Test
    fun everyPaletteColor_hasDistinctName() {
        val names = categoryColorPalette.map { categoryColorNameRes(it) }

        names.forEach { assertNotNull(it) }
        // 스크린리더가 같은 이름을 두 번 읽으면 구분할 수 없다
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun defaultCategoryColors_areInPalette() {
        // 기본 카테고리를 수정할 때도 색 이름이 읽혀야 한다
        DefaultCategories.all.forEach { category ->
            assertNotNull(category.name, categoryColorNameRes(category.color))
        }
    }

    @Test
    fun colorOutsidePalette_hasNoName() {
        assertNull(categoryColorNameRes(0xFF000000))
    }
}
