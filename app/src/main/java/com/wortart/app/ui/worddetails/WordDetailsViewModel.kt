package com.wortart.app.ui.worddetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.repository.VocabRepository
import com.wortart.app.domain.word.WordDetailsContent
import com.wortart.app.domain.word.buildWordDetails
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WordDetailsUiState(
    val isLoading: Boolean = true,
    /** The word, or null when it does not exist (after loading). */
    val card: VocabEntity? = null,
) {
    val content: WordDetailsContent? get() = card?.let(::buildWordDetails)
}

/** Loads one word and keeps its favorite state live. */
class WordDetailsViewModel(
    private val wordId: String,
    private val vocabulary: VocabRepository,
) : ViewModel() {

    val uiState: StateFlow<WordDetailsUiState> = combine(
        flow { emit(vocabulary.getWord(wordId)) },
        vocabulary.observeFavorites(),
    ) { word, favorites ->
        WordDetailsUiState(
            isLoading = false,
            card = word?.copy(isFavorite = favorites.any { it.id == wordId }),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WordDetailsUiState(),
    )

    fun toggleFavorite() {
        val card = uiState.value.card ?: return
        viewModelScope.launch { vocabulary.setFavorite(card.id, !card.isFavorite) }
    }

    companion object {
        fun factory(wordId: String, vocabulary: VocabRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { WordDetailsViewModel(wordId, vocabulary) } }
    }
}
