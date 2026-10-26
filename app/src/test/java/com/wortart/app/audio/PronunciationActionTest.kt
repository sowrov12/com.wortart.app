package com.wortart.app.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class PronunciationActionTest {

    @Test
    fun readyAndIdleSpeaks() {
        val state = SpeechState(SpeechStatus.READY, speakingId = null)
        assertEquals(PronunciationAction.SPEAK, state.actionFor("c1"))
    }

    @Test
    fun speechIsQueuedWhileEngineStarts() {
        val state = SpeechState(SpeechStatus.INITIALIZING)
        assertEquals(PronunciationAction.SPEAK, state.actionFor("c1"))
    }

    @Test
    fun tappingThePlayingCardStopsIt() {
        val state = SpeechState(SpeechStatus.READY, speakingId = "c1")
        assertEquals(PronunciationAction.STOP, state.actionFor("c1"))
    }

    @Test
    fun tappingAnotherCardWhilePlayingSpeaksIt() {
        val state = SpeechState(SpeechStatus.READY, speakingId = "c1")
        assertEquals(PronunciationAction.SPEAK, state.actionFor("c2"))
    }

    @Test
    fun missingGermanVoiceIsReported() {
        val state = SpeechState(SpeechStatus.GERMAN_UNAVAILABLE)
        assertEquals(PronunciationAction.GERMAN_VOICE_MISSING, state.actionFor("c1"))
    }

    @Test
    fun missingEngineIsReported() {
        val state = SpeechState(SpeechStatus.ERROR)
        assertEquals(PronunciationAction.UNAVAILABLE, state.actionFor("c1"))
    }
}
