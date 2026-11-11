package com.wortart.app.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceStatusTest {

    @Test
    fun theVoiceIsBeingCheckedWhileTheEngineStarts() {
        assertEquals(VoiceStatus.CHECKING, SpeechState(SpeechStatus.INITIALIZING).voiceStatus())
    }

    @Test
    fun aReadyEngineMeansOfflinePronunciationWorks() {
        assertEquals(VoiceStatus.READY, SpeechState(SpeechStatus.READY).voiceStatus())
        assertEquals(VoiceStatus.READY, SpeechState(SpeechStatus.READY, speakingId = "c1").voiceStatus())
    }

    @Test
    fun aMissingVoiceOrEngineIsReportedAsMissing() {
        assertEquals(VoiceStatus.MISSING, SpeechState(SpeechStatus.GERMAN_UNAVAILABLE).voiceStatus())
        assertEquals(VoiceStatus.MISSING, SpeechState(SpeechStatus.ERROR).voiceStatus())
    }
}
