package com.namhs.cashnote

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.namhs.cashnote.data.notification.NotificationChannels
import com.namhs.cashnote.data.worker.ExchangeRateSyncScheduler
import com.namhs.cashnote.domain.usecase.EnsureReminderScheduledUseCase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Hilt 의존성 그래프의 시작점이 되는 Application 클래스
@HiltAndroidApp
class CashNoteApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var exchangeRateSyncScheduler: ExchangeRateSyncScheduler

    @Inject
    lateinit var ensureReminderScheduled: EnsureReminderScheduledUseCase

    // 앱 프로세스가 사는 동안의 작업용. 하나가 실패해도 다른 작업은 계속한다
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        // super.onCreate()에서 Hilt가 필드를 주입하므로 그 뒤에 예약한다
        super.onCreate()
        NotificationChannels.create(this)
        exchangeRateSyncScheduler.schedule()
        // 설정을 DataStore에서 읽어야 하므로 시작을 막지 않도록 백그라운드에서 확인한다
        applicationScope.launch { ensureReminderScheduled() }
    }

    // Worker에 의존성을 주입하려고 WorkManager 기본 초기화 대신 HiltWorkerFactory로 직접 초기화한다
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
