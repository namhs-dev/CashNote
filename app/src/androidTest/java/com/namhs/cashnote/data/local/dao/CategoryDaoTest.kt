package com.namhs.cashnote.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.namhs.cashnote.data.local.database.CashNoteDatabase
import com.namhs.cashnote.data.local.database.DefaultCategories
import com.namhs.cashnote.data.local.database.DefaultCategoryCallback
import com.namhs.cashnote.data.local.entity.TransactionEntity
import com.namhs.cashnote.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {
    private lateinit var database: CashNoteDatabase
    private lateinit var dao: CategoryDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, CashNoteDatabase::class.java)
            .addCallback(DefaultCategoryCallback())
            .build()
        dao = database.categoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // 기본 카테고리 (DefaultCategoryCallback)

    @Test
    fun onCreate_insertsDefaultCategories() = runBlocking {
        // 콜백이 SQL로 넣은 값이 Entity로 그대로 읽히는지(type·color 저장 형식 포함) 확인
        assertEquals(DefaultCategories.all, dao.observeAll().first())
    }

    @Test
    fun observeByType_returnsOnlyMatchingType() = runBlocking {
        val expense = dao.observeByType(TransactionType.EXPENSE).first()
        val income = dao.observeByType(TransactionType.INCOME).first()

        assertEquals(7, expense.size)
        assertTrue(expense.all { it.type == TransactionType.EXPENSE })
        assertEquals(3, income.size)
        assertTrue(income.all { it.type == TransactionType.INCOME })
    }

    // 외래 키 RESTRICT

    @Test(expected = SQLiteConstraintException::class)
    fun deleteCategory_referencedByTransaction_fails() {
        runBlocking {
            database.transactionDao().insert(transactionWithCategory(1))
        }

        // DAO에 카테고리 삭제 메서드가 없어 SQL로 직접 시도한다.
        deleteCategory(1)
    }

    @Test
    fun deleteCategory_notReferenced_succeeds() = runBlocking {
        database.transactionDao().insert(transactionWithCategory(1))

        deleteCategory(10)

        val ids = dao.observeAll().first().map { it.id }
        assertEquals((1L..9L).toList(), ids)
    }

    private fun deleteCategory(id: Long) {
        database.openHelper.writableDatabase.execSQL("DELETE FROM categories WHERE id = $id")
    }

    private fun transactionWithCategory(categoryId: Long) = TransactionEntity(
        amount = 12_500,
        type = TransactionType.EXPENSE,
        categoryId = categoryId,
        memo = "점심",
        date = LocalDate.of(2026, 11, 9),
        createdAt = Instant.parse("2026-11-09T03:00:00Z")
    )
}
