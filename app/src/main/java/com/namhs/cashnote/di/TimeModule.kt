package com.namhs.cashnote.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId

/**
 * 현재 시각을 주는 [Clock]을 제공한다.
 *
 * 환율 기준일(평일 오전 11시 고시)은 한국 날짜로 정해지므로 기기 시간대와 상관없이 Asia/Seoul을 쓴다.
 * 테스트에서는 고정 시계를 넣는다.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun provideClock(): Clock = Clock.system(ZoneId.of("Asia/Seoul"))
}
