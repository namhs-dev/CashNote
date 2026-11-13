package com.namhs.cashnote.di

import javax.inject.Qualifier

/** 환율 API 인증키(String) 주입용 Qualifier. 값은 BuildConfig에서 온다 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ExchangeRateApiKey
