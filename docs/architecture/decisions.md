# Architecture Decision Records

Short records of the key decisions made before feature implementation began.

---

## ADR-001 — Authentication: OIDC / OAuth 2.0

**Decision:** Use the OIDC Authorization Code flow, secured server-side with `quarkus-oidc`.

**Rationale:**
- Quarkus has first-class OIDC support; token validation (JWKS) and role mapping are handled declaratively.
- Decouples identity management from the app — any OIDC-compliant provider (Keycloak, Auth0, etc.) can be swapped in.

**Consequences:**
- The client (`shared`) must implement the OIDC Authorization Code + PKCE flow and attach the Bearer token to every API request.
- The server never stores passwords; it trusts the provider's signed JWTs.

---

## ADR-002 — Client State: Local Persistence (SQLDelight)

**Decision:** Use **SQLDelight** as the local persistence and caching layer across all KMP targets.

**Rationale:**
- Provides a type-safe API for SQLite, ensuring consistency between the schema and Kotlin code.
- Enables offline support by caching server data locally.
- Supports all current targets (Android, iOS, JVM, JS, Wasm) using platform-specific drivers.
- Facilitates reactive UI updates by exposing database queries as Coroutine Flows.

**Consequences:**
- Data persists across app restarts.
- Repositories are responsible for synchronizing local cache with the remote API.
- Migrations must be handled via SQLDelight's `.sq` files.
- Platform-specific `DriverFactory` implementations are required to initialize the SQLite driver.

---

## ADR-003 — Domain Model Boundary: Shared DTOs, Server-Owned Entities

**Decision:** The `shared` KMP module defines plain Kotlin data classes used as API DTOs. The server defines its own Hibernate Panache entity layer and maps between them.

**Rationale:**
- Keeps JPA/Panache annotations and server-specific concerns out of the KMP module.
- The `shared` module compiles to all client targets (Android, iOS, JVM, JS, Wasm); Panache entities would not compile there.
- A clean DTO/entity mapping layer on the server makes it easy to evolve the database schema independently of the API contract.

**Boundary:**

```
shared/         → domain DTOs  (plain data classes, no framework annotations)
server/         → Panache entities + mappers to/from DTOs
```

---

## ADR-004 — Dependency Injection: Koin

**Decision:** Use **Koin** for DI across all client targets.

**Rationale:**
- De-facto standard for Kotlin Multiplatform DI; runs on all five targets (Android, iOS, JVM, JS, Wasm).
- Modules are declared in `shared/commonMain` and can be extended per platform in `androidMain` / `iosMain` if needed.
- Integrates directly with KMP ViewModel via `koin-compose-viewmodel`.

**Best practices applied:**
- One Koin module file per feature area (e.g. `plantModule`, `authModule`, `networkModule`).
- `single {}` for shared services (Ktor `HttpClient`, repositories); `viewModel {}` for ViewModels.
- `startKoin {}` is called once from each platform entry point (Android `Application`, iOS `MainViewController`, JVM `main`, Wasm `main`).
- No service-locator calls (`get()`) outside of module definitions — all dependencies are constructor-injected.

---

## ADR-005 — HTTP Client: Ktor Client

**Decision:** Use **Ktor Client** for all HTTP communication from the client side.

**Rationale:**
- Only HTTP library that supports all KMP targets: Android (OkHttp engine), iOS (Darwin engine), JVM (OkHttp/CIO), JS (Fetch), Wasm (Fetch).
- Declared once in `shared/commonMain`; engine is swapped per platform in the Gradle source sets.
- First-class support for coroutines, JSON (via `kotlinx.serialization`), and content negotiation.

**Configuration lives in `shared`:**
- `HttpClient` is created in a Koin `single {}` block in `networkModule`.
- Base URL is injected via a platform `expect`/`actual` constant or build config.
- The OIDC Bearer token is attached via a Ktor `BearerTokens` plugin.

---

## ADR-006 — Maximize commonMain Sharing

**Decision:** Everything that can run on all targets goes into `shared/commonMain`. Platform source sets (`androidMain`, `iosMain`, etc.) contain only what is unavoidably platform-specific.

**Rationale:**
- Reduces duplication and keeps the test surface in one place.
- `commonMain` already has access to KMP ViewModel, Ktor Client, Koin, and `kotlinx.serialization` — the full client stack.

**Rules:**
- Domain DTOs, repositories, ViewModels, Ktor client config, and Koin modules → `commonMain`.
- Platform-specific system APIs (notifications, camera, file system) → `expect`/`actual` in the relevant source set.
- UI composables → `composeApp/commonMain` (Compose Multiplatform is also cross-platform).
- No Android-only libraries are allowed in `shared`.

---

## ADR-007 — Inventory Domain Model: Spaces, Rooms, and Plants

**Decision:** Organize the inventory into a hierarchy: `Space` → `Room` → `Plant`. Watering is tracked via a `WateringEvent` log.

**Rationale:**
- Reflects how users naturally organize plants at home.
- Allows for future expansion into multiple spaces (e.g., "Home" vs. "Office").
- Decouples watering history from the current plant state for better analytics.

**Model Definitions (shared/ DTOs):**
- `Space`: UUID, Name, List of Rooms.
- `Room`: UUID, Name, List of Plants.
- `Plant`: ID, Name, Species, PotSize, SoilType, WateringIntervalDays, LastWateredDate (computed or explicitly stored).
- `WateringEvent`: ID, PlantID, Timestamp.

**Watering Logic:**
- `nextWateringDate = lastWateringDate + wateringIntervalDays`.
- If `lastWateringDate` is null, the plant is considered "Never Watered" and potentially high priority.
- Status is derived: `Healthy` (now < next), `Due` (now ≈ next), `Overdue` (now > next).

---

## ADR-008 — Spatial Data: 2D Coordinates

**Decision:** Store plant positions as normalized 2D coordinates (`x: Float`, `y: Float`) relative to the room container.

**Rationale:**
- Normalized coordinates (0.0 to 1.0) ensure that the blueprint remains consistent across different screen sizes and aspect ratios.
- Coordinates are stored in the server database alongside the `Plant` entity.

---

## ADR-009 — Archiving Strategy: Status & Metadata

**Decision:** Transition plants to an "Archived" state using a status field rather than deletion. 

**Rationale:**
- Preserves the history of the user's "indoor jungle."
- Allows for a read-only gallery of past plants.
- Archiving requires specific metadata: `archiveDate` and `archiveReason` (Enum: `DIED`, `GIFTED`, `SOLD`).

---

## ADR-010 — Local Notifications for Engagement

**Decision:** Use platform-specific local notification APIs to remind users to water plants.

**Rationale:**
- Avoids the cost and complexity of a central push notification server for basic reminders.
- Scheduling logic lives in the platform source sets (`androidMain`, `iosMain`) but is triggered by shared logic when the "thirsty" zone is reached.

---

## ADR-011 — Auth Readiness: Interface-Driven

**Decision:** Use a `NoOpAuthRepository` and a Ktor `Auth` plugin interceptor to make the system "auth-ready" without requiring a real OIDC provider during initial development.

**Rationale:**
- Decouples feature development from authentication infrastructure.
- All HTTP calls automatically include a (currently mock) Bearer token.
- Switching to real OIDC only requires replacing `NoOpAuthRepository` with a real implementation in the Koin `networkModule`.

---

## ADR-012 — Resource String Naming Convention

**Decision:**
Adopt the Android/KMP community naming pattern for resource strings in `composeApp/src/commonMain/composeResources/values/strings.xml`.

| Pattern | Used for | Example |
| :--- | :--- | :--- |
| `app_*` | App-level strings | `app_name` |
| `common_*` | Actions reused across screens | `common_save`, `common_cancel` |
| `error_*` | Error messages | `error_generic`, `error_network` |
| `<feature>_*` | Feature-specific, feature name first | `space_create_title`, `space_name_hint` |
| `<feature>_<dialog>_*` | Dialog strings scoped to feature | `space_rename_dialog_title` |

**Rationale:**
- Aligns with established industry standards in the Android and KMP ecosystems.
- Grouping by feature (feature-first naming) improves discoverability and IDE autocomplete performance.
- The `common_` prefix is the key payoff as the app grows — when a "Rooms" screen needs a Save button, it reuses `common_save` instead of adding a duplicate `save_room` string.

**Consequences:**
- Prevents duplicate strings for generic actions.
- Developers must prioritize using `common_` strings where applicable.
- Simplifies navigation within the resource files as features are grouped alphabetically by prefix.

---

## ADR-013 — Keyboard Navigation and Focus Management

**Decision:**
The application must be fully navigable and operable via keyboard. Every screen and modal must define a logical default focus point upon entry.

**Implementation Rules:**
- **Autofocus**: Every new screen or dialog must automatically request focus on the most logical initial action or input.
- **TextInput**: Name/Detail entry fields must be focused and, if renaming, have all text selected by default.
- **Destructive Actions**: In confirmation dialogs (e.g., Delete), the **Cancel** button must be focused by default to prevent accidental data loss.
- **Form Submission**: Text fields should support `ImeAction.Done` or `ImeAction.Next` to allow submission/navigation without leaving the keyboard.

**Rationale:**
- Improves accessibility for users with motor impairments.
- Enhances productivity for power users on desktop (JVM/Web) and mobile with external keyboards.
- Aligns with "Senior Designer" standards for modern, professional software.

**Consequences:**
- Developers must use `FocusRequester` and `LaunchedEffect` in every new UI component.
- Testing must include keyboard-only verification (Tab, Enter, Arrow keys).
