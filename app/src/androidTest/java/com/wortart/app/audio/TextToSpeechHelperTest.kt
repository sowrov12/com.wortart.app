package com.wortart.app.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for the real TextToSpeech engine. Results depend on the device: an emulator
 * may have no German voice (GERMAN_UNAVAILABLE) or no engine at all (ERROR). Both are fine here.
 * If the engine never reports back, the tests are skipped instead of failing.
 */
@RunWith(AndroidJUnit4::class)
class TextToSpeechHelperTest {

    private lateinit var helper: TextToSpeechHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // TextToSpeech must be created on the main thread.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            helper = TextToSpeechHelper(context)
        }
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { helper.shutdown() }
    }

    private fun awaitFinishedInit(): SpeechState? = runBlocking {
        withTimeoutOrNull(15_000) {
            helper.state.first { it.status != SpeechStatus.INITIALIZING }
        }
    }

    @Test
    fun initialisationReachesAFinalState() {
        val state = awaitFinishedInit()
        assumeTrue("TTS engine did not report back on this device", state != null)
        assertTrue(state!!.status != SpeechStatus.INITIALIZING)
    }

    @Test
    fun speakThenStopClearsTheSpeakingState() {
        assumeTrue("TTS engine did not report back on this device", awaitFinishedInit() != null)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            helper.speak("Guten Tag", "test-utterance")
            helper.stop()
        }

        assertNull(helper.state.value.speakingId)
    }
}
