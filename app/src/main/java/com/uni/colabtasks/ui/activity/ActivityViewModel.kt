package com.uni.colabtasks.ui.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.model.ActivityEntry
import com.uni.colabtasks.domain.repository.TaskListRepository
import com.uni.colabtasks.domain.usecase.activity.ObserveActivityUseCase
import com.uni.colabtasks.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityUiState(
    val isLoading: Boolean = true,
    val listName: String = "",
    val entries: List<ActivityEntry> = emptyList()
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskListRepository: TaskListRepository,
    private val observeActivity: ObserveActivityUseCase
) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle[Destinations.ARG_LIST_ID])

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val list = taskListRepository.getList(listId)
            if (list == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            _uiState.update { it.copy(listName = list.name) }
            observeActivity(list.ownerId, listId).collect { entries ->
                _uiState.update { it.copy(isLoading = false, entries = entries) }
            }
        }
    }
}
