package com.wortart.app.data.stats

import android.content.Context
import com.wortart.app.domain.stats.ActivityHistory
import com.wortart.app.domain.stats.ActivityLog
import com.wortart.app.domain.stats.DayActivity

/** Keeps the per-day answer counts in SharedPreferences (small and independent of the database). */
class PrefsActivityLog(context: Context) : ActivityLog {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun recordAnswer(day: Long) {
        val updated = ActivityHistory.add(ActivityHistory.decode(prefs.getString(KEY, null)), day)
        prefs.edit().putString(KEY, ActivityHistory.encode(updated)).apply()
    }

    @Synchronized
    override fun lastDays(today: Long, days: Int): List<DayActivity> =
        ActivityHistory.lastDays(ActivityHistory.decode(prefs.getString(KEY, null)), today, days)

    @Synchronized
    override fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val PREFS_NAME = "wortart_activity"
        const val KEY = "answers_by_day"
    }
}
