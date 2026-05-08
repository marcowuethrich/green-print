# Testing Strategy

This document outlines the testing patterns and infrastructure used in the GreenPrint project.

## Compose UI Tests (commonTest)

UI tests for the `composeApp` module are located in `composeApp/src/commonTest`. We use a shared testing infrastructure to ensure tests run across all supported platforms (Android, iOS, JVM, etc.).

### ScreenTest Base Class

The `ScreenTest` base class (`ch.wuethrich.greenprint.testing.ScreenTest`) provides a standardized environment for screen-level UI tests.

**Features:**
- **Zero Animations:** Automatically provides `LocalAnimationsEnabled = false` via `CompositionLocal`. This makes transitions instantaneous and removes the need for `mainClock.advanceTimeBy`.
- **Fresh Fakes:** Every test gets new instances of `FakeSpaceRepository`, `FakeRoomRepository`, and `FakePlantRepository`.
- **Koin Integration:** Automatically starts and stops a Koin container for each test, wired with `testAppModule`.
- **Voyager Navigator:** Includes a `setScreenContent` helper to mount screens inside a Voyager `Navigator`.

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

### Component Best Practices
To ensure components are ready for focus in a zero-animation test environment, use `withFrameNanos` before requesting focus:

```kotlin
LaunchedEffect(Unit) {
    if (animationsEnabled) {
        delay(100.milliseconds)
    } else {
        withFrameNanos { } // Yield to let Compose layout before focusing
    }
    focusRequester.requestFocus()
}
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

## Fake Infrastructure

We prefer "Fakes" over "Mocks" for core domain components to ensure more realistic state transitions.

- **FakeRepositories:** Located in `testing/FakeRepositories.kt`. They use `MutableStateFlow` to simulate reactive data updates.
- **FakeSqlDriver:** A memory-based SQLDelight driver for testing database queries without persistence side effects.
