# MEMORY.md — secure-notes

Memoria entre sesiones. Mantenerla breve (máximo ~50 líneas); las reglas permanentes viven en `AGENTS.md`.

## Estado actual
- **Spec 027 implementada y verificada en dispositivo** (2026-10-17), T1–T10 cerradas: **una nota nueva ya no se
  duplica**. Specs 016–027 implementadas y verificadas.
- Pruebas: `:app` **816/41** (799 + las 17 de la 027: `DraftWriteTest` 14 y tres invariantes de `EditorInvariantsTest`),
  los 41 son los de siempre. `:app:assembleDebug` compila. Ninguna clase roja nueva.
- El editor de notas **bajó a 2268 líneas** (presupuesto 2268): el `saveAction` duplicaba la lista de campos del
  guardado y el comando «/» → página tenía una **tercera** copia. `MAX_GUARDADOS` 2 → 1.

## Decisiones que conviene recordar
- **`noteId == 0` son dos hechos**: «no hay nota todavía» y «la nota es esta pero ya tiene fila`. Escribir con
  `id = noteId` inserta **una nota por escritura** (el duplicado reportado) y deducir «sin guardar» de ahí pregunta
  por lo ya guardado. Las dos reglas viven en `ui/DraftWrite.kt`: `draftWriteId` e `isDraftUnsaved`.
- **El editor recuerda el id que le devolvió su escritura** (`DraftWriteGate`, con `Mutex`), en vez de promover la
  pestaña: promover recrearía el editor a mitad de uso y perdería cursor e historial. El id se lee **dentro** del
  candado, o «adjuntar» + «atrás» insertan dos.
- **Atrás guarda y sale sin preguntar** (decidido con quien Decide): ya guardaba y ya avisaba «nota guardada»; quitar
  el diálogo es quitar la incoherencia. El aviso «¿guardar o descartar?» es de **cerrar la pestaña** con la ✕.
- El autoguardado decide con `draftWriteId(...) != 0`, no con `noteId != 0`: lo que se escribe después de adjuntar
  algo a una nota nueva también se guarda.
- **Selección de notas: una sola regla (`ui/NoteSelection`)**, compartida por barra, los dos toques, las dos
  pulsaciones largas y el atrás; el atrás cierra el modo antes que la navegación (spec 026).
- El tono de Material 3 es L\* de CIELAB, no OKLab L (traducción en `ligereza(tone)`); un acento cumple **dos**
  contrastes; catálogo compartido = módulo propio; `dynamic` no tiene paleta propia.
- Índice de archivos del editor por la fachada `CodeEditor`; comprobar `databaseExists` antes de abrir Room. El
  motor de IA solo mediante `AiModelHost`. Markdown vive en `:markdown`.

## Aprendizajes
- **`mutableIntStateOf` funciona en una prueba pura de JVM**: el sistema de instantáneas de Compose no necesita
  Android, así que un estado observable con `Mutex` se prueba entero aquí, con `runBlocking`.
- Un `^` sin `RegexOption.MULTILINE` **no encuentra nada** y el invariante pasa por vacío (suelo `MIN_IMPORTS`).
- Kotlin **no admite referencias adelantadas a funciones locales**: `guardarConId` va antes que `persistNote`, y
  tiene que ser `suspend`. Una `val` sí se puede capturar desde una lambda declarada antes.
- **Quitar una copia duplicada paga el crecimiento**: el `saveAction` con sus 17 líneas financió la puerta.
- Un `BackHandler` o un `onBack` que leen un estado obligan a **declarar ese estado por encima** y a recalcularlo en
  el acto: un `LaunchedEffect` llega un fotograma tarde y el diálogo salía igual.
- `Color` es una value class: `assertEquals` con hexadecimales necesita `.toInt()`. Compilar Kotlin no valida
  `strings.xml`. Las decisiones comprobables sin `Context` van en funciones puras.

## Próximos pasos y notas
- Verificar en dispositivo la spec 020 provocando un fallo real. Spec 002: los dos goldens pendientes.
- Opcional y **fuera de la 027**: al salir de una nota nueva por la flecha atrás, la pestaña sigue siendo el
  borrador vacío junto a la nota ya escrita; promoverla recrearía el editor (ver dudas abiertas de la spec).