# Feature: Plant & Space Inventory

This document defines the complete implementation specification for the GreenPrint inventory system. It serves as the single source of truth for engineering, design, and QA to build a professional, enterprise-grade houseplant management experience.

---

## 1. Executive Summary & Scope

The Inventory System is the core of GreenPrint, managing the lifecycle of living spaces, rooms, and plants. This specification transitions the initial MVP concepts into a robust, sync-capable, and observable architecture.

### Primary Objectives
- **Data Integrity**: Zero-loss synchronization between KMP clients and the PostgreSQL backend.
- **Offline Resilience**: Full read/write capability while offline with background synchronization.
- **Observability**: Comprehensive tracing of watering history and system performance.
- **Accessibility**: Compliance with WCAG 2.1 AA standards for all inventory interactions.

### Implementation Status
This spec describes the target. What the code does today:

| Area | Status |
| :--- | :--- |
| Spaces, rooms, plants (create, edit, delete), archiving | Built (§4, §5) |
| Spatial Blueprint, offline-first plant moves | Built ([spatial_blueprint.md](spatial_blueprint.md)) |
| Health Barometer | Built (§5) |
| Quick Water, watering events | Built, offline-first (§5, §6.2); no Undo yet |
| OIDC login, secure token storage | Not yet: a no-op `AuthRepository` returns a mock token (ADR-011); the dev and test server profiles disable OIDC |
| Photos, watering history view, "Jungle Graveyard" view | Not yet |
| Offline writes for everything except blueprint moves and waterings | Not yet (§6.2) |
| Observability (§6.3), E2E tests (§7) | Not yet |
| Web (wasm) app | Built: the local cache is SQLite in a web worker, saved to IndexedDB (ADR-017) |

---

## 2. Core Domain Model

The system follows a strict hierarchy defined in ADR-007.

| Entity | Primary Keys | Key Attributes | Relationships |
| :--- | :--- | :--- | :--- |
| **Space** | UUID | `name`, `icon`, owning user | Has many Rooms |
| **Room** | UUID | `name`, `icon`, `spaceId` | Belongs to Space, Has many Plants |
| **Plant** | UUID | `name`, `species`, `potSize`, `soilType`, `wateringIntervalDays`, `lastWateredDate`, `photoUrl`, `x`, `y`, `archiveDate`, `archiveReason` | Belongs to Room, Has many WateringEvents |
| **WateringEvent** | UUID | `timestamp`, `plantId`, `source` | Belongs to Plant |

### Data Constraints & Validation
- **Names**: Trimmed, non-empty, 1–128 characters. The server answers `400` with `name_blank` or `name_too_long`.
- **Intervals**: Integer, 1–365 days (`WateringInterval` in `shared`). The form clamps into that range and falls back to 7 days when the field holds no number; the server answers `400` with `interval_out_of_range` and the `field` `wateringIntervalDays` on create and update.
- **Positions**: `x` and `y` place the plant on its room's blueprint, normalized to 0.0–1.0 with both edges allowed (ADR-008). The server answers `400` with `position_out_of_range` and the `field` (`x` or `y`) on create, update and `PUT /plants/{id}/position`.
- **Photos**: Max 5MB raw; client-side resize to 1024px (≤ 500KB) for upload.
- **Uniqueness**: Space names are unique per user, room names per space, and plant names per room. A duplicate answers `409`.

---

## 3. Authentication & Onboarding

### User Stories
- As a user, I want to be automatically logged in via OIDC so I can access my inventory securely.
- As a user, I want my local cache cleared on logout to prevent data leakage.

### Flows & Restrictions
- **Cold Start**: Direct-to-Login if no valid session token.
- **Automatic Login**: If a valid/refreshable token exists, navigate directly to the Welcome Screen.
- **Session Expiry**: Intercept 401 errors; redirect to Login with an informational snackbar.
- **Logout**: Clears local SQLDelight cache and secure token storage.

### Technical Notes
- Uses OIDC Authorization Code + PKCE (ADR-001).
- Tokens stored in platform-specific secure enclaves (Keychain/EncryptedSharedPreferences).
- *Today*: none of this is built; the app starts on the Welcome Screen with a mock token (§1).

---

## 4. Space & Room Management

### User Stories
- As a user, I want to create a single space (e.g., "My Apartment") and organize it into rooms (e.g., "Living Room").
- As a user, I want to choose icons for my space and rooms from a predefined registry.

### Flows & Restrictions
- **Single Space MVP**: UI restricts to one space; "Create" button hidden if space exists.
- **Room Deletion**:
  - If the room still has plants, the app refuses and explains in a "Room Not Empty" dialog that they must be moved or archived first. The check runs against the local cache.
  - The server's `DELETE /rooms/{id}` refuses too: a room with any plant, archived ones included, answers `409` with `{"code": "room_not_empty"}`. Archived plants are the room's history (ADR-009), so a room that has them is kept. The app shows the same dialog for that answer, since its cache does not hold archived plants.
  - Next step: the dialog offers "Move plants" or "Archive plants" and deletes the room afterwards ([spatial_blueprint.md §3](spatial_blueprint.md)).
- **Space Deletion**: Two-step confirmation; requires user to type the space name to confirm.
- **Icon Selection**: Backend assigns defaults (`default_space`, `default_room`) if `null` is sent during creation.

---

## 5. Plant Management & Care

### User Stories
- As a user, I want to add a plant with its species, pot size, soil type, and watering interval.
- As a user, I want to edit a plant's details (name, species, interval, photo) to keep my records accurate.
- As a user, I want to see a visual "Health Barometer" reflecting hydration status.
- As a user, I want to log a watering event with a single tap.
- As a user, I want to archive/delete a plant if it is no longer in my collection.
- As a user, I want to place my plants where they stand in the room ([spatial_blueprint.md](spatial_blueprint.md)).

### Flows & Restrictions
- **Watering Logic**: `nextWateringDate = lastWateringTimestamp + (intervalDays * 24h)`.
- **Barometer**: The bar shows the share of the watering interval still left (100% just watered, 0% due or never watered). It is coloured by `HydrationLevel`: the primary green above 70%, the warning amber above 30%, the error red below that.
- **Quick Water**: Tapping the water action on a plant card logs a watering at once and the hydration bar fills, online or offline (§6.2). A snackbar confirms it, or says the watering is saved on this device and syncs later. *Next step*: a 5s "Undo" in that snackbar, which needs a way to remove a watering on the server.
- **Watering API**: `POST /plants/{id}/waterings` with `{"id": "<uuid>", "timestamp": "<ISO-8601 instant>"}` logs the watering and returns the plant. The client picks the id, so a resent watering is stored once; a watering older than the plant's last one is logged without moving `lastWateredDate`. A malformed id answers `400` `watering_id_invalid`, a malformed or future timestamp (more than 5 minutes ahead) `400` `timestamp_invalid`, an unknown plant `404`.
- **Photo Workflow**: Native camera/gallery pickers; compressed to ≤ 500KB before upload.
- **Editing**: The form edits name, species and interval today; every other field (pot size, soil type, photo, position) is kept as it is. Updates follow the "Last Write Wins" strategy.
- **First watering**: A new plant counts as watered when it is added, so its bar starts full instead of dry.
- **Placement**: A new plant lands on the free blueprint spot farthest from the room's other plants; it is moved on the blueprint afterwards.
- **Archiving (Deletion)**:
  - Deletion is handled as "Archiving" to preserve history (ADR-009).
  - User must select a reason: `DIED`, `GIFTED`, or `SOLD` (`ArchiveReason` in `shared`); the plant menu offers all three. Any other reason answers `400` with `archive_reason_invalid`.
  - Archived plants are moved to a separate "Jungle Graveyard" view (read-only). *Today*: they leave the room's list and blueprint; the view is not built yet.

---

## 6. Technical Architecture

### 6.1 State Management (KMP ViewModel)
- **Pattern**: Unidirectional Data Flow (UDF).
- **State**: `StateFlow<UiState>` (Loading, Success, Error).
- **Events**: One-shot effects (snackbars, dialogs, navigation) on a `SharedFlow<UiEffect>`.
- **Messages**: The ViewModels hold no user-facing text. A failure is an `ErrorReason`, a success a typed change (`PlantChange`), and the screens map each to a string resource (ADR-012).

### 6.2 Synchronization & Persistence
- **Write-Through Cache**: Repository writes to SQLDelight first (Offline-First), then syncs to API.
  - *Built for blueprint moves*: `updatePlantPosition` saves the move locally as `S_PENDING`, then sends it; the next refresh resends unconfirmed moves ([spatial_blueprint.md §2](spatial_blueprint.md)).
  - *Built for waterings*: `waterPlant` writes a `WateringEventEntity` row (`S_PENDING`, with a client-made UUID) and the plant's new `lastWateredDate` in one transaction, then sends it. Success marks the watering `S_SYNCED` and takes the server's date (unless a newer watering still waits); a failure marks it `S_ERROR`. The next refresh of the room resends every unsent watering under its id, before the moves. The plant row's own `syncStatus` stays about its position, so a watering never settles or hides a pending move.
  - *Other writes* (create, edit, archive, delete) still call the API first and update the cache from its answer, so they need a connection.
- **Read Path**: Screens observe SQLDelight; a refresh fetches from the API and replaces the cached rows (for plants, only the rows with no unconfirmed move and no unsent watering).
- **Sync Status**: `S_SYNCED`, `S_PENDING`, `S_ERROR` flags on every entity (`SyncStatus` in `shared`).
- **Conflict Strategy**: "Last Write Wins" based on client-side `updatedAt` timestamps. A refresh never overwrites a row with a change the server has not confirmed. The server stamps every plant change in its own `updated_at`, which the API returns as `updatedAt`.

### 6.3 Observability
- **Logging**: Structured logging via Kermit.
- **Analytics**: Segment/Firebase integration for key funnel metrics.
- **Error Tracking**: Sentry integration for KMP crashes and API failures.

---

## 7. Testing & Quality Assurance

### Testing Strategy
- **Unit Tests**: Domain logic, Barometer calculations (100% coverage).
- **Integration Tests**: SQLDelight schema and Repository sync cycles (`PlantViewModelTest` runs the real repository over in-memory SQLite and a mocked server).
- **UI Tests**: Compose Multiplatform `runComposeUiTest` for critical paths.
- **E2E Tests**: Maestro/Appium for full Onboarding and Plant Care flows.

See [testing.md](../testing.md) for how to run them.

---

## 8. Non-Functional Requirements

- **Performance**: 60fps list scrolling; initial sync < 2s on 4G.
- **Reliability**: 99.9% uptime for watering tracking; automated retry with backoff.
- **Accessibility**: 48x48dp touch targets; full keyboard/screen reader support (ADR-013).

---

## 9. Post-MVP Roadmap
- **AI Vision**: Automatic species/health detection via LLM API.
- **Batch Actions**: Bulk watering and room transfers.
- **Environmental Sync**: Integration with smart home sensors.
