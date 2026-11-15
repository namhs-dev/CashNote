package com.namhs.cashnote.domain.usecase

import com.namhs.cashnote.domain.repository.NetworkMonitor
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 인터넷 연결 여부를 구독한다. 연결돼 있으면 true */
class ObserveNetworkStatusUseCase @Inject constructor(private val networkMonitor: NetworkMonitor) {
    operator fun invoke(): Flow<Boolean> = networkMonitor.isOnline
}
