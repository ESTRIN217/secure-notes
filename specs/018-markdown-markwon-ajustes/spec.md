# Spec 018 — Un solo motor de Markdown (Markwon) fuera del chat

Estado: **implementada** (2026-10-15), T1-T10 cerradas; **sin verificación visual**

> **Nota de procedencia.** Encadena con la 017, que construyó el motor (`ui/markdown/`) para el chat y dejó
> explícitamente fuera de alcance estas tres pantallas («*Las pantallas de changelog y de actualización.
> Siguen con su librería actual. Este cambio es del chat.*», spec 017). Esta spec es ese «cambio del chat»
> Aplicado a las otras tres, y recoge la pregunta que quedó abierta: si el motor sirve para cuatro
> superficies, la segunda librería **sobra**. `RF-104`…`RF-108` y `RNF-53`…`RNF-56` son nuevos; los más altos
> en uso son `RF-103` y `RNF-52` (spec 017).

## Contexto y objetivo

Hay **tres superficies fuera del chat** cuyo contenido **es Markdown** y que hoy se pintan con **dos motores
distintos**, ninguno de ellos el de la 017:

| Superficie | Hoy | Contenido |
|---|---|---|
| Información legal y privacidad (`LegalInfoScreen`) | `RichTextParser.parse()` | Los cinco textos legales, que son Markdown: `**negrita**`, listas `1.` y `•`, párrafos separados por línea en blanco |
| Novedades de una actualización (`UpdateInfoScreen`) | `compose-markdown` | Notas de la release, escritas en GitHub |
| Hoja de novedades (`ChangelogSheet`) | `compose-markdown` | Notas de la release, escritas en GitHub |

Los dos motores tienen defectos concretos, verificados contra el bytecode de `compose-markdown` 0.7.2 y no de
memoria:

- **`RichTextParser` es el analizador del markup heredado de las notas**, no un motor de GFM. No hay tablas, no
  hay listas de tarea, no hay cercadas: en el texto legal un `1. Cláusula` no es una lista, es un párrafo que
  empieza por «1.».
- **`compose-markdown` registra `ImagesPlugin` con su propio `ImageLoader` de Coil**, construido en cada
  llamada a `MarkdownRender.create`. Una `![img](url)` en las notas de una release **sale a la red**. Además
  envuelve su vista en una `key(markdown.hashCode())`, o sea que **destruye y recrea la vista en cada cambio
  de texto**, y construye el motor con `builderNoCore`, con un núcleo de Markdown propio que pinta distinto
  del de Markwon.

El motor de la 017 ya está **dentro del APK**, pinta GFM de verdad, **no** carga imágenes, **no** recrea la
vista al cambiar el texto y usa los colores del tema de la app. El objetivo es que **las cuatro superficies de
Markdown pinten con ese mismo motor** y que la segunda librería **desaparezca del proyecto**.

Un punto que sale de la propia spec 017 y que se arregla de paso: en el chat, `markdownForMessage` se está
ejecutando **dos veces** por mensaje —una en la burbuja, dentro de un `remember`, y otra dentro del composable,
**sin** `remember`, o sea en cada recomposición. Es trabajo en el hilo principal que no aporta nada.

## Usuarios

- Quien lee los términos o la política de privacidad **dentro de la app** y quiere que una lista numerada se
  lea como lista, y que la dirección de GitHub del aviso de seguridad se pueda **abrir** y no solo copiar a
  mano. Hoy es texto plano: se ve, pero no se pulsa.
- Quien consulta las novedades de una versión y las lee como las lee en GitHub.
- Quien mantiene la app y ve una dependencia que ya no la usa nada.

## Definiciones

- **GFM** (*GitHub Flavored Markdown*): el dialecto de Markdown que usa GitHub.
- **Motor**: la instancia de Markwon construida por la 017, con los plugins de tabla, tachado, listas de tarea,
  HTML, Linkify de solo URLs web y salto de línea, y con los colores del tema de la app.
- **Adaptación**: la traducción del markup heredado (`<url=…>`, `<item>`, `<color=…>`…) a Markdown que hace la
  017 antes de entregar el texto al motor.
- **Markdown literal**: un texto que ya es Markdown y al que **no** hay que adaptarle nada.

## Requisitos funcionales

- **RF-104**: CUANDO se pinte el contenido de una sección de la pantalla legal, EL SISTEMA lo pinte con el
  **motor**: negrita, listas numeradas y listas de puntos, párrafos separados y citas se ven como en el
  dialecto, y **no** hay ninguna etiqueta ni asterisco a la vista. Verificable: una prueba que fija que los
  cinco textos legales, **en los nueve idiomas**, llevan al menos una construcción de Markdown y **ninguna**
  etiqueta del markup heredado —es lo que los hace pertenencer a este lado y no al otro—, y otra que comprueba
  que la pantalla legal los pinta con el motor y no con el analizador de texto enriquecido.

- **RF-105**: CUANDO se pinte el texto de una release —en la pantalla de actualización y en la hoja de
  novedades—, EL SISTEMA lo pinte con el **motor**. Verificable: una prueba fija que ninguna de las dos
  pantallas usa ya la librería de Markdown y que las dos entregan su texto al motor **literal**, sin adaptación.

- **RF-106**: CUANDO haya un enlace en cualquiera de las tres superficies —escrito con sintaxis Markdown
  `[texto](direccion)` o como dirección en texto plano—, EL SISTEMA lo pinta como enlace **tocable** y, al
  pulsarlo, abre **esa dirección y no la que la rodeaba**: la puntuación de la frase en la que estaba metida no
  viaja con ella. SI la dirección **no** es web —un número de teléfono, un correo, o un esquema que no sea web—,
  ENTONCES el sistema **no** la pinta como enlace ni abre nada. SI no hay ninguna aplicación que atienda la
  dirección, ENTONCES el sistema lo anota en el registro y **sigue**: no se cae la pantalla. Verificable: la misma
  decisión de qué direcciones cuentan como web, ya comprobada en la 017, más una función pura que fija qué se
  quita de una dirección antes de abrirla, con el caso de los **paréntesis equilibrados** de un lado y de otro.

- **RF-107**: CUANDO se compile el proyecto, EL SISTEMA **no** incluye la librería de Markdown que se retiraba
  en esta spec: no está en el catálogo de dependencias, ni en las dependencias de la app, ni en el inventario
  de licencias de terceros, ni en los avisos legales. Verificable: una prueba que comprueba que el nombre de la
  librería **no** aparece en el catálogo, **no** en el fichero de construcción de la app, **no** en el
  inventario de licencias y **no** en los avisos legales.

- **RF-108**: CUANDO se pinte un mensaje del chat, EL SISTEMA adapte su markup heredado **una sola vez** por
  entrega, y **no** lo adapte otra vez en cada recomposición. Verificable: una prueba fija que el texto se
  adapta en **un** sitio, y una que fija que adaptar dos veces un texto ya adaptado **no** lo cambia (es lo que
  hace que el doble no se notara nunca).

## Requisitos no funcionales

- **RNF-53 — Ningún artefacto nuevo, y el APK se encoge.** El motor ya estaba en el APK. La librería que se
  retira arrastra un **segundo** núcleo de Markdown (un fork con el mismo nombre), el decodificador de GIF de
  Coil y un cargador de imágenes propio. Verificable: el árbol de dependencias de `:app` **no** tiene ningún
  artefacto nuevo, y la lista de coordenadas **pierde** las de la librería retirada.

- **RNF-54 — Pintar no hace red.** Ninguna de las tres superficies puede provocar una descarga. Es el defecto
  que se retira: hoy una imagen en las notas de una release sale a la red. Verificable: el motor es el mismo
  que el del chat, y por tanto sigue sin cargador de imágenes; la prueba que lo fijaba para el chat no cambia,
  y la de esta spec se extiende a las tres pantallas.

- **RNF-55 — Lo comprobable no necesita pantalla.** La decisión de qué direcciones se abren y de si un texto
  se adapta o se entrega tal cual son funciones puras con entradas y salidas explícitas. Verificable: las
  pruebas nuevas **no** llevan `RobolectricTestRunner`, que en esta plataforma no tiene runtime nativo.

- **RNF-56 — Lo que ya funciona sigue funcionando.** El chat no cambia de motor ni de apariencia; la vista
  previa de una nota en la lista y el cuerpo de un bloque plegable **no** cambian de analizador, porque su
  contenido es markup heredado y no Markdown; y el contenido de las notas, de las conversaciones y de los
  mensajes guardados **no** cambia: lo que se cambia es **solo cómo se ve**. Verificable: el diff de la
  pantalla del chat y de la pantalla de lista no toca el pintado de su texto, y el inventario de Markdown
  declara **tres** superficies migradas y ninguna más.

## Casos límite

1. **Una release sin nada de Markdown.** Un cuerpo vacío o de un renglón: se pinta como texto, sin cabeceras ni
   separadores de más. Hoy tampoco añade nada.
2. **Markdown crudo en las notas de una release.** Las notas se escriben en GitHub, donde el Markdown admite
   etiquetas HTML crudas (`<details>`, `<summary>`, `<p>`). Se pintan **con** el motor, que las entiende: es lo
   que ya las pintaba el motor anterior y lo que GitHub muestra. **No** se les aplica la adaptación del chat,
   porque esa adaptación está hecha para el markup heredado de las notas y **retiraría** etiquetas que aquí sí
   son legítimas —una `<hr>` de una release se vería desaparecer—.
3. **Texto legal con `&amp;`.** El texto de «Random Salt &amp; IV» lleva la entidad HTML escapada porque el
   fichero de recursos no admite un `&` a secas. El motor la resuelve y se ve «&». El analizador también.
4. **Viñeta `•` escrita a mano.** Cuatro de los cinco textos legales usan el carácter `•` literal, no la
   sintaxis `- `. Se ve como una viñeta hoy y se ve como una viñeta después: **no** es una lista de Markdown y
   esta spec no reescribe los textos legales.

5. **El único enlace del texto legal va metido en paréntesis.** En los nueve idiomas sale como
   `(<dirección>)`. Es el caso que obliga a la parte de dirección de RF-106: si el detector de enlaces arrastra
   el paréntesis de cierre, al pulsarlo se abriría una dirección que no existe. Un paréntesis **equilibrado**,
   en cambio, **es** parte de la dirección —`https://ejemplo.com/a_(b)` es una dirección válida— y no se quita.
6. **Texto legal muy largo.** La pantalla legal abre cinco secciones plegables y solo una se ve a la vez. Solo
   se pinta la que está desplegada, porque las demás están dentro de un `AnimatedVisibility` cerrado.
7. **Enlace con esquema raro en el texto legal.** `javascript:`, `intent:`, `file:` no abren nada y el toque no
   se da por consumido. Igual que en el chat.
8. **Un toque que no cae sobre texto.** No hay disposición todavía, o el dedo cayó fuera: no ocurre nada y no se
   registra ningún fallo, porque no es un fallo.
9. **Cambio de tema mientras está abierta la pantalla.** El motor se reconstruye con el esquema de color nuevo
   —la clave del `remember` es el esquema— y el texto se vuelve a entregar. Es el mismo comportamiento que ya
   tiene el chat.
10. **`compose-markdown` vuelve a hacer falta.** Si alguna vez otra pantalla la necesita, la prueba de RF-107 se
    pone roja y lo dice antes de que alguien la declare «porque sí».

## Fuera de alcance

- **La vista previa de una nota en la lista y el cuerpo de un bloque plegable.** Siguen con
  `RichTextParser`. Su contenido es **markup heredado** —`<color>`, `<bg>`, `<size>`, `<item>`— que el motor no
  sabe pintar, y que se vería peor: una etiqueta cruda a la vista es peor que perder el color. La vista previa
  además se recorta a 3-4 líneas con elipsis, y va dentro de una lista perezosa.
- **La réplica en curso del chat.** Sigue con el analizador, por la decisión 22 de la 017: el texto a medio
  escribir cambiaría de motor a mitad, y una tabla sin línea separadora se vería como texto y un renglón
  después como tabla.
- **Reescribir los textos legales** para que usen `- ` en lugar de `•`. Se verían igual; solo cambiaría el
  fichero de recursos de nueve locales.
- **Resaltado de sintaxis en los bloques de código** de una release. El motor que se retira solo pintaba nodos
  delimitados por `==`, que nada produce: **no se pierde nada** que se estuviera viendo.
- **Las casillas de las listas de tarea no se pulsan**, igual que en el chat: hacerlas interactivas exige
  decidir quién guarda el estado.
- **La `key` de la lista de la hoja de novedades.** La lista de releases va hoy sin clave ni `contentType`.
  Es una mejora real y ajena a esta spec: el texto se entrega entero al motor en cada actualización, así que
  no hay ningún fallo de reutilización que corregir aquí.
- **`latex-renderer`.** No tiene nada que ver: es el render de ecuaciones del editor y no pasa por ninguno de
  los dos motores.

## Criterios de finalización

- [x] Pruebas nuevas en verde **sin** `RobolectricTestRunner` (RNF-55) y sin fallos nuevos en la suite.
- [x] El árbol de dependencias de `:app` no tiene **ningún** artefacto nuevo y **pierde** los de la librería
      retirada (RNF-53).
- [x] `commonmark-java` sigue en `0.13.0` y Markwon en `4.6.2` (RNF-53), comprobado **leyendo el fichero**.
- [x] Ninguna de las tres superficies tiene cargador de imágenes (RNF-54).
- [x] La dirección que se abre es la dirección, sin la puntuación de la frase (RF-106), y el caso de los
      paréntesis equilibrados está fijado por una prueba.
- [x] La librería retirada no aparece en el catálogo, en el fichero de construcción, en el inventario de
      licencias ni en los avisos legales (RF-107).
- [x] El diff de la pantalla del chat y de la pantalla de lista no toca el pintado de su texto (RNF-56).
- [x] `:app:assembleDebug` sin errores.
- [x] `MEMORY.md` actualizado con las decisiones y con el hecho de que esto no se ha visto en un dispositivo.

### Medido, no supuesto

- **Línea base 656 / 41 · con estos cambios 689 / 41.** **+33 pruebas y +0 fallos**, y el conjunto de fallos se
  comparó **clase por clase**, no solo en cantidad: las diez clases que fallaban fallan lo mismo. `:editor-de-codigo`
  queda en **145 / 2**, sin cambios.
- **Árbol de dependencias: 650 → 630 coordenadas.** `comm` en las dos direcciones: **20 perdidas, 0 ganadas**.
  Además de las tres previstas (la librería, su **fork** `com.github.jeziellago:Markwon` y `coil-gif`), `coil-gif`
  se arrastraba el **Coil 2.x** y un `okhttp` 4.10 que solo existían por él en el árbol de resolución.
- **El APK encoge:** 162.600.361 → 162.117.759 bytes. Verificado **por dentro**, no por el tamaño: no hay ninguna
  clase `jeziellago` en los 29 `dex`.
- **Dos defectos de `direccionParaAbrir` cazados por sus propias pruebas**, ninguno de los dos se habría visto en
  una pantalla sin mirar el detalle: la guarda del bucle no aceptaba el `)` porque es *cierre* y no puntuación, y
  `estaPareado` daba por huérfano un paréntesis **bien pareado** y rompía `https://ejemplo.com/a_(b)`.
- **Dos desviaciones del plan, forzadas por la API:** `fontFamily` no se pasa al `TextView` porque Compose **no
  tiene API pública** para traducirlo (`fontFamilyName` es interno) y en las tres superficies ya **es** el tipo
  de letra por defecto; y `toPx` es miembro de `Density`, no una extensión suelta.

> **Lo que falta, y solo se puede cerrar en un dispositivo:** que una lista numerada del texto legal se vea como
> lista, que la negrita salga, que la dirección de GitHub **se abra sin el paréntesis**, que un `##` de una
> release se vea como cabecera, que una tabla se vea como tabla, y —**el riesgo de verdad de esta spec**— que las
> burbujas del chat se vean **igual** que antes: se toca un composable compartido con la única superficie que
> tiene gestos detrás. Lo único que no se puede comprobar aquí es si el detector de enlaces mete o no el
> paréntesis dentro del `URLSpan`, porque `Linkify` es framework de Android; por eso el recorte es una función
> pura y no una confianza.

## Dudas abiertas

Ninguna. Las tres decisiones que podían tomarse por dos caminos —qué superficies se migran, si los enlaces
del texto legal se abren, y si la librería se retira del todo— las respondió la persona usuaria antes de
redactar, y están argumentadas en `plan.md`.