# Plan — Spec 043: Herramientas de fecha/hora e información y control total de herramientas

## Archivos y responsabilidades

| # | Archivo | Responsabilidad | RF |
|---|---------|-----------------|----|
| 1 | `app/src/main/java/com/example/data/ai/tools/GetDatetimeTool.kt` (**nuevo**) | Objeto que solo expone `spec` de `get_datetime` (`name`, descripción de protocolo en inglés, `parameters` vacío), igual que los tres objetos de la 042. | RF-310, RF-312 |
| 2 | `app/src/main/java/com/example/data/ai/tools/GetInfoTool.kt` (**nuevo**) | Objeto que solo expone `spec` de `get_info`, con descripción adaptada (sin mención a cwd). | RF-311, RF-312 |
| 3 | `app/src/main/java/com/example/data/ai/ToolRegistry.kt` | Crece con las piezas puras del maestro: `allToolsToggle`, `enabledToolCount`, `allToolsEnabled`. Sigue sin `android`; `getSpecsForApi`, `names` y `toggledDisabledTools` de la 042 no se tocan. | RF-308, RF-309, RF-312 |
| 4 | `app/src/main/java/com/example/data/ai/DateTimeToolFormat.kt` (**nuevo, puro**) | Formateadores puros de las dos respuestas: `getDatetimeResult`, `getInfoResult` y el helper `jsonString`. Sin `android`, sin recursos, sin red. | RF-310, RF-311, RNF-198 |
| 5 | `app/src/main/java/com/example/ui/viewmodel/AiViewModel.kt` | Dos `register(...)` nuevos con handlers delgados que recogen instante/zona/`Build`/`getString` y delegan en los formateadores; `setAllTools(enabled)` consulta la pieza pura, actualiza `_disabledTools` y persiste. Sin tocar el flujo de envío. | RF-309…RF-312 |
| 6 | `app/src/main/java/com/example/ui/chat/ToolsSheet.kt` | Cabecera nueva encima de la lista: `Checkbox` maestro + `tools_all` + contador `tools_enabled_count`; deshabilitado si `toolNames.isEmpty()`. Sin lógica de envío ni de persistencia. | RF-308, RF-309, RNF-200 |
| 7 | `app/src/main/java/com/example/ui/AiChatScreen.kt` | Pasa `allToolsEnabled`/`enabledToolCount` y el callback `viewModel::setAllTools` al submenú; sin lógica nueva dentro del Composable. | RF-308, RF-309 |
| 8 | `app/src/main/res/values*/strings.xml` ×9 | Tres claves nuevas: `tools_all`, `tools_enabled_count`, `get_info_note`. Apóstrofes escapados (`\'`). | RF-308, RF-311, RNF-199 |
| 9 | `app/src/test/java/com/example/data/ai/MasterToggleTest.kt` (**nuevo**) | Piezas puras del maestro, JUnit4 sin Robolectric. | RF-308, RF-309 |
| 10 | `app/src/test/java/com/example/data/ai/ToolFormatTest.kt` (**nuevo**) | Formateadores puros de `get_datetime`/`get_info`, JUnit4 sin Robolectric. | RF-310, RF-311 |

Las claves nuevas van en las 9 carpetas (`values/`, `values-es-rVE/`, `values-pt-rBR/`, `values-fr/`,
`values-it/`, `values-en-rGB/`, `values-es-rES/`, `values-pt-rPT/`, `values-b+es+419/`):

| Clave | Valor en `values/` | Uso |
|---|---|---|
| `tools_all` | `All tools` | Etiqueta del checkbox maestro (RF-308) |
| `tools_enabled_count` | `%1$d/%2$d` | Contador `n/m` (RF-308); se rellena con `stringResource(..., activas, total)` |
| `get_info_note` | `This app only exposes note tools. There is no shell or local file system access.` | Nota fija del objeto `get_info` (RF-311, RNF-199) |

El valor de `get_info_note` se decisión traducido en cada locale: la note se muestra al usuario en la
respuesta del modelo (RNF-199). Sin apóstrofes crudos en los XML (AAPT2 los rechaza).

## Wire format (datos concretos)

- **`get_datetime`** (`ToolSpec` sin parámetros). La herramienta devuelve como contenido la cadena
  JSON — mismo orden de campos que llama.cpp:

  ```json
  {"result":"2026-08-17T14:05:09+02:00","timezone":"Europe/Madrid"}
  ```

  `result` = `AAAA-MM-DDTHH:mm:ss±HH:MM`, hora local del dispositivo, con signo incluso a cero
  (`+00:00`). `timezone` = nombre de zona IANA del dispositivo. No se consulta ningún servidor.

- **`get_info`** (`ToolSpec` sin parámetros), forma del *fallback* de navegador adaptada, sin `cwd`:

  ```json
  {"note":"This app only exposes note tools. There is no shell or local file system access.","os":"Android 14 (API 34)"}
  ```

  `os` = `"Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"`. El valor real
  de `note` sale del recurso localizado del locale activo. Sin marca, modelo, IMEI, identificadores ni
  directorio de trabajo.

- Las descripciones de los `ToolSpec` son datos de protocolo en inglés (no recursos), igual que las de
  la 042: `get_datetime` → *"Get the current date and time in the user's local timezone. Returns an
  ISO-8601 timestamp with offset and the IANA timezone name."*; `get_info` → *"Returns runtime info:
  the Android OS version and a note about the available tools."* (sin mención a cwd, para no inducir
  al modelo a esperarlo).

## Funciones puras (con "hoy" como parámetro)

Todas nacen con su test en rojo (constitución, p. 4) y viven en ficheros sin `android` (RNF-198).
El "hoy" de `get_datetime` se inyecta como `instant` y `zoneId`; los valores de sistema de `get_info`
se inyectan como `os`, `api` y `note`.

```kotlin
// ToolRegistry.kt (top-level)
/** Conjunto de deshabilitadas tras el maestro. Activar todas vacía el conjunto (limpia nombres
 *  muertos, RF-309); desactivar todas añade los nombres registrados al conjunto actual. */
fun allToolsToggle(current: Set<String>, registered: List<String>, enableAll: Boolean): Set<String> =
    if (enableAll) emptySet() else current + registered

/** Número de herramientas registradas que no están deshabilitadas: numerador del contador. */
fun enabledToolCount(registered: List<String>, disabled: Set<String>): Int =
    registered.count { it !in disabled }

/** El maestro está marcado solo si todas las registradas están activas.
 *  Con lista vacía devuelve true, pero la UI deja el checkbox deshabilitado (RF-309). */
fun allToolsEnabled(registered: List<String>, disabled: Set<String>): Boolean =
    registered.all { it !in disabled }

// DateTimeToolFormat.kt (top-level, puro)
/** Contenido de get_datetime: ISO-8601 local con desplazamiento + zona IANA. */
fun getDatetimeResult(instant: Instant, zoneId: ZoneId): String =
    """{"result":"${localIso(instant, zoneId)}","timezone":"${jsonString(zoneId.id)}"}"""

/** `xxx` da ±HH:MM y +00:00 a cero (no Z), como en llama.cpp. El instante lleva el desplazamiento
 *  vigente en esa fecha y zona, con horario de verano incluido. */
private fun localIso(instant: Instant, zoneId: ZoneId): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx").format(instant.atZone(zoneId))

/** Contenido de get_info: solo `note` y `os`, sin cwd ni identificadores. */
fun getInfoResult(os: String, api: Int, note: String): String =
    """{"note":${jsonString(note)},"os":${jsonString("Android $os (API $api)")}}"""

/** Escapa comillas, contrabarra y controles: sin librerías nuevas (RNF-197). */
private fun jsonString(value: String): String = buildString { /* '\\' -> '\\\\', '"' -> '\\"', etc. */ }
```

- `allToolsToggle` es la **decisión** del maestro (RF-309): «activar todas» vacía el conjunto — así un
  nombre muerto desaparece sin efecto (RF-307) — y «desactivar todas» hace la unión con las registradas.
- `enabledToolCount` y `allToolsEnabled` son la **decisión** de la cabecera (RF-308): el primero da el
  numerador del contador y el segundo el estado del checkbox. Con cero registradas, `allToolsEnabled`
  es `true` pero la UI deshabilita el control, conforme al caso límite.
- `getDatetimeResult` y `getInfoResult` son la **decisión** del formato de respuesta (RF-310/RF-311):
  reciben todos los valores externos como parámetros, así que son deterministas y se prueban con
  instantes y zonas fijos sin depender del reloj ni de Robolectric.

`java.time` (`Instant`, `ZoneId`, `DateTimeFormatter`) está en la plataforma desde la API 26 y `minSdk`
es 33: no hace falta desugaring ni dependencia nueva. La serialización se hace con `jsonString`, un
helper puro de unas líneas, porque la app no declara `kotlinx.serialization` y `org.json` no funciona
en tests JVM sin Robolectric (RNF-197, RNF-198).

## Algoritmo en pseudocódigo

```text
# Cabecera del submenú (RF-308) — la UI solo pinta y lanza eventos
fun cabecera(toolNames, disabledTools, setAllTools):
    total = toolNames.size
    activas = enabledToolCount(toolNames, disabledTools)
    marcado = allToolsEnabled(toolNames, disabledTools)
    Checkbox(checked = marcado, enabled = total > 0,
             onCheckedChange = { setAllTools(it) })      # RF-309
    Text(stringResource(tools_all))
    Text(stringResource(tools_enabled_count, activas, total))

# Maestro en el ViewModel (RF-309) — persistencia inmediata, como el toggle individual
fun setAllTools(enabled):
    nuevo = allToolsToggle(_disabledTools.value, toolRegistry.names(), enabled)
    _disabledTools.value = nuevo
    prefsRepository.setDisabledTools(nuevo)              # \n-join, formato de la 042 intacto

# Handlers nuevos (RF-310/RF-311) — recogen valores del sistema y delegan en lo puro
fun handlerGetDatetime(args):
    devolver getDatetimeResult(Instant.now(), ZoneId.systemDefault())

fun handlerGetInfo(args):
    devolver getInfoResult(Build.VERSION.RELEASE, Build.VERSION.SDK_INT,
                           application.getString(R.string.get_info_note))

# Registro (RF-312) — cinco, sumando dos a los tres existentes
registro.register(SearchNotesTool.spec, ...)   # intacto
registro.register(GetNoteTool.spec, ...)       # intacto
registro.register(CreateNoteTool.spec, ...)    # intacto
registro.register(GetDatetimeTool.spec, ::handlerGetDatetime)
registro.register(GetInfoTool.spec, ::handlerGetInfo)

# Anuncio — sin cambios respecto a la 042
specs = si backend == OLLAMA: registro.getSpecsForApi(_disabledTools.value) si no: []
```

El anuncio sigue pasando por el único punto de envío de la 042 y solo con Ollama; las dos herramientas
nuevas entran en el conjunto filtrable por ser `ToolSpec` registrados, sin tocar `AiRequest`.

## Interfaz

```kotlin
// ToolRegistry.kt (top-level, puro)
fun allToolsToggle(current: Set<String>, registered: List<String>, enableAll: Boolean): Set<String>
fun enabledToolCount(registered: List<String>, disabled: Set<String>): Int
fun allToolsEnabled(registered: List<String>, disabled: Set<String>): Boolean

// DateTimeToolFormat.kt (top-level, puro)
fun getDatetimeResult(instant: Instant, zoneId: ZoneId): String
fun getInfoResult(os: String, api: Int, note: String): String

// AiViewModel
fun setAllTools(enabled: Boolean)

// ToolsSheet.kt (tonta)
fun ToolsSheet(
    toolNames: List<String>,
    disabledTools: Set<String>,
    onToggle: (String) -> Unit,       // individual, de la 042 — intacto
    onSetAll: (Boolean) -> Unit,      // maestro, nuevo
    onDismiss: () -> Unit
)
```

## Decisiones justificadas (con alternativa descartada)

- **D8 — Checkbox maestro GLOBAL sobre las cinco herramientas**, con contador `n/m`, sin grupos.
  El usuario decidió un único interruptor de «todo» y el contador da el detalle; además encaja con el
  conjunto de deshabilitadas ya global de la 042 (RNF-194). El checkbox aparece **marcado solo si
  todas están activas** y funciona como «activar/desactivar todas». *Descartados:* (a) grupos Browser/Notes
  con un maestro por grupo — el usuario lo descartó explícitamente y obligaría a extender el formato
  guardado en preferencias y en la copia; (b) `TriStateCheckbox` con estado indeterminado — el
  contador `n/m` ya comunica el estado parcial y evita introducir otra API de Material con sus
  opt-ins; (c) ocultar el contador — sin él un checkbox sin marcar no distingue «una apagada» de
  «todas apagadas».
- **D9 — Forma JSON de `get_datetime` y `get_info` copiada de llama.cpp**, con la adaptación acordada
  para `get_info`. `get_datetime` replica `{result, timezone}` de `executeGetDatetimeTool`
  (`tools/ui/src/lib/utils/get-datetime.ts`): hora local del usuario, ISO-8601 con `±HH:MM`, zona IANA,
  sin reloj del servidor. `get_info` toma la forma del *fallback* de navegador
  (`browser-info.ts`): `{note, os}`, **sin cwd**; `os` se adapta a `Android <release> (API <n>)` y la
  nota es texto fijo. *Descartado:* copiar el `get_info` del servidor (`server-tools.cpp`), que
  devuelve `{os: uname -a, cwd: ...}` — `uname` no existe en Android, `cwd` no es información útil
  aquí y exponerlo contradiría RNF-202; también descartado inventar una forma propia — copiar la forma
  de la referencia es lo que permite comparar respuestas con L Chat.
- **D10 — La nota de `get_info` va en recurso, no como dato de protocolo**. Las descripciones de los
  `ToolSpec` son instrucciones para el modelo y se quedan en inglés como dato (D6 de la 042); la nota,
  en cambio, forma parte del contenido que el modelo devuelve y que el **usuario lee en el chat**, así
  que es texto visible y cae bajo el principio 6 de la constitución y RNF-192/RNF-199 de la 042:
  clave `get_info_note` en los 9 locales. *Descartado:* constante en el objeto `GetInfoTool` junto a
  la descripción — sería intraducible y dejaría al usuario leyendo inglés en un chat localizado; y
  descartado construirla con un literal en el handler — rompería el grep de literales y el principio 6.
- **D11 — Formateadores puros con los valores externos como parámetros** (`instant`, `zoneId`, `os`,
  `api`, `note`; el «hoy» se inyecta). El handler recoge `Instant.now()`, `ZoneId.systemDefault()`,
  `Build.VERSION.*` y `getString`, y la decisión de formato queda en una pieza sin `android`, testeable
  con JVM y sin Robolectric. *Descartado:* formatear dentro del handler con `android.os.Build` y
  recursos — dejaría la regla sin prueba, y en este host Robolectric no ejecuta (regla permanente:
  una regla sin prueba es una regla que nadie sabe si funciona); descartado también añadir
  `kotlinx.serialization` solo para dos objetos — nueva dependencia sin motivo (RNF-197), resuelta
  con el helper `jsonString`.

- **D12 (enmienda RF-313/RNF-204) — Un solo manejador en la fila: se quita el `.clickable` del `Row` y
  se deja `Checkbox.onCheckedChange` como único punto de interacción**. El doble handler
  (`Row.clickable` + `Checkbox.onCheckedChange`) hacía que un toque en la casilla pudiera producir dos
  toggles: el evento llegaba al `Checkbox` y se propagaba al `Row`, y el segundo toggle devolvía la
  herramienta a su estado inicial. Quitar el `.clickable` hace que tocar la fila fuera de la casilla ya
  no haga nada —aceptable, porque el control visible y accesible es la casilla— y que un toque en ella
  produzca exactamente un toggle. *Descartada:* fila `toggleable(checked, onCheckedChange)` con la
  casilla pasiva: requiere `Modifier.toggleable` y su `InteractionSource`, añade estado de interacción
  que hoy no se usa, sigue dejando dos manejadores distintos que pueden solaparse y aparta a la casilla
  de su papel de objetivo accesible natural. *Descartada también:* dejar el `.clickable` y quitar el
  `onCheckedChange` de la casilla — una casilla sin handler deja de ser interactuable y rompe la
  semántica de `Checkbox`. El maestro (`ToolsMasterRow`) y el contador no se tocan.

## Estrategia de tests (constitución p. 4: rojo → verde)

JUnit4 **puro, sin Robolectric** (precedentes: `DisabledToolsTest`, `ToolRegistryTest` de la 042,
`PrefsFalso` de la 028). Suite completa con los 38 fallos de plataforma conocidos en `linux/aarch64`;
las dirigidas de esta spec se ejecutan y quedan verdes aquí.

| Test | Ficha | Qué fija | RF |
|---|---|---|---|
| `MasterToggleTest` (nuevo, `app/src/test/java/com/example/data/ai/`) | 1 | `allToolsToggle`: desactivar todas añade las registradas al conjunto; activar todas lo vacía y elimina nombres muertos; idempotente. `enabledToolCount`: 5/5, 4/5, 0/5 y 0/0 con registro vacío. `allToolsEnabled`: solo cierto si todas activas; con lista vacía devuelve cierto (la UI deshabilita el control) | RF-308, RF-309 |
| `ToolFormatTest` (nuevo, mismo paquete) | 1 | `getDatetimeResult` con `Instant` fijo y `ZoneId` de offset positivo (`+02:00`), negativo (`-04:00`), UTC (`+00:00`, sin `Z`), media noche/cambio de año y horario de verano; `timezone` es el id IANA inyectado. `getInfoResult` con `os`/`api`/`note` fijos: forma `Android X (API N)`, sin `cwd` ni identificadores, con escape de comillas en la nota | RF-310, RF-311 |
| `ToolRegistryTest` (ampliar, mismo paquete) | 1 | Con cinco herramientas registradas, `names()` devuelve las cinco en orden y `getSpecsForApi()` sin conjunto manda cinco (invariante «3 → 5») | RF-312 |
| `ToolsSheetInvariantsTest` (nuevo, `app/src/test/java/com/example/ui/chat/`) | 1 | Lee `ToolsSheet.kt`, extrae el cuerpo de `ToolsRow` y fija **exactamente un** `onToggle(name)` y **cero** `.clickable`, quitando antes comentarios y textos para que un KDoc no falsee el recuento | RF-313, RNF-204 |

 Ejecución: escribir cada test en rojo → implementar → `./gradlew :app:compileDebugUnitTestKotlin` y
 `./gradlew :app:testDebugUnitTest --tests "com.example.data.ai.MasterToggleTest"
 --tests "com.example.data.ai.ToolFormatTest" --tests "com.example.data.ai.ToolRegistryTest"
 --tests "com.example.data.ai.DisabledToolsTest"
 --tests "com.example.ui.chat.ToolsSheetInvariantsTest"` en verde. Al cierre, `./gradlew test` (los 38 de
 plataforma, ajenos); la verificación en verde completa queda para host `x86_64`, como en 002/032/042.

## Verificación de invariantes (cierre, T9)

```bash
grep -c "register(" app/src/main/java/com/example/ui/viewmodel/AiViewModel.kt                 # 5
grep -n 'GetDatetimeTool\|GetInfoTool' \
     app/src/main/java/com/example/ui/viewmodel/AiViewModel.kt                                  # ≥ 1 c/u
for k in tools_all tools_enabled_count get_info_note; do
  for d in values values-es-rVE values-pt-rBR values-fr values-it values-en-rGB \
           values-es-rES values-pt-rPT values-b+es+419; do
    grep -q "name=\"$k\"" "app/src/main/res/$d/strings.xml" || echo "FALTA $k en $d"
  done
done
grep -n "'\|'" app/src/main/res/values*/strings.xml | grep -v "\\\\'"   # sin apóstrofes crudos nuevos
grep -rn 'Text("\|Text(text = "' app/src/main/java/com/example/ui/chat/ToolsSheet.kt            # 0 literales
grep -n '\.clickable\|onToggle(name)' app/src/main/java/com/example/ui/chat/ToolsSheet.kt       # 0 .clickable, 1 onToggle(name) en ToolsRow (RF-313)
grep -n 'getSpecsForApi(_disabledTools.value)' \
     app/src/main/java/com/example/ui/viewmodel/AiViewModel.kt                                  # 1 (flujo intacto)
```
