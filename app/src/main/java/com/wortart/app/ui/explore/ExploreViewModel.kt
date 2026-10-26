package com.wortart.app.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.repository.VocabFilter
import com.wortart.app.data.repository.VocabRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the Explore tab is showing. */
sealed interface ExploreDestination {
    /** The topic grid. */
    data object Topics : ExploreDestination

    /** The word list of one topic; [topic] = null means all topics. */
    data class Words(val topic: String?) : ExploreDestination
}

data class TopicSummary(val name: String, val wordCount: Int)

data class ExploreUiState(
    val destination: ExploreDestination = ExploreDestination.Topics,
    /** Selected CEFR level, or null for all levels. */
    val level: String? = null,
    /** All topics with the number of words that match the selected level. */
    val topics: List<TopicSummary> = emptyList(),
    /** Words of the open topic (empty while the grid is shown). */
    val words: List<VocabEntity> = emptyList(),
) {
    val totalWords: Int get() = topics.sumOf { it.wordCount }
}

/**
 * State holder for the Explore tab. The UI observes [uiState]; user actions are the
 * functions below. Task 3.2 adds live search to the same pipeline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModel(private val repository: VocabRepository) : ViewModel() {

    private val destination = MutableStateFlow<ExploreDestination>(ExploreDestination.Topics)
    private val level = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ExploreUiState> =
        combine(destination, level) { dest, lvl -> dest to lvl }
            .flatMapLatest { (dest, lvl) -> stateFor(dest, lvl) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())

    /** Opens the word list of [topic]; null opens all words. */
    fun openTopic(topic: String?) {
        destination.value = ExploreDestination.Words(topic)
    }

    fun closeTopic() {
        destination.value = ExploreDestination.Topics
    }

    /** Selects a level; selecting the active level again (or null) shows all levels. */
    fun selectLevel(newLevel: String?) {
        level.value = if (newLevel == level.value) null else newLevel
    }

    fun toggleFavorite(card: VocabEntity) {
        viewModelScope.launch { repository.setFavorite(card.id, !card.isFavorite) }
    }

    private fun stateFor(dest: ExploreDestination, lvl: String?): Flow<ExploreUiState> {
        val topicsFlow = combine(
            repository.observeTopics(),
            repository.observeWords(VocabFilter(level = lvl)),
        ) { names, wordsOfLevel ->
            val counts = wordsOfLevel.groupingBy { it.topic }.eachCount()
            sortTopics(names).map { TopicSummary(it, counts[it] ?: 0) }
        }

        val wordsFlow: Flow<List<VocabEntity>> = when (dest) {
            ExploreDestination.Topics -> flowOf(emptyList())
            is ExploreDestination.Words ->
                repository.observeWords(VocabFilter(level = lvl, topic = dest.topic))
        }

        return combine(topicsFlow, wordsFlow) { topics, words ->
            ExploreUiState(destination = dest, level = lvl, topics = topics, words = words)
        }
    }

    /** Featured topics first (Food, Career, Health), then the rest alphabetically. */
    private fun sortTopics(names: List<String>): List<String> =
        names.sortedWith(compareBy<String>({ featuredRank(it) }, { it.lowercase() }))

    private fun featuredRank(topic: String): Int {
        val index = FEATURED_TOPICS.indexOf(topic)
        return if (index < 0) FEATURED_TOPICS.size else index
    }

    companion object {
        private val FEATURED_TOPICS = listOf("Food", "Career", "Health")

        fun factory(repository: VocabRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ExploreViewModel(repository) }
        }
    }
}
