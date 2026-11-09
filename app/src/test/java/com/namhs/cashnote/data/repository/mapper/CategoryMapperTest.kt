package com.namhs.cashnote.data.repository.mapper

import com.namhs.cashnote.data.local.entity.CategoryEntity
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryMapperTest {
    @Test
    fun toDomain_mapsAllFields() {
        val entity = CategoryEntity(
            id = 8,
            name = "급여",
            type = TransactionType.INCOME,
            icon = "salary",
            color = 0xFF81C784
        )

        val expected = Category(
            id = 8,
            name = "급여",
            type = TransactionType.INCOME,
            icon = "salary",
            color = 0xFF81C784
        )

        assertEquals(expected, entity.toDomain())
    }
}
