package com.wortart.app.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Flashcard and language choices that survive app restarts. Everything is on by default. */
class AppSettings(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val bengali = MutableStateFlow(prefs.getBoolean(KEY_BENGALI, true))
    private val readAloud = MutableStateFlow(prefs.getBoolean(KEY_READ_ALOUD, true))
    private val reminders = MutableStateFlow(prefs.getBoolean(KEY_REMINDERS, true))

    /** False hides every Bengali text in the whole app. */
    val showBengali: StateFlow<Boolean> = bengali.asStateFlow()

    /** True reads each flashcard aloud when it appears. */
    val readFlashcards: StateFlow<Boolean> = readAloud.asStateFlow()

    /** True sends a daily practice reminder. */
    val remindersOn: StateFlow<Boolean> = reminders.asStateFlow()

    fun setShowBengali(value: Boolean) {
        prefs.edit().putBoolean(KEY_BENGALI, value).apply()
        bengali.value = value
    }

    fun setReadFlashcards(value: Boolean) {
        prefs.edit().putBoolean(KEY_READ_ALOUD, value).apply()
        readAloud.value = value
    }

    fun setRemindersOn(value: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDERS, value).apply()
        reminders.value = value
    }

    private companion object {
        const val PREFS_NAME = "wortart_settings"
        const val KEY_BENGALI = "show_bengali"
        const val KEY_READ_ALOUD = "read_flashcards"
        const val KEY_REMINDERS = "reminders"
    }
}
