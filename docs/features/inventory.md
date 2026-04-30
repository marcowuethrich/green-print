# Feature: Plant & Space Inventory

This document defines the requirements and technical considerations for the first phase of the GreenPrint inventory system.

## 1. Living Space Management
Users can define where their plants live. Initially, we support a single "Living Space" per account.

### User Stories
- As a user, I want to create a profile for my living space (e.g., "My Apartment").
- As a user, I want to add rooms to my space (e.g., "Living Room", "Balcony").
- As a user, I want to rename or delete rooms if my layout changes.

### Technical Notes
- **Entity**: `Room` (UUID, Name).
- **Validation**: Room names must be unique within a space.

## 2. Plant CRUD
The core of the app is managing individual plants.

### User Stories
- As a user, I want to add a plant by entering its name, species, pot size, and soil type.
- As a user, I want to assign every plant to exactly one room.
- As a user, I want to update a plant's details (e.g., when I repot it) or delete it.

### Technical Notes
- **Entity**: `Plant` (UUID, Name, Species, PotSize, SoilType, RoomID).
- **Pot Size & Soil**: Initially manual text entry; will move to categorized enums/entities in a later phase.

## 3. Basic Watering Tracking
Keeping plants alive through scheduled hydration.

### User Stories
- As a user, I want to set a manual watering frequency (in days) for each plant.
- As a user, I want to click a "Watered" button to log a watering event for today.
- As a user, I want the app to tell me when the next watering is due.

### Technical Notes
- **Logic**: 
    - `next_due = last_event_timestamp + (watering_interval * 24h)`.
- **UI State**: Plants should be sorted by "Urgency" (Overdue > Due > Healthy).

## 4. Watering Barometer
Visual hydration status at a glance.

### User Stories
- As a user, I want to see a visual barometer that shows how "thirsty" my plant is.
- As a user, I want the barometer to decrease automatically as time passes since the last watering.

### Technical Notes
- **Calculation**: `percentage = (next_due - now) / watering_interval`.
- **UI**: Color shift from Blue (Full) to Red (Thirsty/Overdue).

## 5. Archive System
Handling the "end of life" for plants.

### User Stories
- As a user, I want to move a plant to the "Archive" when it dies or is given away.
- As a user, I want to record why the plant was archived.
- As a user, I want to browse my archived plants in a read-only gallery.

### Technical Notes
- **Required Fields**: `Reason` (Died, Gifted, Sold), `ArchiveDate`.
- **State**: Archived plants are excluded from the "Active" Blueprint and Watering lists.
