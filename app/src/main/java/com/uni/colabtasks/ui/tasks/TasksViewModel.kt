package com.uni.colabtasks.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.model.TaskCounts
import com.uni.colabtasks.domain.model.TaskFilter
import com.uni.colabtasks.domain.model.TaskList
import com.uni.colabtasks.domain.model.TaskSort
import com.uni.colabtasks.domain.repository.AuthRepository
import com.uni.colabtasks.domain.repository.TaskListRepository
import com.uni.colabtasks.domain.repository.TaskRepository
import com.uni.colabtasks.domain.usecase.task.DeleteTaskUseCase
import com.uni.colabtasks.domain.usecase.task.ObserveTaskCountsUseCase
import com.uni.colabtasks.domain.usecase.task.ObserveTasksUseCase
import com.uni.colabtasks.domain.usecase.task.SaveTaskUseCase
import com.uni.colabtasks.domain.usecase.task.ToggleTaskCompletionUseCase
import com.uni.colabtasks.domain.usecase.tasklist.GetListMembersUseCase
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
    val searchQuery: String = "",
    val sort: TaskSort = TaskSort.DUE_DATE,
    val counts: TaskCounts = TaskCounts.Empty,
    val list: TaskList? = null,
    val memberNames: Map<String, String> = emptyMap(),
    val canEdit: Boolean = true,
    val pendingUndo: Task? = null,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val taskListRepository: TaskListRepository,
    private val taskRepository: TaskRepository,
    observeTasks: ObserveTasksUseCase,
    observeCounts: ObserveTaskCountsUseCase,
    private val toggleCompletion: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val getListMembers: GetListMembersUseCase
) : ViewModel() {

    val listId: String = checkNotNull(savedStateHandle[Destinations.ARG_LIST_ID])

    private val filter = MutableStateFlow(TaskFilter.ALL)
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val sort = MutableStateFlow(TaskSort.DUE_DATE)

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    /** Tareas crudas de Room según el filtro de status (antes de categoría/búsqueda/orden). */
    private val tasksByStatus: StateFlow<List<Task>> = filter
        .flatMapLatest { observeTasks(listId, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Pipeline final: categoría → búsqueda → orden (todo en memoria). */
    private val filteredTasks: StateFlow<List<Task>> =
        combine(tasksByStatus, selectedCategory, searchQuery, sort) { items, category, query, sortOption ->
            items
                .filter { category.isNullOrBlank() || it.category.equals(category, ignoreCase = true) }
                .filter { query.isBlank() || it.matchesQuery(query) }
                .sortedWith(comparatorFor(sortOption))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val counts: StateFlow<TaskCounts> = observeCounts(listId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskCounts.Empty)

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
            val currentUid = authRepository.getCurrentUserId()
            taskListRepository.observeList(listId).collect { list ->
                val canEdit = list?.canEditTasks(currentUid) ?: true
                _uiState.update { it.copy(list = list, canEdit = canEdit) }
                list?.ownerId?.let { ownerId ->
                    taskRepository.syncTasksForOwner(ownerId)
                }
                // Resolver nombres de miembros para mostrar el asignado (una sola vez por lista).
                if (list != null && _uiState.value.memberNames.isEmpty()) {
                    val members = runCatching { getListMembers(list) }.getOrDefault(emptyList())
                    _uiState.update { st -> st.copy(memberNames = members.associate { m -> m.uid to m.label }) }
                }
            }
        }
        viewModelScope.launch {
            filteredTasks.collect { items ->
                _uiState.update { it.copy(isLoading = false, tasks = items) }
            }
        }
        viewModelScope.launch {
            counts.collect { value -> _uiState.update { it.copy(counts = value) } }
        }
        viewModelScope.launch {
            filter.collect { value -> _uiState.update { it.copy(filter = value) } }
        }
        viewModelScope.launch {
            selectedCategory.collect { value -> _uiState.update { it.copy(selectedCategory = value) } }
        }
        viewModelScope.launch {
            searchQuery.collect { value -> _uiState.update { it.copy(searchQuery = value) } }
        }
        viewModelScope.launch {
            sort.collect { value -> _uiState.update { it.copy(sort = value) } }
        }
        viewModelScope.launch {
            availableCategories.collect { value ->
                _uiState.update { current ->
                    val selectionStillValid = current.selectedCategory?.let { it in value } ?: true
                    val newSelection = if (selectionStillValid) current.selectedCategory else null
                    if (current.selectedCategory != newSelection) selectedCategory.value = newSelection
                    current.copy(availableCategories = value)
                }
            }
        }
    }

    fun setFilter(value: TaskFilter) { filter.value = value }
    fun setCategory(value: String?) { selectedCategory.value = value }
    fun setSearchQuery(value: String) { searchQuery.value = value }
    fun setSort(value: TaskSort) { sort.value = value }

    fun toggle(task: Task) {
        viewModelScope.launch { toggleCompletion(task.id, !task.isCompleted) }
    }

    /** Borra la tarea y guarda una copia para poder deshacer. */
    fun delete(task: Task) {
        viewModelScope.launch {
            deleteTaskUseCase(task.id)
            _uiState.update { it.copy(pendingUndo = task) }
        }
    }

    /** Re-crea la última tarea borrada conservando su id y campos. */
    fun undoDelete() {
        val task = _uiState.value.pendingUndo ?: return
        viewModelScope.launch {
            taskRepository.createTask(task)
            _uiState.update { it.copy(pendingUndo = null) }
        }
    }

    fun clearUndo() = _uiState.update { it.copy(pendingUndo = null) }

    fun toggleListFavorite() {
        val current = _uiState.value.list ?: return
        viewModelScope.launch { toggleFavorite(current.id, !current.isFavorite) }
    }

    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }

    private fun Task.matchesQuery(query: String): Boolean {
        val q = query.trim()
        return title.contains(q, ignoreCase = true) ||
            description?.contains(q, ignoreCase = true) == true ||
            category?.contains(q, ignoreCase = true) == true
    }

    /**
     * Comparador: las completadas siempre al final; dentro de cada grupo se aplica
     * el criterio elegido por el usuario.
     */
    private fun comparatorFor(sortOption: TaskSort): Comparator<Task> {
        val secondary: Comparator<Task> = when (sortOption) {
            TaskSort.DUE_DATE -> compareBy(nullsLast()) { it.dueDate }
            TaskSort.PRIORITY -> compareByDescending { it.priority.level }
            TaskSort.ALPHABETICAL -> compareBy { it.title.lowercase() }
            TaskSort.CREATED -> compareByDescending { it.createdAt }
        }
        return compareBy<Task> { it.isCompleted }.then(secondary)
    }
}
