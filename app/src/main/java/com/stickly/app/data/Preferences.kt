package com.stickly.app.data

import android.content.Context

/**
 * Small, app-private settings store. The Telegram token never leaves the device
 * except for requests made directly to api.telegram.org.
 */
class Preferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("stickly_prefs", Context.MODE_PRIVATE)

    var botToken: String
        get() = prefs.getString(KEY_BOT_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_BOT_TOKEN, value.trim()).apply()

    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()

    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM)
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    var lastUrl: String
        get() = prefs.getString(KEY_LAST_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_LAST_URL, value.trim()).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_USER_NAME, value.trim()).apply()

    var hasSeenWelcome: Boolean
        get() = prefs.getBoolean(KEY_HAS_SEEN_WELCOME, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_SEEN_WELCOME, value).apply()

    var lastPackName: String
        get() = prefs.getString(KEY_LAST_PACK_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_LAST_PACK_NAME, value.trim()).apply()

    fun clearAll() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_BOT_TOKEN = "bot_token"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_LAST_URL = "last_url"
        const val KEY_USER_NAME = "user_name"
        const val KEY_HAS_SEEN_WELCOME = "has_seen_welcome"
        const val KEY_LAST_PACK_NAME = "last_pack_name"
    }
}
