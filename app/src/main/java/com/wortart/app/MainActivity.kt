package com.wortart.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wortart.app.audio.PronunciationAction
import com.wortart.app.audio.TextToSpeechHelper
import com.wortart.app.audio.actionFor
import com.wortart.app.ui.MainScreen
import com.wortart.app.ui.explore.ExploreRoute
import com.wortart.app.ui.explore.ExploreViewModel
import com.wortart.app.ui.flashcard.FlashcardDeck
import com.wortart.app.ui.rules.GrammarRulesRoute
import com.wortart.app.ui.rules.RulesViewModel
import com.wortart.app.ui.theme.WortArtTheme
import kotlinx.coroutines.launch

/** Hosts the app shell: Practice (flashcards), Explore and Rules tabs. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as WortArtApp).vocabRepository

        setContent {
            WortArtTheme {
                val wordsFlow = remember { repository.observeWords() }
                val words by wordsFlow.collectAsState(initial = emptyList())
                val scope = rememberCoroutineScope()

                val speech = remember { TextToSpeechHelper(applicationContext) }
                DisposableEffect(speech) { onDispose { speech.shutdown() } }
                val speechState by speech.state.collectAsState()

                val exploreViewModel: ExploreViewModel =
                    viewModel(factory = ExploreViewModel.factory(repository))
                val rulesViewModel: RulesViewModel =
                    viewModel(factory = RulesViewModel.factory(repository))

                MainScreen(
                    practice = {
                        FlashcardDeck(
                            words = words,
                            speakingCardId = speechState.speakingId,
                            onSpeak = { card ->
                                when (speechState.actionFor(card.id)) {
                                    PronunciationAction.SPEAK -> speech.speak(card.word, card.id)
                                    PronunciationAction.STOP -> speech.stop()
                                    PronunciationAction.GERMAN_VOICE_MISSING -> toast(
                                        "German voice is not installed. " +
                                            "Add it in Settings › Text-to-speech output.",
                                    )
                                    PronunciationAction.UNAVAILABLE -> toast(
                                        "Text-to-speech is not available on this device.",
                                    )
                                }
                            },
                            onToggleFavorite = { card ->
                                scope.launch { repository.setFavorite(card.id, !card.isFavorite) }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                    explore = {
                        ExploreRoute(viewModel = exploreViewModel, modifier = Modifier.fillMaxSize())
                    },
                    rules = {
                        GrammarRulesRoute(viewModel = rulesViewModel, modifier = Modifier.fillMaxSize())
                    },
                )
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
