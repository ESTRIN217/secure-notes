# Spec 040 — Info del modelo on-device (vista de detalles)

Fecha: 2026-11-16
Estado: **implementada y verificada en dispositivo** (2026-11-22; implementada 2026-11-16)
RF-281…RF-292 · RNF-174…RNF-178 · HU-1 · HU-2 · HU-3

## Contexto y objetivo

Hoy el único acceso a información de un modelo on-device es el ⓘ de la hoja
de selección (<600 dp, `ModelSelectorSheet`), que abre un diálogo pobre: quant,
RAM mínima y un enlace a Hugging Face. En el panel de gestión (≥600 dp,
`ModelManagerPanel`) no existe ningún acceso a información del modelo.

La referencia (página de detalles de modelo de la WebUI de llama.cpp) muestra,
como panel lateral: nombre del modelo con chips, estado del motor, acciones
(«Start a new chat» y «Load model»), la fila «Model» con botón copiar, las
filas «Context Size», «Parameters» y «Quantization», una sección «Chat
Template» plegable y «Delete this model from disk» en rojo.

Objetivo: que **cada** fila on-device (hoja y panel) tenga un botón ⓘ que abra
una vista de detalles del modelo con esos datos, **sin cargar pesos** y **sin
inventar datos** (dato ausente → «—»).

## Usuarios

Personas que gestionan y usan modelos on-device en el chat de IA de Secure Notes.

## Historias de usuario

- **HU-1.** Como usuario, quiero tocar ⓘ en la fila de un modelo on-device para
  ver sus detalles (archivo, contexto, parámetros, quant, chat template) sin
  descargar ni cargar nada.
- **HU-2.** Como usuario, quiero desde los detalles cargar o descargar el modelo
  (según estado) o abrir una conversación nueva con ese modelo.
- **HU-3.** Como usuario, quiero eliminar el archivo del modelo desde los
  detalles, con confirmación previa.

## Definiciones

- **Vista de detalles** («info»): pantalla/hoja con los datos de un modelo del
  catálogo. Abrirla **nunca** dispara la carga del motor ni `ensureReady()`.
- **«—»**: dato ausente; la vista nunca estima ni sustituye por un valor fijo.
- **context_length**: el declarado por el GGUF (`dimensions.context_length`),
  la misma fuente que usa la tabla del panel (spec 033, RF-241); llega a la vista
  por el `StateFlow` ya recolectado (`onDeviceContextLengths` /
  `ModelSelectorState.contextLengths`, prefs `ai_model_context_lengths`).
- **paramsLabel**: etiqueta declarada en el catálogo, nominal publicada por el
  autor del modelo; no se deriva ni se lee de disco.

## Requisitos funcionales

- **RF-281**: CUANDO el usuario toca el ⓘ en la fila de un modelo on-device
  (hoja o panel), EL SISTEMA abre la vista de detalles de ese modelo. El
  diálogo de información anterior deja de existir y con él se retiran las
  strings `chat_model_quant_line` y `chat_model_open_hf` (uso exclusivo suyo).
- **RF-282**: EL SISTEMA presenta la vista de detalles como panel lateral
  derecho de **420 dp** de ancho (alto completo, scrim sobre el resto, por
  encima de cualquier otra superficie) en pantallas de ancho ≥600 dp y como
  hoja inferior (hermana independiente, no sustituye a la hoja de selección)
  en <600 dp; ambas con botón cerrar y conviviendo con el selector/panel de
  gestión abierto detrás, que queda inactivo. MIENTRAS la vista de detalles
  está abierta, el evento atrás la cierra **únicamente** a ella (las superficies
  de detrás no se cierran); después, el atrás cae en cascada sobre el panel de
  gestión, la hoja de selección y el chat.
- **RF-283**: La cabecera de la vista muestra `displayName` y, cada chip solo si
  hay dato, los chips de parámetros (`paramsLabel`, el mismo valor que la fila
  «Parameters») y de quantización; y el estado del modelo con un punto de
  color, según este criterio exacto: **verde «Loaded»** ⇔ backend ON_DEVICE ∧
  `selectedOnDeviceId == model.id` ∧ `onDeviceModelState == READY` (reusa
  `models_loaded`); **gris «Not loaded»** en el resto de casos (reusa
  `models_not_loaded`); **spinner** ⇔ es el activo ∧ `LOADING`; **ERROR** se
  pinta como gris «Not loaded» (de cara al punto) y el botón de ciclo de vida
  ofrece «Retry» (`ai_ondevice_retry`). MIENTRAS el motor está en LOADING para
  este modelo, los botones de ciclo de vida y de ciclo de conversación se
  muestran deshabilitados.
- **RF-284**: EL SISTEMA muestra dos botones de acción. **«Nueva conversación»**
  (visible con `chat_new`) queda **deshabilitada si el archivo no está en
  disco**. **Botón de ciclo de vida**, según el estado:
  - sin archivo en disco → **«Descargar»** (`ai_ondevice_download`): inicia la
    descarga (y nada más; **no** selecciona, **no** auto-carga al terminar);
  - descargando **este** modelo → indicador de progreso (o indeterminado) y
    botón deshabilitado;
  - descarga de **otro** modelo en curso → botón de descarga deshabilitado;
  - en disco y no cargado (o ERROR) → **«Load Model»** (`ai_ondevice_load`):
    equivalente a seleccionar el modelo ∧ `setBackend(ON_DEVICE)` ∧ cargar
    (mismo camino que el ⓘ de las filas existente), incluido el caso «otro
    modelo cargado» (la selección suelta los pesos del anterior);
  - activo ∧ READY → **«Unload»** (`ai_ondevice_unload`, `unloadModel`; no
    toca la selección ni la descarga de red).
- **RF-285**: CUANDO el usuario toca «Nueva conversación» (solo con archivo en
  disco), EL SISTEMA selecciona ese modelo, pone el backend en on-device, crea
  una sesión nueva del chat (`createSession(backend = ON_DEVICE,
  modelName = displayName)`) y cierra la vista de detalles y, si están
  abiertas, la hoja de selección y el panel de gestión; la pantalla queda
  mostrando la sesión nueva. **Ningún otro control de la vista dispara carga
  de pesos ni `ensureReady()`** (tampoco «Nueva conversación»: la carga ocurre
  en el primer envío, por el flujo existente).
- **RF-286**: La fila «Model» muestra `huggingface.co/<repo>` y, si `quantLabel`
  no está vacío, ` : <QUANT>` (sin «:» colgante si falta el quant), con un
  icono de copiar (`chat_model_info_copy_desc`); al tocarlo, el texto **visible
  completo** va al portapapeles.
- **RF-287**: La fila «Context Size» muestra el `context_length` del GGUF
  (caché `onDeviceContextLengths` / `ModelSelectorState.contextLengths`,
  prefs `ai_model_context_lengths`; **nunca** una lectura de disco desde la
  vista), con separador de miles del locale del dispositivo
  (`LocalConfiguration.locales[0]`, como el panel) y el sufijo « tokens» que ya
  antepone `formatTokens`; SI no hay dato, ENTONCES muestra «—». Nunca 8192
  como comodín.
- **RF-288**: La fila «Parameters» muestra **la etiqueta `paramsLabel`
  declarada en el catálogo** (y el chip de cabecera usa el mismo valor); todas
  las entradas actuales del catálogo declaran su etiqueta con los tamaños
  nominales publicados: `smollm2-360m`→360M, `qwen2.5-0.5b`→0.5B,
  `qwen3-0.6b`→0.6B, `gemma3-1b`→1B, `llama3.2-1b`→1B, `smollm2-1.7b`→1.7B,
  `qwen2.5-1.5b`→1.5B, `deepseek-r1-distill-qwen-1.5b`→1.5B, `qwen3-1.7b`→1.7B,
  `gemma3-2b`→2B, `gemma4-e2b`→E2B, `gemma2-2b`→2B, `llama3.2-3b`→3B,
  `phi3-mini`→3.8B, `phi3.5-mini`→3.8B. `paramsLabel` es **obligatoria y
  no vacía** en `OnDeviceModel`.
- **RF-289**: La fila «Quantization» muestra un chip con `quantLabel`; SI está
  vacío, ENTONCES la fila no se pinta.
- **RF-290**: La sección «Chat Template» inicia plegada y es expandible. Por
  primera vez para ese modelo, al expandirla EL SISTEMA lee del archivo GGUF
  `tokenizer.chat_template` (expuesto por el lector como
  `TokenizerInfo.chatTemplate`) usando un **lector a medida**
  (`GgufMetadataReader.create(skipKeys = …sin «tokenizer.chat_template»)`;
  el lector por defecto la omite y devolvería siempre `null`); la lectura
  solo toca metadatos, nunca carga pesos ni tensores, y se ejecuta fuera del
  hilo principal; MIENTRAS lee, muestra un indicador; SI el archivo no existe
  o la lectura falla, ENTONCES muestra «No disponible». El resultado se
  cachea **en memoria** por `model.id`: siguientes expansiones no releen el
  archivo. El borrado del archivo (RF-291) **invalida esa caché** para ese
  `model.id`.
- **RF-291**: MIENTRAS el archivo está descargado, la vista muestra al final
  «Eliminar del disco» en rojo con icono de papelera (reusa `models_delete_*`);
  CUANDO se pulsa, EL SISTEMA pide confirmación; AL confirmar, intenta borrar
  el archivo: SI el borrado falla, ENTONCES la vista **permanece abierta** y
  muestra el error en una fila inline dentro de la propia vista (el detalle
  surfacea `errorMessage` del VM, que hoy no tiene pintor en el chat); AL
  borrar con éxito, cierra la vista e invalida la caché del chat template (RF-290).
- **RF-292**: Las filas de modelos ocultas (sección «Ocultos») y las filas de
  Ollama NO muestran el botón ⓘ.

## Requisitos no funcionales

- **RNF-174**: La lectura del chat template **no carga pesos ni tensores**
  (solo metadatos del GGUF), se ejecuta fuera del hilo principal y cualquier
  fallo se traduce en «No disponible» sin afectar al resto de la vista ni
  tumbar la app.
- **RNF-175**: La vista de detalles no hace ninguna llamada de red; la única
  acción de red posible es la descarga, iniciada exclusivamente por el botón
  «Descargar» (RF-284).
- **RNF-176**: Cero datos inventados: «—» ante dato ausente; el 8192 del motor
  no se presenta como context_length del modelo; `paramsLabel` proviene del
  catálogo declarado, nunca de heurísticas.
- **RNF-177**: Todo texto visible sale de `strings.xml` en los 9 locales; sin
  literales en `Text(` ni en `contentDescription`. Strings **reutilizadas**:
  `models_col_model` (rótulo «Model»), `models_loaded`, `models_not_loaded`,
  `ai_ondevice_download`, `ai_ondevice_load`, `ai_ondevice_unload`,
  `ai_ondevice_loading`, `ai_ondevice_retry`, `chat_new`, `models_delete_title`,
  `models_delete_body`, `models_delete_disk`, `close`, `chat_model_info_desc`.
  Strings **nuevas** (9 locales): `chat_model_info_context_size`,
  `chat_model_info_parameters`, `chat_model_info_quantization`,
  `chat_model_info_chat_template`, `chat_model_info_template_unavailable`,
  `chat_model_info_copy_desc`, `ai_ondevice_delete_failed`. El «—» es un
  guion tipográfico, no una palabra.
- **RNF-178**: Abrir la vista tiene coste constante: **ninguna** lectura de
  disco ocurre al abrirla (tampoco comprobaciones de existencia: el estado
  «descargado» viene del `StateFlow` recolectado); el chat template solo se
  lee al expandir la sección, a lo sumo una vez por `model.id` y sesión de
  proceso (caché en memoria, invalidada al borrar el archivo).

## Casos límite

- **Modelo no descargado**: la vista abre; botón «Descargar»; «Nueva
  conversación» deshabilitada; «Chat Template» → «No disponible»; «Eliminar del
  disco» no se pinta; Context Size «—» si no hay dato en caché.
- **Archivo corrupto**: lecturas fallan → Context Size «—» y Chat Template
  «No disponible»; el resto de la vista operativa.
- **Motor en LOADING para este modelo**: spinner en la cabecera; botones
  deshabilitados.
- **Motor en ERROR para este modelo**: punto gris «Not loaded»; botón «Retry».
- **Descarga de otro modelo en curso**: botón de descarga deshabilitado; el
  resto de la vista operativa.
- **En disco, no activo, con otro modelo READY**: botón «Load Model» (la
  selección suelta los pesos del anterior).
- **Portapapeles no disponible**: copiar no falla (el sistema puede ignorar la
  escritura).
- **Atrás con detalles abiertos** y con el selector/panel también abiertos: se
  cierra únicamente la vista de detalles.
- **Borrado fallido (archivo no eliminado)**: la vista sigue abierta y se
  muestra el banner de error; la caché del template no se toca (el archivo
  sigue ahí).
- **Borrado con éxito y misma sesión de proceso**: la caché del template para
  ese `model.id` queda invalidada; reexpandir lee el archivo nuevo (o muestra
  «No disponible» si ya no existe).

## Fuera de alcance

- Ollama: ninguna fila ni vista de detalles para backend Ollama.
- Cambiar el selector o el panel más allá de añadir el botón ⓘ on-device y de
  ajustar el ancho de la columna Actions del panel (de 56 a 96 dp).
- Persistir el chat template en preferencias o base de datos.
- La nota «/props … appear once the model is loaded» de la referencia: no
  aplica (en esta app no hay servidor `/props`).
- Mostrar la ruta absoluta del archivo del modelo.
- Leer `tokenizer.chat_template` en el arranque o al abrir la vista.
- El chip «Instruct» de la referencia: el catálogo no declara plantilla, por
  lo que no se pinta (sería un dato inventado).
- Cualquier estrategia de carga/descarga de pesos distinta de la existente
  (`selectOnDeviceModel`, `setBackend`, `loadSelectedModel`, `unloadModel`,
  `downloadOnDeviceModel`, `deleteOnDeviceModel`).

## Criterios de finalización

- ⓘ visible y operativo en las filas on-device de la hoja y del panel; abre la
  vista de detalles (lateral 420 dp en ≥600 dp, hoja hermana en <600 dp).
- RF-281…RF-292 verificados contra la spec; ningún literal nuevo en `Text(` o
  `contentDescription`; las nuevas strings presentes en los 9 locales;
  `./gradlew test` conserva el estado actual (únicamente los 38 fallos de
  plataforma); verificación en dispositivo; `MEMORY.md` actualizado.

## Decisiones confirmadas

- **A. Context Size**: «—» si no hay dato GGUF (nunca 8192 como comodín).
- **B. Chat Template**: lectura real on-demand del GGUF al expandir, con
  lector a medida sin skip de `tokenizer.chat_template`; caché en memoria por
  `model.id` invalidada al borrar el archivo.
- **C. «Nueva conversación»**: selecciona el modelo + backend on-device y crea
  sesión nueva (`modelName = displayName`); deshabilitada sin archivo en disco.
- **D. Nota «/props»**: se omite.
- **E. Parameters**: etiqueta declarada en el catálogo (`paramsLabel`
  obligatoria); la misma etiqueta en chip y fila; nunca regex ni GGUF.
- **F. ⓘ en el panel ≥600 dp**: icono suelto antes del menú ⋮.
- **Ancho del panel lateral (RF-282)**: 420 dp, conviviendo (inactivo) con el
  selector/panel de gestión abierto detrás; el atrás cierra solo la vista.
- **Estado «Loaded» (RF-283)**: verde ⇔ activo ∧ ON_DEVICE ∧ READY; ERROR se
  pinta como «Not loaded» con botón «Retry».
- **Descarga (RF-284)**: «Descargar» no selecciona ni auto-carga; durante la
  descarga de este modelo el botón muestra progreso/deshabilitado; durante la
  descarga de otro modelo se deshabilita.
- **Strings huérfanas**: `chat_model_quant_line` y `chat_model_open_hf` se
  eliminan en la misma entrega (uso exclusivo del diálogo retirado).
- **Borrado fallido (RF-291)**: la vista permanece abierta y se muestra el
  banner de error del ViewModel; solo el borrado con éxito cierra y purga la
  caché.
- **Carga de pesos**: solo «Load Model» dispara `ensureReady()`; los demás
  controles nunca lo hacen.

## Dudas abiertas

- Ninguna.
