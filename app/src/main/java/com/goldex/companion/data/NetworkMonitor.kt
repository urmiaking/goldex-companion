package com.goldex.companion.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionStatus {
    ONLINE,      // سبز: متصل و پایدار
    CONNECTING,  // زرد: در حال ارتباط / همگام‌سازی نرخ‌ها
    OFFLINE      // قرمز: قطع اتصال اینترنت
}

class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _status = MutableStateFlow(checkInitialStatus())
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _status.value = ConnectionStatus.ONLINE
                }

                override fun onLost(network: Network) {
                    _status.value = ConnectionStatus.OFFLINE
                }

                override fun onUnavailable() {
                    _status.value = ConnectionStatus.OFFLINE
                }
            })
        } catch (e: Exception) {
            _status.value = checkInitialStatus()
        }
    }

    fun checkInitialStatus(): ConnectionStatus {
        return try {
            val cm = connectivityManager ?: return ConnectionStatus.OFFLINE
            val activeNetwork = cm.activeNetwork ?: return ConnectionStatus.OFFLINE
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return ConnectionStatus.OFFLINE
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                ConnectionStatus.ONLINE
            } else {
                ConnectionStatus.OFFLINE
            }
        } catch (e: Exception) {
            ConnectionStatus.ONLINE
        }
    }
}
