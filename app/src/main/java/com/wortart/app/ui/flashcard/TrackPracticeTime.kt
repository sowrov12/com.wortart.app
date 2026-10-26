package com.wortart.app.ui.flashcard

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Measures how long the learner practices. Time counts while this composable is on screen
 * and the app is in the foreground; it is reported when the app is paused or the composable
 * leaves the screen (for example when another tab opens).
 *
 * @param onSecondsPracticed called with the whole seconds of one practice stretch.
 */
@Composable
fun TrackPracticeTime(onSecondsPracticed: (Long) -> Unit) {
    val report by rememberUpdatedState(onSecondsPracticed)

    LifecycleResumeEffect(Unit) {
        val startedAt = SystemClock.elapsedRealtime()
        onPauseOrDispose {
            val seconds = (SystemClock.elapsedRealtime() - startedAt) / 1000L
            if (seconds > 0L) report(seconds)
        }
    }
}
