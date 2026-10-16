# Plan — Spec 019: la vista previa del editor de código, con Markwon y sin WebView

> La spec describe el **qué**. Este describe el **cómo** y, sobre todo, **por qué se descartó cada alternativa**.

## 1. Archivos y responsabilidades

### Módulo nuevo `:markdown`

| Archivo | Acción | Responsabilidad |
|---|---|---|
| `settings.gradle.kts` | cambia | `include(":markdown")` |
| `markdown/build.gradle.kts` | **nuevo** | Las seis piezas de Markwon + Compose. **Cero** Coil, **cero** commonmark |
| `markdown/src/main/.../MarkdownEngine.kt` | **movido** de `:app` `ui/markdown/` | El motor, ahora con **colores y cargador parametrizados** |
| `markdown/src/main/.../MarkdownText.kt` | **movido** de `:app` `ui/markdown/` | El composable `AndroidView`. Sin cambios de comportamiento |
| `markdown/src/main/.../MarkdownColors.kt` | **nuevo** | Los colores del motor, en `Int`, para que quien lo pinte decida |
| `markdown/src/main/.../ImageSource.kt` | **nuevo** | **Puro**: qué destino de imagen se puede leer desde dónde (RF-111, RF-112, RF-113) |
| `markdown/src/main/.../LocalFileImageLoader.kt` | **nuevo** | `AsyncDrawableLoader` que decodifica `file://` con `BitmapFactory` y **nada más** |

### `:app`

| Archivo | Acción |
|---|---|
| `ui/markdown/ChatMarkdownSource.kt` | Sigue aquí: adapta una **nota** para una burbuja (decisión 24 de la 018) |
| `ui/markdown/ChatMarkdownText.kt` | Sigue aquí, ahora importando el motor del módulo nuevo |
| `app/build.gradle.kts` | `project(":markdown")` en lugar de las seis piezas sueltas |
| `app/src/test/.../MarkdownSurfacesInvariantsTest.kt` | Rutas de los ficheros movidos |
| `app/src/test/.../MarkdownRenderInvariantsTest.kt` | Rutas de los ficheros movidos |

### `:editor-de-codigo`

| Archivo | Acción |
|---|---|
| `markdown/MarkdownRenderer.kt` | **Se borra** (RNF-60) |
| `markdown/MarkdownPreview.kt` | **Se borra** (era el punto de paso al `WebView`) |
| `ui/PreviewThemeMapping.kt` | **Se borra**: su `PreviewTheme` era el CSS |
| `ui/components/MarkdownPreview.kt` | **Se borra**: el `WebView` entero |
| `ui/MarkdownPreviewPane.kt` | **Nuevo**: el panel con el motor y el desplazamiento |
| `ui/CodeEditorViewModel.kt` | `_previewHtml` → `_previewMarkdown`; el hilo aparte **desaparece** (RF-115) |
| `ui/CodeEditorApp.kt` | Llama al panel nuevo |
| `build.gradle.kts` | `project(":markdown")`, y **fuera** las tres de `commonmark` |
| `src/test/.../MarkdownRendererTest.kt` | **Se borra** (23 pruebas) |
| `src/test/.../MarkdownPreviewContractTest.kt` | **Se reescribe**: el cruce que vigilaba («se genera una vez y se muestra lo generado») sigue valiendo, con otro destinatario |
| `src/test/.../MarkdownPreviewInvariantsTest.kt` | **Nuevo**: RF-110, RNF-57, RNF-60, RNF-61 |

## 2. El módulo nuevo: por qué y no «duplicar el motor»

La dependencia va **en un solo sentido** (`:app` → `:editor-de-codigo`), así que el motor de `:app` no es
alcanzable desde la librería. Las tres salidas eran: módulo nuevo, duplicar el motor, o moverlo a la librería.

**Descartado — duplicar el motor.** Los dos motores acabarían con la misma lista de plugins y la misma política de
toque, y **se desincronizarían sin que nada fallara**: nadie pondría en rojo una prueba por añadir un plugin a uno
y no al otro. Es la misma razón por la que la 018 no dejó dos motores.

**Descartado — moverlo a `:editor-de-codigo`.** Funciona, pero deja el motor de Markdown de **toda** la app dentro
de un módulo cuyo nombre dice «editor de código». Este repositorio ya resolvió exactamente ese problema con
`:visor-pdf` y `:visor-media`, que existen para poder compartir.

**Elegido — un módulo `:markdown`.** Coste: un `build.gradle.kts` más y una entrada en `settings.gradle.kts`. A
cambio, `com.estrin217.markdown` es el hogar de lo que las cinco superficies comparten, y el nombre del paquete
dice lo que es.

**Qué se mueve y qué no.** Se mueven el motor y el composable: son genéricos. **No** se mueve `ChatMarkdownSource`,
porque adapta el markup heredado de una **nota** a una **burbuja**, y ni la vista previa ni las tres superficies de
Ajustes lo necesitan (decisión 24 de la 018).

## 3. El motor, parametrizado

Hoy `MarkdownEngine` lee `MaterialTheme.colorScheme` por dentro. Con dos temas distintos eso no vale, así que se
parametriza **por fuera**:

```kotlin
data class MarkdownColors(
    val enlace: Int, val fondoDeCodigo: Int, val colorDeCodigo: Int,
    val fondoDeBloque: Int, val colorDeCita: Int, val colorDeRaya: Int,
    val colorBajoEncabezado: Int
)

@Composable
fun ProvidesMarkdownEngine(
    colores: MarkdownColors,
    cargadorDeImagenes: AsyncDrawableLoader? = null,
    contenido: @Composable () -> Unit
)
```

Los colores van en `Int` y no en `Color` por el mismo motivo que `PreviewTheme` los llevaba en hexadecimal: **el
motor no debe importar nada de la UI**. Quien llama hace la conversión, como ya hacía `PreviewThemeMapping`.

`cargadorDeImagenes` es `null` en las cuatro superficies de `:app` —no hay ninguna imagen en un mensaje del chat ni
en un texto legal, y RNF-50/RNF-54 lo prohíben— y es el cargador local en la vista previa.

## 4. El cargador de imágenes: sin Coil, y por diseño

El cargador de la 018 **no** carga imágenes (RNF-50), así que el de la vista previa es otro, y es un
`AsyncDrawableLoader` propio de unas 60 líneas:

| Destino | Qué hace |
|---|---|
| Ruta local dentro de la carpeta | Decodifica con `BitmapFactory` en `Dispatchers.IO` y llama a `complete(drawable)` |
| Ruta local que **no** existe | `fail()` con el marcador de «no se pudo leer» |
| Cualquier otra cosa | `fail()` con el marcador de «imagen remota bloqueada», **sin tocar el disco** |

**Por qué `BitmapFactory` y no Coil.** Tres razones, y la tercera es la que manda:

1. **Cero dependencias nuevas** (RNF-57): ni una.
2. La regla permanente de `AGENTS.md` —«el `ImageLoader` singular … no crear `ImageLoader` por pantalla»— **no
   aplica**: no hay ningún `ImageLoader` que crear. Se escribe un cargador de 60 líneas que **solo sabe** decodificar
   un `file://`.
3. **La seguridad es del tipo, no de la configuración.** Un cargador de Coil con la máscara bien puesta se puede
   configurar mal; este **no tiene el método** de pedir nada por red. La red es un caso imposible, no un caso
   prohibido.

**Por qué el marcador y no un hueco.** `MarkdownRenderer` lo eligió a propósito y está razonado en su propio KDoc:
un recuadro con el nombre del host deja claro que la imagen está ahí y por qué no se ve. Un hueco mudo hace pensar
que el Markdown está mal. Se conserva, con **dos** marcadores distintos —«bloqueada» y «no se pudo leer»— porque
son dos hechos distintos y poder decir el correcto es lo que hace útil el recuadro (caso límite 1).

**Caché: no, y se dice.** Sin caché, cada `setMarkdown` vuelve a decodificar. El retardo de 300 ms acota cuántas
veces, y el coste es el mismo que hoy descodifica el `WebView`. Una `LruCache` sería una mejora real, pero es un
paso más que esta spec **no** da: se anota en «Fuera de alcance» con el motivo, en vez de hacerlo a medias.

## 5. La decisión de qué imagen se puede leer (lo puro)

Es lo único de esta spec que se puede probar sin Android, así que es lo que se cuida:

```kotlin
// ImageSource.kt — sin android, sin compose, sin reloj (RNF-58)
enum class ImagenPermitida { PERMITIDA, NO_EXISTE, FUERA_DE_CARPETA, BLOQUEADA }

fun clasificarImagen(destino: String, carpetaBase: String?): ImagenPermitida
```

Las cuatro respuestas, no un `Boolean`, porque RF-111, RF-112 y RF-113 piden tres comportamientos distintos y
**un `Boolean` no puede dar tres**. La ruta se resuelve y se **normaliza** (`..` y `.` fuera) antes de compararla con
la carpeta base: comparar texto sin normalizar es lo que hace que `imagenes/../../fuera.png` pase por dentro
(caso límite 3 de RF-113).

**Descartado — decidir solo por esquema.** `file:///sdcard/Download/x.png` es un `file://` y **no** es del
archivo. Con la decisión por esquema saldría, y es justo el caso que la tiene.

## 6. El desplazamiento (RNF-62, caso de riesgo)

Un `WebView` se desplaza solo. Un `TextView` dentro de un panel con peso **no**: se recorta.

Se envuelve el `MarkdownText` en un `Modifier.verticalScroll(rememberScrollState())`, con el estado **recordado en
el panel** y no dentro del `MarkdownText`, para que sobreviva a las recomposiciones del texto.

**Descartado — `movementMethod` de desplazamiento en el `TextView`.** Choca de frente con la política de la 018: el
`TextView` **no** lleva método de movimiento, que es lo que deja el motor abrir enlaces solo cuando lo decide
`MarkdownText`.

**Riesgo real y medible:** `AndroidView` midiendo un `TextView` muy alto dentro de un contenedor desplazable. No se
puede comprobar aquí sin dispositivo; es lo primero que hay que mirar.

## 7. Lo que se borra y lo que sale de la suite

| Se borra | Por qué |
|---|---|
| `markdown/MarkdownRenderer.kt` | Sin `WebView`, sin llamador (regla 1) |
| `markdown/MarkdownPreview.kt` | Era el punto de paso al `WebView` |
| `ui/PreviewThemeMapping.kt` | Su `PreviewTheme` era el CSS del documento |
| `ui/components/MarkdownPreview.kt` | El `WebView` entero |
| `MarkdownRendererTest.kt` — **23 pruebas** | Probaban el generador de HTML |
| `MarkdownPreviewContractTest.kt` — **7 pruebas** | Se **reescribe**: el cruce que vigilaba sigue valiendo |

Y `:editor-de-codigo` **deja de declarar** `commonmark`: no lo usa nadie. Sigue en el APK, pero **por `markwon-core`**,
que lo exige en `compile` — que es exactamente por lo que la 018 lo dejó en `0.13.0` (decisión 13).

⚠️ Esto **reduce** la suite de `:editor-de-codigo`. Es correcto y hay que decirlo: se borran pruebas de código que
ya no existe, no cobertura de algo que sigue. La cifra que no puede bajar es la de las pruebas **puras que quedan**,
que son las de la decisión de imagen y las de `MarkdownPreviewInvariantsTest`.

## 8. Estrategia de pruebas

**Puras nuevas** (`MarkdownSurfacesInvariantsTest` del módulo `:markdown`, sin `RobolectricTestRunner`):

| Prueba | Qué fija |
|---|---|
| Una imagen local relativa se permite | `imagenes/flujo.png` con la carpeta del archivo |
| La misma imagen con su ruta absoluta se permite | Las dos formas dan lo mismo |
| Un `file://` completo **de fuera** se bloquea | El caso que justifica decidir por ruta y no por esquema |
| Una ruta que se sale con `../` se bloquea | Y también `imagenes/../../fuera.png` |
| `http`, `https`, `data:` y `javascript:` se bloquean | Los cuatro, por separado |
| Sin carpeta base, **todo** se bloquea | Nunca se busca en la raíz del disco |
| La clasificación no muta la entrada | Una función de decisión no toca lo suyo |
| Una ruta local con espacios y acentos se permite | `mis imágenes/año 2026.png` |

**Invariantes** (`MarkdownPreviewInvariantsTest` en `:editor-de-codigo`): sin `WebView` · sin `commonmark` · sin
`MarkdownRenderer` · `PreviewLayoutTest` intacta · el motor sin plugin de imágenes de red ni plugin de HTML · el
cargador sin Coil ni `URL` de red · `:markdown` sin mencionar ni IA ni editor de código.

**Regresión:** las 7 de `PreviewLayoutTest` y las de `EditorViewModeTest` tienen que seguir verdes sin tocarlas, y
la suite de `:app` (689/41) no puede perder más que los fallos que ya tenía.

## 9. Qué RF cubre cada parte

| Parte | RF / RNF |
|---|---|
| Módulo `:markdown`, motor parametrizado | RF-110, RNF-59 |
| `MarkdownText` movido | RF-110 |
| `LocalFileImageLoader` | RF-111, RF-112, RNF-57 |
| `ImageSource` (puro) | RF-111, RF-112, RF-113, RNF-58 |
| Panel nuevo con desplazamiento | RF-115, RNF-62 |
| ViewModel | RF-115 |
| Borrado del pipeline a HTML | RNF-60 |
| `MarkdownPreviewContractTest` reescrito | RF-115 |
| Invariantes | RF-110, RNF-57, RNF-60, RNF-61 |

## 10. Verificación en dispositivo, y en este orden

1. **¿Se desplaza?** Un Markdown largo en `PREVIEW` y en `SPLIT`. Si no se desplaza, la vista previa es inútil y todo
   lo demás da igual.
2. **¿Sale una imagen local?** `![d](imagenes/flujo.png)` al lado de un fichero real.
3. **¿Sale el recuadro de remota?** Un `![d](https://ejemplo.com/x.png)` tiene que **no** salir a la red.
4. **¿`<script>` y `<iframe>` salen como texto?** RF-114 no se puede comprobar sin pantalla: `HtmlPlugin` está en
   Markwon y **no** se registra, pero que el HTML incrustado salga literal y no se interprete es de ver en pantalla.
5. **El editor**: números de línea, resaltado, búsqueda, reemplazo y el cambio de modo, **igual que antes**.