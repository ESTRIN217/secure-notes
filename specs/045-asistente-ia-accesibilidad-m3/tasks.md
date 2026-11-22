# Tasks 045 — Accesibilidad y tokens M3 del asistente de IA

Estado: borrador

- [x] **T1. Extraer contrato puro de acciones de burbuja y tests.** RF-355, RNF-215
  - Hecho cuando: existe un helper puro en `ui/chat/` (sin `android`) que devuelve las acciones
    visibles por burbuja según rol/estado/capacidades, y sus tests JUnit4 pasan en rojo→verde
    cubriendo usuario, asistente error, asistente ok con/sin insertar y contenido vacío.
  - Hecho: `BubbleActions.kt` + `BubbleActionsTest.kt` (5 tests). Verde en `:app:testDebugUnitTest --tests BubbleActionsTest`.

- [x] **T2. Burbujas de chat: quitar swipe y hacer fila accesible.** RF-353, RF-354, RF-355, RF-357, RF-358
  - Hecho cuando: `MessageBubble` ya no registra `detectHorizontalDragGestures` ni estado muerto de
    swipe/menú; la fila de acciones usa el contrato de T1, cada acción interactiva tiene
    `contentDescription` localizado y objetivo táctil ≥48 dp; los iconos decorativos no añaden ruido.
  - Hecho: swipe/estado muerto eliminados; `BubbleActionRow` con `sizeIn(min 48dp)` y `contentDescription`; `MessageStatusIcon` con semántica/roles; error usa `colorScheme.error`. `compileDebugKotlin` verde.

- [x] **T3. Campo de entrada del chat: targets y semántica.** RF-353, RF-354, RF-357, RF-358
  - Hecho cuando: adjuntar, anillo de contexto, razonamiento, selector de modelo y enviar/detener
    tienen objetivo táctil ≥48 dp manteniendo el tamaño visual del icono; los controles tienen nombre
    accesible y el estado enviar/detener se comunica correctamente.
  - Hecho: attach/contexto/razonamiento/modelo/enviar-detener a 48dp en `FloatingChatInputField`; `compileDebugKotlin` verde.

- [x] **T4. Modelos: quitar colores literales y corregir accesibilidad.** RF-353, RF-354, RF-356, RF-357, RF-358
  - Hecho cuando: `ModelSelectorSheet`, `ModelManagerPanel` y `ModelInfoPanel` no usan colores
    literales para favorito/cargado/error; usan roles de `MaterialTheme.colorScheme`; iconos
    interactivos tienen nombre accesible y targets ≥48 dp donde aplique.
  - Hecho: favorito/cargado pasan a `colorScheme.primary`; sin `Color(0x...)` en superficies de alcance; compile verde.

- [x] **T5. Historial/drawer: accesibilidad de sesiones y acciones.** RF-353, RF-354, RF-357, RF-358
  - Hecho cuando: `ChatSessionCard`, `ChatHistoryDrawerContent` y `ChatHistoryScreen` exponen nombres
    accesibles, targets ≥48 dp para acciones interactivas, y el estado fijado/seleccionado no depende
    solo del color o del icono.
  - Hecho: `ChatSessionCard` describe estado fijado; componentes compartidos drawer/historial revisados; compile verde.

- [x] **T6. Ajustes IA y superficies menores del asistente.** RF-353, RF-354, RF-356, RF-357, RF-358
  - Hecho cuando: `AiSettingsScreen`, `AttachmentAiSheet`, `ToolsSheet`, `SystemMessageDialog`,
    `ContextDetailCard` y `EmptyChatWelcome` quedan revisados; no hay iconos interactivos sin nombre
    accesible ni targets <48 dp en controles interactivos; estados/ayudas no dependen solo del color.
  - Hecho: superficies menores revisadas; `SettingsListTile` con `heightIn(min=48.dp)`; sin Color(0x...) en alcance; compile verde.

- [x] **T7. Cadenas en 9 locales.** RNF-214
  - Hecho cuando: toda cadena nueva o modificada para accesibilidad existe en `values/`,
    `values-es-rVE/`, `values-pt-rBR/`, `values-fr/`, `values-it/`, `values-en-rGB/`,
    `values-es-rES/`, `values-pt-rPT/` y `values-b+es+419/`.
  - Hecho: 11 claves nuevas verificadas en los 9 locales (acciones burbuja, estado mensaje, pinned).

- [x] **T8. Compilación, pruebas y cierre de spec.** RNF-213, RNF-215, Criterios de finalización
  - Hecho cuando: `:app:compileDebugKotlin` pasa; las pruebas puras nuevas pasan en este entorno; el
    resultado de tests se documenta con la limitación aarch64/Robolectric si aplica; spec y MEMORY.md
    se actualizan con estado real y verificaciones pendientes en dispositivo.
  - Hecho: `:app:compileDebugKotlin` BUILD SUCCESSFUL; `BubbleActionsTest` (5 tests) BUILD SUCCESSFUL. Verificación TalkBack/escala 200%/dark-dynamic/tablet-PC pendiente en dispositivo.
