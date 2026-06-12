package com.uni.colabtasks.ui.tasklists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.model.Invitation
import com.uni.colabtasks.domain.model.MemberRole
import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.model.TaskCounts
import com.uni.colabtasks.domain.model.TaskList
import com.uni.colabtasks.domain.repository.AuthRepository
import com.uni.colabtasks.domain.repository.TaskListRepository
import com.uni.colabtasks.domain.repository.TaskRepository
import com.uni.colabtasks.domain.usecase.tasklist.AcceptInvitationUseCase
import com.uni.colabtasks.domain.usecase.tasklist.CreateTaskListUseCase
import com.uni.colabtasks.domain.usecase.tasklist.DeleteTaskListUseCase
import com.uni.colabtasks.domain.usecase.tasklist.ObserveInvitationsUseCase
import com.uni.colabtasks.domain.usecase.tasklist.RejectInvitationUseCase
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

/** Contribuyente en edición: email + rol (EDITOR o VIEWER). */
data class ContributorDraft(
    val email: String,
    val role: MemberRole
)

data class TaskListDialogState(
    val visible: Boolean = false,
    val editingId: String? = null,
    val name: String = "",
    val description: String = "",
    val contributors: List<ContributorDraft> = emptyList(),
    val newContributorEmail: String = "",
    val newContributorRole: MemberRole = MemberRole.EDITOR
)

data class TaskListsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: List<TaskListItem> = emptyList(),
    val invitations: List<Invitation> = emptyList(),
    val showFavoritesOnly: Boolean = false,
    val dialog: TaskListDialogState = TaskListDialogState(),
    val currentUserId: String? = null,
    val pendingUndo: TaskList? = null,
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
    private val toggleFavorite: ToggleFavoriteUseCase,
    observeInvitations: ObserveInvitationsUseCase,
    private val acceptInvitation: AcceptInvitationUseCase,
    private val rejectInvitation: RejectInvitationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListsUiState())
    val uiState: StateFlow<TaskListsUiState> = _uiState.asStateFlow()

    private val showFavoritesOnly = MutableStateFlow(false)
    // Snapshot por listId — útil para acciones imperativas como compartir.
    @Volatile private var tasksByListSnapshot: Map<String, List<Task>> = emptyMap()

    fun snapshotTasksFor(listId: String): List<Task> = tasksByListSnapshot[listId].orEmpty()

    init {
        _uiState.update { it.copy(currentUserId = authRepository.getCurrentUserId()) }
        val items = authRepository.currentUser.flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else combineListsWithCounts(user.id)
        }.combine(showFavoritesOnly) { all, onlyFavs ->
            if (onlyFavs) all.filter { it.list.isFavorite } else all
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        viewModelScope.launch {
            items.collect { value ->
                _uiState.update { it.copy(isLoading = false, items = value) }
                // Sincroniza las tareas de cada dueño presente (deduplicado por ownerId en el
                // repositorio) para que las barras de progreso del home reflejen datos reales
                // sin tener que entrar a cada lista.
                value.map { it.list.ownerId }.distinct().forEach { ownerId ->
                    taskRepository.syncTasksForOwner(ownerId)
                }
            }
        }
        viewModelScope.launch {
            showFavoritesOnly.collect { value ->
                _uiState.update { it.copy(showFavoritesOnly = value) }
            }
        }
        // Invitaciones pendientes (aceptar/rechazar) en vivo
        viewModelScope.launch {
            authRepository.currentUser.flatMapLatest { user ->
                if (user == null) flowOf(emptyList()) else observeInvitations(user.id)
            }.collect { invites ->
                _uiState.update { it.copy(invitations = invites) }
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
        val drafts = list.contributors.map { ContributorDraft(it, MemberRole.EDITOR) } +
            list.viewerEmails.map { ContributorDraft(it, MemberRole.VIEWER) }
        it.copy(
            dialog = TaskListDialogState(
                visible = true,
                editingId = list.id,
                name = list.name,
                description = list.description.orEmpty(),
                contributors = drafts
            )
        )
    }

    fun dismissDialog() = _uiState.update { it.copy(dialog = TaskListDialogState()) }
    fun onNameChange(value: String) = _uiState.update { it.copy(dialog = it.dialog.copy(name = value)) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(dialog = it.dialog.copy(description = value)) }
    fun onContributorEmailChange(value: String) = _uiState.update { it.copy(dialog = it.dialog.copy(newContributorEmail = value)) }
    fun onContributorRoleChange(role: MemberRole) = _uiState.update { it.copy(dialog = it.dialog.copy(newContributorRole = role)) }

    fun addContributor() = _uiState.update { state ->
        val email = state.dialog.newContributorEmail.trim()
        if (email.isBlank() || !email.contains('@')) return@update state
        if (state.dialog.contributors.any { it.email.equals(email, ignoreCase = true) }) {
            return@update state.copy(dialog = state.dialog.copy(newContributorEmail = ""))
        }
        state.copy(
            dialog = state.dialog.copy(
                contributors = state.dialog.contributors + ContributorDraft(email, state.dialog.newContributorRole),
                newContributorEmail = ""
            )
        )
    }

    fun removeContributor(email: String) = _uiState.update {
        it.copy(dialog = it.dialog.copy(contributors = it.dialog.contributors.filterNot { c -> c.email == email }))
    }

    fun confirmDialog() {
        val s = _uiState.value.dialog
        val ownerId = authRepository.getCurrentUserId() ?: return
        val editorEmails = s.contributors.filter { it.role == MemberRole.EDITOR }.map { it.email }
        val viewerEmails = s.contributors.filter { it.role == MemberRole.VIEWER }.map { it.email }
        viewModelScope.launch {
            val result = if (s.editingId == null) {
                createTaskList(ownerId, s.name, s.description, editorEmails, viewerEmails).map { Unit }
            } else {
                updateTaskList(s.editingId, s.name, s.description, editorEmails, viewerEmails)
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

    fun refresh() {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            runCatching {
                taskListRepository.syncFromRemote(uid)
                _uiState.value.items.map { it.list.ownerId }.distinct().forEach { ownerId ->
                    taskRepository.syncTasksForOwner(ownerId)
                }
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    // ---- invitaciones ----
    fun accept(invitation: Invitation) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch { acceptInvitation(uid, invitation) }
    }

    fun reject(invitation: Invitation) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch { rejectInvitation(uid, invitation) }
    }

    // Tareas de la última lista borrada — para restaurarlas junto con la lista en el undo.
    private var lastDeletedListTasks: List<Task> = emptyList()

    fun deleteList(list: TaskList) {
        lastDeletedListTasks = tasksByListSnapshot[list.id].orEmpty()
        viewModelScope.launch {
            // No cancelamos el sync del dueño: el listener per-owner reconcilia solo, y filtra
            // por listas conocidas, así que tras el cascade delete no reinserta tareas huérfanas.
            deleteTaskList(list.id)
            _uiState.update { it.copy(pendingUndo = list) }
        }
    }

    fun undoDeleteList() {
        val list = _uiState.value.pendingUndo ?: return
        val tasks = lastDeletedListTasks
        viewModelScope.launch {
            taskListRepository.restoreList(list)
            tasks.forEach { taskRepository.createTask(it) }
            _uiState.update { it.copy(pendingUndo = null) }
            lastDeletedListTasks = emptyList()
        }
    }

    fun clearUndo() = _uiState.update { it.copy(pendingUndo = null) }

    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }
}
