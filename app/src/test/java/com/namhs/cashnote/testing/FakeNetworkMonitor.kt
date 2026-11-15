package com.namhs.cashnote.testing

import com.namhs.cashnote.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow

/** 연결 상태를 테스트에서 직접 바꾸는 가짜 NetworkMonitor. 기본은 온라인이다 */
class FakeNetworkMonitor(initialOnline: Boolean = true) : NetworkMonitor {
    override val isOnline = MutableStateFlow(initialOnline)
}
