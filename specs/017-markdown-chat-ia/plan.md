# Plan — Spec 017 · Markdown (GFM) en el chat de IA

Spec: `specs/017-markdown-chat-ia/spec.md`

Siete tareas. Las dos primeras son **lógica pura** en un fichero nuevo, y son las únicas que se pueden
comprobar de verdad en esta plataforma: si el texto que llega al pintado está mal, eso se ve antes de haber
tocado nada de pantalla. La parte que de verdad no se puede comprobar desde aquí —que se vea una tabla como
tabla y que el menú de la burbuja siga apareciendo— está marcada como tal en cada tarea.

---

## 1. Archivos y responsabilidades

| Archivo | Tipo | Responsabilidad | Cubre |
|---|---|---|---|
| `gradle/libs.versions.toml` | config | Versión del motor de GFM (`4.6.2`) y alias de sus seis piezas | RNF-48 |
| `app/build.gradle.kts` | config | Declarar esas seis piezas **directas** | RNF-48 |
| `THIRD-PARTY-NOTICES` + `assets/oss-licenses.json` | legal | Entradas del motor de GFM (Apache-2.0, noties/Markwon) | constitución 1 |
| `ui/markdown/ChatMarkdownSource.kt` | **puro** | `markdownForMessage(raw)`: retira el markup heredado y devuelve el texto que se pinta | RF-99, RF-100 |
| `ui/markdown/ChatMarkdownSource.kt` | **puro** | `isWebLink(url)`: qué direcciones cuentan como enlace tocable | RF-100 |
| `ui/markdown/ChatMarkwon.kt` | composable | **Un** motor por pantalla de chat, repartido con `CompositionLocal` | RF-98, RNF-48 |
| `ui/markdown/ChatMarkdownText.kt` | composable | `AndroidView` que pinta el mensaje y decide qué hace un toque sobre un enlace | RF-98, RF-100, RF-101, RF-102, RNF-51 |
| `ui/AiChatScreen.kt` | composable | `MessageBubble` pinta con el motor; `StreamingBubble` no cambia | RF-98, RF-103 |
| `app/src/test/java/com/example/ChatMarkdownSourceTest.kt` | prueba | Las dos funciones puras, en rojo antes | RNF-49 |
| `app/src/test/java/com/example/MarkdownRenderInvariantsTest.kt` | prueba | El motor no crece el APK, `commonmark` no se mueve, el fichero puro no se importa | RNF-48, RNF-49 |

**Por qué `ui/markdown/` y no `ui/chat/`:** `ui/chat/` es, por convenio del proyecto, lógica comprobable sin
composición —lo vigila una prueba—, y aquí hay un composable. Son dos cosas distintas y van en dos carpetas
distintas.

---

## 2. Funciones puras

Las dos, en `ui/markdown/ChatMarkdownSource.kt`. Sin `import androidx.compose`, sin `import android.*`, sin
`Context`, sin `R`, sin reloj.

### 2.1 `markdownForMessage(raw: String): String`

**Firma con el «hoy» por parámetro** — el valor de entrada es lo que hay, y no depende de nada externo:

```
markdownForMessage(raw) == texto que se entrega al motor de GFM
```

**Pseudocódigo:**

```
markdownForMessage(raw):
    lineas = raw.split("\n")
    salida = []
    dentroDeCercada = false
    for linea in lineas:
        if linea empieza con tres o más backticks o tildes y es una línea de solo esa marca:
            dentroDeCercada = !dentroDeCercada
            salida += linea
            continue
        if dentroDeCercada:
            salida += linea            # código: se copia tal cual
            continue
        salida += adaptar(linea)
    return unión(salida, "\n")

adaptar(linea):
    linea = reemplazarEnlace(linea)    # <url=U>t</url>  ->  [t](U)
    linea = sustituirPorLista(linea)   # <item …>        ->  - (o - [x] / - [ ] si trae estado)
    for etiqueta in ETIQUETAS_A_RETIRAR:
        linea = línea.replace(etiqueta, "")
    return linea
```

**Las tres decisiones que lleva dentro, y por qué cada una:**

1. **La guarda de la cercada va primero que todo.** Es la que hace que un `<color=…>` dentro de un ejemplo
   de código siga siendo código. El analizador actual copia el interior de una cercada **verbatim**
   (`RichTextParser:214-218`), así que cualquier limpieza que no respete esa zona es una regresión: el texto
   que se ve hoy dentro de un bloque de código **no** se limpia, y con esto tampoco.

2. **Una etiqueta de enlace se traduce, no se retira.** `<url=U>texto</url>` → `[texto](U)`. Retirarla
   perdería el texto; convertirla conserva lo que se ve hoy y además hace que **sea tocable**, que es el
   RF-100.

3. **`<item …>` se traduce a una casilla, y a una casilla SIN marcar cuando no dice nada.** El chat puede
   recibir una nota con lista de tarea en markup propio, y si no se traduce, esa lista sale sin casillas: se
   vería **peor** que hoy. Que `<item>` a secas sea `[ ]` y no `- ` lo decidió una **sonda** contra el
   analizador actual, no la memoria: hoy `<item>` y `<item checked="false">` pintan lo mismo. De paso
   desaparece el `</item>`, que hoy se ve a la vista al final de cada línea.

**Lo que NO hace:** no reescribe el contenido guardado, no inventa un parser de anidamiento, no toca una
línea que no tenga nada de markup (`si ninguna regla de una línea cambia, se devuelve tal cual`, y eso se
comprueba en la prueba de paridad).

### 2.2 `isWebLink(url: String): Boolean`

```
isWebLink(url) == true  si el esquema es http o https
isWebLink(url) == true  si no hay esquema y parece un dominio con punto  (www., ejemplo.com)
isWebLink(url) == false en cualquier otro caso
```

Descarta `javascript:`, `intent:`, `file:`, `content:`, `tel:` y `mailto:`. Es el RF-100 y sostiene la
RNF-50: una
respuesta del modelo no puede lanzar lo que sea. El caso `mailto:` está deliberadamente **excluido** aunque
`LinkifyPlugin` lo produciría con su máscara por defecto: por eso el motor se construye con máscara de
**solo URLs web**.

---

## 3. El motor (`ChatMarkwon.kt`)

**Un** `Markwon` por pantalla, no uno por burbuja. Se construye con `remember` en `ChatMessageList` y se
reparte con `CompositionLocal`. Motivo: una `LazyColumn` **descarta** lo que sale de pantalla, así que un
motor por burbuja se reconstruye cada vez que se vuelve a una burbuja; uno por pantalla no se reconstruye
nunca mientras el chat esté abierto.

**Plugins**, y el motivo de cada uno:

| Plugin | Por qué |
|---|---|
| `CorePlugin` (el de `Markwon.builder`) | El análisis de bloque normal. **No** el constructor sin núcleo que usa la librería de Markdown del changelog, que trae su propia versión de él. |
| `TablePlugin` | Tablas GFM (RF-98). Sin él, las tuberías se ven. |
| `StrikethroughPlugin` | `~~tachado~~` (RF-98). |
| `TaskListPlugin` | Casillas de lista de tarea (RF-101). Dibuja; **no** hace pulsables. |
| `HtmlPlugin` | Para que `<b>`, `<i>`, `<u>`, `<sub>`, `<sup>` y `<code>` **no** se vean como etiquetas: es lo que permite conservarlos en RF-99 en vez de retirarlos. |
| `LinkifyPlugin` con máscara de **solo URLs web** | RF-100: una dirección escrita en texto plano es tocable, y un número de teléfono **no**. |
| `SoftBreakAddsNewLinePlugin` | Un salto de línea simple se ve como salto de línea. Sin él, una respuesta con pasos se junta en un párrafo y deja de leerse. Es lo que espera quien escribe con el modelo. |

**Lo que NO se registra:** ningún cargador de imágenes y ningún resolutor automático de direcciones de
imagen (RNF-50). En esta versión del motor, además, la resolución de imágenes ya no pasa por el mismo sitio
que los enlaces, así que **no** hay nada que desactivar: simplemente no se registra nada.

**Tema** (`MarkwonTheme`): el motor **no pinta el color del texto** —esa decisión es de la pantalla—, así que
el color de cada burbuja lo pone su `TextView`. Del tema solo se afinan las piezas que sí son del motor: color
de enlace, subrayado de enlace, fondo de código en línea, fondo de bloque de código, color de cita, color de
raya y color de la línea bajo un encabezado.

---

## 4. El pintado (`ChatMarkdownText.kt`)

`AndroidView` con un `TextView` plano. Tres decisiones, y las tres importan:

1. **Sin `movementMethod`.** Es lo que hace que el `TextView` **no se quede con los toques**: el menú de
   pulsación larga y el deslizamiento los sigue atendiendo la burbuja (RF-102). Y es lo que hace que el motor
   **no abra nada por su cuenta**: su resolutor de enlaces solo se dispara desde el toque de un enlace, que
   aquí no hay.

2. **Sin `key(...)` sobre el contenido.** La librería de Markdown del changelog envuelve su `AndroidView` en
   una clave derivada del texto: cualquier cambio de contenido **destruye y recrea** la vista. Aquí la vista
   sobrevive y solo se le vuelve a entregar el texto. Por eso este camino, y no el suyo, es el que podría
   admite la réplica en curso en el futuro sin destrozar la pantalla.

3. **El texto no es seleccionable.** Como hoy.

**El toque sobre un enlace**, que es el único comportamiento propio:

```
al soltar el dedo:
    línea  = disposición.getLineForVertical(y corregido)
    offset = disposición.getOffsetForHorizontal(línea, x)
    enlaces = texto.getSpans(offset, offset, URLSpan)
    si enlaces está vacío  ->  devolver "lo deja pasar"      (el menú y el deslizamiento)
    si ninguno es web      ->  devolver "lo deja pasar"      (un esquema raro no abre nada)
    si no hay aplicación que lo atienda ->  anotar y devolver "lo deja pasar"
    si no -> abrir y devolver "lo he consumido"
```

Que devuelva «lo deja pasar» cuando el destino **no** es web es lo que hace que RF-100 y RF-102 no se
pisen: una casilla, una palabra o un enlace raro no pueden tragarse el menú de la burbuja.

---

## 5. Decisiones, con la alternativa descartada

- **Motor de GFM: `io.noties.markwon` directo, y no la librería de Markdown que ya usa el changelog.**
  Descartada: revisados sus AAR y su código, su «resaltado de sintaxis» solo pintaba nodos delimitados por
  `==` que nada produce (**no había resaltado real**), y su `AndroidView` va envuelto en una clave que
  destruye y recrea la vista en cada cambio de texto. A cambio, hay que escribir el `AndroidView`: son unas
  60 líneas, y a cambio de ellas el gesto y el color dejan de ser cosa de la librería.
- **`isTextSelectable = false` y sin `movementMethod`, y no la librería con su `TextView` parcheado.**
  Descartada: su `TextView` devuelve «lo he consumido» para todo cuando el texto es seleccionable, y solo sobre
  un enlace cuando no lo es. Devolver «lo he consumido» **exactamente sobre un enlace** es lo mismo que
  queríamos, pero arrastraba appcompat y cuatro peculiaridades (ajuste de ancho multilínea, reinicio de estado
  al reciclar, accesibilidad por bloques) que aquí no se usan.
- **El motor se construye en la pantalla y se reparte con `CompositionLocal`, y no con un objeto global.**
  Descartada: el proyecto no usa objetos globales para dependencias (`AGENTS.md`: solo para ayudantes sin
  estado), y un global con `Context` se quedaría apuntando al contexto viejo cuando la app cambia de tema.
- **La réplica en curso sigue con el analizador actual (RF-103).** Descartada: pintarla con el motor es
  posible y barato ahora que la vista no se recrea, pero **el texto a medio escribir cambia de motor a mitad**
  —una tabla que aún no tiene separadora se vería como texto y luego como tabla—, y ese parpadeo es peor que
  la inconsistencia. Es la mejora natural de una spec posterior, no de esta.
- **Limpiar el markup heredado con reglas de línea, y no escribir un parser de etiquetas.** Descartada por
  coste y por alcance: el analizador existente tiene 973 líneas y ya funciona para las notas; aquí solo hace
  falta que **no se vean etiquetas crudas** (RF-99). Un parser completo sería más correcto y mucho más caro, y
  su diferencia se vería solo en markup mal formado, que no es el caso de uso.
- **Las casillas se pintan y no se pulsan (RF-101).** Descartada la alternativa: hacerlo exigía decidir quién
  guarda el estado —el mensaje guardado **no** tiene una clave estable con la que actualizarlo— y añadir eso
  es otra spec.
- **Las pantallas de changelog y de actualización no se migran.** Descartada: migrarlas quitaría la
  dependencia de la librería de Markdown y dejaría un aviso legal menos, a cambio de tocar dos pantallas cuyo
  contenido es **remoto** (el texto de la versión lo trae el servidor) en la misma entrega que cambia el chat.

---

## 6. Estrategia de pruebas

| Qué | Cómo | Dónde |
|---|---|---|
| Adaptación del markup heredado | JUnit puro, sin `RobolectricTestRunner`: enlace `<url>`, etiqueta de lista de tarea, cada grupo de la clase «se conserva» y «se retira», cercada intacta, `<` que no es etiqueta, paridad (sin markup no cambia nada) | `ChatMarkdownSourceTest` |
| Qué direcciones cuentan como enlace | JUnit puro: `http`, `https`, sin esquema con punto, y el rechazo de `javascript:`, `intent:`, `file:`, `tel:`, `mailto:`, vacío y basura | `ChatMarkdownSourceTest` |
| Cero jars nuevos | Lectura del fichero: la versión del motor es la que exige el árbol, y `commonmark-java` sigue en `0.13.0` | `MarkdownRenderInvariantsTest` |
| El fichero puro sigue siendo puro | Lectura del fichero: ni `import androidx.compose` ni `import android.` | `MarkdownRenderInvariantsTest` |
| Nada sale a la red | Revisión del módulo de render (no hay plugin de imágenes) + prueba que fija que el texto a pintar no trae direcciones de imagen | `MarkdownRenderInvariantsTest` + revisión |
| Gestos y apertura de enlaces | **No comprobable aquí.** Requiere pantalla. | pase manual |

**Por qué el motor no se prueba:** construirlo necesita un `Context` real y un `Context` de prueba no
funciona en esta plataforma (Robolectric sin runtime nativo para `linux/aarch64`, 40 fallos ya
documentados). Se compila, y lo que dependa de un `Context` se comprueba a mano. Por eso **toda decisión va
en las funciones puras**: lo que se puede comprobar sin pantalla, se comprueba sin pantalla.
