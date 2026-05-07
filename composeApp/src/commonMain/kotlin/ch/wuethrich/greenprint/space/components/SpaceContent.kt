package ch.wuethrich.greenprint.space.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.common_retry
import greenprint.composeapp.generated.resources.error_generic
import greenprint.composeapp.generated.resources.ic_leaf
import greenprint.composeapp.generated.resources.space_add_first_room
import greenprint.composeapp.generated.resources.space_no_rooms_description
import greenprint.composeapp.generated.resources.space_no_rooms_title
import greenprint.composeapp.generated.resources.space_no_selection
import greenprint.composeapp.generated.resources.space_rooms_section_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SpaceLoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
    }
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
            items(state.space.rooms) { room ->
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

@Composable
fun SpaceErrorContent(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(vertical = 32.dp).fillMaxSize()
    ) {
        Text(
            text = stringResource(Res.string.error_generic),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Button(
            onClick = onRetry, 
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(Res.string.common_retry))
        }
    }
}
