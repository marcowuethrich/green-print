# 🌿 GreenPrint

[![Kotlin](https://img.shields.io/badge/kotlin-2.3-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![KMP](https://img.shields.io/badge/Kotlin_Multiplatform-Enabled-7f52ff?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS%20%7C%20Web%20%7C%20Desktop-lightgrey.svg)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**GreenPrint** is a spatial houseplant inventory app built with Kotlin Multiplatform (KMP). Unlike traditional list-based trackers, GreenPrint allows users to visually map their indoor jungle using an interactive top-down blueprint of their living space.

This project serves as a showcase of modern cross-platform architecture, shared business logic, and custom UI development across multiple platforms.

---

## ✨ Core Features

* **📐 Spatial Living Space:** Create a top-down blueprint of your home and drag-and-drop plants into specific rooms.
* **🌱 Plant Archiving:** Manage plant lifecycles, tracking details like soil mix, and archiving plants that have died or been gifted.
* **💧 Smart Watering:** Track hydration with a visual barometer and get cross-device notifications when plants need water.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Multiplatform UI | Compose Multiplatform |
| Shared client logic | Kotlin Multiplatform — Ktor Client, Koin, KMP ViewModel |
| Backend | Quarkus / Kotlin — Hibernate Panache, quarkus-oidc |
| Database migrations | Atlas |
| Auth | OIDC / OAuth 2.0 (via quarkus-oidc; Testcontainers for integration tests) |

---

## 📁 Project Structure

Everything that can run on all client targets lives in `shared`. Platform-specific entry points are thin.

* **`/shared`**: The shared client library. Contains domain DTOs, the Ktor HTTP client, Koin DI modules, and KMP ViewModels. All client-side business logic lives here; state is held in-memory. Consumed by `composeApp` on every target.
* **`/composeApp`**: Compose Multiplatform UI (`commonMain`) and thin platform entry points (`androidMain`, `iosMain`, `jvmMain`, `wasmJsMain`). No business logic — only UI code.
* **`/iosApp`**: The iOS Xcode project entry point.
* **`/server`**: The Quarkus backend. Defines its own Panache entity layer (independent of the shared DTOs) and validates OIDC tokens issued by the identity provider.

---

## 💻 Getting Started

### Prerequisites
* [Android Studio](https://developer.android.com/studio) (Latest recommended)
* [Xcode](https://developer.apple.com/xcode/) (For iOS builds)
* [Kotlin Multiplatform Mobile Plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile)

### Build & Run Commands

You can run these directly from your terminal or use the run configurations in Android Studio/IntelliJ. *(Note: On Windows, use `.\gradlew.bat` instead of `./gradlew`)*

**🤖 Android:**
```bash
./gradlew :composeApp:assembleDebug
```

**🍎 iOS:**
Open the `/iosApp` directory in Xcode and run it directly from the IDE, or use the run configuration in Android Studio.

**🖥️ Desktop (JVM):**
```bash
./gradlew :composeApp:run
```

**🌐 Web (Wasm - Modern Browsers):**
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

**☁️ Server (Quarkus):**
```bash
./gradlew :server:quarkusDev
```

---

## 🚀 Roadmap

- [x] Wire up Koin DI, Ktor Client, and KMP ViewModels in `shared`.
- [x] Set up OIDC authentication (quarkus-oidc) with integration tests.
- [x] Set up server-side persistence with Quarkus Hibernate Panache and Atlas schema migrations.
- [ ] **Phase 1: Inventory Fundamentals**
    - [ ] Single living space management (Rooms: Create, Rename, Delete).
    - [ ] Plant CRUD (Species, Pot size, Soil type).
    - [ ] Manual watering tracking and due date calculation.
- [ ] **Phase 2: Visual Experience**
    - [ ] Interactive spatial blueprint UI.
    - [ ] Drag-and-drop plant placement.
- [ ] **Phase 3: Intelligence**
    - [ ] AI auto-generation using LLM vision models for automatic plant care profiles.

## 🤝 Contributing & License
This is primarily a personal portfolio project. Licensed under the [MIT License](LICENSE).