package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.ForeignAmount
import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.usecase.ExportMonthlyCsvUseCase.Companion.escapeCsv
import com.namhs.cashnote.domain.usecase.ExportMonthlyCsvUseCase.Companion.neutralizeFormula
import com.namhs.cashnote.testing.FakeCategoryRepository
import com.namhs.cashnote.testing.FakeTransactionRepository
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportMonthlyCsvUseCaseTest {
    private val food = Category(1, "식비", TransactionType.EXPENSE, "food", 0xFFE57373)
    private val salary = Category(8, "급여", TransactionType.INCOME, "salary", 0xFF81C784)

    private val bom = Char(0xFEFF).toString()
    private val header = "날짜,유형,카테고리,금액(원),통화,외화 금액,적용 환율,메모\r\n"

    private fun transaction(
        id: Long,
        date: LocalDate,
        amount: Long,
        category: Category,
        memo: String = "",
        foreign: ForeignAmount? = null,
        createdAt: Instant = Instant.parse("2026-11-01T00:00:00Z")
    ) = Transaction(
        id = id,
        amount = amount,
        type = category.type,
        categoryId = category.id,
        memo = memo,
        date = date,
        createdAt = createdAt,
        foreign = foreign
    )

    private fun useCase(vararg transactions: Transaction) = ExportMonthlyCsvUseCase(
        FakeTransactionRepository(transactions.toList()),
        FakeCategoryRepository(listOf(food, salary))
    )

    @Test
    fun export_writesBomHeaderAndRowsInDateOrder() = runTest {
        val usd = ForeignAmount("USD", BigDecimal("12.50"), BigDecimal("1450.5000"))
        val csv = useCase(
            transaction(1, LocalDate.of(2026, 11, 5), 3_000_000, salary),
            transaction(2, LocalDate.of(2026, 11, 3), 18_131, food, "점심", usd),
            // 다른 달 거래는 빠진다
            transaction(3, LocalDate.of(2026, 10, 31), 5_000, food)
        )(YearMonth.of(2026, 11))

        assertEquals("cashnote-2026-11.csv", csv.fileName)
        assertEquals(2, csv.rowCount)
        assertEquals(
            bom + header +
                "2026-11-03,지출,식비,18131,USD,12.50,1450.5,점심\r\n" +
                "2026-11-05,수입,급여,3000000,KRW,,,\r\n",
            csv.content
        )
    }

    @Test
    fun export_sameDay_keepsInputOrder() = runTest {
        val day = LocalDate.of(2026, 11, 3)
        val csv = useCase(
            transaction(
                2,
                day,
                2_000,
                food,
                "저녁",
                createdAt = Instant.parse("2026-11-03T10:00:00Z")
            ),
            transaction(
                1,
                day,
                1_000,
                food,
                "아침",
                createdAt = Instant.parse("2026-11-03T01:00:00Z")
            )
        )(YearMonth.of(2026, 11))

        val lines = csv.content.removePrefix(bom).split("\r\n")
        assertTrue(lines[1].endsWith("아침"))
        assertTrue(lines[2].endsWith("저녁"))
    }

    @Test
    fun export_emptyMonth_hasOnlyHeader() = runTest {
        val csv = useCase()(YearMonth.of(2026, 11))

        assertEquals(0, csv.rowCount)
        assertEquals(bom + header, csv.content)
    }

    @Test
    fun export_memoWithSpecialCharacters_isQuoted() = runTest {
        val csv = useCase(
            transaction(1, LocalDate.of(2026, 11, 3), 10_000, food, "점심, \"맛집\"\n동료와")
        )(YearMonth.of(2026, 11))

        assertTrue(csv.content.endsWith(",\"점심, \"\"맛집\"\"\n동료와\"\r\n"))
    }

    @Test
    fun escapeCsv_plainText_isUnchanged() {
        assertEquals("점심", "점심".escapeCsv())
    }

    @Test
    fun neutralizeFormula_prefixesFormulaStarters() {
        assertEquals("'=SUM(A1)", "=SUM(A1)".neutralizeFormula())
        assertEquals("'+82", "+82".neutralizeFormula())
        assertEquals("'-환불", "-환불".neutralizeFormula())
        assertEquals("'@멘션", "@멘션".neutralizeFormula())
        assertEquals("점심", "점심".neutralizeFormula())
        assertEquals("", "".neutralizeFormula())
    }

    @Test
    fun export_formulaMemo_isNeutralized() = runTest {
        val csv = useCase(
            transaction(1, LocalDate.of(2026, 11, 3), 10_000, food, "=HYPERLINK(\"x\")")
        )(YearMonth.of(2026, 11))

        assertTrue(csv.content.endsWith(",\"'=HYPERLINK(\"\"x\"\")\"\r\n"))
    }
}
