# Plan — Spec 016 · GFM en el markup heredado

Spec: `specs/016-gfm-legacy/spec.md`

Decidido por la persona usuaria antes de redactar: subconjunto útil de GFM, **solo render**, se extiende
el tokenizador propio, spec `016`, y los dos cambios de apariencia (setext gana sobre la raya, y el énfasis
intraword desaparece).

## 1. Archivos y responsabilidades

| Archivo | Qué hace con esto |
|---|---|
| `util/GfmMarkup.kt` **nuevo** | **Toda** la lógica nueva, pura y comprobable sin pantalla: definiciones de referencia, subrayado setext, autolink con ángulos, flanking del emphasis, cierre de encabezado, tiradas de backticks, fila separadora de tabla. Sin `import androidx.compose`, sin `Context`, sin `R`. |
| `util/RichTextParser.kt` | **Consume** `GfmMarkup` dentro del bucle que ya existe. Solo conecta llamadas: llama a las funciones puras y traduce su respuesta en `appendChar`, `skipOrGrayTagChars` o `startStyle`/`endStyle`. |
| `util/NoteEditorUtils.kt` | Un cambio de **una línea**: `preprocessMarkdownBlocks` deja de convertir en `<hr/>` un `---`/`===` que sigue a una línea con texto, porque ese caso es setext (caso límite 3, RF-90). |
| `test/java/com/example/GfmMarkupTest.kt` **nuevo** | Las funciones puras, probadas directamente. Sin `RobolectricTestRunner` (RNF-45). |
| `test/java/com/example/RichTextParserTest.kt` | Pruebas de integración por construcción: que el texto pintado sale bien, que el enlace lleva anotación, que el cursor no cae dentro de una marca. Dos casos preexistentes **cambian** a propósito (RF-90, RF-96). |

**Por qué un fichero nuevo y no más ramas dentro del analizador.** El analizador son 673 líneas con un
bucle de una pasada y 6 pilas de estado. Añadirle dentro la gramática de GFM mezclaría «qué es una tabla» con
«cómo se escribe en el buffer», y lo segundo **no se puede probar sin pantalla**. Con la gramática aparte, la
pregunta «¿esto es un subrayado setext?» tiene una respuesta que se obtiene llamando a una función con dos
cadenas, y eso corre en esta plataforma. Es la misma razón por la que la spec 014 partió el editor.

## 2. El analizador actual y por qué encaja con GFM

El bucle (`parseWithMapping`) avanza carácter a carácter con un `isLineStart` que marca el comienzo de línea,
y en ese punto consulta `parseLineStartMarkers`. Lo que ya sabe hacer, en una pasada:

```
while (i < N) {
  si es inicio de línea y hay marcado → parseLineStartMarkers(...)      // cercas, títulos, viñetas, citas, rayas
  si está dentro de una cerca → copiar literal                            // el contenido de la cerca no se reconoce
  si empieza por '\' y sigue un escapable → quitar la barra, copiar el carácter
  si empieza por '<' → HtmlTagParser.parseTag(...)                       // ⚠️ hoy se come <https://…> (RF-92)
  si no → parseMarkdownSyntax(...)                                       // negrita, cursiva, tachado, código, enlaces
  en otro caso → appendChar
}
```

**La consecuencia importante**: es un reconocedor de una pasada, sin vuelta atrás. GFM necesita **dos cosas
que este bucle no tiene**:

1. **Mirar hacia delante** (para un autolink hay que encontrar el `>` de cierre; para un emphasis hay que
   encontrar el delimitador que cierra). Hoy solo se mira un carácter. Se resuelve con `String.indexOf` /
   `regionMatches` sobre `rawText`, sin cambiar la estructura del bucle: cada rama devuelve el índice en el
   que sigue, que es lo que el bucle ya espera.
2. **Una pre-pasada** para las definiciones de referencia (`[r]: url`), porque una definición puede estar
   **después** del enlace que la usa, y un bucle de una pasada no puede mirar lo que aún no ha leído.

Por eso `parseWithMapping` gana **una línea al principio**:

```kotlin
val references = GfmMarkup.collectReferenceDefinitions(rawText)   // pre-pasada, O(n), solo si hay '['
```

El resultado es un `Map<String, String>` de etiqueta normalizada a URL. **Normalizada a minúsculas**, porque
GFM no distingue `[R]` de `[r]`. Con el mapa delante, el bucle no necesita recordar nada nuevo.

## 3. La pre-pasada de definiciones de referencia (RF-91)

```kotlin
object GfmMarkup {
    /** Etiqueta → URL. Gana la primera definición si se repite (caso límite 4). */
    fun collectReferenceDefinitions(text: String): Map<String, String>
}
```

Se ejecuta **una vez por documento**, antes del bucle, y solo si el texto contiene `'['` (un early-out que
evita recorrer dos veces el 100 % de las notas, que son HTML propio y no Markdown).

Cada línea se examina con una forma fija: hasta tres espacios de sangría, luego `[`, luego la etiqueta hasta
el `]`, luego `:`, luego la URL hasta el final de línea o hasta un `<`. Lo que se guarda es **la etiqueta
normalizada** y **el rango de la definición completa**, porque esa rango tiene que poder ocultarse o
engrisarse al pintarla.

**Por qué gana la primera y no la última.** GFM dice que gana la primera. Aquí además hay una razón de
seguridad: si una nota trajera una definición falsa al final, sobreescribir con ella una definición legítima
al principio cambiaría a dónde lleva un enlace que el usuario escribió. Gana la primera porque es el orden en
que se lee, y porque el caso raro es el que no puede surprisear.

**Por qué el rango completo y no solo los corchetes.** La definición `[r]: https://…` **entera** es una marca:
ocultarla es lo que espera quien la escribió (la URL ya está en el enlace), y a medias sería peor que nada.

## 4. Setext: el único cambio de estructura (RF-90)

Setext es un título **del texto de la línea anterior**, y el bucle ya pasó por esa línea. El trick es que
`parseLineStartMarkers` se ejecuta **al empezar** la línea de los `=`, cuando el texto de arriba ya está en
el buffer: basta con mirar **atrás** para encontrar dónde empezó esa línea.

```
Al inicio de una línea, antes de comprobar si es raya:
  1. ¿La línea actual es solo '=' o solo '-'?                      → si no, seguir como hoy
  2. ¿La línea ANTERIOR tenía texto (no estaba vacía
     y no empezaba ya por un marcador de bloque)?                   → si no, es raya, seguir como hoy
  3. Si sí: aplicar el estilo h1/h2 al tramo YA escrito de la línea anterior,
     y ocultar o engrisar la línea de '=' / '-'.
```

El punto 3 es el que da la información: **el texto ya está en el buffer**, así que se le aplica el estilo con
`builder.addStyle(...)` sobre su rango, que es lo mismo que ya hace `startStyle`/`endStyle` al cerrar un
título al final de línea. No hay que rebobinar ni guardar nada.

**Por qué hay que tocar también `preprocessMarkdownBlocks`.** Sin ese cambio, el `---` se convertiría en
`<hr/>` **antes** de llegar al analizador, en la capa de bloques, y el setext no se vería nunca en una nota.
Hoy esa función convierte en raya cualquier línea de tres o más `-`/`*`/`_`. El cambio es añadir **una
condición**: que la línea anterior no esté vacía. Es la misma regla del punto 2, escrita en el otro sitio.
Que las dos capas decidan distinto sería exactamente el fallo que la spec 012 ya pagó una vez («`---` dentro
de una tabla es fila separadora y **no** raya»: dos capas con reglas distintas dan dos respuestas).

**Por qué `===` no necesita nada en la capa de bloques.** `=` no está en el conjunto `-`, `*`, `_` que esa
función mira, así que hoy `===` **ya** pasa de largo y llega intacto al analizador. Solo el `---` cruza.

## 5. Autolink con ángulos (RF-92) — el arreglo de un fallo

Hoy `<https://ejemplo.com>` se pierde: `char == '<'` entra en `HtmlTagParser.parseTag`, que busca el `>` de
cierre, lee `https://ejemplo.com` como **nombre de etiqueta**, y cae en la rama `else` de `handleHtmlTag`,
que hace `skipOrGrayTagChars` — es decir, **oculta el tramo entero**. Con `showTagsGray` se ve en gris; con
`hideTags` desaparece sin más.

El arreglo es **una comprobación antes** de que el `<` llegue al HTML:

```kotlin
// en el bucle, justo ANTES del bloque que llama a HtmlTagParser
val autolink = GfmMarkup.angleAutolink(rawText, i)
if (autolink != null) { … }
```

Si `GfmMarkup` reconoce `<scheme:resto>` o `<correo@dominio>`, se pinta como enlace con anotación `URL` y se
ocultan los ángulos. Si **no** lo reconoce, el control cae al `if (char == '<')` de siempre y el
comportamiento con etiquetas HTML queda **intacto**. Es un cambio **aditivo**: no se toca
`HtmlTagParser` ni `handleHtmlTag`, y por eso ninguna etiqueta existente puede cambiar de aspecto.

La expresión regular es deliberadamente estrecha: `^<((?:https?://|mailto:)[^\s<>]+|[^\s<>@]+@[^\s<>@]+\.[^\s<>@]+)>`.
El segundo alternativa es el correo. Todo lo que no case cae en la etiqueta HTML de siempre.

## 6. Énfasis intraword (RF-96) — la regla de `MarkdownInline`, no una nueva

> **Corregido al escribir el código.** Este apartado describía al principio una regla propia —«si hay letra o
> dígito pegado por cualquiera de los dos lados, no aplica»— y **era falsa**: rechazaba `*cursiva*`, porque el
> `*` de cierre va pegado a la letra que lleva el emphasis. Las pruebas lo dejaron en rojo a la primera. La
> regla correcta es la de `MarkdownInline` de la spec 012, y no es casualidad: el editor de código y el de
> notas son **dos superficies del mismo dialecto**, y si decidieran distinto, el mismo texto se vería distinto
> según dónde esté escrito.

La regla tiene **dos mitades**, y por eso hay dos funciones: abrir y cerrar son cosas distintas.

```
ABRIR  (emphasisDelimiterAppliesAt):  detrás no es espacio  Y  delante no es letra ni dígito
CERRAR (emphasisDelimiterClosesAt):   delante no es espacio
```

- `a*b*c`: el primer `*` tiene `a` delante → **no abre**. El segundo tampoco → **no cierra**. Sin cursiva.
- `ancho_maximo`: los dos `_` tienen letra pegada → no abren. **Salvado**, que es justo lo que pasa en
  cualquier nota con nombres de constante.
- `*cursiva*`: el de apertura abre porque detrás hay `e`, que no es espacio, y delante hay un espacio; el de
  cierre cierra porque delante tiene `a`, que no es espacio. **Cursiva**, como hoy.
- `2*3*4`: delante hay un dígito, y la regla mira **letras y dígitos** por igual → no abre. El caso límite 12
  del borrador afirmaba lo contrario; la prueba lo desmintió y se corrigió.
- `precio*` al final del documento: no puede **abrir** porque detrás no hay nada. Abrir y cerrar no se
  contradicen: el mismo `*` pegado a una letra sí **cierra**.

**Por qué no la regla completa de flanking.** Un documento con tres condiciones sobre puntuación adyacente
resuelve casos como el énfasis envolviendo un paréntesis, que en una nota no existen. La de arriba cubre
`a*b*c` y `snake_case`, que es lo que se ve. Queda como límite conocido en la spec.

**Por qué no copiar el fichero.** `MarkdownInline` es `internal` de `:editor-de-codigo`, y `:app` no depende
de esa biblioteca. Copiar el texto habría dejado dos copias que divergen; la alternativa —mover la gramática a
un módulo compartido— es un refactor de otra spec. Se replica la **regla**, con una prueba en cada lado que
fije el mismo caso, que es lo que hace que una divergencia se vea antes de que llegue a una pantalla.

## 7. Código en línea con tiradas (RF-95)

Hoy el acento grave **alterna** el estilo de código cada vez que aparece. Con N acentos hay que encontrar el
cierre **con el mismo número**, y si no está, **no es código**.

```
backtickRun = cuenta de acentos graves consecutivos en i
cierre = buscar el siguiente acento grave en rawText a partir de i + backtickRun
  si el que hay no forma una tirada de backtickRun  →  no es código (busca el siguiente acento)
  si no hay ninguno                              →  no es código, y se deja el acento a la vista
```

Se elige el **primer** acento grave que forme una tirada del tamaño exacto, como hace GFM. Y hay un detalle:
la posición actual es importante para el mapeo — los acentos de apertura y los de cierre se ocultan con
`skipOrGrayTagChars`, que es lo que los registra como rango protegido y hace que el cursor **salte** de uno
al otro en vez de quedarse dentro.

**Por qué no cambiar `escapedChars`.** El conjunto de escapables ya incluye `` ` `` (línea 92 del analizador):
`` \` `` ya muestra un acento grave literal. Esa vía no se toca.

## 8. Cierre de encabezado y viñeta `+` (RF-93, RF-94)

Son las dos más pequeñas, y van juntas porque las dos son «un carácter más al principio o al final de una
línea que ya se está pintando».

- **`#` de cierre**: al empezar el marcador del título, el analizador ya sabe hasta dónde llega la línea. Se
  mira el final de la línea: si termina en `#+` **con al menos un espacio delante**, ese tramo se oculta o
  engrise con `skipOrGrayTagChars`. Si está pegado (`Título#`), no se toca — es parte del título (RF-93).
- **`+ ` como viñeta**: es **una entrada más** en la lista `lineMarkers` que ya existe para `- ` y `* `. La
  lista ya tiene el campo `replacement` para el glifo, así que la entrada nueva es una línea.

**Por qué no se extiende la lista a `1) ` o `a) `.** Son listas ordenadas alternas que GFM acepta y que nadie
pega en una nota. Añadirlas ensancha la lista sin servir de nada.

## 9. Tabla que llega como texto (RF-97)

Una tabla con tubería se convierte en **bloque de tabla** en la capa de bloques, y el analizador nunca la ve
en una nota normal. Se ve en: la burbuja del chat con IA (el modelo escribe tablas), la vista previa de la
lista, el cuerpo de un bloque plegable, y **mientras se escribe una tabla a medias** (la línea separadora aún
no está, o sí y aún no se ha guardado).

La regla de reconocimiento **no es «una línea con tuberías»** sino la de GFM: hay una **línea separadora**
(`| --- | --- |`, `| :-- | --: |`, etc.) y la línea anterior trae al menos una tubería. Sin separadora no hay
tabla.

```
¿Línea actual separadora?   = solo '|', '-', ':', espacios y tabuladores, ≥ 2 columnas
¿Línea anterior con tubería?
  → sí: estilo de separadora en gris para esta línea,
         y estilo de cabecera en negrita para la anterior
  → no: texto normal
```

El resto de filas (las de datos) se pintan tal cual: la idea es que la tabla **se vea** como tabla sin
convertirla en bloque, y las tuberías en gris ya la delatan. **`\|` escapado** sale como `|` (caso límite de
la spec 012): el escape con barra ya está en el conjunto de escapables del analizador.

**Por qué se exige `|` inicial.** La capa de bloques (`preprocessMarkdownBlocks`, línea 146) solo convierte
tablas que **empiezan** por `|`. Si el analizador aceptara tablas sin tubería inicial, las dos capas
decidirían distinto sobre el mismo texto: una se convertiría en bloque y la otra se pintaría. Que las dos
acepten lo mismo evita tener que preguntar «¿en qué capa estoy?».

**Por qué no se pinta la alineación.** `:---:` y `---:` se leen bien aunque se observe el separador entero,
así que la alineación no necesita ir más lejos. Ponerla exigiría mirar el interior de una fila que en el
analizador es texto plano, y no compensa.
## 10. Estrategia de tests

Todo nuevo nace en **rojo** antes de existir el código (constitución 4).

| Fichero | Qué prueba | Corre en esta plataforma |
|---|---|---|
| `GfmMarkupTest` (nuevo) | Las funciones puras: definiciones (con duplicado, sin definir, minúsculas), setext (con y sin texto encima), autolink (http, correo, y el `<no>` que **no** es autolink), flanking (`a*b*c`, `ancho_maximo`, `2*3*4`, `*sí*`), tiradas de backticks (par, impar sin cierre), separadora de tabla (2 columnas, 1 columna, sin `|` inicial) | **Sí**, JUnit puro |
| `RichTextParserTest` (añadir) | La pintura: `## Título ##` sin `#` en el texto, `<https://…>` presente y con anotación, `[t][r]` con y sin definición, `+ Uno` como `• Uno`, setext con estilo h2, tabla de texto con cabecera en negrita | **Sí**, JUnit puro |
| `RichTextParserTest` (añadir, RNF-46) | Cursor: `snapOffset` pega al borde visible en cada marca nueva; `previousVisibleOffset`/`nextVisibleOffset` desde dentro de la marca | **Sí**, JUnit puro |
| `RichTextParserTest` (cambiar 2) | `testHorizontalRuleMarkdown` pasa a afirmar que `---` bajo texto es encabezado; el caso de `***` sigue siendo raya. Un caso de emphasis pasa a afirmar que `a*b*c` no sale cursiva | **Sí** |

**Ninguna prueba nueva lleva `RobolectricTestRunner`**: en esta plataforma no hay runtime nativo para
`linux/aarch64` y esas pruebas no se ejecutan (está documentado en `MEMORY.md`). Todo lo nuevo es JUnit puro,
así que **sí corre aquí** (RNF-45).

**Mutación obligatoria** (tarea T6). Se rompe cada rama a propósito y se comprueba que **su** prueba se pone
roja. Sin esto, una prueba que no se ha visto fallar no está probada, y ya ha pasado dos veces en este
proyecto.

## 11. Orden de implementación

| Tarea | Qué | Depende de |
|---|---|---|
| T1 | `GfmMarkup.kt`: definiciones de referencia + setext | — |
| T2 | `GfmMarkup.kt`: autolink con ángulos, flanking, tiradas, separadora de tabla | T1 |
| T3 | Cableado en `RichTextParser`: pre-pasada, setext, autolink | T1, T2 |
| T4 | Cableado: `#` de cierre, `+ `, tiradas, flanking | T3 |
| T5 | Cableado: tabla de texto + `preprocessMarkdownBlocks` | T4 |
| T6 | Mutación de cada rama | T5 |
| T7 | Rangos protegidos y cursores | T6 |
| T8 | Cierre: suite completa, `assembleDebug`, `MEMORY.md` | T7 |

## 12. Decisiones y alternativas descartadas

1. **Reescribir el analizador con `commonmark-java`.** Ya está en el APK (spec 012, decisión 13) y es un
   analizador de bloque **correcto**. Descartado: el analizador actual no solo pinta texto, **mapea offsets**
   de fuente a texto pintado, y de ese mapa dependen el cursor, la selección, el copiado y los rangos
   protegidos (`ParseResult.originalToTransformed`, `snapOffset`, `previousVisibleOffset`…). Un analizador de
   bloque da la posición de cada nodo, no un mapeo carácter a carácter de un texto con marcas ocultas, y
   reconstruirlo sería **más** código y más riesgo que extender lo que ya funciona y ya está probado. Además
   la spec 012 ya decidió que ese motor es del editor de código, y esta spec es del editor de notas: dos
   gramáticas para dos superficies, no una.

2. **`+ ` no entra en la lista de viñetas porque «ya hay dos».** Entra: GFM lo reconoce, es una línea más en
   una lista que ya tiene un campo para el glifo, y el coste de no tenerlo es que `+ Uno` sale como texto.

3. **La tabla se convierte en bloque desde el analizador.** Descartado: es trabajo de la capa de bloques
   (`preprocessMarkdownBlocks`), que ya la hace, y hacerlo aquí sería **doble conversión** para la misma
   línea. RF-97 dice explícitamente que la tabla de bloque no se toca.

4. **La fila separadora se pinta en gris en vez de ocultarse.** Ocultarla sería más fiel al render de GitHub,
   pero la fila separadora **es** información (dice cuántas columnas tiene la tabla), y el modo
   «enseñar las marcas en gris» existe justamente para poder verlas. Ocultarla del todo impediría ver la
   tabla con sus marcas, que es lo que el modo gris promete.

5. **Regla de flanking completa de GFM.** Descartada: cubre casos con puntuación adyacente que no aparecen en
   una nota, y son un documento entero de reglas. La versión simple cubre `a*b*c` y `snake_case`, que es lo
   que se ve. Queda como límite conocido en la spec.

6. **Front matter.** Descartado: `---` al principio con un `---` de cierre es *front matter* de los editores
   estáticos, no de GitHub, y en una nota no anuncia nada. Además, el criterio del underline setext ya resuelve
   ese caso: sin texto encima, `---` es raya.
