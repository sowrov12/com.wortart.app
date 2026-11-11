package com.wortart.app.domain.stats

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** The number of flashcards answered on one day (days are epoch days). */
data class DayActivity(val epochDay: Long, val answers: Int)

/** Pure helpers for the per-day answer history that feeds the weekly chart. */
object ActivityHistory {

    /** Older days are dropped so the stored history stays small. */
    const val KEEP_DAYS = 60

    /** Reads "19999:5,20000:3" into a map. Broken parts are skipped. */
    fun decode(text: String?): Map<Long, Int> {
        if (text.isNullOrBlank()) return emptyMap()
        val result = mutableMapOf<Long, Int>()
        for (part in text.split(',')) {
            val pieces = part.split(':')
            if (pieces.size != 2) continue
            val day = pieces[0].trim().toLongOrNull() ?: continue
            val count = pieces[1].trim().toIntOrNull() ?: continue
            if (count > 0) result[day] = count
        }
        return result
    }

    fun encode(history: Map<Long, Int>): String =
        history.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }

    /** Adds [count] answers to [day] and forgets days older than [KEEP_DAYS]. */
    fun add(history: Map<Long, Int>, day: Long, count: Int = 1): Map<Long, Int> {
        val updated = history + (day to ((history[day] ?: 0) + count))
        return updated.filterKeys { it > day - KEEP_DAYS }
    }

    /** The last [days] days ending today, oldest first; days without answers have 0. */
    fun lastDays(history: Map<Long, Int>, today: Long, days: Int = 7): List<DayActivity> =
        (days - 1 downTo 0).map { offset ->
            val day = today - offset
            DayActivity(day, history[day] ?: 0)
        }

    /** "M", "T", "W" ... for a chart label. */
    fun dayInitial(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)

    /** "Mon", "Tue" ... for screen readers. */
    fun dayName(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
}

/** Remembers how many cards were answered on each day. */
interface ActivityLog {
    fun recordAnswer(day: Long)
    fun lastDays(today: Long, days: Int = 7): List<DayActivity>
    fun clear()
}

/** Used where no history is kept (tests, previews). */
object NoActivityLog : ActivityLog {
    override fun recordAnswer(day: Long) = Unit
    override fun lastDays(today: Long, days: Int): List<DayActivity> =
        ActivityHistory.lastDays(emptyMap(), today, days)
    override fun clear() = Unit
}
