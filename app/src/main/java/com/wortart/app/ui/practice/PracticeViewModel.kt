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
import com.wortart.app.domain.question.ChoiceQuestionBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class PracticeStatus { LOADING, EMPTY, ACTIVE, FINISHED }

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
) : ViewModel() {

    private val state = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = state.asStateFlow()

    /** Opens the running session, or starts a new one if there is none. */
    fun startOrResume() {
        if (state.value.status == PracticeStatus.ACTIVE) return
        startNew()
    }

    fun startNew() {
        viewModelScope.launch {
            state.value = PracticeUiState(status = PracticeStatus.LOADING)
            val words = vocabulary.observeWords().first()
            val cards = words
                .shuffled(random)
                .mapNotNull { card ->
                    ChoiceQuestionBuilder.build(card, words)?.let { SessionCard(card, it) }
                }
                .take(sessionSize)

            state.value = PracticeUiState(
                status = if (cards.isEmpty()) PracticeStatus.EMPTY else PracticeStatus.ACTIVE,
                cards = cards,
            )
        }
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

    /** Moves to the next card, or finishes the session after the last one. */
    fun next() {
        state.update { current ->
            when {
                current.status != PracticeStatus.ACTIVE || !current.submitted -> current
                current.isLastCard -> current.copy(status = PracticeStatus.FINISHED)
                else -> current.copy(index = current.index + 1, selected = null, submitted = false)
            }
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

        fun factory(vocabulary: VocabRepository, stats: StatsRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { PracticeViewModel(vocabulary, stats) } }
    }
}
