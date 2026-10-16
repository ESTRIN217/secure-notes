# Spec 023 — El bloque Vídeo abre los vídeos web dentro de la app

Estado: **implementada y verificada en dispositivo** (2026-10-16), T1-T9 cerradas. La verificación (T7) necesitó **dos** pasadas: la primera destapó dos defectos ya arreglados (RF-150: la pestaña se cerraba sola; RF-151: se rotulaba `watch?v=…`) y la segunda los dio por buenos. Guion al final.

> **Nota de procedencia.** No encadena con ninguna spec anterior. Los RF más altos en uso son `RF-139` y
> `RNF-81` (spec 022), así que aquí empiezan en `RF-140` y `RNF-82`.
>
> **No añade dependencias.** La petición nombra el «Android YouTube Player» como lo que se espera ver, y ese
> reproductor **ya existe** dentro de la app: `InAppYouTubePlayer` (`:visor-media`), junto al reproductor de
> vídeo por ExoPlayer (`Media3VideoPlayer`). El problema no es que falte un reproductor, sino que el **bloque
> del editor no lo usa**: manda cualquier URL web a la app de fuera. Esta spec arregla ese enrutado.
>
> **No cambia el formato de los datos.** El `src` del bloque se sigue guardando igual; no hay migración ni
> cambio de `DataBlock`.
>
> **Dos superficies, una sola regla.** El bloque del editor (`EditableVideoBlock`) es el que hoy manda el
> vídeo web afuera; el render de solo lectura (`NoteContentBlockRenderer.renderVideoBlock`) hace lo contrario:
> manda **todo** al visor interno. Las dos pasan a usar la **misma** clasificación (RF-144, RNF-83), para que
> no quede escrito dos veces y en sentidos opuestos. Aviso de honestidad: hoy el segundo camino **no es
> alcanzable** —`NoteContentBlockCard` solo se usa en la rama `else` de `BlockEditor` y `BlockType.VIDEO` tiene
> rama propia—, así que su cambio se comprueba con un invariante, no abriendo una pantalla.

## Contexto y objetivo

Hoy el bloque Vídeo tiene **dos caminos** según su fuente:

- **Fichero local** → `onOpen()` → visor interno (`Screen.MediaViewer`), que lo reproduce y permite volver a la
  nota.
- **Cualquier URL `http(s)`** → `Intent.ACTION_VIEW`: el vídeo sale de la app y se abre en el navegador o en la
  app de YouTube, dejando la nota atrás.

El segundo camino es el problema. El visor interno **ya sabe** reproducir dos de los tres casos que llegan por
URL —YouTube y archivo de vídeo directo—, pero el bloque no le pasa ninguno: los manda todos fuera. El usuario
pierde el contexto de la nota y, si el enlace era de YouTube, sale a otra aplicación sin necesidad.

Además, la **otra** superficie que pinta un bloque de vídeo hace justo lo contrario: el render de solo lectura
manda al visor interno **hasta lo que no se puede reproducir**. La misma decisión está escrita dos veces y en
sentidos opuestos, y ninguna de las dos es la correcta.

El objetivo es que exista **una sola** clasificación de la URL —YouTube, archivo de vídeo directo, u otra URL
web— y que las dos superficies la usen: lo reproducible se ve dentro de la app, lo demás se abre fuera
**exactamente como hoy**.

## Usuarios

- Quien pega un enlace de YouTube (o *shorts*) en una nota: quiere verlo sin salir de la app y sin perder la
  nota de vista.
- Quien pega un enlace directo a un archivo de vídeo (`.mp4`, `.m3u8`…): quiere que se reproduzca dentro.
- Quien pega un enlace a una página de vídeo que la app no sabe reproducir (p. ej. Vimeo): quiere que se siga
  abriendo fuera, para no quedarse con una pantalla muerta.
- Quien ya tiene notas con bloques de vídeo: espera que **nada** de lo que ya funcionaba deje de funcionar.

## Historias de usuario

- **HU-1.** Como quien escribe una nota con un enlace de YouTube, quiero reproducirlo dentro de la app para no
  perder la nota ni ir a otra aplicación.
- **HU-2.** Como quien pega un enlace directo a un archivo de vídeo, quiero que se reproduzca dentro para no
  tener que descargarlo ni salir a un navegador.
- **HU-3.** Como quien pega un enlace web que la app no puede reproducir, quiero que se siga abriendo fuera tal
  como hoy, para no perder el acceso a ese contenido.
- **HU-4.** Como quien ya tiene notas con vídeos locales, quiero que sigan abriéndose igual tras el cambio.

## Definiciones

- **Vídeo web**: la fuente del bloque empieza por `http://` o `https://`. Es lo que hoy distingue el bloque
  entre local y web.
- **Reproducible internamente**: la URL es **de YouTube** o es un **archivo de vídeo directo** con extensión
  soportada. Son las dos cosas que el visor interno sabe reproducir.
- **Reproductor interno**: la superficie de reproducción que ya vive en `:visor-media` y que el bloque abre como
  hoy abre un vídeo local (`Screen.MediaViewer`). No es una pieza nueva.
- **Abrir fuera**: lanzar la app externa para la URL (`Intent.ACTION_VIEW`). Es el comportamiento actual del
  bloque para cualquier URL web.
- **Render de solo lectura**: la otra superficie que pinta un bloque de vídeo (`NoteContentBlockRenderer`), la
  que no es el bloque editable del editor. Comparte la clasificación con el editor (RNF-83).
- **Extensión soportada**: `mp4`, `m4v`, `mov`, `webm`, `mkv`, `m3u8`, `ts`, `flv`, `ogv` y `ogg`. Es la lista
  cerrada y es la que el reproductor interno (ExoPlayer) puede reproducir con las dependencias que ya tiene.

## Requisitos funcionales

- **RF-140**: CUANDO se pulse un bloque Vídeo cuya fuente sea una URL de YouTube, EL SISTEMA reproduce el vídeo
  en el reproductor interno, **sin** lanzar la app externa.
- **RF-141**: CUANDO se pulse un bloque Vídeo cuya fuente sea una URL `http(s)` que apunte a un archivo de
  vídeo con extensión soportada, EL SISTEMA reproduce el vídeo en el reproductor interno.
- **RF-142**: SI la fuente del bloque es `http(s)` y **no** es de YouTube ni un archivo de vídeo con extensión
  soportada, ENTONCES EL SISTEMA abre la URL fuera, como hoy.
- **RF-143**: SI la fuente del bloque **no** es `http(s)` (ruta local), ENTONCES EL SISTEMA mantiene el
  comportamiento actual (reproductor interno), sin cambios.
- **RF-144**: EL SISTEMA decide, con una regla **pura**, si la fuente se abre dentro o fuera. La regla distingue
  los tres casos —YouTube, archivo de vídeo directo y cualquier otra URL web— y **no** abre, descarga ni
  consulta la red.
- **RF-145**: EL SISTEMA reconoce la extensión de la URL **sin distinguir mayúsculas** y **aunque la URL lleve
  parámetros de consulta o fragmento** después (por ejemplo `pelicula.MP4?token=abc#t=10`).
- **RF-146**: MIENTRAS el vídeo elegido se reproduzca en el reproductor interno, EL SISTEMA permite volver a la
  nota con el mismo gesto que un vídeo local, sin cerrar la app.
- **RF-147**: SI el vídeo se reconoce como reproducible internamente pero no se puede reproducir, ENTONCES EL
  SISTEMA muestra el aviso de error del reproductor interno, que incluye una acción para abrirlo **fuera**. Es
  el comportamiento que ya existe en el visor; esta spec no lo redefine, lo conserva como red de seguridad.
- **RF-148**: CUANDO el bloque muestre la fuente como reproducible internamente, EL SISTEMA seguirá mostrando la
  miniatura de YouTube si es de YouTube, y el marcador de vídeo actual en los demás casos.
- **RF-149**: CUANDO el render de solo lectura pinte un bloque de vídeo, EL SISTEMA aplica la **misma**
  clasificación que el bloque editable: lo reproducible internamente al reproductor interno, y el resto de URLs
  web fuera. Hoy ese camino manda **todo** al reproductor interno, así que este requisito invierte su
  comportamiento actual.
- **RF-150**: MIENTRAS una pestaña de medio tenga una fuente web —`http(s)`, o un enlace de YouTube sin esquema—,
  EL SISTEMA **no** la cierra por no encontrarla como fichero local. La pestaña se abre, se ve el vídeo y sigue
  abierta al volver a la nota.
- **RF-151**: CUANDO la pestaña de un vídeo web se rotule, EL SISTEMA usa el **identificador** del vídeo si es de
  YouTube —así las cuatro formas del enlace (`watch?v=`, `youtu.be/`, `/shorts/`, `/embed/`) dan el **mismo**
  rótulo— y, si no lo es, el último tramo de la **ruta** con la consulta y el fragmento fuera (`v.mp4?token=…` →
  `v.mp4`). Cuando la URL no tiene ruta útil, el rótulo es el host. Si la fuente no es una URL web, la regla no
  cambia: sigue siendo el nombre del archivo.

## Requisitos no funcionales

- **RNF-82**: Cero dependencias nuevas. Ningún `build.gradle.kts` ni `oss-licenses.json` cambia. En particular,
  **no** se añade la librería de terceros «Android YouTube Player»: el reproductor interno ya existe.
- **RNF-83**: La clasificación de URLs vive en una pieza **pura** (sin `android`), con sus casos de prueba, y
  `:app` y `:visor-media` deciden igual porque usan **la misma** pieza.
- **RNF-84**: En el camino interno **no** se invoca `Intent.ACTION_VIEW` para la URL del vídeo. Verificable por
  invariante de fuentes.
- **RNF-85**: Cero textos visibles nuevos. Si hiciera falta alguno, iría a los **nueve** `strings.xml` a la vez.
- **RNF-86**: El texto visible del bloque no cambia: ni la leyenda, ni la alineación, ni el menú de opciones, ni
  el aviso de que no se puede abrir una URL.
- **RNF-87**: `./gradlew test` no añade ninguna clase roja nueva; los fallos de plataforma y los preexistentes
  siguen siendo **los mismos**.
- **RNF-88**: Sin cambio de comportamiento en la lectura de datos: un bloque guardado hoy se interpreta igual
  mañana.
- **RNF-89**: Decidir si una pestaña de medio sigue viva **no** hace E/S de red: una fuente web se da por viva
  sin consultarla. La comprobación corre en cada cambio de pestañas o de notas, y una petición por cambio sería
  una fuga de datos del usuario (constitution 5) además de un tirón de interfaz.
- **RNF-90**: La poda sigue cerrando las pestañas de medio **locales** cuyo archivo ya no está. Es el
  comportamiento que ya existe y esta spec no lo cambia; solo añade el caso web que no existía.
- **RNF-91**: El rótulo de la pestaña se calcula con una función **pura** y **sin red**: sale de la propia
  dirección, no de una petición de metadatos. Y **cero textos nuevos** (RNF-85): el rótulo no es una leyenda
  traducida, es un dato de la URL del usuario.

## Casos límite

- **YouTube con y sin esquema**: `https://youtu.be/ID`, `youtu.be/ID`, `youtube.com/watch?v=ID`,
  `youtube.com/shorts/ID`, `youtube.com/embed/ID`, `music.youtube.com/watch?v=ID`. Todos van al reproductor
  interno.
- **Extensión en mayúsculas**: `.MP4`, `.Mp4` → interna.
- **URL con consulta o fragmento**: `…/v.MP4?token=abc`, `…/v.m3u8#t=30` → interna.
- **URL sin extensión**: `https://vimeo.com/123456`, `https://ejemplo.com/ver/video` → fuera.
- **Extensión desconocida**: `https://ejemplo.com/pelicula.avi` → fuera (`.avi` no está en la lista).
- **Extensión en el nombre pero no al final de la ruta**: `https://ejemplo.com/mp4/listado` → fuera.
- **Fuente vacía**: se sigue viendo el marcador «reemplazar», como hoy.
- **URL de YouTube de un vídeo privado o borrado**: el reproductor interno mostrará el error de YouTube; no se
  añade ninguna detección por adelantado.
- **`content://` o `file://`**: no son `http(s)`; se tratan como hoy (interna).
- **Pestaña de medio con fuente web**: `https://…/v.mp4` o `https://www.youtube.com/watch?v=…` **no** se cierra
  por la poda de ficheros. Antes de esta spec esa pestaña no existía —el bloque mandaba la URL fuera— y la poda
  solo había visto rutas locales.
- **Pestaña de medio con fichero local borrado**: se sigue cerrando (RNF-90). El caso nuevo **no** relaja la
  regla; le da un camino aparte a lo que no es un fichero.
- **Rótulo de la pestaña** (RF-151): `…/watch?v=ID` → `ID`; `…/watch?v=ID&list=PL…&t=30` → `ID`;
  `youtu.be/ID` con y sin esquema → `ID`; `…/videos/v.mp4?token=abc` → `v.mp4`; `…/stream.m3u8#t=10` →
  `stream.m3u8`; `https://ejemplo.com/` → `ejemplo.com`; `/local/v.mp4` → sin cambio (`v.mp4`, la regla de
  siempre).

## Fuera de alcance

- **Reproducir páginas web de vídeo que no sean de YouTube** (Vimeo, Dailymotion, TikTok…). Requeriría un
  reproductor web embebido o un extractor, y es otro problema.
- **Añadir la librería de terceros «Android YouTube Player»** u otra equivalente.
- **Miniaturas de vídeo web directo** en el bloque (hoy solo YouTube tiene miniatura). Tampoco se añaden a la
  superficie de solo lectura.
- **Descargar**, guardar o exportar el vídeo web.
- **Cambiar el formato de datos del bloque** o migrar notas existentes.
- **Adjuntos del chat, tipos `TEXT`/títulos y enlaces de texto**: esta spec es solo el bloque Vídeo.
- **Resucitar el render de solo lectura de vídeo**: no se le busca uso. Se le aplica la regla para que, si
  alguien lo alcanza mañana, decida como el editor.

## Criterios de finalización

- Pulsar un bloque con URL de YouTube lo reproduce dentro de la app.
- Pulsar un bloque con URL directa a un archivo `.mp4` (y `.m3u8`) lo reproduce dentro.
- Pulsar un bloque con una URL web no reproducible la sigue abriendo fuera.
- Un bloque con vídeo local se comporta igual que antes.
- La clasificación tiene sus pruebas puras en verde y `:app` no suma ninguna clase roja nueva.
- Las **dos** superficies (bloque editable y render de solo lectura) usan la misma clasificación; comprobado por
  invariante de fuentes, ya que la segunda no es alcanzable desde ninguna pantalla.
- La pestaña de un vídeo web **sigue abierta** tras la poda, y la de un fichero local borrado se sigue cerrando
  (RF-150, RNF-89, RNF-90), con prueba pura en `OpenNoteTabsTest`.
- La pestaña de un vídeo web se rotula con el identificador (YouTube) o con el nombre de la ruta sin consulta ni
  fragmento (RF-151, RNF-91), con prueba pura en `:visor-media`.
- `docs/EDITOR.md` deja de decir que el bloque «se abre externamente» y explica qué se abre dentro y qué no.

## Dudas abiertas

- Las decisiones de diseño —reutilizar el visor interno, límite «YouTube + archivo directo», y alcance
  «bloque editable **y** render de solo lectura, con una sola regla»— se cerraron con quien Decide antes de
  redactar.
- **[RESUELTA] El rótulo de la pestaña de un vídeo web.** El título de una pestaña de medio salía del último
  tramo de la ruta (`tabTitle`), así que un `.mp4` se leía `v.mp4`, pero la forma más común de enlace de
  YouTube —`https://www.youtube.com/watch?v=…`— se leía **`watch?v=dQw4w9WgXcQ`** en la barra de pestañas. No
  era un defecto **preexistente**: antes de esta spec ninguna pestaña de medio apuntaba a una URL web, porque el
  bloque mandaba la URL fuera, así que el rótulo feo **lo estrena este cambio**. Quien Decide eligió **arreglarlo
  dentro de esta spec**: es **RF-151** (tarea T9), con su prueba pura y sin textos nuevos (RNF-91).

## Verificación en dispositivo

Lo que ninguna prueba pura comprueba es **dónde** se abre el vídeo. El vídeo abre en una **pestaña** dentro del
editor (así funciona ya el vídeo local), y la pestaña tiene que **seguir abierta**. Con la app instalada y una
nota que tenga los cuatro bloques, en este orden:

1. Bloque con `https://www.youtube.com/watch?v=…`: al pulsarlo, **se reproduce dentro** y suena.
2. Bloque con `https://…/algo.mp4` (o `.m3u8`): al pulsarlo, **se reproduce dentro**.
3. Bloque con `https://vimeo.com/…` (o cualquier URL sin extensión de vídeo): al pulsarlo, **sale fuera** a la
   app correspondiente, como antes de esta spec.
4. Bloque con un `.mp4` **de la galería** (ruta local): igual que antes de esta spec.
5. En los casos 1 y 2: la pestaña **sigue abierta** después de unos segundos (no se cierra sola), se puede
   cambiar a la pestaña de la nota y volver a la del vídeo, y el botón de atrás vuelve a la nota **sin cerrar la
   app** ni perder lo escrito. El rótulo de la pestaña de YouTube es el **identificador** del vídeo (por ejemplo
   `dQw4w9WgXcQ`), no `watch?v=…` (RF-151).
6. En el caso 3, el aviso `Cannot open URL` sigue saliendo si no hay ninguna app que atienda el enlace.

## Defecto encontrado en la verificación (2026-10-16)

**Al pulsar un vídeo web, la pestaña se abría y se cerraba sola.** No era una regresión de la decisión —abrir
dentro— sino de su convivencia con la **poda de pestañas**:

1. El bloque abre el vídeo en una pestaña de medio (`OpenTab.Media`), igual que un vídeo local.
2. `PruneMissingTabs` corre en cada cambio de pestañas o de notas y llama a `pruneMissing` con
   «¿existe el archivo?».
3. `tabFileExists("https://…")` no es `content://`, así que preguntaba `File("https://…").exists()`: **`false`**.
4. La pestaña se daba por muerta y se cerraba; si era la única, además se volvía a la lista.

La regla de poda se escribió **cuando una pestaña de medio solo podía apuntar a un fichero local** —el bloque
mandaba toda URL web a la app de fuera—, así que el caso web no estaba previsto. Se arregla con **RF-150**: una
fuente web se da por viva sin comprobarla (`RNF-89`), y la poda de ficheros locales sigue igual (`RNF-90`). La
prueba de regresión es pura (`OpenNoteTabsTest`), que es la única clase que se ejecuta en esta plataforma.

**Cerrada con la segunda pasada.** Con los dos arreglos, el guion de seis pasos se cumplió entero: los cuatro
tipos de bloque abren donde deben, la pestaña del vídeo web **se mantiene**, se cambia a la de la nota y se
vuelve, y atrás vuelve a la nota sin cerrar la app.

**Segundo hallazgo de la misma pasada, cosmético: el rótulo.** Con la pestaña ya abierta se vio que el título
salía del último tramo de la ruta, así que `…/watch?v=ID` se rotulaba `watch?v=ID`. Tampoco era preexistente
—nunca antes hubo una pestaña de medio con una URL web—, así que se resolvió aquí: **RF-151** (tarea T9), con
prueba pura en `:visor-media` y sin textos nuevos (`RNF-91`).

**Lección, para el plan de las siguientes:** abrir algo en una **pestaña** no es solo decir «pinta esto dentro»;
hay que mirar **quién más mira esa pestaña** —la poda, el rótulo, el guardado—, porque todas esas reglas se
escribieron cuando «pestaña de medio» significaba «fichero local». Ninguna prueba pura veía el fallo porque el
caso no existía; las dos reglas que faltaban sí son puras y ya tienen su prueba.

