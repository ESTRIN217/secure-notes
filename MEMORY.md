# MEMORY.md — secure-notes

Memoria entre sesiones. Mantenerla breve (máximo ~50 líneas); las reglas permanentes viven en `AGENTS.md`.

## Estado actual
- **2026-11-18**: Spec **042** implementada (pendiente en dispositivo) — hoja de adjuntos del chat: System Message y Tools (`specs/042-tools-y-system-message/`, RF-300…RF-307 · RNF-190…RNF-196). La hoja pasa de 2 a **4 opciones** (Nota, Archivo, System Message, Tools; enmienda a RF-34, greps 4/1/0/1/1) y en las cuatro `onDismiss()` va **antes** del callback (RF-35 extendida). Piezas nuevas: `ui/chat/SystemMessageDialog.kt` (guardar **no envía petición**, RF-302; aviso del siguiente mensaje/recarga como texto fijo, RF-303) y `ui/chat/ToolsSheet.kt` (segunda `ModalBottomSheet`, Checkbox por herramienta, `tools_empty`). **Decisiones**: D1 Tools como segunda hoja modal (no diálogo ni página dentro de la hoja de adjuntos — D-6 de la 006); D2 el filtrado vive en `ToolRegistry.getSpecsForApi(disabledNames)` (puro, fichero sin `android`); D3 el conjunto se guarda `\n`-join bajo `ai_disabled_tools` (patrón favoritos/ocultos, D3 de la 033); D4 el conjunto **SÍ viaja** en la copia (una línea en `BACKUP_SETTINGS` + defecto `""`); D5 aviso de recarga, no recarga al guardar (el motor solo se materializa por `AiModelHost`/`ensureReady` al **enviar**); D6 nombres de herramienta como **dato** (`names()`), no recursos; D7 el prompt **NO** entra en la copia (fuera de alcance). ViewModel: `toolNames`/`disabledTools` (StateFlow, semilla de prefs) + `toggleTool` (consulta `toggledDisabledTools`, persiste en el acto); `getSpecsForApi(_disabledTools.value)` en el único punto de envío (~1021); `resolvedPrompt` y el resto del flujo intactos. Tests dirigidos: `DisabledToolsTest` 5, `ToolRegistryTest` 5, `BackupSettingsInvariantsTest` 7 — **0 fallos**; `:app:compileDebugUnitTestKotlin` OK. **WIP del usuario intacto** (AiChatScreen: solo añadidos autorizados; ModelManagerPanel sin tocar). **Pendiente**: verificación en dispositivo (toggle persiste entre sesiones, restaurar copia, cambiar prompt on-device recarga al enviar).
- **2026-11-16**: Spec **041** implementada (pendiente en dispositivo) — desglose de caché del contexto en la tarjeta (RF-293…RF-299 · RNF-179…RNF-185). `last_prompt_entry_position` en `lib/` (fuente de verdad; espejo `/root/llama.cpp`); Room v10→v11 (`MIGRATION_10_11`, `promptCachedTokens`). 18 tests verdes; build nativa OK.
- **2026-11-16**: Spec **040** implementada (pendiente en dispositivo) — detalles de modelo on-device: ⓘ en hoja (<600 dp) y panel (≥600 dp); Context Size del GGUF, chat template on-demand con caché. 39 fallos = 38 plataforma + `ShortcutInvariantsTest` preexistente (MainActivity.kt:209-212 viola RNF-74; ajeno — decidir invariante).
- **2026-11-16**: Bug 036 corregido — `processingTimeMs` quedaba `NULL`; la consulta escribe también `processingTimeMs = :promptMs`. Sin cambio de esquema.
- **2026-11-16**: Specs **039** (métricas reales desde el motor; binding vendorizado en `lib/`, motor externo) y **038** (chats vacíos se descartan, análogo a 032) implementadas, pendientes en dispositivo.
- **2026-11-15**: Specs **037** (detalles del anillo de contexto; verificada en dispositivo), **036** (metadatos por burbuja; verificada), **035** (barra de carga del motor) y **034** (anillo de ocupación; exacto Ollama/estimado on-device) implementadas.
- **Spec 033 (2026-11-14), implementada; falta verificación en dispositivo**: razonamiento y modelo en el chat (RF-215…RF-247); favoritos/ocultos en prefs y en la copia.
- **Spec 032 (2026-11-11), cerrada y verificada en dispositivo**: la nota nueva vacía se descarta. `./gradlew test` → 868 tests, 38 fallos (todos de plataforma `linux/aarch64`).
- **Spec 031**: sitio de marca estático en `public/` publicado en https://notes-502213.web.app. **v4.0.0 publicada** (tag + Release). Acerca de: tarjeta de llama.cpp (CC-BY-NC-4.0).
- **Spec 028**: los ajustes de tema viajan en la copia; `BACKUP_SETTINGS` centraliza los valores (042 añade la línea 17: `ai_disabled_tools`).

## Decisiones que conviene recordar
- **2026-11-16**: El motor de llama.cpp **nunca se vendoriza en el repo** (decisión firme del usuario). Solo el binding Android (`lib/`) vive en el repo; el motor sigue externo en `/root/llama.cpp` y la build nativa fuera de ese host falla a propósito.
- Las reglas permanentes (nota nueva, tema, `AiModelHost`, CodeEditor, markdown, almacenamiento, rendimiento, seguridad) viven en `AGENTS.md`; no duplicarlas aquí.
- Antes de purgar `libs.versions.toml`, revisar el módulo externo `:lib`: consume datastore-preferences y junit-platform-launcher.
- Robolectric 4.17 en Java 25 necesita `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` en tareas `Test` que inicializan estado Android.

## Aprendizajes
- Configuration cache funciona; usar `--no-configuration-cache` solo ante síntomas de caché/KSP obsoleta.
- Un `^` sin `RegexOption.MULTILINE` puede hacer que un invariante pase por vacío; Kotlin no admite referencias adelantadas a funciones locales; compilar Kotlin no valida recursos XML.
- El recorrido `sinComentariosNiTextos` de `BackupSettingsInvariantsTest` **quita el contenido de los literales** (ni siquiera conserva las comillas): los asserts textuales sobre un valor con `"` no casan nunca — fijar el valor con `assertEquals` sobre el mapa, y reservar `contains` para patrones sin comillas.
- Buscar código muerto con rutas acotadas; `grep -r` global recorre `build/` y puede agotar tiempo.

## Próximos pasos
- Verificar spec 040 en dispositivo (ⓘ en hoja y panel; chat template real; borrar con y sin éxito; 4 estados del botón de ciclo de vida).
- Verificar spec 041 en dispositivo (MIGRATION_10_11 real sobre BD v10 sin wipe; fresh/cached vs Web UI turno a turno; cancelación y cambio de modelo sin sub-líneas).
- Verificar spec 042 en dispositivo (toggle persiste entre sesiones y viaja en la copia; cambiar el prompt on-device recarga el modelo al enviar, no al guardar; todas deshabilitadas → petición sin herramientas).
