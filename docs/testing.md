# Testing Strategy

This document outlines the testing patterns and infrastructure used in the GreenPrint project.

## The PR Gate: `prePRcheck`

One command runs everything a pull request has to pass (ADR-016):

```bash
./gradlew prePRcheck
```

It runs, and fails on the first problem:
- `ktlintCheck` (with the Compose rules) and `detekt` on every module, at zero findings: no baselines.
- `:shared:jvmTest`, `:composeApp:jvmTest` and `:server:test`.
- `:androidApp:assembleDebug` and `:composeApp:wasmJsBrowserDistribution`, so the Android and Web builds stay green.

The server tests need Docker (see [Server Tests](#server-tests-quarkus)). `./gradlew ktlintFormat` fixes most formatting findings; detekt's report is printed in the build output and written to each module's `build/reports/detekt/detekt.txt`.

## Shared Module Tests

- **`shared/src/commonTest`**: platform-independent logic, for example `SpaceViewModelTest` (including a room the server refuses to delete, and typed error reasons) and `BlueprintLayoutTest` (free spots for new plants, clamping).
- **`shared/src/jvmTest`**: tests that need a real database. `PlantViewModelTest` runs `PlantViewModel` over the real `PlantRepositoryImpl`, an in-memory SQLite cache (`JdbcSqliteDriver(IN_MEMORY)`) and a Ktor `MockEngine` that plays the server and can go offline. It covers the blueprint's offline-first moves (online, offline, resent on refresh, kept across a restart, clamped) and Quick Water the same way: an online watering is logged on both sides, an offline one fills the hydration bar at once and is reported, is sent once on the next refresh, and survives a restart. The repository gets a fixed `Clock`, so the tests can check the exact `lastWateredDate`.
- The SQLDelight code is generated async (ADR-017). A test seeds its in-memory database through `GreenPrintDb.Schema.synchronous()` and reads it back with `awaitAsList()` / `awaitAsOne()`.
- The repository takes its database dispatcher as a constructor parameter, and the mock engine runs on the test dispatcher, so `advanceUntilIdle()` waits for the database and the "network" alike.

```bash
./gradlew :shared:jvmTest
```

## Compose UI Tests (commonTest)

UI tests for the `composeApp` module are located in `composeApp/src/commonTest` and use the Compose Multiplatform v2 test API (`androidx.compose.ui.test.v2.runComposeUiTest`). They are written once for every target; the gate runs them on the JVM (Desktop) target.

### ScreenTest Base Class

The `ScreenTest` base class (`ch.wuethrich.greenprint.testing.ScreenTest`) provides a standardized environment for screen-level UI tests.

**Features:**
- **Zero Animations:** `setScreenContent` provides `LocalAnimationsEnabled = false`. This makes transitions instantaneous and removes the need for `mainClock.advanceTimeBy`.
- **Fresh Fakes:** Every test gets new instances of `FakeSpaceRepository`, `FakeRoomRepository`, and `FakePlantRepository`.
- **Koin Integration:** Automatically starts and stops a Koin container for each test, wired with `testAppModule` (plus any `extraModules()`).
- **Voyager Navigator:** The `setScreenContent` helper mounts screens inside a Voyager `Navigator`; pass `behind` to put a `MarkerScreen` underneath and assert that a screen popped back.

### Robust Synchronization Patterns

Avoid hardcoded delays like `Thread.sleep()` or `advanceTimeBy()`. Instead, use `waitUntil` with specific matchers:

#### Waiting for Focus
When testing focus (e.g., auto-focusing a field), use the `isFocused()` matcher inside `waitUntil`:

```kotlin
waitUntil(timeoutMillis = 2000L) {
    onAllNodes(isFocused() and hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
}
```

#### Handling Overlapping UI
If multiple nodes match (e.g., a "Delete" button in a menu and another in a confirmation dialog), use `onLast()` or specific ancestor matchers to target the correct one:

```kotlin
// Click the "Delete" in the dialog, not the menu item
onAllNodesWithText("Delete").onLast().performClick()
```

#### Gestures, Keys and Accessibility Actions
`PlantScreenTest` covers Quick Water (online and offline snackbars) and the archive menu, and shows the patterns for the Spatial Blueprint: `performTouchInput { swipe(start = center, end = center + Offset(200f, 0f)) }` for a drag, `requestFocus()` plus `performKeyInput { pressKey(Key.DirectionRight) }` for the keyboard, and `performCustomAccessibilityActionWithLabel(...)` for screen-reader actions. Assert on what the fake repository received (`plantRepository.positionUpdates`) to check that a drag saves once, on release.

### Component Best Practices
Dialogs move focus to their first control with `RequestInitialFocus(focusRequester)` (`ui/components/DialogParts.kt`, ADR-013). It waits for the entrance animation, or one frame when animations are off, so it works in the zero-animation test environment too:

```kotlin
val focusRequester = remember { FocusRequester() }
RequestInitialFocus(focusRequester)
OutlinedTextField(..., modifier = Modifier.focusRequester(focusRequester))
```

### Running Tests Locally

The fastest and most reliable way to run UI tests locally is using the JVM (Desktop) target. This avoids the overhead of Android emulators or Robolectric environment issues.

**Run all UI tests:**
```bash
./gradlew :composeApp:jvmTest
```

**Run a specific test class:**
```bash
./gradlew :composeApp:jvmTest --tests "ch.wuethrich.greenprint.space.SpaceScreenTest"
```

## Server Tests (Quarkus)

`server/src/test` holds `@QuarkusTest` resource tests that call the REST API with REST Assured, for example `PlantPositionResourceTest` for `PUT /plants/{id}/position` and its `400`/`404` answers, `PlantWateringResourceTest` for `POST /plants/{id}/waterings` (resends stored once, an older watering, future and malformed timestamps), `RoomResourceTest` for the `409 room_not_empty` of a room with plants, and `RoomPlantResourceTest` for the interval and archive-reason checks. Users are set per test with `@TestSecurity`, and each test clears the tables first (`TestDatabase.kt`); the classes share the database, so they hold a `@ResourceLock("database")`.

Quarkus Dev Services start PostgreSQL 16 in a container (Testcontainers), so the tests need a running Docker daemon. With [Colima](https://github.com/abiosoft/colima) on macOS:

```bash
colima start
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
./gradlew :server:test
```

Docker Desktop needs no variables. The container is reused between runs (`devservices.reuse=true`).

## The Web App

The gate builds the Wasm bundle but runs no browser. To use the web app against the dev server, start both (the server first; it seeds a demo space):

```bash
./gradlew :server:quarkusDev
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # http://localhost:8081, the server's dev CORS origin
```

The cache lives in the browser's IndexedDB (database `greenprint`); with the server stopped, a reload still shows the cached data and a watering is kept on the device. Compose renders into a canvas, so a browser script drives it by coordinates and screenshots, not by DOM selectors.

## Fake Infrastructure

We prefer "Fakes" over "Mocks" for core domain components to ensure more realistic state transitions.

- **FakeRepositories:** Located in `composeApp/src/commonTest/.../testing/FakeRepositories.kt`. Each one is backed by a replaying `MutableSharedFlow` (replay 1): a test emits the data a screen should show, and the screen sees the latest value even if it subscribes later. Writes return a result the test sets (`createResult`, `deleteResult`, ...); `updatePlantPosition` also records each move in `positionUpdates` and re-emits the moved plant, like the real write-through repository; `waterPlant` records each watering in `waterings` and answers `waterResult`, and `archivePlant` records each request in `archives`.
- **FakeSqlDriver:** A no-op SQLDelight driver: reads return empty cursors and writes persist nothing. It only satisfies the `GreenPrintDbQueries` that Koin injects; tests that need a real database use an in-memory `JdbcSqliteDriver` instead (see `PlantViewModelTest`).
