package com.passqr.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val TAG = "PassQR.Network"

/**
 * Data class holding the current Wi-Fi connection info retrieved from the system.
 */
data class CurrentWifiInfo(
    val ssid: String,
    val security: String
)

/**
 * Retrieves the currently connected Wi-Fi network's SSID and security type
 * from the system. Returns null if not connected or info unavailable.
 *
 * Uses [NetworkCapabilities.getTransportInfo] on API 29+ to get [WifiInfo]
 * without needing deprecated WifiManager.connectionInfo.
 */
fun getCurrentWifiInfo(context: Context): CurrentWifiInfo? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return null
            val caps = cm.getNetworkCapabilities(network) ?: return null
            val wifiInfo = caps.transportInfo as? WifiInfo ?: return null

            val ssid = wifiInfo.ssid?.removeSurrounding("\"") ?: return null
            if (ssid == "<unknown ssid>" || ssid.isEmpty()) return null

            val security = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // SECURITY_TYPE constants on WifiInfo (API 30+):
                // 1=OPEN, 2=WEP, 3=PSK (WPA/WPA2), 4=EAP (WPA3-Enterprise),
                // 5=SAE (WPA3-Personal), 6=OWE, 7=WAPI_PSK, 8=WAPI_CERT
                when (wifiInfo.currentSecurityType) {
                    1 -> "nopass"   // SECURITY_TYPE_OPEN
                    2 -> "WEP"      // SECURITY_TYPE_WEP
                    else -> "WPA"   // PSK/EAP/SAE all map to WPA for QR
                }
            } else {
                "WPA"
            }

            Log.d(TAG, "getCurrentWifiInfo: ssid=$ssid, security=$security")
            CurrentWifiInfo(ssid = ssid, security = security)
        } else {
            @Suppress("DEPRECATION")
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val connectionInfo = wifiManager.connectionInfo ?: return null
            @Suppress("DEPRECATION")
            val ssid = connectionInfo.ssid?.removeSurrounding("\"") ?: return null
            if (ssid == "<unknown ssid>" || ssid.isEmpty()) return null

            Log.d(TAG, "getCurrentWifiInfo (legacy): ssid=$ssid")
            CurrentWifiInfo(ssid = ssid, security = "WPA")
        }
    } catch (e: Exception) {
        Log.e(TAG, "getCurrentWifiInfo failed", e)
        null
    }
}

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
