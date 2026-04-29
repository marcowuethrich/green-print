# Architecture Overview

GreenPrint is a **Kotlin Multiplatform** spatial houseplant inventory app. Shared business logic and UI run across Android, iOS, Desktop (JVM), and Web (Wasm), backed by a Quarkus REST server.

---

## Level 2 — Container

```mermaid
C4Container
  title Container Diagram — GreenPrint

  Person(user, "Plant Owner", "Uses any of the client apps to manage plants.")

  System_Boundary(gp, "GreenPrint") {

    Container(androidApp, "Android App", "Compose Multiplatform / Android", "Primary mobile interface for spatial plant management.")
    Container(iosApp, "iOS App", "Compose Multiplatform / iOS", "Native iOS client sharing the Compose UI layer.")
    Container(desktopApp, "Desktop App", "Compose Multiplatform / JVM", "Desktop client for power users.")
    Container(webApp, "Web App", "Compose Multiplatform / Wasm", "Browser-based client — no install required.")

    Container(shared, "Shared Module", "Kotlin Multiplatform", "Core business logic and domain models. Consumed by all client targets.")

    Container(server, "Backend Server", "Quarkus / Kotlin + Hibernate Panache", "REST API for cloud sync, plant-profile management, and notification dispatch. ORM handled by Hibernate Panache.")
    ContainerDb(db, "Server Database", "PostgreSQL + Atlas Migrations", "Persists plant profiles, user data, and watering schedules. Schema lifecycle managed by Atlas.")
  }

  System_Ext(llmApi, "LLM Vision API", "AI plant identification and care-profile generation.")
  System_Ext(pushService, "Push Notification Service", "FCM / APNs for watering reminders.")

  Rel(user, androidApp, "Uses", "Touch UI")
  Rel(user, iosApp, "Uses", "Touch UI")
  Rel(user, desktopApp, "Uses", "Desktop UI")
  Rel(user, webApp, "Uses", "Browser")

  Rel(androidApp, shared, "Uses")
  Rel(iosApp, shared, "Uses")
  Rel(desktopApp, shared, "Uses")
  Rel(webApp, shared, "Uses")

  Rel(shared, server, "Syncs data with", "HTTPS / REST")
  Rel(server, db, "Reads & writes", "JDBC")
  Rel(server, llmApi, "Requests plant profiles from", "HTTPS")
  Rel(server, pushService, "Sends notifications via", "HTTPS")
```
