package com.wortart.app.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wortart.app.data.local.entity.VocabEntity
import com.wortart.app.data.repository.StatsRepository
import com.wortart.app.data.repository.VocabRepository
import com.wortart.app.domain.question.ChoiceQuestion
import com.wortart.app.data.local.entity.groupLabels
import com.wortart.app.data.local.entity.kindKey
import com.wortart.app.data.local.entity.WordGroups
import com.wortart.app.domain.question.ChoiceQuestionBuilder
import com.wortart.app.domain.question.PracticeSets
import com.wortart.app.domain.question.QuestionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class PracticeStatus { LOADING, EMPTY, ACTIVE, FINISHED, SETUP }

/**
 * What the learner chose on the setup screen.
 *
 * @param group category filter (a label from WordGroups), or null for all words.
 * @param type one specific card set, or null.
 * @param mixed true to give every card a random set that fits it (used when [type] is null).
 * @param levels CEFR levels to practice; empty means all levels.
 */
data class PracticeConfig(
    val group: String? = null,
    val type: QuestionType? = null,
    val mixed: Boolean = false,
    val levels: Set<String> = emptySet(),
)

/** Number of words per category, for the category page. */
data class GroupCounts(
    val total: Int = 0,
    val groups: List<Pair<String, Int>> = emptyList(),
    val favorites: Int = 0,
)

/** A flashcard together with its question. */
data class SessionCard(val card: VocabEntity, val question: ChoiceQuestion)

data class PracticeUiState(
    val status: PracticeStatus = PracticeStatus.LOADING,
    val cards: List<SessionCard> = emptyList(),
    /** Position of the current card, starting at 0. */
    val index: Int = 0,
    /** The option marked by the learner (not yet submitted unless [submitted]). */
    val selected: String? = null,
    val submitted: Boolean = false,
    val correctCount: Int = 0,
    /** The choices of the setup screen (also kept during the session). */
    val config: PracticeConfig = PracticeConfig(),
    /** Groups that exist in the vocabulary, for the setup screen. */
    val presentGroups: List<String> = emptyList(),
    /** Number of words that match the setup choices. */
    val availableCount: Int = 0,
    /** True when cards never run out (no counter, no summary). */
    val endless: Boolean = false,
) {
    val total: Int get() = cards.size
    val current: SessionCard? get() = cards.getOrNull(index)
    val isLastCard: Boolean get() = index == cards.lastIndex

    /** "3 / 20" reads as 15 % filled. */
    val progress: Float get() = if (total == 0) 0f else (index + 1).toFloat() / total

    val isCorrect: Boolean
        get() = submitted && selected != null && selected == current?.question?.correctAnswer
}

/**
 * A flashcard session: a fixed number of random words, each with a question. The learner
 * marks an answer, submits it, sees the result and moves on. Every submitted answer is counted
 * in the statistics.
 */
class PracticeViewModel(
    private val vocabulary: VocabRepository,
    private val stats: StatsRepository,
    private val sessionSize: Int = SESSION_SIZE,
    private val random: Random = Random.Default,
    initialConfig: PracticeConfig = PracticeConfig(),
    /** True: cards keep coming at random, there is no fixed session length. */
    private val endless: Boolean = false,
) : ViewModel() {

    private var config: PracticeConfig = initialConfig
    private var allWords: List<VocabEntity> = emptyList()

    private val state = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = state.asStateFlow()

    /** Opens the running session, or starts a new one if there is none. */
    fun startOrResume() {
        if (state.value.status == PracticeStatus.ACTIVE) return
        startNew()
    }

    fun startNew() {
        viewModelScope.launch {
            state.value = PracticeUiState(status = PracticeStatus.LOADING, config = config, endless = endless)
            val words = vocabulary.observeWords().first()
            val cards = buildSession(words, config)

            state.value = PracticeUiState(
                status = if (cards.isEmpty()) PracticeStatus.EMPTY else PracticeStatus.ACTIVE,
                cards = cards,
                config = config,
                endless = endless,
            )
        }
    }

    private fun matches(card: VocabEntity, cfg: PracticeConfig): Boolean {
        val group = cfg.group
        return (cfg.levels.isEmpty() || card.level in cfg.levels) &&
            (
                group == null ||
                    (group == WordGroups.FAVORITES && card.isFavorite) ||
                    group in card.groupLabels()
                )
    }

    private fun buildSession(words: List<VocabEntity>, cfg: PracticeConfig): List<SessionCard> {
        val pools = words.groupBy { it.kindKey() }
        val type = cfg.type
        return words
            .filter { matches(it, cfg) }
            .shuffled(random)
            .asSequence()
            .mapNotNull { card ->
                val pool = pools[card.kindKey()].orEmpty()
                val question = when {
                    type != null -> ChoiceQuestionBuilder.buildSet(card, type, pool)
                    cfg.mixed -> ChoiceQuestionBuilder.buildMixed(card, pool, random)
                    else -> ChoiceQuestionBuilder.build(card, words)
                }
                question?.let { SessionCard(card, it) }
            }
            .take(sessionSize)
            .toList()
    }

    /** Opens the setup screen where category, card set and levels are chosen. */
    fun openSetup() {
        viewModelScope.launch {
            allWords = vocabulary.observeWords().first()
            if (config.type == null && !config.mixed) config = config.copy(mixed = true)
            publishSetup()
        }
    }

    private fun publishSetup() {
        val present = WordGroups.ORDER.filter { group -> allWords.any { group in it.groupLabels() } }
        state.value = PracticeUiState(
            status = PracticeStatus.SETUP,
            config = config,
            presentGroups = present,
            availableCount = allWords.count { matches(it, config) },
        )
    }

    fun selectGroup(group: String?) {
        val type = config.type
        val keepType = type == null || type in PracticeSets.forGroup(group)
        config = if (keepType) {
            config.copy(group = group)
        } else {
            config.copy(group = group, type = null, mixed = true)
        }
        publishSetup()
    }

    /** Chooses one card set; null chooses "Mixed". */
    fun selectSet(type: QuestionType?) {
        config = if (type == null) config.copy(type = null, mixed = true) else config.copy(type = type, mixed = false)
        publishSetup()
    }

    fun toggleLevel(level: String) {
        val levels = if (level in config.levels) config.levels - level else config.levels + level
        config = config.copy(levels = levels)
        publishSetup()
    }

    fun clearLevels() {
        config = config.copy(levels = emptySet())
        publishSetup()
    }

    /** Marks an option. Ignored once the answer was submitted. */
    fun select(option: String) {
        state.update { current ->
            if (current.status == PracticeStatus.ACTIVE && !current.submitted) {
                current.copy(selected = option)
            } else {
                current
            }
        }
    }

    /** Checks the marked option. Does nothing if no option is marked or it was submitted already. */
    fun submit() {
        val current = state.value
        val question = current.current?.question ?: return
        val choice = current.selected ?: return
        if (current.status != PracticeStatus.ACTIVE || current.submitted) return

        val correct = choice == question.correctAnswer
        state.value = current.copy(
            submitted = true,
            correctCount = current.correctCount + if (correct) 1 else 0,
        )
        viewModelScope.launch { stats.recordAnswer(correct) }
    }

    /** Moves to the next card, or finishes the session after the last one (never in endless mode). */
    fun next() = advance(requireSubmitted = true)

    /** Skips the current card without answering it. */
    fun skip() = advance(requireSubmitted = false)

    private fun advance(requireSubmitted: Boolean) {
        val current = state.value
        if (current.status != PracticeStatus.ACTIVE) return
        if (requireSubmitted && !current.submitted) return

        if (!current.isLastCard) {
            state.value = current.copy(index = current.index + 1, selected = null, submitted = false)
            return
        }
        if (!endless) {
            if (requireSubmitted) state.value = current.copy(status = PracticeStatus.FINISHED)
            return
        }
        // Endless: a fresh batch of random cards.
        viewModelScope.launch {
            val cards = buildSession(vocabulary.observeWords().first(), config)
            if (cards.isNotEmpty()) {
                state.value = state.value.copy(cards = cards, index = 0, selected = null, submitted = false)
            }
        }
    }

    // ---- choices made while practicing (the session restarts with the new choice)

    fun toggleLevelLive(level: String) {
        val levels = if (level in config.levels) config.levels - level else config.levels + level
        config = config.copy(levels = levels)
        startNew()
    }

    fun clearLevelsLive() {
        config = config.copy(levels = emptySet())
        startNew()
    }

    fun chooseGroup(group: String?) {
        val type = config.type
        val keepType = type == null || type in PracticeSets.forGroup(group)
        config = if (keepType) {
            config.copy(group = group)
        } else {
            config.copy(group = group, type = null, mixed = true)
        }
        startNew()
    }

    /** Chooses the card set while practicing; null chooses "Mixed". */
    fun chooseSetLive(type: QuestionType?) {
        config = if (type == null) config.copy(type = null, mixed = true) else config.copy(type = type, mixed = false)
        startNew()
    }

    private val groupCountsState = MutableStateFlow(GroupCounts())

    /** How many words each category has (for the category page). */
    val groupCounts: StateFlow<GroupCounts> = groupCountsState.asStateFlow()

    fun loadGroupCounts() {
        viewModelScope.launch {
            val levels = config.levels
            val words = vocabulary.observeWords().first()
                .filter { levels.isEmpty() || it.level in levels }
            groupCountsState.value = GroupCounts(
                total = words.size,
                favorites = words.count { it.isFavorite },
                groups = WordGroups.ORDER
                    .map { group -> group to words.count { group in it.groupLabels() } }
                    .filter { it.second > 0 },
            )
        }
    }

    fun toggleFavorite() {
        val current = state.value
        val session = current.current ?: return
        val newValue = !session.card.isFavorite

        val updated = session.copy(card = session.card.copy(isFavorite = newValue))
        state.value = current.copy(
            cards = current.cards.toMutableList().also { it[current.index] = updated },
        )
        viewModelScope.launch { vocabulary.setFavorite(session.card.id, newValue) }
    }

    companion object {
        const val SESSION_SIZE = 20
        const val ENDLESS_BATCH = 30

        fun factory(vocabulary: VocabRepository, stats: StatsRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    PracticeViewModel(
                        vocabulary,
                        stats,
                        sessionSize = ENDLESS_BATCH,
                        initialConfig = PracticeConfig(mixed = true),
                        endless = true,
                    )
                }
            }
    }
}
