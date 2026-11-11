# Spec 032 — Tareas

> Orden de dependencia. Una tarea por sesión de trabajo (≤ 30 min). Los tests van **primero**.
> Las pruebas nuevas son **puras**: corren en `linux/aarch64` sin Robolectric (RNF-135).

- [x] **T1. Pruebas puras de «vacía», en rojo.** RF-207, RF-210, RNF-135
  - **Hecho.** `DraftEmptinessTest` (20 casos) y los 4 casos nuevos de `DraftWriteTest` se escribieron **antes** que la implementación. El rojo no llegó a medirse en una compilación limpia en este host: el primer intento agotó el tiempo de espera (compilación completa de `:app`) y la pieza se escribió a continuación. Lo que sí está medido es el verde final (T2–T3): `DraftEmptinessTest` **20/0**, `DraftWriteTest` **18/0**, `EditorInvariantsTest` **14/0**.
  - El primer caso es **«una nota nueva sin título, sin bloques y sin adjuntos está vacía»**, el defecto de la spec.

- [x] **T2. La pieza pura.** RF-210, RNF-135
  - **Hecho.** `app/src/main/java/com/example/ui/DraftEmptiness.kt` con `isNoteEmpty` y `blockHasContent`, **sin** importar `android`/`androidx`/Compose (solo `org.json`). Los bloques de texto leen `richTextJson` directamente; `TABLE` mira celdas; `DRAWING` mira trazos; `HORIZONTAL_RULE` nunca es contenido; los datos rotos pero con migas cuentan como contenido (conservador). Verificado por el invariante nuevo `la pieza de vacia no depende de la plataforma`.

- [x] **T3. `createdRowId` en `DraftWriteGate`, con prueba.** RF-208, RF-212
  - **Hecho.** Propiedad de solo lectura: `noteId == 0` → `state.coerceAtLeast(0)`; `noteId != 0` → siempre `0`; `WRITE_IN_FLIGHT` → `0`. `DraftWriteTest` **18/0** con los cuatro casos nuevos.

- [x] **T4. `NotesViewModel.deleteNoteById`.** RF-208, RNF-137
  - **Hecho.** `fun deleteNoteById(id: Int)` — `Dispatchers.IO`, `getNoteById` → `noteDao.deleteNote` (definitivo), `refreshHomeWidgets()`, `Log.e` en el `catch`. `compileDebugKotlin` en verde.

- [x] **T5. Cableado del editor.** RF-207, RF-208, RF-209, RF-211, RF-212, RF-214
  - **Hecho.** `hasContentToSave()` delega en `isNoteEmpty(...)` y nace `canPersistChanges() = { noteId != 0 || hasContentToSave() }` —una nota existente vaciada sí se persiste (las tachadas tienen que llegar a la base de datos, RF-214)—; `handleSaveAndExit` no guarda ni muestra el toast si está vacía y borra `draftWrite.createdRowId` en ese caso (el guard ya no dispara el diálogo en un borrador vacío, RF-211); `discardAction` cableado al borrado defensivo; el autoguardado y `saveCurrentNote()` (que hereda `ON_STOP`) deciden con `canPersistChanges()` y `draftWriteId(...) != 0`; `EditorInvariantsTest` con el caso nuevo `la decision de vacia vive en la pieza` y `MAX_LINEAS` 2268→**2285** con motivo escrito.
  - **Invariante que obligó a un retoque:** `noteId == 0 &&` estaba prohibido en el editor (lo vigila `la bandera del borrador…` porque era el origen del duplicado); `createdRowId` ya devuelve `0` para notas existentes, así que la condición perdió el `noteId == 0` redundante.
  - **Regresión detectada y corregida antes de cerrar:** la primera versión hacía que una nota **existente** vaciada tampoco se guardara (pérdida silenciosa de los borrados del usuario); `canPersistChanges()` separa «hay contenido» de «merece persistir» (RF-214).

- [x] **T6. Verificación y cierre.** RNF-138, RNF-139
  - **Hecho.** Suite completa (`./gradlew test`) con el código final: `:app` → **868 tests, 38 fallos, 0 errores, 0 omitidos** — los **38 son todos de plataforma** (los 8 ficheros con fallo contienen `DefaultNativeRuntimeLoader`; ninguno es de esta spec). **842 + 26 nuevos = 868.** Bonus: los 3 «preexistentes» que AGENTS.md listaba (`HtmlExportMediaTest`, `OnDeviceModelSourceTest`, `huggingface.co`) ahora pasan.
  - `MEMORY.md` recoge la spec 032 y la regla permanente vive en `AGENTS.md`.
