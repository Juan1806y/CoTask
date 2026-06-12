package com.uni.colabtasks.ui.taskedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uni.colabtasks.R
import com.uni.colabtasks.domain.model.Comment
import com.uni.colabtasks.domain.model.Priority
import com.uni.colabtasks.ui.util.formatShortDate
import com.uni.colabtasks.ui.util.priorityColor
import com.uni.colabtasks.ui.util.priorityLabel
import com.uni.colabtasks.domain.model.TaskCategory
import com.uni.colabtasks.ui.common.components.LoadingIndicator
import com.uni.colabtasks.ui.util.formatShortDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TaskEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingIndicator(Modifier.padding(innerPadding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            TaskEditCard(
                isEditing = viewModel.isEditing,
                title = state.title,
                description = state.description,
                category = state.category,
                dueDate = state.dueDate,
                priority = state.priority,
                members = state.members,
                assignedTo = state.assignedTo,
                isSaving = state.isSaving,
                onTitleChange = viewModel::onTitleChange,
                onDescriptionChange = viewModel::onDescriptionChange,
                onCategoryChange = viewModel::onCategoryChange,
                onDateChange = viewModel::onDueDateChange,
                onPriorityChange = viewModel::onPriorityChange,
                onAssigneeChange = viewModel::onAssigneeChange,
                onSave = viewModel::save,
                onCancel = onBack
            )

            // Comentarios (solo para tareas existentes)
            if (viewModel.isEditing) {
                Spacer(Modifier.size(16.dp))
                CommentsSection(
                    comments = state.comments,
                    draft = state.commentDraft,
                    currentUid = state.currentUid,
                    onDraftChange = viewModel::onCommentDraftChange,
                    onSend = viewModel::sendComment,
                    onDelete = viewModel::deleteComment
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditCard(
    isEditing: Boolean,
    title: String,
    description: String,
    category: String,
    dueDate: Long?,
    priority: Priority,
    members: List<com.uni.colabtasks.domain.model.ListMember>,
    assignedTo: String?,
    isSaving: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDateChange: (Long?) -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onAssigneeChange: (String?) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (isEditing) R.string.edit_task else R.string.dialog_new_task_title
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!isEditing) {
                        Text(
                            text = stringResource(R.string.dialog_new_task_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cancel))
                }
            }
            Spacer(Modifier.size(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.task_title_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.size(10.dp))
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.task_description_hint)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.size(10.dp))

            // Date field
            OutlinedTextField(
                value = dueDate?.let { formatShortDate(it) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.task_date_hint)) },
                placeholder = { Text(stringResource(R.string.task_date_placeholder)) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.size(10.dp))

            CategoryPicker(
                value = category,
                onValueChange = onCategoryChange
            )
            Spacer(Modifier.size(14.dp))

            PrioritySelector(selected = priority, onSelect = onPriorityChange)

            // Selector de asignación (solo si la lista tiene más de un miembro)
            if (members.size > 1) {
                Spacer(Modifier.size(14.dp))
                AssigneePicker(
                    members = members,
                    assignedTo = assignedTo,
                    onSelect = onAssigneeChange
                )
            }

            Spacer(Modifier.size(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(stringResource(if (isEditing) R.string.save else R.string.create_task))
                }
                Spacer(Modifier.size(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateChange(pickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(value: String, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val predefined = listOf(
        TaskCategory.PERSONAL to stringResource(R.string.category_personal),
        TaskCategory.WORK to stringResource(R.string.category_work),
        TaskCategory.STUDY to stringResource(R.string.category_study),
        TaskCategory.HOME to stringResource(R.string.category_home),
        TaskCategory.HEALTH to stringResource(R.string.category_health),
        TaskCategory.OTHER to stringResource(R.string.category_other)
    )

    // Para mostrar: si value coincide con alguna predefinida, muestra la label localizada;
    // si es custom, muestra el value tal cual.
    val displayed = TaskCategory.matchKey(value)
        ?.let { match -> predefined.firstOrNull { it.first == match }?.second }
        ?: value

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayed,
            onValueChange = { onValueChange(it) }, // permite escribir custom directamente
            label = { Text(stringResource(R.string.task_category_hint)) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            predefined.forEach { (cat, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueChange(cat.key)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrioritySelector(selected: Priority, onSelect: (Priority) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.priority),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Priority.NONE, Priority.LOW, Priority.MEDIUM, Priority.HIGH).forEach { p ->
                val chipColor = priorityColor(p)
                FilterChip(
                    selected = selected == p,
                    onClick = { onSelect(p) },
                    label = { Text(priorityLabel(p)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor.copy(alpha = 0.25f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssigneePicker(
    members: List<com.uni.colabtasks.domain.model.ListMember>,
    assignedTo: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = members.firstOrNull { it.uid == assignedTo }?.label
        ?: stringResource(R.string.unassigned)

    Column {
        Text(
            text = stringResource(R.string.assign_to),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(6.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.unassigned)) },
                    onClick = { onSelect(null); expanded = false }
                )
                members.forEach { member ->
                    DropdownMenuItem(
                        text = { Text(member.label) },
                        onClick = { onSelect(member.uid); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentsSection(
    comments: List<Comment>,
    draft: String,
    currentUid: String?,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onDelete: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.comments_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.size(8.dp))

            if (comments.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_comments),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                comments.forEach { comment ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row {
                                Text(
                                    text = comment.authorName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    text = formatShortDate(comment.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (comment.authorUid == currentUid) {
                            IconButton(onClick = { onDelete(comment.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    placeholder = { Text(stringResource(R.string.comment_hint)) },
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )
                IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = stringResource(R.string.send),
                        tint = if (draft.isNotBlank()) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
