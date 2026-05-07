package ch.wuethrich.greenprint.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.space.SpaceScreen
import ch.wuethrich.greenprint.space.SpaceUiEffect
import ch.wuethrich.greenprint.space.SpaceUiState
import ch.wuethrich.greenprint.space.SpaceViewModel
import ch.wuethrich.greenprint.ui.IconPicker
import ch.wuethrich.greenprint.ui.IconRegistry
import ch.wuethrich.greenprint.ui.motion.softPress
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.common_cancel
import greenprint.composeapp.generated.resources.common_retry
import greenprint.composeapp.generated.resources.common_save
import greenprint.composeapp.generated.resources.error_generic
import greenprint.composeapp.generated.resources.ic_leaf
import greenprint.composeapp.generated.resources.space_create_button
import greenprint.composeapp.generated.resources.space_create_title
import greenprint.composeapp.generated.resources.space_name_hint
import greenprint.composeapp.generated.resources.space_section_label
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

class WelcomeScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: SpaceViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        var showCreateModal by remember { mutableStateOf(false) }
        var visible by remember { mutableStateOf(false) }
        var isCreating by remember { mutableStateOf(false) }
        val welcomeFocusRequester = remember { FocusRequester() }
        
        var validationError by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            visible = true
        }

        // Handle one-time effects (like validation errors)
        LaunchedEffect(Unit) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    is SpaceUiEffect.ValidationError -> {
                        validationError = effect.message
                        isCreating = false
                        showCreateModal = true
                    }
                    is SpaceUiEffect.RoomHasPlants -> {
                        // Not expected here, but must be exhaustive
                    }
                }
            }
        }

        // Handle auto-focus when visible and not in modal
        LaunchedEffect(visible, showCreateModal, uiState) {
            if (visible && !showCreateModal && uiState !is SpaceUiState.Loading) {
                kotlinx.coroutines.delay(100.milliseconds)
                welcomeFocusRequester.requestFocus()
            }
        }

        // Auto-navigate after successful creation
        LaunchedEffect(uiState) {
            val state = uiState
            if (isCreating && state is SpaceUiState.HasSpace && validationError == null) {
                isCreating = false
                showCreateModal = false
                navigator.push(SpaceScreen())
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_leaf),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "The World",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent,
            modifier = Modifier.background(Brush.verticalGradient(DesignSystem.BackgroundGradient))
        ) { innerPadding ->
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (val state = uiState) {
                            SpaceUiState.Loading -> WelcomeLoadingContent()
                            SpaceUiState.Empty -> WelcomeEmptyContent(
                                onCreateClick = { 
                                    validationError = null
                                    showCreateModal = true 
                                },
                                focusRequester = welcomeFocusRequester
                            )
                            is SpaceUiState.HasSpace -> WelcomePopulatedContent(
                                state = state,
                                onEnter = { navigator.push(SpaceScreen()) },
                                focusRequester = welcomeFocusRequester
                            )
                            is SpaceUiState.Error -> WelcomeErrorContent(onRetry = viewModel::retry)
                        }
                    }
                }
            }
        }

        if (showCreateModal) {
            CreateSpaceModal(
                error = validationError,
                onConfirm = { name, icon ->
                    validationError = null
                    isCreating = true
                    viewModel.createSpace(name, icon)
                    // Modal stays open until success or error
                },
                onDismiss = { 
                    showCreateModal = false
                    validationError = null
                },
                onClearError = {
                    validationError = null
                }
            )
        }
    }
}

@Composable
private fun WelcomeLoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun WelcomeEmptyContent(
    onCreateClick: () -> Unit,
    focusRequester: FocusRequester
) {
    val ctaInteraction = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_leaf),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Welcome to GreenPrint",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start your plant journey by creating a space.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Button(
            onClick = onCreateClick,
            interactionSource = ctaInteraction,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .softPress(ctaInteraction)
        ) {
            Text(
                text = stringResource(Res.string.space_create_button),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WelcomePopulatedContent(
    state: SpaceUiState.HasSpace,
    onEnter: (Space) -> Unit,
    focusRequester: FocusRequester
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(Res.string.space_section_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            onClick = { onEnter(state.space) },
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            elevation = CardDefaults.cardElevation(
                defaultElevation = DesignSystem.CardElevation,
                hoveredElevation = DesignSystem.CardElevationHovered,
            ),
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = IconRegistry.getPainter(state.space.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = state.space.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap to enter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeErrorContent(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.error_generic),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(stringResource(Res.string.common_retry))
        }
    }
}

@Composable
private fun CreateSpaceModal(
    error: String?,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.yield()
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { 
            Text(
                text = stringResource(Res.string.space_create_title),
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
                            if (name.isNotBlank()) {
                                onConfirm(name, selectedIcon)
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
