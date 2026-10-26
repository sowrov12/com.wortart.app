package com.wortart.app.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

/** Plays text aloud. Implemented by [TextToSpeechHelper]; can be faked in tests. */
interface SpeechEngine {
    val state: StateFlow<SpeechState>

    /** Speaks [text], replacing anything that is playing. [utteranceId] shows up in [SpeechState.speakingId]. */
    fun speak(text: String, utteranceId: String)

    fun stop()

    /** Releases the engine. Call once, when the screen is destroyed. */
    fun shutdown()
}

/**
 * German pronunciation with Android's built-in TextToSpeech. Works offline when the
 * device has a German voice installed. Create and use it on the main thread.
 */
class TextToSpeechHelper(context: Context) : SpeechEngine {

    private data class PendingSpeech(val text: String, val utteranceId: String)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(SpeechState())
    override val state: StateFlow<SpeechState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var pending: PendingSpeech? = null

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            _state.update { it.copy(speakingId = utteranceId) }
        }

        override fun onDone(utteranceId: String?) = clearIfCurrent(utteranceId)

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) = clearIfCurrent(utteranceId)

        override fun onError(utteranceId: String?, errorCode: Int) = clearIfCurrent(utteranceId)

        override fun onStop(utteranceId: String?, interrupted: Boolean) = clearIfCurrent(utteranceId)
    }

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            // Posted so that `tts` is always assigned before we use it.
            mainHandler.post { handleInit(status) }
        }
    }

    override fun speak(text: String, utteranceId: String) {
        when (_state.value.status) {
            SpeechStatus.INITIALIZING -> pending = PendingSpeech(text, utteranceId)
            SpeechStatus.READY -> {
                val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (result == TextToSpeech.ERROR) clearIfCurrent(utteranceId)
            }
            SpeechStatus.GERMAN_UNAVAILABLE, SpeechStatus.ERROR -> Unit
        }
    }

    override fun stop() {
        pending = null
        tts?.stop()
        _state.update { it.copy(speakingId = null) }
    }

    override fun shutdown() {
        pending = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        _state.update { it.copy(speakingId = null) }
    }

    private fun handleInit(status: Int) {
        val engine = tts
        if (engine == null) return // shutdown() was called while starting up

        if (status != TextToSpeech.SUCCESS) {
            _state.update { it.copy(status = SpeechStatus.ERROR) }
            return
        }
        if (!selectGerman(engine)) {
            _state.update { it.copy(status = SpeechStatus.GERMAN_UNAVAILABLE) }
            return
        }

        engine.setSpeechRate(SPEECH_RATE)
        engine.setOnUtteranceProgressListener(progressListener)
        _state.update { it.copy(status = SpeechStatus.READY) }

        pending?.let { request ->
            pending = null
            speak(request.text, request.utteranceId)
        }
    }

    private fun selectGerman(engine: TextToSpeech): Boolean {
        for (locale in listOf(Locale.GERMANY, Locale.GERMAN)) {
            val result = engine.setLanguage(locale)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                return true
            }
        }
        return false
    }

    private fun clearIfCurrent(utteranceId: String?) {
        _state.update { if (it.speakingId == utteranceId) it.copy(speakingId = null) else it }
    }

    private companion object {
        /** Slightly slower than normal, easier for learners to follow. */
        const val SPEECH_RATE = 0.9f
    }
}
