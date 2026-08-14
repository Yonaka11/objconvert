# StarByGiGi Scanner (Android app)

Open this `android-app/` folder directly as a project in **Android Studio (Iguana or newer)**.

- Android Studio will sync Gradle automatically. This repo does not ship the binary
  `gradle/wrapper/gradle-wrapper.jar`; Android Studio will offer to fetch/regenerate it on first open
  (or run `gradle wrapper --gradle-version 8.6` once yourself if you prefer the command line).
- Run on a real device if possible — the camera preview works in an emulator too, but a physical phone
  makes barcode scanning much more reliable.
- On first launch, tap the gear icon in the top bar and paste in your Apps Script Web App URL + token
  (see `../apps-script/README.md`).

Package: `com.starbygigi.pricescan` · min SDK 26 · target/compile SDK 34 · Kotlin + CameraX + ML Kit.
