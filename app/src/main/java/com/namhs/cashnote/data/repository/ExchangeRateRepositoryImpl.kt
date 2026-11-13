package com.namhs.cashnote.data.repository

import com.namhs.cashnote.data.local.dao.ExchangeRateDao
import com.namhs.cashnote.data.remote.ExchangeRateRemoteDataSource
import com.namhs.cashnote.data.repository.mapper.toDomain
import com.namhs.cashnote.data.repository.mapper.toEntityOrNull
import com.namhs.cashnote.domain.model.ExchangeRate
import com.namhs.cashnote.domain.repository.ExchangeRateRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room을 유일한 데이터 소스로 두는 환율 Repository.
 *
 * 화면은 [observeExchangeRates]로 Room만 구독하고, 네트워크 결과는 [refresh]에서 Room 갱신에만 쓴다.
 */
class ExchangeRateRepositoryImpl @Inject constructor(
    private val exchangeRateDao: ExchangeRateDao,
    private val remoteDataSource: ExchangeRateRemoteDataSource,
    private val clock: Clock
) : ExchangeRateRepository {
    override fun observeExchangeRates(): Flow<List<ExchangeRate>> =
        exchangeRateDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refresh() {
        // 조회 기간 안에 고시된 환율이 없으면 기존 캐시를 그대로 둔다
        val fetched = remoteDataSource.fetchLatest(LocalDate.now(clock)) ?: return
        val updatedAt = clock.instant()
        val entities = fetched.rates.mapNotNull { it.toEntityOrNull(fetched.date, updatedAt) }
        // 쓸 수 있는 항목이 하나도 없으면 캐시를 비우지 않는다
        if (entities.isEmpty()) return
        exchangeRateDao.replaceAll(entities)
    }
}
