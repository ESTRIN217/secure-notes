# Spec 023 — Tareas

> Orden de dependencia. Una tarea por sesión de trabajo (≤ 30 min). Los tests van **primero**.

- [x] **T1. Pruebas puras de la clasificación, en rojo.** RF-141, RF-144, RF-145
  - Hecho cuando: existe `visor-media/src/test/java/com/estrin217/visormedia/util/VideoUrlHelperTest.kt` con los
    casos de `plan.md` § Estrategia 1 (las 10 extensiones, mayúsculas, `?`/`#`, YouTube con y sin esquema,
    `vimeo.com`, `.avi`, `/mp4/listado`, ruta local, vacío) y `./gradlew :visor-media:test` **falla** porque
    `isDirectVideoFileUrl` y `opensInApp` todavía no existen.
  - **Hecho.** Rojo por las dos únicas referencias nuevas (`Unresolved reference 'isDirectVideoFileUrl'`,
    `'opensInApp'`) y por nada más: el resto de casos compilan, así que la regresión de lo que ya existía ya
    estaba medida antes de tocar la implementación.

- [x] **T2. La clasificación pura (una sola regla).** RF-140, RF-141, RF-142, RF-143, RF-144, RF-145, RF-149
  - Hecho cuando: `VideoUrlHelper.kt` tiene `VIDEO_EXTENSIONS` (las 10), `isDirectVideoFileUrl` y `opensInApp`
    con el orden del pseudocódigo (YouTube → directo → web → local), **sin** importar `android` ni `androidx`, y
    `./gradlew :visor-media:test` pasa en verde.
  - **Hecho.** **15 pruebas, 0 fallos** (`:visor-media` pasa de `0/0` a `15/0`). `takeWhile { it != '?' && it != '#' }`
    es lo que deja `.MP4?token=abc#t=10` en `mp4` sin tocar la ruta.

- [x] **T3. El bloque editable abre dentro lo que se reproduce dentro.** RF-140, RF-141, RF-142, RF-143
  - Hecho cuando: `EditableVideoBlock.handleOpen` decide con `opensInApp(src)`; el camino externo llama a
    `abrirVideoFuera(context, src)` de `ui/VideoBlockOpen.kt` (nuevo, con el `Intent.ACTION_VIEW` y el
    `Toast` de siempre); el fichero **ya no** contiene `ACTION_VIEW`; la miniatura y el marcador no cambian; y
    `./gradlew :app:compileDebugKotlin` compila.
  - **Hecho.** Tres imports quedaron sin uso (`Intent`, `Uri`, `Toast`) y se fueron con el `ACTION_VIEW`.
    `abrirVideoFuera` registra el fallo con `OpaqueTrace` —**sin la URL**, que es material de la persona— en vez
    de tragarse la excepción en silencio.

- [x] **T4. El render de solo lectura decide igual.** RF-149
  - Hecho cuando: el `clickable` de `renderVideoBlock` en `NoteContentBlockRenderer.kt` usa `opensInApp(src)`
    para elegir entre `onNavigateToMediaViewer("video", src)` y `abrirVideoFuera(...)`; el fichero **ya no**
    contiene `ACTION_VIEW`; y `./gradlew :app:compileDebugKotlin` compila.
  - **Hecho.** `LocalContext.current` sube fuera del `Box`: el `clickable` del `modifier` no ve lo que se declara
    dentro del `content`. `compileDebugKotlin` en verde.

- [x] **T5. Invariantes de la spec 023.** RNF-82, RNF-83, RNF-84, RNF-85, RNF-86, RNF-88
  - Hecho cuando: existe `app/src/test/java/com/example/VideoOpenInvariantsTest.kt` con los invariantes de
    `plan.md` § Estrategia 2 (una sola regla en las dos superficies, `ACTION_VIEW` solo en `VideoBlockOpen.kt`,
    `VideoUrlHelper` puro, las 10 extensiones, cero textos nuevos, y la librería descartada fuera de Gradle),
    cada recorrido **quita comentarios antes de buscar** y **se autocomprueba**, y pasan en verde.
  - **Hecho.** **8 pruebas, 0 fallos.** La primera versión del invariante de textos comparaba las claves de los
    nueve locales **entre sí** y se puso roja: el repo no las tiene cuadradas (`values-en-rGB` declara 876 claves
    frente a 1183). Se cambió por el que sí es cierto —la lista **literal** de claves que muestra el bloque
    editable no cambia— y se dejó escrito en el `plan.md` por qué no se compara lo otro.

- [x] **T6. Documentación y cuenta de la suite.** RNF-87
  - Hecho cuando: `docs/EDITOR.md` deja de decir que el bloque de vídeo «se abre externamente» y explica qué se
    abre dentro y qué no; y la suite completa confirma que `:app` **no** suma ninguna clase roja nueva, que
    `:visor-media` pasa de `0/0` a `N/0`, y que los fallos de plataforma y los preexistentes son **los mismos**
    (comparados por clase, no por cantidad).
  - **Hecho.** `docs/EDITOR.md` línea 77 reescrita. Suite: `:app` **734/41** (antes 726/41: **+8** invariantes,
    **las mismas 41 clases** — 38 de plataforma y las 3 ajenas de `HtmlExportMediaTest` y
    `OnDeviceModelSourceTest`), `:editor-de-codigo` **136/2**, `:markdown` **49/0**, `:visor-media` **15/0**
    (antes `0/0`). Comparado **por clase**, no por cantidad.

- [x] **T7. Verificación en dispositivo (la hace quien Decide).** RF-140, RF-141, RF-142, RF-143, RF-146, RF-147, RF-151
  - Hecho cuando: se recorre el guion de seis pasos al final de `spec.md` y los cuatro tipos de bloque abren
    donde deben. Es lo único que ninguna prueba pura puede comprobar: que el vídeo se vea **dentro**.
  - **Sirvió dos veces.** Su primera pasada encontró los dos defectos que ninguna prueba veía —el vídeo web se
    abría **en una pestaña** y la pestaña **se cerraba sola** (la poda daba por muerta cualquier fuente que no
    fuera un fichero local) → T8 y RF-150; y esa pestaña se rotulaba `watch?v=…` → T9 y RF-151—. La segunda
    pasada, ya con los dos arreglos, **cerró la spec: los cuatro bloques abren donde deben y la pestaña se
    mantiene**. Es la prueba de que el guion de dispositivo no era ceremonia: las dos reglas que fallaban solo
    se veían abriendo la app.

- [x] **T8. La pestaña de un vídeo web no se cierra sola.** RF-150, RNF-89, RNF-90
  - Pruebas **primero**, en rojo: en `app/src/test/java/com/example/ui/OpenNoteTabsTest.kt`, una pestaña de
    medio con `https://…/v.mp4`, con `https://www.youtube.com/watch?v=…` y con `youtu.be/ID` sin esquema
    **sobrevive** a `pruneMissing`; el caso de fichero local borrado sigue cerrándose; y el lambda de «¿existe?»
    **no** se consulta para una fuente web (contador).
  - Hecho. Rojo por la única referencia nueva, después `OpenNoteTabsTest` **24/0** (eran 20/0) y
    `:app:compileDebugKotlin` compila. Nota: la prueba del contador es la que fija RNF-89 sin red.
  - ⚠️ Cuidado con el orden de lectura del arreglo: el corto-circuito (`isRemoteMediaSource(src) ||`) es **la
    regla**, no una optimización. Invertirlo consultaría el disco —y en el caso real, la red— por cada pestaña.

- [x] **T9. El rótulo de la pestaña de un vídeo web es el identificador (o el nombre de la ruta).** RF-151, RNF-91
  - Pruebas **primero**, en rojo: en `visor-media/.../VideoUrlHelperTest.kt`, `webVideoLabel` devuelve el
    identificador para `watch?v=ID`, `watch?v=ID&list=PL…&t=30`, `youtu.be/ID` (con y sin esquema), `/shorts/ID`
    y `/embed/ID`; el último tramo de la ruta sin consulta ni fragmento para `v.mp4?token=abc` y
    `stream.m3u8#t=10`; el host para `https://ejemplo.com/`; y `null` para `/local/v.mp4`, `content://…` y
    `file:///…`, que conservan la regla de siempre.
  - Hecho. Rojo por la única referencia nueva, después `:visor-media` **21/0** (eran 15/0) y el invariante
    `VideoOpenInvariantsTest` **9/0** (era 8/0). Una expectativa mía estaba mal y la corregí: `…/V.MP4` se
    rotula `V.MP4` —el rótulo **enseña** la dirección, no la compara—, así que la caja solo se normaliza donde
    se compara (`isDirectVideoFileUrl`), no aquí.
