# Plan 047 — Semántica y expresividad del asistente de IA

Estado: aprobado

## Archivos y responsabilidades

| Archivo | Responsabilidad | RF |
|---|---|---|
| `app/src/main/java/com/example/ui/AiChatScreen.kt` | Top bar del chat con jerarquía clara; `ConversationPane`; transición de cambio de destino Chat↔Modelos (RF-383) sin duplicar overlays. | RF-381, RF-383, RF-387 |
| `app/src/main/java/com/example/ui/chat/AssistantRail.kt` | Filas del rail con semántica M3 (selección por `selected`/rol, no solo color), expresividad en selección y transición; icon-only con nombre/target. | RF-382, RF-383, RF-386 |
| `app/src/main/java/com/example/ui/ModelManagerPanel.kt` | Filas de modelo con semántica de estado; estados descargando/cargado/error con expresividad y texto; acciones destructivas diferenciadas. | RF-382, RF-383, RF-384 |
| `app/src/main/java/com/example/ui/chat/ToolsSheet.kt` | Filas de herramientas con `Checkbox` semántico; estado maestro; expresividad contenida en selección. | RF-382, RF-386 |
| `app/src/main/java/com/example/ui/FloatingChatInputField.kt` | Expresividad marcada en entrada y generación (ya hay toolbar wavy/send animado); conservar targets y nombres de la 045. | RF-383, RF-386, RF-387 |
| `app/src/main/java/com/example/ui/EmptyChatWelcome.kt` | Bienvenida expresiva (icono/sugerencias) sin perder los chips de la 045. | RF-383 |
| `app/src/main/java/com/example/ui/chat/ContextDetailCard.kt` | Estados con texto+semántica, sin color único; expresividad en el indicador. | RF-382, RF-384 |
| `app/src/main/res/values*/strings.xml` (9 locales) | Cualquier cadena nueva de semántica/estado. | RNF-227 |

Fuera de plan: `ChatHistoryScreen.kt` (RF-388 queda igual).

## Decisiones justificadas

1. **Todas las acciones visibles (RF-380)**. Alternativa descartada: overflow «más». Se descarta por
   decisión del usuario; la fila de la 045 ya es accesible y no se duplica.
2. **Expresividad marcada (RF-383)**. Alternativa descartada: contención sobria. Se descarta por
   decisión del usuario; se extiende forma/movimiento a estados y transiciones de lista, siempre con
   significado y respetando ReduceMotion (RNF-229).
3. **`ChatHistoryScreen` intacto (RF-388)**. Alternativa descartada: simplificar/retirar. Se descarta
   por decisión del usuario; solo se limpia semántica en componentes compartidos.
4. **Sin dependencias nuevas (RNF-226)**. La expresividad se logra con Compose/M3 ya presente
   (`animateColorAsState`, `AnimatedContent`, shapes, roles de tema).

## Estrategia de tests

- La 047 es principalmente semántica/visual; no hay lógica de negocio nueva.
- Tests existentes de `bubbleActions`/`ChatWindowLayout` siguen vigentes y se reejecutan.
- `:app:compileDebugKotlin` tras cada tarea; verificación visual/accesibilidad en dispositivo
  pendiente (ReduceMotion, texto grande, foco/lectura).

## Riesgos

- Añadir movimiento a transiciones de lista puede introducir jank: usar `animate*AsState` sobre
  estado derivado y evitar trabajo en el lambda de dibujo (reglas de rendimiento de AGENTS.md).
- Mantener todas las acciones visibles en burbuja estrecha puede recortar: verificar con texto grande.
- No tocar `ChatHistoryScreen` limita el alcance a componentes compartidos.
