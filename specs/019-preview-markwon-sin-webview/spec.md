# Spec 019 — La vista previa de Markdown del editor de código, con Markwon y sin WebView

Estado: **implementada** (2026-10-16), T1-T11 cerradas ✅ **VERIFICADA EN DISPOSITIVO por quien Decide (2026-10-16)**: «todo se ve bien».

> **Nota de procedencia.** Encadena con la 018, que retiró `compose-markdown` y dejó el motor de GFM
> (`ui/markdown/`) como **el** motor de Markdown de la app. Esta spec lo extiende a la **quinta** superficie, que
> hasta ahora era la única que no lo usaba: la vista previa del editor de código, que hoy se pinta en un
> `WebView`. `RF-110`…`RF-116` y `RNF-57`…`RNF-62` son nuevos; los más altos en uso son `RF-108` y `RNF-56` (spec
> 018).
>
> **Reescribe** cuatro RF de la spec 012 que hablaban del `WebView` como de la solución —RF-70, RF-73, RF-74 y
> RF-75— y deja **sin tocar** RF-71 y RF-72, cuyo comportamiento se conserva por otra vía.

## Contexto y objetivo

La vista previa del editor de código convierte el Markdown a un **documento HTML con CSS embebido** y lo carga en
un `WebView` sin JavaScript (`markdown/MarkdownRenderer.kt` + `ui/components/MarkdownPreview.kt`). Es la **única**
superficie de Markdown de la app que no usa el motor de GFM.

Trae tres cosas que **no** son culpa del `WebView` y que hay que decidir antes de tocar nada:

1. **El resaltado de sintaxis no se pierde.** El `css()` de `MarkdownRenderer` no tiene nada de resaltado: `pre code`
   sale en monoespaciado con fondo. El resaltado de verdad es el del **panel del editor** (`MarkdownTokenizer`), que
   no se toca. Es la objeción habitual a esto, y aquí no aplica.
2. **Las imágenes locales sí se ven hoy**, y son una RF de la 012 (RF-73): funcionan por `allowFileAccess = true`
   más una base `file://` apuntando a la carpeta del archivo. Un `TextView` con Markwon **no** pinta imágenes sin
   un cargador, así que sin cargador se pierde RF-73.
3. **Una imagen remota hoy no se calla**: `neutralizeRemoteSources` la sustituye por un recuadro con el texto
   «imagen remota bloqueada». Sin cargador también se pierde eso, y un hueco mudo es peor que un recuadro que
   explica.

El objetivo es que **no quede ningún `WebView` en el editor de código**: la vista previa se pinta con el mismo
motor que el resto de la app, con el tema del editor, con las imágenes locales y sin ninguna posibilidad de red.

## Usuarios

- Quien escribe un README y quiere ver la vista previa **sin que la app tenga que montar un motor web entero**
  para pintar texto y tablas: es más rápido, usa menos memoria y no puede filtrar nada.
- Quien tiene un README con `imagenes/diagrama.png` y hoy lo ve: **no debe dejar de verlo**.
- Quien mantiene la app y tiene una superficie que toca ficheros del disco y otra que toca la red dentro del mismo
  componente.

## Definiciones

- **Vista previa**: el panel que enseña el Markdown del archivo abierto. Aparece en los modos `PREVIEW` y `SPLIT`.
- **Ruta local**: un destino de imagen que resuelve a un fichero **dentro de la carpeta del archivo abierto** (o de
  sus subcarpetas).
- **Imagen remota**: cualquier otro destino —`http(s)://`, `data:`, `javascript:`— o una ruta que **se sale** de la
  carpeta del archivo.

## Requisitos funcionales

- **RF-110**: CUANDO haya una vista previa visible, EL SISTEMA la pinte con el **motor de GFM**, con el **tema del
  editor** —los colores de `SyntaxTheme`, como hoy (RF-72)—, y **sin** `WebView`: encabezados, negrita, listas,
  listas numeradas, listas de tarea con su casilla, citas, rayas, tablas, cercadas de código y enlaces se ven como
  en el dialecto. Verificable: un invariante que comprueba que en el módulo del editor de código **no** queda
  ninguna referencia a `WebView`, y otro que comprueba que la vista previa recibe su texto del motor.

- **RF-111**: CUANDO el Markdown tenga una imagen **local relativa** (por ejemplo `![d](imagenes/flujo.png)`), EL
  SISTEMA la **muestra** en la vista previa, resuelta contra la carpeta del archivo abierto. Verificable: una función
  pura que decide qué destinos se pueden leer, con la ruta relativa, con la ruta absoluta equivalente y con el
  `file://` completo.

- **RF-112**: CUANDO una imagen **no** sea local, EL SISTEMA **no** la carga y pinta en su sitio el **marcador**
  «imagen remota bloqueada», con el borde del color de cita: el hueco es mudo y el recuadro explica. Verificable:
  la misma función pura de RF-111 devuelve «no local» para `http://`, `https://`, `data:` y `javascript:`, y un
  invariante que comprueba que el motor **no** registra ningún cargador de imágenes capaz de salir a la red.

- **RF-113**: EL SISTEMA **no** lee ficheros **fuera** de la carpeta del archivo abierto. Una imagen que se sale con
  `../` se trata como remota y se marca. Verificable: la función pura de RF-111 rechaza `../fuera.png` y
  `imagenes/../../fuera.png` y acepta `imagenes/dentro.png`.

- **RF-114**: EL SISTEMA muestra el HTML incrustado **como texto**, sin interpretarlo ni ejecutarlo: `<script>`,
  `<iframe>` y `onerror=` no hacen nada (RF-71 de la 012, conservada). Verificable: un invariante que comprueba
  que el motor de la vista previa **no** registra el plugin de HTML, que es el que lo interpretaría; y el
  comportamiento en pantalla queda anotado como pendiente de dispositivo, porque comprobarlo necesita Android.

- **RF-115**: CUANDO el texto cambie, EL SISTEMA **no** bloquea la escritura. La vista previa se repinta después
  de un retardo corto, y un fallo al pintar **no** tumba la pantalla ni esconde el editor (RF-74 de la 012).
  Verificable: el retardo vive en el ViewModel, la pintada no, y un invariante que comprueba que la pintada no se
  hace en el hilo principal **desde el ViewModel** (que es donde se producía el HTML).

- **RF-116**: CUANDO un enlace del Markdown se pulse, EL SISTEMA lo abre fuera si es `http`, `https` o `mailto`, y
  **no abre nada** con cualquier otro esquema. Verificable: un invariante que comprueba que se usa la misma decisión
  de dirección web de la 018, y el resto se deja a las pruebas puras de esa función.

## Requisitos no funcionales

- **RNF-57 — Cero dependencias nuevas.** El motor ya está en el APK y el cargador de imágenes se escribe **sin
  librería**: decodifica con `BitmapFactory` el `file://` que le llega y nada más, así que **no existe ningún
  cargador capaz de salir a la red**, ni siquiera por accidente. Verificable: el árbol de dependencias de `:app` y
  el de `:editor-de-codigo` **no** ganan ningún artefacto, y un invariante que comprueba que el cargador **no**
  menciona ninguna clase de Coil ni de red.

- **RNF-58 — La decisión de qué imagen se puede leer es lógica pura.** Vive en un fichero sin `android`, sin
  `androidx.compose` y sin reloj, para que sus pruebas **sí corran** en esta plataforma, donde no hay runtime
  nativo de Robolectric. Verificable: `grep -c '^import android'` vale **0** y sus pruebas pasan sin
  `RobolectricTestRunner`.

- **RNF-59 — El motor vive en un módulo propio.** `:markdown`, del que dependen `:app` y `:editor-de-codigo`, con
  el mismo patrón que ya usan `:visor-pdf` y `:visor-media`. Lo que se mueve es lo **genérico** —el motor y el
  composable—; lo que es **del chat** —adaptar el markup heredado de una nota— se queda en `:app`. Verificable:
  `:app` y `:editor-de-codigo` declaran `project(":markdown")`, y el directorio `com/estrin217/markdown/` no
  menciona ni `AiChat` ni `CodeEditor`.

- **RNF-60 — Cero código muerto.** Sin `WebView` en el editor de código, el generador de HTML se queda sin
  llamador, y con él su tema, su traducción y sus pruebas. Se **borran** —constitución, regla 1: nada sin uso— y
  con ellos las tres declaraciones de `commonmark` en la librería. Verificable: `MarkdownRenderer.kt`,
  `MarkdownPreview.kt`, `PreviewThemeMapping.kt` y sus pruebas **no** existen; `:editor-de-codigo` **no** declara
  `commonmark`, y un invariante lo comprueba.

- **RNF-61 — Lo que ya funciona sigue funcionando.** El editor, los números de línea, el resaltado del panel de
  texto, la búsqueda y el reemplazo, el cambio de modo `SPLIT`/`PREVIEW` y el reparto 50/50 **no** cambian. El
  contenido del archivo **no** cambia: lo que se cambia es solo cómo se ve. Verificable: `git diff` no toca
  `MarkdownTokenizer`, `MarkdownInline` ni `EditorViewMode`, y las siete pruebas de `PreviewLayoutTest` siguen
  verdes.

- **RNF-62 — Nada de esto se ha visto en un dispositivo.** Es lo primero que hay que hacer al terminar, porque hay
  tres cosas que ninguna prueba de este repositorio puede cerrar: que la vista previa **se desplace** —un
  `TextView` dentro de un panel no se desplaza solo—, que una **imagen local** salga, y que una larga no se corte.
  **✅ Cerrado (2026-10-16)**: verificado en dispositivo por quien Decide, «todo se ve bien».

## Casos límite

1. **Imagen que no existe.** `![d](imagenes/no-esta.png)`: el destino es local, así que se **intenta** leer, y al
   fallar se marca. La marca dice que no se pudo leer, no que está bloqueada: son dos cosas distintas y hay que
   poder decir la correcta.
2. **Imagen remota dentro de un Markdown sin ninguna otra imagen.** La única imagen del documento sale marcada, y
   el resto se lee igual: una imagen bloqueada no corta el documento.
3. **`file://` completo y escrito a mano.** `![d](file:///sdcard/Download/x.png)`: es un `file://` pero **no** está
   dentro de la carpeta del archivo, así que se marca. Es el caso que justifica que la decisión sea de ruta, no de
   esquema.
4. **Sin archivo abierto.** El panel de vista previa solo existe cuando hay un archivo, así que no hay carpeta que
   ofrecer; si aun así llega un destino, se marca. Nunca se busca en la raíz del disco.
5. **Documento enorme.** Una tabla de mil celdas o un fichero de dos megas: hoy el `WebView` lo pinta y Markwon
   también, pero Markwon **parsea en el hilo principal** dentro de `setMarkdown`. Es el mismo trabajo que ya hace el
   chat con cada mensaje, así que no es caso nuevo; lo que sí cambia es que hoy el HTML **no** se pintaba en el
   hilo principal (RF-75 de la 012). Queda anotado como coste asumido, no como problema resuelto.
6. **Imagen dentro de una tabla o de una cita.** Markwon no monta la imagen si el nodo no es de bloque, así que una
   imagen en línea dentro de un párrafo se ve como texto. Hoy el `WebView` sí la montaba. Es una pérdida real y
   pequeña, y queda anotada como aceptada.
7. **Un `![` sin cerrar.** El motor no ve imagen y lo deja como texto. Igual que en el chat.
8. **El archivo se renombra o se borra con la vista previa abierta.** La ruta base cambia y las imágenes se
   vuelven a resolver contra la nueva. Si el directorio ya no existe, se marcan, y no hay excepción.
9. **Una imagen muy grande.** `BitmapFactory` la decodifica entera. No hay límite de tamaño, porque ponerlo
   introduciría el caso de «sale recortada», que es peor que «tarda un poco». Queda anotado como límite conocido.
10. **Dos vistas previas abiertas.** No puede pasar: hay un solo panel. Pero el motor se construye **una vez por
    pantalla**, igual que en el resto de superficies, para que cambiar de tema no lo reconstruya por elemento.

## Fuera de alcance

- **Resaltado de sintaxis en la vista previa.** No hay hoy (`pre code` es monoespaciado plano) y no se añade.
- **El `WebView` de `:visor-media`.** Es el otro `WebView` del repositorio y existe para reproducir vídeo de
  YouTube, que de verdad necesita un motor web. Esta spec **no** lo toca: el editor de código se queda sin ninguno.
- **Las vistas previas de nota.** Una nota se sigue pintando con el analizador de texto enriquecido; su contenido es
  markup heredado, no Markdown (decisión 24 de la 018).
- **Reescribir el `MarkdownRenderer` para otra cosa.** Se borra; no se convierte en otra cosa.
- **Sacar `MarkdownSource` (la adaptación del chat) a `:markdown`.** Sigue siendo del chat: adapta el markup
  heredado de una **nota** a una burbuja, y el editor de código no lo necesita.
- **Guardar en caché las imágenes decodificadas** más de una vez por sesión. Se debate en el plan; si no se hace,
  se dice que no se hace.

## Criterios de finalización

- [x] Pruebas nuevas en verde **sin** `RobolectricTestRunner` (RNF-58) y sin fallos nuevos en `:editor-de-codigo`.
- [x] `:editor-de-codigo` **no** menciona `WebView` en ningún fichero (RF-110).
- [x] La decisión de qué imagen se puede leer es pura y está probada, incluidos los tres casos límite de ruta
      (RF-111, RF-112, RF-113).
- [x] El cargador **no** menciona ninguna clase de Coil ni de red (RNF-57).
- [x] La vista previa apaga la interpretación del HTML incrustado en su llamada (RF-114).
- [x] `:markdown` es un módulo y lo dependen `:app` y `:editor-de-codigo` (RNF-59).
- [x] `MarkdownRenderer`, `MarkdownPreview`, `PreviewThemeMapping` y sus pruebas **no** existen, y
      `:editor-de-codigo` **no** declara `commonmark` (RNF-60).
- [x] `git diff` no toca `MarkdownTokenizer`, `MarkdownInline` ni `EditorViewMode`, y las 7 pruebas de
      `PreviewLayoutTest` siguen verdes (RNF-61).
- [x] `:app:assembleDebug` sin errores y la suite de `:app` sin fallos nuevos.
- [x] `MEMORY.md` actualizado, **incluido que nada de esto se ha visto en un dispositivo** (RNF-62).

### Medido, no supuesto

- **`:app` 668 / 41** contra la base **689 / 41**, con el conjunto de fallos comparado **clase por clase**: los
  mismos 41 y los mismos 3 ajenos. Los 22 de menos son pruebas que **se mudaron** al módulo nuevo con su código
  (16 de `direccionParaAbrir` y 6 de `isWebLink`), no pruebas perdidas.
- **`:editor-de-codigo` 136 / 2** contra **145 / 2**; `:markdown` **49 / 0**. La bajada son las 23 pruebas de
  `MarkdownRendererTest`, de un fichero que ya no existe.
- **Árbol de dependencias de `:app`: 630 → 630, cero ganadas y cero perdidas**, con `comm` en las dos direcciones.
  Es la medida que importa, porque es el árbol del APK. El de `:editor-de-codigo` gana 6 —las seis piezas de
  Markwon, que ya estaban— y **no pierde** los tres artefactos de `commonmark`, porque `markwon-core` los exige en
  `compile`: se quitaron las declaraciones.
- **El APK se mueve +2.224 bytes**, y verificado **por dentro** en los 29 `dex`: `MarkdownRenderer` sale **0**
  veces, el componente `WebView` del editor **0**, y `LocalFileImageLoader` **16**.
- **Tres cosas que el análisis cambió, y ninguna a peor:**
  1. **`markwon-image` no hace falta.** `ImagesPlugin` vive en un artefacto aparte que **no está en el APK** y
     declararlo rompía RNF-57; pero **toda** la maquinaria de imágenes (`AsyncDrawable`, `AsyncDrawableLoader`,
     `ImageSpanFactory`, `MarkwonConfiguration.Builder.asyncDrawableLoader()`) **sí** está dentro de
     `markwon-core`. El plugin son diez líneas escritas aquí.
  2. **La API pasa la ruta del archivo, no un cargador.** El tipo `AsyncDrawableLoader` no cruza el módulo, y
     necesitarlo habría obligado a exponerlo. Pasando la ruta, el panel **no nombra ninguna clase de Markwon** y
     **no puede** dar al motor un cargador de red aunque quisiera: la garantía se vuelve estructural.
  3. **Dos invariantes de la 017 y la 018 quedaron desfasados por esto**, y no por un fallo: decían «el motor no
     menciona ninguna clase de imágenes», que era verdad cuando el motor no sabía de imágenes. Se reescribieron a
     la regla real —«el módulo no tiene nada que sepa salir a la red»— y se añadió la que sí importa: **ninguna**
     superficie de `:app` pide imágenes.
- **Un invariante que lee comentarios no mide lo que dice medir.** Cuatro invariantes nuevos se pusieron rojos con
  el código **correcto**, los cuatro porque buscaban `WebView` y `ChatMarkdownText` en el texto completo y solo
  quedaban en el **KDoc** que explica que ya no se usan. El recorrido ahora quita comentarios antes de buscar.

> **Lo que falta, y solo se puede cerrar en un dispositivo, en este orden:** (1) **que la vista previa se
> desplace** —un `TextView` dentro de un panel con peso no se desplaza solo, y sin esto es inútil—; (2) que una
> **imagen local** salga; (3) que una remota **no** salga a la red y salga el recuadro; (4) que `<script>` y
> `<iframe>` se vean **como texto**, que es lo único que RF-114 no puede comprobar sin Android; (5) que el editor,
> el resaltado, la búsqueda y el cambio de modo sigan **igual**.

## Dudas abiertas

Ninguna. Las tres decisiones que podían tomarse por dos caminos —qué pasa con las imágenes locales, dónde vive el
motor y qué se hace con el generador de HTML que se queda sin uso— las respondió la persona usuaria antes de
redactar, y están argumentadas en `plan.md`.