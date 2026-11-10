package com.namhs.cashnote.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.usecase.AddTransactionUseCase
import com.namhs.cashnote.domain.usecase.GetCategoriesUseCase
import com.namhs.cashnote.domain.usecase.GetTransactionUseCase
import com.namhs.cashnote.domain.usecase.UpdateTransactionUseCase
import com.namhs.cashnote.testing.FakeCategoryRepository
import com.namhs.cashnote.testing.FakeTransactionRepository
import com.namhs.cashnote.testing.MainDispatcherRule
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TransactionEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val food = Category(1, "식비", TransactionType.EXPENSE, "food", 0xFFE57373)
    private val transport = Category(2, "교통", TransactionType.EXPENSE, "transport", 0xFF64B5F6)
    private val salary = Category(3, "급여", TransactionType.INCOME, "salary", 0xFF81C784)

    private val existing = Transaction(
        id = 7,
        amount = 12_500,
        type = TransactionType.EXPENSE,
        categoryId = food.id,
        memo = "점심",
        date = LocalDate.of(2026, 11, 9),
        createdAt = Instant.parse("2026-11-09T03:15:30Z")
    )

    private val transactionRepository = FakeTransactionRepository(listOf(existing))
    private val categoryRepository = FakeCategoryRepository(listOf(food, transport, salary))

    // 초기 상태

    @Test
    fun init_newTransaction_startsAsExpenseWithExpenseCategories() {
        val state = viewModel().uiState.value

        assertFalse(state.isEditMode)
        assertEquals(TransactionType.EXPENSE, state.type)
        assertEquals(listOf(food, transport), state.categories)
    }

    @Test
    fun init_editMode_fillsFormWithTransaction() {
        val state = viewModel(transactionId = existing.id).uiState.value

        assertTrue(state.isEditMode)
        assertFalse(state.isLoading)
        assertEquals("12500", state.amountInput)
        assertEquals(food.id, state.selectedCategoryId)
        assertEquals(existing.date, state.date)
        assertEquals("점심", state.memo)
    }

    @Test
    fun init_missingTransaction_showsMessageAndNavigatesBack() = runTest {
        viewModel(transactionId = 99).sideEffect.test {
            assertEquals(
                TransactionEditSideEffect.ShowMessage(R.string.transaction_not_found),
                awaitItem()
            )
            assertEquals(TransactionEditSideEffect.NavigateBack, awaitItem())
        }
    }

    // 입력

    @Test
    fun amountChanged_keepsDigitsOnly() {
        val viewModel = viewModel()

        viewModel.onIntent(TransactionEditIntent.AmountChanged("1,2a3"))

        assertEquals("123", viewModel.uiState.value.amountInput)
    }

    @Test
    fun amountChanged_limitsTo12Digits() {
        val viewModel = viewModel()

        viewModel.onIntent(TransactionEditIntent.AmountChanged("1234567890123"))

        assertEquals("123456789012", viewModel.uiState.value.amountInput)
    }

    @Test
    fun typeChanged_resetsCategoryAndLoadsCategoriesOfNewType() {
        val viewModel = viewModel()
        viewModel.onIntent(TransactionEditIntent.CategorySelected(food.id))

        viewModel.onIntent(TransactionEditIntent.TypeChanged(TransactionType.INCOME))

        val state = viewModel.uiState.value
        assertEquals(TransactionType.INCOME, state.type)
        assertNull(state.selectedCategoryId)
        assertEquals(listOf(salary), state.categories)
    }

    // 입력 검증

    @Test
    fun saveClicked_emptyAmount_showsAmountErrorAndDoesNotSave() {
        val viewModel = viewModel()
        viewModel.onIntent(TransactionEditIntent.CategorySelected(food.id))

        viewModel.onIntent(TransactionEditIntent.SaveClicked)

        assertTrue(viewModel.uiState.value.isAmountError)
        assertFalse(viewModel.uiState.value.isCategoryError)
        assertTrue(transactionRepository.added.isEmpty())
    }

    @Test
    fun saveClicked_zeroAmount_showsAmountError() {
        val viewModel = viewModel()
        viewModel.onIntent(TransactionEditIntent.AmountChanged("0"))
        viewModel.onIntent(TransactionEditIntent.CategorySelected(food.id))

        viewModel.onIntent(TransactionEditIntent.SaveClicked)

        assertTrue(viewModel.uiState.value.isAmountError)
        assertTrue(transactionRepository.added.isEmpty())
    }

    @Test
    fun saveClicked_noCategory_showsCategoryErrorAndDoesNotSave() {
        val viewModel = viewModel()
        viewModel.onIntent(TransactionEditIntent.AmountChanged("5000"))

        viewModel.onIntent(TransactionEditIntent.SaveClicked)

        assertFalse(viewModel.uiState.value.isAmountError)
        assertTrue(viewModel.uiState.value.isCategoryError)
        assertTrue(transactionRepository.added.isEmpty())
    }

    @Test
    fun inputChanged_clearsValidationErrors() {
        val viewModel = viewModel()
        viewModel.onIntent(TransactionEditIntent.SaveClicked)

        viewModel.onIntent(TransactionEditIntent.AmountChanged("5000"))
        viewModel.onIntent(TransactionEditIntent.CategorySelected(food.id))

        assertFalse(viewModel.uiState.value.isAmountError)
        assertFalse(viewModel.uiState.value.isCategoryError)
    }

    // 저장

    @Test
    fun saveClicked_newTransaction_addsAndNavigatesBack() = runTest {
        val viewModel = viewModel()
        val date = LocalDate.of(2026, 11, 3)

        viewModel.sideEffect.test {
            viewModel.onIntent(TransactionEditIntent.AmountChanged("5000"))
            viewModel.onIntent(TransactionEditIntent.CategorySelected(transport.id))
            viewModel.onIntent(TransactionEditIntent.DateSelected(date))
            viewModel.onIntent(TransactionEditIntent.MemoChanged("버스"))
            viewModel.onIntent(TransactionEditIntent.SaveClicked)

            assertEquals(TransactionEditSideEffect.NavigateBack, awaitItem())
        }
        val added = transactionRepository.added.single()
        assertEquals(0L, added.id)
        assertEquals(5_000L, added.amount)
        assertEquals(TransactionType.EXPENSE, added.type)
        assertEquals(transport.id, added.categoryId)
        assertEquals(date, added.date)
        assertEquals("버스", added.memo)
    }

    @Test
    fun saveClicked_editMode_updatesKeepingIdAndCreatedAt() = runTest {
        val viewModel = viewModel(transactionId = existing.id)

        viewModel.sideEffect.test {
            viewModel.onIntent(TransactionEditIntent.AmountChanged("15000"))
            viewModel.onIntent(TransactionEditIntent.SaveClicked)

            assertEquals(TransactionEditSideEffect.NavigateBack, awaitItem())
        }
        assertTrue(transactionRepository.added.isEmpty())
        assertEquals(existing.copy(amount = 15_000), transactionRepository.updated.single())
    }

    @Test
    fun saveClicked_failure_showsMessageAndStopsSaving() = runTest {
        val viewModel = viewModel()
        transactionRepository.failure = IllegalStateException("디스크 오류")

        viewModel.sideEffect.test {
            viewModel.onIntent(TransactionEditIntent.AmountChanged("5000"))
            viewModel.onIntent(TransactionEditIntent.CategorySelected(food.id))
            viewModel.onIntent(TransactionEditIntent.SaveClicked)

            assertEquals(
                TransactionEditSideEffect.ShowMessage(R.string.transaction_save_failed),
                awaitItem()
            )
        }
        assertFalse(viewModel.uiState.value.isSaving)
    }

    private fun viewModel(transactionId: Long = 0) = TransactionEditViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(TransactionEditViewModel.ARG_TRANSACTION_ID to transactionId)
        ),
        getTransaction = GetTransactionUseCase(transactionRepository),
        getCategories = GetCategoriesUseCase(categoryRepository),
        addTransaction = AddTransactionUseCase(transactionRepository),
        updateTransaction = UpdateTransactionUseCase(transactionRepository)
    )
}
