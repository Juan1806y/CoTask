package com.uni.colabtasks.ui.tasklists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.uni.colabtasks.R

@Composable
fun EditListDialog(
    state: TaskListDialogState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onContributorEmailChange: (String) -> Unit,
    onAddContributor: () -> Unit,
    onRemoveContributor: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                if (state.editingId == null) R.string.dialog_new_list_title
                                else R.string.dialog_edit_list_title
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (state.editingId == null) {
                            Text(
                                text = stringResource(R.string.dialog_new_list_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.list_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.list_description_hint)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.add_contributor),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.newContributorEmail,
                        onValueChange = onContributorEmailChange,
                        label = { Text(stringResource(R.string.contributor_email_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onAddContributor) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_contributor))
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (state.contributors.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.heightIn(max = 140.dp)
                    ) {
                        items(state.contributors, key = { it }) { email ->
                            AssistChip(
                                onClick = { onRemoveContributor(email) },
                                label = { Text(email) },
                                trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Actions
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            stringResource(
                                if (state.editingId == null) R.string.create_list else R.string.save
                            )
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}
