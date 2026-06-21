package com.passqr.util

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

/**
 * In-app language switcher for PassQR.
 *
 * Persists user's choice in [android.content.SharedPreferences] and applies it
 * to the activity context via [Configuration]. This is independent of the
 * system locale, matching the pattern used by ZeroCache / Morsify / SpecMD /
 * ExAPK / BypassDNS for cross-project consistency.
 */
object LocaleManager {

    private const val PREFS = "passqr_locale"
    private const val KEY = "language"
    const val LANG_INDONESIAN = "id"
    const val LANG_ENGLISH = "en"

    fun availableLanguages(): List<String> = listOf(LANG_ENGLISH, LANG_INDONESIAN)

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY, LANG_ENGLISH) ?: LANG_ENGLISH
    }

    fun setLanguage(context: Context, language: String) {
        val safe = if (language == LANG_INDONESIAN) LANG_INDONESIAN else LANG_ENGLISH
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY, safe) }
    }

    fun toggle(context: Context): String {
        val current = getLanguage(context)
        val next = if (current == LANG_INDONESIAN) LANG_ENGLISH else LANG_INDONESIAN
        setLanguage(context, next)
        return next
    }

    fun wrap(context: Context): Context {
        val lang = getLanguage(context)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
