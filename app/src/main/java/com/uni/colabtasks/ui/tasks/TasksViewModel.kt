package com.uni.colabtasks.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.model.TaskCounts
import com.uni.colabtasks.domain.model.TaskFilter
import com.uni.colabtasks.domain.model.TaskList
import com.uni.colabtasks.domain.repository.TaskListRepository
import com.uni.colabtasks.domain.repository.TaskRepository
import com.uni.colabtasks.domain.usecase.task.DeleteTaskUseCase
import com.uni.colabtasks.domain.usecase.task.ObserveTaskCountsUseCase
import com.uni.colabtasks.domain.usecase.task.ObserveTasksUseCase
import com.uni.colabtasks.domain.usecase.task.ToggleTaskCompletionUseCase
import com.uni.colabtasks.domain.usecase.tasklist.ToggleFavoriteUseCase
import com.uni.colabtasks.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val isLoading: Boolean = true,
    val tasks: List<Task> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val selectedCategory: String? = null,
    val availableCategories: List<String> = emptyList(),
    val counts: TaskCounts = TaskCounts.Empty,
    val list: TaskList? = null,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskListRepository: TaskListRepository,
    private val taskRepository: TaskRepository,
    observeTasks: ObserveTasksUseCase,
    observeCounts: ObserveTaskCountsUseCase,
    private val toggleCompletion: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    val listId: String = checkNotNull(savedStateHandle[Destinations.ARG_LIST_ID])

    private val filter = MutableStateFlow(TaskFilter.ALL)
    private val selectedCategory = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    /** Tareas crudas de Room según el filtro de status (antes del filtro por categoría). */
    private val tasksByStatus: StateFlow<List<Task>> = filter
        .flatMapLatest { observeTasks(listId, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Tareas finales tras aplicar el filtro de categoría (en memoria). */
    private val filteredTasks: StateFlow<List<Task>> =
        combine(tasksByStatus, selectedCategory) { items, category ->
            if (category.isNullOrBlank()) items
            else items.filter { it.category.equals(category, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val counts: StateFlow<TaskCounts> = observeCounts(listId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskCounts.Empty)

    /**
     * Categorías presentes en las tareas de esta lista (sin importar el filtro de status,
     * para que siempre se vean todas las opciones disponibles).
     */
    private val availableCategories: StateFlow<List<String>> =
        observeTasks(listId, TaskFilter.ALL)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
            .let { allFlow ->
                combine(allFlow, selectedCategory) { items, _ ->
                    items.mapNotNull { it.category?.takeIf { c -> c.isNotBlank() } }
                        .distinct()
                        .sorted()
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
            }

    init {
        viewModelScope.launch {
            taskListRepository.observeList(listId).collect { list ->
                _uiState.update { it.copy(list = list) }
                list?.ownerId?.let { ownerId ->
                    taskRepository.syncFromRemote(ownerId, listId)
                }
            }
        }
        viewModelScope.launch {
            filteredTasks.collect { items ->
                _uiState.update { it.copy(isLoading = false, tasks = items) }
            }
        }
        viewModelScope.launch {
            counts.collect { value ->
                _uiState.update { it.copy(counts = value) }
            }
        }
        viewModelScope.launch {
            filter.collect { value -> _uiState.update { it.copy(filter = value) } }
        }
        viewModelScope.launch {
            selectedCategory.collect { value -> _uiState.update { it.copy(selectedCategory = value) } }
        }
        viewModelScope.launch {
            availableCategories.collect { value ->
                _uiState.update { current ->
                    // Si la categoría seleccionada ya no existe en la lista, reseteamos a "todas".
                    val seleccionStillValid = current.selectedCategory?.let { it in value } ?: true
                    val newSelection = if (seleccionStillValid) current.selectedCategory else null
                    if (current.selectedCategory != newSelection) selectedCategory.value = newSelection
                    current.copy(availableCategories = value)
                }
            }
        }
    }

    fun setFilter(value: TaskFilter) { filter.value = value }

    fun setCategory(value: String?) { selectedCategory.value = value }

    fun toggle(task: Task) {
        viewModelScope.launch { toggleCompletion(task.id, !task.isCompleted) }
    }

    fun delete(task: Task) {
        viewModelScope.launch { deleteTaskUseCase(task.id) }
    }

    fun toggleListFavorite() {
        val current = _uiState.value.list ?: return
        viewModelScope.launch { toggleFavorite(current.id, !current.isFavorite) }
    }

    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }
}
