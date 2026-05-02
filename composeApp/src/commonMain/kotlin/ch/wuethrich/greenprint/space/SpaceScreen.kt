package ch.wuethrich.greenprint.space

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.wuethrich.greenprint.ui.IconPicker
import ch.wuethrich.greenprint.ui.IconRegistry
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.common_cancel
import greenprint.composeapp.generated.resources.common_delete
import greenprint.composeapp.generated.resources.common_rename
import greenprint.composeapp.generated.resources.common_retry
import greenprint.composeapp.generated.resources.common_save
import greenprint.composeapp.generated.resources.error_generic
import greenprint.composeapp.generated.resources.ic_leaf
import greenprint.composeapp.generated.resources.ic_more_vert
import greenprint.composeapp.generated.resources.space_create_button
import greenprint.composeapp.generated.resources.space_create_description
import greenprint.composeapp.generated.resources.space_create_title
import greenprint.composeapp.generated.resources.space_delete_dialog_message
import greenprint.composeapp.generated.resources.space_delete_dialog_title
import greenprint.composeapp.generated.resources.space_leave_action
import greenprint.composeapp.generated.resources.space_name_hint
import greenprint.composeapp.generated.resources.space_rename_dialog_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceScreen(
    onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SpaceViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var visible by remember { mutableStateOf(false) }
    val mainFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        visible = true
        mainFocusRequester.requestFocus()
    }

    val title = when (val state = uiState) {
        is SpaceUiState.HasSpace -> state.space.name
        else -> ""
    }
    
    val iconString = when (val state = uiState) {
        is SpaceUiState.HasSpace -> state.space.icon
        else -> null
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState is SpaceUiState.HasSpace) {
                            Icon(
                                painter = IconRegistry.getPainter(iconString),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        ) 
                    }
                },
                actions = {
                    if (uiState is SpaceUiState.HasSpace) {
                        SpaceSettingsMenu(
                            onRename = { newName, newIcon -> 
                                viewModel.updateSpace((uiState as SpaceUiState.HasSpace).space.id, newName, newIcon) 
                            },
                            onDelete = {
                                viewModel.deleteSpace((uiState as SpaceUiState.HasSpace).space.id)
                                onLeave()
                            },
                            onLeave = onLeave,
                            spaceName = (uiState as SpaceUiState.HasSpace).space.name,
                            spaceIcon = (uiState as SpaceUiState.HasSpace).space.icon
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(Brush.verticalGradient(DesignSystem.BackgroundGradient))
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (val state = uiState) {
                        SpaceUiState.Loading -> SpaceLoadingContent()
                        SpaceUiState.Empty -> SpaceEmptyContent(onCreate = { viewModel.createSpace(it) })
                        is SpaceUiState.HasSpace -> SpaceDetailContent(
                            state = state,
                            onRename = { newName, newIcon -> viewModel.updateSpace(state.space.id, newName, newIcon) },
                            onDelete = {
                                viewModel.deleteSpace(state.space.id)
                                onLeave()
                            },
                            onLeave = onLeave,
                            focusRequester = mainFocusRequester
                        )
                        is SpaceUiState.Error -> SpaceErrorContent(onRetry = viewModel::retry)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpaceSettingsMenu(
    onRename: (String, String?) -> Unit,
    onDelete: () -> Unit,
    onLeave: () -> Unit,
    spaceName: String,
    spaceIcon: String?
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                painter = painterResource(Res.drawable.ic_more_vert),
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.space_leave_action)) },
                onClick = {
                    showMenu = false
                    onLeave()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.common_rename)) },
                onClick = {
                    showMenu = false
                    showRenameDialog = true
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.common_delete)) },
                onClick = {
                    showMenu = false
                    showDeleteDialog = true
                },
                colors = androidx.compose.material3.MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error
                )
            )
        }
    }

    if (showRenameDialog) {
        RenameSpaceDialog(
            currentName = spaceName,
            currentIcon = spaceIcon,
            onConfirm = { newName, newIcon ->
                onRename(newName, newIcon)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showDeleteDialog) {
        DeleteSpaceDialog(
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun SpaceLoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SpaceEmptyContent(onCreate: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "No space selected",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun SpaceDetailContent(
    state: SpaceUiState.HasSpace,
    onRename: (String, String?) -> Unit,
    onDelete: () -> Unit,
    onLeave: () -> Unit,
    focusRequester: FocusRequester
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Empty state for rooms
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(vertical = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_leaf),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No Rooms Yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Divide your space into rooms to organize your plants.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Button(
                onClick = { /* TODO: Create room */ },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth(0.8f)
                    .focusRequester(focusRequester)
            ) {
                Text(
                    text = "Add Your First Room",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SpaceErrorContent(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.error_generic),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Text(stringResource(Res.string.common_retry))
        }
    }
}

@Composable
private fun RenameSpaceDialog(
    currentName: String,
    currentIcon: String?,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var textFieldValue by remember(currentName) {
        mutableStateOf(
            TextFieldValue(
                text = currentName,
                selection = TextRange(0, currentName.length)
            )
        )
    }
    var selectedIcon by remember(currentIcon) { mutableStateOf(currentIcon) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(Res.string.space_rename_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    label = { Text(stringResource(Res.string.space_name_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (textFieldValue.text.isNotBlank() && (textFieldValue.text != currentName || selectedIcon != currentIcon)) {
                                onConfirm(textFieldValue.text, selectedIcon)
                            }
                        }
                    )
                )
                
                IconPicker(
                    label = "Choose an icon (optional)",
                    availableIcons = IconRegistry.spaceIcons,
                    selectedIcon = selectedIcon,
                    onIconSelected = { selectedIcon = if (it == selectedIcon) null else it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(textFieldValue.text, selectedIcon) },
                enabled = textFieldValue.text.isNotBlank() && (textFieldValue.text != currentName || selectedIcon != currentIcon),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(Res.string.common_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun DeleteSpaceDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(Res.string.space_delete_dialog_title), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(Res.string.space_delete_dialog_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(Res.string.common_delete), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.focusRequester(cancelFocusRequester)
            ) {
                Text(stringResource(Res.string.common_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}
