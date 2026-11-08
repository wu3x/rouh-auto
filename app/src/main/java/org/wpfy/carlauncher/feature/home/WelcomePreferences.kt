package org.wpfy.carlauncher.feature.home

import android.content.Context

/** Local-only startup greeting preferences; no location or account access required. */
object WelcomePreferences {
    private const val FILE = "welcome_preferences"
    private const val ENABLED = "enabled"
    private const val NAME = "name"

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(ENABLED, true)

    fun name(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(NAME, "").orEmpty()

    fun setEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, value).apply()
    }

    fun setName(context: Context, value: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(NAME, value.trim().take(30)).apply()
    }
}
