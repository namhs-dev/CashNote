package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.BudgetLevel
import com.namhs.cashnote.domain.model.NotificationSettings
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.testing.FakeBudgetRepository
import com.namhs.cashnote.testing.FakeNotifier
import com.namhs.cashnote.testing.FakeSettingsRepository
import com.namhs.cashnote.testing.FakeTransactionRepository
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckBudgetAlertUseCaseTest {
    private val november = YearMonth.of(2026, 11)
    private val december = YearMonth.of(2026, 12)

    private val settingsRepository = FakeSettingsRepository().apply {
        notificationSettings.value = NotificationSettings(budgetAlertEnabled = true)
    }
    private val budgetRepository = FakeBudgetRepository(mapOf(november to 100_000L))
    private val transactionRepository = FakeTransactionRepository()
    private val notifier = FakeNotifier()

    private val checkBudgetAlert = CheckBudgetAlertUseCase(
        settingsRepository,
        GetBudgetStatusUseCase(budgetRepository, transactionRepository),
        notifier
    )

    private var nextId = 1L

    /** [month] 1일에 [amount]원 지출을 추가한다 */
    private suspend fun spend(amount: Long, month: YearMonth = november) {
        transactionRepository.add(
            Transaction(
                id = nextId++,
                amount = amount,
                type = TransactionType.EXPENSE,
                categoryId = 1,
                date = month.atDay(1),
                createdAt = Instant.parse("2026-11-01T00:00:00Z")
            )
        )
    }

    @Test
    fun belowWarning_sendsNothing() = runTest {
        spend(79_999)

        assertFalse(checkBudgetAlert(november))
        assertTrue(notifier.budgetAlerts.isEmpty())
    }

    @Test
    fun reachingWarning_sendsOnce() = runTest {
        spend(80_000)

        assertTrue(checkBudgetAlert(november))
        // 같은 단계에서 또 저장해도 다시 보내지 않는다
        spend(5_000)
        assertFalse(checkBudgetAlert(november))

        assertEquals(listOf(Triple(BudgetLevel.WARNING, 100_000L, 80_000L)), notifier.budgetAlerts)
    }

    @Test
    fun warningThenExceeded_sendsBoth() = runTest {
        spend(85_000)
        checkBudgetAlert(november)

        spend(20_000)

        assertTrue(checkBudgetAlert(november))
        assertEquals(
            listOf(BudgetLevel.WARNING, BudgetLevel.EXCEEDED),
            notifier.budgetAlerts.map { it.first }
        )
    }

    @Test
    fun jumpStraightToExceeded_sendsOnlyExceeded() = runTest {
        spend(120_000)

        assertTrue(checkBudgetAlert(november))
        assertEquals(listOf(BudgetLevel.EXCEEDED), notifier.budgetAlerts.map { it.first })
    }

    @Test
    fun exceededThenDroppedToWarning_doesNotResend() = runTest {
        spend(120_000)
        checkBudgetAlert(november)

        // 거래를 지워 80%대로 내려가도 이미 더 높은 단계를 알렸으므로 보내지 않는다
        transactionRepository.delete(
            transactionRepository.observeTransactions(november).first().first()
        )
        spend(85_000)

        assertFalse(checkBudgetAlert(november))
        assertEquals(1, notifier.budgetAlerts.size)
    }

    @Test
    fun nextMonth_startsOver() = runTest {
        budgetRepository.setBudget(december, 100_000)
        spend(90_000)
        checkBudgetAlert(november)

        spend(90_000, december)

        assertTrue(checkBudgetAlert(december))
        assertEquals(2, notifier.budgetAlerts.size)
    }

    @Test
    fun alertDisabled_sendsNothing() = runTest {
        settingsRepository.notificationSettings.value = NotificationSettings()
        spend(120_000)

        assertFalse(checkBudgetAlert(november))
        assertTrue(notifier.budgetAlerts.isEmpty())
    }

    @Test
    fun noBudget_sendsNothing() = runTest {
        // 10월에는 예산이 없다(11월부터 적용)
        spend(500_000, YearMonth.of(2026, 10))

        assertFalse(checkBudgetAlert(YearMonth.of(2026, 10)))
        assertTrue(notifier.budgetAlerts.isEmpty())
    }
}
