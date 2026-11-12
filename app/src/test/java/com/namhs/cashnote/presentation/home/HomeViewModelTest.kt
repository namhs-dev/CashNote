package com.namhs.cashnote.presentation.home

import androidx.lifecycle.SavedStateHandle
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

    // 다른 달 거래. 이번 달 목록에는 나오지 않는다
    private val nextMonth = thisMonth.plusMonths(1)
    private val nextMonthTransaction = transaction(id = 4, date = nextMonth.atDay(1))

    private val transactionRepository =
        FakeTransactionRepository(listOf(breakfast, lunch, dinner, nextMonthTransaction))
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

    // 월 이동

    @Test
    fun nextMonthClicked_showsNextMonthTransactions() {
        val viewModel = viewModel()

        viewModel.onIntent(HomeIntent.NextMonthClicked)

        val state = viewModel.uiState.value
        assertEquals(nextMonth, state.month)
        assertEquals(
            listOf(nextMonthTransaction),
            state.dailyGroups.flatMap { group -> group.items.map { it.transaction } }
        )
    }

    @Test
    fun previousMonthClicked_movesToPreviousMonth() {
        val viewModel = viewModel()

        viewModel.onIntent(HomeIntent.PreviousMonthClicked)

        assertEquals(thisMonth.minusMonths(1), viewModel.uiState.value.month)
    }

    @Test
    fun nextMonthClicked_inDecember_movesToJanuaryOfNextYear() {
        val viewModel = viewModel(savedStateHandle = savedMonth("2026-12"))

        viewModel.onIntent(HomeIntent.NextMonthClicked)

        assertEquals(YearMonth.of(2027, 1), viewModel.uiState.value.month)
    }

    @Test
    fun previousMonthClicked_inJanuary_movesToDecemberOfPreviousYear() {
        val viewModel = viewModel(savedStateHandle = savedMonth("2026-01"))

        viewModel.onIntent(HomeIntent.PreviousMonthClicked)

        assertEquals(YearMonth.of(2025, 12), viewModel.uiState.value.month)
    }

    @Test
    fun monthChanged_toEmptyMonth_isEmpty() {
        val viewModel = viewModel()

        viewModel.onIntent(HomeIntent.PreviousMonthClicked)

        // 이번 달 목록이 남아 있지 않아야 한다
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun monthChanged_savesMonthToSavedStateHandle() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = viewModel(savedStateHandle = savedStateHandle)

        viewModel.onIntent(HomeIntent.NextMonthClicked)

        assertEquals(nextMonth.toString(), savedStateHandle.get<String>(HomeViewModel.KEY_MONTH))
    }

    @Test
    fun init_withSavedMonth_restoresMonthAndItsTransactions() {
        val march = transaction(id = 1, date = LocalDate.of(2026, 3, 5))
        val viewModel = viewModel(
            repository = FakeTransactionRepository(listOf(march)),
            savedStateHandle = savedMonth("2026-03")
        )

        val state = viewModel.uiState.value
        assertEquals(YearMonth.of(2026, 3), state.month)
        assertEquals(listOf(march), state.dailyGroups.single().items.map { it.transaction })
    }

    @Test
    fun monthChanged_ignoresChangesInPreviousMonth() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(HomeIntent.NextMonthClicked)

        // 이전 달 구독이 취소되지 않았다면 이 변경이 목록을 이번 달 거래로 덮어쓴다
        transactionRepository.add(transaction(id = 5, date = day1))

        assertEquals(
            listOf(nextMonth.atDay(1)),
            viewModel.uiState.value.dailyGroups.map { it.date }
        )
    }

    // 화면 이동

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

    private fun viewModel(
        repository: FakeTransactionRepository = transactionRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) = HomeViewModel(
        savedStateHandle = savedStateHandle,
        getMonthlyTransactions = GetMonthlyTransactionsUseCase(repository, categoryRepository),
        deleteTransaction = DeleteTransactionUseCase(repository),
        addTransaction = AddTransactionUseCase(repository)
    )

    // 프로세스 복원처럼 이미 월이 저장된 상태를 만든다
    private fun savedMonth(month: String) =
        SavedStateHandle(mapOf(HomeViewModel.KEY_MONTH to month))

    private fun transaction(id: Long, date: LocalDate) = Transaction(
        id = id,
        amount = 10_000,
        type = TransactionType.EXPENSE,
        categoryId = food.id,
        date = date,
        createdAt = Instant.parse("2026-01-01T00:00:00Z")
    )
}
