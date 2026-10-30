package ch.wuethrich.greenprint.welcome

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ch.wuethrich.greenprint.model.ErrorReason
import ch.wuethrich.greenprint.model.Space
import ch.wuethrich.greenprint.space.SpaceScreen
import ch.wuethrich.greenprint.space.SpaceUiEffect
import ch.wuethrich.greenprint.space.SpaceUiState
import ch.wuethrich.greenprint.space.SpaceViewModel
import ch.wuethrich.greenprint.ui.IconRegistry
import ch.wuethrich.greenprint.ui.components.ErrorScreen
import ch.wuethrich.greenprint.ui.components.GenericFormModal
import ch.wuethrich.greenprint.ui.components.GreenPrintScaffold
import ch.wuethrich.greenprint.ui.components.LoadingScreen
import ch.wuethrich.greenprint.ui.motion.PageEntrance
import ch.wuethrich.greenprint.ui.motion.softPress
import ch.wuethrich.greenprint.ui.text
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.app_name
import greenprint.composeapp.generated.resources.common_icon_picker_label
import greenprint.composeapp.generated.resources.ic_leaf
import greenprint.composeapp.generated.resources.space_create_button
import greenprint.composeapp.generated.resources.space_create_title
import greenprint.composeapp.generated.resources.space_name_hint
import greenprint.composeapp.generated.resources.space_section_label
import greenprint.composeapp.generated.resources.welcome_description
import greenprint.composeapp.generated.resources.welcome_tap_to_enter
import greenprint.composeapp.generated.resources.welcome_title
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

/** The create-space dialog: open or not, saving or not, and why the server rejected it. */
@Stable
private class CreateSpaceState {
    var isOpen by mutableStateOf(false)
        private set
    var validationError: ErrorReason? by mutableStateOf(null)
    private var isCreating by mutableStateOf(false)

    fun open() {
        validationError = null
        isOpen = true
    }

    fun close() {
        isOpen = false
        validationError = null
    }

    /** The dialog stays open until the space is created or rejected. */
    fun submitting() {
        validationError = null
        isCreating = true
    }

    fun rejected(reason: ErrorReason) {
        validationError = reason
        isCreating = false
        isOpen = true
    }

    /** True once, when the space this dialog submitted exists. */
    fun consumeCreated(): Boolean {
        if (!isCreating || validationError != null) return false
        isCreating = false
        isOpen = false
        return true
    }
}

class WelcomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: SpaceViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val createSpace = remember { CreateSpaceState() }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(viewModel.uiEffect) {
            viewModel.uiEffect.collect { effect ->
                if (effect is SpaceUiEffect.ValidationError) createSpace.rejected(effect.reason)
            }
        }
        // Focus the screen's one action once it shows and no dialog covers it (ADR-013).
        LaunchedEffect(createSpace.isOpen, uiState) {
            if (!createSpace.isOpen && (uiState is SpaceUiState.Empty || uiState is SpaceUiState.HasSpace)) {
                delay(DesignSystem.Motion.DURATION_INSTANT_MS.milliseconds)
                focusRequester.requestFocus()
            }
        }
        // A newly created space opens straight away.
        LaunchedEffect(uiState) {
            if (uiState is SpaceUiState.HasSpace && createSpace.consumeCreated()) navigator.push(SpaceScreen())
        }

        GreenPrintScaffold(
            title = stringResource(Res.string.app_name),
            titleIcon = painterResource(Res.drawable.ic_leaf),
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                PageEntrance {
                    WelcomeBody(
                        uiState = uiState,
                        focusRequester = focusRequester,
                        onCreateClick = createSpace::open,
                        onEnter = { navigator.push(SpaceScreen()) },
                        onRetry = viewModel::retry,
                    )
                }
            }
        }

        if (createSpace.isOpen) {
            CreateSpaceModal(
                error = createSpace.validationError?.let { stringResource(it.text) },
                onConfirm = { name, icon ->
                    createSpace.submitting()
                    viewModel.createSpace(name, icon)
                },
                onDismiss = createSpace::close,
                onClearError = { createSpace.validationError = null },
            )
        }
    }
}

@Composable
private fun WelcomeBody(
    uiState: SpaceUiState,
    focusRequester: FocusRequester,
    onCreateClick: () -> Unit,
    onEnter: (Space) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = DesignSystem.ContentMaxWidth)
            .fillMaxWidth()
            .padding(horizontal = DesignSystem.Space5, vertical = DesignSystem.Space6),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState) {
            SpaceUiState.Loading -> LoadingScreen()

            SpaceUiState.Empty -> WelcomeEmptyContent(onCreateClick = onCreateClick, focusRequester = focusRequester)

            is SpaceUiState.HasSpace -> WelcomePopulatedContent(
                space = uiState.space,
                onEnter = onEnter,
                focusRequester = focusRequester,
            )

            is SpaceUiState.Error -> ErrorScreen(onRetry = onRetry, message = stringResource(uiState.reason.text))
        }
    }
}

@Composable
private fun WelcomeEmptyContent(
    onCreateClick: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val ctaInteraction = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignSystem.Space5),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_leaf),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(DesignSystem.Space8),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(DesignSystem.Space2))
            Text(
                text = stringResource(Res.string.welcome_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
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
                .softPress(ctaInteraction),
        ) {
            Text(
                text = stringResource(Res.string.space_create_button),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun WelcomePopulatedContent(
    space: Space,
    onEnter: (Space) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(DesignSystem.Space5),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(Res.string.space_section_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )

        Card(
            onClick = { onEnter(space) },
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
                modifier = Modifier.padding(DesignSystem.Space5),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(DesignSystem.Space7)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = IconRegistry.getPainter(space.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(DesignSystem.Space5),
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = space.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.welcome_tap_to_enter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateSpaceModal(
    error: String?,
    onConfirm: (name: String, icon: String?) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
) {
    GenericFormModal(
        title = stringResource(Res.string.space_create_title),
        nameLabel = stringResource(Res.string.space_name_hint),
        availableIcons = IconRegistry.spaceIcons,
        iconLabel = stringResource(Res.string.common_icon_picker_label),
        error = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        onClearError = onClearError,
    )
}
