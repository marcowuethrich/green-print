package ch.wuethrich.greenprint.space

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import ch.wuethrich.greenprint.model.Room
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
import greenprint.composeapp.generated.resources.ic_add
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

sealed interface SpaceModal {
    data object None : SpaceModal
    data object CreateRoom : SpaceModal
    data object RenameSpace : SpaceModal
    data class RenameRoom(val room: Room) : SpaceModal
    data object DeleteSpace : SpaceModal
    data class DeleteRoom(val room: Room) : SpaceModal
}

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

    var activeModal by remember { mutableStateOf<SpaceModal>(SpaceModal.None) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        mainFocusRequester.requestFocus()
    }

    // Handle one-time effects (like validation errors)
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SpaceUiEffect.ValidationError -> {
                    validationError = effect.message
                    isSaving = false
                }
            }
        }
    }

    // Auto-reset saving state and CLOSE MODAL on success
    LaunchedEffect(uiState) {
        if (isSaving && uiState is SpaceUiState.HasSpace && validationError == null) {
            isSaving = false
            activeModal = SpaceModal.None
        }
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
                            onLeave = onLeave,
                            onRenameClick = { 
                                validationError = null
                                activeModal = SpaceModal.RenameSpace 
                            },
                            onDeleteClick = { activeModal = SpaceModal.DeleteSpace }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            if (uiState is SpaceUiState.HasSpace) {
                FloatingActionButton(
                    onClick = { 
                        validationError = null
                        isSaving = false
                        activeModal = SpaceModal.CreateRoom 
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(painter = painterResource(Res.drawable.ic_add), contentDescription = "Add Room")
                }
            }
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
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (val state = uiState) {
                        SpaceUiState.Loading -> SpaceLoadingContent()
                        SpaceUiState.Empty -> SpaceEmptyContent()
                        is SpaceUiState.HasSpace -> SpaceDetailContent(
                            state = state,
                            onRenameRoomClick = { 
                                validationError = null
                                activeModal = SpaceModal.RenameRoom(it) 
                            },
                            onDeleteRoomClick = { activeModal = SpaceModal.DeleteRoom(it) },
                            onCreateRoomClick = {
                                validationError = null
                                activeModal = SpaceModal.CreateRoom
                            },
                            focusRequester = mainFocusRequester
                        )
                        is SpaceUiState.Error -> SpaceErrorContent(onRetry = viewModel::retry)
                    }
                }
            }
        }
    }

    // Modal Handling
    when (val modal = activeModal) {
        SpaceModal.None -> {}
        SpaceModal.CreateRoom -> {
            if (uiState is SpaceUiState.HasSpace) {
                CreateRoomModal(
                    error = validationError,
                    onConfirm = { name, icon ->
                        isSaving = true
                        viewModel.createRoom((uiState as SpaceUiState.HasSpace).space.id, name, icon)
                    },
                    onDismiss = { 
                        activeModal = SpaceModal.None
                        validationError = null
                    },
                    onClearError = { validationError = null }
                )
            }
        }
        SpaceModal.RenameSpace -> {
            if (uiState is SpaceUiState.HasSpace) {
                val space = (uiState as SpaceUiState.HasSpace).space
                RenameSpaceDialog(
                    currentName = space.name,
                    currentIcon = space.icon,
                    error = validationError,
                    onConfirm = { name, icon ->
                        isSaving = true
                        viewModel.updateSpace(space.id, name, icon)
                    },
                    onDismiss = { 
                        activeModal = SpaceModal.None
                        validationError = null
                    },
                    onClearError = { validationError = null }
                )
            }
        }
        is SpaceModal.RenameRoom -> {
            RenameRoomDialog(
                currentName = modal.room.name,
                currentIcon = modal.room.icon,
                error = validationError,
                onConfirm = { name, icon ->
                    isSaving = true
                    viewModel.updateRoom(modal.room.id, name, icon)
                },
                onDismiss = { 
                    activeModal = SpaceModal.None
                    validationError = null
                },
                onClearError = { validationError = null }
            )
        }
        SpaceModal.DeleteSpace -> {
            if (uiState is SpaceUiState.HasSpace) {
                DeleteSpaceDialog(
                    onConfirm = {
                        viewModel.deleteSpace((uiState as SpaceUiState.HasSpace).space.id)
                        onLeave()
                    },
                    onDismiss = { activeModal = SpaceModal.None }
                )
            }
        }
        is SpaceModal.DeleteRoom -> {
            DeleteRoomDialog(
                roomName = modal.room.name,
                onConfirm = {
                    viewModel.deleteRoom(modal.room.id)
                    activeModal = SpaceModal.None
                },
                onDismiss = { activeModal = SpaceModal.None }
            )
        }
    }
}

@Composable
private fun SpaceSettingsMenu(
    onLeave: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
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
                    onRenameClick()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.common_delete)) },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                },
                colors = androidx.compose.material3.MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

@Composable
private fun SpaceLoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SpaceEmptyContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 32.dp)
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
    onRenameRoomClick: (Room) -> Unit,
    onDeleteRoomClick: (Room) -> Unit,
    onCreateRoomClick: () -> Unit,
    focusRequester: FocusRequester
) {
    if (state.space.rooms.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(vertical = 48.dp, horizontal = 24.dp)
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
                    onClick = onCreateRoomClick,
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
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Rooms",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(state.space.rooms) { room ->
                RoomCard(
                    room = room,
                    onRenameClick = { onRenameRoomClick(room) },
                    onDeleteClick = { onDeleteRoomClick(room) }
                )
            }
        }
    }
}

@Composable
private fun RoomCard(
    room: Room,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(DesignSystem.CardBorder, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = IconRegistry.getPainter(room.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_more_vert),
                        contentDescription = "Room Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.common_rename)) },
                        onClick = {
                            showMenu = false
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.common_delete)) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        colors = androidx.compose.material3.MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateRoomModal(
    error: String?,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { 
            Text(
                text = "Add Room",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it 
                        onClearError()
                    },
                    label = { Text("Room name") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (name.isNotBlank()) {
                                onConfirm(name, selectedIcon)
                            }
                        }
                    )
                )
                
                IconPicker(
                    label = "Choose an icon (optional)",
                    availableIcons = IconRegistry.roomIcons,
                    selectedIcon = selectedIcon,
                    onIconSelected = { 
                        selectedIcon = if (it == selectedIcon) null else it 
                        onClearError()
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedIcon) },
                enabled = name.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(Res.string.common_save),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.common_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
    )
}

@Composable
private fun RenameRoomDialog(
    currentName: String,
    currentIcon: String?,
    error: String?,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
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
        title = { Text("Rename Room", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { 
                        textFieldValue = it 
                        onClearError()
                    },
                    label = { Text("Room name") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
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
                    availableIcons = IconRegistry.roomIcons,
                    selectedIcon = selectedIcon,
                    onIconSelected = { 
                        selectedIcon = if (it == selectedIcon) null else it 
                        onClearError()
                    }
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
private fun DeleteRoomDialog(
    roomName: String,
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
        title = { Text("Delete room?", fontWeight = FontWeight.Bold) },
        text = { Text("Are you sure you want to delete '$roomName'?") },
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

@Composable
private fun SpaceErrorContent(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 32.dp)
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
    error: String?,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
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
                    onValueChange = { 
                        textFieldValue = it 
                        onClearError()
                    },
                    label = { Text(stringResource(Res.string.space_name_hint)) },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
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
                    onIconSelected = { 
                        selectedIcon = if (it == selectedIcon) null else it 
                        onClearError()
                    }
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
