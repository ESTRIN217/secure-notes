# Spec 024 — Tareas

> Orden de dependencia. Una tarea por sesión de trabajo (≤ 30 min). Los tests van **primero**.

- [x] **T1. Pruebas puras de las dos decisiones, en rojo.** RNF-92, RF-152, RF-153, RF-158
  - Hecho cuando: existen `editor-de-codigo/src/test/java/com/estrin217/editordecodigo/CodeFilePathIndexTest.kt`
    y `.../PersistablePermissionTest.kt` con los casos de `plan.md` § Estrategia 1 (ruta absoluta, `file://`,
    `content://`, `null`, blanco, duplicados, lista vacía, ruta con espacios; `content`/`file`/`null`/`""`), y
    `./gradlew :editor-de-codigo:testDebugUnitTest --rerun` **falla** porque `CodeFilePathIndex.localPaths` y
    `PersistablePermission.appliesTo` todavía no existen.
  - **Hecho.** Rojo por las **dos únicas** referencias nuevas (`Unresolved reference 'CodeFilePathIndex'` y
    `'PersistablePermission'`) y por nada más: el resto de casos compilan, así que la regresión de lo que ya
    existía ya estaba medida antes de tocar la implementación. 13 casos (9 + 4).

- [x] **T2. Las dos piezas puras.** RF-152, RF-153, RF-158, RNF-92
  - Hecho cuando: `utils/PersistablePermission.kt` y `utils/CodeFilePathIndex.kt` existen **sin** importar
    `android` ni `androidx`, con el orden del pseudocódigo, y `./gradlew :editor-de-codigo:testDebugUnitTest`
    pasa en verde (las dos pruebas nuevas, y los `136/2` de siempre).
  - **Hecho.** `:editor-de-codigo` de **136/2 a 149/2**: +13 pruebas, **los mismos 2 fallos**
    (`ExternalFileStorageRobolectricTest`, plataforma). Se añadió un caso más al índice —una fila cuya «ruta»
    es `content://`— para cubrir la rama defensiva.

- [x] **T3. Los recientes del editor, por su fachada.** RF-152, RNF-93, RNF-94
  - Hecho cuando: `CodeFileDao` tiene `getAllFilesOnce()`; `AppDatabase` expone `DB_NAME` y `databaseExists`;
    `CodeEditor.recentLocalFilePaths(context)` devuelve `emptySet()` si la base no existe y, si no,
    `CodeFilePathIndex.localPaths(dao.getAllFilesOnce())`; y `./gradlew :editor-de-codigo:compileDebugKotlin` y
    `:editor-de-codigo:test` compilan/corren sin cambios en el resultado.
  - **Hecho.** `compileDebugKotlin` en verde y **149/2** otra vez: ninguna prueba nueva rota por cambiar la
    base de datos del editor.

- [x] **T4. El permiso se guarda al abrir.** RF-155, RF-156, RF-157, RF-158, RNF-96, RNF-98
  - Hecho cuando: `FileImportExportHelper.persistSharedStoragePermission(context, uri)` pide lectura y escritura,
    cae a lectura si el proveedor solo concede eso, y registra con `OpaqueTrace` (sin datos) cuando las dos
    fallan; `readExternalFile` y `readExternalFolder` la llaman **antes** de leer; y
    `./gradlew :editor-de-codigo:compileDebugKotlin` compila.
  - **Hecho.** `tryPersist` devuelve la excepción en vez de tragársela, para que el registro diga la clase del
    fallo sin leer su mensaje. Una URI ya persistida no lanza: cuenta como éxito (RNF-98).

- [x] **T5. El administrador deja de ser ciego a los recientes.** RF-152, RF-153, RF-154, RNF-93, RNF-94, RNF-97
  - Hecho cuando: `StorageAnalyzer.scan` suma `collectEditorPaths(context)` a `collectReferencedPaths(database)`;
    `collectEditorPaths` delega en `CodeEditor.recentLocalFilePaths` y, ante cualquier excepción, registra con
    `OpaqueTrace` y devuelve `emptySet()`; `isReferenced`/`isOrphan` **no** cambian; y
    `./gradlew :app:compileDebugKotlin` compila.
  - **Hecho.** `isReferenced`/`isOrphan` intactos: lo único que cambia es **qué** cuenta como referenciado
    (RNF-97).

- [x] **T6. Invariantes de la spec 024.** RNF-92, RNF-94, RNF-97, RF-152, RF-155, RF-157, RF-158
  - Hecho cuando: existe `app/src/test/java/com/example/StorageEditorFilesInvariantsTest.kt` con los invariantes
    de `plan.md` § Estrategia 2 (el analizador suma los recientes; la pieza pura no importa `android`/`androidx`;
    la fachada comprueba `databaseExists`; el lector pide el permiso y lo registra si falla), cada recorrido
    **quita comentarios** y **se autocomprueba**, y `./gradlew :app:testDebugUnitTest --tests '*StorageEditorFiles*'`
    pasa.
  - **Hecho.** **7 invariantes en verde.** El piso del recorrido se bajó de 20 a **12** líneas al medir el repo:
    `PersistablePermission.kt` tiene 17, y un piso de 20 habría puesto rojo el invariante por un fichero
    legítimamente corto.

- [x] **T7. Compilación y suite completa, sin regresiones.** RNF-95, RNF-96
  - Hecho cuando: `./gradlew :editor-de-codigo:test --continue` da `136+2` con **los mismos 2 fallos**
    preexistentes, `./gradlew :app:test --continue` da `739+` con **los mismos 41 fallos** y **ninguna clase
    nueva rota**, y `git diff --stat` **no** toca ningún `build.gradle.kts` ni ningún `strings.xml`.
  - **Hecho.** `:editor-de-codigo` **149/2** y `:app` **746/41** contra las bases **136/2** y **739/41**: los
    +13 y +7 son las pruebas nuevas, y las clases rojas son **las mismas** de siempre (en `:app`,
    `ExternalFileStorageRobolectricTest` está en `:editor-de-codigo`). Cero `build.gradle.kts` y cero
    `strings.xml` en el diff.

- [x] **T8. Memoria y guion.**
  - Hecho cuando: `MEMORY.md` registra la spec 024 (estado, decisiones 40-44 del plan y errores a evitar) y la
    spec queda con sus cifras reales; el guion de verificación de `plan.md` se copia al final de `spec.md`.
  - **Hecho.**
