# MEMORY.md — secure-notes 
Memoria del proyecto entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no 
aporte. 
## Estado actual 
- **Configuración de IA (on-device) fusionada**: `AiSettingsScreen.kt` tiene una sola sección
  `Models` (`onDeviceModelsSection`) con `SelectedModelPanel` (detalle + origen + acciones) arriba
  y la lista de modelos abajo. Sustituye a las 3 secciones previas + `RecommendedModelCard`.
  Compila (`./gradlew :app:assembleDebug` OK el 2026-09-30). **Sin verificación visual**: no se
  pudo abrir en un dispositivo; revisar que la fila de origen no se corte y que los badges no
  desbordan con nombres largos.
- `OnDeviceModel` expone `sourceLabel` / `downloadUrl` / `sourcePageUrl` / `quantLabel`;
  `ModelDownloader` usa `model.downloadUrl`.
- Añadido `OnDeviceModelSourceTest.kt`, **nunca ejecutado** (ver abajo).

## Decisiones (y por qué) 
- El "modelo recomendado" pasó a ser un **badge en la fila** del mejor modelo en vez de una card
  separada: la card duplicaba nombre/tamaño/RAM que ya mostraba la lista. Una sola fuente de verdad.
- La lista de modelos ya **no es colapsable** (`showAllModels` + `AnimatedVisibility` eliminados):
  al fusionar, la lista pasó a ser el contenido principal de la sección y colapsarla solotapaba
  datos.
- El origen (repo de Hugging Face) se muestra **por modelo** (fila con link al navegador + copiar)
  y **global** (nota en Device Info, junto a la nota del motor): el usuario pidió ambos niveles.
- El modelo se sigue seleccionando por radio button; la fila completa ahora es clicable.
- `isModelDownloaded()` se sigue leyendo del disco **dentro de la composición** (herencia del
  código previo). Funciona porque `downloadState` fuerza la recomposición. Pendiente: exponer un
  `StateFlow` de "modelos descargados" si molesta.

## Aprendizajes y errores a evitar 
- **`compileDebugUnitTestKotlin` está ROTO desde antes** (no lo causó esta tarea): `PdfExporterTest.kt`
  y `WidgetNotesTest.kt` usan firmas viejas de `PdfExporter`. Kotlin compila todo el source set de
  tests a la vez → **ningún** test unitario corre hasta arreglarlos. Confirmado con `git stash`
  sobre árbol limpio. Arreglarlos es lo siguiente de mayor valor.
- AAPT2 **rechaza apóstrofos crudos** en `strings.xml` (`Invalid unicode escape sequence`) →
  escribir `\'`. Falla en `mergeDebugResources`, después de que Kotlin ya compiló bien: un
  `compileDebugKotlin` verde **no** implica recursos válidos.
- `LoadingIndicator` (material3 1.5.0-alpha28) necesita `ExperimentalMaterial3ExpressiveApi`
  (no basta `ExperimentalMaterial3Api`) y **no tiene `strokeWidth`**.
- Al reemplazar un bloque grande con un script Python, `DeviceInfoRow` quedó dentro del rango
  borrado y salió como `Unresolved reference`. Revisar los límites del rango antes de escribir.
- `settings.gradle.kts` apunta a `/root/llama.cpp/...` (no a una ruta bajo `/data/user/0/...`)
  y hace `include(":lib")` **solo si existe**: si el checkout falta, el módulo se omite en
  silencio y el build sigue sin IA nativa.

## Próximos pasos 
- Revisar visualmente la sección `Models` en un dispositivo (única verificación pendiente).
- Arreglar `PdfExporterTest.kt` / `WidgetNotesTest.kt` para desbloquear `./gradlew test` y ejecutar
  `OnDeviceModelSourceTest.kt`.
- Opcional: estado descargado reactivo en vez de lectura de disco en composición.