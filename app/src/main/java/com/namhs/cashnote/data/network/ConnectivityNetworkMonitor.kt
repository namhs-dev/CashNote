package com.namhs.cashnote.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.namhs.cashnote.domain.repository.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * ConnectivityManager의 기본 네트워크 콜백으로 인터넷 연결 상태를 구독한다.
 *
 * 구독하는 동안에만 콜백을 등록하고, 구독이 끝나면 해제한다.
 */
class ConnectivityNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkMonitor {
    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        if (connectivityManager == null) {
            // 연결 상태를 알 수 없으면 오프라인 안내를 띄우지 않도록 온라인으로 본다
            trySend(true)
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(networkCapabilities.hasInternet())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        // 콜백은 연결이 있을 때만 바로 불리므로, 현재 상태를 먼저 보낸다
        trySend(connectivityManager.isCurrentlyOnline())
        connectivityManager.registerDefaultNetworkCallback(callback)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .conflate()

    private fun ConnectivityManager.isCurrentlyOnline(): Boolean =
        getNetworkCapabilities(activeNetwork)?.hasInternet() == true

    private fun NetworkCapabilities.hasInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
