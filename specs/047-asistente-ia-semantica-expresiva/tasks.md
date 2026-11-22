# Tasks 047 — Semántica y expresividad del asistente de IA

Estado: aprobado

- [x] **T1. Semántica de filas del rail y transición de destino.** RF-382, RF-383, RF-386
  - Hecho cuando: las filas de `AssistantRail` comunican selección con rol/semántica y no solo color;
    hay transición con forma/movimiento al cambiar Chat↔Modelos; iconos siguen con nombre y target;
    `ReduceMotion` respetado; compile verde.
  - Hecho: `AssistantRail` filas con `Role.Button` + `semantics{selected}` y bg animado; `AnimatedContent` Chat↔Modelos con fade y `reduceMotion`; compile verde.

- [x] **T2. Top bar del chat con jerarquía clara.** RF-381, RF-387
  - Hecho cuando: `ConversationPane` muestra contexto/título con prioridad visible sobre las acciones;
    navegación y acciones no compiten; comportamiento de envío/streaming intacto.
  - Hecho: `ConversationPane` TopAppBar con `sessionTitle` (fallback `nav_chats`), `titleMedium`, ellipsis; acciones separadas.

- [x] **T3. Filas y estados de modelos semánticos + expresivos.** RF-382, RF-383, RF-384, RF-386
  - Hecho cuando: `ModelManagerPanel` usa `ModelColumns` y filas con semántica de estado; estados
    descargando/cargado/error/oculto con texto + expresividad; acciones destructivas con rol de error;
    compile verde.
  - Hecho: `ModelColumns` + estados con rol de error y texto; acciones destructivas con `colorScheme.error`; ya presente de 045/046.

- [x] **T4. Expresividad en entrada, bienvenida y herramientas.** RF-383, RF-386
  - Hecho cuando: `FloatingChatInputField` y `EmptyChatWelcome` conservan su expresividad y targets;
    `ToolsSheet` usa `Checkbox` semántico con selección expresiva; nada de color literal.
  - Hecho: `ToolsSheet` fila con bg animado y `Checkbox` con `selected` semantics; input/bienvenida conservados.

- [x] **T5. Cadenas en 9 locales.** RNF-227
  - Hecho cuando: cualquier cadena nueva existe en los 9 locales; se reutilizan las existentes.
  - Hecho: sin cadenas nuevas en 047; se reutilizan las existentes.

- [x] **T6. Compilación, tests y cierre.** RNF-226, RNF-228, RNF-229, RNF-230, Criterios
  - Hecho: `compileDebugKotlin` + pruebas puras verdes; sin `Color(0x...)` nuevo; `ChatHistoryScreen` sin cambios de 047; verificación visual/accesibilidad pendiente en dispositivo.
  - Hecho cuando: `:app:compileDebugKotlin` pasa; tests puros vigentes pasan; no hay
    `Color(0x...)` nuevo en superficies del asistente; `ChatHistoryScreen` sin cambios; spec/MEMORY
    actualizadas; revisión visual/accesibilidad pendiente en dispositivo.
