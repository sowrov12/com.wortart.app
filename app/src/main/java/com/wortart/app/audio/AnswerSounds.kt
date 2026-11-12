package com.wortart.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.wortart.app.R

/** Short sounds for a correct and a wrong flashcard answer. */
class AnswerSounds(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val correctId: Int = pool.load(context, R.raw.correct_answer, 1)
    private val wrongId: Int = pool.load(context, R.raw.wrong_answer, 1)

    fun play(correct: Boolean) {
        pool.play(if (correct) correctId else wrongId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }
}
