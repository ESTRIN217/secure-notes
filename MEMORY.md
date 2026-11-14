# MEMORY.md — secure-notes

Memoria entre sesiones. Mantenerla breve (máximo ~50 líneas); las reglas permanentes viven en `AGENTS.md`.

## Estado actual
- **2026-11-12**: `FloatingChatInputField.kt` — `ChatTextField` pasa de `TextField` (Material3) a `BasicTextField` (foundation 1.13.0-alpha01): sin contenedor ni indicador, placeholder por `decorationBox`, texto en `bodyLarge` y cursor `SolidColor(primary)`; en deshabilitado el texto se atenúa a `alpha 0.38f` porque `BasicTextField` no lo hace solo. Añadido `FloatingChatInputFieldPreview` (`@Preview(showBackground = true)`) con estado plausible. `:app:compileDebugKotlin` verde; sin strings nuevas ni cambio de comportamiento.
- **Spec 033 (2026-11-14), implementada + ampliaciones; falta verificación en dispositivo**: razonamiento y modelo en el chat IA (`specs/033-…/{spec,plan,tasks}.md`). RF-215…RF-247. Esfuerzo global al proceso en `AiViewModel` (botones esfuerzo y modelo: hoja <600 dp; ≥600 dp el modelo abre un **panel a pantalla completa**, hermano del Scaffold, réplica de «Ollama Models»: Models + subtítulo EN, búsqueda, tabla Model/Context/Status/Actions con power pulsable load/unload y ⋮ con Add/Remove favorites, Delete from disk, Hide model; favoritos corazón y ocultos `ai_hidden_models` persistidos en prefs e incluidos en la copia —**dieciséis** ajustes—; catálogo on-device completo; Context=8192 real ON_DEVICE o `context_length` de `/api/show` en Ollama). `think` top-level booleano en `/api/chat`; MAX = `num_predict -1` (Ollama) / tope 1 000 000 (llama.cpp). `./gradlew test` → **890 tests, 38 fallos (plataforma), 0 omitidos**; `assembleDebug` OK. `loadSession` restaura snapshot (RF-228, declarado).
- Specs 016–031 implementadas; spec 027 verificada en dispositivo: las notas nuevas ya no se duplican.
- **Spec 032 (2026-11-11), cerrada y verificada en dispositivo**: la nota nueva vacía se descarta. `ui/DraftEmptiness.kt` (`isNoteEmpty`) decide «vacía»; una nota existente vaciada sí se guarda (`canPersistChanges`, RF-214); la fila creada en la sesión se borra definitivo al salir (`DraftWriteGate.createdRowId`); /page, Markdown, lista, «Nuevo dibujo» e histórico intactos. `./gradlew test` → **868 tests, 38 fallos (todos de plataforma)**.
- **Spec 031 (2026-10-18)**: sitio de marca estático en `public/` (HTML/CSS planos, sin JS ni CDN) — esa es la
  raíz de Firebase Hosting, la que crea `firebase init`. `firebase.json` con `cleanUrls` + caché y **sin**
  reescritura SPA (si la hubiera, `404.html` no se serviría nunca). Proyecto: `secure-notes-502213` en
  `.firebaserc`. **Publicado en https://notes-502213.web.app** (10 ficheros, 404 propio verificado con 404 real).
  Las imágenes viven **una sola vez**, en `public/assets/`, y el `README.md` apunta ahí: la carpeta `assets/` de la
  raíz se borró para que no haya dos copias que se desfasen. Para actualizar el sitio: editar `public/` y desplegar.
- **v4.0.0 publicada** (2026-10-18): tag `v4.0.0` + release en GitHub (Latest) con los APK debug y
  release `arm64-v8a`; `versionName = "4.0.0"` / `versionCode = 4`; `CHANGELOG.md` cerrado con fecha.
- Spec 031: **Acerca de (2026-11-10)**: tarjeta de llama.cpp con logo **local**
  (`res/drawable-nodpi/llama_cpp_icon_dark.png` / `_light.png`, 1200×1200, copiados de
  `/root/llama-brand/logo/png/icon/` — asset oficial de `ggml-org/llama.brand`, CC-BY-NC-4.0 con
  permiso explícito de redistribución en software para identificar Llama.cpp; atribución en
  `THIRD-PARTY-NOTICES` y espejo en `oss-licenses.json`). Glifo según el esquema real
  (`colorScheme.background.luminance()`), tarjeta clickable → repo `ggml-org/llama.cpp`; la versión ya no es un hardcode: `app/build.gradle.kts` parsea
  `LLAMA_VERSION_MAJOR/MINOR/PATCH` (+ `LLAMA_BUILD_IS_DEV`) del `CMakeLists.txt` del checkout →
  `BuildConfig.LLAMA_CPP_VERSION` (hoy `0.9.0-dev`; fallback `"local"` si no hay checkout → CI).
  El botón web del `DeveloperCard` usa `about_website` (9 locales) y los dos botones `weight(1f)`.
- Spec 028: los ajustes de tema viajan en la copia de seguridad; `BACKUP_SETTINGS` centraliza los valores.
- Specs 029-030: pruebas dirigidas de HTML/catálogo/PDF en **26/0** (PDF con backend de test; falta validar
  PDF real en dispositivo) y captura de bloqueo desde estado explícito, sin `NotesViewModel`, Room ni Keystore.
- Spec 002 implementada (2026-10-18): T1-T5 completadas; `./gradlew test` → **BUILD SUCCESSFUL**,
  con 842 tests, 0 fallos y 0 omitidos en `:app`. Goldens de ecuaciones y bloqueo verificados;
  `roborazzi.test.verify=true`. **Verificado en un host `x86_64` (codespace)**: en `linux/aarch64`
  (host de desarrollo) siguen los 38 fallos de plataforma de Robolectric.
- Suite completa dirigida más reciente: `:editor-de-codigo` **149/0**, `:visor-pdf` **4/0**, `:lib` **1/0**.
- Limpieza de código muerto (2026-10-17): 69 archivos, –3 482 líneas; build debug y compilación de tests
  validados correctamente.
- CI (2026-10-18): `app/build.gradle.kts` referenciaba `project(":lib")` sin condicionar mientras
  `settings.gradle.kts` sí condicionaba el `include`; en CI no existía el checkout y fallaba la
  configuración. Guardado con `findProject(":lib") != null`. Verificado con `./gradlew projects` sin el
  checkout: `BUILD SUCCESSFUL`.

## Decisiones que conviene recordar
- Las reglas permanentes sobre nota nueva, tema, `AiModelHost`, `CodeEditor`, markdown, almacenamiento,
  rendimiento y seguridad viven en `AGENTS.md`; no duplicarlas aquí.
- Antes de purgar `libs.versions.toml`, revisar el módulo externo `:lib`
  (`/root/llama.cpp/examples/llama.android/lib/build.gradle.kts`): consume datastore-preferences y
  junit-platform-launcher.
- Robolectric 4.17 en Java 25 necesita `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` en tareas
  `Test` que inicializan estado Android.
- El módulo externo `:lib` usa JUnit Platform/Jupiter; API y launcher no bastan, también hace falta Jupiter
  Engine cuando se incluye el módulo.
- `mutableIntStateOf` funciona en tests JVM puros; decisiones que no necesitan `Context` van en piezas puras.

## Aprendizajes
- Configuration cache funciona; usar `--no-configuration-cache` solo ante síntomas de caché/KSP obsoleta.
- Buscar código muerto con rutas acotadas; `grep -r` global recorre `build/` y puede agotar tiempo.
- Un `^` sin `RegexOption.MULTILINE` puede hacer que un invariante pase por vacío.
- Kotlin no admite referencias adelantadas a funciones locales; `Color` requiere `.toInt()` en asserts
  hexadecimales; compilar Kotlin no valida recursos XML.
- Un `include(":x")` condicional en `settings.gradle.kts` no basta: **toda** referencia `project(":x")` en
  cualquier otro build script tiene que usar la misma condición, o la configuración falla donde el módulo
  no existe. Comprobarlo con `./gradlew projects`, no con una compilación.

## Próximos pasos
- Validar PDF real en dispositivo; verificar spec 020 provocando un fallo real.
