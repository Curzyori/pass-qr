package com.passqr.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure credential store backed by [EncryptedSharedPreferences].
 *
 * Stores the last successfully scanned/generated Wi-Fi credentials so the
 * Generator tab can retrieve them without manual input (PROMPT-2 §2).
 *
 * Keys: ssid, security, password
 */
class WifiCredentialsStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Save Wi-Fi credentials. */
    fun save(ssid: String, security: String, password: String) {
        prefs.edit()
            .putString(KEY_SSID, ssid)
            .putString(KEY_SECURITY, security)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    /** Retrieve saved SSID, or null if nothing stored. */
    fun getSsid(): String? = prefs.getString(KEY_SSID, null)

    /** Retrieve saved security type, defaults to "WPA". */
    fun getSecurity(): String = prefs.getString(KEY_SECURITY, "WPA") ?: "WPA"

    /** Retrieve saved password, or empty string if none. */
    fun getPassword(): String = prefs.getString(KEY_PASSWORD, "") ?: ""

    /** Whether any credentials have been saved. */
    fun hasCredentials(): Boolean = getSsid() != null

    /** Clear stored credentials. */
    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_FILE = "passqr_wifi_creds"
        private const val KEY_SSID = "ssid"
        private const val KEY_SECURITY = "security"
        private const val KEY_PASSWORD = "password"
    }
}
