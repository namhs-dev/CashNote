package com.namhs.cashnote.data.worker

import androidx.work.ListenableWorker.Result
import com.namhs.cashnote.data.remote.ExchangeRateApiException
import com.namhs.cashnote.data.remote.ExchangeRateRemoteDataSource
import com.namhs.cashnote.data.worker.ExchangeRateSyncWorker.Companion.MAX_RETRY_COUNT
import com.namhs.cashnote.data.worker.ExchangeRateSyncWorker.Companion.resultOnError
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ExchangeRateSyncWorkerTest {
    private fun httpException(code: Int): HttpException {
        val response = Response.error<Any>(code, "".toResponseBody())
        return HttpException(response)
    }

    @Test
    fun ioException_retriesUntilMaxRetryCount() {
        val error = IOException("offline")

        assertEquals(Result.retry(), resultOnError(error, runAttemptCount = 0))
        assertEquals(Result.retry(), resultOnError(error, runAttemptCount = MAX_RETRY_COUNT - 1))
        assertEquals(Result.failure(), resultOnError(error, runAttemptCount = MAX_RETRY_COUNT))
    }

    @Test
    fun timeout_isRetried() {
        assertEquals(Result.retry(), resultOnError(SocketTimeoutException(), runAttemptCount = 0))
    }

    @Test
    fun serverError_retriesUntilMaxRetryCount() {
        val error = httpException(503)

        assertEquals(Result.retry(), resultOnError(error, runAttemptCount = 0))
        assertEquals(Result.failure(), resultOnError(error, runAttemptCount = MAX_RETRY_COUNT))
    }

    @Test
    fun clientError_failsWithoutRetry() {
        assertEquals(Result.failure(), resultOnError(httpException(404), runAttemptCount = 0))
    }

    @Test
    fun apiException_failsWithoutRetry() {
        val invalidAuth = ExchangeRateApiException(ExchangeRateRemoteDataSource.RESULT_INVALID_AUTH)

        assertEquals(Result.failure(), resultOnError(invalidAuth, runAttemptCount = 0))
    }

    @Test
    fun malformedResponse_failsWithoutRetry() {
        val error = SerializationException("bad json")

        assertEquals(Result.failure(), resultOnError(error, runAttemptCount = 0))
    }
}
