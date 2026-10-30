# Feature: Spatial Blueprint UI

The Spatial Blueprint provides a visual way to manage plants based on their physical location in a room.

## 1. 2D Room Canvas
Each room is represented by a 2D canvas.

### User Stories
- As a user, I want to open a room and see a top-down view of its layout.
- As a user, I want to see plant icons representing the actual plants in that room.

### Behaviour
- The room screen has a **List / Blueprint** switch above its content. List is the default; the choice survives configuration changes.
- The blueprint is a top-down floor (4:3, with a light grid) that shows every **non-archived** plant of the room as a marker: a pill with a leaf icon and the plant's name, centred on the plant's `(x, y)` and kept fully inside the floor.
- A marker whose position is not yet confirmed by the server (`syncStatus` `S_PENDING` or `S_ERROR`) shows a small warning-coloured dot.
- Tapping a marker opens the plant's existing **Edit Plant** dialog.
- Visuals use theme tokens only (`DesignSystem`, `MaterialTheme`); all strings come from `strings.xml` (ADR-012).

## 2. Drag-and-Drop Placement
Interactive placement of plants.

### User Stories
- As a user, I want to drag a plant icon to its physical location in the room.
- As a user, I want the position of the plant to be saved automatically.

### Behaviour
- **Drag**: the marker follows the pointer; the position is saved **once, when the marker is released**.
- **Keyboard** (ADR-013): Tab reaches a marker; the arrow keys move it by 5% of the room per press, and every press is saved.
- **Screen readers**: each marker announces "*Name* on the room blueprint" and offers the custom actions *Move left / right / up / down* (same 5% step). An unconfirmed position is announced as its state.
- Positions are clamped into the room before they are saved.
- **New plants** get a free spot instead of the corner: the client picks the cell of a 5 × 5 grid that is farthest from every plant already in the room (the centre for a room's first plant).

### Technical Notes
- **Coordinates**: Normalized `(x, y)` floats (0.0 to 1.0, edges included; ADR-008).
- **Persistence**: Coordinates are saved to the backend `Plant` entity. No schema change was needed (the `x`/`y` columns already exist), so there is no new Atlas migration.
- **API**: `PUT /plants/{id}/position` with `{"x": 0.25, "y": 0.75}` returns the updated plant. A coordinate outside 0.0..1.0 returns `400` with `{"code": "position_out_of_range", "field": "x" | "y"}`; the same check runs on `POST /rooms/{roomId}/plants` and `PUT /plants/{id}`. An unknown plant, or one in another user's space, returns `404`.
- **Offline-first write-through** (inventory.md §6.2): `PlantRepository.updatePlantPosition` first writes the new position to SQLDelight with `S_PENDING` and a fresh `updatedAt`, then calls the API. Success marks the row `S_SYNCED`, a failure `S_ERROR`; either only applies if no newer move of the same plant was written meanwhile (the row's `updatedAt` still matches). Offline, the move stays on the device (including across restarts) and the screen shows a snackbar; the next refresh of the room sends every unconfirmed move before it fetches, and a refresh never overwrites an unconfirmed row with the server's copy (Last Write Wins). A plant the server no longer knows is dropped from the cache.
- `PlantViewModel.movePlant` keeps one sync per plant in flight: a newer move of the same plant cancels the older request.
- The blueprint works on Android, iOS, Desktop and the web. On the web the cache is SQLite in a web worker, saved to IndexedDB (ADR-017), so an offline move survives a reload there too.

## 3. Room Management
- **Rooms** act as the container for the 2D canvas.
- Deleting a room that still has plants is refused for now: the app explains that the plants must be moved or archived first (`Room Not Empty` dialog). The server refuses it as well, with `409` `room_not_empty`, and counts archived plants too, since they are the room's history (inventory.md §4).

### Next Step
- Let the delete dialog offer **Move plants** (pick another room of the space) or **Archive plants** (with a reason, ADR-009) and finish the deletion afterwards. This needs a move endpoint for plants (`PUT /plants/{id}/room`) and a room picker in the client.
