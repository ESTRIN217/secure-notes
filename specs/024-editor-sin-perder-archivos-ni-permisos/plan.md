# Spec 024 — Plan

> Adjunto a `spec.md` (spec 024, **aprobada**). Los RF/RNF se citan tal cual. El plan describe **cómo**, con su
> alternativa descartada; los nombres de fichero son los reales del repo.

## Alcance de ficheros

### `:editor-de-codigo` — nuevo

- **`editor-de-codigo/src/main/java/com/estrin217/editordecodigo/utils/PersistablePermission.kt`** — **puro**:
  no importa `android` ni `androidx`. Una sola regla, en un solo sitio: `fun appliesTo(scheme: String?): Boolean`
  → `scheme == "content"` (RF-158). Vive aparte de `FileImportExportHelper` (que **sí** importa `android`) para
  que la decisión se pruebe sin Robolectric, que es lo que corre en esta plataforma (regla de `AGENTS.md`).
- **`editor-de-codigo/src/main/java/com/estrin217/editordecodigo/utils/CodeFilePathIndex.kt`** — **puro**: no
  importa `android` ni `androidx`, solo `CodeFile`. Es la pieza única que decide **qué rutas locales** protege el
  editor (RNF-92), a partir de sus recientes:
  - `fun localPaths(files: List<CodeFile>): Set<String>` — devuelve las rutas locales absolutas, normalizadas y
    sin duplicados. Reglas: `filePath` nulo o en blanco no aporta nada; se quita el prefijo `file://` si está;
    `content://…` no es ruta local y no se protege (RF-158, casos límite); nunca se deriva la ruta desde `name`.
  - **Alternativa descartada:** derivar la ruta desde `name` cuando `filePath` es nulo, usando la carpeta del
    editor. Necesita `Context` y deja la regla **sin prueba** en esta plataforma (regla de `AGENTS.md`); además
    `CodeFileRepository.saveFile` garantiza `filePath` para todo archivo local, así que no hace falta.

### `:editor-de-codigo` — modificado

- **`data/db/CodeFileDao.kt`** — `@Query("SELECT * FROM code_files") suspend fun getAllFilesOnce(): List<CodeFile>`.
  Room ejecuta las suspend en su propio executor, así que la lectura no bloquea el hilo principal (RNF-93).
- **`data/db/AppDatabase.kt`** — el literal `"code_tools_db"` sube a `const val DB_NAME` (usado por el builder y
  por la comprobación de existencia) y se añade `fun databaseExists(context: Context): Boolean`.
  - **Alternativa descartada:** consultar siempre. Abrir Room **crea** el fichero de base de datos, y el
    administrador no debe estrenar datos ajenos por el mero hecho de escanear (RNF-94). Con el guard, una base
    que no existe devuelve «cero rutas protegidas» sin tocar el disco.
- **`CodeEditor.kt`** (fachada `:app` ↔ `:editor-de-codigo`) — `suspend fun recentLocalFilePaths(context: Context): Set<String>`:
  si la base no existe → `emptySet()`; si no, `CodeFilePathIndex.localPaths(AppDatabase.getInstance(...).codeFileDao().getAllFilesOnce())`.
  Es la **puerta** por la que `:app` lee los recientes sin tocar los internos del editor (dirección única
  `:app` → `:editor-de-codigo`, spec 022).
- **`utils/FileImportExportHelper.kt`** — un añadido:
  - `fun persistSharedStoragePermission(context: Context, uri: Uri)` — guarda el permiso persistente (RF-155).
    Pregunta a `PersistablePermission.appliesTo(uri.scheme)` si toca (RF-158); primero pide lectura **y**
    escritura; si el proveedor solo concedió lectura, reintenta solo lectura; si las dos fallan, registra el fallo
    con `OpaqueTrace` (solo la clase de la excepción, RNF-96) y **sigue** (RF-157).
  - La llamada es el **primer paso** de `readExternalFile` y de `readExternalFolder` (la URI del árbol). Así
    quedan cubiertos de una vez **los dos** caminos —el selector del sistema (`OpenDocument`/`OpenDocumentTree`
    en `CodeEditorApp`) y el `Intent` de otra app (`CodeEditorViewModel.importUri`)— sin depender de que cada
    llamante se acuerde. Un tercer camino futuro hereda la regla por construcción.
- **Tests nuevos (puros):** `src/test/java/com/estrin217/editordecodigo/CodeFilePathIndexTest.kt` y
  `.../PersistablePermissionTest.kt`.

### `:app` — modificado

- **`app/src/main/java/com/example/data/storage/StorageAnalyzer.kt`** — `scan()` suma a las rutas referenciadas
  las de los recientes del editor:
  - nuevo `private suspend fun collectEditorPaths(context: Context): Set<String>` que delega en
    `CodeEditor.recentLocalFilePaths(context)` y, ante cualquier excepción, registra con `OpaqueTrace` y devuelve
    `emptySet()` (RNF-94: un fallo al leer los recientes no puede tumbar el escaneo).
  - `val referencedPaths = collectReferencedPaths(database) + collectEditorPaths(context)`.
  - `isReferenced`/`isOrphan` **no cambian**: huérfano sigue siendo «no referenciado»; lo único que cambia es
    **qué** cuenta como referenciado (RF-152, RF-154, RNF-97).
- **`app/src/test/java/com/example/StorageEditorFilesInvariantsTest.kt`** — nuevo; invariantes **puros** por
  lectura de fuente (patrón de `VideoOpenInvariantsTest`).

**No se toca la interfaz.** La sección de huérfanos deja de listar los archivos del editor porque dejan de ser
huérfanos; no hay sección ni categoría nueva (decisión aprobada). No se toca ningún `strings.xml` (RNF-96) ni
ningún `build.gradle.kts` (RNF-95).

## Funciones puras (con `hoy` como parámetro sólo donde toca)

| Función | Módulo | Entrada | Salida | RF/RNF |
|---|---|---|---|---|
| `CodeFilePathIndex.localPaths` | `:editor-de-codigo` | `List<CodeFile>` (los recientes) | `Set<String>` de rutas locales | RF-152, RF-153, RNF-92 |
| `PersistablePermission.appliesTo` | `:editor-de-codigo` | `scheme: String?` | `Boolean` | RF-158 |

`persistSharedStoragePermission` y `recentLocalFilePaths` **no** son puras (usan `Context`/`ContentResolver`) y
por eso no llevan prueba unitaria; la decisión que contienen está extraída a las funciones puras de la tabla y su
**cableado** se comprueba con invariantes.

## Algoritmo en pseudocódigo

```
# RF-152/153/154 — el administrador deja de borrar lo del editor
scan(context, database):
    referenced  = collectReferencedPaths(database)      # lo de hoy (notas)
    referenced += collectEditorPaths(context)           # lo nuevo (recientes del editor)
    ...                                                 # el resto del escaneo NO cambia

collectEditorPaths(context):
    try:     return CodeEditor.recentLocalFilePaths(context)
    catch e: registrarFallo("read editor recent files", "codeFile"); return emptySet()
                                                                        # RNF-94

# RNF-92 — qué rutas locales protege el editor (puro)
localPaths(files):
    rutas = {}
    for f in files:
        p = (f.filePath ?: continue).trim()
        if p.startsWith("file://"): p = p.removePrefix("file://")
        if p.isEmpty() or p.startsWith("content://"): continue
        rutas.add(p)
    return rutas

# RF-155/156/157/158 — el permiso sobrevive al reinicio
appliesTo(scheme):  return scheme == "content"                         # puro

persistSharedStoragePermission(context, uri):
    if not appliesTo(uri.scheme): return                               # RF-158
    if intentar(uri, READ or WRITE): return                            # RF-155, RF-156
    if intentar(uri, READ):         return                             # el proveedor sólo dio lectura
    registrarFallo("persist uri permission", "externalFile")           # RF-157, sin datos
```

`intentar(uri, flags)` envuelve `contentResolver.takePersistableUriPermission` en un `try/catch` que devuelve
`false` ante cualquier excepción; una URI ya persistida no lanza y cuenta como éxito (RNF-98).

## Decisiones justificadas (con su alternativa descartada)

1. **La consulta de los recientes vive en la fachada del editor, no en `:app`.** `StorageAnalyzer` llama a
   `CodeEditor.recentLocalFilePaths`, no a `AppDatabase`/`CodeFileDao`. Alternativa descartada: que `:app` abra
   `com.estrin217.editordecodigo.data.db.AppDatabase` directamente — funcionaría (la dependencia ya existe),
   pero ata el administrador a los internos del editor y rompe el único punto de entrada del módulo.
2. **Persistir dentro de `readExternalFile`/`readExternalFolder` y no en los llamantes.** Alternativa
   descartada: llamarlo en cada sitio que abre el selector — tres sitios hoy, y el próximo que se añada se
   olvidará. Dentro del lector, la regla se hereda por construcción.
3. **Guard de base inexistente.** Alternativa descartada: consultar siempre y dejar que Room cree la base; el
   escaneo de almacenamiento crearía un fichero en `databases/` que antes no existía.
4. **Permiso de lectura y escritura con caída a lectura.** Alternativa descartada: pedir solo lectura (no
   permitiría guardar en el original tras reiniciar) y pedir solo escritura (no permitiría releer).
5. **Sin sección propia en el administrador.** Alternativa descartada: nueva `StorageCategory` + sección; añade
   superficie de interfaz y textos que la petición no pide.

## Estrategia de tests (`./gradlew test`)

### 1. Pruebas **puras** de la decisión (`:editor-de-codigo`, sí corren aquí)

`CodeFilePathIndexTest` (RNF-92, RF-152/153):

- lista vacía → conjunto vacío;
- ruta absoluta (el caso real de `createPhysicalFile`) → protegida;
- `file:///data/…/a.kt` → protegida como `/data/…/a.kt` (prefijo fuera);
- fila con `filePath = null` y `uriString = content://…` → **no** aporta ruta local (caso límite);
- `filePath` en blanco → ignorada;
- dos filas con la misma ruta → una sola entrada;
- ruta con espacios (`/…/mi archivo.kt`) → se conserva literal.

`PersistablePermissionTest` (RF-158): `content` → `true`; `file` → `false`; `null` → `false`; `""` → `false`.

### 2. Invariantes de `:app` (`StorageEditorFilesInvariantsTest`, puros por lectura de fuente)

Con las dos reglas del proyecto: cada recorrido **quita comentarios** antes de buscar y **se autocomprueba**
(lee el fichero y exige un mínimo de líneas). Cubren el **cableado**, que es lo que las pruebas puras no ven:

- **RF-152/RNF-97** — `StorageAnalyzer.kt` menciona `CodeEditor.recentLocalFilePaths` y su resultado se suma al
  conjunto de rutas referenciadas (se comprueba la línea de la suma, no solo la mención).
- **RNF-92** — `CodeFilePathIndex.kt` **no** importa `android` ni `androidx`.
- **RF-158 / RNF-92** — `PersistablePermission.kt` **no** importa `android` ni `androidx`, y `FileImportExportHelper`
  pregunta a `PersistablePermission.appliesTo` antes de pedir el permiso.
- **RF-155/157** — `FileImportExportHelper.kt` contiene `takePersistableUriPermission` y registra el fallo por la
  vía del proyecto; `readExternalFile` y `readExternalFolder` llaman a `persistSharedStoragePermission`.
- **RNF-94** — `CodeEditor.recentLocalFilePaths` consulta `databaseExists` antes de obtener la base.

### 3. Honestidad sobre lo que **no** se prueba aquí

- Que el permiso persistente **sobreviva al reinicio** es comportamiento del sistema, no código nuestro: no hay
  prueba aquí. Va al guion de dispositivo (RF-156).
- Que la base del editor no exista y devuelva `emptySet()` necesita `Context` → Robolectric, que no corre en
  esta plataforma. Se cubre con el invariante de `databaseExists` (RNF-94).
- Cero claves nuevas (RNF-96) y cero `build.gradle.kts` tocados (RNF-95) se comprueban con `git diff` en las
  tareas, no con una prueba (una prueba que fijara el número de claves mentiría en cuanto otra spec añada una).

### 4. Compilación

`./gradlew :editor-de-codigo:compileDebugKotlin`, `./gradlew :editor-de-codigo:test` (línea base `136/2`),
`./gradlew :app:test --continue` (línea base `739/41`): las mismas clases rojas de siempre, ninguna nueva.

## Guion de verificación en dispositivo (RF-153, RF-156)

1. Editor de código → crear un archivo (`prueba.md`), escribir y guardar.
2. Ajustes → Almacenamiento → **no** debe aparecer en «Archivos huérfanos». Pulsar «Borrar huérfanos» (y, si
   procede, «Limpiar ahora» con la limpieza automática activada).
3. Volver al editor: el archivo sigue abriéndose con su contenido.
4. Abrir con el selector del sistema un archivo de Descargas y editarlo.
5. Cerrar la app **por completo** (quitarla de recientes) y volver a abrirla.
6. Abrir ese archivo desde los recientes del editor: se ve el contenido (antes se abría vacío).
