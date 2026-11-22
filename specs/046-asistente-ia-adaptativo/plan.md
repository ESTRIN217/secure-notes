# Plan 046 — Asistente de IA adaptativo en tablet y PC

Estado: aprobado (enmienda 2026-11-22 pendiente de implementar)

## Contexto de la enmienda

La implementación inicial (rail de historial persistente + modelos como side sheet + drawer
dismissible) se retira en Media/Expandido/Wide y se sustituye por: **rail lateral permanente
colapsable** (todos los destinos + conversaciones) + **área principal única** que cambia entre Chat y
Modelos (ancho completo). Compacto se mantiene igual. Dejan de aplicar RF-362/363/364.

## Archivos y responsabilidades

| Archivo | Responsabilidad | RF |
|---|---|---|
| `app/src/main/java/com/example/ui/chat/ChatWindowLayout.kt` | Se mantiene. Añadir helpers puros si hace falta para destino/ancho de tabla. | RNF-223 |
| `app/src/test/java/com/example/ui/chat/ChatWindowLayoutTest.kt` | Tests de los nuevos helpers puros. | RNF-223 |
| `app/src/main/java/com/example/ui/AiChatScreen.kt` | Sustituir el `PermanentNavigationDrawer`/`DismissibleNavigationDrawer` por un rail colapsable con destinos; area principal conmuta entre `ConversationPane` y `ModelManagerPanel` a ancho completo; conservar sugerencias; conservar estado al cambiar destino. | RF-370..379 |
| `app/src/main/java/com/example/ui/ModelManagerPanel.kt` | Arreglar `TableHeaderRow` y filas con anchos ponderados que no partan texto; modo "destino a ancho completo" (sin side sheet); quitar el wrapper side sheet del llamador en Media+. | RF-372, RF-375, RNF-225 |
| `app/src/main/java/com/example/ui/ModelInfoPanel.kt` | El detalle de modelo ya no va como side sheet en Media+; se integra como contenido del destino Modelos o pantalla de detalle dentro del área principal. | RF-372 (derivado) |
| `app/src/main/java/com/example/ui/MainActivity.kt` (`NavigationRailContent`) | Reutilizar el patrón de rail/orden de destinos; si es necesario, extraer un `AssistantRail` que use la misma semántica y rgba de destinos. | RF-370, RF-378 |
| `app/src/main/res/values*/strings.xml` (9 locales) | Cadenas del rail (Nuevo chat, Buscar, Modelos, Ajustes, etc.) si no existen ya; la mayoría ya están traducidas. | RNF-224 |

## Funciones puras

Se reutiliza `ChatWindowLayout` (COMPACT/MEDIUM/EXPANDED/WIDE) ya implementada. No se necesitan
nuevas decisiones puras complejas; el destino activo es estado de UI. Si se extrae ancho de tabla:

```kotlin
fun modelTableWeights(availableWidthDp: Int): ... // opcional, solo si se prueba sin UI
```

Preferentemente la tabla usa `weight(1f)`/`weight(...)` y `widthIn`, sin cálculo puro nuevo.

## Algoritmo / pseudocódigo

```
AiChatScreen:
  chatLayout = chatLayoutMode(widthDp)
  var assistantDestination by rememberSaveable { mutableStateOf(ASSISTANT_CHAT) }
  var railExpanded by rememberSaveable { mutableStateOf(true) }

  when (chatLayout) {
    COMPACT -> // drawer modal + chat + overlays (sin cambios, RF-377)
    else -> Row {
      AssistantRail(
        destination = assistantDestination,
        expanded = railExpanded,
        onDestinationChange = { assistantDestination = it },
        onToggleExpanded = { railExpanded = !railExpanded },
        sessions = sessions, ...
      )
      Box(Modifier.weight(1f)) {
        when (assistantDestination) {
          ASSISTANT_CHAT -> ConversationPane(showHistoryNav = false)
          ASSISTANT_MODELS -> ModelManagerPanel(modo = FULL_WIDTH)
        }
      }
    }
  }

ModelManagerPanel modo FULL_WIDTH:
  Surface(fillMaxSize)
  header + buscador
  LazyColumn con tabla:
    TableHeaderRow con columnas weight(1f)/weight(0.25f)/weight(0.25f)/weight(0.3f)
    filas con weight equivalente y widthIn para no partir
```

## Decisiones justificadas

1. **Rail con todos los destinos (RF-370/RF-378)**. Alternativa descartada: rail solo de historial
   (lo que había). Se descarta porque las referencias muestran navegación + conversaciones juntas y
   el usuario pidió «ajustes y el resto».
2. **Modelos como destino de ancho completo (RF-372)**. Alternativa descartada: side sheet acotado
   (RF-363 anterior). Se descarta porque partía las cabeceras y el usuario pidió el patrón de
   llama.cpp 074005.
3. **Rail colapsable (RF-371)**. Alternativa descartada: rail siempre extendido. Se descarta por
   decisión del usuario (referencia 074019).
4. **Conservar sugerencias (RF-376)**. Alternativa descartada: saludo estático. Se descarta por
   decisión del usuario y porque ya están cubiertas por spec 045.
5. **Compacto sin cambios (RF-377)**. Alternativa descartada: meter el rail también en móvil. Se
   descarta porque el drawer modal ya funciona y el espacio no lo permite.

## Estrategia de tests

- Tests puros existentes de `ChatWindowLayout` siguen vigentes; añadir solo si se extrae lógica.
- `./gradlew :app:compileDebugKotlin` tras cada tarea.
- Verificación manual pendiente: tablet/PC con rail extendido/colapsado, cambio Chat↔Modelos,
  tabla sin columnas partidas, breakpoints, rotación, IME, escala de texto.

## Riesgos

- `ConversationPane` es un composable local dentro de `AiChatScreen`; al envolverlo en el rail hay que
  no duplicar overlays (diálogos/sheets) que hoy están hermanos del Scaffold.
- `NavigationRailContent` espera un `NavController`-like de la app principal; reutilizar el patrón
  sin acoplar AiChatScreen al `Navigator` de notas.
- La tabla de modelos tiene muchas acciones por fila; con `weight` hay que garantizar min widths.
