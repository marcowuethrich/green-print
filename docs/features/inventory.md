# Feature: Plant & Space Inventory

This document defines the requirements and technical considerations for the first phase of the GreenPrint inventory system.

## 1. Welcome Screen
The initial entry point of the application.

### User Stories
- As a user, I want to see a welcoming screen with the name of the app (**GreenPrint**) when I open it.
- As a user, I want to see my existing living space(s) listed clearly on this screen.
- As a user, I want a clear call-to-action to "Create your first living space" if I don't have one yet.

### Flow & Restrictions (Frontend)
- **First Impression**: The screen serves as the launchpad for the user's plant management journey.
- **Empty State**: If no living space exists, a prominent "Create your first living space" button is shown.
- **Populated State**: If a living space exists, it is displayed in a list. The "Create" button is hidden/disabled due to the single-space restriction.

## 2. Living Space Management
Users can define where their plants live. 

### User Stories
- As a user, I want to create a profile for my living space (e.g., "My Apartment").
- As a user, I want to optionally choose an icon for my living space from a predefined list (e.g., House, Apartment, Office).
- As a user, I want to rename my living space if I move or change its name.
- As a user, I want to change or remove the custom icon for my space at any time.
- As a user, I want to enter a living space from the Welcome Screen to manage its rooms and plants.
- As a user, I want to leave a living space to return to the Welcome Screen.
- As a user, I want to add rooms to my space (e.g., "Living Room", "Balcony").
- As a user, I want to optionally choose an icon for each room (e.g., Sofa for Living Room, Bed for Bedroom).
- As a user, I want to rename or delete rooms if my layout changes.

### Flow & Restrictions (Frontend)
- **Single Space Limit**: Initially, the user can only create **one** living space. This restriction is enforced in the frontend (the "Create" button is hidden/disabled on the Welcome Screen if a space exists).
- **Icon Selection**: 
    - During creation, the user can skip icon selection.
    - If skipped, the backend will assign a default icon based on the entity type (Space or Room).
    - The icon picker should show a categorized list (Space icons vs Room icons).
- **Auto-Entry**: After creating a living space, the user is automatically "entered" into that space and navigated to the list of rooms within it.
- **Navigation**: 
    - Leaving a space returns the user to the Welcome Screen (list of existing living spaces).
    - Since only one space is currently allowed, the user will see their single space but cannot create a second one.
- **Settings Menu**: The "Leave" and "Delete" actions for a living space must be tucked away behind a **Settings** menu within the space view to prevent accidental triggers.

### Technical Notes
- **Entities**: 
    - `Space` (UUID, Name, Icon: String?).
    - `Room` (UUID, Name, Icon: String?).
- **Icon Library**:
    - Icons are hardcoded in the frontend and referenced by a unique string ID (e.g., `house_24`, `bedroom_24`).
    - The backend stores this ID. If `null` is sent during creation, the backend provides a default (e.g., `default_space`, `default_room`).
- **Validation**: 
    - **Space Names**: Must be unique for the **same user**. Different users can reuse space names from other users.
    - **Room Names**: Must be unique within a space. Different users may reuse the same room names in their own spaces.

## 3. Plant CRUD
The core of the app is managing individual plants.

### User Stories
- As a user, I want to add a plant by entering its name, species, pot size, and soil type.
- As a user, I want to assign every plant to exactly one room.
- As a user, I want to update a plant's details (e.g., when I repot it) or delete it.

### Technical Notes
- **Entity**: `Plant` (UUID, Name, Species, PotSize, SoilType, RoomID).
- **Pot Size & Soil**: Initially manual text entry; will move to categorized enums/entities in a later phase.

## 4. Basic Watering Tracking
Keeping plants alive through scheduled hydration.

### User Stories
- As a user, I want to set a manual watering frequency (in days) for each plant.
- As a user, I want to click a "Watered" button to log a watering event for today.
- As a user, I want the app to tell me when the next watering is due.

### Technical Notes
- **Logic**: 
    - `next_due = last_event_timestamp + (watering_interval * 24h)`.
- **UI State**: Plants should be sorted by "Urgency" (Overdue > Due > Healthy).

## 5. Watering Barometer
Visual hydration status at a glance.

### User Stories
- As a user, I want to see a visual barometer that shows how "thirsty" my plant is.
- As a user, I want the barometer to decrease automatically as time passes since the last watering.

### Technical Notes
- **Calculation**: `percentage = (next_due - now) / watering_interval`.
- **UI**: Color shift from Blue (Full) to Red (Thirsty/Overdue).

## 6. Archive System
Handling the "end of life" for plants.

### User Stories
- As a user, I want to move a plant to the "Archive" when it dies or is given away.
- As a user, I want to record why the plant was archived.
- As a user, I want to browse my archived plants in a read-only gallery.

### Technical Notes
- **Required Fields**: `Reason` (Died, Gifted, Sold), `ArchiveDate`.
- **State**: Archived plants are excluded from the "Active" Blueprint and Watering lists.
