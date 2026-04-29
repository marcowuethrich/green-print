# Architecture Overview

GreenPrint is a **Kotlin Multiplatform** spatial houseplant inventory app. Shared business logic and UI run across Android, iOS, Desktop (JVM), and Web (Wasm), backed by a Quarkus REST server.

---

## Level 2 — Container

```mermaid
C4Container
  title Container Diagram — GreenPrint

  Person(user, "Plant Owner", "Uses any of the client apps to manage plants.")

  System_Ext(oidcProvider, "OIDC Provider", "Identity provider for OAuth 2.0 / OIDC (e.g. Keycloak). Issues and signs JWT access tokens.")
  System_Ext(llmApi, "LLM Vision API", "AI plant identification and care-profile generation.")
  System_Ext(pushService, "Push Notification Service", "FCM / APNs for watering reminders.")

  System_Boundary(gp, "GreenPrint") {

    Container(androidApp, "Android App", "Compose Multiplatform / Android", "Primary mobile interface for spatial plant management.")
    Container(iosApp, "iOS App", "Compose Multiplatform / iOS", "Native iOS client sharing the Compose UI layer.")
    Container(desktopApp, "Desktop App", "Compose Multiplatform / JVM", "Desktop client for power users.")
    Container(webApp, "Web App", "Compose Multiplatform / Wasm", "Browser-based client — no install required.")

    Container(shared, "Shared Module", "Kotlin Multiplatform · Ktor Client · Koin · KMP ViewModel", "Domain DTOs, Ktor HTTP client, Koin DI modules, and ViewModels shared across all client targets. State is held in-memory (online-only; local cache can be added later).")

    Container(server, "Backend Server", "Quarkus / Kotlin · Hibernate Panache · quarkus-oidc", "REST API. Owns its own Panache entity layer (independent of shared DTOs). Validates Bearer tokens issued by the OIDC provider.")
    ContainerDb(db, "Server Database", "PostgreSQL + Atlas Migrations", "Persists plant profiles, user data, and watering schedules. Schema lifecycle managed by Atlas.")
  }

  Rel(user, androidApp, "Uses", "Touch UI")
  Rel(user, iosApp, "Uses", "Touch UI")
  Rel(user, desktopApp, "Uses", "Desktop UI")
  Rel(user, webApp, "Uses", "Browser")

  Rel(androidApp, shared, "Uses")
  Rel(iosApp, shared, "Uses")
  Rel(desktopApp, shared, "Uses")
  Rel(webApp, shared, "Uses")

  Rel(shared, oidcProvider, "Authenticates user via", "OIDC / OAuth 2.0")
  Rel(shared, server, "Calls REST API with", "HTTPS · Bearer JWT")
  Rel(server, oidcProvider, "Validates tokens against", "JWKS / HTTPS")
  Rel(server, db, "Reads & writes", "JDBC")
  Rel(server, llmApi, "Requests plant profiles from", "HTTPS")
  Rel(server, pushService, "Sends notifications via", "HTTPS")
```
