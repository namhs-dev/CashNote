package com.namhs.cashnote.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** 실제 DataStore를 임시 파일에 만들어 읽고 쓴다 */
class SettingsRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun repository(file: File = File(temporaryFolder.root, "settings.preferences_pb")) =
        SettingsRepositoryImpl(
            PreferenceDataStoreFactory.create(scope = dataStoreScope, produceFile = { file })
        )

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun defaultCurrency_initiallyKrw() = runTest {
        assertNull(repository().observeDefaultCurrency().first())
    }

    @Test
    fun setDefaultCurrency_emitsSavedCode() = runTest {
        val repository = repository()

        repository.setDefaultCurrency("USD")

        assertEquals("USD", repository.observeDefaultCurrency().first())
    }

    @Test
    fun setDefaultCurrency_null_backToKrw() = runTest {
        val repository = repository()
        repository.setDefaultCurrency("JPY")

        repository.setDefaultCurrency(null)

        assertNull(repository.observeDefaultCurrency().first())
    }
}
