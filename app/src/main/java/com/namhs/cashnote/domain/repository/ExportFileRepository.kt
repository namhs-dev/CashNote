package com.namhs.cashnote.domain.repository

import com.namhs.cashnote.domain.model.CsvExport

/** 내보낼 파일을 다른 앱과 공유할 수 있는 곳에 저장한다 */
interface ExportFileRepository {
    /**
     * [export]를 파일로 저장하고, 다른 앱이 읽을 수 있는 content:// 주소를 문자열로 돌려준다.
     * 같은 이름의 파일이 있으면 덮어쓴다.
     */
    suspend fun save(export: CsvExport): String
}
