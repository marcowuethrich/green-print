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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.wuethrich.greenprint.model.Room
import ch.wuethrich.greenprint.space.SpaceUiState
import ch.wuethrich.greenprint.ui.components.RequestInitialFocus
import ch.wuethrich.greenprint.ui.motion.StaggerFadeUp
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.ic_leaf
import greenprint.composeapp.generated.resources.space_add_first_room
import greenprint.composeapp.generated.resources.space_no_rooms_description
import greenprint.composeapp.generated.resources.space_no_rooms_title
import greenprint.composeapp.generated.resources.space_no_selection
import greenprint.composeapp.generated.resources.space_rooms_section_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val CALL_TO_ACTION_WIDTH = 0.8f

@Composable
fun SpaceEmptyContent(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignSystem.Space4),
        modifier = modifier.padding(vertical = DesignSystem.Space6).fillMaxSize(),
    ) {
        Text(
            text = stringResource(Res.string.space_no_selection),
            style = MaterialTheme.typography.titleLarge,
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
    modifier: Modifier = Modifier,
) {
    if (state.space.rooms.isEmpty()) {
        NoRoomsContent(onCreateRoomClick = onCreateRoomClick, modifier = modifier)
    } else {
        RoomList(
            rooms = state.space.rooms,
            onRoomClick = onRoomClick,
            onRenameRoomClick = onRenameRoomClick,
            onDeleteRoomClick = onDeleteRoomClick,
            modifier = modifier,
        )
    }
}

/** The first-run state of a space; focus starts on its one action (ADR-013). */
@Composable
private fun NoRoomsContent(onCreateRoomClick: () -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    RequestInitialFocus(focusRequester)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(vertical = DesignSystem.Space7, horizontal = DesignSystem.Space5),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_leaf),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(Res.string.space_no_rooms_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(Res.string.space_no_rooms_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Button(
                onClick = onCreateRoomClick,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth(CALL_TO_ACTION_WIDTH)
                    .focusRequester(focusRequester),
            ) {
                Text(
                    text = stringResource(Res.string.space_add_first_room),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun RoomList(
    rooms: List<Room>,
    onRoomClick: (Room) -> Unit,
    onRenameRoomClick: (Room) -> Unit,
    onDeleteRoomClick: (Room) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(DesignSystem.Space5),
        verticalArrangement = Arrangement.spacedBy(DesignSystem.Space4),
    ) {
        item {
            Text(
                text = stringResource(Res.string.space_rooms_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = DesignSystem.Space2),
            )
        }
        itemsIndexed(rooms, key = { _, room -> room.id }) { index, room ->
            StaggerFadeUp(index = index) {
                RoomCard(
                    room = room,
                    onRenameClick = { onRenameRoomClick(room) },
                    onDeleteClick = { onDeleteRoomClick(room) },
                    onClick = { onRoomClick(room) },
                )
            }
        }
    }
}
