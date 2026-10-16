# Spec 023 — Plan

> Adjunto a `spec.md` (spec 023, **implementada**). Los RF/RNF se citan tal cual. El plan describe **cómo**, con su
> alternativa descartada; los nombres de fichero son los reales del repo.

## Alcance de ficheros

### `:visor-media` — modificado

- **`visor-media/src/main/java/com/estrin217/visormedia/util/VideoUrlHelper.kt`** — **puro** (ya lo era: no
  importa `android` ni `androidx`). Es la pieza única de clasificación (RNF-83). Se le añaden:
  - la lista cerrada de extensiones que el reproductor interno reproduce;
  - `isDirectVideoFileUrl(src)` → RF-141, RF-145;
  - `opensInApp(src)` → la **decisión** de las dos superficies, en un solo sitio (RF-140…RF-144, RF-149).
  - `webVideoLabel(src)` → RF-151, el rótulo de la pestaña de un vídeo web, que **reutiliza**
    `extractYouTubeId` en vez de estrenar conocimiento de YouTube.
  - No se toca `isWebVideoUrl`, `isYouTubeUrl`, `extractYouTubeId` ni `youTubeThumbnail`: su comportamiento es
    el de hoy y hay código que depende de él (RF-148).

- **`visor-media/src/test/java/com/estrin217/visormedia/util/VideoUrlHelperTest.kt`** — nuevo. Pruebas **puras**
  de la clasificación (ver estrategia). Primera prueba del módulo; hoy no tiene ninguna.

### `:app` — modificado

- **`app/src/main/java/com/example/ui/EditableVideoBlock.kt`** — `handleOpen` deja de mirar `isWebVideoUrl` y
  pasa a preguntar a `opensInApp(src)`: dentro → `onOpen()` (lo de hoy para vídeo local), fuera →
  `abrirVideoFuera(...)`. RF-140, RF-141, RF-142, RF-143. La miniatura y el marcador no cambian (RF-148), así
  que `isWebVideoUrl` **sigue** usándose para pintar, no para decidir.
- **`app/src/main/java/com/example/ui/NoteContentBlockRenderer.kt`** — el `clickable` de `renderVideoBlock`
  (línea ~305) usa la misma decisión; su comportamiento actual (todo al visor interno) queda invertido para las
  URLs no reproducibles. RF-149. La miniatura (RF-148) no cambia.
- **`app/src/main/java/com/example/ui/OpenNoteTabs.kt`** — la regla de vida de una pestaña de medio deja de ser
  solo «¿existe el archivo?»: una fuente web se da por viva **sin comprobarla** (RF-150, RNF-89). La decisión
  entra aquí, en el gestor, y no en el lambda del host, para que sea **pura** y se pueda probar sin Robolectric
  —que es la única clase de prueba que corre en esta plataforma—; el lambda sigue respondiendo de lo local
  (`content://` y ficheros), como hasta ahora.
- **`app/src/main/java/com/example/ui/NoteEditorTabsHost.kt`** — `tabTitle` (línea ~331) pregunta primero a
  `webVideoLabel(src)` y **solo si no es una URL web** cae a la regla de siempre (último tramo de la ruta,
  `ifBlank { tab.type }`), que es la que necesitan las fuentes locales y `content://` (RF-151). El resto del
  fichero no se toca: la poda sigue delegando en el gestor (RF-150) y el título de una nota no cambia.
- **`docs/EDITOR.md`** — la fila del bloque Vídeo deja de decir «se abre externamente» y dice qué se abre
  dentro y qué no.

### `:app` — nuevo

- **`app/src/main/java/com/example/ui/VideoBlockOpen.kt`** — `internal fun abrirVideoFuera(context: Context,
  url: String)`: el `Intent.ACTION_VIEW` con su `try/catch` y el `Toast` de siempre
  (`R.string.toast_cannot_open_url`, cero textos nuevos, RNF-85). Es el **único** sitio de las dos superficies
  con `ACTION_VIEW`, y por eso el invariante puede exigir que los dos ficheros de bloque **no** lo contengan
  (RNF-84).

- **`app/src/test/java/com/example/VideoOpenInvariantsTest.kt`** — nuevo. Invariantes de fuentes, con las dos
  reglas del proyecto (quitar comentarios antes de buscar y autocomprobar que se lee). Ver estrategia.

### Sin cambios

- **`visor-media/.../ui/MediaViewerScreen.kt`** — ya elige bien: `isYouTubeUrl` → `InAppYouTubePlayer`, y el
  resto → `Media3VideoPlayer`. Con el bloque mandándole solo lo reproducible, RF-140 y RF-141 se cumplen sin
  tocarlo (`plan.md` § Decisiones, la 5). Tampoco se toca `InAppYouTubePlayer` ni `Media3VideoPlayer`: su aviso
  de error con «abrir fuera» ya es la red de seguridad de RF-147.
- **Cualquier `build.gradle.kts` y `oss-licenses.json`** — RNF-82.
- **Los nueve `strings.xml`** — RNF-85, RNF-86.
- **`DataBlock`, `NoteContentBlock`, Room** — RNF-88.

## Funciones puras

```kotlin
// VideoUrlHelper.kt — :visor-media, puro, probado sin Robolectric

private val VIDEO_EXTENSIONS = setOf("mp4", "m4v", "mov", "webm", "mkv", "m3u8", "ts", "flv", "ogv", "ogg")

fun isDirectVideoFileUrl(src: String): Boolean   // RF-141, RF-145
fun opensInApp(src: String): Boolean             // RF-140, RF-141, RF-142, RF-143, RF-144, RF-149
fun webVideoLabel(src: String): String?          // RF-151

// OpenNoteTabs.kt — :app, puro (sin android), probado sin Robolectric

fun isRemoteMediaSource(ref: String): Boolean    // RF-150, RNF-89
```

`isRemoteMediaSource(ref)` = `VideoUrlHelper.isWebVideoUrl(ref) || VideoUrlHelper.isYouTubeUrl(ref)`: cubre
`http(s)` **y** el enlace de YouTube sin esquema (`youtu.be/ID`), que el bloque también abre dentro y que
tampoco es un fichero. Se apoya en la clasificación compartida en vez de repetir la lista.

No hay parámetro «hoy» en ninguna de las dos: la decisión no depende del tiempo, del disco ni de la red
(RF-144, RNF-89).

## Algoritmo (pseudocódigo)

```
isDirectVideoFileUrl(src):
    si !isWebVideoUrl(src):                    // RF-141: solo http(s)
        → false
    t   = src.trim().lowercase()               // RF-145: mayúsculas
    ruta = t hasta el primer '?' o '#'         // RF-145: consulta y fragmento
    → VIDEO_EXTENSIONS.any { ruta.endsWith("." + it) }

opensInApp(src):
    si isYouTubeUrl(src):        → true        // RF-140  (incluye youtu.be sin esquema)
    si isDirectVideoFileUrl(src):→ true        // RF-141
    si isWebVideoUrl(src):       → false       // RF-142  (Vimeo, páginas, .avi…)
    → true                                     // RF-143  (ruta local, content://, file://)

isRemoteMediaSource(ref):
    → isWebVideoUrl(ref) || isYouTubeUrl(ref)  // RF-150  (youtu.be/ID tiene que caer aquí)

isTabAlive(Media(src)):
    → isRemoteMediaSource(src) || fileExists(src)
      // el corto-circuito es RNF-89: a una fuente web **no** se le pregunta por el disco
      // ni por la red, así que la poda sigue siendo pura y sin E/S

webVideoLabel(src):
    si no es URL web ni de YouTube:  → null         // lo local conserva la regla de siempre
    si extractYouTubeId(src):        → el id       // RF-151: watch?v=, youtu.be/, /shorts/, /embed/
    si la ruta tiene tramo:          → el último tramo, sin `?` ni `#`
    si no:                           → el host
```

`extractYouTubeId` **ya existía** (es la que arma la miniatura de RF-148): el rótulo no estrena conocimiento de
YouTube, lo reutiliza. Y las cuatro formas del enlace dan el **mismo** rótulo, que es lo que se espera al mirar
dos pestañas del mismo vídeo.

El orden importa: `youtu.be/ID` sin esquema **no** es `isWebVideoUrl`, pero **sí** es de YouTube, y el bloque
ya lo abría dentro antes de esta spec. Comprobar la web primero lo sacaría fuera: es el caso que hay que no
romper. Y ese mismo caso tiene que caer también en `isRemoteMediaSource`, o la pestaña se abriría y se cerraría
sola: es exactamente el defecto que apareció en la verificación.

## Interfaz

```kotlin
// EditableVideoBlock.kt
val handleOpen: () -> Unit = {
    if (VideoUrlHelper.opensInApp(src)) onOpen() else abrirVideoFuera(context, src)
}

// NoteContentBlockRenderer.kt (renderVideoBlock)
.clickable { if (VideoUrlHelper.opensInApp(src)) context.onNavigateToMediaViewer("video", src)
             else abrirVideoFuera(context, src) }

// VideoBlockOpen.kt
internal fun abrirVideoFuera(context: Context, url: String)   // Intent.ACTION_VIEW + Toast
```

`onOpen()` / `onNavigateToMediaViewer("video", src)` no cambian: siguen siendo la puerta al visor interno.

## Decisiones justificadas

1. **La regla vive en `VideoUrlHelper` (`:visor-media`), no en `:app`.**
   *Descartado*: escribirla en `:app`. Sería la **tercera** copia de la misma idea —el editor abriendo fuera, el
   render abriendo dentro— y la spec entera es acabar con eso. `:app` **ya** importa
   `com.estrin217.visormedia.util.VideoUrlHelper`, así que compartirla no cuesta un módulo nuevo ni una
   dependencia (constitution 1, RNF-82). El ADR-0004 replica helpers porque `:app` **no** es visible desde las
   librerías; aquí el sentido de la dependencia es `:app` → `:visor-media`, así que ese motivo no aplica.

2. **El límite es «YouTube + archivo de vídeo directo», con lista cerrada.**
   *Descartado*: mandar **toda** URL web al visor interno (lo que hace hoy el render de solo lectura). Una
   página de Vimeo acabaría en el reproductor de ExoPlayer, que falla y deja una pantalla de error: peor que
   abrir el navegador. La lista es la que ExoPlayer reproduce con las dependencias que ya están declaradas
   (`hls` incluido); por eso **no** entra `.mpd` (DASH no está) ni `.avi`.

3. **Cero dependencias: no se añade «Android YouTube Player».**
   *Descartado*: `com.pierfrancescosoffritti.androidyoutubeplayer`. La petición lo nombraba, pero el reproductor
   ya existe (`InAppYouTubePlayer`, `:visor-media`) y funciona; añadir una librería obligaría a justificarla en
   la spec, en `oss-licenses.json` y en `THIRD-PARTY-NOTICES` a cambio de nada (constitution 1, RNF-82).

4. **El `ACTION_VIEW` se delega a un fichero propio, `abrirVideoFuera`.**
   *Descartado*: dejar el `Intent` en cada superficie y comprobar la rama con un invariante textual del tipo
   «este `ACTION_VIEW` está dentro del `else`». Frágil y fácil de sortear por accidente. Con la delegación, el
   invariante es binario y legible: **las dos superficies no contienen `ACTION_VIEW`** (RNF-84).

5. **`MediaViewerScreen` no se toca.**
   *Descartado*: repetir el filtro dentro del visor. Sería validar dos veces lo mismo y, peor, rompería el
   camino que **no** es el bloque: los adjuntos y la búsqueda siguen abriendo el visor para lo que les manden.
   El filtro es del **emisor** (el bloque), no del receptor.

6. **El render de solo lectura se incluye aunque hoy no sea alcanzable.**
   *Descartado*: dejarlo como está. `NoteContentBlockCard` solo se usa en la rama `else` de `BlockEditor` y
   `BlockType.VIDEO` tiene rama propia, así que `renderVideoBlock` no se ejecuta desde ninguna pantalla. Se
   cambia igual para que no quede escrito el criterio contrario al del editor; se comprueba con invariante, no
   con un guion de dispositivo, y así se dice en la spec (RF-149) en vez de fingir que se prueba a mano.

7. **La regla de vida de una pestaña de medio entra en `OpenNoteTabs`, no en el lambda del host.**
   *Descartado*: devolver `true` desde `tabFileExists` para las URLs web. Es menos código, pero mete la decisión
   dentro de una función que se llama «¿existe?» —y una URL web no existe en el disco— y la deja **sin prueba**,
   porque ese lambda necesita un `Context` y aquí Robolectric no se ejecuta. En el gestor la regla es pura, se
   prueba con `OpenNoteTabsTest`, y el lambda se queda respondiendo de lo único que sabe: lo local.
   *Descartado también*: no tocar la poda y hacer que el bloque no abra pestaña, sino pantalla completa. Cambia
   el comportamiento de un vídeo local respecto a un vídeo web, que es justo lo que la spec unifica.

8. **El rótulo de la pestaña se arregla dentro de esta spec, en la pieza compartida.**
   *Descartado*: dejarlo y anotarlo. No es un defecto preexistente: antes ninguna pestaña de medio apuntaba a una
   URL web, así que el rótulo feo (`watch?v=…`) lo **estrena** este cambio, y quien Decide prefirió cerrarlo aquí
   antes que dejar una spec nueva con una limitación visible en su propio camino. Va en `webVideoLabel`, junto a
   la clasificación, porque **reutiliza `extractYouTubeId`** (el mismo que arma la miniatura) y así las cuatro
   formas del enlace dan el mismo rótulo.
   *Descartado*: parchear la cadena en el `tabTitle` del host con un `if` sobre YouTube. Sería la segunda copia
   del conocimiento de YouTube (la primera es `extractYouTubeId`) y no se podría probar sin Robolectric.
   *Descartado*: consultar los metadatos del vídeo para poner el título real. Es una petición de red por cada
   pestaña que se abre: fuga de datos del usuario (constitution 5) y `RNF-91`.

## Estrategia de tests

### 1. Puras de la clasificación — `:visor-media`

`./gradlew :visor-media:test` (clase `VideoUrlHelperTest`, JUnit 4, sin Robolectric). Casos:

| Caso | Entrada | Esperado |
|---|---|---|
| YouTube watch | `https://www.youtube.com/watch?v=dQw4w9WgXcQ` | `opensInApp` |
| YouTube corto | `youtu.be/dQw4w9WgXcQ` (sin esquema) | `opensInApp` |
| YouTube shorts/embed/music | `…/shorts/ID`, `…/embed/ID`, `music.youtube.com/watch?v=ID` | `opensInApp` |
| Vídeo directo | `https://cdn.ejemplo.com/v.mp4` | `opensInApp` |
| Extensión en mayúsculas | `…/V.MP4` | `opensInApp` |
| Consulta y fragmento | `…/v.MP4?token=a#t=10`, `…/v.m3u8#t=30` | `opensInApp` |
| Cada extensión de la lista | las 10 | `opensInApp` |
| Página de vídeo | `https://vimeo.com/123456` | **no** `opensInApp`, sí `isWebVideoUrl` |
| Extensión no soportada | `https://ejemplo.com/pelicula.avi` | no `opensInApp` |
| Extensión no al final | `https://ejemplo.com/mp4/listado` | no `opensInApp` |
| Local | `/data/user/0/…/v.mp4`, `content://…` | `opensInApp` |
| Vacío | `""`, `"   "` | `opensInApp` (no es URL web) |

También los casos de las funciones que **no** cambian (`isWebVideoUrl`, `isYouTubeUrl`) para fijar que la lista
de extensiones no las ha movido (RNF-88).

### 1b. Puras del rótulo — `:visor-media`

En la misma clase: `webVideoLabel(src)` (RF-151, RNF-91). Son las que impiden que vuelva `watch?v=…`: el
identificador para las **cuatro** formas del enlace de YouTube (`watch?v=`, `youtu.be/` con y sin esquema,
`/shorts/`, `/embed/`, y con más parámetros detrás); el último tramo de la **ruta** sin consulta ni fragmento
para un archivo directo (`v.mp4?token=abc`, `stream.m3u8#t=10`); el host cuando no hay ruta; y `null` para lo que
**no** es una URL web (`/local/v.mp4`, `content://…`, `file:///…`), que conserva la regla de siempre.

### 1c. Regresión de la poda de pestañas — `:app`, pura

`./gradlew :app:testDebugUnitTest --tests '*OpenNoteTabsTest*'`. Es la prueba que faltaba y por la que la
pestaña se cerraba sola (RF-150):

- Una pestaña de medio con `https://…/v.mp4` **sobrevive** a `pruneMissing` con un `fileExists` que dice que no
  a todo (el lambda no llega a consultarse: RNF-89).
- Una pestaña con `https://www.youtube.com/watch?v=…` y otra con `youtu.be/ID` **sin esquema** sobreviven igual.
- Una pestaña con un fichero local que ya no está **se sigue cerrando** (RNF-90, prueba que ya existía).
- `pruneMissing` **no** llama al lambda para una fuente web: se comprueba con un contador, que es la forma de
  fijar RNF-89 sin red.

### 2. Invariantes de fuentes — `:app`

`./gradlew :app:testDebugUnitTest --tests '*VideoOpenInvariantsTest*'`. Lee fuentes, **quita comentarios antes
de buscar** y **autocomprueba** que el recorrido no está vacío:

- **Una sola regla, dos superficies** (RNF-83, RF-149): `EditableVideoBlock.kt` y `NoteContentBlockRenderer.kt`
  mencionan `opensInApp`.
- **Nadie abre fuera por su cuenta** (RNF-84): ninguno de esos dos ficheros contiene `ACTION_VIEW`; el único
  fichero con `ACTION_VIEW` del alcance es `VideoBlockOpen.kt`.
- **La pieza es pura** (RNF-83): `VideoUrlHelper.kt` no importa `android` ni `androidx`.
- **La lista cerrada** (RF-141): las 10 extensiones de la spec están en `VideoUrlHelper.kt`; el invariante falla
  si falta una.
- **Cero textos nuevos** (RNF-85): la lista **literal** de claves que muestra el bloque editable no cambia, y la
  apertura externa reutiliza `toast_cannot_open_url`. **No** se compara la paridad entre los nueve locales: el
  repo **no** la cumple —`values-en-rGB` declara 876 claves frente a 1183, y le faltan casi todas las del bloque
  de vídeo—, así que darla por buena sería mentira y denunciarla sería medir otra cosa. Que el bloque no estrene
  claves sí es comprobable, y es lo que pide el requisito.
- **Cero dependencias nuevas** (RNF-82): ningún `build.gradle.kts` ni `gradle/libs.versions.toml` menciona la
  librería descartada (`pierfrancesco`, `androidyoutubeplayer`, `android-youtube-player`). El candado va contra
  **esa** puerta concreta, que es la que la spec cerró, en vez de contra un diff de coordenadas que envejecería
  con cada dependencia legítima.
- **El rótulo lo pone la pieza compartida** (RF-151): `NoteEditorTabsHost.kt` menciona `webVideoLabel`. Es la
  mitad que ninguna prueba pura alcanza —`tabTitle` es `@Composable` y usa `stringResource`, así que necesita
  Robolectric— y sin ella la regla podría quedarse escrita y sin usar, que es el defecto que la 020 persiguió
  con «una línea de catálogo sin nada detrás solo lo aparenta».

### 3. Compilación y suite

`./gradlew :app:compileDebugKotlin :visor-media:compileDebugKotlin`, y después `./gradlew test --continue
--rerun-tasks` para contar los fallos **por clase**: `:app` no puede sumar ninguna clase roja nueva (RNF-87) y
`:visor-media` debe pasar de `0/0` a `N/0`.

### 4. Dispositivo (no automatizable aquí)

El guion va al final de `spec.md`/`tasks.md`: abrir una nota con un bloque de YouTube, con un `.mp4` directo, con
una URL no reproducible y con un vídeo local; comprobar en los cuatro casos dónde se abre el vídeo y que se
vuelve a la nota con el gesto de atrás.
