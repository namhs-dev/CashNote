package com.namhs.cashnote.presentation.category

import app.cash.turbine.test
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.usecase.AddCategoryUseCase
import com.namhs.cashnote.domain.usecase.DeleteCategoryUseCase
import com.namhs.cashnote.domain.usecase.GetCategoriesUseCase
import com.namhs.cashnote.domain.usecase.UpdateCategoryUseCase
import com.namhs.cashnote.testing.FakeCategoryRepository
import com.namhs.cashnote.testing.FakeTransactionRepository
import com.namhs.cashnote.testing.MainDispatcherRule
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CategoryManageViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val food = Category(1, "식비", TransactionType.EXPENSE, "food", 0xFFE57373)
    private val transport = Category(2, "교통", TransactionType.EXPENSE, "transport", 0xFF64B5F6)
    private val salary = Category(3, "급여", TransactionType.INCOME, "salary", 0xFF81C784)

    private val categoryRepository = FakeCategoryRepository(listOf(food, transport, salary))

    // 식비는 거래 2건이 쓰고 있고, 교통·급여는 쓰는 거래가 없다
    private val transactionRepository = FakeTransactionRepository(
        listOf(transaction(1, food.id), transaction(2, food.id))
    )

    private fun transaction(id: Long, categoryId: Long) = Transaction(
        id = id,
        amount = 10_000,
        type = TransactionType.EXPENSE,
        categoryId = categoryId,
        memo = "",
        date = LocalDate.of(2026, 11, 16),
        createdAt = Instant.parse("2026-11-16T03:00:00Z")
    )

    private fun viewModel() = CategoryManageViewModel(
        getCategories = GetCategoriesUseCase(categoryRepository),
        addCategory = AddCategoryUseCase(categoryRepository),
        updateCategory = UpdateCategoryUseCase(categoryRepository),
        deleteCategory = DeleteCategoryUseCase(categoryRepository, transactionRepository)
    )

    // 삭제

    @Test
    fun deleteClicked_asksConfirmation() {
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.CategoryClicked(transport))

        viewModel.onIntent(CategoryManageIntent.EditorDeleteClicked)

        assertTrue(viewModel.uiState.value.editor!!.isConfirmingDelete)
        assertEquals(3, categoryRepository.categories.value.size)
    }

    @Test
    fun deleteClicked_newCategory_isIgnored() {
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.AddClicked)

        viewModel.onIntent(CategoryManageIntent.EditorDeleteClicked)

        assertFalse(viewModel.uiState.value.editor!!.isConfirmingDelete)
    }

    @Test
    fun deleteCancelled_keepsEditorAndCategory() {
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.CategoryClicked(transport))
        viewModel.onIntent(CategoryManageIntent.EditorDeleteClicked)

        viewModel.onIntent(CategoryManageIntent.DeleteCancelled)

        val editor = viewModel.uiState.value.editor
        assertFalse(editor!!.isConfirmingDelete)
        assertEquals(transport.id, editor.categoryId)
        assertEquals(3, categoryRepository.categories.value.size)
    }

    @Test
    fun deleteConfirmed_unusedCategory_deletesAndClosesEditor() {
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.CategoryClicked(transport))
        viewModel.onIntent(CategoryManageIntent.EditorDeleteClicked)

        viewModel.onIntent(CategoryManageIntent.DeleteConfirmed)

        assertNull(viewModel.uiState.value.editor)
        assertEquals(listOf(food), viewModel.uiState.value.categories)
    }

    @Test
    fun deleteConfirmed_usedCategory_showsCountAndKeepsCategory() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.CategoryClicked(food))
        viewModel.onIntent(CategoryManageIntent.EditorDeleteClicked)

        viewModel.sideEffect.test {
            viewModel.onIntent(CategoryManageIntent.DeleteConfirmed)

            assertEquals(CategoryManageSideEffect.ShowCategoryInUse(2), awaitItem())
        }
        // 수정 다이얼로그는 남겨 두어 이름·색상 수정으로 이어갈 수 있다
        val editor = viewModel.uiState.value.editor
        assertEquals(food.id, editor?.categoryId)
        assertFalse(editor!!.isConfirmingDelete)
        assertFalse(editor.isSaving)
        assertEquals(listOf(food, transport), viewModel.uiState.value.categories)
    }

    @Test
    fun deleteConfirmed_failure_showsMessage() = runTest {
        categoryRepository.failure = IOException("disk")
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.CategoryClicked(transport))
        viewModel.onIntent(CategoryManageIntent.EditorDeleteClicked)

        viewModel.sideEffect.test {
            viewModel.onIntent(CategoryManageIntent.DeleteConfirmed)

            assertEquals(
                CategoryManageSideEffect.ShowMessage(R.string.category_delete_failed),
                awaitItem()
            )
        }
        assertFalse(viewModel.uiState.value.editor!!.isSaving)
    }

    // 추가

    @Test
    fun addClicked_opensEmptyEditorWithFirstPaletteColor() {
        val viewModel = viewModel()

        viewModel.onIntent(CategoryManageIntent.AddClicked)

        val editor = viewModel.uiState.value.editor
        assertTrue(editor!!.isNew)
        assertEquals("", editor.name)
        assertEquals(categoryColorPalette.first(), editor.color)
    }

    @Test
    fun save_newCategory_addsToCurrentTypeAndClosesEditor() {
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.TypeChanged(TransactionType.INCOME))
        viewModel.onIntent(CategoryManageIntent.AddClicked)
        viewModel.onIntent(CategoryManageIntent.EditorNameChanged("  부수입  "))
        viewModel.onIntent(CategoryManageIntent.EditorColorSelected(0xFF4DB6AC))

        viewModel.onIntent(CategoryManageIntent.EditorSaveClicked)

        val added = categoryRepository.categories.value.last()
        // 앞뒤 공백은 지우고, 유형은 보고 있던 탭을 따른다
        assertEquals("부수입", added.name)
        assertEquals(TransactionType.INCOME, added.type)
        assertEquals(0xFF4DB6AC, added.color)
        assertEquals(CategoryManageViewModel.ICON_DEFAULT, added.icon)
        assertNull(viewModel.uiState.value.editor)
        assertEquals(listOf(salary, added), viewModel.uiState.value.categories)
    }

    @Test
    fun save_emptyName_showsErrorAndKeepsEditor() {
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.AddClicked)
        viewModel.onIntent(CategoryManageIntent.EditorNameChanged("   "))

        viewModel.onIntent(CategoryManageIntent.EditorSaveClicked)

        assertEquals(CategoryNameError.EMPTY, viewModel.uiState.value.editor?.nameError)
        assertEquals(3, categoryRepository.categories.value.size)
    }

    @Test
    fun save_duplicateNameInSameType_showsError() {
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.AddClicked)
        viewModel.onIntent(CategoryManageIntent.EditorNameChanged("식비"))

        viewModel.onIntent(CategoryManageIntent.EditorSaveClicked)

        assertEquals(CategoryNameError.DUPLICATE, viewModel.uiState.value.editor?.nameError)
    }

    @Test
    fun save_sameNameInOtherType_isAllowed() {
        // 수입 탭에서 지출 카테고리와 같은 이름은 허용한다
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.TypeChanged(TransactionType.INCOME))
        viewModel.onIntent(CategoryManageIntent.AddClicked)
        viewModel.onIntent(CategoryManageIntent.EditorNameChanged("식비"))

        viewModel.onIntent(CategoryManageIntent.EditorSaveClicked)

        assertNull(viewModel.uiState.value.editor)
        assertEquals(4, categoryRepository.categories.value.size)
    }

    @Test
    fun nameChanged_clearsError() {
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.AddClicked)
        viewModel.onIntent(CategoryManageIntent.EditorSaveClicked)

        viewModel.onIntent(CategoryManageIntent.EditorNameChanged("여행"))

        assertNull(viewModel.uiState.value.editor?.nameError)
    }

    // 수정

    @Test
    fun categoryClicked_opensEditorWithCategory() {
        val viewModel = viewModel()

        viewModel.onIntent(CategoryManageIntent.CategoryClicked(food))

        val editor = viewModel.uiState.value.editor
        assertEquals(food.id, editor?.categoryId)
        assertEquals("식비", editor?.name)
        assertEquals(food.color, editor?.color)
    }

    @Test
    fun save_existingCategory_updatesNameAndColorOnly() {
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.CategoryClicked(food))
        viewModel.onIntent(CategoryManageIntent.EditorNameChanged("외식"))
        viewModel.onIntent(CategoryManageIntent.EditorColorSelected(0xFFF06292))

        viewModel.onIntent(CategoryManageIntent.EditorSaveClicked)

        val updated = categoryRepository.categories.value.first { it.id == food.id }
        assertEquals(food.copy(name = "외식", color = 0xFFF06292), updated)
        assertNull(viewModel.uiState.value.editor)
    }

    @Test
    fun save_existingCategoryKeepingOwnName_isAllowed() {
        // 자기 이름은 중복으로 보지 않는다
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.CategoryClicked(food))
        viewModel.onIntent(CategoryManageIntent.EditorColorSelected(0xFFF06292))

        viewModel.onIntent(CategoryManageIntent.EditorSaveClicked)

        assertNull(viewModel.uiState.value.editor)
        assertEquals(0xFFF06292, categoryRepository.categories.value.first().color)
    }

    // 닫기·실패

    @Test
    fun editorDismissed_closesWithoutSaving() {
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.AddClicked)
        viewModel.onIntent(CategoryManageIntent.EditorNameChanged("여행"))

        viewModel.onIntent(CategoryManageIntent.EditorDismissed)

        assertNull(viewModel.uiState.value.editor)
        assertEquals(3, categoryRepository.categories.value.size)
    }

    @Test
    fun save_failure_showsMessageAndKeepsEditor() = runTest {
        categoryRepository.failure = IOException("disk")
        val viewModel = viewModel()
        viewModel.onIntent(CategoryManageIntent.AddClicked)
        viewModel.onIntent(CategoryManageIntent.EditorNameChanged("여행"))

        viewModel.sideEffect.test {
            viewModel.onIntent(CategoryManageIntent.EditorSaveClicked)

            assertEquals(
                CategoryManageSideEffect.ShowMessage(R.string.category_save_failed),
                awaitItem()
            )
        }
        val editor = viewModel.uiState.value.editor
        assertEquals("여행", editor?.name)
        assertFalse(editor!!.isSaving)
    }

    @Test
    fun init_showsExpenseCategories() {
        val state = viewModel().uiState.value

        assertEquals(TransactionType.EXPENSE, state.type)
        assertEquals(listOf(food, transport), state.categories)
    }

    @Test
    fun typeChanged_showsIncomeCategories() {
        val viewModel = viewModel()

        viewModel.onIntent(CategoryManageIntent.TypeChanged(TransactionType.INCOME))

        val state = viewModel.uiState.value
        assertEquals(TransactionType.INCOME, state.type)
        assertEquals(listOf(salary), state.categories)
    }

    @Test
    fun backClicked_navigatesBack() = runTest {
        val viewModel = viewModel()

        viewModel.sideEffect.test {
            viewModel.onIntent(CategoryManageIntent.BackClicked)

            assertEquals(CategoryManageSideEffect.NavigateBack, awaitItem())
        }
    }
}
