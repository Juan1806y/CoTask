package com.uni.colabtasks.ui.taskedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.model.Comment
import com.uni.colabtasks.domain.model.ListMember
import com.uni.colabtasks.domain.model.Priority
import com.uni.colabtasks.domain.model.Recurrence
import com.uni.colabtasks.domain.model.Subtask
import com.uni.colabtasks.domain.repository.AuthRepository
import com.uni.colabtasks.domain.repository.CommentRepository
import com.uni.colabtasks.domain.repository.TaskListRepository
import com.uni.colabtasks.domain.usecase.task.GetTaskUseCase
import com.uni.colabtasks.domain.usecase.task.SaveTaskUseCase
import com.uni.colabtasks.domain.usecase.tasklist.GetListMembersUseCase
import com.uni.colabtasks.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskEditUiState(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val dueDate: Long? = null,
    val priority: Priority = Priority.NONE,
    val assignedTo: String? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    val subtasks: List<Subtask> = emptyList(),
    val newSubtaskTitle: String = "",
    val members: List<ListMember> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val commentDraft: String = "",
    val currentUid: String? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val saved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TaskEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val taskListRepository: TaskListRepository,
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
    private val getListMembers: GetListMembersUseCase,
    private val commentRepository: CommentRepository
) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle[Destinations.ARG_LIST_ID])
    private val taskId: String? = savedStateHandle[Destinations.ARG_TASK_ID]

    private var cachedListOwnerId: String? = null
    private var canEdit: Boolean = true

    val isEditing: Boolean get() = taskId != null

    private val _uiState = MutableStateFlow(TaskEditUiState())
    val uiState: StateFlow<TaskEditUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(currentUid = authRepository.getCurrentUserId()) }
        viewModelScope.launch {
            val list = taskListRepository.getList(listId)
            cachedListOwnerId = list?.ownerId
            canEdit = list?.canEditTasks(authRepository.getCurrentUserId()) ?: true
            // Carga miembros para el selector de asignación (no bloquea el resto).
            if (list != null) {
                val members = runCatching { getListMembers(list) }.getOrDefault(emptyList())
                _uiState.update { it.copy(members = members) }
            }

            // Observa comentarios de la tarea (solo en edición de tarea existente).
            if (taskId != null && list != null) {
                launch {
                    commentRepository.observeComments(list.ownerId, taskId).collect { comments ->
                        _uiState.update { it.copy(comments = comments) }
                    }
                }
            }

            if (taskId != null) {
                val task = getTaskUseCase(taskId)
                if (task != null) {
                    _uiState.update {
                        it.copy(
                            title = task.title,
                            description = task.description.orEmpty(),
                            category = task.category.orEmpty(),
                            dueDate = task.dueDate,
                            priority = task.priority,
                            assignedTo = task.assignedTo,
                            recurrence = task.recurrence,
                            subtasks = task.subtasks,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "La tarea no existe") }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, errorMessage = null) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(category = value) }
    fun onDueDateChange(value: Long?) = _uiState.update { it.copy(dueDate = value) }
    fun onPriorityChange(value: Priority) = _uiState.update { it.copy(priority = value) }
    fun onAssigneeChange(uid: String?) = _uiState.update { it.copy(assignedTo = uid) }
    fun onRecurrenceChange(value: Recurrence) = _uiState.update { it.copy(recurrence = value) }
    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }

    // ---- subtareas (estado local; se persisten al guardar) ----
    fun onNewSubtaskChange(value: String) = _uiState.update { it.copy(newSubtaskTitle = value) }

    fun addSubtask() = _uiState.update { state ->
        val title = state.newSubtaskTitle.trim()
        if (title.isEmpty()) return@update state
        state.copy(
            subtasks = state.subtasks + Subtask(
                id = java.util.UUID.randomUUID().toString(),
                title = title,
                isDone = false
            ),
            newSubtaskTitle = ""
        )
    }

    fun toggleSubtask(subtaskId: String) = _uiState.update { state ->
        state.copy(subtasks = state.subtasks.map {
            if (it.id == subtaskId) it.copy(isDone = !it.isDone) else it
        })
    }

    fun removeSubtask(subtaskId: String) = _uiState.update { state ->
        state.copy(subtasks = state.subtasks.filterNot { it.id == subtaskId })
    }

    fun onCommentDraftChange(value: String) = _uiState.update { it.copy(commentDraft = value) }

    fun sendComment() {
        val text = _uiState.value.commentDraft.trim()
        val ownerId = cachedListOwnerId ?: return
        val uid = authRepository.getCurrentUserId() ?: return
        if (text.isEmpty() || taskId == null) return
        val name = authRepository.getCurrentDisplayName() ?: "Alguien"
        _uiState.update { it.copy(commentDraft = "") }
        viewModelScope.launch {
            runCatching { commentRepository.addComment(ownerId, taskId, uid, name, text) }
        }
    }

    fun deleteComment(commentId: String) {
        val ownerId = cachedListOwnerId ?: return
        if (taskId == null) return
        viewModelScope.launch {
            runCatching { commentRepository.deleteComment(ownerId, taskId, commentId) }
        }
    }

    fun save() {
        val s = _uiState.value
        if (!canEdit) {
            _uiState.update { it.copy(errorMessage = "No tienes permiso para editar esta lista") }
            return
        }
        val ownerId = cachedListOwnerId ?: run {
            _uiState.update { it.copy(errorMessage = "La lista no existe") }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            saveTaskUseCase(
                existingId = taskId,
                listId = listId,
                ownerId = ownerId,
                title = s.title,
                description = s.description,
                category = s.category,
                dueDate = s.dueDate,
                priority = s.priority,
                assignedTo = s.assignedTo,
                recurrence = s.recurrence,
                subtasks = s.subtasks
            ).onSuccess {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Error") }
            }
        }
    }
}
