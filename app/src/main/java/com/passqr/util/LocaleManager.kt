package com.passqr.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit

/**
 * In-app language switcher for PassQR.
 *
 * Persists user's choice in SharedPreferences and applies it via Configuration.
 * Supports English (default) and Indonesian.
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

    fun toggle(activity: Activity): String {
        val current = getLanguage(activity)
        val next = if (current == LANG_INDONESIAN) LANG_ENGLISH else LANG_INDONESIAN
        setLanguage(activity, next)
        activity.recreate()
        return next
    }

    fun getFlagEmoji(context: Context): String {
        return if (getLanguage(context) == LANG_INDONESIAN) "🇮🇩" else "🇬🇧"
    }

    fun wrap(context: Context): Context {
        val lang = getLanguage(context)
        val locale = java.util.Locale(lang)
        java.util.Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
