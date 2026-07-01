# Secure Notes — Agent Guide

## Build & Run

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release (needs key.properties)
```

Key signing loaded from `key.properties` (release keystore). Debug builds also sign with release config.

Secrets (`GEMINI_API_KEY`) loaded from `.env` via Secrets Gradle Plugin. See `.env.example`.

## Tests

```bash
./gradlew test                   # unit + Robolectric tests
./gradlew connectedCheck         # instrumented tests on device/emulator
```

- **Unit tests**: `app/src/test/java/` — JUnit 4 + Robolectric + Roborazzi.
- **Instrumented tests**: `app/src/androidTest/java/`.
- **Screenshot tests** (Roborazzi) output PNGs to `app/src/test/screenshots/` and require `@GraphicsMode(GraphicsMode.Mode.NATIVE)`.
- Room + Moshi use KSP — annotation processing changes require a clean build.

## Architecture

Single-module Android app (`:app`). MVVM with Jetpack Compose (Material 3), Room, and `StateFlow`.

| Layer | Path | Key files |
|---|---|---|
| UI / Navigation | `com.example.ui` | `MainActivity.kt`, `NoteEditorScreen.kt`, `DrawingCanvasScreen.kt`, `MediaViewerScreen.kt` |
| ViewModel | `com.example.ui.viewmodel` | `NotesViewModel.kt` |
| Data (Room) | `com.example.data.local` | `NoteDatabase.kt`, `NoteDao.kt` |
| Model | `com.example.data.model` | `Note.kt`, `Tag.kt`, `ListItem.kt` |
| Encryption | `com.example.data.security` | `EncryptionUtils.kt` (AES-256/GCM, PBKDF2) |
| Sync | `com.example.data.sync` | `GoogleDriveSyncService.kt` |
| Utils | `com.example.util` | `RichTextParser.kt`, `ExportUtils.kt` |

**Entrypoint**: `com.example.MainActivity` (package `com.example`, applicationId `com.estrin217.securenotes`).

## Key Conventions & Gotchas

- **Room DB** uses `.fallbackToDestructiveMigration()` — schema changes destroy data.
- **Configuration cache** is on (`org.gradle.configuration-cache=true`). Invalidate with `--no-configuration-cache` if build acts stale.
- **Kotlin incremental compilation disabled** (`kotlin.incremental=false` in `gradle.properties`).
- **WYSIWYG editor** stores rich text as custom HTML-like tags (`<b>`, `<color=...>`, `<cl>`, etc.). Parsed by `RichTextParser`.
- **Native localizations**: `values/` (en), `values-es-rVE/` (es-VE), `values-pt-rBR/` (pt-BR).
- **Formatting** not configured; no explicit formatter.
- `compileSdk = targetSdk = 36`, `minSdk = 24`.
- Gradle 9.5.1, AGP 9.2.1, Kotlin 2.2.10.
