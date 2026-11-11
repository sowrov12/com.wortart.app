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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A category (topic) with the number of words that match the other filters. */
data class TopicSummary(val name: String, val wordCount: Int)

data class ExploreUiState(
    /** Selected CEFR level, or null for all levels. */
    val level: String? = null,
    /** Selected category (topic), or null for all categories. */
    val topic: String? = null,
    val favoritesOnly: Boolean = false,
    /** The search text that is currently applied (trimmed, after the short typing delay). */
    val query: String = "",
    /** All categories with the number of words that match the level, favorites and search. */
    val topics: List<TopicSummary> = emptyList(),
    /** The words that match every filter. */
    val words: List<VocabEntity> = emptyList(),
    /** False until the first result has arrived. */
    val isLoaded: Boolean = false,
) {
    val hasFilters: Boolean
        get() = level != null || topic != null || favoritesOnly || query.isNotEmpty()
}

/**
 * State holder for the Explore tab. Search, level, category and favorites combine freely and can
 * be cleared one by one or all at once.
 *
 * [query] is the raw text of the search field and updates immediately. The list reacts to a
 * trimmed, debounced copy, so fast typing does not trigger a database query per keystroke, while
 * clearing the field is applied at once. Search covers German, English and Bengali.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ExploreViewModel(private val repository: VocabRepository) : ViewModel() {

    private data class Filters(
        val level: String?,
        val topic: String?,
        val favoritesOnly: Boolean,
        val query: String,
    )

    private val level = MutableStateFlow<String?>(null)
    private val topic = MutableStateFlow<String?>(null)
    private val favoritesOnly = MutableStateFlow(false)
    private val rawQuery = MutableStateFlow("")

    /** Text of the search field, exactly as typed. */
    val query: StateFlow<String> = rawQuery.asStateFlow()

    private val appliedQuery: Flow<String> = rawQuery
        .map { it.trim() }
        .debounce { text -> if (text.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
        .distinctUntilChanged()

    val uiState: StateFlow<ExploreUiState> =
        combine(level, topic, favoritesOnly, appliedQuery) { lvl, tpc, favs, text ->
            Filters(lvl, tpc, favs, text)
        }
            .flatMapLatest { filters -> stateFor(filters) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())

    fun onQueryChange(text: String) {
        rawQuery.value = text
    }

    fun clearQuery() {
        rawQuery.value = ""
    }

    /** Selects a level; selecting the active level again (or null) shows all levels. */
    fun selectLevel(newLevel: String?) {
        level.value = if (newLevel == level.value) null else newLevel
    }

    /** Selects a category; selecting the active one again (or null) shows all categories. */
    fun selectTopic(newTopic: String?) {
        topic.value = if (newTopic == topic.value) null else newTopic
    }

    fun toggleFavoritesOnly() {
        favoritesOnly.value = !favoritesOnly.value
    }

    /** Removes the search text and every filter. */
    fun clearFilters() {
        rawQuery.value = ""
        level.value = null
        topic.value = null
        favoritesOnly.value = false
    }

    /** Shows only the favorite words: clears every other filter and the search text. */
    fun showFavorites() {
        rawQuery.value = ""
        level.value = null
        topic.value = null
        favoritesOnly.value = true
    }

    fun toggleFavorite(card: VocabEntity) {
        viewModelScope.launch { repository.setFavorite(card.id, !card.isFavorite) }
    }

    private fun stateFor(filters: Filters): Flow<ExploreUiState> {
        // Category counts ignore the selected category, so every category shows how many
        // words it would give with the other filters.
        val topicsFlow = combine(
            repository.observeTopics(),
            repository.observeWords(
                VocabFilter(
                    query = filters.query,
                    level = filters.level,
                    favoritesOnly = filters.favoritesOnly,
                ),
            ),
        ) { names, matching ->
            val counts = matching.groupingBy { it.topic }.eachCount()
            sortTopics(names).map { TopicSummary(it, counts[it] ?: 0) }
        }

        val wordsFlow = repository.observeWords(
            VocabFilter(
                query = filters.query,
                level = filters.level,
                topic = filters.topic,
                favoritesOnly = filters.favoritesOnly,
            ),
        )

        return combine(topicsFlow, wordsFlow) { topics, words ->
            ExploreUiState(
                level = filters.level,
                topic = filters.topic,
                favoritesOnly = filters.favoritesOnly,
                query = filters.query,
                topics = topics,
                words = words,
                isLoaded = true,
            )
        }
    }

    /** Featured categories first (Food, Career, Health), then the rest alphabetically. */
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
