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
