package com.uni.colabtasks.ui.tasklists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.model.TaskCounts
import com.uni.colabtasks.domain.model.TaskList
import com.uni.colabtasks.domain.repository.AuthRepository
import com.uni.colabtasks.domain.repository.TaskListRepository
import com.uni.colabtasks.domain.repository.TaskRepository
import com.uni.colabtasks.domain.usecase.tasklist.CreateTaskListUseCase
import com.uni.colabtasks.domain.usecase.tasklist.DeleteTaskListUseCase
import com.uni.colabtasks.domain.usecase.tasklist.ToggleFavoriteUseCase
import com.uni.colabtasks.domain.usecase.tasklist.UpdateTaskListUseCase
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
import javax.inject.Inject

data class TaskListItem(
    val list: TaskList,
    val counts: TaskCounts
)

data class TaskListDialogState(
    val visible: Boolean = false,
    val editingId: String? = null,
    val name: String = "",
    val description: String = "",
    val contributors: List<String> = emptyList(),
    val newContributorEmail: String = ""
)

data class TaskListsUiState(
    val isLoading: Boolean = true,
    val items: List<TaskListItem> = emptyList(),
    val showFavoritesOnly: Boolean = false,
    val dialog: TaskListDialogState = TaskListDialogState(),
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskListsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val taskListRepository: TaskListRepository,
    private val taskRepository: TaskRepository,
    private val createTaskList: CreateTaskListUseCase,
    private val updateTaskList: UpdateTaskListUseCase,
    private val deleteTaskList: DeleteTaskListUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListsUiState())
    val uiState: StateFlow<TaskListsUiState> = _uiState.asStateFlow()

    private val showFavoritesOnly = MutableStateFlow(false)
    // Snapshot por listId — útil para acciones imperativas como compartir.
    @Volatile private var tasksByListSnapshot: Map<String, List<Task>> = emptyMap()

    fun snapshotTasksFor(listId: String): List<Task> = tasksByListSnapshot[listId].orEmpty()

    init {
        val items = authRepository.currentUser.flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else combineListsWithCounts(user.id)
        }.combine(showFavoritesOnly) { all, onlyFavs ->
            if (onlyFavs) all.filter { it.list.isFavorite } else all
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        viewModelScope.launch {
            items.collect { value ->
                _uiState.update { it.copy(isLoading = false, items = value) }
                // Lanza sync de tareas para cada lista visible para que la barra de progreso
                // del home refleje datos reales sin tener que entrar a la lista primero.
                // Las llamadas están deduplicadas por (ownerId, listId) en el repositorio,
                // así que invocarlas repetidamente es seguro y barato.
                value.forEach { item ->
                    taskRepository.syncFromRemote(item.list.ownerId, item.list.id)
                }
            }
        }
        viewModelScope.launch {
            showFavoritesOnly.collect { value ->
                _uiState.update { it.copy(showFavoritesOnly = value) }
            }
        }
    }

    private fun combineListsWithCounts(ownerId: String) = combine(
        taskListRepository.observeLists(ownerId),
        taskRepository.observeAllAccessibleTasks()
    ) { lists, tasks ->
        val grouped = tasks.groupBy { it.listId }
        tasksByListSnapshot = grouped // keep last snapshot for imperative actions (e.g. share)
        val countsById = grouped.mapValues { (_, ts) ->
            val total = ts.size
            val done = ts.count { it.isCompleted }
            TaskCounts(total = total, done = done, pending = total - done)
        }
        lists.map { TaskListItem(list = it, counts = countsById[it.id] ?: TaskCounts.Empty) }
    }

    // ---- dialog ----
    fun openCreateDialog() = _uiState.update {
        it.copy(dialog = TaskListDialogState(visible = true))
    }

    fun openEditDialog(list: TaskList) = _uiState.update {
        it.copy(
            dialog = TaskListDialogState(
                visible = true,
                editingId = list.id,
                name = list.name,
                description = list.description.orEmpty(),
                contributors = list.contributors
            )
        )
    }

    fun dismissDialog() = _uiState.update { it.copy(dialog = TaskListDialogState()) }
    fun onNameChange(value: String) = _uiState.update { it.copy(dialog = it.dialog.copy(name = value)) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(dialog = it.dialog.copy(description = value)) }
    fun onContributorEmailChange(value: String) = _uiState.update { it.copy(dialog = it.dialog.copy(newContributorEmail = value)) }

    fun addContributor() = _uiState.update { state ->
        val email = state.dialog.newContributorEmail.trim()
        if (email.isBlank() || !email.contains('@')) return@update state
        if (state.dialog.contributors.contains(email)) {
            return@update state.copy(dialog = state.dialog.copy(newContributorEmail = ""))
        }
        state.copy(
            dialog = state.dialog.copy(
                contributors = state.dialog.contributors + email,
                newContributorEmail = ""
            )
        )
    }

    fun removeContributor(email: String) = _uiState.update {
        it.copy(dialog = it.dialog.copy(contributors = it.dialog.contributors - email))
    }

    fun confirmDialog() {
        val s = _uiState.value.dialog
        val ownerId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = if (s.editingId == null) {
                createTaskList(ownerId, s.name, s.description, s.contributors).map { Unit }
            } else {
                updateTaskList(s.editingId, s.name, s.description, s.contributors)
            }
            result.onSuccess { dismissDialog() }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message ?: "Error") } }
        }
    }

    // ---- list actions ----
    fun toggleFavoritesFilter() = showFavoritesOnly.update { !it }

    fun toggleListFavorite(list: TaskList) {
        viewModelScope.launch { toggleFavorite(list.id, !list.isFavorite) }
    }

    fun deleteList(list: TaskList) {
        viewModelScope.launch { deleteTaskList(list.id) }
    }

    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }
}
