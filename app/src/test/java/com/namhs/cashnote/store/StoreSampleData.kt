package com.namhs.cashnote.store

import com.namhs.cashnote.domain.model.BudgetStatus
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.CategoryExpense
import com.namhs.cashnote.domain.model.ExchangeRate
import com.namhs.cashnote.domain.model.ForeignAmount
import com.namhs.cashnote.domain.model.MonthlySummary
import com.namhs.cashnote.domain.model.PaymentMethod
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.model.TransactionWithCategory
import com.namhs.cashnote.presentation.category.CategoryManageUiState
import com.namhs.cashnote.presentation.home.DailyTransactions
import com.namhs.cashnote.presentation.home.HomeUiState
import com.namhs.cashnote.presentation.statistics.StatisticsUiState
import com.namhs.cashnote.presentation.transaction.TransactionEditUiState
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * 스토어 스크린샷용 예시 데이터. 2026년 11월 17일(화) 기준이다.
 *
 * 카테고리는 앱을 처음 설치했을 때와 같은 기본 카테고리(DefaultCategories)의 이름·색을 쓴다.
 */
object StoreSampleData {
    private val month = YearMonth.of(2026, 11)

    private val food = expense(1, "식비", "food", 0xFFFF8A65)
    private val transport = expense(2, "교통", "transport", 0xFF4FC3F7)
    private val shopping = expense(3, "쇼핑", "shopping", 0xFFBA68C8)
    private val housing = expense(4, "주거·통신", "housing", 0xFF7986CB)
    private val health = expense(5, "의료·건강", "health", 0xFFE57373)
    private val leisure = expense(6, "문화·여가", "leisure", 0xFFFFD54F)
    private val etcExpense = expense(7, "기타 지출", "etc", 0xFF90A4AE)
    private val salary = Category(8, "급여", TransactionType.INCOME, "salary", 0xFF81C784)

    private val expenseCategories =
        listOf(food, transport, shopping, housing, health, leisure, etcExpense)

    // 24.99달러 × 1,450.5 = 36,248원 (반올림)
    private val usdRate = ExchangeRate(
        currencyCode = "USD",
        currencyName = "미국 달러",
        unit = 1,
        baseRate = BigDecimal("1450.5"),
        rateDate = LocalDate.of(2026, 11, 17),
        // 한국 시각 낮 12시
        updatedAt = Instant.parse("2026-11-17T03:00:00Z")
    )
    private val bookUsd = ForeignAmount("USD", BigDecimal("24.99"), usdRate.ratePerUnit)

    // 1,200엔 × 9.35(100엔 935원) = 11,220원
    private val ramenJpy = ForeignAmount("JPY", BigDecimal("1200"), BigDecimal("9.35"))

    // 지출 합계는 통계 화면의 카테고리별 합계와 같다
    private const val TOTAL_INCOME = 4_500_000L
    private const val TOTAL_EXPENSE = 1_184_300L

    val home = HomeUiState(
        month = month,
        isLoading = false,
        dailyGroups = listOf(
            // 외화 거래를 맨 위에 둔다. 세 번째 줄부터는 추가(+) 버튼에 가려진다
            day(
                17,
                item(1, 11_220, food, "도쿄 라멘", 17, foreign = ramenJpy),
                item(2, 36_248, shopping, "해외 직구 책", 17, foreign = bookUsd)
            ),
            day(
                16,
                item(3, 9_000, food, "점심 김치찌개", 16),
                item(4, 1_500, transport, "출근 버스", 16)
            ),
            day(
                15,
                item(5, 15_000, leisure, "영화 관람", 15),
                item(6, 5_500, food, "카페", 15, PaymentMethod.CASH)
            ),
            day(10, item(7, TOTAL_INCOME, salary, "11월 급여", 10, PaymentMethod.OTHER))
        ),
        summary = MonthlySummary(income = TOTAL_INCOME, expense = TOTAL_EXPENSE),
        // 79% 사용: 경고(80%) 전 단계
        budget = BudgetStatus(budget = 1_500_000, spent = TOTAL_EXPENSE)
    )

    val foreignEdit = TransactionEditUiState(
        amountInput = "24.99",
        currencyCode = "USD",
        exchangeRates = listOf(usdRate),
        appliedRate = usdRate.ratePerUnit,
        rateDate = usdRate.rateDate,
        rateUpdatedAt = usdRate.updatedAt,
        categories = expenseCategories,
        selectedCategoryId = shopping.id,
        date = LocalDate.of(2026, 11, 17),
        memo = "해외 직구 책",
        paymentMethod = PaymentMethod.CARD
    )

    val statistics = StatisticsUiState(
        month = month,
        isLoading = false,
        expenses = listOf(
            food to 412_000L,
            housing to 350_000L,
            shopping to 186_300L,
            transport to 98_000L,
            leisure to 84_000L,
            health to 32_000L,
            etcExpense to 22_000L
        ).map { (category, amount) ->
            CategoryExpense(category, amount, amount.toFloat() / TOTAL_EXPENSE)
        }
    )

    val categoryManage = CategoryManageUiState(
        type = TransactionType.EXPENSE,
        categories = expenseCategories
    )

    private fun expense(id: Long, name: String, icon: String, color: Long) =
        Category(id, name, TransactionType.EXPENSE, icon, color)

    private fun day(dayOfMonth: Int, vararg items: TransactionWithCategory) =
        DailyTransactions(month.atDay(dayOfMonth), items.toList())

    private fun item(
        id: Long,
        amount: Long,
        category: Category,
        memo: String,
        dayOfMonth: Int,
        paymentMethod: PaymentMethod = PaymentMethod.CARD,
        foreign: ForeignAmount? = null
    ) = TransactionWithCategory(
        transaction = Transaction(
            id = id,
            amount = amount,
            type = category.type,
            categoryId = category.id,
            memo = memo,
            date = month.atDay(dayOfMonth),
            createdAt = Instant.EPOCH,
            foreign = foreign,
            paymentMethod = paymentMethod
        ),
        category = category
    )
}
