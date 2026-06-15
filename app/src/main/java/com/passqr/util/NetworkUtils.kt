package com.passqr.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val TAG = "PassQR.Network"

/**
 * Checks whether the device currently has an active Wi-Fi connection.
 */
fun isWifiConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network: Network = cm.activeNetwork ?: run {
        Log.d(TAG, "isWifiConnected: no active network")
        return false
    }
    val caps = cm.getNetworkCapabilities(network) ?: run {
        Log.d(TAG, "isWifiConnected: no capabilities for active network")
        return false
    }
    val result = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    Log.d(TAG, "isWifiConnected: $result")
    return result
}

/**
 * Composable that observes Wi-Fi connectivity in real time via
 * [ConnectivityManager.NetworkCallback] and returns a reactive [State].
 *
 * PROMPT-3 §3: Added explicit [Log] statements for debugging the
 * connectivityManager callback flow.
 */
@Composable
fun rememberWifiConnected(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(isWifiConnected(context)) }

    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        Log.d(TAG, "registerNetworkCallback: listening for Wi-Fi changes")

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "onAvailable: network=$network")
                state.value = true
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "onLost: network=$network")
                state.value = isWifiConnected(context)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                Log.d(TAG, "onCapabilitiesChanged: hasWifi=$hasWifi")
                state.value = hasWifi
            }
        }

        cm.registerNetworkCallback(request, callback)
        onDispose {
            Log.d(TAG, "unregisterNetworkCallback")
            cm.unregisterNetworkCallback(callback)
        }
    }

    return state
}
