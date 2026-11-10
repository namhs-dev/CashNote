package com.namhs.cashnote.testing

import com.namhs.cashnote.domain.model.Transaction
import com.namhs.cashnote.domain.repository.TransactionRepository
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** 메모리에 거래를 담는 가짜 Repository. 쓰기 호출을 기록하고, 실패를 흉내 낼 수 있다 */
class FakeTransactionRepository(initial: List<Transaction> = emptyList()) : TransactionRepository {
    private val transactions = MutableStateFlow(initial)

    val added = mutableListOf<Transaction>()
    val updated = mutableListOf<Transaction>()
    val deleted = mutableListOf<Transaction>()

    /** null이 아니면 쓰기 함수가 이 예외를 던진다 */
    var failure: Exception? = null

    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    /** 실제 DB 쿼리와 같은 순서(날짜 → 생성 시각 → id, 최신순)로 해당 월만 돌려준다 */
    override fun observeTransactions(month: YearMonth): Flow<List<Transaction>> =
        transactions.map { list ->
            list.filter { YearMonth.from(it.date) == month }
                .sortedWith(
                    compareByDescending<Transaction> { it.date }
                        .thenByDescending { it.createdAt }
                        .thenByDescending { it.id }
                )
        }

    override suspend fun getTransaction(id: Long): Transaction? =
        transactions.value.find { it.id == id }

    override suspend fun add(transaction: Transaction): Long {
        failure?.let { throw it }
        // 실제 Repository처럼 id가 0이면 새 id를, 아니면 그 id를 쓴다
        val id = if (transaction.id == 0L) nextId++ else transaction.id
        added += transaction
        transactions.update { it + transaction.copy(id = id) }
        return id
    }

    override suspend fun update(transaction: Transaction) {
        failure?.let { throw it }
        updated += transaction
        transactions.update { list ->
            list.map { if (it.id == transaction.id) transaction else it }
        }
    }

    override suspend fun delete(transaction: Transaction) {
        failure?.let { throw it }
        deleted += transaction
        transactions.update { list -> list.filterNot { it.id == transaction.id } }
    }
}
