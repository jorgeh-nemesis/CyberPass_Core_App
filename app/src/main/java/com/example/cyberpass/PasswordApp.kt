package com.example.cyberpass

import android.content.ClipData
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordApp(viewModel: MainViewModel) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showOnlyFavorites by viewModel.showOnlyFavorites.collectAsStateWithLifecycle()

    val showAddDialog = remember { mutableStateOf(false) }
    val showSettings = remember { mutableStateOf(false) }
    var isReorderMode by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val editingEntry = remember { mutableStateOf<PasswordEntry?>(null) }
    val sharingEntry = remember { mutableStateOf<PasswordEntry?>(null) }
    val entryToDelete = remember { mutableStateOf<PasswordEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val passwordCopiedMsg = stringResource(R.string.password_copied)
    val usernameCopiedMsg = stringResource(R.string.username_copied)
    val backupSuccessMsg = stringResource(R.string.backup_successful)
    val backupFailedMsg = stringResource(R.string.backup_failed)
    val restoreSuccessMsg = stringResource(R.string.restore_successful)
    val restoreFailedMsg = stringResource(R.string.restore_failed)

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveEntry(from.index, to.index)
    }

    Scaffold(
        containerColor = Color(0xFF111111),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (isSearching) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { 
                                    Text(
                                        stringResource(R.string.search),
                                        color = Color.White.copy(alpha = 0.7f)
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFF2EFC54),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        } else {
                            Row {
                                Text(
                                    text = "Cyber",
                                    style = TextStyle(
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                )
                                Text(
                                    text = "Pass",
                                    style = TextStyle(
                                        color = Color(0xFF2EFC54),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF111111),
                        titleContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { 
                            isSearching = !isSearching
                            if (!isSearching) viewModel.setSearchQuery("")
                        }) {
                            Icon(
                                if (isSearching) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { viewModel.toggleShowOnlyFavorites() }) {
                            Icon(
                                if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Show favorites",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { isReorderMode = !isReorderMode }
                        ) {
                            Icon(
                                Icons.Default.Reorder, 
                                contentDescription = "Toggle Reorder Mode",
                                tint = if (isReorderMode) Color(0xFF2EFC54) else Color.White
                            )
                        }
                        IconButton(onClick = { showSettings.value = true }) {
                            Icon(
                                Icons.Default.Settings, 
                                contentDescription = stringResource(R.string.settings),
                                tint = Color.White
                            )
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog.value = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_entry))
            }
        }
    ) { padding: PaddingValues ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No results found" else stringResource(R.string.no_passwords_yet),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
                    .padding(padding)
            ) {
                itemsIndexed(entries, key = { _, item -> item.id }) { _, entry ->
                    ReorderableItem(reorderableLazyListState, key = entry.id) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                        EntryCard(
                            entry = entry,
                            onEdit = { editingEntry.value = entry },
                            onDelete = { entryToDelete.value = entry },
                            onCopy = {
                                scope.launch {
                                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("password", entry.password)))
                                }
                                Toast.makeText(context, passwordCopiedMsg, Toast.LENGTH_SHORT).show()
                            },
                            onCopyUsername = {
                                scope.launch {
                                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("username", entry.username)))
                                }
                                Toast.makeText(context, usernameCopiedMsg, Toast.LENGTH_SHORT).show()
                            },
                            onShare = { sharingEntry.value = entry },
                            showDragHandle = isReorderMode,
                            dragHandleModifier = if (isReorderMode) Modifier.draggableHandle() else Modifier,
                            modifier = Modifier.shadow(elevation)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog.value) {
        EntryDialog(
            onDismiss = { showAddDialog.value = false },
            onSave = { title, username, password, notes, category, isFavorite ->
                viewModel.addEntry(
                    PasswordEntry(
                        title = title,
                        username = username,
                        password = password,
                        notes = notes,
                        category = category,
                        isFavorite = isFavorite
                    )
                )
                showAddDialog.value = false
            }
        )
    }

    if (editingEntry.value != null) {
        EntryDialog(
            onDismiss = { editingEntry.value = null },
            initialEntry = editingEntry.value,
            onSave = { title, username, password, notes, category, isFavorite ->
                editingEntry.value?.let { entry ->
                    viewModel.updateEntry(
                        entry.copy(
                            title = title,
                            username = username,
                            password = password,
                            notes = notes,
                            category = category,
                            isFavorite = isFavorite
                        )
                    )
                }
                editingEntry.value = null
            }
        )
    }

    if (showSettings.value) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSettings.value = false },
            sheetState = sheetState
        ) {
            SettingsBottomSheet(
                viewModel = viewModel,
                onBackupComplete = { success: Boolean ->
                    showSettings.value = false
                    scope.launch {
                        snackbarHostState.showSnackbar(if (success) backupSuccessMsg else backupFailedMsg)
                    }
                },
                onRestoreComplete = { success: Boolean ->
                    showSettings.value = false
                    scope.launch {
                        snackbarHostState.showSnackbar(if (success) restoreSuccessMsg else restoreFailedMsg)
                    }
                }
            )
        }
    }

    if (sharingEntry.value != null) {
        QrShareDialog(
            entry = sharingEntry.value!!,
            onDismiss = { sharingEntry.value = null }
        )
    }

    if (entryToDelete.value != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete.value = null },
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_confirmation_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        entryToDelete.value?.let { viewModel.deleteEntry(it) }
                        entryToDelete.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete.value = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
