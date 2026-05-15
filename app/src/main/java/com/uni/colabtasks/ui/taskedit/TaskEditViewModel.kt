package com.uni.colabtasks.ui.taskedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.repository.TaskListRepository
import com.uni.colabtasks.domain.usecase.task.GetTaskUseCase
import com.uni.colabtasks.domain.usecase.task.SaveTaskUseCase
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
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val saved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TaskEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskListRepository: TaskListRepository,
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase
) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle[Destinations.ARG_LIST_ID])
    private val taskId: String? = savedStateHandle[Destinations.ARG_TASK_ID]

    private val _uiState = MutableStateFlow(TaskEditUiState())
    val uiState: StateFlow<TaskEditUiState> = _uiState.asStateFlow()

    /** Cacheado en init para no consultar Room en cada save. */
    private var cachedListOwnerId: String? = null

    val isEditing: Boolean get() = taskId != null

    init {
        viewModelScope.launch {
            // Necesitamos saber quién es el dueño de la lista para escribir las tareas
            // en el árbol correcto (importante para listas compartidas).
            cachedListOwnerId = taskListRepository.getList(listId)?.ownerId

            if (taskId != null) {
                val task = getTaskUseCase(taskId)
                if (task != null) {
                    _uiState.update {
                        it.copy(
                            title = task.title,
                            description = task.description.orEmpty(),
                            category = task.category.orEmpty(),
                            dueDate = task.dueDate,
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
    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }

    fun save() {
        val s = _uiState.value
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
                dueDate = s.dueDate
            ).onSuccess {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Error") }
            }
        }
    }
}
