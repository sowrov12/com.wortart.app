package com.wortart.app.audio

enum class SpeechStatus {
    /** The speech engine is still starting up. */
    INITIALIZING,

    /** German speech is ready. */
    READY,

    /** An engine exists, but no German voice is installed. */
    GERMAN_UNAVAILABLE,

    /** No usable speech engine on this device. */
    ERROR,
}

/**
 * @param speakingId id of the utterance currently playing (the flashcard id), or null.
 */
data class SpeechState(
    val status: SpeechStatus = SpeechStatus.INITIALIZING,
    val speakingId: String? = null,
)

/** Whether pronunciation will work without a connection. */
enum class VoiceStatus { CHECKING, READY, MISSING }

/** READY once a German voice is installed; MISSING when there is no German voice or no speech engine. */
fun SpeechState.voiceStatus(): VoiceStatus = when (status) {
    SpeechStatus.INITIALIZING -> VoiceStatus.CHECKING
    SpeechStatus.READY -> VoiceStatus.READY
    SpeechStatus.GERMAN_UNAVAILABLE, SpeechStatus.ERROR -> VoiceStatus.MISSING
}

/** What the UI should do when the audio button of a card is tapped. */
enum class PronunciationAction { SPEAK, STOP, GERMAN_VOICE_MISSING, UNAVAILABLE }

/**
 * Tap on the audio button of card [cardId]:
 * - tapping the card that is playing stops it
 * - while the engine is still starting, speech is queued, so this also returns SPEAK
 */
fun SpeechState.actionFor(cardId: String): PronunciationAction = when {
    status == SpeechStatus.GERMAN_UNAVAILABLE -> PronunciationAction.GERMAN_VOICE_MISSING
    status == SpeechStatus.ERROR -> PronunciationAction.UNAVAILABLE
    speakingId == cardId -> PronunciationAction.STOP
    else -> PronunciationAction.SPEAK
}
