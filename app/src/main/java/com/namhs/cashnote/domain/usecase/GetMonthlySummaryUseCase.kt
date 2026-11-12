package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.model.MonthlySummary
import com.namhs.cashnote.domain.repository.TransactionRepository
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 해당 월의 수입·지출 합계를 구독한다. 거래가 추가·수정·삭제되면 다시 계산된다 */
class GetMonthlySummaryUseCase @Inject constructor(private val repository: TransactionRepository) {
    operator fun invoke(month: YearMonth): Flow<MonthlySummary> =
        repository.observeMonthlySummary(month)
}
