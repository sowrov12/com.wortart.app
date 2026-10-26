package com.wortart.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.wortart.app.ui.flashcard.FlashcardDeck
import com.wortart.app.ui.theme.WortArtColors
import com.wortart.app.ui.theme.WortArtTheme
import kotlinx.coroutines.launch

/**
 * Temporary host screen for the flashcard UI (Task 2.1).
 * Phase 3 replaces it with bottom navigation (Practice / Explore / Rules / Stats).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as WortArtApp).vocabRepository

        setContent {
            WortArtTheme {
                val wordsFlow = remember { repository.observeWords() }
                val words by wordsFlow.collectAsState(initial = emptyList())
                val scope = rememberCoroutineScope()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = WortArtColors.Background,
                ) {
                    FlashcardDeck(
                        words = words,
                        // Task 2.3 connects TextToSpeech here.
                        onSpeak = { },
                        onToggleFavorite = { card ->
                            scope.launch { repository.setFavorite(card.id, !card.isFavorite) }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding(),
                    )
                }
            }
        }
    }
}
