package com.wortart.app.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wortart.app.data.local.entity.WordCategories
import com.wortart.app.data.repository.VocabFilter
import com.wortart.app.data.repository.VocabRepository
import com.wortart.app.domain.rules.RuleSection
import com.wortart.app.domain.rules.grammarRuleSections
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Provides the grammar rule sections. The irregular-verb and adjective tables are filled
 * from the vocabulary database and update automatically when it changes.
 */
class RulesViewModel(repository: VocabRepository) : ViewModel() {

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

    companion object {
        fun factory(repository: VocabRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { RulesViewModel(repository) }
        }
    }
}
