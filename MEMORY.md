# MEMORY.md — secure-notes

Memoria entre sesiones. Mantenerla breve (máximo ~50 líneas); las reglas permanentes viven en `AGENTS.md`.

## Estado actual
- **Spec 027 implementada y verificada en dispositivo** (2026-10-17): **una nota nueva ya no se duplica**.
  Specs 016–027 implementadas y verificadas.
- **Limpieza de código muerto (2026-10-17)**: 69 archivos, **–3 482 líneas**. Símbolos sin uso en `:app`,
  `:editor-de-codigo`, `:visor-pdf`, `:visor-media`; `CodeFont.kt` + 2 fuentes; `OpenUrlDialog.kt`; 2 438
  elementos `string`/`plurals` sin referencia en 9 locales; deps muertas del `build.gradle.kts` y del catálogo.
  Validado: `:app:assembleDebug` + `:app:compileDebugUnitTestKotlin` → `BUILD SUCCESSFUL` (con config cache).
- Pruebas: `:app` **816/41**, los 41 son los de siempre (plataforma). Ninguna clase roja nueva.
- El editor de notas está en **2268 líneas** (presupuesto 2268).

## Decisiones que conviene recordar
- **Purga de código muerto: qué no se toca.** Overrides, `@Preview`, entradas del `AndroidManifest`, *type
  converters* de Room y miembros usados por generación/posición (`fromLanguage`/`toLanguage`, `toolCallId`).
  Toda clave de `strings.xml` se quita **en los 9 locales a la vez**. Antes de purgar `libs.versions.toml` hay
  que mirar el módulo **externo** `:lib` (`/root/llama.cpp/examples/llama.android/lib/build.gradle.kts`):
  consume `androidx-datastore-preferences` y `junit-platform-launcher` (se restauraron por eso).
- Las reglas de la 027 (nota nueva, `DraftWriteGate`, «atrás guarda y sale») están en `AGENTS.md`: no repetirlas aquí.
- **Selección de notas: una sola regla (`ui/NoteSelection`)**, compartida por barra, los dos toques, las dos
  pulsaciones largas y el atrás; el atrás cierra el modo antes que la navegación (spec 026).
- El tono de Material 3, el catálogo compartido, `CodeEditor`/`databaseExists`, `AiModelHost` y `:markdown`
  son reglas permanentes: viven en `AGENTS.md`.

## Aprendizajes
- **La configuration cache funciona aquí**: `BUILD SUCCESSFUL` sin `--no-configuration-cache`; ese flag solo si
  la build actúa obsoleta tras KSP. Un «Gradle build daemon has been stopped: stop command received» **no** es
  un fallo de compilación: es un `gradlew --stop` de fuera.
- **Buscar código muerto**: usar el buscador con `path`/`include`; un `grep -r` por el repo entero entra en
  `build/` y se va de timeout. El detector de «una sola aparición» sirve, pero **no** para imports.
- **`mutableIntStateOf` funciona en una prueba pura de JVM**: el sistema de instantáneas de Compose no necesita
  Android, así que un estado observable con `Mutex` se prueba entero aquí, con `runBlocking`.
- Un `^` sin `RegexOption.MULTILINE` **no encuentra nada** y el invariante pasa por vacío (suelo `MIN_IMPORTS`).
- Kotlin **no admite referencias adelantadas a funciones locales**: `guardarConId` va antes que `persistNote`, y
  tiene que ser `suspend`. Una `val` sí se puede capturar desde una lambda declarada antes.
- Un `BackHandler` u `onBack` que leen un estado obligan a **declarar ese estado por encima** y a recalcularlo en
  el acto: un `LaunchedEffect` llega un fotograma tarde y el diálogo salía igual.
- `Color` es una value class: `assertEquals` con hexadecimales necesita `.toInt()`. Compilar Kotlin no valida
  `strings.xml`. Las decisiones comprobables sin `Context` van en funciones puras.

## Próximos pasos y notas
- Verificar en dispositivo la spec 020 provocando un fallo real. Spec 002: los dos goldens pendientes.
- Opcional y **fuera de la 027**: al salir de una nota nueva por la flecha atrás, la pestaña sigue siendo el
  borrador vacío junto a la nota ya escrita; promoverla recrearía el editor (ver dudas abiertas de la spec).
