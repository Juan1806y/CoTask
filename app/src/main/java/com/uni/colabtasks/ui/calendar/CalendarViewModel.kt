package com.uni.colabtasks.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.model.Recurrence
import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.model.nextOccurrence
import com.uni.colabtasks.domain.repository.AuthRepository
import com.uni.colabtasks.domain.usecase.task.ObserveTasksWithDueDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/** Ocurrencia de una tarea en un día del calendario. `isProjected` = repetición futura prevista. */
data class DayTask(
    val task: Task,
    val isProjected: Boolean
)

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDay: LocalDate? = null,
    val tasksByDay: Map<LocalDate, List<DayTask>> = emptyMap()
) {
    val daysWithTasks: Set<LocalDate> get() = tasksByDay.keys
    val tasksForSelectedDay: List<DayTask> get() = selectedDay?.let { tasksByDay[it] }.orEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    authRepository: AuthRepository,
    observeTasksWithDueDate: ObserveTasksWithDueDateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(selectedDay = LocalDate.now()))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val zone: ZoneId = ZoneId.systemDefault()
    private val month = MutableStateFlow(YearMonth.now())

    init {
        val tasksFlow = authRepository.currentUser.flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else observeTasksWithDueDate()
        }

        // Recalcula las ocurrencias (reales + repeticiones proyectadas) cada vez que cambian
        // las tareas o el mes visible.
        val byDayFlow = combine(tasksFlow, month) { tasks, visibleMonth ->
            val map = mutableMapOf<LocalDate, MutableList<DayTask>>()
            tasks.forEach { task ->
                occurrencesInMonth(task, visibleMonth).forEach { (date, projected) ->
                    map.getOrPut(date) { mutableListOf() }.add(DayTask(task, projected))
                }
            }
            map.mapValues { it.value.toList() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

        viewModelScope.launch {
            byDayFlow.collect { grouped ->
                _uiState.update { it.copy(tasksByDay = grouped) }
            }
        }
        viewModelScope.launch {
            month.collect { value -> _uiState.update { it.copy(currentMonth = value) } }
        }
    }

    /**
     * Días del mes visible en los que ocurre la tarea: la ocurrencia real (su dueDate) más,
     * si es recurrente y no está completada, las repeticiones futuras dentro del mes.
     */
    private fun occurrencesInMonth(task: Task, visibleMonth: YearMonth): List<Pair<LocalDate, Boolean>> {
        val dueMs = task.dueDate ?: return emptyList()
        val base = Instant.ofEpochMilli(dueMs).atZone(zone).toLocalDate()
        val windowStart = visibleMonth.atDay(1)
        val windowEnd = visibleMonth.atEndOfMonth()

        val result = mutableListOf<Pair<LocalDate, Boolean>>()
        // Ocurrencia real (su dueDate), si cae en el mes visible.
        if (!base.isBefore(windowStart) && !base.isAfter(windowEnd)) {
            result += base to false
        }
        // Repeticiones futuras proyectadas (solo para recurrentes no completadas).
        if (task.recurrence != Recurrence.NONE && !task.isCompleted) {
            var nextMs: Long? = nextOccurrence(dueMs, task.recurrence)
            var guard = 0
            while (nextMs != null && guard < 2000) {
                val d = Instant.ofEpochMilli(nextMs).atZone(zone).toLocalDate()
                if (d.isAfter(windowEnd)) break
                if (!d.isBefore(windowStart)) result += d to true
                nextMs = nextOccurrence(nextMs, task.recurrence)
                guard++
            }
        }
        return result
    }

    fun selectMonth(value: YearMonth) { month.value = value }
    fun nextMonth() = selectMonth(month.value.plusMonths(1))
    fun previousMonth() = selectMonth(month.value.minusMonths(1))

    fun selectDay(day: LocalDate?) {
        _uiState.update { it.copy(selectedDay = day) }
    }
}
