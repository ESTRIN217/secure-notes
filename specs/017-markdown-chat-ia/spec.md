# Spec 017 — Markdown (GFM) en el chat de IA

Estado: **implementada** (2026-10-14), T1-T7 cerradas ✅ **VERIFICADA EN DISPOSITIVO por quien Decide (2026-10-16)**: «todo se ve bien».

> **Nota de procedencia.** No encadena con ninguna spec anterior: es la primera que cambia el **motor de
> pintado** del chat. Las specs 006 (rediseño del chat), 012 (Markdown GFM en el editor de código) y 016
> (GFM en el markup heredado) dejaron el chat con el analizador propio; esta lo cambia. `RF-98`…`RF-103` y
> `RNF-48`…`RNF-52` son nuevos: los más altos en uso son `RF-97` y `RNF-47` (spec 016).

## Contexto y objetivo

El contenido de un mensaje del chat con IA **es Markdown**: el sistema le pide al modelo que use Markdown
cuando mejore la lectura (`AiPromptBuilder.systemPrompts`, acción `GENERATE`), y lo que llega al pintado
son encabezados, listas, tablas, cercas de código y enlaces.

Hoy eso lo pinta el analizador de texto enriquecido, que además reconoce el **markup heredado** de las
notas. Funciona, y funciona bien para Markdown simple. Lo que no hace es GFM de verdad:

- Una **tabla** llega como texto con las tuberías dentro.
- Una **casilla de lista de tarea** `- [ ]` sale como el glifo `☐` de una fuente, no como una casilla con su
  estado.
- Una **imagen** `![alt](url)` no aparece.
- Una **alerta** `> [!NOTE]` es una cita con el corchete a la vista.

El mismo texto, pegado en el editor de notas, se ve como en GitHub. En el chat, no.

El objetivo es que el Markdown que llega del modelo se vea **como se ve en GitHub**, sin tocar el formato
almacenado, sin obligar a quien escribe a recordar el dialecto propio de la app y **sin sacar del chat los
gestos que ya funcionan**.

## Usuarios

- Quien pregunta algo y recibe una respuesta con una tabla, un código o una lista de pasos: hoy tiene que
  leer tuberías y guiones; quiere lo que escribió, pintado.
- Quien ya usa el chat a diario y tiene la costumbre de **deslizar una burbuja para editarla o copiarla** y de
  **pulsarla largo para el menú**: es el usuario que no se va a quejar si nada cambia, y es el que no puede
  romperse. Este es el riesgo real de la spec, no el render.

## Definiciones

- **GFM** (*GitHub Flavored Markdown*): el dialecto de Markdown que usa GitHub.
- **Markup heredado**: el formato de texto de una nota — etiquetas HTML propias y Markdown — tal y como lo
  pinta el analizador de texto enriquecido. El chat lo recibe a veces, porque una nota con ese formato se
  puede pegar, adjuntar o pedirle al modelo que la reescriba.
- **Mensaje**: una fila del historial de una sesión de chat. No es la burbuja de la respuesta que se está
  escribiendo: esa es la **réplica en curso**.
- **Cercada**: un bloque de código delimitado por tres o más backticks o tildes en línea propia.

## Requisitos funcionales

- **RF-98**: CUANDO un mensaje del historial se pinte, EL SISTEMA lo pinta con el dialecto **GFM**: la
  respuesta a un modelo sale como respuesta en Markdown —encabezados, negrita, cursiva, tachado, listas,
  listas numeradas, listas de tarea, citas, rayas, tablas, cercas de código y enlaces— y no como un bloque de
  texto con marcas a la vista. Aplica **igual a los dos roles**: el mensaje que escribe la persona también se
  pinta con el mismo motor, de modo que un mensaje con código pegado se ve igual venga de donde venga.
  Verificable: una prueba fija que una tabla, una lista de tarea y una cerca de código **no** salen con su
  sintaxis a la vista en el texto que se entrega al pintado, y que el contenido almacenado **no** cambia.

- **RF-99**: CUANDO un mensaje contenga **markup heredado**, EL SISTEMA lo adapta antes de pintarlo y **no**
  enseña etiquetas crudas. Concretamente: una etiqueta de enlace `<url=direccion>texto</url>` se pinta como
  un enlace Markdown de texto `texto` a esa dirección; una etiqueta de lista de tarea `<item>`,
  `<item checked>` y `<item checked="false">` se pinta como casilla de lista de tarea **sin marcar**, y
  `<item checked="true">` como **marcada**; las etiquetas `<b>`, `<i>`, `<u>`, `<s>`, `<sub>`, `<sup>` y
  `<code>` **se conservan** y se pintan con su estilo; el resto —`<color=#…>`, `<bg=#…>`, `<size=…>`,
  `<font=…>`, `<normal>`, `<h1>`…`<h3>`, `<ol>`, `<ul>`, `<li>`, `<img …>`, `<video …>`, `<audio …>` y
  `<hr>`— se retiran dejando su texto. SI lo que hay entre ángulos **no** es una etiqueta —una comparación
  como `a < b`—, ENTONCES el sistema lo deja tal cual, porque no es markup. Verificable: pruebas fijas para
  cada grupo de las tres clases, y una que comprueba que `a < b` y `2 < 3` siguen intactos.
  *(Corregido al escribir el código. El borrador decía que `<item>` se retiraba, como `<color>`. Una sonda
  contra el analizador actual enseñó que hoy `<item>` y `<item checked="false">` pintan **la misma casilla
  sin marcar** (`☐ Comprar pan`), así que `<item>` se **traduce a casilla** y no a viñeta: traducirlo a `- `
  le quitaría una casilla a quien hoy la ve, y este cambio no va de eso. La sonda también enseñó que hoy el
  cierre `</item>` se ve **a la vista** al final de cada línea; al traducir, deja de verse.)*

- **RF-100**: CUANDO un mensaje tenga un enlace escrito con sintaxis Markdown `[texto](direccion)`, EL
  SISTEMA lo pinta como enlace **tocable** y, al pulsarlo, abre esa dirección en el navegador. CUANDO el
  mensaje tenga una dirección escrita en texto plano, sin sintaxis Markdown, EL SISTEMA también la pinta como
  enlace tocable. SI la dirección **no** es web —un número de teléfono, un correo, o un esquema que no sea web—
  ENTONCES el sistema **no** la pinta como enlace. SI se pulsa un enlace cuya dirección no es web ni de
  correo, ENTONCES no ocurre nada y el mensaje no se da por pulsado. Verificable: pruebas fijas de la
  decisión de qué direcciones cuentan como web, y comprobación en dispositivo de que un toque sobre el enlace
  lo abre.

- **RF-101**: CUANDO un mensaje contenga una lista de tarea, con líneas que empiezan por `- [ ]` o por
  `- [x]`, EL SISTEMA pinta una **casilla** por línea, marcada o sin marcar según el texto. EL SISTEMA
  **no** cambia el estado de una casilla al pulsarla: las casillas son de lectura. Verificable: el contenido
  almacenado de un mensaje con casillas **no** cambia al pintarlo, y en la pantalla las casillas salen con su
  estado.

- **RF-102**: MIENTRAS una burbuja se pinte, EL SISTEMA conserva los gestos que ya funcionan: la pulsación
  larga abre el menú de la burbuja y el deslizamiento horizontal edita el mensaje (si es de la persona) o lo
  copia (si es del asistente). SI el gesto empieza **exactamente sobre un enlace**, ENTONCES el sistema abre
  el enlace y no el menú: es el único sitio donde el gesto cambia, y es coherente con que un enlace se pulse
  para abrirse. Verificable: la decisión de qué se abre está en una función comprobable, y en dispositivo se
  comprueba que el menú y el deslizamiento siguen respondiendo en el resto de la burbuja.

- **RF-103**: MIENTRAS una respuesta se esté escribiendo, EL SISTEMA la pinta **sin** esperar a que termine,
  sin saltos de formato y con el mismo texto que al terminar. CUANDO la respuesta **termine**, EL SISTEMA la
  pinta con el mismo motor que el resto del historial. Verificable: la réplica en curso y el mensaje ya
  guardado entregan **las mismas construcciones** al pintarse, sin que una se convierta en la otra a mitad de
  escribir.

## Requisitos no funcionales

- **RNF-48 — Cero jars nuevos en el APK.** El motor de GFM **ya está dentro de la app**: viene como
  dependencia de la librería de Markdown que usan el changelog y la pantalla de actualización, y sus
  artefactos (`core`, `ext-tables`, `ext-strikethrough`, `ext-tasklist`, `html`, `linkify`) entran hoy como
  `runtime`. Esta spec los usa **directamente**, así que hay que declararlos de forma explícita —una
  dependencia `runtime` no llega al classpath de compilación— pero **no se descarga ni se empaqueta nada
  nuevo**. Verificable: el árbol de dependencias de `:app` no tiene ningún artefacto nuevo frente a la línea
  base, y `commonmark-java` sigue en `0.13.0`, que es la versión que el motor exige.

- **RNF-49 — Las decisiones se pueden comprobar sin pantalla.** Qué texto se entrega al pintado, qué
  direcciones cuentan como enlace y qué ocurre con el código viven en funciones puras con entradas y salidas
  explícitas, en un fichero que **no importa nada de composición ni de la plataforma**. Verificable: esas
  funciones se prueban directamente y ninguna de las pruebas nuevas lleva `RobolectricTestRunner`, que en
  esta plataforma no tiene runtime nativo.

- **RNF-50 — Pintar no hace red.** Ninguna construcción de un mensaje puede provocar una descarga: no hay
  cargador de imágenes, no hay resolución automática de direcciones de imagen y el único toque que abre algo
  es un gesto de la persona. Verificable: revisión del módulo de render: ningún plugin de imágenes
  registrado, y una prueba que fija que el texto a pintar **no** contiene ninguna dirección de imagen.

- **RNF-51 — Un fallo al abrir un enlace no se traga.** Si al abrir un enlace no hay ninguna aplicación que
  lo atienda, el sistema lo anota en el registro y **sigue**: ni se cae la pantalla ni se queda el mensaje en
  un estado raro. Verificable: el `catch` del `Intent` registra el fallo y la función devuelve control.

- **RNF-52 — Lo que ya funciona sigue funcionando.** El menú de la burbuja, el deslizamiento, el copiado, el
  reintento de un mensaje fallido, las fichas de adjuntos, el pie de estado y el historial se mueven tal cual,
  y el contenido almacenado de las conversaciones **no** cambia: lo que se cambia es solo el pintado.
  Verificable: el diff de la pantalla del chat toca el pintado del cuerpo del mensaje y **nada más**, y el
  historial de una conversación con la base de datos sigue dando el mismo contenido que antes.

## Casos límite

1. **Cercada sin cerrar.** Un mensaje a medio escribir puede llevar ``` ```kotlin ``` sin la cerca de
   cierre. Se pinta como bloque de código hasta el final del texto, que es lo que hace el dialecto. Al
   terminar la respuesta no cambia: se había pintado ya así.
2. **`<url=…>` sin cerrar.** `<url=https://ejemplo.com>` sin `</url>` **no** es un enlace: no hay texto que
   hipervincular. La etiqueta se retira **entera** —y con ella la dirección, que va dentro—, y el texto que la
   rodea se queda. *(Lo dijo una sonda contra el analizador actual, no la memoria: `Ver <url=…> la doc` sale
   `Ver  la doc`. El borrador afirmaba que la dirección se quedaba como texto, y es falso.)*
3. **`a < b`.** El `<` seguido de espacio no abre etiqueta. Si el sistema lo tratara como etiqueta, el texto
   de la comparación desaparecería de la pantalla.
4. **Etiquetas solapadas.** `<b>uno <i>dos</b> tres</i>`: cada etiqueta se cierra con su propio cierre, sin
   buscar el que le toca por orden. Es la lectura tolerante, y no hay nada mejor que hacer con un markup mal
   formado sin inventarse un parser nuevo.
5. **Mensaje enorme.** Un mensaje largo con veinte tablas es más caro de pintar que veinte líneas, porque el
   motor trabaja en el hilo principal. Es el mismo coste que ya paga el resto del chat; no se acota, pero se
   recuerda como límite conocido.
6. **Dirección con paréntesis.** `[texto](https://ejemplo.com/a_(b))`: la dirección se toma hasta el paréntesis
   de cierre del enlace, como en el dialecto. Un destino mal escrito **no** es cosa de esta spec.
7. **`javascript:`, `intent:` y `file:`.** Se pintan como enlace si vienen en sintaxis Markdown, pero al
   pulsarlos **no** abren nada: solo se abren direcciones web y de correo. Es lo que evita que una respuesta
   del modelo pueda lanzar lo que sea.
8. **Lista de tarea dentro de una lista de tarea.** Se pinta anidada. Las casillas siguen sin ser pulsables,
   y eso incluye las de dentro.
9. **Repetición del mismo enlace.** `[docs](https://ejemplo.com)` dos veces en el mismo mensaje: los dos
   son tocables y cada uno abre lo suyo. Cada occurrence tiene su propio destino; no se deduplica nada.
10. **Mensaje vacío.** Una respuesta fallida pinta su fila de error, no un cuerpo de texto vacío.

## Fuera de alcance

- **Casillas marcables.** Se pintan, no se pulsan (RF-101). Hacerlas interactivas exige decidir quién guarda
  el estado y en qué se guarda, y eso es otra spec.
- **Selección de texto en el chat.** El texto del chat **no es seleccionable hoy**: la pulsación larga abre el
  menú de la burbuja. Esta spec no lo cambia en ninguna dirección.
- **Imágenes y adjuntos en un mensaje.** Un `![alt](url)` no descarga nada (RNF-50).
- **Resaltado de sintaxis por lenguaje.** Los bloques de código salen con fondo y sin colorear tokens. La
  librería que hoy se usa en el changelog tampoco lo hace: su resaltado solo pintaba nodos delimitados por `==`,
  que nada produce, así que **no se pierde nada** que se estuviera viendo.
- **Las pantallas de changelog y de actualización.** Siguen con su librería actual. Este cambio es del chat.
- **Alertas, notas al pie y shortcodes de emoji.** Hay un preprocesador de GFM en el proyecto que las sabe
  activar, y queda **fuera**: sus etiquetas están fijadas en un idioma y no es lo que se pidió.
- **Reescribir lo que se guarda.** Nada del contenido de las conversaciones cambia; solo cambia cómo se ve.

## Criterios de finalización

- [x] Pruebas nuevas en verde **sin** `RobolectricTestRunner` (RNF-49) y sin fallos nuevos en la suite.
- [x] `commonmark-java` sigue en `0.13.0` (RNF-48), comprobado **leyendo el fichero**, no de memoria.
- [x] El árbol de dependencias de `:app` no gana ningún artefacto nuevo (RNF-48).
- [x] Ningún plugin de imágenes registrado y una prueba que fija que no hay direcciones de imagen en lo que se
      pinta (RNF-50).
- [x] El `catch` del `Intent` registra el fallo (RNF-51).
- [x] El diff de la pantalla del chat toca solo el pintado del cuerpo del mensaje (RNF-52).
- [x] `:app:assembleDebug` sin errores.
- [x] `MEMORY.md` actualizado con las decisiones y con el hecho de que esto no se ha visto en un dispositivo.

### Medido, no supuesto

- **Línea base 611 / 41 · con estos cambios 656 / 41.** La base se midió apartando los ficheros y revirtiendo
  la pantalla y el catálogo, no de memoria. **Cero regresiones** y **45 pruebas** nuevas (36 de lógica pura +
  9 de invariantes). Los 41 fallos son los preexistentes: 38 de runtime nativo de Robolectric en
  `linux/aarch64`, 2 de `OnDeviceModelSourceTest` y 1 de `HtmlExportMediaTest`.
- **Cero artefactos nuevos**, comprobado con `comm` sobre el árbol de `releaseRuntimeClasspath` de `:app` con
  y sin los cambios: 647 coordenadas en ambos casos y **vacío en las dos direcciones**.
- **Tres mutaciones** para comprobar que las pruebas pueden ponerse rojas: la guarda de la cercada anulada
  (**5 rojas**), `isWebLink` aceptando cualquier esquema (**2 rojas**), y las etiquetas de estilo que el motor
  sí sabe pintar retiradas también (**1 roja**). Al revertir las tres, todo verde.
- **Dos expectativas de esta spec resultaron falsas** y las corregió una sonda contra el analizador actual, no
  la implementación: que un `<url=…>` sin cerrado deja la dirección como texto (**no: también desaparece**,
  porque va dentro de la etiqueta) y que `<item>` se traduce a viñeta (**no: hoy es una casilla sin marcar**, y
  traducirlo a `- ` le quitaría una casilla a quien hoy la ve). Ver RF-99 y el caso límite 2.

> **Lo que falta, y solo se puede cerrar en un dispositivo:** que una tabla se vea como tabla, que una
> casilla salga marcada o sin marcar, que un enlace se abra al pulsarlo, y —lo más importante— que el menú de
> la pulsación larga y el deslizamiento sigan respondiendo. Las pruebas fijan **qué texto llega al pintado** y
> **qué direcciones cuentan como enlace**; no fijan que un toque abra lo correcto ni que el menú siga
> apareciendo.
>
> **✅ Cerrado (2026-10-16)**: verificado en dispositivo por quien Decide.

## Dudas abiertas

Ninguna. Las decisiones que podían tomarse por dos caminos —qué motor, si las casillas se pulsan, si la
réplica en curso usa el mismo pintado, si las otras dos pantallas se migran, y si se activan alertas y emojis—
las respondió la persona usuaria antes de redactar, y están argumentadas en `plan.md`.
