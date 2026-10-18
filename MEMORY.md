# MEMORY.md — secure-notes

Memoria entre sesiones. Mantenerla breve (máximo ~50 líneas); las reglas permanentes viven en `AGENTS.md`.

## Estado actual
- Specs 016–028 implementadas; spec 027 verificada en dispositivo: las notas nuevas ya no se duplican.
- Spec 028: los ajustes de tema viajan en la copia de seguridad; `BACKUP_SETTINGS` centraliza los valores.
- Spec 029: las suites dirigidas de HTML, catálogo y PDF pasan **26/0**. La prueba PDF usa un backend de test;
  queda validar exportación PDF real en dispositivo.
- Spec 030: `GreetingScreenshotTest` renderiza bloqueo desde estado explícito, sin `NotesViewModel`, Room ni
  Keystore. `LockScreen(viewModel)` conserva el cableado de producción. Screenshot dirigido y
  `:app:compileDebugUnitTestKotlin`: **BUILD SUCCESSFUL**. Después, la spec 002 regeneró el golden preexistente
  `greeting.png`, cuya firma PNG estaba dañada.
- Spec 002 implementada (2026-10-18): T1-T5 completadas; `./gradlew test` → **BUILD SUCCESSFUL**,
  con 842 tests, 0 fallos y 0 omitidos en `:app`. Goldens de ecuaciones y bloqueo verificados;
  `roborazzi.test.verify=true`.
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
