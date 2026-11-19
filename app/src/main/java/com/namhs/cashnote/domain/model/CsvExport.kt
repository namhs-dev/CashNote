package com.namhs.cashnote.domain.model

/** 내보낼 CSV 파일 하나 */
data class CsvExport(
    /** 저장할 파일 이름. 예: pocketjangbu-2026-11.csv */
    val fileName: String,
    /** BOM과 머리글을 포함한 파일 내용 */
    val content: String,
    /** 머리글을 뺀 거래 행 수 */
    val rowCount: Int
)
