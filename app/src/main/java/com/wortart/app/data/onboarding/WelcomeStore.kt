package com.wortart.app.data.onboarding

import android.content.Context

/** Remembers whether the welcome screen was already shown, so it appears only on the first start. */
class WelcomeStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val hasSeenWelcome: Boolean
        get() = prefs.getBoolean(KEY_SEEN, false)

    fun markSeen() {
        prefs.edit().putBoolean(KEY_SEEN, true).apply()
    }

    private companion object {
        const val PREFS_NAME = "wortart_onboarding"
        const val KEY_SEEN = "welcome_seen"
    }
}
