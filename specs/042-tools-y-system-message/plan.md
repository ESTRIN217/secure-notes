# Plan — Spec 042: Hoja de adjuntos del chat: System Message y Tools

## Archivos y responsabilidades

| # | Archivo | Responsabilidad | RF |
|---|---|---|---|
| 1 | `app/src/main/java/com/example/ui/AttachmentAiSheet.kt` | **Reescritura** (incluye el WIP a mano: los dos `SettingsListTile` con títulos en duro, `Icons.Filled.Email`, onClick vacíos e indentación mezclada 3/4 se **reescriben**, no se conservan). KDoc actualizado: «exactamente **cuatro** opciones (RF-300, enmienda a RF-34)». Firma gana `onOpenSystemMessage` y `onOpenTools`. Los dos azulejos nuevos: `SettingsListTile` con `leadingIcon`/`title`/`subtitle`/`trailingIcon` desde recursos y `onClick { onDismiss(); callback() }` — el orden cerrar-antes-de-abrir de RF-35 se extiende aquí, escrito en el sitio. Iconos: System Message → `Icons.Filled.EditNote` (el `Email` del WIP sugiere correo y se descarta); Tools → `Icons.Filled.Build` con chevron. Si la función pasada de 20 líneas, se extraen azulejos privados (el invariante prohíbe el nombre `AttachmentListTile`, no otros). | RF-300, RF-301, RF-304 |
| 2 | `app/src/main/java/com/example/ui/chat/SystemMessageDialog.kt` (**nuevo**) | Diálogo «tonto» del prompt: `AlertDialog` con `OutlinedTextField` multilínea, texto inicial = prompt actual, botones Guardar/Cancelar (Cancelar reutiliza la clave `cancel` existente), y el aviso fijo de RF-303 como texto de ayuda. Opt-in `ExperimentalMaterial3Api` si toca. | RF-301, RF-302, RF-303 |
| 3 | `app/src/main/java/com/example/ui/chat/ToolsSheet.kt` (**nuevo**) | Segunda hoja modal «tonta»: `ModalBottomSheet` con una fila por herramienta — nombre como dato + `Checkbox` — y el mensaje de lista vacía si no hay registro. | RF-304, RF-305 |
| 4 | `app/src/main/java/com/example/ui/AiChatScreen.kt` | Monta las dos superficies nuevas junto a la hoja de adjuntos: estados `showSystemMessageDialog` / `showToolsSheet`, los dos callbacks nuevos de `AttachmentAiSheet`, los datos del ViewModel (`systemPrompt`, `toolNames`, `disabledTools`) y `viewModel.setSystemPrompt(...)` / `viewModel.toggleTool(...)` en los callbacks. Cero lógica dentro de los Composables. | RF-301, RF-302, RF-304, RF-305 |
| 5 | `app/src/main/java/com/example/data/ai/ToolRegistry.kt` | `getSpecsForApi(disabledNames: Set<String> = emptySet())` filtra por nombre manteniendo el orden de registro; `names(): List<String>` para la UI. Fichero ya puro (sin `android`): el filtrado es la decisión y aquí es testeable sin Robolectric. | RF-306, RF-307 |
| 6 | `app/src/main/java/com/example/ui/viewmodel/AiViewModel.kt` | `_disabledTools = MutableStateFlow(prefsRepository.getDisabledTools())` + `disabledTools: StateFlow<Set<String>>` + `toggleTool(name)` (con la pieza pura) + `toolNames: StateFlow<List<String>>`. En el punto que hoy hace `toolRegistry.getSpecsForApi()` (~línea 1021) pasa `_disabledTools.value`. **No se toca** el flujo de envío ni `resolvedPrompt` (~520/~975). | RF-305, RF-306 |
| 7 | `app/src/main/java/com/example/data/PreferencesRepository.kt` + `SharedPreferencesRepository.kt` | `getDisabledTools(): Set<String>` / `setDisabledTools(names: Set<String>)`, con el patrón literal de favoritos/ocultos: cadena `\n`-join split-trim-filter bajo la clave nueva. | RF-305, RNF-194 |
| 8 | `app/src/main/java/com/example/Constants.kt` | `const val AI_DISABLED_TOOLS_KEY = "ai_disabled_tools"`. | RNF-194 |
| 9 | `app/src/main/java/com/example/data/backup/BackupSettings.kt` | **Una línea** en `BACKUP_SETTINGS` (`AI_DISABLED_TOOLS_KEY to SettingType.TEXT`) + defecto `""` en `DEFAULT_SETTINGS` (convención 028: quien añade un ajuste a la copia añade una línea ahí y nadie más). Viaja como TEXT porque el conjunto ya viaja unido con `\n`. | RNF-194 |
| 10 | `strings.xml` ×9 locales | Claves nuevas (abajo). Cero apóstrofes crudos: `\'`. | RF-300, RF-301, RF-303, RF-304, RNF-192 |

**Strings nuevas** (9 locales de una vez: `values/`, `values-es-rVE/`, `values-pt-rBR/`,
`values-fr/`, `values-it/`, `values-en-rGB/`, `values-es-rES/`, `values-pt-rPT/`,
`values-b+es+419/`): `attach_system_message`, `attach_system_message_subtitle`, `attach_tools`,
`attach_tools_subtitle`, `system_message_title`, `system_message_hint` («se aplica al siguiente
mensaje; on-device puede recargar el modelo»), `system_message_save`, `tools_title`,
`tools_empty`. Cancelar reutiliza `cancel` (existe, línea 509). T1 verifica cada clave en las
nueve carpetas.

## Funciones puras (con "hoy" como parámetro)

Ambas viven en ficheros sin `android` (RNF-191) y nacen con su test en rojo (constitución, p. 4).

```kotlin
// ToolRegistry.kt (top-level, junto al registro)
/** Conjunto de herramientas deshabilitadas tras (des)activar `name`. `enabled` = hoy queda activa. */
fun toggledDisabledTools(current: Set<String>, name: String, enabled: Boolean): Set<String> =
    if (enabled) current - name else current + name

// ToolRegistry (método)
/** Especificaciones que se anuncian al modelo: las registradas menos las deshabilitadas, en orden. */
fun getSpecsForApi(disabledNames: Set<String> = emptySet()): List<Map<String, Any>>
```

- `toggledDisabledTools` es la **decisión** del toggle (RF-305): el ViewModel no reescribe la
  regla, la consulta — igual que `draftWriteId`/`isNoteEmpty` en las specs 027/032. Casos: activar
  quita, desactivar añade, es idempotente y con un nombre desconocido devuelve un conjunto válido.
- `getSpecsForApi(disabled)` es la **decisión** del anuncio (RF-306/RF-307): un nombre del
  conjunto que ya no está registrado no filtra nada y no falla; todas deshabilitadas → lista
  vacía; sin conjunto → comportamiento de hoy, intacto.

## Algoritmo en pseudocódigo

```text
# Envío (el único cambio es el parámetro; el flujo de siempre no se toca)
fun specsParaPeticion(registro, deshabilitadas, backend):
    si registro vacío o backend != OLLAMA: devolver []            # hoy, igual
    devolver registro.getSpecsForApi(deshabilitadas)              # antes: getSpecsForApi()

# Toggle (RF-305) — guardado inmediato, sin confirmación
fun toggleTool(name):
    deshabilitadas = toggledDisabledTools(_disabledTools.value, name, hoyEstáDeshabilitada)
    _disabledTools.value = deshabilitadas
    prefsRepository.setDisabledTools(deshabilitadas)              # \n-join, patrón favoritos/ocultos

# System Message (RF-301/302) — guardar no envía nada
fun confirmarDialogo(texto):
    viewModel.setSystemPrompt(texto)          # ya existe: StateFlow + prefsRepository (líneas 594-597)
    # La siguiente petición resuelve: _systemPrompt.value.ifBlank { promptPorDefectoDeLaAcción }
    # On-device, ensureReady detecta prompt != activo y recarga (relee pesos) al enviar, no aquí.

# Orden cerrar-antes-de-abrir (RF-35 extendida), escrito en el sitio de cada azulejo:
onClick { onDismiss(); onOpenSystemMessage() /* u onOpenTools() */ }
```

## Interfaz

```kotlin
// AttachmentAiSheet.kt — de 3 a 5 callbacks; dueña de su hoja (D-6 de la 006, intacto)
fun AttachmentAiSheet(
    onPickNote: () -> Unit,
    onPickFile: () -> Unit,
    onOpenSystemMessage: () -> Unit,
    onOpenTools: () -> Unit,
    onDismiss: () -> Unit
)

// SystemMessageDialog.kt — tonta: entra el prompt actual, salen guardar/cancelar
fun SystemMessageDialog(
    currentPrompt: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
)

// ToolsSheet.kt — tonta: entran datos, sale el toggle
fun ToolsSheet(
    toolNames: List<String>,
    disabledTools: Set<String>,
    onToggle: (String) -> Unit,     // name
    onDismiss: () -> Unit
)

// PreferencesRepository / SharedPreferencesRepository
fun getDisabledTools(): Set<String>
fun setDisabledTools(names: Set<String>)

// AiViewModel
val toolNames: StateFlow<List<String>>
val disabledTools: StateFlow<Set<String>>
fun toggleTool(name: String)

// ToolRegistry
fun getSpecsForApi(disabledNames: Set<String> = emptySet()): List<Map<String, Any>>
fun names(): List<String>
```

## Decisiones justificadas (con alternativa descartada)

- **D1 — Tools como segunda `ModalBottomSheet`**, no diálogo. Coincide con el lenguaje de hojas
  del chat (la propia hoja de adjuntos, RF-34), escala a filas `Checkbox` con más herramientas y es
  lo que muestra la app de referencia. *Descartada:* `AlertDialog` con lista — con varias
  herramientas queda corto y rompe el patrón de hojas del chat; y montarla como segunda página
  **dentro** de la hoja de adjuntos — obligaría a la hoja a conocer datos de herramientas y rompería
  la decisión D-6 de la 006 (la hoja es tonta: solo callbacks), además de duplicar el estado de
  descarte.
- **D2 — El filtrado vive en `ToolRegistry.getSpecsForApi(disabled)`**, en el fichero del registro,
  que ya es puro. *Descartada:* filtrar en `AiViewModel` sobre la lista ya construida — duplicaría
  la forma del mapa de API en otro sitio y dejaría al registro capaz de anunciar herramientas que
  nadie pidió; y descartada filtrar en la UI — lógica de envío dentro de un Composable viola la
  constitución (p. 3) y RNF-193.
- **D3 — El estado se guarda como cadena `\n`-joined** bajo `ai_disabled_tools`, con el patrón
  literal de favoritos/ocultos. *Descartado:* `putStringSet` nativo — `SettingType` solo conoce
  TEXT/FLAG/NUMBER y añadir un tipo nuevo a la tabla única de la copia por una ganancia nula; y
  descartado un JSON propio — más código para el mismo viaje.
- **D4 — El conjunto SÍ viaja en la copia**: una línea en `BACKUP_SETTINGS` + defecto `""`
  (RNF-194). *Descartada:* no incluirlo — restaurar una copia dejaría el chat anunciando
  herramientas que el usuario había apagado, y los modelos favoritos/ocultos ya viajan
  (precedente directo de la 033).
- **D5 — Aviso de recarga como texto fijo del editor** (RF-303), no recarga al confirmar.
  El coste real — releer pesos — ocurre al enviar, por el camino de `ensureReady`, y avisarlo basta.
  *Descartada:* aplicar/recargar el modelo al guardar — materializaría el motor fuera de
  `AiModelHost`/`ensureReady` (regla permanente de rendimiento: el motor solo se materializa por
  acquire/ensureReady) y cargaría pesos aunque el usuario no vuelva a enviar nada; *descartada*
  también no avisar — la primera respuesta tras cambiar el prompt on-device tardaría más sin
  explicación.
- **D6 — Nombres de herramienta como dato** (`ToolSpec.name`), no como recursos: igual que los
  nombres de modelo (spec 033); los recursos cubren el chrome (títulos, subtítulos, botones,
  vacío). *Descartada:* traducir los nombres — la UI de toggle debe decir exactamente qué se
  deshabilita, y el nombre es el identificador de la API.
- **D7 — El prompt no entra en la copia** en esta spec: hoy `ai_system_prompt` no viaja y sumarlo
  es otro requisito (fuera de alcance); solo entra el conjunto de herramientas (lo aprobado).

## Estrategia de tests (constitución p. 4: rojo → verde)

JUnit4 **puro, sin Robolectric** (precedentes: `PrefsFalso` de la 028, `ToolRegistry` ya es puro).
Suite completa con los 38 fallos de plataforma conocidos en `linux/aarch64`; las dirigidas de esta
spec se ejecutan y quedan verdes aquí.

| Test | Ficha | Qué fija | RF |
|---|---|---|---|
| `ToolRegistryTest` (nuevo, `app/src/test/java/com/example/data/ai/`) | 1 | `getSpecsForApi()` sin conjunto manda todas (hoy intacto); con una deshabilitada la excluye y conserva el orden; todas deshabilitadas → vacía; nombre inexistente en el conjunto no falla ni altera el resto; `names()` refleja el registro | RF-306, RF-307 |
| `DisabledToolsTest` (nuevo, mismo paquete) | 1 | `toggledDisabledTools`: desactivar añade; activar quita; idempotente; nombre desconocido da conjunto válido | RF-305 |
| `BackupSettingsInvariantsTest` (ampliar, `app/src/test/java/com/example/`) | 1 | La clave nueva está en `BACKUP_SETTINGS` y en `DEFAULT_SETTINGS` con su defecto `""`: viaja una línea y el defecto no cambia de paso | RNF-194 |

Ejecución: escribir el test en rojo → implementar → `./gradlew :app:compileDebugUnitTestKotlin`
y `./gradlew :app:testDebugUnitTest --tests "com.example.data.ai.ToolRegistryTest"
--tests "com.example.data.ai.DisabledToolsTest" --tests "com.example.BackupSettingsInvariantsTest"`
en verde. Al cierre, `./gradlew test` (868+ tests; los 38 de plataforma, ajenos) y la verificación
en verde completa queda para host `x86_64`, como las specs 002/032.

## Verificación de invariantes (cierre, T10)

```bash
grep -c "SettingsListTile(" app/src/main/java/com/example/ui/AttachmentAiSheet.kt   # 4  (enmienda RF-34)
grep -c "ModalBottomSheet" app/src/main/java/com/example/ui/AttachmentAiSheet.kt   # 1
grep -c "fun AttachmentListTile" app/src/main/java/com/example/ui/AttachmentAiSheet.kt  # 0
grep -c '"text/\*", "application/json", "application/xml"' \
      app/src/main/java/com/example/ui/AiChatScreen.kt                             # 1 (RF-35 intacto)
grep -n 'Icons.Filled.Email\|"System Message"\|"Tools"' \
      app/src/main/java/com/example/ui/AttachmentAiSheet.kt                        # 0 (WIP reescrito, RNF-192)
grep -rn "getSpecsForApi()" app/src/main/java/com/example/ui/viewmodel/AiViewModel.kt  # 0: la llamada pasa el conjunto
```
