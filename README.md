# NotifManager

NotifManager is an Android app for capturing, organizing, batching, and reviewing notifications. It uses rules, schedules, and insight views to help reduce notification noise while keeping important items accessible.

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

## Notes

The app requires Android notification access permissions to capture notifications. Generated build outputs, local IDE state, and APK artifacts are excluded from Git by `.gitignore`.
