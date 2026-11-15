package com.namhs.cashnote.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.namhs.cashnote.domain.usecase.RefreshExchangeRatesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException

/**
 * 최신 환율을 받아 Room에 저장하는 주기 동기화 Worker.
 *
 * 화면은 Room Flow를 구독하므로 저장되면 자동으로 새 환율을 보여 준다.
 */
@HiltWorker
class ExchangeRateSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val refreshExchangeRates: RefreshExchangeRatesUseCase
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        refreshExchangeRates()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        resultOnError(e, runAttemptCount)
    }

    companion object {
        /** 같은 주기 안에서 재시도하는 최대 횟수. 넘기면 다음 주기 실행에 맡긴다 */
        const val MAX_RETRY_COUNT = 3

        /**
         * 동기화 실패 시 재시도 여부를 정한다.
         *
         * 연결 끊김·서버 오류(5xx)처럼 잠시 뒤 풀릴 수 있는 오류만 재시도한다.
         * 인증키 없음·일일 한도 초과([com.namhs.cashnote.data.remote.ExchangeRateApiException])나
         * 응답 형식 오류는 재시도해도 같으므로 바로 실패로 끝낸다.
         *
         * @param runAttemptCount 이번 실행 전까지 시도한 횟수 (첫 실행은 0)
         */
        internal fun resultOnError(error: Throwable, runAttemptCount: Int): Result {
            val isTransient = when (error) {
                is IOException -> true
                is HttpException -> error.code() >= 500
                else -> false
            }
            val canRetry = isTransient && runAttemptCount < MAX_RETRY_COUNT
            return if (canRetry) Result.retry() else Result.failure()
        }
    }
}
