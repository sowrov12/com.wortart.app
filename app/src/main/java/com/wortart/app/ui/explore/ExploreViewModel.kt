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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    /** The search text that is currently applied (trimmed, after the short typing delay). */
    val query: String = "",
    /** All topics with the number of words that match the selected level. */
    val topics: List<TopicSummary> = emptyList(),
    /**
     * Words to list: search results while [isSearching], otherwise the words of the open
     * topic. Empty while the topic grid is shown.
     */
    val words: List<VocabEntity> = emptyList(),
) {
    val isSearching: Boolean get() = query.isNotEmpty()
    val totalWords: Int get() = topics.sumOf { it.wordCount }
}

/**
 * State holder for the Explore tab.
 *
 * Search: [query] is the raw text of the search field and updates immediately. The word
 * list reacts to a trimmed, debounced copy of it, so fast typing does not trigger a database
 * query per keystroke, while clearing the field is applied at once. Search covers German,
 * English and Bengali text, respects the level chips and, when a topic is open, stays inside
 * that topic.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ExploreViewModel(private val repository: VocabRepository) : ViewModel() {

    private val destination = MutableStateFlow<ExploreDestination>(ExploreDestination.Topics)
    private val level = MutableStateFlow<String?>(null)
    private val rawQuery = MutableStateFlow("")

    /** Text of the search field, exactly as typed. */
    val query: StateFlow<String> = rawQuery.asStateFlow()

    private val appliedQuery: Flow<String> = rawQuery
        .map { it.trim() }
        .debounce { text -> if (text.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
        .distinctUntilChanged()

    val uiState: StateFlow<ExploreUiState> =
        combine(destination, level, appliedQuery) { dest, lvl, text -> Triple(dest, lvl, text) }
            .flatMapLatest { (dest, lvl, text) -> stateFor(dest, lvl, text) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())

    fun onQueryChange(text: String) {
        rawQuery.value = text
    }

    fun clearQuery() {
        rawQuery.value = ""
    }

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

    private fun stateFor(dest: ExploreDestination, lvl: String?, text: String): Flow<ExploreUiState> {
        val topicsFlow = combine(
            repository.observeTopics(),
            repository.observeWords(VocabFilter(level = lvl)),
        ) { names, wordsOfLevel ->
            val counts = wordsOfLevel.groupingBy { it.topic }.eachCount()
            sortTopics(names).map { TopicSummary(it, counts[it] ?: 0) }
        }

        val openTopic = (dest as? ExploreDestination.Words)?.topic
        val wordsFlow: Flow<List<VocabEntity>> = when {
            text.isNotEmpty() ->
                repository.observeWords(VocabFilter(query = text, level = lvl, topic = openTopic))
            dest is ExploreDestination.Words ->
                repository.observeWords(VocabFilter(level = lvl, topic = dest.topic))
            else -> flowOf(emptyList())
        }

        return combine(topicsFlow, wordsFlow) { topics, words ->
            ExploreUiState(destination = dest, level = lvl, query = text, topics = topics, words = words)
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
        /** How long typing must pause before the search runs. */
        const val SEARCH_DEBOUNCE_MS = 150L

        private val FEATURED_TOPICS = listOf("Food", "Career", "Health")

        fun factory(repository: VocabRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ExploreViewModel(repository) }
        }
    }
}
