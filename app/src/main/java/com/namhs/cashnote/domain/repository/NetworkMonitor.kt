package com.namhs.cashnote.domain.repository

import kotlinx.coroutines.flow.Flow

/** 기기의 인터넷 연결 상태를 알려 준다 */
interface NetworkMonitor {
    /** 인터넷에 연결돼 있으면 true. 구독하면 현재 상태를 먼저 내보내고, 바뀔 때마다 다시 내보낸다 */
    val isOnline: Flow<Boolean>
}
