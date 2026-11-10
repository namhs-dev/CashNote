package com.namhs.cashnote.presentation.home

import app.cash.turbine.test
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.usecase.AddTransactionUseCase
import com.namhs.cashnote.domain.usecase.DeleteTransactionUseCase
import com.namhs.cashnote.domain.usecase.GetMonthlyTransactionsUseCase
import com.namhs.cashnote.testing.FakeCategoryRepository
import com.namhs.cashnote.testing.FakeTransactionRepository
import com.namhs.cashnote.testing.MainDispatcherRule
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val food = Category(1, "식비", TransactionType.EXPENSE, "food", 0xFFE57373)

    // HomeViewModel은 이번 달을 구독하므로 날짜도 이번 달 기준으로 만든다.
    // 1일과 2일은 어느 달에나 있어 실행 날짜와 관계없이 서로 다른 날짜가 된다
    private val thisMonth = YearMonth.now()
    private val day1 = thisMonth.atDay(1)
    private val day2 = thisMonth.atDay(2)

    private val breakfast = transaction(id = 1, date = day1)
    private val lunch = transaction(id = 2, date = day2)
    private val dinner = transaction(id = 3, date = day2)

    private val transactionRepository = FakeTransactionRepository(listOf(breakfast, lunch, dinner))
    private val categoryRepository = FakeCategoryRepository(listOf(food))

    // 목록

    @Test
    fun init_groupsTransactionsByDateNewestFirst() {
        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals(listOf(day2, day1), state.dailyGroups.map { it.date })
        assertEquals(
            listOf(listOf(dinner, lunch), listOf(breakfast)),
            state.dailyGroups.map { group -> group.items.map { it.transaction } }
        )
    }

    @Test
    fun init_noTransactions_isEmpty() {
        val viewModel = viewModel(FakeTransactionRepository())

        assertTrue(viewModel.uiState.value.isEmpty)
    }

    // 이동

    @Test
    fun addClicked_navigatesToNewTransaction() = runTest {
        val viewModel = viewModel()

        viewModel.sideEffect.test {
            viewModel.onIntent(HomeIntent.AddClicked)

            assertEquals(HomeSideEffect.NavigateToEdit(0), awaitItem())
        }
    }

    @Test
    fun transactionClicked_navigatesToEdit() = runTest {
        val viewModel = viewModel()

        viewModel.sideEffect.test {
            viewModel.onIntent(HomeIntent.TransactionClicked(lunch.id))

            assertEquals(HomeSideEffect.NavigateToEdit(lunch.id), awaitItem())
        }
    }

    // 삭제와 실행 취소

    @Test
    fun deleteRequested_deletesAndShowsUndo() = runTest {
        val viewModel = viewModel()

        viewModel.sideEffect.test {
            viewModel.onIntent(HomeIntent.DeleteRequested(breakfast.id))

            assertEquals(HomeSideEffect.ShowDeleteUndo, awaitItem())
        }
        assertEquals(listOf(breakfast), transactionRepository.deleted)
        assertEquals(listOf(day2), viewModel.uiState.value.dailyGroups.map { it.date })
    }

    @Test
    fun undoDeleteClicked_restoresSameTransaction() {
        val viewModel = viewModel()
        viewModel.onIntent(HomeIntent.DeleteRequested(breakfast.id))

        viewModel.onIntent(HomeIntent.UndoDeleteClicked)

        // 같은 id 그대로 다시 넣는다
        assertEquals(listOf(breakfast), transactionRepository.added)
        assertEquals(listOf(day2, day1), viewModel.uiState.value.dailyGroups.map { it.date })
    }

    @Test
    fun undoDeleteClicked_withoutDelete_doesNothing() {
        val viewModel = viewModel()

        viewModel.onIntent(HomeIntent.UndoDeleteClicked)

        assertTrue(transactionRepository.added.isEmpty())
    }

    @Test
    fun undoDeleteClicked_twice_restoresOnlyOnce() {
        val viewModel = viewModel()
        viewModel.onIntent(HomeIntent.DeleteRequested(breakfast.id))

        viewModel.onIntent(HomeIntent.UndoDeleteClicked)
        viewModel.onIntent(HomeIntent.UndoDeleteClicked)

        assertEquals(1, transactionRepository.added.size)
    }

    @Test
    fun deleteRequested_failure_showsMessage() = runTest {
        val viewModel = viewModel()
        transactionRepository.failure = IllegalStateException("디스크 오류")

        viewModel.sideEffect.test {
            viewModel.onIntent(HomeIntent.DeleteRequested(breakfast.id))

            assertEquals(HomeSideEffect.ShowMessage(R.string.home_delete_failed), awaitItem())
        }
    }

    private fun viewModel(repository: FakeTransactionRepository = transactionRepository) =
        HomeViewModel(
            getMonthlyTransactions = GetMonthlyTransactionsUseCase(repository, categoryRepository),
            deleteTransaction = DeleteTransactionUseCase(repository),
            addTransaction = AddTransactionUseCase(repository)
        )

    private fun transaction(id: Long, date: LocalDate) = Transaction(
        id = id,
        amount = 10_000,
        type = TransactionType.EXPENSE,
        categoryId = food.id,
        date = date,
        createdAt = Instant.parse("2026-01-01T00:00:00Z")
    )
}
