package com.namhs.cashnote.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** 환율 동기화 Worker를 하루 1회, 네트워크가 연결됐을 때만 실행되도록 예약한다 */
@Singleton
class ExchangeRateSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 같은 이름의 작업이 이미 예약돼 있으면 그대로 둔다(KEEP).
     * 앱을 실행할 때마다 호출해도 주기가 처음부터 다시 시작되지 않는다.
     */
    fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<ExchangeRateSyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val WORK_NAME = "exchange_rate_sync"

        /** 재시도 간격의 시작값. 15분 → 30분 → 60분으로 늘어난다 */
        private const val BACKOFF_DELAY_MINUTES = 15L
    }
}
