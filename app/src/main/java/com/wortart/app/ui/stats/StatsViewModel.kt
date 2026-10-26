package com.wortart.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wortart.app.data.repository.StatsRepository
import com.wortart.app.domain.stats.StatsCalculator
import com.wortart.app.domain.stats.StatsSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * Turns the stored [com.wortart.app.data.local.entity.UserStats] into display values
 * (German Score, current streak, success percentage, practice time).
 */
class StatsViewModel(
    repository: StatsRepository,
    private val todayEpochDay: () -> Long = { LocalDate.now().toEpochDay() },
) : ViewModel() {

    val uiState: StateFlow<StatsSummary> = repository.observeStats()
        .map { stats -> StatsCalculator.summary(stats, todayEpochDay()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsSummary(),
        )

    companion object {
        fun factory(repository: StatsRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { StatsViewModel(repository) }
        }
    }
}
