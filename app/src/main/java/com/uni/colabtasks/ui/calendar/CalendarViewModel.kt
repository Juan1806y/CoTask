package com.uni.colabtasks.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.repository.AuthRepository
import com.uni.colabtasks.domain.usecase.task.ObserveTasksWithDueDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDay: LocalDate? = null,
    val tasksByDay: Map<LocalDate, List<Task>> = emptyMap()
) {
    val daysWithTasks: Set<LocalDate> get() = tasksByDay.keys
    val tasksForSelectedDay: List<Task> get() = selectedDay?.let { tasksByDay[it] }.orEmpty()
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

    init {
        val tasksByDayFlow = authRepository.currentUser.flatMapLatest { user ->
            if (user == null) flowOf(emptyMap())
            else observeTasksWithDueDate().map { tasks ->
                tasks.groupBy { task ->
                    java.time.Instant.ofEpochMilli(task.dueDate!!)
                        .atZone(zone).toLocalDate()
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

        viewModelScope.launch {
            tasksByDayFlow.collect { grouped ->
                _uiState.update { it.copy(tasksByDay = grouped) }
            }
        }
    }

    fun selectMonth(month: YearMonth) {
        _uiState.update { it.copy(currentMonth = month) }
    }

    fun nextMonth() = selectMonth(_uiState.value.currentMonth.plusMonths(1))
    fun previousMonth() = selectMonth(_uiState.value.currentMonth.minusMonths(1))

    fun selectDay(day: LocalDate?) {
        _uiState.update { it.copy(selectedDay = day) }
    }
}
