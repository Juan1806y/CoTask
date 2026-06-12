package com.uni.colabtasks.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uni.colabtasks.R
import com.uni.colabtasks.domain.model.ActivityAction
import com.uni.colabtasks.domain.model.ActivityEntry
import com.uni.colabtasks.ui.common.components.EmptyState
import com.uni.colabtasks.ui.common.components.LoadingIndicator
import com.uni.colabtasks.ui.util.formatShortDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onBack: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.activity_title), fontWeight = FontWeight.SemiBold)
                        if (state.listName.isNotBlank()) {
                            Text(
                                state.listName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> LoadingIndicator()
                state.entries.isEmpty() -> EmptyState(text = stringResource(R.string.empty_activity))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.entries, key = { it.id }) { entry ->
                        ActivityRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(entry: ActivityEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = actionIcon(entry.action),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actionText(entry),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatShortDate(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun actionIcon(action: ActivityAction): ImageVector = when (action) {
    ActivityAction.CREATED -> Icons.Outlined.Add
    ActivityAction.COMPLETED -> Icons.Outlined.CheckCircle
    ActivityAction.REOPENED -> Icons.Outlined.RadioButtonUnchecked
    ActivityAction.DELETED -> Icons.Outlined.Delete
    ActivityAction.EDITED -> Icons.Outlined.Edit
    ActivityAction.ASSIGNED -> Icons.Outlined.PersonAdd
}

@Composable
private fun actionText(entry: ActivityEntry): String {
    val res = when (entry.action) {
        ActivityAction.CREATED -> R.string.activity_created
        ActivityAction.COMPLETED -> R.string.activity_completed
        ActivityAction.REOPENED -> R.string.activity_reopened
        ActivityAction.DELETED -> R.string.activity_deleted
        ActivityAction.EDITED -> R.string.activity_edited
        ActivityAction.ASSIGNED -> R.string.activity_assigned
    }
    return stringResource(res, entry.actorName, entry.targetTitle)
}
