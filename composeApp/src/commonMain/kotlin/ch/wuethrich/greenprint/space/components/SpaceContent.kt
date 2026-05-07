package ch.wuethrich.greenprint.space.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.space.SpaceUiState
import ch.wuethrich.greenprint.ui.components.ErrorScreen
import ch.wuethrich.greenprint.ui.components.LoadingScreen
import ch.wuethrich.greenprint.ui.motion.StaggerFadeUp
import greenprint.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SpaceLoadingContent(modifier: Modifier = Modifier) {
    LoadingScreen(modifier = modifier)
}

@Composable
fun SpaceEmptyContent(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(vertical = 32.dp).fillMaxSize(),
    ) {
        Text(
            text = stringResource(Res.string.space_no_selection),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
fun SpaceDetailContent(
    state: SpaceUiState.HasSpace,
    onRenameRoomClick: (Room) -> Unit,
    onDeleteRoomClick: (Room) -> Unit,
    onRoomClick: (Room) -> Unit,
    onCreateRoomClick: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    // Auto-focus the "Add Room" button when the space is empty
    if (state.space.rooms.isEmpty()) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(100.milliseconds)
            focusRequester.requestFocus()
        }
        
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        text = stringResource(Res.string.space_no_rooms_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(Res.string.space_no_rooms_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
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
                        text = stringResource(Res.string.space_add_first_room),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(Res.string.space_rooms_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            itemsIndexed(state.space.rooms) { index, room ->
                StaggerFadeUp(index = index) {
                    RoomCard(
                        room = room,
                        onRenameClick = { onRenameRoomClick(room) },
                        onDeleteClick = { onDeleteRoomClick(room) },
                        onClick = { onRoomClick(room) }
                    )
                }
            }
        }
    }
}

@Composable
fun SpaceErrorContent(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    ErrorScreen(onRetry = onRetry, modifier = modifier)
}
