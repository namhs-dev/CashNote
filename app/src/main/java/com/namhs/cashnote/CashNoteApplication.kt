package com.namhs.cashnote

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Hilt 의존성 그래프의 시작점이 되는 Application 클래스
@HiltAndroidApp
class CashNoteApplication : Application()