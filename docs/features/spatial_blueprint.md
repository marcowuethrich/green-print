# Feature: Spatial Blueprint UI

The Spatial Blueprint provides a visual way to manage plants based on their physical location in a room.

## 1. 2D Room Canvas
Each room is represented by a 2D canvas.

### User Stories
- As a user, I want to open a room and see a top-down view of its layout.
- As a user, I want to see plant icons representing the actual plants in that room.

## 2. Drag-and-Drop Placement
Interactive placement of plants.

### User Stories
- As a user, I want to drag a plant icon to its physical location in the room.
- As a user, I want the position of the plant to be saved automatically.

### Technical Notes
- **Coordinates**: Normalized `(x, y)` floats (0.0 to 1.0).
- **Persistence**: Coordinates are saved to the backend `Plant` entity.

## 3. Room Management
- **Rooms** act as the container for the 2D canvas.
- Deleting a room will prompt the user to move or archive the plants within it.
