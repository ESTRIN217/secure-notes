# Plan — Spec 018: un solo motor de Markdown (Markwon) fuera del chat

> La spec describe el **qué**. Este documento describe el **cómo** y, sobre todo, **por qué se descartó cada
> alternativa**. Cada decisión que se aquí aparece con la alternativa que se tiró, porque lo que no se explica
> es lo que se deshace por accidente sin que nada se rompa.

## 1. Archivos y responsabilidades

| Archivo | Acción | Responsabilidad |
|---|---|---|
| `ui/markdown/MarkdownEngine.kt` | **nuevo**, sustituye a `ChatMarkwon.kt` | El motor: plugins, tema de la app, `LocalMarkdownEngine` y `ProvidesMarkdownEngine` |
| `ui/markdown/MarkdownText.kt` | **nuevo** | `MarkdownText(markdown, modifier, color, textStyle)`: el `AndroidView` genérico, la política de toque y el mapeo del `TextStyle` |
| `ui/markdown/ChatMarkdownText.kt` | cambia | Delega en `MarkdownText`. Conserva **toda** la documentación de la 017, que es la que explica por qué el `TextView` no lleva método de movimiento |
| `ui/markdown/ChatMarkdownSource.kt` | cambia | La adaptación del markup heredado, la decisión de qué direcciones son web y **la dirección tal y como se abre** |
| `ui/settings/LegalInfoScreen.kt` | cambia | `RichTextParser.parse()` → `MarkdownText` + el motor |
| `ui/settings/UpdateInfoScreen.kt` | cambia | `MarkdownText` de la librería retirada → `MarkdownText` de la app |
| `ui/settings/ChangelogSheet.kt` | cambia | Ídem |
| `ui/AiChatScreen.kt` | cambia (2 líneas) | Quita el `remember(turn.content) { markdownForMessage(...) }` de la burbuja; el nombre del motor pasa a `MarkdownEngine` |
| `app/build.gradle.kts` | cambia | Fuera `compose-markdown` |
| `gradle/libs.versions.toml` | cambia | Fuera `composeMarkdown` |
| `app/src/main/assets/oss-licenses.json` | cambia | Fuera la entrada; el uso de Markwon pasa a decir «toda superficie de Markdown» |
| `app/src/main/java/com/example/util/OssLicenses.kt` | cambia | Fuera del respaldo |
| `THIRD-PARTY-NOTICES` | cambia | Ídem |
| `app/src/test/java/com/example/MarkdownRenderInvariantsTest.kt` | cambia | El invariante de «compose-markdown se queda» **se invierte**; la ruta del módulo de render cambia de nombre |
| `app/src/test/java/com/example/MarkdownSurfacesInvariantsTest.kt` | **nuevo** | Los invariantes de esta spec |

## 2. Renombrar el motor: por qué y por qué no es cosmético

`ChatMarkwon` deja de ser del chat en cuanto lo usan la pantalla legal, la de actualización y la hoja de
novedades. Un motor llamado `ChatMarkwon` usado por `LegalInfoScreen` es una pista falsa: quien lo lea creerá
que hay un motor aparte para cada sitio.

| Antes | Después |
|---|---|
| `ChatMarkwon` (objeto) | `MarkdownEngine` |
| `ChatMarkwon.LocalChatMarkwon` | `MarkdownEngine.LocalMarkdownEngine` |
| `ChatMarkwon.ProvidesChatMarkwon` | `MarkdownEngine.ProvidesMarkdownEngine` |
| `ui/markdown/ChatMarkwon.kt` | `ui/markdown/MarkdownEngine.kt` |

**Lo que NO se renombra**: `ChatMarkdownSource` y `ChatMarkdownText`. Siguen siendo del chat porque siguen
siendo lo que adapta el markup heredado de una **nota** y lo pinta en una **burbuja**. Renombrarlos sería
mentir: lo queAdaptan no lo necesitan las otras tres superficies (caso límite 2 de la spec).

**Descartado**: dejar el nombre y escribir `ChatMarkwon.ProvidesMarkdownEngine` en la pantalla legal. Menos
diff, y una contradicción en cada lectura.

## 3. La interfaz: un composable y por qué `textStyle` es opcional

```kotlin
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color,
    textStyle: TextStyle? = null
)
```

**El texto entra tal cual.** Quien llama entrega Markdown y el motor lo pinta. La adaptación del markup
heredado **no** se aplica aquí: vive en `ChatMarkdownText`, que es el único sitio que la necesita (caso límite 2
de la spec).

**`textStyle` es opcional y por defecto `null` significa «no toques el estilo de la vista».** No es pereza:
`ChatMarkdownText` **no** pasa ninguno, y así sigue viéndose exactamente igual que hoy. Si el parámetro fuera
obligatorio, el chat tendría que pasar `MaterialTheme.typography.bodyMedium`, y eso **cambiaría su
apariencia**: hoy la burbuja usa el tamaño por defecto de la plataforma —que además respeta el ajuste de
fuente del dispositivo— y `bodyMedium` es un valor fijo. RNF-56 dice que el chat no cambia de apariencia, y
esto es lo que lo cumple.

Las tres superficies de Ajustes **sí** lo pasan, porque sus tres textos pasan hoy por la librería que sí
aplica el `TextStyle` que le dan, y no se puede perder.

### Qué del `TextStyle` se aplica

| Propiedad | Cómo | Por qué |
|---|---|---|
| `fontSize` | `setTextSize(COMPLEX_UNIT_PX, …)` | Es lo que cambia el tamaño de todo el bloque |
| `fontWeight` | `Typeface.create(…, BOLD/NORMAL)` | `Typeface` solo tiene dos: normal y negrita. Un peso 600 cae en negrita, y está escrito así |
| `fontFamily` | `Typeface.create(familia, …)` | — |
| `letterSpacing` | `setLetterSpacing(em)` | — |
| `lineHeight` | `setLineSpacing(alto − natural, 1f)` | Es la fórmula que usa Compose. Se mide **después** de fijar tamaño y familia, porque el alto natural depende de las dos |
| `textAlign` | **no** | Las tres superficies son de texto a la izquierda. Aplicarlo a medias sería peor que no aplicarlo |

Cada conversión va detrás de su `isSpecified`: `TextUnit.Unspecified.toPx()` **lanza**, y un `TextStyle`
parcial es lo normal.

## 4. Algoritmo del composable

```
MarkdownText(markdown, modifier, color, textStyle):
    motor = LocalMarkdownEngine.current          // falla con mensaje claro si nadie lo proveyó
    AndroidView(
        modifier = modifier,
        factory  = { contexto -> TextView plano, sin método de movimiento, no seleccionable,
                     con oyente de toque },
        update   = { vista ->
            si textStyle != null: aplicarEstilo(vista, textStyle)
            vista.setTextColor(color)
            motor.setMarkdown(vista, markdown)     // **no** hay key(markdown.hashCode())
        }
    )
```

**El orden importa**: primero el estilo, después el color, después el texto. Al revés, `setMarkdown` pondría el
color del tema por encima del que se acaba de elegir.

**Sin clave sobre el contenido.** Es el defecto de la librería que se retira: su `AndroidView` va envuelto en un
`key(markdown.hashCode())`, que destruye y recrea la vista en cada cambio de texto. Aquí la vista sobrevive y
solo se le vuelve a entregar el texto.

## 5. La política de toque: una sola, para las cuatro superficies

Las cuatro superficies comparten **el mismo** `TextView` y **el mismo** oyente, y por tanto la misma política:

1. Solo un toque que **termine** (`ACTION_UP`) sobre un enlace **web** abre algo.
2. El enlace es el que hay **exactamente bajo el dedo**: `getOffsetForHorizontal` sobre la fila que da
   `getLineForVertical`, corregido por el relleno y el desplazamiento de la vista.
3. Si no hay enlace, no es web, o no hay ninguna aplicación que lo atienda, se devuelve `false`: **el toque no
   se da por consumido** y el sistema lo sigue atendiendo.

En el chat, devolver `false` es lo que mantiene vivos el menú de la pulsación larga y el deslizamiento. En las
tres superficies de Ajustes no hay burbuja detrás, así que `false` significa simplemente «este toque no abre
nada». **Es el mismo código y la misma decisión**, y por eso RF-106 no necesita una regla nueva: reutiliza la
de RF-100, que ya está comprobada.

### 5.1 La dirección que se abre no es la que la rodeaba

**Este paso no es opcional, y el propio texto legal lo obliga.** En los nueve idiomas, el único
enlace de la pantalla legal sale así:

```
Contact the developer privately via the GitHub profile (https://github.com/ESTRIN217) with the app version…
                                                                              ^^^^^^^^^^^^^^^^^^^^^^^^
```

Un `URLSpan` es un texto con posición, no una dirección: si el detector de enlaces ha metido el paréntesis de
cierre dentro, la dirección que se abre es `https://github.com/ESTRIN217)`, que no existe. Y eso no se puede
comprobar en esta plataforma, porque `Linkify` y `Patterns` son **framework de Android** y aquí no hay runtime
nativo de Robolectric. Es exactamente la clase de cosa que este proyecto marca como «una expectativa de
memoria es una suposición».

Así que se añade una **función pura** al fichero puro, `ChatMarkdownSource.direccionParaAbrir(url)`:

| Al final de la dirección | Qué hace |
|---|---|
| `.` `,` `;` `:` `!` `?` `"` `'` | La quita **siempre**: es puntuación de la frase |
| `)` `]` `}` `>` | La quita **solo si queda sin parear**; si hay un `(`/`[`/`{`/`<` que la abre, **es** parte de la dirección y se conserva |
| Cualquier otra cosa | No toca nada |

Se aplica **antes** de `isWebLink`, y solo recorta: nunca añade caracteres, así que no puede convertir una
dirección inocua en otra que no lo sea. Es **idempotente**: aplicarla dos veces da lo mismo que aplicarla una.

Los dos casos que la sostienen:

- `https://github.com/ESTRIN217)` → `https://github.com/ESTRIN217` — el `)` no tiene `(` delante.
- `https://ejemplo.com/a_(b)` → igual — el `)` sí está pareado, y **quitarlo rompería** una dirección válida
  (el caso límite 6 de la 017).

**Descartado**: reescribir los textos legales para quitar los paréntesis. Son nueve idiomas, es texto legal
firmado, y dejaría el mismo problema en cualquier nota de release que escriba un `)` al final de una frase.
**Descartado también**: fiarse de que el detector lo hace bien. No se puede comprobar aquí, y el coste de
equivocarse es un enlace roto en la letra del contrato.

**Descartado**: poner `LinkMovementMethod` en las tres superficies de Ajustes y dejarlo solo en el chat. Son
dos políticas para el mismo gesto en cuatro sitios de la misma app, y `LinkMovementMethod` abre **cualquier
esquema**: un `javascript:` de unas notas de release se ejecutaría. Además habría que decidir a mano qué pasa
con el gesto en el chat, que es justo lo que ya está decidido.

## 6. Dónde vive el motor en cada pantalla

`ProvidesMarkdownEngine` va **por encima** de la lista perezosa, nunca dentro de cada elemento:

| Pantalla | Dónde |
|---|---|
| Chat | Donde ya está, alrededor de la `LazyColumn` (sin moverlo) |
| Legal | Alrededor del `Scaffold` |
| Actualización | Alrededor del `Scaffold` |
| Novedades | Dentro del contenido del `ModalBottomSheet` |

**Por qué por pantalla y no uno solo en toda la app.** El motor es un objeto de tema: su constructor es
barato pero no gratis, y cambiar de tema claro a oscuro obliga a construirlo otra vez. En cualquier momento solo
hay una de estas pantallas abierta, así que **cuatro motores** nunca coexisten. La regla permanente de
`AGENTS.md` sobre un único `ImageLoader` va de Coil, cuyo coste es el que se paga al cargar una imagen; aquí el
coste es una vez por pantalla.

**Descartado**: subir el motor a `SecureNotesApplication` y proveeérlo en la raíz de cada `Activity`. Sería más
puro, pero toca `MainActivity` y `CodeEditorActivity` para algo que no se nota, y contradice la decisión
explícita de la 017 («uno por pantalla»).

## 7. `RF-108`: el texto se adapta una vez

Hoy el texto de un mensaje pasa **dos veces** por `markdownForMessage`:

- `AiChatScreen`, dentro de la burbuja: `remember(turn.content) { markdownForMessage(turn.content) }`
- `ChatMarkdownText`, en su cuerpo: `ChatMarkdownSource.markdownForMessage(crudo)` — **sin** `remember`

La segunda se ejecuta **en cada recomposición de la burbuja**, sobre el texto entero, en el hilo principal. Que
no se notara es casualidad: adaptar un texto ya adaptado **no** lo cambia, y eso es justamente lo que fija la
prueba de RF-108. Esa propiedad —que el arreglo se apoya en ella— está **comprobada**, no supuesta.

**Dónde queda la adaptación**: dentro de `ChatMarkdownText`, con su `remember(clave = crudo)`. Es donde está el
`remember` que el KDoc de la 017 **ya decía** que tenía y que nunca tuvo. La burbuja deja de hacerlo.

## 8. Licencias y la librería retirada

`compose-markdown` 0.7.2 arrastra, además de sus propias clases: un **fork de Markwon** con el mismo nombre
(`com.github.jeziellago:Markwon`), el decodificador de GIF de Coil (`coil-gif`), Coil y `coil-network-okhttp`, y
`androidx.appcompat`. De todo eso, el APK **solo** conserva lo que la app usa de otra forma:

| Artefacto | Sigue entrando por |
|---|---|
| `coil`, `coil-network-okhttp` | Declarados por la propia app |
| `io.noties.markwon:*` (6 piezas) | Declarados explícitamente desde la 017 (eran `runtime`) |
| `com.github.jeziellago:Markwon` (fork) | **Nada** → desaparece |
| `coil-gif` | **Nada** → desaparece |
| `androidx.appcompat` | `pdf-viewer` y `document-service` |

O sea: **el APK se encoge**, y además se **arregla un agujero de licencias**: el fork nunca estuvo en
`oss-licenses.json`. Al retirar la librería desaparece el problema, no hace falta inventarse su licencia.

Por **`THIRD-PARTY-NOTICES`** se toca la fila de Markwon para que su columna de uso deje de decir «chat con IA»,
que ya sería falso.

## 9. Estrategia de pruebas

**Pruebas puras nuevas** (`MarkdownSurfacesInvariantsTest`, sin `RobolectricTestRunner`, RNF-55):

| Prueba | Qué fija |
|---|---|
| Las tres superficies entregan su texto al motor | `MarkdownText` propio en los tres ficheros, y `RichTextParser` fuera del legal |
| El texto legal es Markdown y no markup heredado | Los cinco textos, **en los nueve idiomas**: al menos una construcción de Markdown y **ninguna** etiqueta propia. Es lo que los hace pertenecer a este lado |
| El texto legal sí tiene un enlace | Y la dirección que lleva la pantalla se abre **entera**, sin el paréntesis |
| Las notas de una release se entregan literales | Ni `markdownForMessage` ni `ChatMarkdownSource` en los dos ficheros |
| El motor se provee por encima de la lista | Las tres pantallas envuelven con `ProvidesMarkdownEngine` |
| Se adapta una sola vez | `markdownForMessage` aparece **una** vez en `ChatMarkdownText` y **ninguna** en la burbuja |
| Adaptar dos veces no cambia nada | Prueba pura sobre `ChatMarkdownSource`: doble aplicación da el mismo texto |
| La librería retirada no está en ninguna parte | Catálogo, `build.gradle.kts`, inventario de licencias y avisos legales |
| Ninguna de las tres superficies carga imágenes | No hay `ImageLoader` ni plugin de imágenes en su pintado |

**Y pruebas puras de la dirección** (`direccionParaAbrir`), que son las que sostienen RF-106 sin necesitar un
dispositivo: paréntesis sin parear, paréntesis **pareados** que **no** se tocan, puntuación final, una dirección
sin nada que quitar, que no recorta nunca hacia una dirección más corta que el esquema, y que aplicarla dos
veces da lo mismo que una.

**Invariantes existentes que cambian**: el de «`compose-markdown` se queda porque otras dos pantallas lo usan»
**se invierte** (RF-107) y la constante `MODULO_RENDER` apunta al fichero nuevo.

**Regresión**: los 9 invariantes de la 017 y las 61 pruebas de `RichTextParser` tienen que seguir verdes sin
tocarse. **Un fallo nuevo en la suite es motivo para parar**, no para subir la cuenta.

**Medición**: `commonmark-java` en `0.13.0` y Markwon en `4.6.2`, **leyendo el fichero**. Es el accidente más
barato y más silencioso que tiene esta app: si Graddy resolviera otra versión, se romperían el chat, el editor
de código y las tres superficies **a la vez**, y ninguna prueba funcional se pondría roja.

## 10. Qué RF cubre cada parte

| Parte | RF / RNF |
|---|---|
| `MarkdownEngine` (renombre y motor) | RF-104, RF-105, RF-106 |
| `direccionParaAbrir` (función pura) | RF-106, RNF-55 |
| `MarkdownText` (composable, estilo, toque) | RF-104, RF-105, RF-106, RNF-54 |
| `ChatMarkdownText` delega y adapta una vez | RF-108, RNF-56 |
| `LegalInfoScreen` | RF-104, RF-106 |
| `UpdateInfoScreen`, `ChangelogSheet` | RF-105, RF-106 |
| Fuera `compose-markdown` | RF-107, RNF-53 |
| `oss-licenses.json`, `OssLicenses.kt`, `THIRD-PARTY-NOTICES` | RF-107 |
| `MarkdownSurfacesInvariantsTest` | RF-104…RF-108, RNF-53…RNF-56 |

## 11. Verificación en dispositivo, al final

Esto no lo puede cerrar ninguna prueba de este repositorio, y hay que decirlo con el mismo peso que el resto
del plan:

1. La pantalla legal: que una lista numerada se vea como lista, que la negrita salga y que la dirección de
   GitHub **se abra al pulsarla sin el paréntesis**. El paréntesis es lo único que no se puede comprobar sin
   una pantalla, y es justo lo que RF-106 fija por pruebas.
2. La hoja de novedades: que una cabecera `##` se vea como cabecera, que una tabla se vea como tabla y que un
   enlace se abra.
3. La pantalla de actualización: que el changelog desplegable se pinte y se anime bien.
4. **El chat**: que las burbujas se vean **igual** que antes. Es el riesgo de este plan, no el render: se toca
   un composable compartido con la única superficie que tiene gestos detrás.