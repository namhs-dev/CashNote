package com.namhs.cashnote.presentation.statistics

import androidx.lifecycle.SavedStateHandle
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.CategoryExpense
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.usecase.GetCategoryExpensesUseCase
import com.namhs.cashnote.testing.FakeCategoryRepository
import com.namhs.cashnote.testing.FakeTransactionRepository
import com.namhs.cashnote.testing.MainDispatcherRule
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StatisticsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val food = Category(1, "식비", TransactionType.EXPENSE, "food", 0xFFE57373)
    private val transport = Category(2, "교통", TransactionType.EXPENSE, "transport", 0xFF64B5F6)

    // 실행 날짜와 관계없이 이번 달·다음 달 기준으로 만든다
    private val thisMonth = YearMonth.now()
    private val nextMonth = thisMonth.plusMonths(1)

    private val transactionRepository = FakeTransactionRepository(
        listOf(
            transaction(id = 1, category = food, amount = 30_000, date = thisMonth.atDay(1)),
            transaction(id = 2, category = transport, amount = 10_000, date = thisMonth.atDay(2)),
            transaction(id = 3, category = transport, amount = 5_000, date = nextMonth.atDay(1))
        )
    )
    private val categoryRepository = FakeCategoryRepository(listOf(food, transport))

    @Test
    fun init_loadsExpensesAndTotalOfThisMonth() {
        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals(thisMonth, state.month)
        assertEquals(
            listOf(CategoryExpense(food, 30_000, 0.75f), CategoryExpense(transport, 10_000, 0.25f)),
            state.expenses
        )
        assertEquals(40_000L, state.totalExpense)
    }

    @Test
    fun nextMonthClicked_showsExpensesOfNextMonth() {
        val viewModel = viewModel()

        viewModel.onIntent(StatisticsIntent.NextMonthClicked)

        val state = viewModel.uiState.value
        assertEquals(nextMonth, state.month)
        assertEquals(listOf(CategoryExpense(transport, 5_000, 1f)), state.expenses)
    }

    @Test
    fun previousMonthClicked_movesToPreviousMonth() {
        val viewModel = viewModel()

        viewModel.onIntent(StatisticsIntent.PreviousMonthClicked)

        assertEquals(thisMonth.minusMonths(1), viewModel.uiState.value.month)
    }

    @Test
    fun monthChanged_toMonthWithoutExpenses_isEmpty() {
        val viewModel = viewModel()

        viewModel.onIntent(StatisticsIntent.PreviousMonthClicked)

        // 이번 달 지출이 남아 있지 않아야 한다
        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals(0L, viewModel.uiState.value.totalExpense)
    }

    @Test
    fun monthChanged_savesMonthToSavedStateHandle() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = viewModel(savedStateHandle)

        viewModel.onIntent(StatisticsIntent.NextMonthClicked)

        assertEquals(
            nextMonth.toString(),
            savedStateHandle.get<String>(StatisticsViewModel.KEY_MONTH)
        )
    }

    @Test
    fun init_withSavedMonth_restoresMonth() {
        val viewModel = viewModel(
            SavedStateHandle(mapOf(StatisticsViewModel.KEY_MONTH to nextMonth.toString()))
        )

        val state = viewModel.uiState.value
        assertEquals(nextMonth, state.month)
        assertEquals(5_000L, state.totalExpense)
    }

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        StatisticsViewModel(
            savedStateHandle = savedStateHandle,
            getCategoryExpenses = GetCategoryExpensesUseCase(
                transactionRepository,
                categoryRepository
            )
        )

    private fun transaction(id: Long, category: Category, amount: Long, date: LocalDate) =
        Transaction(
            id = id,
            amount = amount,
            type = category.type,
            categoryId = category.id,
            date = date,
            createdAt = Instant.parse("2026-01-01T00:00:00Z")
        )
}
