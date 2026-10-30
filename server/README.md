# 🌿 GreenPrint — Server

[![Quarkus](https://img.shields.io/badge/Quarkus-3.40-4695EB?logo=quarkus)](https://quarkus.io)
[![Kotlin](https://img.shields.io/badge/kotlin-2.4-blue.svg?logo=kotlin)](http://kotlinlang.org)
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

Dev mode needs Docker: Dev Services start PostgreSQL 16 in a container, the same major version as the tests and Atlas (see [Testing](#-testing)). In dev mode:
- OIDC is off and any `Authorization: Bearer <token>` is taken as the user's id (`DevBearerAuthMechanism`). The apps send a mock token (ADR-011).
- A user without a space gets a demo one: three rooms and six plants spread over their blueprints, last watered at different times so the list shows healthy, thirsty and dry plants (`DevDataSeeder`).
- CORS allows `http://localhost:8081`, where `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` serves the web app.
- Hibernate updates the schema itself; production schema changes are Atlas migrations in `server/migrations/` (ADR-014).

---

## 🔌 API

| Method | Path | What it does |
| :--- | :--- | :--- |
| `GET`, `POST` | `/spaces` | List the user's spaces, create one |
| `GET`, `PUT`, `DELETE` | `/spaces/{id}` | Read, rename, delete a space |
| `POST` | `/spaces/{spaceId}/rooms` | Create a room |
| `PUT`, `DELETE` | `/rooms/{id}` | Rename a room; delete an empty one (a room with plants, archived ones too, answers `409` `room_not_empty`) |
| `GET`, `POST` | `/rooms/{roomId}/plants` | List a room's active plants, create one |
| `PUT`, `DELETE` | `/plants/{id}` | Edit, delete a plant |
| `PUT` | `/plants/{id}/position` | Move a plant on its room's blueprint: `{"x": 0.25, "y": 0.75}` |
| `POST` | `/plants/{id}/waterings` | Record a watering: `{"id": "<uuid>", "timestamp": "<ISO-8601 instant>"}`; returns the plant with its `lastWateredDate`. A resent id is stored once |
| `POST` | `/plants/{id}/archive` | Archive a plant with a reason: `DIED`, `GIFTED` or `SOLD` |

Every call is scoped to the current user: another user's space, room or plant answers `404` (listing the plants of another user's room returns an empty list). Invalid input answers `400` with an `ApiError` body (`{"code": ..., "field": ...}`, see `resource/Validation.kt`): names are trimmed and must be 1–128 characters (`name_blank`, `name_too_long`); blueprint coordinates must lie in 0.0..1.0, edges included (`position_out_of_range`); the watering interval must be 1–365 days (`interval_out_of_range`); an archive reason must be one of the three (`archive_reason_invalid`); a watering needs a UUID id (`watering_id_invalid`) and a timestamp that parses and is not in the future (`timestamp_invalid`). A duplicate name answers `409`. Every plant carries `updatedAt`, the time of its last change on the server.

---

## 🧪 Testing

```bash
./gradlew :server:test
```

The `@QuarkusTest` resource tests run against PostgreSQL 16 from Quarkus Dev Services (Testcontainers), so they need a running Docker daemon. With [Colima](https://github.com/abiosoft/colima) on macOS:

```bash
colima start
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
./gradlew :server:test
```

Docker Desktop needs no variables. The server tests are also part of the repository's PR gate, `./gradlew prePRcheck` (see [docs/testing.md](../docs/testing.md)).

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
./server/build/*-runner
```

---

## 📚 Related Guides

- [Quarkus Gradle Tooling](https://quarkus.io/guides/gradle-tooling)
- [REST Jackson](https://quarkus.io/guides/rest#json-serialisation) — Jackson serialization for Quarkus REST
- [Quarkus Kotlin](https://quarkus.io/guides/kotlin) — Kotlin and coroutines support
