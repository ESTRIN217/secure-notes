# Secure Notes — Agent Guide

## Build & Run

```bash
./gradlew assembleDebug          # debug APK (also signs with release config)
./gradlew assembleRelease        # release (needs key.properties)
./gradlew clean --no-configuration-cache  # if build acts stale (cache bug)
```

- Secrets (`GEMINI_API_KEY`) loaded from `.env` via Secrets Gradle Plugin. See `.env.example`.
- Release signing: copy `key.properties.template` → `key.properties`. Debug builds also sign with release config.
- `app/google-services.json` required for Firebase (Google services plugin).

## Tests

```bash
./gradlew test                   # unit + Robolectric + Roborazzi
```

- **Unit tests**: `app/src/test/java/` — JUnit 4 + Robolectric + Roborazzi.
- **Screenshot tests** (Roborazzi) output to `app/src/test/screenshots/` and require `@GraphicsMode(GraphicsMode.Mode.NATIVE)` + `@Config(sdk = [36])`.
- Room + Moshi use KSP — annotation processing changes require a clean build.
- No instrumented tests (`connectedCheck`) usable without a device/emulator.

## Architecture

Single-module Android app (`:app`). MVVM with Jetpack Compose (Material 3), Room, `StateFlow`.

| Layer | Path | Key files |
|---|---|---|
| UI / Navigation | `com.example.ui` | `MainListScreen.kt`, `NoteEditorScreen.kt`, `DrawingCanvasScreen.kt`, `MediaViewerScreen.kt`, `SearchScreen.kt`, `LockScreen.kt` |
| Settings UI | `com.example.ui.settings` | `SettingsScreen.kt`, `PrivacySettingsScreen.kt`, `BackupRestoreScreen.kt`, `AboutScreen.kt`, `SettingsWidgets.kt` |
| ViewModel | `com.example.ui.viewmodel` | `NotesViewModel.kt`, `ThemeViewModel.kt`, `BackupViewModel.kt`, `UpdaterViewModel.kt` |
| Data (Room) | `com.example.data.local` | `NoteDatabase.kt`, `NoteDao.kt`, `TagDao.kt` |
| Model | `com.example.data.model` | `Note.kt`, `Tag.kt`, `DecryptedNote.kt`, `NoteContentBlock.kt`, `UiState.kt`, `Attachment.kt` |
| Encryption | `com.example.data.security` | `CipherService.kt` (interface), `EncryptionServiceImpl.kt` (AES-256/GCM), `KeyDerivation.kt` (PBKDF2, 200K iterations) |
| Sync | `com.example.data.sync` | `CloudSyncManager.kt` (interface), `GoogleDriveSyncService.kt` (OkHttp impl), `SyncWorker.kt` (WorkManager) |
| Preferences | `com.example.data` | `PreferencesRepository.kt` (interface), `SharedPreferencesRepository.kt` |
| Utils | `com.example.util` | `RichTextParser.kt`, `ExportUtils.kt`, `BiometricAuthManager.kt`, `export/` (Txt, Markdown, Pdf, Html, Json exporters) |

**Entrypoint**: `com.example.MainActivity` (package `com.example`, applicationId `com.estrin217.securenotes`).

## Key Conventions

### DI
- No DI framework. ViewModels constructed via `ViewModelProvider.Factory` in `MainActivity.kt:161-175`.
- Dependencies (`NoteDatabase`, `CipherService`, `GoogleDriveSyncService`) created manually and injected through factory.

### UI / State
- `StateFlow` + `collectAsStateWithLifecycle()` for reactive UI. No additional reactive libraries.
- `UiState` uses separate data classes (`AuthState`, `ListState`, `SyncState`) rather than sealed Loading/Success/Error.
- Screen navigation via `Navigator` class with `Screen` sealed hierarchy and `AnimatedContent` transitions.

### Conventions
- Functions ≤ 20 lines. Guard clauses over nested `if`/`else`. No nullable chains deeper than 2.
- No repository pattern (yet) — DAOs consumed directly by ViewModels.
- `object` singletons only for companion helpers (`KeyDerivation`, `RichTextParser`). No static access to dependencies.

### Error Handling
- Encryption: `Result<String>` return type from `CipherService`.
- Sync I/O: `Result<T>` from `SyncService` suspend functions.
- `Log.e()` at minimum in `catch` blocks — never silent swallowing.

## Editor Content Format

WYSIWYG editor stores rich text as markdown + custom HTML-like tags parsed by `RichTextParser`/`HtmlTagParser`:

| Tag | Effect |
|---|---|
| `<b>`, `<i>`, `<u>`, `<s>` | bold, italic, underline, strikethrough |
| `<color=#RRGGBB>` | text color |
| `<bg=#RRGGBB>` | background color |
| `<url=URL>` | clickable link |
| `<h1>`–`<h3>`, `<normal>` | heading / normal size |
| `<sub>`, `<sup>` | subscript, superscript |
| `<font=family>` | serif, monospace, sans-serif, cursive |
| `<size=N>` | font size in sp |
| `<indent>`, `<ol>`/`<ul>`/`<li>`, `<item>` | indentation, lists, checklist items |
| `<cl>` | clear formatting |
| `<img>`, `<video>`, `<audio>` | media embeds |

Also supports markdown syntax: `**bold**`, `*italic*`, `` `code` ``, `~~strikethrough~~`, `[text](url)`, `# headings`, `> quote`, `- [ ]` checklists.

## Gotchas

- **Room**: `.fallbackToDestructiveMigration()` — schema changes destroy data silently.
- **Configuration cache**: `org.gradle.configuration-cache=true` in `gradle.properties`. Invalidate with `--no-configuration-cache` if build acts stale.
- **Kotlin**: `kotlin.incremental=false` in `gradle.properties`. Clean builds may be required after KSP changes.
- **Robolectric**: Unit tests use `@Config(sdk = [36])` (compileSdk). Screenshot tests require `@GraphicsMode(GraphicsMode.Mode.NATIVE)`.
- **Secrets**: Secrets plugin reads `.env` (gitignored) with fallback to `.env.example`.
- **Localizations**: `values/` (en), `values-es-rVE/` (es-VE), `values-pt-rBR/` (pt-BR), `values-fr/` (fr), `values-en-rGB/`, `values-es-rES/`, `values-pt-rPT/`, `values-b+es+419/`.
- `compileSdk = targetSdk = 36`, `minSdk = 24`.
- Gradle 9.5.1, AGP 9.2.1, Kotlin 2.2.10.
