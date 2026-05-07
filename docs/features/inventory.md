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

---

## 2. Core Domain Model

The system follows a strict hierarchy defined in ADR-007.

| Entity | Primary Keys | Key Attributes | Relationships |
| :--- | :--- | :--- | :--- |
| **Space** | UUID | `name`, `iconId`, `ownerId` | Has many Rooms |
| **Room** | UUID | `name`, `iconId`, `spaceId` | Belongs to Space, Has many Plants |
| **Plant** | UUID | `name`, `species`, `wateringIntervalDays`, `photoUrl` | Belongs to Room, Has many WateringEvents |
| **WateringEvent** | UUID | `timestamp`, `plantId`, `source` | Belongs to Plant |

### Data Constraints & Validation
- **Names**: Trimmed, non-empty, 1–128 characters.
- **Intervals**: Integer, 1–365 days (clamped).
- **Photos**: Max 5MB raw; client-side resize to 1024px (≤ 500KB) for upload.
- **Uniqueness**: Space names are unique per `ownerId`. Room names are unique per `spaceId`.

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

---

## 4. Space & Room Management

### User Stories
- As a user, I want to create a single space (e.g., "My Apartment") and organize it into rooms (e.g., "Living Room").
- As a user, I want to choose icons for my space and rooms from a predefined registry.

### Flows & Restrictions
- **Single Space MVP**: UI restricts to one space; "Create" button hidden if space exists.
- **Room Deletion**: 
  - If room contains plants: Dialog prompts to "Move plants" or "Archive plants."
  - Room is only deleted after its contents are cleared.
- **Space Deletion**: Two-step confirmation; requires user to type the space name to confirm.
- **Icon Selection**: Backend assigns defaults if `null` is sent during creation.

---

## 5. Plant Management & Care

### User Stories
- As a user, I want to add a plant with its species, pot size, soil type, and watering interval.
- As a user, I want to edit a plant's details (name, species, interval, photo) to keep my records accurate.
- As a user, I want to see a visual "Health Barometer" reflecting hydration status.
- As a user, I want to log a watering event with a single tap.
- As a user, I want to archive/delete a plant if it is no longer in my collection.

### Flows & Restrictions
- **Watering Logic**: `nextWateringDate = lastWateringTimestamp + (intervalDays * 24h)`.
- **Barometer Color Scale**: Blue (100%) → Yellow (30%) → Red (0%/Overdue).
- **Quick Water**: Tapping the water-drop icon logs an event immediately; shows a 5s "Undo" snackbar.
- **Photo Workflow**: Native camera/gallery pickers; compressed to ≤ 500KB before upload.
- **Editing**: All fields are editable; updates are synced via the "Last Write Wins" strategy.
- **Archiving (Deletion)**:
  - Deletion is handled as "Archiving" to preserve history (ADR-009).
  - User must select a reason: `DIED`, `GIFTED`, or `SOLD`.
  - Archived plants are moved to a separate "Jungle Graveyard" view (read-only).

---

## 6. Technical Architecture

### 6.1 State Management (KMP ViewModel)
- **Pattern**: Unidirectional Data Flow (UDF).
- **State**: `StateFlow<UiState>` (Loading, Success, Error).
- **Events**: `Channel<UiEffect>` (Navigation, Snackbars).

### 6.2 Synchronization & Persistence
- **Write-Through Cache**: Repository writes to SQLDelight first (Offline-First), then syncs to API.
- **Sync Status**: `S_SYNCED`, `S_PENDING`, `S_ERROR` flags on every entity.
- **Conflict Strategy**: "Last Write Wins" based on client-side `updatedAt` timestamps.

### 6.3 Observability
- **Logging**: Structured logging via Kermit.
- **Analytics**: Segment/Firebase integration for key funnel metrics.
- **Error Tracking**: Sentry integration for KMP crashes and API failures.

---

## 7. Testing & Quality Assurance

### Testing Strategy
- **Unit Tests**: Domain logic, Barometer calculations (100% coverage).
- **Integration Tests**: SQLDelight schema and Repository sync cycles.
- **UI Tests**: Compose Multiplatform `createComposeRule` for critical paths.
- **E2E Tests**: Maestro/Appium for full Onboarding and Plant Care flows.

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
