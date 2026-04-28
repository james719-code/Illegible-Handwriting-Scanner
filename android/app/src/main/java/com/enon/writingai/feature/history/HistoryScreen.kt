package com.enon.writingai.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.enon.writingai.ui.components.AppSecondaryButton
import com.enon.writingai.ui.common.AppScreenScaffold

private enum class HistoryFilter {
    All,
    Recent,
    WithImage,
}

@Composable
fun HistoryScreen(
    onBack: (() -> Unit)? = null,
    onEntryClick: (String) -> Unit,
    viewModel: HistoryViewModel = viewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<HistoryEntry?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.All) }
    val filterScrollState = rememberScrollState()

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshEntries()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val filteredEntries = remember(viewModel.uiState.entries, query, filter) {
        val normalizedQuery = query.trim().lowercase()
        viewModel.uiState.entries
            .filter { entry ->
                val matchesText = normalizedQuery.isBlank() ||
                    entry.title.lowercase().contains(normalizedQuery) ||
                    entry.subtitle.lowercase().contains(normalizedQuery)
                val matchesFilter = when (filter) {
                    HistoryFilter.All -> true
                    HistoryFilter.Recent -> true
                    HistoryFilter.WithImage -> !entry.imageUri.isNullOrBlank()
                }
                matchesText && matchesFilter
            }
            .let { items ->
                if (filter == HistoryFilter.Recent) {
                    items.take(6)
                } else {
                    items
                }
            }
    }

    AppScreenScaffold(title = "History", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HistoryOverviewCard(
                totalEntries = viewModel.uiState.entries.size,
                filteredEntries = filteredEntries.size,
            )

            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                    )
                },
                placeholder = { Text("Search your archive...") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(filterScrollState),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilterChipItem(
                    label = "All",
                    selected = filter == HistoryFilter.All,
                    onClick = { filter = HistoryFilter.All },
                )
                FilterChipItem(
                    label = "Recent",
                    selected = filter == HistoryFilter.Recent,
                    onClick = { filter = HistoryFilter.Recent },
                )
                FilterChipItem(
                    label = "With image",
                    selected = filter == HistoryFilter.WithImage,
                    onClick = { filter = HistoryFilter.WithImage },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ModeButton(
                    selected = viewModel.uiState.mode == HistoryViewMode.List,
                    label = "List",
                    icon = Icons.AutoMirrored.Filled.ViewList,
                    onClick = { viewModel.setMode(HistoryViewMode.List) },
                )
                ModeButton(
                    selected = viewModel.uiState.mode == HistoryViewMode.Card,
                    label = "Card",
                    icon = Icons.Default.GridView,
                    onClick = { viewModel.setMode(HistoryViewMode.Card) },
                )
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
                    shadowElevation = 1.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add history record")
                        }
                        Text(
                            text = "Quick add",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }

            Text(
                text = if (filteredEntries.isEmpty()) "No saved scans yet" else "${filteredEntries.size} saved scans",
                style = MaterialTheme.typography.headlineMedium,
            )

            if (filteredEntries.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Scan or import more pages to grow your archive.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(filteredEntries, key = { _, item -> item.id }) { index, entry ->
                        if (viewModel.uiState.mode == HistoryViewMode.Card) {
                            HistoryCardItem(
                                entry = entry,
                                index = index,
                                onOpen = { onEntryClick(entry.id) },
                                onEdit = { editTarget = entry },
                                onDelete = { viewModel.deleteEntry(entry.id) },
                            )
                        } else {
                            HistoryListItem(
                                entry = entry,
                                index = index,
                                onOpen = { onEntryClick(entry.id) },
                                onEdit = { editTarget = entry },
                                onDelete = { viewModel.deleteEntry(entry.id) },
                            )
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            HistoryEntryDialog(
                title = "Create history entry",
                confirmLabel = "Create",
                initialName = "",
                initialImageUri = "",
                onDismiss = { showCreateDialog = false },
                onConfirm = { name, imageUri ->
                    viewModel.createEntry(name, imageUri)
                    showCreateDialog = false
                },
            )
        }

        editTarget?.let { target ->
            HistoryEntryDialog(
                title = "Update title",
                confirmLabel = "Update",
                initialName = target.title,
                initialImageUri = target.imageUri ?: "",
                onDismiss = { editTarget = null },
                onConfirm = { name, _ ->
                    viewModel.updateEntryTitle(target.id, name)
                    editTarget = null
                },
                readOnlyImageUri = true,
            )
        }
    }
}

@Composable
private fun HistoryOverviewCard(
    totalEntries: Int,
    filteredEntries: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Archive overview",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "$filteredEntries visible of $totalEntries saved results",
                style = MaterialTheme.typography.headlineMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HistoryStatChip(
                    label = "Visible",
                    value = filteredEntries.toString(),
                    icon = Icons.Default.Visibility,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                HistoryStatChip(
                    label = "Saved",
                    value = totalEntries.toString(),
                    icon = Icons.Default.History,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HistoryStatChip(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.16f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.12f),
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = null,
    )
}

@Composable
private fun RowScope.ModeButton(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.weight(1f),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun HistoryCardItem(
    entry: HistoryEntry,
    index: Int,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (index % 2 == 0) {
                MaterialTheme.colorScheme.surfaceContainerLowest
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HistoryImage(imageUri = entry.imageUri)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = entry.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TypeTag(label = if (entry.imageUri.isNullOrBlank()) "OCR" else "IMAGE")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
private fun HistoryListItem(
    entry: HistoryEntry,
    index: Int,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (index % 2 == 0) {
                MaterialTheme.colorScheme.surfaceContainerLowest
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HistoryImage(
                imageUri = entry.imageUri,
                modifier = Modifier
                    .height(88.dp)
                    .aspectRatio(1f),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TypeTag(label = if (entry.imageUri.isNullOrBlank()) "OCR" else "IMAGE")
            }
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
private fun TypeTag(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun HistoryImage(
    imageUri: String?,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16 / 9f),
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        if (imageUri.isNullOrBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            AsyncImage(
                model = imageUri,
                contentDescription = "History image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun HistoryEntryDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialImageUri: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, imageUri: String) -> Unit,
    readOnlyImageUri: Boolean = false,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var imageUri by remember(initialImageUri) { mutableStateOf(initialImageUri) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Entry title") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = imageUri,
                    onValueChange = { imageUri = it },
                    label = { Text("Optional image URI") },
                    enabled = !readOnlyImageUri,
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, imageUri) }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(text = confirmLabel, modifier = Modifier.padding(start = 6.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
