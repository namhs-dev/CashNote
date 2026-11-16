package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.CsvExport
import com.namhs.cashnote.domain.model.TransactionType
import com.namhs.cashnote.domain.model.TransactionWithCategory
import com.namhs.cashnote.domain.repository.CategoryRepository
import com.namhs.cashnote.domain.repository.TransactionRepository
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * [month]의 거래를 CSV로 만든다.
 *
 * 머리글·유형 이름은 화면 문자열이 아니라 파일 형식의 일부라 strings.xml이 아닌 코드에 둔다.
 */
class ExportMonthlyCsvUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(month: YearMonth): CsvExport {
        val transactions = transactionRepository.observeTransactions(month).first()
        val categoryById = categoryRepository.observeCategories().first().associateBy { it.id }
        val rows = transactions.mapNotNull { transaction ->
            categoryById[transaction.categoryId]?.let { TransactionWithCategory(transaction, it) }
        }
        return CsvExport(
            fileName = "cashnote-$month.csv",
            content = toCsv(rows),
            rowCount = rows.size
        )
    }

    companion object {
        /** 엑셀이 UTF-8로 읽도록 파일 앞에 붙인다. 없으면 한글이 깨진다 */
        private val BOM = Char(0xFEFF).toString()

        /** RFC 4180 줄바꿈 */
        private const val LINE_END = "\r\n"

        private val HEADER = listOf(
            "날짜",
            "유형",
            "카테고리",
            "금액(원)",
            "통화",
            "외화 금액",
            "적용 환율",
            "메모"
        )

        /** 가계부 원장처럼 날짜가 빠른 순서로 쓴다. 같은 날은 입력한 순서 */
        internal fun toCsv(rows: List<TransactionWithCategory>): String {
            val sorted = rows.sortedWith(
                compareBy<TransactionWithCategory> { it.transaction.date }
                    .thenBy { it.transaction.createdAt }
                    .thenBy { it.transaction.id }
            )
            return buildString {
                append(BOM)
                append(HEADER.toCsvLine())
                sorted.forEach { append(it.toCsvFields().toCsvLine()) }
            }
        }

        private fun TransactionWithCategory.toCsvFields(): List<String> {
            val foreign = transaction.foreign
            return listOf(
                transaction.date.toString(),
                when (transaction.type) {
                    TransactionType.EXPENSE -> "지출"
                    TransactionType.INCOME -> "수입"
                },
                category.name.neutralizeFormula(),
                transaction.amount.toString(),
                foreign?.currencyCode ?: "KRW",
                foreign?.amount?.toPlainString().orEmpty(),
                foreign?.appliedRate?.stripTrailingZeros()?.toPlainString().orEmpty(),
                transaction.memo.neutralizeFormula()
            )
        }

        /**
         * 사용자가 입력한 글자가 =, +, -, @로 시작하면 앞에 '를 붙인다.
         * 스프레드시트가 수식으로 실행하지 않고 글자로 보여 주게 하려는 것이다(CSV 인젝션 방지).
         */
        internal fun String.neutralizeFormula(): String =
            if (firstOrNull() in FORMULA_PREFIXES) "'$this" else this

        private val FORMULA_PREFIXES = setOf('=', '+', '-', '@')

        private fun List<String>.toCsvLine(): String =
            joinToString(separator = ",", postfix = LINE_END) { it.escapeCsv() }

        /** 쉼표·따옴표·줄바꿈이 있으면 따옴표로 감싸고, 안의 따옴표는 두 번 쓴다 */
        internal fun String.escapeCsv(): String =
            if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
                "\"" + replace("\"", "\"\"") + "\""
            } else {
                this
            }
    }
}
