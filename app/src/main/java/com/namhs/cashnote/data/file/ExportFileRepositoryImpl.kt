package com.namhs.cashnote.data.file

import android.content.Context
import androidx.core.content.FileProvider
import com.namhs.cashnote.domain.model.CsvExport
import com.namhs.cashnote.domain.repository.ExportFileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 내보낼 파일을 앱 캐시의 exports 폴더에 저장하고 FileProvider 주소를 만든다.
 *
 * 캐시 폴더라 공유가 끝난 파일을 따로 지우지 않아도 기기 공간이 부족하면 시스템이 정리한다.
 * 폴더 이름은 res/xml/file_paths.xml의 cache-path와 맞춰야 한다.
 */
class ExportFileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ExportFileRepository {
    override suspend fun save(export: CsvExport): String = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, EXPORT_DIRECTORY).apply { mkdirs() }
        val file = File(directory, export.fileName)
        file.writeText(export.content, Charsets.UTF_8)
        FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
            .toString()
    }

    companion object {
        const val EXPORT_DIRECTORY = "exports"

        /** AndroidManifest의 provider authorities와 맞춘다 */
        const val AUTHORITY_SUFFIX = ".fileprovider"
    }
}
