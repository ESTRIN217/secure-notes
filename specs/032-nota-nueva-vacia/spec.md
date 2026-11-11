# Spec 032 — La nota nueva vacía se descarta

Estado: **cerrada — implementada y verificada en dispositivo** (2026-11-11). RF-207…RF-214, RNF-135…RNF-139.

> **Nota de procedencia.** Encadena con la spec 027 («borrador sin duplicar»): los RF más altos en uso
> son `RF-206` y `RNF-134`, así que aquí empiezan en `RF-207` y `RNF-135`.
>
> **Origen.** Comportamiento heredado de la spec 027: una nota nueva arranca con `blocks = emptyList()` y
> `DataBlock.serialize(blocks)` produce `"[]"`, que **no** es blank. Como `hasContentToSave()` decidía con
> `content.isNotBlank()`, una nota nueva completamente vacía contaba como «con contenido»: al pulsar atrás
> se **insertaba una fila vacía** y salía el toast de «guardada», y el guard `hasUnsaved` disparaba el
> diálogo «¿guardar o descartar?» sin nada que descartar.
>
> **No añade dependencias** ni claves de texto: es una regla nueva (pieza pura) y el cableado de sus
> consumidores.

## Contexto y objetivo

Al abrir el editor con `noteId == 0` (nota nueva), la app considera que hay algo que guardar aunque no haya
ni título, ni texto, ni adjuntos ni bloques: el contenido serializado es `"[]"` y `isNotBlank()` da `true`.
Consecuencias medibles hoy:

1. Pulsar atrás en una nota nueva vacía **inserta una fila vacía** y muestra «nota guardada».
2. El autoguardado (y `ON_STOP`) no persiste un borrador nunca escrito, pero **sí** escribe `"[]"` sobre la
   fila si el borrador ya la creó esta sesión (p. ej. tras adjuntar un archivo y luego borrarlo todo).
3. El aviso «¿guardar o descartar?» del cierre de pestaña aparece en un borrador nuevo vacío.
4. «Descartar» del diálogo no borra la fila aunque la sesión la haya creado.

**Objetivo:** que una nota **nueva** que esté **vacía** se descarte: salir con atrás no inserta ni deja fila,
y el guard deja de avisar. Las notas ya existentes en la base de datos no se tocan, y las notas creadas
explícitamente por el usuario (distintas del borrador) tampoco.

## Usuarios

- **HU-1.** Como quien abre una nota nueva por error y no escribe nada, quiero que al salir no quede
  ninguna nota vacía en mi lista para no ensuciarla.
- **HU-2.** Como quien escribe algo, lo borra todo y sale, quiero que tampoco quede una fila vacía si la
  nota la creé yo en esta sesión.
- **HU-3.** Como quien ya tiene notas vacías en la base de datos (creadas por este defecto), quiero que la
  app las deje quietas y no las borre sin preguntarme.

## Definiciones

- **Nota nueva / borrador**: la que se edita con `noteId == 0`, sea cual sea su estado en `DraftWriteGate`.
- **Fila creada en esta sesión**: la que `DraftWriteGate` insertó porque la sesión escribió sobre el
  borrador (`guardarConId` con destino `0`). Una fila creada **antes** de entrar al editor (nota existente, o
  creada por otra vía) **no** es una fila de la sesión.
- **Vacía** (la regla central de esta spec): título en blanco **y** lista de adjuntos vacía **y** ningún
  bloque con contenido:
  - Bloques de texto (`TEXT`, `HEADING1…HEADING4`, `BULLET_LIST`, `NUMBERED_LIST`, `CHECKLIST_ITEM`,
    `QUOTE`, `CODE_BLOCK`, `CALLOUT`, `COLLAPSIBLE`): contenido si algún segmento lleva `text` no vacío o
    `equationLatex`; en bloques legacy sin `richTextJson`, el `content` sin etiquetas HTML no vacío.
  - `IMAGE`, `VIDEO`, `AUDIO`, `VOICE`, `FILE`, `BOOKMARK`: contenido si `content` (ruta o URL) no es vacío.
  - `PAGE`, `PAGE_LINK`: contenido si `meta["noteId"]` no es vacío.
  - `TABLE`: contenido si alguna celda de alguna fila no es vacía.
  - `DRAWING`: contenido si el JSON de trazos no está vacío (`"[]"` ⇒ vacío); en dibujos legacy, si la ruta
    no es vacía.
  - `HORIZONTAL_RULE`: nunca contenido (es decoración, no datos).
  - Un bloque de texto con solo formato (negrita, color) y **sin** texto **no** es contenido.
- **Descartar**: no guardar; y si el borrador creó una fila esta sesión y está vacía, borrarla
  (borrado definitivo) al salir de forma definitiva (atrás o «Descartar»).

## Requisitos funcionales

- **RF-207**: CUANDO el usuario sale del editor con atrás y la nota nueva está vacía, EL SISTEMA sale sin
  insertar ni actualizar fila, sin mostrar el toast de «guardada».
- **RF-208**: SI el borrador creó una fila en esta sesión y la nota queda vacía al salir de forma definitiva
  (atrás o «Descartar»), ENTONCES EL SISTEMA **borra esa fila** (borrado definitivo).
- **RF-209**: CUANDO el autoguardado o el guardado de `ON_STOP` evalúan persistir sobre un **borrador**,
  EL SISTEMA **no** persiste la nota si está vacía según la regla de esta spec —tampoco sobre una fila que
  el borrador creó esta sesión—. Las notas existentes (`id > 0`) vaciadas **sí** se persisten, como hasta
  hoy: eliminarlo rompería las tachadas del usuario y esta spec no habla de notas existentes.
- **RF-210**: EL SISTEMA decide «vacía» con una **pieza pura** —título, bloques y adjuntos— sin `android` ni
  `Context`, y las cuatro que la consultan (atrás, autoguardado, `ON_STOP`, guard) consultan **la misma**
  pieza; ninguna la reescribe.
- **RF-211**: CUANDO el guard `hasUnsaved` se evalúa sobre un borrador nuevo vacío, EL SISTEMA no marca
  «sin guardar», así que el diálogo «¿guardar o descartar?» no aparece y el cierre con ✕ procede como hoy.
- **RF-212**: CUANDO el usuario pulsa «Descartar» en el diálogo del guard, EL SISTEMA borra la fila creada
  en esta sesión si la nota está vacía; si no la hay, no hace nada (el diálogo hoy solo aparece sin fila).
- **RF-213**: EL SISTEMA no descarta notas creadas explícitamente por el usuario fuera del borrador: las de
  «/page», las de «Enviar a notas» del editor de código (Markdown), las de la lista (imagen/import) y las de
  «Nuevo dibujo».
- **RF-214**: EL SISTEMA mantiene el resto del comportamiento de la spec 027: atrás guarda y sale sin
  preguntar cuando hay contenido, el guardado del diálogo promueve la pestaña y «Atrás guarda y sale» no
  cambia para notas existentes ni para notas nuevas con contenido.

## Requisitos no funcionales

- **RNF-135**: La pieza de «vacía» es **pura** (sin `android`, sin `Context`, sin Compose) y sus pruebas
  corren sin Robolectric, que no se ejecuta en esta plataforma.
- **RNF-136**: Cero dependencias nuevas y cero textos visibles nuevos: ningún `strings.xml`,
  `build.gradle.kts`, `libs.versions.toml` ni `oss-licenses.json` cambia.
- **RNF-137**: El borrado de la fila es **definitivo** (`deleteNote` del DAO), decidido así: la fila nunca
  tuvo contenido y la papelera no debe llenarse de notas fantasma.
- **RNF-138**: El borrado **nunca** ocurre a mitad de sesión (autoguardado/`ON_STOP` solo desisten de
  persistir): la fila se borra únicamente al salir de forma definitiva, para no invalidar lo que el usuario
  pueda re-escribir y no romper los adjuntos que ya referenció.
- **RNF-139**: `./gradlew test` no añade ninguna clase roja nueva: los fallos de plataforma (38) y los
  preexistentes (3) siguen siendo los mismos.

## Casos límite

- **Salir con atrás sin escribir nada** (`blocks` vacío): no se inserta nada, no hay toast (RF-207). Este es
  el defecto que originó la spec.
- **Escribir y borrarlo todo antes de salir**: si nunca se creó fila, no se inserta; si la creó esta sesión,
  se borra (RF-208).
- **Adjuntar un archivo y quitarlo todo**: la fila creada esta sesión se borra al salir; los archivos
  huérfanos que quedaran son asunto del administrador de almacenamiento, no de esta spec.
- **Solo título**: no está vacía (el título es contenido); se guarda como siempre.
- **Bloques con solo formato** (negrita, color) sin texto: vacío; sin fila (RF-210).
- **Bloque PAGE creado por el borrador**: el PAGE apunta a **otra** nota (creada aparte), así que la nota del
  borrador queda vacía y se descarta; la nota enlazada por `/page` **no** se toca (RF-213).
- **Nota existente (nota con `id > 0`) vaciada**: no está «nueva», así que no se borra ni se deja de guardar:
  se persiste con el contenido vacío actual, como hasta hoy.
- **Nota nueva con solo `HORIZONTAL_RULE`**: vacía; se descarta.
- **Dibujo insertado y luego vaciado** (`content = "[]"`): vacío; se descarta. Un dibujo con trazos: no.
- **Nota de «Nuevo dibujo»** (creada desde la lista con contenido vacío a propósito): fuera del alcance del
  descarte (RF-213).
- **Nota de «/page»**: vacía por diseño; fuera del descarte (RF-213).
- **Fallo al borrar la fila**: se registra (`Log.e`) y la salida continúa; no se bloquea la navegación.
- **Proceso muerto con la fila vacía** (sin pasar por atrás): la fila permanece; es el precio de no borrar a
  mitad de sesión (RNF-138) y es consistente con «no tocar el histórico».

## Fuera de alcance

- **Borrar/ofrecer borrar notas vacías ya existentes** en la base de datos: por decisión, se dejan como
  están; el usuario limpia cuando quiera con las herramientas existentes.
- **Las notas creadas explícitamente fuera del borrador**: «/page», Markdown del editor de código, lista
  (imagen/import), «Nuevo dibujo». No cambian de comportamiento.
- **La papelera**: esta spec no mete nada en ella; el descarte es definitivo (RNF-137).
- **El cierre con ✕**: no cambia ni su guard ni su flujo; el cambio es que un borrador nuevo vacío ya no lo
  dispara (RF-211).
- **Promocionar el borrador al salir con atrás**: sigue sin hacerlo (como en la spec 027); el guardado del
  diálogo «Guardar» sí promueve.

## Criterios de finalización

- Salir con atrás de una nota nueva vacía no crea ninguna fila ni muestra el toast (RF-207), comprobado con
  la prueba de la pieza pura más una revisión del cableado.
- Una nota nueva cuyo borrador creó fila esta sesión y quedó vacía deja **cero** filas al salir por atrás o
  por «Descartar» (RF-208, RF-212).
- El autoguardado y `ON_STOP` nunca persisten una nota vacía (RF-209).
- La definición de «vacía» vive en una pieza pura probada sin Robolectric y la usan los cuatro consumidores
  sin reescribirla (RF-210, RNF-135).
- Los nuevos tests son puros y pasan en este host; `:app` no suma clases rojas nuevas (RNF-139).
- Cero dependencias y cero textos nuevos (RNF-136).

## Cierre

**Qué queda comprobado aquí.** La pieza pura decide «vacía» con sus pruebas (RF-210, RNF-135): la regresión
`serialize(emptyList()) == "[]"` que hacía que una nota nueva sin nada contara como «con contenido». El
cableado vigila el resto (RF-207…RF-209, RF-211, RF-212); los invariantes nuevos de fuentes comprueban
que el editor no vuelve a decidir con `content.isNotBlank()` y que la pieza no importa `android`.

**Verificación en `linux/aarch64`.** `./gradlew test` → `:app` **868 tests, 38 fallos**: los 38 son de
plataforma (`DefaultNativeRuntimeLoader`, los mismos de siempre), ninguno de esta spec, y **0 errores, 0
omitidos** (RNF-139). Los 26 tests nuevos son puros y pasan. Los 3 «preexistentes» que AGENTS.md listaba
pasan ya (el contexto cambió, no esta spec los tocó). `DraftEmptinessTest` **20/0**, `DraftWriteTest`
**18/0**, `EditorInvariantsTest` **14/0** (`MAX_LINEAS` 2268→2285). Cero dependencias y cero textos
nuevos (RNF-136); el borrado es definitivo y solo al salir (RNF-137, RNF-138).

**Regresión detectada y corregida antes del cierre.** La primera versión hacía que una nota **existente**
vaciada ya no se guardara (los borrados del usuario se perdían en silencio). `canPersistChanges()`
separa «hay contenido» de «merece persistir» (RF-214): una nota existente vaciada se persiste como hasta
hoy; un borrador solo persiste con contenido.

**Verificación en dispositivo (2026-11-11).** Quien Decide ejecutó el guion completo y cumple: la nota
nueva vacía se descarta al salir (no queda fila, sin toast), la fila creada en la sesión se borra al salir
tras adjuntar y quitar todo, y la nota existente vaciada se guarda vacía como hasta hoy.

## Dudas abiertas

- **[RESUELTA] ¿El borrado al salir es definitivo o va a la papelera?** **Definitivo** (RNF-137), decidido
  con quien Decide: la fila nunca tuvo contenido y la papelera no debe llenarse de fantasmas.
- **[RESUELTA] ¿Se borran a mitad de sesión (autoguardado)?** **No** (RNF-138): solo desisten de persistir;
  borrar al re-escribir rompería referencias del propio borrador.
- **[RESUELTA] ¿Las notas vacías ya existentes se limpian?** **No** (fuera de alcance): se dejan quietas.
- **[RESUELTA] ¿Y las notas creadas a propósito («/page», Markdown, lista, «Nuevo dibujo»)?** **Nunca se
  descartan** (RF-213).
- **[RESUELTA] ¿Una nota existente vaciada se borra?** **No**: esta spec solo habla de notas nuevas.
