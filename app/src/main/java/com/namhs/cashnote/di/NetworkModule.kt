package com.namhs.cashnote.di

import com.namhs.cashnote.BuildConfig
import com.namhs.cashnote.data.remote.api.ExchangeRateApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * 환율 API 호출에 쓰는 Json, OkHttpClient, Retrofit, [ExchangeRateApi]를 제공한다.
 *
 * 로깅 인터셉터는 debug 빌드에서만 붙여 release에 요청·응답(API 키 포함)이 남지 않게 한다.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BASE_URL = "https://oapi.koreaexim.go.kr/"
    private const val TIMEOUT_SECONDS = 15L

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // API에 필드가 추가돼도 파싱이 깨지지 않게 모르는 키는 무시한다
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        createRetrofit(BASE_URL, okHttpClient, json)

    /** Retrofit 구성. 테스트에서 MockWebServer 주소로 같은 설정의 Retrofit을 만들 때도 쓴다 */
    internal fun createRetrofit(baseUrl: String, okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideExchangeRateApi(retrofit: Retrofit): ExchangeRateApi =
        retrofit.create(ExchangeRateApi::class.java)

    @Provides
    @ExchangeRateApiKey
    fun provideExchangeRateApiKey(): String = BuildConfig.EXCHANGE_RATE_API_KEY
}
