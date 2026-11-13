package com.namhs.cashnote.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.ExchangeRate
import com.namhs.cashnote.domain.model.ForeignAmount
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.usecase.AddTransactionUseCase
import com.namhs.cashnote.domain.usecase.GetCategoriesUseCase
import com.namhs.cashnote.domain.usecase.GetExchangeRatesUseCase
import com.namhs.cashnote.domain.usecase.GetTransactionUseCase
import com.namhs.cashnote.domain.usecase.RefreshExchangeRatesUseCase
import com.namhs.cashnote.domain.usecase.UpdateTransactionUseCase
import com.namhs.cashnote.testing.FakeCategoryRepository
import com.namhs.cashnote.testing.FakeExchangeRateRepository
import com.namhs.cashnote.testing.FakeTransactionRepository
import com.namhs.cashnote.testing.MainDispatcherRule
import java.io.IOException
import java.math.BigDecimal
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

    private val friday = LocalDate.of(2026, 11, 13)
    private val updatedAt = Instant.parse("2026-11-13T03:00:00Z")
    private val usd = ExchangeRate("USD", "미국 달러", 1, BigDecimal("1450.5"), friday, updatedAt)
    private val jpy = ExchangeRate("JPY", "일본 옌", 100, BigDecimal("935.12"), friday, updatedAt)
    private val eur = ExchangeRate("EUR", "유로", 1, BigDecimal("1600"), friday, updatedAt)

    // 20유로를 1유로 = 1,500원으로 저장한 기존 외화 거래
    private val existingForeign = existing.copy(
        id = 8,
        amount = 30_000,
        foreign = ForeignAmount("EUR", BigDecimal("20.00"), BigDecimal("1500"))
    )

    private val transactionRepository = FakeTransactionRepository(listOf(existing, existingForeign))
    private val categoryRepository = FakeCategoryRepository(listOf(food, transport, salary))
    private val exchangeRateRepository = FakeExchangeRateRepository()

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

    // 외화

    @Test
    fun amountChanged_foreign_allowsTwoDecimalPlaces() {
        exchangeRateRepository.rates.value = listOf(usd)
        val viewModel = viewModel()
        viewModel.onIntent(TransactionEditIntent.CurrencySelected("USD"))

        viewModel.onIntent(TransactionEditIntent.AmountChanged("12.345"))
        assertEquals("12.34", viewModel.uiState.value.amountInput)

        viewModel.onIntent(TransactionEditIntent.AmountChanged("1.2.3"))
        assertEquals("1.23", viewModel.uiState.value.amountInput)
    }

    @Test
    fun currencySelected_setsRateAndPreview() {
        exchangeRateRepository.rates.value = listOf(usd, jpy)
        val viewModel = viewModel()

        viewModel.onIntent(TransactionEditIntent.CurrencySelected("JPY"))
        viewModel.onIntent(TransactionEditIntent.AmountChanged("1000"))

        val state = viewModel.uiState.value
        assertEquals(listOf("JPY", "USD"), state.foreignCurrencyOptions.sorted())
        assertEquals(0, BigDecimal("9.3512").compareTo(state.appliedRate))
        assertEquals(friday, state.rateDate)
        // 1,000엔 × 9.3512 = 9351.2 → 9351원
        assertEquals(9_351L, state.convertedKrw)
    }

    @Test
    fun currencySelected_backToKrw_dropsFractionAndRate() {
        exchangeRateRepository.rates.value = listOf(usd)
        val viewModel = viewModel()
        viewModel.onIntent(TransactionEditIntent.CurrencySelected("USD"))
        viewModel.onIntent(TransactionEditIntent.AmountChanged("12.5"))

        viewModel.onIntent(TransactionEditIntent.CurrencySelected(null))

        val state = viewModel.uiState.value
        assertEquals("12", state.amountInput)
        assertNull(state.appliedRate)
        assertNull(state.convertedKrw)
    }

    @Test
    fun init_emptyRateCache_refreshesOnce() {
        exchangeRateRepository.refreshResult = listOf(usd)

        val viewModel = viewModel()

        assertEquals(1, exchangeRateRepository.refreshCount)
        assertEquals(listOf(usd), viewModel.uiState.value.exchangeRates)
    }

    @Test
    fun init_rateCacheExists_doesNotRefresh() {
        exchangeRateRepository.rates.value = listOf(usd)

        viewModel()

        assertEquals(0, exchangeRateRepository.refreshCount)
    }

    @Test
    fun init_refreshFailure_isIgnored() = runTest {
        exchangeRateRepository.failure = IOException("offline")

        val viewModel = viewModel()

        viewModel.sideEffect.test { expectNoEvents() }
        assertEquals(1, exchangeRateRepository.refreshCount)
        assertTrue(viewModel.uiState.value.exchangeRates.isEmpty())
    }

    @Test
    fun saveClicked_foreign_savesConvertedAmountAndForeignInfo() = runTest {
        exchangeRateRepository.rates.value = listOf(usd)
        val viewModel = viewModel()

        viewModel.sideEffect.test {
            viewModel.onIntent(TransactionEditIntent.CurrencySelected("USD"))
            viewModel.onIntent(TransactionEditIntent.AmountChanged("12.5"))
            viewModel.onIntent(TransactionEditIntent.CategorySelected(food.id))
            viewModel.onIntent(TransactionEditIntent.SaveClicked)

            assertEquals(TransactionEditSideEffect.NavigateBack, awaitItem())
        }
        val added = transactionRepository.added.single()
        // 12.5 × 1450.5 = 18131.25 → 18131원
        assertEquals(18_131L, added.amount)
        assertEquals(ForeignAmount("USD", BigDecimal("12.5"), usd.ratePerUnit), added.foreign)
    }

    @Test
    fun saveClicked_foreignWithoutRate_showsMessageAndDoesNotSave() = runTest {
        // 기존 거래의 통화(EUR)는 캐시에 없어 바꾸면 환율이 없다
        val viewModel = viewModel(transactionId = existingForeign.id)
        viewModel.onIntent(TransactionEditIntent.CurrencySelected("USD"))

        viewModel.sideEffect.test {
            viewModel.onIntent(TransactionEditIntent.SaveClicked)

            assertEquals(
                TransactionEditSideEffect.ShowMessage(R.string.transaction_rate_unavailable),
                awaitItem()
            )
        }
        assertTrue(viewModel.uiState.value.isRateUnavailable)
        assertTrue(transactionRepository.updated.isEmpty())
    }

    @Test
    fun saveClicked_foreignRoundsToZero_showsAmountError() {
        exchangeRateRepository.rates.value = listOf(jpy)
        val viewModel = viewModel()
        viewModel.onIntent(TransactionEditIntent.CurrencySelected("JPY"))
        viewModel.onIntent(TransactionEditIntent.AmountChanged("0.01"))
        viewModel.onIntent(TransactionEditIntent.CategorySelected(food.id))

        viewModel.onIntent(TransactionEditIntent.SaveClicked)

        assertTrue(viewModel.uiState.value.isAmountError)
        assertTrue(transactionRepository.added.isEmpty())
    }

    @Test
    fun init_editForeign_fillsForeignAmountAndKeepsAppliedRate() {
        // 캐시에 더 최신 EUR 환율이 있어도 기존 거래의 적용 환율을 쓴다
        exchangeRateRepository.rates.value = listOf(eur)

        val state = viewModel(transactionId = existingForeign.id).uiState.value

        assertEquals("EUR", state.currencyCode)
        assertEquals("20.00", state.amountInput)
        assertEquals(BigDecimal("1500"), state.appliedRate)
        assertNull(state.rateDate)
    }

    @Test
    fun saveClicked_editForeignSameCurrency_keepsOriginalRate() = runTest {
        exchangeRateRepository.rates.value = listOf(eur)
        val viewModel = viewModel(transactionId = existingForeign.id)

        viewModel.sideEffect.test {
            viewModel.onIntent(TransactionEditIntent.AmountChanged("30"))
            viewModel.onIntent(TransactionEditIntent.SaveClicked)

            assertEquals(TransactionEditSideEffect.NavigateBack, awaitItem())
        }
        val updated = transactionRepository.updated.single()
        assertEquals(45_000L, updated.amount)
        assertEquals(ForeignAmount("EUR", BigDecimal("30"), BigDecimal("1500")), updated.foreign)
    }

    @Test
    fun saveClicked_editForeignChangedCurrency_usesLatestRate() = runTest {
        exchangeRateRepository.rates.value = listOf(usd, eur)
        val viewModel = viewModel(transactionId = existingForeign.id)

        viewModel.sideEffect.test {
            viewModel.onIntent(TransactionEditIntent.CurrencySelected("USD"))
            viewModel.onIntent(TransactionEditIntent.SaveClicked)

            assertEquals(TransactionEditSideEffect.NavigateBack, awaitItem())
        }
        val updated = transactionRepository.updated.single()
        // 20.00 × 1450.5 = 29010원
        assertEquals(29_010L, updated.amount)
        assertEquals("USD", updated.foreign?.currencyCode)
        assertEquals(0, usd.ratePerUnit.compareTo(updated.foreign?.appliedRate))
    }

    @Test
    fun saveClicked_editForeignToKrw_clearsForeignInfo() = runTest {
        val viewModel = viewModel(transactionId = existingForeign.id)

        viewModel.sideEffect.test {
            viewModel.onIntent(TransactionEditIntent.CurrencySelected(null))
            viewModel.onIntent(TransactionEditIntent.AmountChanged("30000"))
            viewModel.onIntent(TransactionEditIntent.SaveClicked)

            assertEquals(TransactionEditSideEffect.NavigateBack, awaitItem())
        }
        val updated = transactionRepository.updated.single()
        assertEquals(30_000L, updated.amount)
        assertNull(updated.foreign)
    }

    @Test
    fun sanitizeAmount_krw_dropsFraction() {
        assertEquals("12", TransactionEditViewModel.sanitizeAmount("12.5", isForeign = false))
    }

    private fun viewModel(transactionId: Long = 0) = TransactionEditViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(TransactionEditViewModel.ARG_TRANSACTION_ID to transactionId)
        ),
        getTransaction = GetTransactionUseCase(transactionRepository),
        getCategories = GetCategoriesUseCase(categoryRepository),
        addTransaction = AddTransactionUseCase(transactionRepository),
        updateTransaction = UpdateTransactionUseCase(transactionRepository),
        getExchangeRates = GetExchangeRatesUseCase(exchangeRateRepository),
        refreshExchangeRates = RefreshExchangeRatesUseCase(exchangeRateRepository)
    )
}
