# Architecture Overview

GreenPrint is a **Kotlin Multiplatform** spatial houseplant inventory app. Shared business logic and UI run across Android, iOS, Desktop (JVM), and Web (Wasm), backed by a Quarkus REST server.

---

## Modules

| Module | Gradle plugin | What it holds |
| :--- | :--- | :--- |
| `androidApp` | `com.android.application` | The installable Android app: `MainActivity`, the Koin `Application`, the manifest and the launcher resources (ADR-015). |
| `composeApp` | `org.jetbrains.kotlin.multiplatform` + `com.android.kotlin.multiplatform.library` | The Compose Multiplatform UI for every platform, plus the Desktop (`Main.kt`), Web (`webMain`, with the cache's web worker and `webpack.config.d`) and iOS (`MainViewController`) entry points. |
| `shared` | `org.jetbrains.kotlin.multiplatform` + `com.android.kotlin.multiplatform.library` | DTOs, KMP ViewModels, repositories, the Ktor client and the SQLDelight cache. Also used by `server` for the DTOs and the blueprint coordinate limits both sides apply. |
| `server` | `io.quarkus` | The Quarkus REST API with its own Panache entities. |
| `iosApp` | — (Xcode) | The SwiftUI shell that hosts the `composeApp` framework. |

The quality gate (`./gradlew prePRcheck`: ktlint, detekt, tests and the app builds) is described in ADR-016 and [testing.md](../testing.md).

---

## Level 2 — Container

```mermaid
C4Container
  title Container Diagram — GreenPrint

  Person(user, "Plant Owner", "Uses any of the client apps to manage plants.")

  System_Ext(oidcProvider, "OIDC Provider", "Identity provider for OAuth 2.0 / OIDC (e.g. Keycloak). Issues and signs JWT access tokens.")
  System_Ext(llmApi, "LLM Vision API (Roadmap)", "AI plant identification and care-profile generation.")

  System_Boundary(gp, "GreenPrint") {

    Container(androidApp, "Android App", "androidApp / Android", "Primary mobile interface for spatial plant management. Hosts the composeApp UI.")
    Container(iosApp, "iOS App", "iosApp / SwiftUI + Compose Multiplatform", "Native iOS shell around the shared Compose UI.")
    Container(desktopApp, "Desktop App", "composeApp / JVM", "Desktop client for power users.")
    Container(webApp, "Web App", "composeApp / Wasm", "Browser-based client — no install required.")

    Container(shared, "Shared Module", "Kotlin Multiplatform · Ktor · Koin · SQLDelight", "Domain DTOs, KMP ViewModels, and Offline-First Repositories with a local SQLDelight cache (on the web: SQLite in a web worker, saved to IndexedDB).")

    Container(server, "Backend Server", "Quarkus / Kotlin · Hibernate Reactive Panache · quarkus-oidc", "REST API. Owns its own Panache entity layer (independent of shared DTOs). Validates Bearer tokens issued by the OIDC provider.")
    ContainerDb(db, "Server Database", "PostgreSQL + Atlas Migrations", "Persists spaces, rooms, plants, watering events and users. Schema lifecycle managed by Atlas (ADR-014).")
  }

  Rel(user, androidApp, "Uses", "Touch UI")
  Rel(user, iosApp, "Uses", "Touch UI")
  Rel(user, desktopApp, "Uses", "Desktop UI")
  Rel(user, webApp, "Uses", "Browser")

  Rel(androidApp, shared, "Uses")
  Rel(iosApp, shared, "Uses")
  Rel(desktopApp, shared, "Uses")
  Rel(webApp, shared, "Uses")

  Rel(shared, oidcProvider, "Authenticates user via", "OIDC / OAuth 2.0 (planned)")
  Rel(shared, server, "Calls REST API with", "HTTPS · Bearer JWT")
  Rel(server, oidcProvider, "Validates tokens against", "JWKS / HTTPS")
  Rel(server, db, "Reads & writes", "Reactive PostgreSQL client")
  Rel(server, llmApi, "Requests plant profiles from", "HTTPS (Future Implementation)")
```

**Status notes**
- **Login**: the clients do not run an OIDC flow yet; a no-op `AuthRepository` sends a mock token (ADR-011). In the dev profile the server turns OIDC off and takes any bearer token as the user (`DevBearerAuthMechanism`); tests use `@TestSecurity`.
- **Notifications**: local watering reminders (AlarmManager, UNUserNotificationCenter) are specified in [notifications.md](../features/notifications.md) and not built yet.
- **Web cache**: the Wasm app's cache is SQLite (sql.js) in a web worker (`composeApp/src/webMain/resources/greenprint-db.worker.js`) behind SQLDelight's `WebWorkerDriver`, saved to IndexedDB after each committed write (ADR-017). The schema is generated asynchronously; the web entry point opens the cache before Koin starts. Wasm is the only web target.

---

## Level 3 — Component (Shared Module)

```mermaid
C4Component
  title Component Diagram — Shared Module (KMP)

  Container_Boundary(ui_layer, "UI Layer (composeApp)") {
    Component(screens, "Feature Screens", "Compose", "State-aware UI components.")
  }

  Container_Boundary(shared_logic, "Shared Module") {
    Component(vm, "KMP ViewModels", "AndroidX Lifecycle", "Expose a StateFlow of UI state and a SharedFlow of one-shot effects.")
    Component(repo, "Offline-First Repository", "Kotlin", "Orchestrates data between Local Store and API Client. Blueprint moves and waterings are written through the local store first.")
    Component(db_store, "Local Store", "SQLDelight", "Persistent local cache for offline-first capabilities.")
    Component(api_client, "Remote API Client", "Ktor", "Encapsulates REST communication and authentication headers.")
  }

  Rel(screens, vm, "Observes State / Dispatches Events")
  Rel(vm, repo, "Calls domain actions")
  Rel(repo, db_store, "Reads/Writes (Single Source of Truth)")
  Rel(repo, api_client, "Synchronizes with (Foreground)")
```

## Level 3 — Component (Backend Server)

```mermaid
C4Component
  title Component Diagram — Backend Server (Quarkus)

  Container_Boundary(quarkus_app, "Backend Server") {
    Component(resource, "JAX-RS Resources", "Quarkus / Kotlin coroutines", "Entry points for REST calls. Validate input (Validation.kt), check ownership and run each call in a transaction.")
    Component(auth_filter, "OIDC Security Filter", "Quarkus OIDC", "Intercepts requests to validate Bearer JWTs; a dev bearer mechanism stands in when OIDC is off.")
    Component(service, "UserService", "CDI Bean", "Resolves the current user from the security identity.")
    Component(panache, "Panache Entities", "Hibernate Reactive", "Data access layer using the Active Record pattern.")
  }

  System_Ext(oidc, "OIDC Provider", "External Identity Provider")
  System_Ext(pg, "PostgreSQL", "Database Server")

  Rel(resource, auth_filter, "Authenticates via")
  Rel(auth_filter, oidc, "Validates token against")
  Rel(resource, service, "Asks for the current user")
  Rel(resource, panache, "Uses for persistence")
  Rel(panache, pg, "Reads/Writes via", "Reactive SQL Client")
```

---

## Dynamic Diagram — Synchronization Flow

This diagram shows the "Write-Through Cache" and "Last Write Wins" strategy as built for moving a plant on the Spatial Blueprint ([spatial_blueprint.md](../features/spatial_blueprint.md)). Quick Water follows the same two phases: the watering is logged in `WateringEventEntity` and the plant's `lastWateredDate` moves locally, then `POST /plants/{id}/waterings` confirms it (inventory.md §5, §6.2). The other plant writes still call the API first and cache its answer.

```mermaid
sequenceDiagram
    autonumber
    participant U as User
    participant UI as Compose UI
    participant VM as PlantViewModel
    participant R as PlantRepository
    participant L as Local DB (SQLDelight)
    participant A as Remote API (Ktor)

    U->>UI: Drops a plant marker
    UI->>VM: movePlant(id, x, y)
    VM->>R: updatePlantPosition(id, x, y)

    rect rgb(240, 248, 255)
    Note over R,L: Phase 1: Local Write
    R->>L: Save x, y (status=S_PENDING, updatedAt=now)
    L-->>VM: Room's plants re-emitted with the new position
    VM-->>UI: Update View State
    end

    rect rgb(255, 250, 240)
    Note over R,A: Phase 2: Remote Sync
    R->>A: PUT /plants/{id}/position {x, y}
    alt 200 OK
        A-->>R: Updated plant
        R->>L: status=S_SYNCED if updatedAt is unchanged
    else Offline or error
        R->>L: status=S_ERROR if updatedAt is unchanged
        VM-->>UI: Snackbar "saved on this device"
    end
    end

    Note over R,A: The next refresh of the room first resends every unsent watering and every row that is not S_SYNCED,<br/>then fetches; a row with an unconfirmed change is never overwritten by the server's copy.
```

---

## Level 4 — Deployment

The target deployment; the repository ships no deployment manifests yet.

```mermaid
C4Deployment
  title Deployment Diagram — GreenPrint

  Deployment_Node(mob, "User Device", "Mobile/Tablet") {
    Deployment_Node(android, "Android OS", "Android 7.0+ (API 24)") {
      Container(android_bin, "Android App", "APK/AAB", "Native Runtime")
    }
    Deployment_Node(ios, "iOS", "iOS 18.2+") {
      Container(ios_bin, "iOS App", "IPA", "Native Runtime")
    }
  }

  Deployment_Node(computer, "User Computer", "macOS / Windows / Linux") {
    Container(desktop_bin, "Desktop App", "JVM", "Compose Multiplatform for Desktop")
  }

  Deployment_Node(web, "User Browser", "Modern Browser") {
    Deployment_Node(browser, "V8 / SpiderMonkey / JavaScript Engine") {
      Container(wasm_app, "Web App", "Wasm", "Compose Multiplatform for Web")
    }
  }

  Deployment_Node(cloud, "Cloud Infrastructure", "Kubernetes / Docker Compose") {
    Deployment_Node(k8s_ns, "GreenPrint Namespace") {
      Container(backend, "API Server", "Quarkus (JVM or native executable)", "Containerized REST API")
      Container(postgres, "Database", "PostgreSQL 16", "Managed or Containerized Instance")
    }

    Deployment_Node(idp_node, "Identity Provider") {
      Container(keycloak, "Keycloak / OIDC", "OIDC Provider", "SSO & Identity Management")
    }
  }

  Rel(android_bin, backend, "HTTPS", "TLS 1.3")
  Rel(ios_bin, backend, "HTTPS", "TLS 1.3")
  Rel(desktop_bin, backend, "HTTPS", "TLS 1.3")
  Rel(wasm_app, backend, "HTTPS", "TLS 1.3")
  Rel(backend, postgres, "Reactive Wire Protocol", "Port 5432")
  Rel(backend, keycloak, "JWKS / OIDC", "HTTPS")
```
