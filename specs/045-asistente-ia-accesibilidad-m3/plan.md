# Plan 045 — Accesibilidad y tokens M3 del asistente de IA

Estado: aprobado

## Archivos y responsabilidades

| Archivo | Responsabilidad | RF |
|---|---|---|
| `app/src/main/java/com/example/ui/AiChatScreen.kt` | Quitar swipe horizontal y estado muerto de burbuja; hacer fila de acciones accesible (targets ≥48dp, `contentDescription`, semántica de estado); reemplazar colores literales de estado por roles. | RF-353..358 |
| `app/src/main/java/com/example/ui/FloatingChatInputField.kt` | Subir targets de adjuntar, anillo de contexto, razonamiento, selector de modelo y enviar/detener a ≥48dp sin cambiar el aspecto visual del icono; revisar nombres accesibles/estado. | RF-353,354,357,358 |
| `app/src/main/java/com/example/ui/ModelSelectorSheet.kt` | Quitar color literal de favorito; asegurar nombres accesibles y targets en filas/botones. | RF-353,354,356,357 |
| `app/src/main/java/com/example/ui/ModelManagerPanel.kt` | Quitar color literal de favorito; revisar iconos interactivos/decorativos, targets y descripciones. | RF-353,354,356,357 |
| `app/src/main/java/com/example/ui/ModelInfoPanel.kt` | Quitar color literal de estado cargado; revisar acciones icon-only, targets y descripciones. | RF-353,354,356,357,358 |
| `app/src/main/java/com/example/ui/AttachmentAiSheet.kt` | Verificar filas con target/nombre accesible (usa `SettingsListTile`); ajustar solo si hace falta. | RF-353,354 |
| `app/src/main/java/com/example/ui/chat/ToolsSheet.kt` | Verificar checkbox/fila accesible; sin cambios funcionales salvo accesibilidad. | RF-353,354 |
| `app/src/main/java/com/example/ui/chat/SystemMessageDialog.kt` | Verificar nombres/ayuda accesible del diálogo. | RF-353 |
| `app/src/main/java/com/example/ui/chat/ContextDetailCard.kt` | Verificar que el detalle expone texto/semántica comprensible; no depende solo de color. | RF-356,358 |
| `app/src/main/java/com/example/ui/EmptyChatWelcome.kt` | Verificar icono decorativo, chips accesibles y texto escalable. | RF-353,357 |
| `app/src/main/java/com/example/ui/ChatHistoryScreen.kt` | Revisar `ChatSessionCard`/acciones para targets, nombres accesibles y estado seleccionado/fijado no solo por color. | RF-353,354,357,358 |
| `app/src/main/java/com/example/ui/AiChatScreen.kt` (`ChatHistoryDrawerContent`) | Mismo tratamiento que historial: búsqueda, cerrar, nueva conversación, tarjetas. | RF-353,354,357 |
| `app/src/main/java/com/example/ui/settings/AiSettingsScreen.kt` | Revisar controles icon-only, estados de conexión/error y targets en botones/iconos. | RF-353,354,356,358 |
| `app/src/main/res/values*/strings.xml` (9 locales) | Cadenas nuevas/modificadas para `contentDescription`, etiquetas de acciones de burbuja y estados. | RNF-214 |
| `app/src/test/java/com/example/ui/chat/...` (nuevo, puro) | Pruebas puras para mapeos de acciones/etiquetas de burbuja si se extrae lógica; sin Robolectric. | RNF-215 |

Fuera de plan: `ChatSearchScreen.kt` (código muerto, fuera de alcance por spec).

## Funciones puras

No hay lógica de negocio nueva compleja; la mayor parte es UI Compose. Se extrae solo lo que tenga
decisión comprobable sin `Context`:

1. `BubbleAction` (o equivalente en `ui/chat/`, sin `android`): enum/listado de acciones visibles
   por burbuja según rol/estado/capacidades (`isUser`, `isError`, `isLastAssistant`, `canInsert`,
   `hasContent`). Devuelve acciones en orden estable. **RF-355, RF-353**.
2. `bubbleActionLabelRes(action): Int` o mapa puro acción→clave de recurso id, si se mantiene la
   decisión fuera del composable; alternativamente el composable resuelve el string desde un id
   estable. La prueba verifica que cada acción expuesta tenga id/clave asignada. **RF-353**.
3. Reutilizar helpers existentes si ya cubren el caso; no crear purezas artificiales.

Firma prevista (ajustable en implementación):

```kotlin
enum class BubbleAction { COPY, SHARE, RESEND, RETRY, EDIT, INSERT }

fun bubbleActions(
    isUser: Boolean,
    isError: Boolean,
    isLastAssistant: Boolean,
    canInsert: Boolean,
    hasContent: Boolean
): List<BubbleAction>
```

El test verifica el contrato de RF-355: usuario obtiene editar/reusar/copia/compartir según aplique;
asistente obtiene copiar/compartir/reintentar o regenerar/insertar según estado. No usa
Robolectric.

## Algoritmo / pseudocódigo

### A. Burbujas de mensaje (RF-353,354,355,357,358)

```
MessageBubble(...):
  quitar showMenu muerto, swipeOffset, animatedSwipeOffset, SWIPE_THRESHOLD
  quitar combinedClickable longClick que solo ponía showMenu=true
  quitar pointerInput(detectHorizontalDragGestures)
  quitar offset animado por swipe

  acciones = bubbleActions(...)
  renderizar fila de acciones con:
    - IconButton/IconToggle/Button con minimumInteractiveComponentSize / Modifier.sizeIn(min=48.dp)
    - contentDescription localizado para cada acción
    - icono visible puede ser 20/24dp; el target no
    - iconos decorativos dentro de botón no añaden descripción extra

  estados:
    MessageStatusIcon: si es solo decorativo junto a texto, contentDescription null;
    si comunica estado por sí mismo, description localizada por estado.
```

### B. Targets táctiles del campo de entrada (RF-354)

```
FloatingChatInputField:
  adjuntar IconButton: quitar size(40.dp) o envolver con sizeIn(minWidth=48.dp,minHeight=48.dp)
  anillo contexto Box clickable: size(36.dp) -> mínimo 48dp, manteniendo anillo visual centrado
  ReasoningEffortButton Surface height(32.dp): altura/touch target >=48dp; padding visual ajustado
  ModelPickerButton Surface height(32.dp): igual
  SendOrStopButton IconButton size(32.dp): >=48dp, icono 20-24dp centrado

  no cambiar comportamiento; no mover lógica al composable.
```

### C. Colores literales (RF-356)

```
AiChatScreen MessageStatusIcon ERROR: Color(0xFFE53935) -> MaterialTheme.colorScheme.error
ModelManagerPanel favorito: Color(0xFFE91E63) -> MaterialTheme.colorScheme.primary (o tertiary/role definido) 
ModelSelectorSheet favorito: Color(0xFFE91E63) -> mismo rol que ModelManagerPanel
ModelInfoPanel punto "loaded": Color(0xFF4CAF50) -> MaterialTheme.colorScheme.primary o role de éxito acordado

Decisión: usar roles existentes del tema; no inventar paleta paralela. Favorito usa primary
(ya usado en otros lugares como acento) o un role consistente; documentar en decisión.
```

### D. contentDescription (RF-353,357)

```
Para cada Icon/IconButton/Surface clicable en superficies de alcance:
  si ejecuta acción -> contentDescription localizado
  si comunica estado sin texto adyacente -> contentDescription localizado
  si es decorativo junto a texto que ya dice lo mismo -> contentDescription = null

Prioridad: burbujas, toolbar de entrada, selector/panel/info de modelo, drawer/historial,
ajustes IA. Revisar grep de contentDescription = null en esos archivos.
```

## Interfaz

No hay cambios de navegación ni de contratos de ViewModel. Cambios de firma internos solo si se
extrae `bubbleActions` (nuevo archivo puro en `ui/chat/`).

Posibles strings nuevos (nombres tentativos, se confirman en implementación; deben existir en 9
locales):

- `chat_action_copy`, `chat_action_share`, `chat_action_retry`, `chat_action_regenerate`,
  `chat_action_edit`, `chat_action_insert` — si no existen ya equivalentes adecuados.
- Descripciones de estado para `MessageStatus` si el icono queda como único portador.
- Revisar claves existentes: `ai_resend`, `ai_insert`, `chat_rename`, `chat_delete_confirm`,
  `close`, `back`, `attach`, `remove`, `more_options`, etc., antes de añadir duplicados.

## Decisiones justificadas

1. **Quitar swipe en vez de mantenerlo como atajo**. Alternativa descartada: conservar swipe y
   añadir acciones accesibles. Se descarta por decisión explícita del usuario y porque mantener dos
   caminos duplicados en la misma burbuja complica el mantenimiento y la accesibilidad; RNF-217 lo
   prohíbe.
2. **Mantener fila de acciones visible**. Alternativa descartada: menú contextual único. Se descarta
   por decisión del usuario; la fila ya existe y evita esconder acciones críticas. Se debe limpiar su
   densidad/targets, no convertirla en otro patrón.
3. **Roles del tema para favorito/cargado/error**. Alternativa descartada: añadir colores semánticos
   custom en el tema. Se descarta por alcance P0 y por no introducir tokens nuevos sin spec de
   diseño; usar `primary`/`error`/`onSurfaceVariant` mantiene coherencia con dynamic color.
4. **No tocar `ChatSearchScreen`**. Alternativa descartada: arreglarlo “ya que estamos”. Se descarta
   por código muerto confirmado por el usuario y por regla de scope de la spec.
5. **Prueba pura solo para lógica extraída**. Alternativa descartada: snapshot/Robolectric para
   targets. Se descarta por entorno linux/aarch64 sin runtime Robolectric nativo; la regla de 48dp
   se verifica por revisión + convención en código y, donde sea posible, por helper puro.

## Estrategia de tests

- **Antes de código**: crear test puro para `bubbleActions` (RF-355/RNF-215). Rojo primero.
- **Casos mínimos**:
  - usuario, no error, con contenido → incluye EDIT/RESEND/COPY/SHARE según contrato definido.
  - asistente, error → incluye RETRY/COPY/SHARE.
  - asistente, ok, último, `canInsert=true` → incluye INSERT.
  - asistente, ok, no último, `canInsert=false` → no incluye INSERT.
  - contenido vacío → no expone acciones que no aplican.
- **Compilación**: `./gradlew :app:compileDebugKotlin`.
- **Pruebas**: `./gradlew :app:testDebugUnitTest --tests ...` para las nuevas pruebas puras en este
  entorno; suite completa queda condicionada por Robolectric aarch64 (AGENTS.md). Documentar el
  resultado real.
- **Verificación manual pendiente**: TalkBack, foco teclado, escala de texto 200%, dark/dynamic
  theme, móvil/tablet/PC.

## Riesgos

- El campo de entrada (`HorizontalFloatingToolbar`) puede quedar apretado en 320dp si se suben
  targets a 48dp; puede requerir ajustes de padding/spacing sin cambiar jerarquía.
- La fila de acciones de burbuja ya es densa; hacerla accesible puede requerir más altura o
  reflujo, pero sin añadir gestos.
- `ChatSessionCard` se usa en drawer e historial; cambiarla cubre ambas superficies pero hay que no
  romper layout existente.
