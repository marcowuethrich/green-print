# 🌿 GreenPrint

[![Kotlin](https://img.shields.io/badge/kotlin-2.3-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![KMP](https://img.shields.io/badge/Kotlin_Multiplatform-Enabled-7f52ff?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Backend](https://img.shields.io/badge/Backend-Quarkus-ff0044?logo=quarkus)](https://quarkus.io/)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS%20%7C%20Web%20%7C%20Desktop-lightgrey.svg)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**GreenPrint** is a high-performance spatial houseplant inventory system built with **Kotlin Multiplatform (KMP)** and **Quarkus**. It serves as a reference implementation for enterprise-grade cross-platform development, featuring shared business logic, reactive state management, and a visually immersive spatial UI.

---

## 🏗️ Architectural Overview

GreenPrint follows a **clean, modular architecture** designed for scalability and maintainability across mobile, web, and desktop platforms.

- **Unified Shared Core**: All business logic, state management (ViewModels), and network infrastructure live in a single `shared` module, ensuring 100% consistency across Android, iOS, JVM, and Wasm.
- **Modern UI**: Driven by **Compose Multiplatform**, allowing for a unified design system with native performance on all targets.
- **Enterprise Backend**: A reactive **Quarkus** service providing robust OIDC-secured APIs and high-efficiency persistence.

---

## 🛠️ Full Tech Stack

### Frontend (KMP & Compose)
* **UI Framework**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) for consistent, declarative UI.
* **Navigation**: [Voyager](https://voyager.adriel.cafe/) with state-aware stack management.
* **Dependency Injection**: [Koin](https://insert-koin.io/) for cross-platform DI.
* **Networking**: [Ktor Client](https://ktor.io/) with serialization and reactive flows.
* **Persistence**: [SQLDelight](https://cashapp.github.io/sqldelight/) for type-safe, multiplatform database access.

### Backend (Quarkus)
* **Core**: [Quarkus](https://quarkus.io/) — The Supersonic Subatomic Java Framework.
* **Persistence**: [Hibernate Panache](https://quarkus.io/guides/hibernate-panache) for streamlined data access.
* **Security**: [OIDC / OAuth 2.0](https://quarkus.io/guides/security-openid-connect) with server-side token validation.
* **Migrations**: [Atlas](https://atlasgo.io/) for modern, declarative database schema management.

### Quality & Engineering
* **UI Testing**: Instantaneous cross-platform UI tests using `waitUntil` and global animation control (skipping animations in test mode for maximum speed).
* **Local Verfication**: Optimized JVM-target testing workflow (`./gradlew :composeApp:jvmTest`) for rapid feedback loops.
* **CI/CD Ready**: Strict architectural rules enforced via documentation-first engineering (ADRs).

---

## 📁 Project Structure

* **`/shared`**: The brains. Contains domain models, Ktor infrastructure, SQLDelight schemas, and KMP ViewModels. Consumed by all client targets.
* **`/composeApp`**: The face. Pure UI layer using Compose Multiplatform. Targets Android, iOS, Desktop (JVM), and Web (Wasm).
* **`/server`**: The backbone. Quarkus-based microservice handling persistence and auth.
* **`/docs`**: The manual. [Architecture Decision Records (ADRs)](docs/architecture/decisions.md) and [Testing Strategy](docs/testing.md).

---

## 💻 Developer Guide

### Prerequisites
* **JDK 21+**
* **Android Studio / IntelliJ IDEA** (with KMP plugin)
* **Atlas CLI** (for DB migrations)

### Essential Commands

| Target | Command |
|---|---|
| **Server (Dev Mode)** | `./gradlew :server:quarkusDev` |
| **Android** | `./gradlew :composeApp:assembleDebug` |
| **Desktop (JVM)** | `./gradlew :composeApp:run` |
| **Web (Wasm)** | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` |
| **Full UI Test Suite** | `./gradlew :composeApp:jvmTest` |
| **DB Migration** | `atlas migrate apply --url $DB_URL` |

---

## 🚀 Roadmap

- [x] High-performance UI testing infrastructure (Zero-delay strategy).
- [x] OIDC-secured Quarkus backend with Panache persistence.
- [x] Atlas-driven declarative database migrations.
- [ ] **Phase 1: Inventory Management** — Room CRUD and Plant lifecycle tracking.
- [ ] **Phase 2: Spatial Blueprint** — Interactive 2D mapping and drag-and-drop placement.
- [ ] **Phase 3: Intelligence** — AI-assisted plant care profiles via Vision LLMs.

## 🤝 License
Licensed under the [MIT License](LICENSE).