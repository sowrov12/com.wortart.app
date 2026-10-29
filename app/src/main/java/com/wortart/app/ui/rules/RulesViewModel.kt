package com.wortart.app.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wortart.app.data.lessons.LessonsRepository
import com.wortart.app.data.local.entity.WordCategories
import com.wortart.app.data.repository.VocabFilter
import com.wortart.app.data.repository.VocabRepository
import com.wortart.app.domain.rules.RuleSection
import com.wortart.app.domain.rules.grammarRuleSections
import com.wortart.app.domain.rules.lessons.Lesson
import com.wortart.app.domain.rules.lessons.LessonsFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LessonsStatus { LOADING, READY, ERROR }

/** State of the "Lessons" part of the Rules tab. */
data class LessonsUiState(
    val status: LessonsStatus = LessonsStatus.LOADING,
    val file: LessonsFile? = null,
    val error: String? = null,
    /** Selected CEFR level, or null for all levels. */
    val level: String? = null,
    /** Quiz answers already given: key "lessonId:quizIndex" to the chosen option. */
    val answers: Map<String, String> = emptyMap(),
) {
    /** The lessons that match the selected level. */
    val lessons: List<Lesson>
        get() = file?.sections.orEmpty().filter { level == null || it.level == level }
}

/**
 * Provides both parts of the Rules tab:
 * - [lessons]: the bilingual grammar lessons from assets/grammar_rules.json, with a level
 *   filter and quiz answers that are kept while the app is open,
 * - [sections]: the reference tables, whose irregular-verb and adjective tables are filled
 *   from the vocabulary database.
 */
class RulesViewModel(
    repository: VocabRepository,
    private val lessonsRepository: LessonsRepository,
) : ViewModel() {

    val sections: StateFlow<List<RuleSection>> = combine(
        repository.observeWords(VocabFilter(category = WordCategories.IRREGULAR_VERB)),
        repository.observeWords(VocabFilter(category = WordCategories.ADJECTIVE)),
    ) { irregularVerbs, adjectives ->
        grammarRuleSections(irregularVerbs, adjectives)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = grammarRuleSections(emptyList(), emptyList()),
    )

    private val loaded = MutableStateFlow<Result<LessonsFile>?>(null)
    private val level = MutableStateFlow<String?>(null)
    private val answers = MutableStateFlow<Map<String, String>>(emptyMap())

    val lessons: StateFlow<LessonsUiState> = combine(loaded, level, answers) { result, selectedLevel, given ->
        when {
            result == null -> LessonsUiState(level = selectedLevel, answers = given)
            result.isSuccess -> LessonsUiState(
                status = LessonsStatus.READY,
                file = result.getOrNull(),
                level = selectedLevel,
                answers = given,
            )
            else -> LessonsUiState(
                status = LessonsStatus.ERROR,
                error = result.exceptionOrNull()?.message ?: "Could not load the lessons.",
                level = selectedLevel,
                answers = given,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LessonsUiState(),
    )

    init {
        load()
    }

    /** Loads the lessons (also used by the "Try again" button). */
    fun load() {
        viewModelScope.launch {
            loaded.value = null
            loaded.value = lessonsRepository.load()
        }
    }

    /** Selects a level; selecting the active level again (or null) shows all levels. */
    fun selectLevel(newLevel: String?) {
        level.value = if (newLevel == level.value) null else newLevel
    }

    /** Records a quiz answer. Each question can be answered once. */
    fun answerQuiz(key: String, option: String) {
        answers.update { current -> if (key in current) current else current + (key to option) }
    }

    companion object {
        fun factory(
            repository: VocabRepository,
            lessonsRepository: LessonsRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { RulesViewModel(repository, lessonsRepository) }
        }
    }
}
