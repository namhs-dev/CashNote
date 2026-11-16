package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.repository.ExportFileRepository
import java.time.YearMonth
import javax.inject.Inject

/**
 * [month]의 거래를 CSV 파일로 저장하고 공유용 주소를 돌려준다.
 *
 * 거래가 없는 달이면 빈 파일을 만들지 않고 null을 돌려준다.
 */
class ShareMonthlyCsvUseCase @Inject constructor(
    private val exportMonthlyCsv: ExportMonthlyCsvUseCase,
    private val exportFileRepository: ExportFileRepository
) {
    suspend operator fun invoke(month: YearMonth): String? {
        val export = exportMonthlyCsv(month)
        if (export.rowCount == 0) return null
        return exportFileRepository.save(export)
    }
}
