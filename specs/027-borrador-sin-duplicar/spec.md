# Spec 027 — Una nota nueva no se duplica ni pregunta por lo que ya guardó

Estado: **implementada y verificada en dispositivo** (2026-10-17), T1–T10 cerradas. El aviso de «descartar» y el
botón de atrás no se pueden probar sin Robolectric, que no se ejecuta en esta plataforma, así que se verificaron a
mano con el guion de ocho pasos del final.

> **Nota de procedencia.** No encadena con ninguna spec anterior. Los RF más altos en uso son `RF-179` y
> `RNF-112` (spec 026), así que aquí empiezan en `RF-180` y `RNF-113`.
>
> **No añade dependencias** ni claves de texto: es una corrección de comportamiento en la escritura de la nota
> más una pieza pura con sus pruebas.
>
> **Origen.** Reporte del usuario: «al crear una nota nueva esta se auto guarda o se guarda al tocar el botón
> atrás, pero también sale un diálogo descartar… y si le doy guardar se duplica la nota».

## Contexto y objetivo

Una nota nueva es una **pestaña de borrador** (`OpenTab.Note(0)`). El editor no tiene una fila en la base de datos
todavía, y el guardado lo inserta: `saveNoteAndGetId` inserta cuando recibe `id = 0` (`NotesViewModel.kt:757`).

El problema es que **nadie le comunica al editor que la fila ya existe**:

1. `persistNote` (`NoteEditorScreen.kt:769`) escribe con `id = noteId`, y en una nota nueva `noteId` es `0`: se
   inserta la fila y **el id que devuelve el `insert` se tira**. La pestaña sigue siendo `note:0`.
2. El id solo se recupera en un sitio: `promoteDraft`, que llama **únicamente** el «Guardar» del diálogo de
   borrador (`NoteEditorTabsHost.kt:393`).
3. La bandera del aviso se deriva de `noteId == 0` (`NoteEditorScreen.kt:1090`), así que sigue diciendo «sin
   guardar» aunque la nota esté escrita.

De ahí los dos síntomas reportados:

- **El aviso sale después de guardar.** La flecha de atrás y el «atrás» del sistema son `handleSaveAndExit`
  (`NoteEditorScreen.kt:1289`), que **guarda** (`saveCurrentNote`, con su aviso «nota guardada») y acto seguido
  llama `onBack()`. `shouldDefer` lee `hasUnsaved`, que sigue en `true`, y muestra el diálogo. Es guardar y
  después preguntar por lo ya guardado.
- **«Guardar» duplica.** `saveAction` (`NoteEditorScreen.kt:1093`) vuelve a escribir con `id = 0`: **segundo
  `insert`**, con el mismo contenido. Dos notas idénticas.

Lo mismo ocurre con cualquier escritura **implícita** de una nota nueva —adjuntar un fichero o una grabación
(`NoteEditorFileOperations.kt:38`), o una inserción de IA (`NoteEditorScreen.kt:796` y `:824`)—: insertan la
primera fila y dejan el editor creyendo que sigue siendo un borrador, así que el «Guardar» del aviso crea la
segunda.

**Objetivo:** que una nota nueva tenga **una sola fila**, y que el aviso de «descartar» aparezca solo cuando hay
algo de verdad que perder.

## Usuarios

- Quien escribe una nota nueva y sale con atrás: espera volver a la lista con **una** nota, no con el aviso de que
  algo está sin guardar que ya está guardado, y menos todavía con dos copias.
- Quien adjunta un fichero o usa la IA dentro de una nota nueva: espera que la nota exista una vez y que seguir
  escribiendo la actualice, como en cualquier otra nota.
- Quien cierra la pestaña de una nota en la barra de pestañas: espera que ahí **sí** le pregunten, porque cerrar
  la pestaña es la acción donde perder el contenido sí es una pérdida.

## Historias de usuario

- **HU-1.** Como quien escribe una nota nueva y pulsa atrás, quiero que se guarde **una sola vez** y sin preguntas,
  para no acabar con dos notas idénticas en la lista.
- **HU-2.** Como quien adjunta un fichero a una nota nueva y sigue escribiendo, quiero que la nota se actualice y
  no se cree otra, para no acumular copias sueltas.
- **HU-3.** Como quien cierra la pestaña de una nota que aún no se ha escrito, quiero que me pregunten, para
  poder guardar o descartar a conciencia.

## Definiciones

- **Nota nueva** o **borrador**: la pestaña de nota cuyo id es `0`, que aún no tiene fila en la base de datos.
- **Fila creada**: la nota que insertó el guardado de un borrador. En cuanto existe, el borrador deja de serlo.
- **Escritura en vuelo**: una escritura lanzada cuyo id de fila todavía no ha vuelto de la base de datos. El
  contenido de pantalla ya está siendo escrito, así que no hay nada que descartar.
- **Aviso de «descartar»**: el diálogo con «Guardar / Descartar / Cancelar» de `DraftGuardDialog`
  (`NoteEditorTabsHost.kt:457`). Hoy lo comparten el cierre de la pestaña del borrador, «cerrar las demás»,
  «cerrar todas» y el atrás.

## Requisitos funcionales

- **RF-180**: CUANDO una nota nueva se escriba por primera vez, EL SISTEMA conserva el id que devolvió esa
  inserción y **toda escritura posterior actualiza esa misma fila**, en vez de crear otra.
- **RF-181**: SI dos escrituras de una nota nueva se lanzan casi a la vez —por ejemplo, adjuntar un fichero y
  pulsar atrás—, ENTONCES EL SISTEMA las escribe **de una en una** y solo **la primera** inserta: el resultado es
  una fila, no dos.
- **RF-182**: CUANDO se lance cualquier escritura de una nota nueva —con el botón de atrás, al adjuntar, con la
  IA o al autoguardar—, ENTONCES EL SISTEMA apaga el aviso de «descartar» **en el acto**, antes de que se lea si
  hay que preguntar, porque lo que hay en pantalla está siendo escrito.
- **RF-183**: SI una nota nueva tiene contenido y **no** se ha lanzado ninguna escritura, ENTONCES EL SISTEMA
  muestra el aviso de «descartar» al cerrar su pestaña, como hoy.
- **RF-184**: CUANDO el usuario pulse «Guardar» en el aviso de «descartar», EL SISTEMA inserta **una sola vez** y
  entrega a la barra de pestañas el id de la fila creada, de modo que la pestaña del borrador pase a ser esa nota.
- **RF-185**: CUANDO el autoguardado se dispare —al dejar de escribir o al mandar la app a segundo plano— y la nota
  **ya tenga fila**, EL SISTEMA la actualiza con el mismo criterio que cualquier otra nota; y si es una nota
  nueva **sin** fila, EL SISTEMA no la inscribe solo, porque hasta ese momento el autoguardado no inscribe
  borradores.
- **RF-186**: EL SISTEMA decide «con qué id se escribe» y «¿hay algo por guardar?» con **una sola** regla, en una
  pieza compartida; ni el editor ni la barra de pestañas vuelven a deducirlo de `noteId == 0`.

## Requisitos no funcionales

- **RNF-113**: Las dos reglas viven en una pieza con sus pruebas, sin `android`, para que se prueben sin
  Robolectric, que no se ejecuta en esta plataforma.
- **RNF-114**: Cero dependencias nuevas: ningún `build.gradle.kts` ni `oss-licenses.json` cambia.
- **RNF-115**: Cero textos visibles nuevos. Los tres del diálogo (`tabs_draft_dialog_title`,
  `tabs_draft_dialog_message`, `tabs_draft_save`, `tabs_draft_discard`) ya existen en los **nueve** locales.
- **RNF-116**: El editor de notas **no crece**. `NoteEditorScreen.kt` tiene hoy 2277 líneas y el presupuesto es
  exactamente ese (`EditorInvariantsTest.MAX_LINEAS`); esta spec lo **baja**, porque el guardado del aviso
  duplicaba la lista de campos y desaparece al pasar por el camino único.
- **RNF-117**: El guardado completo sigue estando en **un solo sitio** (RF-85). La cuenta de llamadas al guardado
  «a pelo» en el editor baja de 2 a 1, y la que queda es la que guarda **otra** nota (mover un bloque).
- **RNF-118**: `./gradlew test` no añade ninguna clase roja nueva: los fallos de plataforma y los preexistentes
  siguen siendo **los mismos** que antes de esta spec.
- **RNF-119**: La escritura sigue lanzándose en el ámbito del `ViewModel`, no en el de la pantalla: una rotación
  con la escritura a medias no puede perderla ni dejar el borrador a medio insertar.

## Casos límite

- **Atrás con una nota nueva con contenido**: se guarda **una** vez, con su aviso «nota guardada», y se vuelve a
  la lista. **No** aparece el diálogo (RF-182).
- **Atrás con una nota nueva vacía**: no se guarda nada y se vuelve a la lista, como hoy.
- **Atrás con una nota nueva en la que ya se adjuntó un fichero**: se actualiza la fila que ya existe; no se crea
  otra (RF-180).
- **Adjuntar un fichero y pulsar atrás casi a la vez**: una sola fila (RF-181).
- **Cerrar la pestaña del borrador con la ✕ sin haber escrito nada**: no hay aviso y la pestaña se cierra, como
  hoy. Con contenido escrito y sin escribir, aparece el aviso (RF-183).
- **Pulsar «Cancelar» en el aviso**: no se escribe nada y el editor sigue abierto, como hoy.
- **Pulsar «Guardar» en el aviso**: una sola fila y la pestaña pasa a ser esa nota (RF-184).
- **La escritura falla** (la base devuelve error): el id no se guarda, así que el siguiente intento vuelve a
  insertar. No queda un borrador marcado como escrito que en realidad no lo está.
- **Dos inserciones de IA seguidas** sobre la misma nota nueva: la segunda actualiza la fila de la primera.
- **Una nota que ya existía** (`noteId != 0`): el id de la pestaña manda siempre y nada cambia respecto de hoy.
- **Paginación del chat o del historial**: no se tocan.

## Fuera de alcance

- **La pestaña del borrador que queda en la barra** después de salir con el botón de atrás sin pasar por el aviso.
  Hoy la nota existe en la lista y la pestaña sigue apuntando al borrador vacío; convertir esa pestaña en la nota
  real al escribir sin aviso es un comportamiento **nuevo** (ver dudas abiertas) y queda para otra spec.
- **Recuperar el contenido de una nota duplicada ya creada**: esta spec evita el duplicado, no lo limpia.
- **El autoguardado de un borrador que aún no tiene fila**: sigue sin insertar (RF-185). Insertar al dejar de
  escribir crearía notas vacías al abrir y cerrar el editor.
- **Cambiar los textos o los botones del aviso**, o su redacción.

## Criterios de finalización

- Escribir en una nota nueva y pulsar atrás deja **una** nota en la lista, sin aviso de nada (RF-180, RF-182).
- Adjuntar un fichero o usar la IA en una nota nueva y salir deja **una** nota, actualizada (RF-180, RF-181).
- Cerrar con la ✕ la pestaña de un borrador sin escribir muestra el aviso, y «Guardar» deja **una** nota
  (RF-183, RF-184).
- Con la nota ya escrita, seguir escribiendo la **actualiza** por autoguardado (RF-185).
- «Con qué id se escribir» y «¿hay algo por guardar?» están escritos en **una sola** pieza, y ni el editor ni la
  barra de pestañas deducen nada de `noteId == 0` (RF-186, comprobado por invariante de fuentes).
- La pieza tiene sus pruebas en verde, `:app` no suma ninguna clase roja nueva y el editor no ha crecido
  (RNF-113, RNF-116, RNF-118).

## Dudas abiertas

- **[RESUELTA] ¿Qué debe hacer el botón de atrás con una nota nueva con contenido?** **Guardar y salir, sin
  preguntar** (RF-182), que es lo que se decidió con quien Decide al aprobar esta spec. El botón de atrás **ya**
  guardaba y **ya** avisaba «nota guardada», así que quitar el diálogo es quitar la incoherencia, no quitar el
  guardado. La alternativa —que el atrás siempre pregunte y no guarde antes— queda descartada: obligaría a quitar
  el guardado actual de adjuntar ficheros y de la IA, y el aviso «¿guardar o descartar?» tiene sentido donde de
  verdad se pierde contenido, que es **cerrar la pestaña** del borrador con la ✕ de la barra (RF-183).
- **[NECESITA ACLARITACIÓN] ¿La pestaña del borrador debe convertirse en la nota real al escribir sin aviso?**
  Sería lo coherente (`promoteDraft` existe para eso) y dejaría de quedar una pestaña vacía al lado de la nota
  recién escrita, pero **recrea el editor a mitad de uso** —el cursor y el historial se pierden— y no es lo que
  pide el reporte. Queda fuera de alcance; decidirse aquí evitaría un paso extra.

## Verificación en dispositivo

Ninguna prueba pura ve el editor ni el aviso, que necesitan Robolectric para ejecutarse y aquí no corre. Con la
app instalada y la lista de notas a la vista:

1. Nota nueva → escribir dos líneas → **flecha de atrás**: aparece el aviso «nota guardada», se vuelve a la lista y
   hay **una** nota con ese texto (RF-180, RF-182).
2. Repetir con el **botón de atrás del sistema**: el mismo resultado.
3. Nota nueva → escribir → **barra de pestañas → ✕ del borrador**: aparece el aviso «¿guardar o descartar?»; con
   «Guardar» hay **una** nota; con «Descartar», ninguna; con «Cancelar», se sigue editando (RF-183, RF-184).
4. Nota nueva → **adjuntar una imagen** → seguir escribiendo → atrás: hay **una** nota, con la imagen y con el
   texto escrito después de adjuntarla (RF-180, RF-185).
5. Nota nueva → adjuntar una imagen → pulsar atrás **de inmediato** (sin esperar): una sola nota (RF-181).
6. Nota nueva → escribir → **IA** (reemplazar en el sitio o insertar) → atrás: una sola nota, con el texto de la
   IA (RF-180).
7. Abrir una nota **que ya existía**, escribir, añadir una imagen y salir: se comporta como antes de esta spec, sin
   copias.
8. Con una nota nueva ya escrita, dejarla quieta un rato y mandar la app a segundo plano: al volver, la nota está
   con lo último escrito (RF-185).

**Cerrada.** El guion se cumplió entero en dispositivo el 2026-10-17: el botón de atrás guarda y sale sin preguntar,
la ✕ del borrador sigue preguntando, y los ocho caminos terminan en **una** nota —nueva, con adjunto, con IA y con
el autoguardado—, sin duplicados ni aviso espurio. Una nota que ya existía se comporta como antes de esta spec.