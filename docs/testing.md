# Testing Strategy

This document outlines the testing patterns and infrastructure used in the GreenPrint project.

## Compose UI Tests (commonTest)

UI tests for the `composeApp` module are located in `composeApp/src/commonTest`. We use a shared testing infrastructure to ensure tests run across all supported platforms (Android, iOS, JVM, etc.).

### ScreenTest Base Class

The `ScreenTest` base class (`ch.wuethrich.greenprint.testing.ScreenTest`) provides a standardized environment for screen-level UI tests.

**Features:**
- **Fresh Fakes:** Every test gets new instances of `FakeSpaceRepository`, `FakeRoomRepository`, and `FakePlantRepository`.
- **Koin Integration:** Automatically starts and stops a Koin container for each test, wired with `testAppModule`.
- **Voyager Navigator:** Includes a `setScreenContent` helper to mount screens inside a Voyager `Navigator`.
- **Back-stack Assertions:** Support for `MarkerScreen` to verify navigation pops.

### Usage Example

To test a screen, extend `ScreenTest` and use `runComposeUiTest`:

```kotlin
@OptIn(ExperimentalTestApi::class)
class MyScreenTest : ScreenTest() {

    @Test
    fun rendersTitle() = runComposeUiTest {
        // 1. Arrange: Setup fake data
        val space = Space(id = "1", name = "Home Office")
        spaceRepository.emitSpaces(listOf(space))

        // 2. Act: Mount the screen
        setScreenContent(MyScreen())

        // 3. Assert: Verify UI state
        onNodeWithText("Home Office").assertIsDisplayed()
    }

    @Test
    fun handlesDeletion() = runComposeUiTest {
        val behind = MarkerScreen()
        val screen = MyScreen()
        
        // Mount with a parent screen behind it
        setScreenContent(screen, behind = behind)

        // Perform action that triggers pop
        onNodeWithContentDescription("Delete").performClick()

        // Assert we are back on the marker screen
        onNodeWithText(MarkerScreen.MARKER_TEXT).assertIsDisplayed()
    }
}
```

### Dependency Injection in Tests

If a screen requires specific ViewModels or other dependencies not in `testAppModule`, override `extraModules()`:

```kotlin
override fun extraModules() = listOf(
    module {
        factory { MyViewModel(get()) }
    }
)
```

## Fake Infrastructure

We prefer "Fakes" over "Mocks" for core domain components to ensure more realistic state transitions.

- **FakeRepositories:** Located in `testing/FakeRepositories.kt`. They use `MutableStateFlow` to simulate reactive data updates.
- **FakeSqlDriver:** A memory-based SQLDelight driver for testing database queries without persistence side effects.
