package com.namhs.cashnote

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

// Hilt 의존성 그래프의 시작점이 되는 Application 클래스
@HiltAndroidApp
class CashNoteApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Worker에 의존성을 주입하려고 WorkManager 기본 초기화 대신 HiltWorkerFactory로 직접 초기화한다
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
