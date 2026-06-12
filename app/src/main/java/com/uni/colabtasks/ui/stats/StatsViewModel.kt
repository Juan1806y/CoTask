package com.uni.colabtasks.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.model.TaskCounts
import com.uni.colabtasks.domain.repository.AuthRepository
import com.uni.colabtasks.domain.usecase.task.ObserveAllAccessibleTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** Una barra del gráfico semanal: lunes de la semana + cuántas tareas se completaron. */
data class WeekBar(
    val weekStart: LocalDate,
    val label: String,
    val count: Int
)

data class StatsUiState(
    val counts: TaskCounts = TaskCounts.Empty,
    val weekly: List<WeekBar> = emptyList(),
    val isLoading: Boolean = true
) {
    val maxWeekly: Int get() = weekly.maxOfOrNull { it.count } ?: 0
    val totalCompletedShown: Int get() = weekly.sumOf { it.count }
    val hasData: Boolean get() = counts.total > 0
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    authRepository: AuthRepository,
    observeAllTasks: ObserveAllAccessibleTasksUseCase
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val labelFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    val uiState: StateFlow<StatsUiState> =
        authRepository.currentUser
            .flatMapLatest { user ->
                if (user == null) flowOf(emptyList()) else observeAllTasks()
            }
            .map { tasks ->
                val counts = TaskCounts(
                    total = tasks.size,
                    pending = tasks.count { !it.isCompleted },
                    done = tasks.count { it.isCompleted }
                )
                StatsUiState(
                    counts = counts,
                    weekly = buildWeeklyBars(tasks.filter { it.isCompleted }.map { it.updatedAt }),
                    isLoading = false
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    /**
     * Agrupa los timestamps de completado en las últimas [WEEKS_SHOWN] semanas ISO
     * (lunes a domingo). `updatedAt` se usa como proxy del momento de completado.
     */
    private fun buildWeeklyBars(completedAtMillis: List<Long>): List<WeekBar> {
        val thisMonday = LocalDate.now(zone).mondayOfWeek()
        // Lunes de cada una de las últimas N semanas, en orden ascendente.
        val weekStarts = (WEEKS_SHOWN - 1 downTo 0).map { thisMonday.minusWeeks(it.toLong()) }
        val countByWeek = weekStarts.associateWith { 0 }.toMutableMap()

        completedAtMillis.forEach { millis ->
            val monday = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().mondayOfWeek()
            if (monday in countByWeek) countByWeek[monday] = countByWeek.getValue(monday) + 1
        }

        return weekStarts.map { start ->
            WeekBar(
                weekStart = start,
                label = start.format(labelFormatter),
                count = countByWeek.getValue(start)
            )
        }
    }

    private fun LocalDate.mondayOfWeek(): LocalDate =
        minusDays((dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

    private companion object {
        const val WEEKS_SHOWN = 6
    }
}
