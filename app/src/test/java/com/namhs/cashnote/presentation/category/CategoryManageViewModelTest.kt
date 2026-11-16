package com.namhs.cashnote.presentation.category

import app.cash.turbine.test
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.usecase.GetCategoriesUseCase
import com.namhs.cashnote.testing.FakeCategoryRepository
import com.namhs.cashnote.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CategoryManageViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val food = Category(1, "식비", TransactionType.EXPENSE, "food", 0xFFE57373)
    private val transport = Category(2, "교통", TransactionType.EXPENSE, "transport", 0xFF64B5F6)
    private val salary = Category(3, "급여", TransactionType.INCOME, "salary", 0xFF81C784)

    private val categoryRepository = FakeCategoryRepository(listOf(food, transport, salary))

    private fun viewModel() = CategoryManageViewModel(GetCategoriesUseCase(categoryRepository))

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
