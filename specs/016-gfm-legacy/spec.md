# Spec 016 — GFM en el markup heredado de las notas

Estado: **implementada** (2026-10-13), T1-T8 cerradas ✅ **VERIFICADA EN DISPOSITIVO por quien Decide (2026-10-16)**.

> **Nota de procedencia.** Encadena con `specs/012-markdown-gfm-editor/spec.md` (**implementada**),
> que dejó este trabajo **explícitamente fuera de alcance** en su sección «Fuera de alcance»: «**Resaltado
> GFM en el editor de notas.** `DataBlock.migrateLegacyContent` y `RichTextParser` quedan como están; el
> reconocimiento de Markdown en las notas es otra spec». Esta es esa spec. `RF-90`…`RF-97` y
> `RNF-44`…`RNF-47` son nuevos: los más altos en uso son `RF-89` (spec 014) y `RNF-43` (spec 014).

## Contexto y objetivo

El contenido de una nota es texto con **markup**: un subconjunto de etiquetas HTML propias
(`<b>`, `<color=#…>`, `<url=…>`, `<item checked="…">`, `<table>`) y un subconjunto de **Markdown**, ambos
pintados por el mismo analizador. Ese subconjunto de Markdown se reconocía hoy **a mano, construcción por
construcción**, sin ninguna de las construcciones que GitHub añadió después.

Lo que ya funciona: encabezados ATX, cercas con backticks o tildes, viñetas, listas numeradas, listas de
tarea, citas, rayas, enlaces en línea `[texto](url)`, autolinks desnudos `https://…`, negrita, cursiva,
tachado, código en línea y escapes con `\`.

Lo que **no** funciona son ocho construcciones de GFM, y tres de ellas no solo no se reconocen: **fallan de
forma visible**, que es peor que no soportarlas:

1. `<https://ejemplo.com>` **desaparece de la nota**. La angled bracket abre el reconocimiento de etiquetas
   HTML, que no conoce una URL, así que oculta el tramo entero como si fuera una etiqueta. El usuario ve
   desaparecer texto que escribió.
2. Un encabezado con `#` de cierre (`## Título ##`) conserva los `#` finales dentro del título.
3. Una tabla con tubería que llega como **texto** (una burbuja del chat con IA, la vista previa de la lista,
   el cuerpo de un bloque plegable) sale con las tuberías crudas.

Las otras cinco —encabezados setext, enlaces por referencia, viñeta `+`, código en línea con varias
backticks y énfasis dentro de una palabra— salen como texto literal, que es el fallo honesto y fácil de
reconocer.

El objetivo es que pegar Markdown de GitHub en una nota se vea **como se ve en GitHub**, sin tocar el
formato almacenado ni obligar a quien escribe a recordar el dialecto propio de la app.

## Usuarios

- Quien pega un fragmento de Markdown de GitHub en una nota: hoy ve texto desaparecer o tuberías
  desnudas; quiere que se vea igual que en el origen.
- Quien escribe notas con el dialecto propio de la app (`<b>`, `<color=#…>`): no quiere que nada de lo que
  ya funciona cambie. Es el usuario que **no** se nota, y por eso mismo es el que no puede romperse.

## Definiciones

- **Markup heredado**: el formato de texto de una nota — etiquetas HTML propias y Markdown — tal y como lo
  pinta el analizador de texto enriquecido. No es el formato de los bloques del editor: un bloque guarda su
  contenido en texto y el analizador lo pinta.
- **GFM** (*GitHub Flavored Markdown*): el dialecto de Markdown que usa GitHub.
- **Construcción**: un tramo del documento con un significado del dialecto, con su posición exacta.
- **Capa de bloques**: la que convierte el texto de una nota en bloques del editor. Es distinta del
  analizador de texto: **decide antes**, y una tabla con tubería la convierte en bloque de tabla, con lo que
  el analizador ya nunca la ve.
- **Cursiva intraword**: énfasis con el asterisco o el guion bajo **dentro de una palabra**, como en `a*b*c` o
  `ancho_maximo`. GFM lo prohíbe.
- **Raya de separación**: la línea horizontal. `---` con tres o más guiones en línea propia es raya; `---`
  **bajo** una línea de texto es el subrayado de un encabezado setext, y no es la misma cosa.

## Requisitos funcionales

- **RF-90**: CUANDO una línea de texto seguida de otra línea de solo `=` sea el contenido de una nota, EL
  SISTEMA pinta la primera como encabezado de primer nivel y oculta o engrise la segunda. CUANDO la segunda
  línea sea de solo `-`, EL SISTEMA la pinta como encabezado de segundo nivel. SI la línea de `=` o `-`
  **no** sigue a una línea de texto —porque está al principio del documento o porque la anterior está
  vacía— ENTONCES EL SISTEMA la trata como raya de separación, como hasta ahora. Verificable: una prueba
  fija que `Título\n===` da encabezado y que `Texto\n\n---` **sigue** dando raya.

- **RF-91**: CUANDO el documento contenga una definición de enlace por referencia de la forma
  `[etiqueta]: url`, EL SISTEMA la reconoce y la oculta o engrise como marca. CUANDO el documento use
  `[texto][etiqueta]` con esa etiqueta definida, EL SISTEMA pinta `texto` como enlace y comprueba que al
  pulsarlo se abre `url`. CUANDO use `[etiqueta]` a secas con esa etiqueta definida, EL SISTEMA lo trata
  como enlace a sí mismo. SI la etiqueta **no** está definida en el documento, ENTONCES EL SISTEMA deja
  `[texto][etiqueta]` y `[etiqueta]` **tal cual están escritos**, con sus corchetes. Verificable: una prueba
  con la definición presente y otra sin ella, que comprueban la diferencia en el texto pintado y que el
  enlace solo lleva anotación cuando la etiqueta existe.

- **RF-92**: CUANDO el documento contenga un autolink con ángulos, de la forma `<https://ejemplo.com>` o
  `<correo@ejemplo.com>`, EL SISTEMA lo pinta como enlace pulsable con esa misma dirección dentro, y oculta
  o engrise los ángulos. SI lo que hay entre ángulos **no** es una dirección, ENTONCES EL SISTEMA lo
  trata como etiqueta HTML, como hasta ahora. Verificable: una prueba fija que `<https://ejemplo.com>`
  **aparece** en el texto pintado —hoy desaparece— y que lleva anotación de enlace.

- **RF-93**: CUANDO un encabezado termine con uno o más `#` separados del texto por al menos un espacio, EL
  SISTEMA los oculta o engrise como marca, sin pintarlos como parte del título. SI los `#` están pegados al
  texto, o si no hay espacio delante, ENTONCES EL SISTEMA los deja tal cual, porque entonces son parte del
  título y no su cierre. Verificable: `## Título ##` no contiene `#` en el texto pintado y `## Título#` sí
  lo contiene.

- **RF-94**: CUANDO una línea empiece por `+ ` seguido de texto, EL SISTEMA la pinta como elemento de lista
  con viñeta, igual que hace con `- ` y con `* `. Verificable: una prueba fija que `+ Uno` y `- Uno`
  producen el mismo texto pintado.

- **RF-95**: CUANDO un tramo se abra con dos o más acentos graves y se cierre con **exactamente el mismo
  número** de acentos graves, EL SISTEMA pinta el interior como código en línea y oculta las dos tiradas. SI
  la apertura **no** tiene cierre con el mismo número de acentos graves en el documento, ENTONCES EL
  SISTEMA la deja como texto. Verificable: `` `` `a` `` `` sale como un solo fragmento de código, y una
  apertura impar sin cierre sale con sus acentos graves a la vista.

- **RF-96**: EL SISTEMA **no** aplica cursiva a un `*` o a un `_` que tenga una letra o un dígito pegado por
  el lado por el que **abriría** el énfasis. Cerrar sí puede llevar la letra pegada, que es lo que hace que
  `*cursiva*` siga siendo cursiva. Verificable: `a*b*c` y `ancho_maximo` salen sin cursiva y con su texto
  intacto, y `*cursiva*` y `**fuerte**` entre espacios siguen saliendo con ella. Es la **misma** regla que
  aplica `MarkdownInline` en el editor de código: dos superficies del mismo dialecto no pueden decidir
  distinto sobre el mismo texto.

- **RF-97**: CUANDO una tabla con tubería llegue al analizador **como texto** —en una burbuja del chat con
  IA, en la vista previa de la lista, en un bloque plegable, o mientras se escribe una tabla a medias—, EL
  SISTEMA la pinta: la fila de cabecera en negrita, la fila de separadores en gris, y las tuberías en gris.
  Una tabla se reconoce **cuando su línea separadora** es de la forma `| --- | --- |`, con al menos dos
  columnas, y la línea anterior trae al menos una tubería. CUANDO la tabla se componga en el editor de
  notas como **bloque de tabla**, EL SISTEMA no hace nada distinto: el bloque ya está construido antes de
  llegar aquí y no se toca. Verificable: una prueba que el texto pintado de una tabla lleva la fila de
  cabecera en negrita, y una prueba que una tabla convertida en bloque sale con las mismas cabeceras que
  antes.

## Requisitos no funcionales

- **RNF-44 — Cero dependencias nuevas.** Se extiende el analizador de texto enriquecido, que ya está en el
  APK. No se declara `commonmark-java` ni ninguna otra: aunque la spec 012 lo dejaron en el APK, usar un
  analizador de bloque obligaría a **reconstruir el mapeo de offsets** de fuente a texto pintado, del que
  dependen el cursor, la selección y el copiado. Verificable: `git diff` de los `build.gradle.kts` vacío, y
  el árbol de dependencias de `:app` sin artefactos nuevos.

- **RNF-45 — La lógica se puede comprobar sin pantalla.** Las decisiones nuevas —qué línea es un
  subrayado setext, qué tramo es una definición de referencia, dónde abre y dónde cierra un énfasis— viven en
  funciones puras con entradas y salidas explícitas, no dentro del bucle que construye el texto pintado.
  Verificable: esas funciones se prueban directamente, sin construir un `AnnotatedString`, y las pruebas
  **no** llevan `RobolectricTestRunner`, que en esta plataforma no tiene runtime nativo.

- **RNF-46 — El cursor no se queda dentro de una marca.** Toda marca nueva oculta —los ángulos del
  autolink, la definición de referencia, el `#` de cierre, la fila de separadores, la tubería— se registra
  como rango protegido, igual que las marcas que ya existían. Verificable: una prueba que lleva el cursor a
  cada marca nueva y comprueba que se pega al borde visible, no que queda dentro; y otra que salta hacia
  delante y hacia atrás desde dentro de la marca.

- **RNF-47 — Lo que ya funcionaba sigue funcionando.** Las 38 pruebas de `RichTextParserTest` y el resto de
  las de `:app` que tocan Markdown salen en verde **sin editarlas**, salvo las dos cuyo comportamiento esta
  spec cambia a propósito (RF-90 y RF-96), que se modifican para afirmar lo nuevo. Verificable: el diff de
  `RichTextParserTest` toca **solo** los casos de `---` bajo texto y de énfasis dentro de palabra, y el
  recuento de pruebas del fichero **no baja**.

## Casos límite

1. **`---` al principio del documento.** No hay línea de texto que subrayar, así que es raya de separación,
   como hoy. La decisión se toma con **la línea anterior**, no con la forma de la línea actual.
2. **`===` después de una línea vacía.** También es raya. Igual que en GFM, el subrayado setext necesita texto
   justo encima.
3. **`---` que la capa de bloques convierte en `<hr/>`.** Una raya sola entre líneas en blanco la convierte
   la capa de bloques antes de que llegue al analizador. Si el texto de arriba no está vacío, **no** es raya:
   es un subrayado setext, y por eso la decisión se toma en los dos sitios con la misma regla.
4. **Definición de referencia repetida.** `[r]: a` y después `[r]: b`: gana la **primera**, igual que en
   GFM.
5. **`[r]` sin definición.** Se queda literal. Es lo que distingue este analizador del tokenizador de la
   spec 012, donde `[r]` sí es enlace colapsado: son dos gramáticas distintas y unirlas daría un resultado
   que no es el de ninguna.
6. **Autolink con angled brackets y ruta larga.** `<https://ejemplo.com/a/b?c=d&e=f>`: la dirección se toma
   hasta el `>` de cierre, sin cortar en `/`, `?` ni `&`.
7. **Código en línea con acentos graves impares.** `` ```a`` `` no tiene cierre: sale texto con sus
   acentos graves.
8. **Acentos graves dentro de una cercada.** No cambian: el interior de una cercada ya no pasa por el
   reconocimiento en línea, y esta spec no lo toca.
9. **Tabla de una columna.** El mínimo de GFM son dos columnas; con una, la línea separadora no se reconoce
   y la tabla sale como texto con sus tuberías, sin perder nada.
10. **Tabla sin línea separadora.** Mientras se escribe, aún no la hay: sale como texto. Cuando aparece, se
    pinta. No se recuerda qué se pintó antes.
11. **Tabla que llega como texto con celdas que contienen emphasis.** La fila se pinta entera, así que dentro
    de una celda el emphasis **no** se reconoce. Es coherente con lo que ya pasa con los títulos, y queda
    documentado como límite conocido.
12. **`*` pegado a un dígito.** `2*3*4` **no** es énfasis: la regla mira **letras y dígitos** por igual, y
    delante hay un `2`. *(Corregido al escribir el código: en el borrador se afirmaba que sí lo era, y la
    prueba lo dejó en rojo. La regla real es la de `MarkdownInline`, de la spec 012.)*

## Fuera de alcance

- **Convertir Markdown en bloques.** Una tabla con tubería ya se convierte en bloque de tabla
  (`RF-83` de la spec 013, y el preprocesado que existía antes). Esta spec **solo pinta** las tablas que
  llegan como texto; no toca `DataBlock`, ni `migrateLegacyContent`, ni la capa de bloques.
- **Escribir Markdown.** `MarkdownConverter` y los exportadores no cambian: lo que la app reconoce al
  leer no tiene que saberse escribir al exportar.
- **Notas al modelo de IA.** `markdownForAI` sigue usando `RichTextConverter`, que ya
  traduce el markup a Markdown y a texto plano. Si el texto plano no recoge alguna construcción nueva, es
  otro spec.
- **Front matter.** `---` al principio del documento con una línea `---` de cierre es *front matter* en GFM
  de editor, no de GitHub, y en una nota no tiene nada que anunciar.
- **Footnotes, HTML embebido y otros dialectos.** No son de GFM o no aportan nada en una nota.
- **Resaltado de sintaxis en el editor de notas.** El editor de notas no colorea por sintaxis como el
  editor de código; este cambio es reconocimiento de markup, no resaltado de código.
- **Cambiar el dialecto almacenado.** Nada se reescribe en el contenido guardado: el texto es el que el
  usuario escribió.

## Criterios de finalización

- [x] `./gradlew :app:testDebugUnitTest --rerun`: **609 pruebas, 41 fallos**, y los 41 son los
      preexistentes y ajenos ya documentados en `MEMORY.md` (38 de runtime nativo de Robolectric en
      `linux/aarch64`, 2 `AssertionError` y 1 de comparación `huggingface.co`). **Comprobado contra la
      línea base**: con los cambios en una caja la suite da 529/41, así que hay **cero regresiones** y
      **80 pruebas** nuevas.
- [x] `git diff` de `app/build.gradle.kts` y `build.gradle.kts` vacío (RNF-44).
- [x] El diff de `RichTextParserTest` toca solo el caso de comportamiento que esta spec cambia a
      propósito, y el número de pruebas no baja (RNF-47): de 38 a 61, con 281 líneas añadidas y 8
      borradas, que son las de `testHorizontalRuleMarkdown`.
- [x] Sin `RobolectricTestRunner` en los ficheros de prueba nuevos (RNF-45): `GfmMarkupTest` son JUnit
      puro, 56 pruebas, y por eso **sí se ejecutan** en esta plataforma.
- [x] `:app:assembleDebug` sin errores.
- [x] `MEMORY.md` actualizado con las decisiones y con el hecho de que esto no se ha visto en un
      dispositivo. **✅ Cerrado (2026-10-16): verificado en dispositivo por quien Decide.**

> **Lo que falta, y solo se puede cerrar en un dispositivo:** que las tablas de texto se vean como
> tablas, que el setext se vea como encabezado y no como raya, que el cursor no salte de forma rara al
> cruzar los `#` de cierre, la definición `[r]:` o los ángulos del autolink, y que un enlace por
> referencia **sea pulsable** y abra la URL correcta. Las pruebas fijan el texto pintado, los estilos y
> los rangos protegidos; **no** que el cursor se mueva bien en una pantalla ni que un toque abra el
> enlace.
>
> **✅ Cerrado (2026-10-16)**: verificado en dispositivo por quien Decide, «todo se ve bien».

## Dudas abiertas

Ninguna. Las decisiones que podían tomarse por dos caminos —el subconjunto de GFM, si el cambio llegaba
también a la exportación, si se reescribía el analizador con commonmark, si `---` bajo texto dejaba de ser
raya, si se quitaba el énfasis intraword, y el número de spec— las respondió la persona usuaria antes de
redactar, y están argumentadas en `plan.md`.
