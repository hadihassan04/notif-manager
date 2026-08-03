# Tide

Tide is an Android app for capturing, organizing, batching, and reviewing notifications. It uses rules, schedules, and insight views to help reduce notification noise while keeping important items accessible.

## Tech Stack

- Kotlin
- Android Jetpack Compose
- Material 3
- Room
- DataStore Preferences
- Gradle Kotlin DSL

## Project Structure

- `app/src/main/java/com/notifmanager/` - app entry point, UI, and view model code
- `app/src/main/java/com/notifmanager/core/` - rule, schedule, and insights logic
- `app/src/main/java/com/notifmanager/data/` - Room database, entities, DAO, and repository
- `app/src/main/java/com/notifmanager/notifications/` - notification capture, batching, publishing, and receivers
- `app/src/test/java/com/notifmanager/core/` - unit tests for core logic

## Requirements

- Android Studio with JDK 17 support
- Android SDK 36
- Gradle wrapper included in this repository

## Getting Started

1. Open the project in Android Studio.
2. Let Android Studio sync Gradle dependencies.
3. Create a `local.properties` file if Android Studio does not create one automatically:

   ```properties
   sdk.dir=/path/to/Android/sdk
   ```

4. Build and run the `app` configuration on an emulator or Android device.

## Commands

Run unit tests:

```sh
./gradlew testDebugUnitTest
```

Build a debug APK:

```sh
./gradlew assembleDebug
```

Clean generated build outputs:

```sh
./gradlew clean
```

## CI/CD

`.github/workflows/ci.yml` runs on every push and pull request to `main`: unit
tests, a debug build, and `lintVitalRelease`. The debug APK is attached to the
run as an artifact.

`.github/workflows/release.yml` runs when a `v*` tag is pushed (or manually via
*Run workflow*). It builds a signed, R8-minified release APK and publishes a
GitHub release containing:

- `tide-<versionName>-<versionCode>.apk`
- `mapping-<versionName>-<versionCode>.zip` — the R8 mapping, required to read
  obfuscated stack traces from that APK

### Releasing

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`, then commit.
2. Tag and push:

   ```sh
   git tag v0.2.0
   git push origin v0.2.0
   ```

### Required repository secrets

The release workflow signs the APK with the same key used locally, supplied
through secrets rather than the gitignored `keystore.properties`:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | `base64 -i notif-manager-release.jks` output |
| `KEYSTORE_PASSWORD` | `storePassword` from `keystore.properties` |
| `KEY_ALIAS` | `keyAlias` from `keystore.properties` |
| `KEY_PASSWORD` | `keyPassword` from `keystore.properties` |

The build reads these from the environment when `keystore.properties` is absent,
so local builds are unaffected. The decoded keystore is written to the runner's
temp directory, never into the workspace.

## Notes

The app requires Android notification access permissions to capture notifications. Generated build outputs, local IDE state, and APK artifacts are excluded from Git by `.gitignore`.
