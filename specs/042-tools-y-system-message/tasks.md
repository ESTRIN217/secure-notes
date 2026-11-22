# Tasks — Spec 042: Hoja de adjuntos del chat: System Message y Tools

- [x] **T1. Strings ×9 locales + enmienda documentada.** RF-300, RF-301, RF-303, RF-304, RNF-192
  - Hecho cuando: las 9 carpetas de locales tienen las claves nuevas (`attach_system_message`,
    `attach_system_message_subtitle`, `attach_tools`, `attach_tools_subtitle`,
    `system_message_title`, `system_message_hint`, `system_message_save`, `tools_title`,
    `tools_empty`; Cancelar reutiliza `cancel`) con `grep -c` = 1 por clave y carpeta, ningún
    apóstrofe crudo (escapado `\'`), y `./gradlew :app:mergeDebugResources` termina OK.
- [x] **T2. Estado del conjunto en preferencias.** RF-305, RNF-194
  - Hecho cuando: `Constants.kt` declara `AI_DISABLED_TOOLS_KEY = "ai_disabled_tools"`, la interfaz
    de preferencias expone `getDisabledTools(): Set<String>` / `setDisabledTools(Set<String>)` con
    el patrón `\n`-join de favoritos/ocultos, y `:app:compileDebugKotlin` pasa.
- [x] **T3. Pieza pura del toggle, test en rojo primero.** RF-305, RNF-191
  - Hecho cuando: `DisabledToolsTest` existe y falla en rojo sin la pieza; tras implementar
    `toggledDisabledTools(current, name, enabled)` (top-level en el fichero del registro, sin
    `android`), `./gradlew :app:testDebugUnitTest --tests "com.example.data.ai.DisabledToolsTest"`
    queda verde (desactivar añade, activar quita, idempotente, nombre desconocido válido).
- [x] **T4. Filtrado del anuncio en el registro, test en rojo primero.** RF-306, RF-307
  - Hecho cuando: `ToolRegistryTest` existe y falla en rojo; tras implementar
    `getSpecsForApi(disabledNames)` y `names()`, la dirigida queda verde: sin conjunto manda todas,
    una deshabilitada se excluye con orden intacto, todas → vacía, nombre inexistente no falla.
- [x] **T5. La copia de seguridad lleva el ajuste.** RNF-194
  - Hecho cuando: `BackupSettings.kt` suma UNA línea en `BACKUP_SETTINGS` (TEXT) y defecto `""` en
    `DEFAULT_SETTINGS`; `BackupSettingsInvariantsTest` ampliado queda verde fijando la línea nueva
    y su defecto.
- [x] **T6. ViewModel: estado, toggle y paso del conjunto al envío.** RF-305, RF-306, RNF-195
  - Hecho cuando: `AiViewModel` expone `toolNames`, `disabledTools` (StateFlow, semilla de prefs) y
    `toggleTool(name)` que consulta la pieza pura de T3 y persiste; la llamada de
    `getSpecsForApi()` (~1021) pasa `_disabledTools.value`; `resolvedPrompt` (~520/~975) y el resto
    del flujo de envío sin tocar; `:app:compileDebugUnitTestKotlin` pasa.
- [x] **T7. Hoja reescrita a cuatro opciones (WIP incluido).** RF-300, RF-35 (extendida)
  - Hecho cuando: `AttachmentAiSheet` muestra cuatro `SettingsListTile` en orden Nota, Archivo,
    System Message, Tools; los dos azulejos del WIP reescritos (recursos, `EditNote`/`Build`,
    subtitle, indentación uniforme, callbacks reales); en las cuatro el `onDismiss()` va antes del
    callback; KDoc dice «exactamente cuatro (RF-300)»; greps: `SettingsListTile(` = 4,
    `ModalBottomSheet` = 1, `fun AttachmentListTile` = 0, y 0 coincidencias de `Icons.Filled.Email`
    ni de literales `"System Message"`/`"Tools"`.
- [x] **T8. Editor del System Message.** RF-301, RF-302, RF-303
  - Hecho cuando: `SystemMessageDialog` (tonta) se abre desde la hoja —que se cierra antes— con el
    prompt actual, guarda con `viewModel.setSystemPrompt(...)` sin enviar petición, vacío borra el
    prompt personalizado, cancelar no cambia nada, y el aviso del siguiente mensaje/recarga es
    visible (recurso, no literal).
- [x] **T9. Submenú de Tools (segunda hoja modal).** RF-304, RF-305
  - Hecho cuando: `ToolsSheet` se abre desde la hoja —que se cierra antes— con las herramientas
    registradas de `viewModel.toolNames` y un `Checkbox` por estado; marcar/desmarcar persiste en
    el acto vía `viewModel.toggleTool`; sin registro se muestra `tools_empty`; `ModalBottomSheet`
    en su fichero = 1.
- [x] **T10. Verificación final e invariantes.** RNF-195, RNF-196
  - Hecho cuando: `./gradlew :app:compileDebugUnitTestKotlin` OK; los tres tests dirigidos
    (`ToolRegistryTest`, `DisabledToolsTest`, `BackupSettingsInvariantsTest`) verdes con
    `--tests`; los greps de invariantes del plan (4/1/0/1, `getSpecsForApi()` sin conjunto = 0 en
    el ViewModel, sin literales en `Text(`) pasan; el grep de tipos de archivo en `AiChatScreen`
    sigue devolviendo 1; y `MEMORY.md` actualizado con estado, decisiones (D1–D7) y pendiente de
    verificación en dispositivo.

**Verificación en dispositivo (2026-11-22)**: completada por el usuario. Pendientes históricos de verificación cerrados.
