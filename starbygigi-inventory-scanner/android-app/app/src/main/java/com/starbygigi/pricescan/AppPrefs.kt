package com.starbygigi.pricescan

import android.content.Context

/**
 * Tiny wrapper around SharedPreferences for the Apps Script Web App URL + shared token.
 * These are entered once on-device via the settings dialog.
 */
class AppPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var webAppUrl: String?
        get() = prefs.getString(KEY_URL, null)
        set(value) = prefs.edit().putString(KEY_URL, value?.trim()).apply()

    var token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value.trim()).apply()

    val isConfigured: Boolean
        get() = !webAppUrl.isNullOrBlank() && token.isNotBlank()

    companion object {
        private const val PREFS_NAME = "starbygigi_prefs"
        private const val KEY_URL = "web_app_url"
        private const val KEY_TOKEN = "api_token"
    }
}
