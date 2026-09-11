# Guía de principios de programación — Secure Notes

> Este documento es la referencia en español de los principios de ingeniería del
> proyecto. La descripción operativa (rutas, capas, gotchas de build) vive en
> `AGENTS.md`. Si ambos discrepan, manda `AGENTS.md` y debe corregirse aquí.

## 1. Stack tecnológico

* **Lenguaje:** Kotlin 2.4.10.
* **UI:** Jetpack Compose (BOM 2026.06.01) con Material Design 3 Expressive.
* **Estado:** MVVM + `StateFlow` y `collectAsStateWithLifecycle()`. Sin otras librerías reactivas.
* **Persistencia:** Room 2.8.4 (única fuente local). KSP para Room y Moshi.
* **Seguridad:** AES-256-GCM + PBKDF2-HMAC-SHA256 (200 000 iteraciones). Salt e IV
  aleatorios por nota (`SecureRandom`). La contraseña maestra solo vive en memoria
  durante la sesión; los respaldos se cifran en el dispositivo **antes** de subir a Drive.
* **Red local:** OkHttp 5.4.0 (Ollama / LM Studio), Retrofit 3.0.0 + Moshi 1.15.2 (Drive).
* **IA local:** llama.cpp nativo (JNI/NDK, solo `arm64-v8a`) vía binding `llama.android`
  (`:lib`, fachada `com.arm.aichat.AiChat`); `OnDeviceService` envuelve `LlamaCppEngine`.
* **Licencias runtime:** ver `THIRD-PARTY-NOTICES` (raíz) y su fuente de verdad en app,
  `app/src/main/assets/oss-licenses.json` (pantalla Ajustes > Legal > Licencias).

## 2. DRY (No te repitas)

* Los patrones de UI repetidos se centralizan en widgets compartidos
  (`SettingsSectionTitle`, `SettingsCardGroup`, `SettingsSwitchTile`, `SettingsListTile`).
* El sistema de etiquetas ricas tiene una única fuente de verdad: `RichTextParser`
  (etiquetas `<b>`, `<color=…>`, `<url=…>`, etc.) más `HtmlTagParser` y los conversores
  (`RichTextConverter`, `MarkdownConverter`, `NoteContentBlockConverter`).
* La lista de librerías OSS tiene una única fuente de verdad: `oss-licenses.json`.
  Ni `THIRD-PARTY-NOTICES` ni `LicensesScreen` deben inventariar por su cuenta.

## 3. SOLID (aplicado a este código, no a ejemplos genéricos)

| Principio | Aplicación real |
|---|---|
| **SRP** | Cada ViewModel atiende una pantalla (`NotesViewModel`, `AiViewModel`, `BackupViewModel`…); cada DAO su tabla (`NoteDao`, `TagDao`); el cifrado vive en `CipherService` / `EncryptionServiceImpl` + `KeyDerivation`; los Composables son UI pura. |
| **OCP** | Nuevos `BlockType` o exportadores (`export/Txt, Markdown, Pdf, Html, Json`) se añaden extendiendo parámetros/tipos, sin reescribir el editor. |
| **LSP** | Las implementaciones sustituyen a sus interfaces sin romper llamadas (`PreferencesRepository` → `SharedPreferencesRepository`; `CloudSyncManager` → `GoogleDriveSyncService`; `AIService` → `OllamaService` / `OnDeviceService`). |
| **ISP** | Parámetros finos en Composables y callbacks de un solo método con lambdas Kotlin, en vez de interfaces anchas. |
| **DIP** | Inyección por constructor mediante `ViewModelProvider.Factory` en `MainActivity` (sin framework DI). Los DAO, `CipherService` y servicios de sync entran por constructor para poder testearse. |

## 4. Código limpio

* Funciones de **≤ 20 líneas** con una sola responsabilidad.
* Nombres que revelan intención (`encryptContent`, `toggleDarkMode`, `searchNotes`).
* **Guard clauses** antes que `if`/`else` anidados.
* Cadenas de nulables de **máximo 2 niveles** (`a?.b?.c` como tope).
* Comentarios que explican el **porqué** (casos borde, trade-offs, semántica nativa),
  nunca el qué.
* Singletons `object` solo para ayudantes sin dependencias (`KeyDerivation`,
  `RichTextParser`). Nada de acceso estático a dependencias.

## 5. KISS (simple antes que inteligente)

* App de un solo módulo (`:app` más módulos propios `:visor-pdf`, `:visor-media`,
  `:code-tools` y el binding `:lib`); DI manual, sin framework.
* `StateFlow` + `collectAsStateWithLifecycle()`; Room como única persistencia
  (sin caché separada ni ORM adicional).
* Navegación con la jerarquía sellada `Screen` + `Navigator` y transiciones
  `AnimatedContent`; sin rutas especulativas.

## 6. YAGNI (no lo vas a necesitar)

* Sin abstracción `repository` mientras los DAO basten; las interfaces existen solo
  donde hay sustitución real (tests u otra implementación: `CipherService`,
  `PreferencesRepository`, `CloudSyncManager`, `AIService`).
* Sin feature flags, código muerto ni navegación especulativa.
* Sin dependencias nuevas si la plataforma ya las trae (`org.json` del framework,
  `JSONObject` para `oss-licenses.json` en vez de añadir un parser).

## 7. Manejo de errores y robustez

* Cifrado: `CipherService` devuelve `Result<String>`; sync/E-S: `Result<T>`.
  El estado de pantalla usa clases dedicadas (`AuthState`, `ListState`, `SyncState`),
  no un sellado genérico Loading/Success/Error.
* Validar en el borde de la UI; los fallos de cifrado se muestran explícitos,
  nunca tumban la UI.
* **Nunca tragar excepciones en silencio:** todo `catch` lleva al menos `Log.e()`
  o reenvuelve el error.
* Room, ficheros y red van en `Dispatchers.IO`; el loader de licencias
  (`loadOssLicenses`) lee `assets` en `Dispatchers.IO` y cae a una lista mínima
  si el JSON falta.

## 8. Testing

* Unitarios + Robolectric + Roborazzi con `./gradlew test`.
* Robolectric con `@Config(sdk = [36])` (= `targetSdk`, no `compileSdk`).
* Screenshots Roborazzi con `@GraphicsMode(NATIVE)`; salidas en `app/src/test/screenshots/`.
* Room + Moshi usan KSP: tras cambiar anotaciones, build limpio
  (`clean --no-configuration-cache`; hay bug conocido de caché).
* Sin tests instrumentados (`connectedCheck`) sin dispositivo/emulador.

## 9. Seguridad y datos (no negociable)

* Todo el cifrado/descifrado ocurre **solo** en el dispositivo.
* Sin la contraseña maestra es matemáticamente imposible descifrar.
* Ni desarrollador, ni Google, ni terceros pueden leer notas cifradas.
* **Gotcha Room:** `.fallbackToDestructiveMigration()` — un cambio de esquema
  destruye datos en silencio. Toda migración debe tratarse como crítica.

## 10. IA nativa (semántica del binding, obligatoria)

* El motor JNI es singleton: `setSystemPrompt()` solo **una vez por carga** (limpia
  KV + historial); el contexto se acumula entre peticiones con el mismo prompt.
* `LlamaCppEngine` aísla: recarga si cambia el system prompt o el estado no es
  `ModelReady`; vía rápida en caso contrario.
* **Jamás llamar `destroy()`** (envenena el singleton del proceso);
  solo `unload()`/`cleanUp()`.
* `maxTokens` puede excederse hasta ~longitud del prompt (doble conteo aguas arriba).
* Temperatura (0.3) y contexto (8192) los fija el upstream; los `sampling params`
  de `AiRequest` se ignoran.

## 11. Cómo contribuir (checklist)

1. ¿Cabe en ≤ 20 líneas con guard clauses? Si no, dividir.
2. ¿Nueva dependencia runtime? Añadirla a `oss-licenses.json` **y** a
   `THIRD-PARTY-NOTICES` (nombre, versión, SPDX, titular, URL, uso). Alcance:
   solo lo que viaje en el APK.
3. ¿Cambio en `DataBlock`/migración legacy? Mantener `fromLegacyHtml` y las regex
   legacy funcionando.
4. ¿Strings nuevos? Añadirlos en los 9 locales (`values`, `es-rVE/rES`, `pt-rBR/rPT`,
   `fr`, `it`, `en-rGB`, `b+es+419`).
5. ¿KSP/Room/Moshi tocados? `clean --no-configuration-cache` + `./gradlew test`.
