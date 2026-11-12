# Spec 033 — Razonamiento y modelo en el chat

Fecha: 2026-11-11
Estado: **implementada (pendiente verificación en dispositivo)** — 2026-11-11
RF-215…RF-228 · RNF-140…RNF-145 · HU-1 · HU-2

## Contexto y objetivo

El campo de entrada flotante del chat (`FloatingChatInputField`, RF-33 de la spec 006) tiene hoy dos botones del diseño en estado de stub: un botón «reasoning» (icono de bombilla, sin acción) y un botón de modelo (`Box` + «Qwen/Qwen2.5-coder 1.5b Instruct» + chevron, sin acción). El código del stub **no compila** (`Icons.Filled.Lightbuld`, `text()` en minúscula, importación de `Preview` vacía).

Además, el motor no ofrece ningún control del esfuerzo de razonamiento: `AiViewModel.execute()` enriquece siempre con `maxTokens = _maxTokens.value` (preferencia global `ai_max_tokens`, default 256) y no hay forma de pedir más presupuesto a una respuesta concreta ni de desactivar el «thinking» en Ollama.

**Objetivo:** los dos botones funcionan de verdad. El primero permite elegir el esfuerzo de razonamiento por sesión (default / off / low / medium / high / max) y la elección afecta a la siguiente petición; en pantallas grandes se presenta como menú anclado en vez de hoja modal. El segundo muestra el modelo activo y permite cambiarlo desde el propio chat (on-device descargado u Ollama), sin pasar por Ajustes → IA.

## Usuarios

Usuario final del chat de IA de Secure Notes (persona dueña de sus notas y de su modelo, local-first).

## Historias de usuario

- **HU-1.** Como usuario, quiero ajustar el esfuerzo de razonamiento de la siguiente respuesta, para reservar presupuesto amplio para problemas difíciles y gastar poco en respuestas sencillas.
- **HU-2.** Como usuario, quiero ver y cambiar el modelo que responde dentro del chat, para no saltar a Ajustes cada vez que quiero cambiar de motor (on-device u Ollama).

## Definiciones

- **Reasoning effort**: ajuste del chat que decide (a) el tope de tokens de la respuesta y (b) si se pide «thinking» a Ollama. Niveles:
  | Nivel | Tope de tokens | `think` (Ollama) |
  |---|---|---|
  | `DEFAULT` | preferencia `ai_max_tokens` (default 256) | omitido (default del modelo) |
  | `OFF` | preferencia `ai_max_tokens` | `false` |
  | `LOW` | 512 | `true` |
  | `MEDIUM` | 2048 | `true` |
  | `HIGH` | 8192 | `true` |
  | `MAX` | ilimitado | `true` |

- **Esfuerzo activo**: un único ajuste **global al proceso** (un `StateFlow` en `AiViewModel`), compartido por todas las sesiones de chat abiertas, no por conversación ni por mensaje. No sobrevive al proceso: al reabrir la app vuelve a `DEFAULT`.
- **Pantalla grande**: ancho `screenWidthDp >= 600` (mismo corte que la lista principal).
- **Ilimitado**: en Ollama, `num_predict = -1` en `options` (convenio nativo). En llama.cpp no existe «infinito» seguro en `stop_generation_position = posición + prompt + n_predict` (suma en entero de 32 bits): se aplica un tope grande documentado (1 000 000 + prompt ≪ 2³¹−1).
- **Modelo activo**: en Ollama, el nombre de la preferencia de modelo (o el valor actual de `_modelName`); en on-device, el `displayName` del modelo seleccionado (`_selectedOnDeviceModel`), o vacío si no hay ninguno.
- **Tope nominal vs efectivo**: los rótulos «máx. 512/2048/8192 t» describen el tope pedido (`num_predict` / `n_predict`). En on-device la generación además muere en el contexto (8192, hardcoded) y en Ollama `num_predict` cuenta thinking + respuesta: el rótulo no promete salida efectiva garantizada.

## Requisitos funcionales

- **RF-215**: CUANDO el usuario pulsa el botón de razonamiento, EL SISTEMA abre una hoja modal (`<600 dp`) o un menú desplegable anclado al botón (`>=600 dp`) con las seis opciones en este orden: Default, Off, Low, Medium, High, Max. Cada opción muestra su tope como subtítulo con formato parametrizado (`%1$d`, p. ej. «Máx. 512 tokens»); «Max» muestra «Ilimitado»; «Default» muestra «Usa el ajuste de la app» con el valor actual (p. ej. «máx. 256 t»). La presentación (hoja vs menú) la decide una pieza pura a partir de `screenWidthDp` (RNF-140).
- **RF-216**: MIENTRAS el esfuerzo activo no es `DEFAULT`, el botón MUESTRA el nivel (p. ej. «High») junto al icono, en una línea con etiqueta desde `strings.xml`; CUANDO es `DEFAULT`, el botón MUESTRA solo el icono. El `contentDescription` del botón incluye siempre el nivel («Nivel de razonamiento: High» / «Nivel de razonamiento: Default»), sea cual sea el ancho.
- **RF-217**: CUANDO el usuario selecciona una opción de esfuerzo, EL SISTEMA la convierte en el esfuerzo activo global y cierra la hoja/menú (incluso si la selección falla: la hoja no se queda colgada).
- **RF-218**: CUANDO se envía una petición al modelo en el chat, EL SISTEMA aplica el esfuerzo activo: `DEFAULT`/`OFF` usan `ai_max_tokens`; `LOW`/`MEDIUM`/`HIGH` usan 512/2048/8192; `MAX` usa ilimitado (Ollama `-1` en `options.num_predict`; on-device el tope grande documentado). Esfuerzo solo-por-proceso: al matar la app se restaura `DEFAULT` (RF-221).
- **RF-219**: CUANDO el backend es Ollama, EL SISTEMA añade `think` **top-level** en `/api/chat` (hermano de `model`/`messages`/`options`, **no** dentro de `options`) según el esfuerzo (`OFF`→`false`; `LOW`..`MAX`→`true`; `DEFAULT`→campo ausente); CUANDO el backend es on-device (llama.cpp), EL SISTEMA ignora `think` y aplica solo el tope de tokens.
- **RF-220**: EL SISTEMA aplica el esfuerzo **solo al chat** (envío de mensajes, incluidas las reejecuciones tras editar un mensaje); las acciones de texto con resultado in-place (resumir, corregir gramática) mantienen su cota rápida propia (`coerceAtMost(512)`) y no se ven afectadas.
- **RF-221**: EL SISTEMA guarda el esfuerzo activo solo en memoria (estado global al proceso en `AiViewModel`); no escribe en preferencias, Room ni en la copia de seguridad. Al recrear el proceso vuelve a `DEFAULT` y el botón pinta solo el icono.
- **RF-222**: CUANDO el usuario pulsa el botón de modelo, EL SISTEMA muestra en el botón, en una sola línea con `maxLines=1` y elipsis, el nombre del modelo activo; si está vacío (on-device sin selección), MUESTRA el texto «Sin modelo» (clave nueva) y el botón sigue habilitado y abre el selector. El `contentDescription` del botón es «Cambiar modelo, actual: <nombre>» / «Cambiar modelo, actual: sin modelo».
- **RF-223**: CUANDO el usuario pulsa el botón de modelo, EL SISTEMA abre una hoja modal (`<600 dp`) o un menú anclado (`>=600 dp`) que lista: (a) **En el dispositivo** — los modelos del catálogo con `.gguf` ya descargado según `downloadedModelIds`; si ninguno lo está, un estado vacío con texto de aviso (selector sin acciones en esa sección); (b) **Ollama** — si hay lista `/api/tags` cacheada en el último estado Connected, se lista para elección directa sin llamada de red; si no la hay, se muestra el modelo configurado y un botón «Probar conexión». Al pulsar «Probar conexión»: estado de carga visible; en éxito se listan los modelos; en fallo, mensaje inline con el texto del error (reusa `ai_connection_fail`) y el botón queda disponible para reintentar; la hoja no se cierra. La hoja/menú abierta con datos cacheados no llama a la red.
- **RF-224**: CUANDO el usuario selecciona un modelo on-device del selector, EL SISTEMA lo establece como modelo activo y cambia el backend a on-device; si el modelo elegido es distinto del cargado, EL SISTEMA suelta los pesos mediante `AiModelHost` (release en background, igual vía que descarga/borrado en Ajustes) y la siguiente petición carga el modelo nuevo perezosamente; si la carga falla, el error aflora por el canal existente (`ModelState.ERROR` + error visible). CUANDO selecciona un modelo de Ollama, EL SISTEMA establece su nombre como modelo activo y cambia el backend a Ollama; el selector se cierra en ambos casos.
- **RF-225**: EL SISTEMA coloca el campo de texto en una **línea propia encima de la fila de botones** (ancho completo con `fillMaxWidth()`) y la fila debajo en este orden: adjuntar (+), esfuerzo, modelo, enviar/detener; el botón de enviar queda anclado al extremo derecho (`Spacer` con peso entre esfuerzo y modelo). En ancho compacto (<600 dp) el nombre del modelo es quien cede por elipsis; las etiquetas de esfuerzo (Off/Low/Med/High/Max) no se recortan; el layout no desborda a 320 dp. El comportamiento del campo (RF-33 de la spec 006: escribir deshabilitado durante generación, `canSend || isProcessing`, vibración, etc.) no cambia.
- **RF-226**: MIENTRAS el modelo está generando (`isProcessing`), EL SISTEMA mantiene los dos botones operativos; un cambio de esfuerzo u de modelo aplica a la **siguiente** petición, no a la generación en curso. Durante una carga perezosa de pesos (`onDeviceModelState == LOADING`), una petición nueva falla rápido con el error existente («load in progress») y el usuario reenvía cuando termine.
- **RF-227**: EL SISTEMA deja el archivo `FloatingChatInputField.kt` compilando: sin stubs rotos (`Icons.Filled.Lightbuld`, `text()` en minúscula), sin la `@Preview` vacía ni el import muerto; los dos botones usan iconos de `material-icons-core`.
- **RF-228**: EL SISTEMA mantiene el comportamiento actual de `loadSession`: al restaurar una sesión, backend y modelo se restauran al snapshot con el que se creó la sesión (`ChatSessionEntity.backend`/`modelName`), sobrescribiendo la elección global. La selección hecha en el selector del chat se persiste en las preferencias globales (como en Ajustes) y aplica a sesiones nuevas; las sesiones antiguas restauran su snapshot al reabrirse. Este comportamiento está vigente hoy y queda **declarado, no corregido** por esta spec (ver Fuera de alcance).

## Requisitos no funcionales

- **RNF-140**: Funciones puras en JVM para: mapeo esfuerzo→tokens y esfuerzo→`think` (y su colocación documentada: top-level en Ollama), decisión hoja/menú (`screenWidthDp >= 600`), y etiqueta corta de esfuerzo. La lógica no vive en `@Composable` ni en el ViewModel; cubiertas por tests puros (color/Compose ausentes).
- **RNF-141**: Cero dependencias nuevas (Constitución #1); iconos del set `material-icons-core` ya presente.
- **RNF-142**: Ninguna cadena visible está escrita en el código; todos los textos visibles salen de `strings.xml` con claves nuevas y se añaden en los **9** locales (Constitución #6); el invariante de claves equivalentes en 9 locales sigue verde. Los subtítulos numéricos usan formato parametrizado (`%1$d`) y nada de abreviaturas tipo «máx.» hardcoded; los rótulos de esfuerzo (Off/Low/Medium/High/Max) son claves localizadas en los 9 locales.
- **RNF-143**: El motor on-device sigue sin disponer de «thinking»: el esfuerzo solo recorta/amplía el tope de tokens; el `max` ilimitado nunca desborda el entero de 32 bits de `stop_generation_position` (1 000 000 + prompt ≤ 8192 ≪ 2³¹−1).
- **RNF-144**: La decisión hoja/menú se toma una vez por composición con `LocalConfiguration` (corte 600 dp); exactamente una lectura de `LocalConfiguration`/`screenWidthDp` en este componente (verificable por grep en fuentes), sin estado duplicado en dos niveles.
- **RNF-145**: Accesibilidad: ambos botones tienen `contentDescription` autónomo con la información de estado (nivel de razonamiento actual / modelo actual); los estados de carga y error de «Probar conexión» son anunciables por TalkBack (texto visible, no solo color).

## Casos límite

- **Caso límite 1**: `screenWidthDp == 600` → menú (la hoja es solo para `<600`).
- **Caso límite 2**: Esfuerzo `MAX` con un modelo de Ollama sin capacidad de pensamiento (p. ej. un instruct no-thinking): `think=true` es ignorado o no aplicable; la respuesta se genera sin pensamiento; no hay error visible adicional. (Nombres model-defined como `low|medium|high` vía `/api/show` quedan descartados — ver Decisiones cerradas.)
- **Caso límite 3**: Esfuerzo `MAX` en on-device: el tope aplicado es 1 000 000 tokens; la generación se detiene por EOS o por llenar el contexto (8192, hardcoded), igual que hoy.
- **Caso límite 4**: Selector de modelo sin sesión Ollama alcanzable y sin `/api/tags` conocido: se muestra el modelo configurado y el botón «Probar conexión»; la descarga/eliminación de modelos on-device no se toca (sigue en Ajustes → IA).
- **Caso límite 5**: Sin modelos on-device descargados: la sección «En el dispositivo» muestra el estado vacío; la sección Ollama sigue disponible.
- **Caso límite 6**: Nombre de modelo muy largo: una línea con elipsis; el layout no desborda en ancho compacto; la etiqueta de esfuerzo no se recorta. A 320 dp se reparte: modelo cede, esfuerzo conserva.
- **Caso límite 7**: Cambio de esfuerzo/modelo durante la generación: no aborta ni ensucia la petición en curso; aplica a la siguiente.
- **Caso límite 8**: Un archivo `.gguf` que pasa `downloadedModelIds` pero desaparece en disco antes de la selección: `selectOnDeviceModel` **sí** toca el disco —invoca `ModelDownloader.getModelPath()` en el hilo llamante (hace `mkdirs()`, crea `.nomedia` y valida la cabecera GGUF)— igual que hace Ajustes hoy; la spec acepta ese I/O mínimo y no añade validación nueva. En ese caso, o el pref no se escribe (archivo ausente), o el fallo aflora en la carga con el canal de error existente. El selector **no** añade una segunda validación ni cambia ese comportamiento.
- **Caso límite 9**: Selección on-device distinta del modelo cargado con pesos ya en memoria: el motor se suelta (release) y la siguiente petición carga el modelo elegido perezosamente; una petición lanzada durante `LOADING` falla rápido con el error existente («load in progress») y el usuario reenvía.
- **Caso límite 10**: Proceso recreado con esfuerzo ≠ `DEFAULT`: vuelve a `DEFAULT` y el botón pinta solo el icono (RF-221).
- **Caso límite 11**: Reabrir una sesión antigua con distinto backend/modelo guardado en su snapshot sobrescribe la elección global actual (RF-228); comportamiento declarado, no defecto.
- **Caso límite 12**: Reiniciar la app y pulsar «Probar conexión» con el backend Ollama inalcanzable: mensaje inline de error (`ai_connection_fail`) con reintento permitido; la hoja permanece abierta.

## Fuera de alcance

- Persistir el esfuerzo entre sesiones o procesos (Room, preferencias, `BACKUP_SETTINGS`).
- Guardar el esfuerzo o el modelo en entidades de sesión/mensaje (`ChatSessionEntity`, `ConversationEntity` ya guardan `modelName` de creación de sesión).
- Cambiar el comportamiento de `loadSession` (restaurar snapshot de sesión): queda **declarado** (RF-228), no corregido.
- Nivel «xhigh»/strings model-defined en `/api/chat` vía `/api/show`: descartado por complejidad.
- Mostrar el «thinking» en streaming en la burbuja de la respuesta.
- Descarga/eliminación/carga de modelos desde el selector (vive en Ajustes → IA).
- Cambiar las acciones in-place de IA (resumir, corregir gramática).
- Persistir qué esfuerzo se usó por mensaje.
- Invariante/genérico para modelos Ollama con thinking nativo por defecto (se aplica el modelo default).

## Criterios de finalización

1. Los dos botones funcionan según RF-215…RF-228 en un build `assembleDebug` correcto; comprobación manual mínima: abrir chat, pulsar ambos botones, cambiar esfuerzo y modelo (on-device), enviar, reabrir la sesión histórica; evidencia adjunta en el cierre (captura o lista de verificación).
2. Mapeos esfuerzo→tokens/`think`, y el corte 600 dp, cubiertos por tests puros JVM en verde. `./gradlew test` sin regresiones sobre la baseline conocida de la spec 032: **868 tests, 38 fallos de plataforma (linux/aarch64), 0 omitidos**; los nuevos tests puros pasan aquí.
3. Los 9 `strings.xml` están sincronizados con todos los textos visibles nuevos; sin literales en `Text(`/`contentDescription` del código nuevo; el invariante de fuentes sigue verde.
4. `MEMORY.md` refleja la spec cerrada y sus decisiones.
5. Ninguna pantalla fuera del chat se ve afectada (Ajustes → IA sigue igual).

## Decisiones cerradas (aprobadas 2026-11-11; clarificación 2026-11-11)

- **`think` booleano uniforme**: `OFF`→`false`; `LOW`/`MEDIUM`/`HIGH`/`MAX`→`true`; `DEFAULT`→campo ausente. Strings model-defined descartados.
- **Botón de modelo en pantalla grande**: menú anclado simétrico al de esfuerzo; la hoja queda solo para `<600 dp`.
- **«Probar conexión»**: solo cuando no hay lista `/api/tags` disponible; sin llamada de red al abrir si ya hay datos; fallo inline con retry dentro de la hoja (RF-223).
- **Numeración**: RF-215…RF-228, RNF-140…RNF-145, HU-1/HU-2 (HU numera por spec, como en las specs 001-032).
- **`think` es top-level** en `/api/chat`, no dentro de `options`; en on-device el campo se ignora (RNF-140 lo deja verificable).
- **Esfuerzo activo global al proceso**: single `StateFlow` en `AiViewModel`; no por conversación ni por mensaje (Definiciones, RF-218/RF-221).
- **Subtítulo «Default»**: muestra el valor actual de `ai_max_tokens` (p. ej. «máx. 256 t»); subtítulos numéricos con `%1$d` localizado; «Ilimitado» como clave propia (RF-215, RNF-142).
- **Cambio de modelo on-device**: pesos liberados vía `AiModelHost` y carga perezosa en la siguiente petición; durante `LOADING` la petición falla rápido con el error existente (RF-224/RF-226, Caso límite 9).
- **`loadSession`**: se documenta, no se corrige — al restaurar sesión se restaura el snapshot (RF-228, Caso límite 11).
- **Caso límite 8**: se acepta el I/O mínimo de `getModelPath()` en el hilo llamante (igual que Ajustes); sin validación nueva (Caso límite 8).
- **Accesibilidad**: `contentDescription` autónomos con estado en ambos botones (RNF-145).
- **Baseline de tests**: formulación de la spec 032 (868/38) en el criterio 2.
- **Rótulos de esfuerzo** (`Default`/`Off`/`Low`/`Medium`/`High`/`Max`): tokens en inglés en los **9** locales (son nombres de nivel, no prosa); todo el resto de textos visibles sí está localizado.

## Cambio 2026-11-11 — Búsqueda y acciones por modelo

Cambio de requisitos solicitado por el usuario tras la implementación. RF-229…RF-237 amplían esta spec; el resto sigue vigente.

- **RF-229**: CUANDO el usuario abre el selector de modelo (hoja u menú), EL SISTEMA pinta una barra de búsqueda en la primera posición (placeholder «Buscar modelos…»). MIENTRAS la consulta no está vacía, la sección «En el dispositivo» lista el **catálogo completo** filtrado (insensible a mayúsculas; contra nombre, id, quantización, nombre de archivo y repo) y la sección Ollama lista los nombres cacheados filtrados. CUANDO la consulta está vacía, se ve todo.
- **RF-230**: La sección «En el dispositivo» lista el catálogo completo, no solo los descargados. Pulsar la fila de un modelo **descargado** lo selecciona (backend on-device) y cierra el selector; pulsar la fila de uno **NO descargado** inicia su descarga sin tocar la selección activa.
- **RF-231**: Cada fila on-device lleva tres botones: **info** (diálogo con quantización, RAM mínima/recomendada, repo de origen, botón «Abrir en Hugging Face» y cerrar), **favorito** (estrella) y **control de ciclo de vida del modelo en memoria** (como «Cargar modelo»/«Descargar modelo» de Ajustes → IA) con cuatro estados según el disco y el motor: (a) archivo ausente → icono `Download` → descarga, sin seleccionar; (b) archivo presente y pesos NO cargados (`NOT_LOADED`/`ERROR`) → icono `PlayArrow` → selecciona el modelo, cambia el backend a on-device y lo carga (`selectOnDeviceModel` + `loadSelectedModel`); (c) pesos cargados (`READY`, modelo activo) → icono `Stop` → `unloadModel()`, sin confirmar y sin tocar selección ni backend; (d) `LOADING` → spinner deshabilitado. No hay botón de borrar el archivo en el selector: borrar el `.gguf` vive solo en Ajustes → IA.
- **RF-232**: Solo hay una descarga a la vez: mientras una está en curso, el icono de la fila del modelo en descarga muestra progreso; los controles de descarga de las demás filas están deshabilitados. Seleccionar fila (si está descargado), cargar (`PlayArrow`) y favorito siguen disponibles. Mientras `onDeviceModelState == LOADING` el control de ciclo de vida de esa fila muestra el spinner y las cargas desde otras filas son rechazadas por el host con el error existente.
- **RF-233**: Los favoritos —on-device con clave `gguf:<id>`, Ollama con clave `ollama:<nombre>`— se persisten en SharedPreferences (`ai_favorite_models`, CSV por comas) vía `PreferencesRepository`; el selector los ordena **primero** en ambas secciones (orden estable dentro de cada grupo); el ajuste **viaja en la copia de seguridad** (`BACKUP_SETTINGS`, TEXT, por defecto `""`).
- **RF-234**: Las filas Ollama también llevan el botón de favorito (mismo mecanismo y clave `ollama:<nombre>`); no llevan info ni control de ciclo de vida.
- **RF-235**: La presentación se mantiene por ancho: <600 dp hoja, ≥600 dp menú anclado, **con la barra de búsqueda como primer ítem** y los tres botones por fila a 32 dp (targets estrechos, deliberados). En el menú se conserva «Probar conexión». Exactamente una lectura de `LocalConfiguration` (RNF-144).
- **RF-236**: Cadenas visibles nuevas en los **9** locales (`chat_model_search_hint`, `…_no_results`, `…_info_desc`, `…_favorite_desc`, `…_download_desc`, `…_open_hf`, `…_quant_line`, `…_load_desc`, `…_unload_desc`); se reutilizan las claves genéricas existentes (`close`, `cancel`, `btn_ok`). El criterio de finalización 3 sigue aplicando.
- **RF-237**: La búsqueda y los iconos no hacen red ni alteran el modelo activo; descargar o cargar un archivo no cambia el backend (la selección solo cambia al pulsar «Cargar modelo», la fila de un modelo descargado, o los atajos equivalentes); «Descargar modelo» (unload) no toca la selección ni el backend, solo los pesos.

**RNF-146**: La lógica de búsqueda y orden —filtro por consulta, «favoritos primero» (partición estable) y las claves de favorito (`gguf:`/`ollama:`)— es **pura** y tiene tests JVM; el filtrado no hace trabajo en el hilo de UI más allá de un `remember` con la consulta como clave.

## Historial de cambios

- 2026-11-11 — Clarificación QA: estado corregido, HU renumerados por spec, «Por sesión»→global al proceso, `think` fijado top-level, subtítulo «Default» con valor actual, caso límite 8 reescrito con el comportamiento real de `getModelPath()`, ON_DEVICE→ON_DEVICE con release vía `AiModelHost`, RF-228 declarando `loadSession`, RF-222 caso «Sin modelo», RF-223 estados de error/carga de «Probar conexión», RNF-145 accesibilidad, criterio 2 con baseline, eliminada referencia colgante a «Dudas abiertas» (sección renombrada a Decisiones cerradas).
- 2026-11-11 — Implementación: rótulos de esfuerzo como **tokens en inglés en los 9 locales**; selector de modelo anclado al botón (hoja/menú montados en el campo junto al botón); icono `Memory` para el botón de modelo. Verificación en dispositivo pendiente.
- 2026-11-11 — Cambio de requisitos (usuario): el campo de texto pasa a una **línea propia encima** de la fila de botones (`fillMaxWidth()`, sin `weight`); la fila queda `[+][esfuerzo][modelo]→[enviar]` con enviar anclado al extremo derecho. RF-225 actualizado.
- 2026-11-11 — Ampliación (usuario): barra de búsqueda en el selector de modelo (hojas y menú ≥600 dp), catálogo completo visible (no solo descargados), botones **info/favorito/descarga** por fila, favoritos persistidos en prefs y en la copia, progreso por fila con una única descarga a la vez. RF-229…RF-237, RNF-146; ver «Cambio 2026-11-11».
- 2026-11-12 — Corrección (usuario): el botón «upload/download» es **cargar/descargar el modelo en memoria**, como «Cargar modelo»/«Descargar modelo» de Ajustes → IA; si el `.gguf` no está en disco, descarga. Verificado el malentendido (se había interpretado como descarga/borrado de archivo). El control de ciclo de vida tiene 4 estados (RF-231); el botón de borrar el archivo **ya no existe** en el selector (borrar vive solo en Ajustes → IA); sin confirmación al descargar (unload). RF-231/RF-232/RF-236/RF-237 reescritos en la misma sección.
