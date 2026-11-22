# Tasks — Spec 043: Herramientas de fecha/hora e información y control total de herramientas

- [x] **T1. Strings ×9 locales + enmienda documentada.** RF-308, RF-311, RNF-199
  - Hecho cuando: las 9 carpetas de locales tienen `tools_all`, `tools_enabled_count` (`%1$d/%2$d`)
    y `get_info_note`, con `grep -c` = 1 por clave y carpeta, sin apóstrofes crudos (escapados `\'`),
    y `./gradlew :app:mergeDebugResources` termina OK; el plan queda como fuente de las decisiones
    D8–D11 y la enmienda a la 042 está reflejada en `spec.md`.
- [x] **T2. Piezas puras del checkbox maestro, test en rojo primero.** RF-308, RF-309, RNF-198
  - Hecho cuando: `MasterToggleTest` existe y falla en rojo sin las piezas; tras implementar
    `allToolsToggle`, `enabledToolCount` y `allToolsEnabled` (top-level en `ToolRegistry.kt`, sin
    `android`), `./gradlew :app:testDebugUnitTest --tests "com.example.data.ai.MasterToggleTest"`
    queda verde: desactivar todas añade las registradas, activar todas vacía el conjunto y limpia
    nombres muertos, idempotente, contadores 5/5 · 4/5 · 0/5 · 0/0, marcado solo si todas activas.
- [x] **T3. Formateadores puros de las dos respuestas, test en rojo primero.** RF-310, RF-311, RNF-197, RNF-198
  - Hecho cuando: `ToolFormatTest` existe y falla en rojo; tras implementar `getDatetimeResult`,
    `getInfoResult` y `jsonString` (en `DateTimeToolFormat.kt`, sin `android`, sin librerías nuevas),
    la dirigida queda verde: ISO-8601 con `+02:00`, `-04:00` y `+00:00` (sin `Z`), media noche/cambio
    de año, horario de verano, `timezone` IANA inyectado, `Android X (API N)`, sin `cwd` ni
    identificadores y con comillas escapadas en la nota.
- [x] **T4. Objetos `ToolSpec` nuevos.** RF-310, RF-311, RF-312
  - Hecho cuando: existen `GetDatetimeTool.kt` y `GetInfoTool.kt` con `spec` sin parámetros, nombres
    `get_datetime`/`get_info`, descripciones de protocolo en inglés (la de `get_info` sin mención a
    cwd) y cero claves de recurso para los nombres (D6 de la 042 intacta); `:app:compileDebugKotlin`
    pasa.
- [x] **T5. Cinco herramientas registradas y `setAllTools` en el ViewModel.** RF-309…RF-312, RNF-201, RNF-202
  - Hecho cuando: `AiViewModel` registra las cinco herramientas (dos `register(...)` nuevos con
    handlers delgados que delegan en T3 y recogen `Instant.now()`/`ZoneId.systemDefault()`/`Build`/
    `getString`), expone `setAllTools(enabled)` que consulta T2, actualiza `_disabledTools` y persiste
    con `setDisabledTools`; el punto de anuncio sigue siendo `getSpecsForApi(_disabledTools.value)` y
    solo con Ollama; `resolvedPrompt` y el flujo de envío sin tocar; `:app:compileDebugUnitTestKotlin`
    pasa y `grep -c "register("` en el ViewModel devuelve **5**.
- [x] **T6. Cabecera maestro en el submenú.** RF-308, RF-309, RNF-200
  - Hecho cuando: `ToolsSheet` muestra encima de la lista un `Checkbox` con `tools_all` y el contador
    `tools_enabled_count` alimentado por `enabledToolCount`/`allToolsEnabled`; `checked` = todas
    activas, `enabled` = `toolNames.isNotEmpty()`, y el click llama a `onSetAll` sin persistir ni
    enviar nada dentro del Composable; sin registro el checkbox está deshabilitado y sigue
    `tools_empty`; funciones ≤ 20 líneas y 0 literales en `Text(`.
- [x] **T7. Cableado en la pantalla de chat.** RF-308, RF-309
  - Hecho cuando: `AiChatScreen` pasa a `ToolsSheet` el callback `viewModel::setAllTools` y los datos
    que ya recoge (`toolNames`, `disabledTools`) sin lógica nueva en el Composable; el toggle
    individual de la 042 sigue funcionando y el contador se recalcula al marcar o desmarcar cualquiera.
- [x] **T8. Invariante «3 → 5» en el registro, test en rojo primero.** RF-312, RF-306
  - Hecho cuando: `ToolRegistryTest` ampliado falla en rojo si el registro sigue teniendo tres; tras T5
    queda verde con `names()` devolviendo las cinco en orden y `getSpecsForApi()` sin conjunto
    mandando cinco; todas deshabilitadas dan lista vacía y un nombre muerto en el conjunto no filtra
    de más (RF-307 intacto).
- [x] **T9. Verificación final e invariantes.** RNF-201, RNF-203
  - Hecho cuando: `./gradlew :app:compileDebugUnitTestKotlin` OK; los cuatro tests dirigidos
    (`MasterToggleTest`, `ToolFormatTest`, `ToolRegistryTest`, `DisabledToolsTest`) verdes con
    `--tests`; los greps del plan pasan (5 registros; 9/9 claves por locale; sin apóstrofes crudos;
    0 literales en `Text(` de `ToolsSheet`; 1 `getSpecsForApi(_disabledTools.value)`, 0 cambios en el
    flujo); `./gradlew test` solo con los 38 fallos de plataforma conocidos en `linux/aarch64`; y
    `MEMORY.md` actualizado con estado, decisiones D8–D11 y pendiente de verificación en dispositivo
    (toggle maestro y contador, fecha/hora en zona real, `get_info` sin cwd, persistencia y copia).
- [x] **T10 (enmienda). Un toque, un toggle: quitar el doble handler de `ToolsRow`, test en rojo primero.** RF-313, RNF-204
  - Hecho cuando: `ToolsSheetInvariantsTest` existe en `app/src/test/java/com/example/ui/chat/` y falla
    en ROJO con el `Row.clickable` actual (2 `onToggle(name)`, 1 `.clickable` en el cuerpo de
    `ToolsRow`); tras quitar el `.clickable` del `Row` y dejar `Checkbox.onCheckedChange` como único
    handler, la dirigida queda en VERDE con exactamente 1 `onToggle(name)` y 0 `.clickable`;
    `ToolsMasterRow`, `ToolRegistry`, `AiViewModel`, prefs y el flujo de envío sin tocar, con el maestro
    y el contador igual; `./gradlew :app:compileDebugUnitTestKotlin` OK y los tests dirigidos de la 043
    (`MasterToggleTest`, `ToolFormatTest`, `ToolRegistryTest`, `DisabledToolsTest`) y de la copia
    (`BackupSettingsTest`, `BackupSettingsInvariantsTest`) en verde; queda pendiente la comprobación en
    dispositivo del toque individual.

**Verificación en dispositivo (2026-11-22)**: completada por el usuario. Pendientes históricos de verificación cerrados.
