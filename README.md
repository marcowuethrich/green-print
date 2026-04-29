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

## 📁 Project Structure

This project leverages KMP to share business logic and UI across targets.

* **`/shared`**: Core business logic and domain models shared across all targets.
* **`/composeApp`**: Shared Compose Multiplatform UI code (`commonMain`), along with platform-specific implementations (`androidMain`, `iosMain`, `jvmMain`, `wasmJsMain`).
* **`/iosApp`**: The iOS entry point and Xcode project. Add any iOS-native SwiftUI code here.
* **`/server`**: The Quarkus backend application.

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

- [ ] Implement core KMP architecture and shared domain models.
- [ ] Set up server-side persistence with Quarkus Hibernate Panache and Atlas schema migrations.
- [ ] Build the interactive spatial blueprint UI.
- [ ] Add AI auto-generation using LLM vision models for automatic plant care profiles.

## 🤝 Contributing & License
This is primarily a personal portfolio project. Licensed under the [MIT License](LICENSE).