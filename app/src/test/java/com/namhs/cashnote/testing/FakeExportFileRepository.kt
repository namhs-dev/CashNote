package com.namhs.cashnote.testing

import com.namhs.cashnote.domain.model.CsvExport
import com.namhs.cashnote.domain.repository.ExportFileRepository

/** 저장한 파일을 기록하는 가짜 Repository. [failure]가 있으면 저장에서 이 예외를 던진다 */
class FakeExportFileRepository : ExportFileRepository {
    val saved = mutableListOf<CsvExport>()

    var failure: Exception? = null

    override suspend fun save(export: CsvExport): String {
        failure?.let { throw it }
        saved += export
        return "content://test/${export.fileName}"
    }
}
