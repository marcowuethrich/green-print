# 🌿 GreenPrint — Server

[![Quarkus](https://img.shields.io/badge/Quarkus-3.x-4695EB?logo=quarkus)](https://quarkus.io)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Java](https://img.shields.io/badge/Java-25-orange.svg?logo=openjdk)](#)

The **GreenPrint** backend — a [Quarkus](https://quarkus.io) application written in Kotlin, serving as the cloud layer for the KMP client apps.

> All commands below are intended to be run from the **repository root**, not from this directory.

---

## 💻 Development

Run in dev mode with live reload enabled:

```bash
./gradlew :server:quarkusDev
```

The Quarkus Dev UI is available while in dev mode at <http://localhost:8080/q/dev/>.

---

## 📦 Packaging & Running

Build the application:

```bash
./gradlew :server:build
```

This produces `build/quarkus-app/quarkus-run.jar`. Dependencies are placed in `build/quarkus-app/lib/`, so this is **not** an über-jar.

```bash
java -jar server/build/quarkus-app/quarkus-run.jar
```

To build an über-jar instead:

```bash
./gradlew :server:build -Dquarkus.package.jar.type=uber-jar
```

```bash
java -jar server/build/*-runner.jar
```

---

## 🚀 Native Executable

Build a native binary using GraalVM:

```bash
./gradlew :server:build -Dquarkus.native.enabled=true
```

Or build inside a container if GraalVM is not installed locally:

```bash
./gradlew :server:build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

Run the native binary:

```bash
./server/build/greenprint-1.0.0-SNAPSHOT-runner
```

---

## 📚 Related Guides

- [Quarkus Gradle Tooling](https://quarkus.io/guides/gradle-tooling)
- [REST Jackson](https://quarkus.io/guides/rest#json-serialisation) — Jackson serialization for Quarkus REST
- [Quarkus Kotlin](https://quarkus.io/guides/kotlin) — Kotlin and coroutines support
