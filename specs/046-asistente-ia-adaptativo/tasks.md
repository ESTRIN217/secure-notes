# Tasks 046 — Asistente de IA adaptativo en tablet y PC

Estado: aprobado (enmienda 2026-11-22 pendiente de implementar)

## Implementación inicial (ya hecha)

- [x] T1..T8 de la versión inicial: layout puro, modo calculado, drawer persistente/dismissible, side sheet de modelos, conservación de estado. **Retirada parcialmente** por la enmienda (RF-362/363/364).

## Enmienda 2026-11-22

- [x] **T9. Quitar el side sheet de modelos y el drawer de historial en Media+. RF-374, RF-370**
  - Hecho cuando: en Media/Expandido/Wide ya no se usa `PermanentNavigationDrawer` ni
    `DismissibleNavigationDrawer` ni el wrapper side sheet de `ModelManagerPanel`; Compacto mantiene
    su drawer modal; compile verde.
  - Hecho: reorganizado en `Row { AssistantRail + Box }` con `ConversationPane`/destino; side sheet retirado; compile verde.

- [x] **T10. Rail lateral colapsable con todos los destinos.** RF-370, RF-371, RF-378
  - Hecho cuando: en Media+ el asistente muestra un rail con Nuevo chat, Búsqueda, Modelos, Ajustes y
    el resto de destinos, más conversaciones fijadas/recientes; hay un control para colapsarlo a
    iconos y expandirlo; el destino activo se conserva; compacto sin rail.
  - Hecho: `ui/chat/AssistantRail.kt` con destinos + conversaciones + toggle; anchos puros 260/72dp en `ChatWindowLayout`; callbacks cableados en `Screen.kt`.

- [x] **T11. Área principal conmuta entre Chat y Modelos.** RF-372, RF-373, RF-374, RF-379
  - Hecho cuando: seleccionar Modelos en el rail muestra `ModelManagerPanel` a ancho completo en el
    área principal (no overlay) y seleccionar Chat vuelve a la conversación; el chat conserva saludo,
    sugerencias (RF-376), campo flotante y estado (texto/adjuntos); nunca hay tres paneles.
  - Hecho: `assistantDestination` (CHAT/MODELS) conmuta el contenido; botón modelo del input también conmuta en Media+; `ModelManagerPanelAsDestination` local; estado conservado.

- [x] **T12. Tabla de modelos legible a ancho completo.** RF-372, RF-375, RNF-225
  - Hecho cuando: `TableHeaderRow` y las filas usan anchos ponderados/`widthIn` que no parten
    «Modelo/Contexto/Estado/Acciones»; la tabla se ve legible en Media, Expandido y Wide, y con
    escala de texto grande; no queda código del side sheet estrecho.
  - Hecho: `ModelColumns` con weights + `widthIn(min)` compartido por cabecera y filas (local/Ollama); compile verde.

- [x] **T13. Detalle de modelo integrado.** RF-372 (derivado), RF-374
  - Hecho cuando: `ModelInfoPanel` ya no se abre como side sheet sobre el chat en Media+; se muestra
    como contenido del destino Modelos (pantalla de detalle dentro del área principal) con vuelta
    clara; Compacto conserva su hoja.
  - Hecho: `ModelInfoContent` local; en Media+ se pinta dentro del destino MODELS; en Compacto sigue como overlay.

- [x] **T14. Cadenas en 9 locales.** RNF-224
  - Hecho cuando: cualquier cadena nueva del rail/tabla existe en los 9 locales; se reutilizan las
    existentes siempre que sea posible.
  - Hecho: sin cadenas nuevas en la enmienda; el rail reutiliza `chat_new`, `chat_search_hint`, `nav_chats`, `models_panel_title`, `nav_settings`, `chat_history_title`, `app_name`.

- [x] **T15. Compilación, pruebas y cierre.** RNF-223, RNF-224, RNF-225, Criterios de finalización
  - Hecho cuando: `:app:compileDebugKotlin` pasa; pruebas puras vigentes pasan; spec/MEMORY
    actualizadas con el estado real; verificación en dispositivo de rail/breakpoints/tabla pendiente.
