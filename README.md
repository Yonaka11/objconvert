# Pocket Familiar

Pocket Familiar is a free, ad-free Android app prototype for a small original animated companion that can live above other apps as a floating overlay.

This branch contains the first vertical slice:

- Jetpack Compose home screen
- overlay permission status and settings deep link
- Android 13+ notification permission request
- Start Pet / Stop Pet controls
- foreground service with persistent notification and Stop action
- `WindowManager` overlay using `TYPE_APPLICATION_OVERLAY`
- one static original placeholder pet drawn with Android canvas APIs

Autonomous walking, dragging, physics, animation frames, DataStore settings, and battery-aware behavior are planned next.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Kotlin Coroutines / StateFlow
- Android foreground service
- `WindowManager` overlays

## Android Versions

The app uses `minSdk 26` because `TYPE_APPLICATION_OVERLAY` was introduced in Android 8.0 (API 26). Supporting older versions would require legacy overlay window types that are intentionally avoided for this MVP.

The project targets the current SDK and declares a `specialUse` foreground service type for the user-controlled floating companion overlay.

## Run and Test

1. Open the project in Android Studio.
2. Sync Gradle and run the `app` configuration on a device or emulator.
3. Tap **Grant Overlay Permission** and enable "Display over other apps" for Pocket Familiar.
4. On Android 13+, grant notification permission when prompted.
5. Tap **Start Pet**.
6. Confirm that the placeholder familiar appears above other apps and that the persistent notification is shown.
7. Tap **Stop Pet** in the app or notification to remove the overlay.

## Privacy

Pocket Familiar does not include ads, analytics, accounts, subscriptions, cloud services, screen recording, accessibility services, or internet access.

## Legacy Static Page

The repository still contains the previous static `index.html` model viewer. The Android app lives in the Gradle `app` module.
