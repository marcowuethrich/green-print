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

## ADR-002 — Client State: In-Memory (Online-Only)

**Decision:** All client state is held in-memory inside KMP ViewModels. There is no local persistence layer for now.

**Rationale:**
- Removes the complexity of cache invalidation and sync conflicts at the start of the project.
- The app is a personal portfolio tool — users are expected to have connectivity.

**Consequences:**
- Data is lost if the app is killed; the next launch re-fetches from the server.
- Adding a local cache later (e.g. Room on Android, or SQLDelight for KMP) is a self-contained change: swap the repository data source without touching ViewModels or the API layer.

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
