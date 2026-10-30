# Architecture Decision Records

Short records of the key architecture decisions: ADR-001 to ADR-014 were made before feature implementation began, the later ones as the project grew. Web means the Wasm target only: the JavaScript target was dropped with ADR-017.

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
- Supports all current targets (Android, iOS, JVM, Wasm) using platform-specific drivers (on the web, ADR-017).
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
- The `shared` module compiles to all client targets (Android, iOS, JVM, Wasm); Panache entities would not compile there.
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
- De-facto standard for Kotlin Multiplatform DI; runs on every target (Android, iOS, JVM, Wasm).
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
- Only HTTP library that supports all KMP targets: Android (OkHttp engine), iOS (Darwin engine), JVM (OkHttp/CIO), Wasm (Fetch).
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
- `shared` holds no user-facing text: the ViewModels report typed reasons (`ErrorReason` for a failure, `PlantChange` for a saved plant change) and the UI maps each to one of these strings (`ui/ErrorReasonText.kt`).

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

---

## ADR-014 — Server Database Migrations: Atlas

**Decision:** Use **[Atlas](https://atlasgo.io)** for versioned database migrations on the server. **Do not** add Flyway or Liquibase.

**Rationale:**
- Stronger plan/diff/lint workflow than Flyway or Liquibase.
- Decoupled from the Quarkus runtime — no migration plugin in `server/build.gradle.kts`, no auto-apply on boot.
- HCL config (`server/atlas.hcl`) keeps env wiring next to the migration directory.

**Layout:**
- Config: `server/atlas.hcl`
- Versioned SQL: `server/migrations/` (filename pattern `YYYYMMDDhhmmss_<slug>.sql`)
- Apply: `atlas migrate apply --env <env>`

**Consequences:**
- Production schema is owned by Atlas; `quarkus.hibernate-orm.schema-management.strategy=none` in prod.
- Dev/test profiles still use Hibernate's `update` / `drop-and-create` for fast iteration — keep entity DDL and Atlas migrations in sync (e.g., `gen_random_uuid()` defaults must be expressible by both, or moved off the test path).
- Schema changes are made by adding a new versioned SQL file in `server/migrations/`, not by editing applied migrations. Re-hash with `atlas migrate hash` if the directory uses integrity tracking.

---

## ADR-015 — Android Build: An `androidApp` Module Beside the KMP Libraries (AGP 9)

**Decision:** The installable Android app is its own module, `androidApp`, built with `com.android.application`. `composeApp` and `shared` are Kotlin Multiplatform libraries whose Android target uses `com.android.kotlin.multiplatform.library`. No module applies `org.jetbrains.kotlin.android`: AGP 9 compiles Kotlin itself.

**Rationale:**
- AGP 9 no longer supports `com.android.application` (or `com.android.library`) in a module that applies the Kotlin Multiplatform plugin; the Android target of a KMP module must be the KMP Android library plugin.
- The split is the layout JetBrains and Google recommend for Compose Multiplatform apps, so new templates, guides and tooling match the project.
- The app module stays small: `MainActivity`, the Koin `Application`, the manifest and the launcher resources. Everything shared stays in `composeApp` and `shared`.

**Layout:**
- `androidApp`: `applicationId`, `targetSdk`, versioning, packaging and build types.
- `composeApp`: the Compose UI for every platform as an Android library (`namespace ch.wuethrich.greenprint.composeapp`, Compose resources shipped as Android assets) plus the Desktop, Web and iOS entry points.
- `shared`: the KMP library with ViewModels, repositories and the SQLDelight cache.

**Consequences:**
- Android runs and installs from `:androidApp` (`./gradlew :androidApp:assembleDebug`, `:androidApp:installDebug`); `:composeApp` has no Android app task.
- Android-only app wiring (activities, `Application`, manifest entries such as permissions for notifications) goes into `androidApp`; Android code the shared UI needs goes into `composeApp/src/androidMain`.
- The Kotlin compiler options for Android live in each module's `kotlin {}` block, since there is no Kotlin Android plugin to configure.

---

## ADR-016 — Code Quality: A Zero-Findings Gate (`prePRcheck`)

**Decision:** Every module is checked by **ktlint** (with the [Compose rules](https://mrmans0n.github.io/compose-rules/)) and **detekt**, and the tree is kept at **zero findings**. There are no baseline files and no thresholds raised to hide findings. `./gradlew prePRcheck` runs the whole gate locally before a pull request.

**Rules:**
- **Configuration**: ktlint and the Compose rules are configured in `.editorconfig` (`intellij_idea` style, so IntelliJ's formatter and `ktlintFormat` agree); detekt runs its default rules plus `config/detekt/detekt.yml`, which only adds detekt's documented Compose settings and the project's target names.
- **A finding is fixed in the code.** A suppression is the exception: it sits on the one declaration or expression it covers, never on a file or class, with the reason in a comment beside it.
- **The gate**: `prePRcheck` runs `ktlintCheck` and `detekt` on every module, the `shared`, Compose UI and server tests, and the Android debug and Wasm builds.

**Rationale:**
- A baseline turns old findings into accepted debt that nobody revisits; a gate that is always at zero makes every new finding visible in the change that introduced it.
- The Compose rules encode the conventions the style guide asks for (a `modifier` on every emitting composable, parameter order, event names, no ViewModels passed down) so review time goes to behaviour.
- One task is one command to run before a pull request, and the same command for CI to call.

**Consequences:**
- A change that adds a finding fails the build; `./gradlew ktlintFormat` fixes most formatting findings automatically.
- detekt 1.23 is pinned to the Kotlin compiler it was built with and runs without type resolution (only the `detekt` task per module; the per-source-set tasks are off) until detekt 2.x reads the current JDK.
- Raising a threshold or adding a baseline is a change to this ADR, not a local workaround.

---

## ADR-017 — Web Persistence: SQLite in a Web Worker, Saved to IndexedDB

**Decision:** The Wasm app's local cache is SQLite from **sql.js**, run in a web worker that speaks the protocol of SQLDelight's `WebWorkerDriver` (`composeApp/src/webMain/resources/greenprint-db.worker.js`). sql.js keeps the database in memory, so the worker saves a copy to **IndexedDB** at once after every committed write and opens that copy on the next start. SQLDelight generates the schema and queries **asynchronously** (`generateAsync`), and Wasm is the only web target: the JavaScript target is dropped.

**Rationale:**
- The screens read from the cache only (offline-first, ADR-002), so a web build without a real cache shows no data at all.
- `WebWorkerDriver` is SQLDelight's driver for the browser; it can only answer asynchronously, which is why the schema and every query are generated as suspend calls. Android, iOS and the JVM run the same async schema through `Schema.synchronous()`, so there is one generated API.
- sql.js needs no special HTTP headers, unlike SQLite's own wasm build with OPFS, which wants cross-origin isolation for its fastest mode. Saving the whole database to IndexedDB is cheap at this cache's size (tens of KB).
- One web target keeps one set of web source sets, one lock file and one bundle to test.

**Consequences:**
- The web entry point opens the cache (`openWebCache`) before Koin starts, since the schema must be created or migrated first; SQLite's `user_version` holds the schema version, as on the other platforms.
- `composeApp/webpack.config.d/` copies `sql-wasm-browser.wasm` next to the worker and runs the web dev server on port 8081, the server's dev CORS origin.
- Repository code awaits query results (`awaitAsList()`, `awaitAsOne()`), and tests that seed a database outside a coroutine use the synchronous schema.
- **Saving:** a write outside a transaction is saved as soon as it ran, a transaction when it commits; there is no delay, so a tab that closes or is hidden right after a write has nothing waiting to save, and no flush on `pagehide` is needed. Only an IndexedDB write still running when the browser ends the worker can be lost. IndexedDB runs one store's writes in the order they were made, so the newest save wins.
- **Recovery:** sql.js opens a damaged file without complaint and fails on the first query, so the worker checks a saved copy with `PRAGMA quick_check`; one that fails is logged, deleted, and the cache starts empty. If the app still cannot open the cache (a copy whose schema it cannot migrate), the entry point (`Main.kt`) logs it, ends that worker and starts a new one that discards the saved copy. Either way the app starts, and refills the cache from the server; writes not yet sent from that copy are lost.
- **Several tabs:** each tab holds its own copy of the database in its own worker, opened from the saved copy when the tab started, and saves its whole copy after each write. The last tab to save wins: its copy replaces the other tabs' saved writes, including writes they made offline and have not sent yet. Such a write still lives in its own tab's memory and is sent from there on that tab's next refresh, but it is lost if that tab closes first. A tab never sees another tab's writes until it is reloaded. A private window that refuses IndexedDB still gets a cache, for that visit only.
- Ktor's fetch engine reports a request that got no answer as `kotlin.Error`; in the browser that is mapped to `NetworkError.Connectivity`, so the offline path works there as on the other platforms.

