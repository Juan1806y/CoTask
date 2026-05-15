package com.uni.colabtasks.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uni.colabtasks.R
import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.model.TaskCategory
import com.uni.colabtasks.domain.model.TaskCounts
import com.uni.colabtasks.domain.model.TaskFilter
import com.uni.colabtasks.ui.common.components.EmptyState
import com.uni.colabtasks.ui.common.components.LoadingIndicator
import com.uni.colabtasks.ui.util.formatShortDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onBack: () -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.list?.name.orEmpty(),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleListFavorite) {
                        Icon(
                            imageVector = if (state.list?.isFavorite == true) Icons.Outlined.Bookmark
                                         else Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(R.string.favorite)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            StatsRow(counts = state.counts)
            Spacer(Modifier.height(8.dp))
            FilterRow(current = state.filter, counts = state.counts, onChange = viewModel::setFilter)
            if (state.availableCategories.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                CategoryFilterRow(
                    categories = state.availableCategories,
                    selected = state.selectedCategory,
                    onSelect = viewModel::setCategory
                )
            }
            Spacer(Modifier.height(8.dp))
            NewTaskButton(onClick = onAddTask)
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> LoadingIndicator()
                    state.tasks.isEmpty() -> EmptyState(text = stringResource(R.string.empty_tasks))
                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items = state.tasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onClick = { onEditTask(task.id) },
                                onToggle = { viewModel.toggle(task) },
                                onDelete = { viewModel.delete(task) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(counts: TaskCounts) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(label = stringResource(R.string.stat_total), value = counts.total, modifier = Modifier.weight(1f))
        StatChip(label = stringResource(R.string.stat_pending), value = counts.pending, modifier = Modifier.weight(1f))
        StatChip(label = stringResource(R.string.stat_done), value = counts.done, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(current: TaskFilter, counts: TaskCounts, onChange: (TaskFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = current == TaskFilter.ALL,
            onClick = { onChange(TaskFilter.ALL) },
            label = { Text("${stringResource(R.string.filter_all)} (${counts.total})") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        FilterChip(
            selected = current == TaskFilter.PENDING,
            onClick = { onChange(TaskFilter.PENDING) },
            label = { Text("${stringResource(R.string.filter_pending)} (${counts.pending})") }
        )
        FilterChip(
            selected = current == TaskFilter.DONE,
            onClick = { onChange(TaskFilter.DONE) },
            label = { Text("${stringResource(R.string.filter_done)} (${counts.done})") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Category,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selected == null,
                    onClick = { onSelect(null) },
                    label = { Text(stringResource(R.string.filter_all_categories)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
            items(items = categories, key = { it }) { category ->
                FilterChip(
                    selected = selected.equals(category, ignoreCase = true),
                    onClick = { onSelect(category) },
                    label = { Text(categoryDisplayLabel(category)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

/**
 * Traduce el `key` almacenado en la tarea a la label visible:
 * - Si coincide con una categoría predefinida, usa la string localizada.
 * - Si es custom (texto libre del usuario), lo muestra tal cual.
 */
@Composable
private fun categoryDisplayLabel(category: String): String = when (TaskCategory.matchKey(category)) {
    TaskCategory.PERSONAL -> stringResource(R.string.category_personal)
    TaskCategory.WORK -> stringResource(R.string.category_work)
    TaskCategory.STUDY -> stringResource(R.string.category_study)
    TaskCategory.HOME -> stringResource(R.string.category_home)
    TaskCategory.HEALTH -> stringResource(R.string.category_health)
    TaskCategory.OTHER -> stringResource(R.string.category_other)
    null -> category
}

@Composable
private fun NewTaskButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(50)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.new_task))
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    // Surface no-clickable as wrapper. Sub-areas have their own click targets:
    //   - Checkbox  → toggle
    //   - Center content → edit (onClick)
    //   - Delete IconButton → delete
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() }
            )
            CategoryThumbnail(category = task.category)
            Spacer(Modifier.size(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick)
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (!task.category.isNullOrBlank()) {
                    Text(
                        text = categoryDisplayLabel(task.category),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!task.description.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                task.dueDate?.let { ms ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatShortDate(ms),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryThumbnail(category: String?) {
    val bg = MaterialTheme.colorScheme.primaryContainer
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Category,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (category.isNullOrBlank()) 0.4f else 1f)
        )
    }
}
