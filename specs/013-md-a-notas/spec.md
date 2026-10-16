# Spec 013 — Del Markdown del editor a la base de notas

Estado: implementada (2026-10-10), T1-T6 cerradas ✅ **VERIFICADA EN DISPOSITIVO por quien Decide (2026-10-16)**: «todo se ve bien».

> **Nota de procedencia.** Encadena con `specs/012-markdown-gfm-editor/` (**cerrada**), que dejó
> preparado el hueco: el tokenizador, el renderizador y la conversión de Markdown a bloques ya
> existen, pero no hay ninguna vía para que un `.md` del editor llegue a una nota. `RF-76` es el más
> alto en uso (spec 012) y `RNF-33` el más alto también.
>
> **Alcance deliberadamente estrecho**: una acción, un destino, ningún ajuste. Lo que se podría
> añadir alrededor (etiquetas, elegir nota existente, exportar de vuelta) queda fuera y enumerado.

## Contexto y objetivo

La spec 012 dejó escribir y **ver** Markdown en el editor de código. Falta lo más evidente: usar ese
texto como **nota**.

Hoy la vuelta existe a medias y por otro camino: el FAB «Importar archivo» de la lista acepta `.md`
(`FileImporter.EXTENSION_MAP`) y lo convierte con `DataBlock.migrateLegacyContent`. Ese camino
**funciona**, pero obligan a salir del editor, buscar el archivo otra vez y elegir entre dos acciones
en un diálogo. Si alguien está escribiendo un `.md` en el editor, el siguiente paso natural es
convertirlo, no volver al principio.

El editor de código es una librería (`:editor-de-codigo`) que **no conoce** la base de notas:
vive en su propia base Room (`code_tools_db`) y su contenido está en disco, no en notas. Por eso la
acción tiene que ser un **callback** que el anfitrión aporta: la librería ofrece «aquí tienes el
nombre y el Markdown», y `:app` decide qué hacer con eso.

## Usuarios

- Quien escribe documentación en el editor y la quiere además como nota: un paso, sin salir.
- Quien ya tiene notas en Markdown y edita el `.md` aparte: quiere que la nota se cree **desde** el
  archivo, con su nombre.

## Definiciones

- **Anfitrión**: la app que monta la librería (`:app`). Es quien tiene el `NotesViewModel` y quien
  escribe en la base de notas.
- **Traspaso**: el paso de datos del `CodeEditorActivity` al `MainActivity`. No es un traspaso de
  archivos, sino el de un pequeño paquete con nombre y contenido.

## Requisitos funcionales

- **RF-76**: CUANDO el archivo activo es Markdown y hay un anfitrión conectado, EL SISTEMA ofrece la
  acción «Enviar a notas». Verificable: el item de menú existe **solo** si `onSendToNotes != null` y
  el lenguaje es Markdown; con otro lenguaje o sin anfitrión no aparece.

- **RF-77**: CUANDO se elige «Enviar a notas», EL SISTEMA guarda primero el archivo en disco y después
  convierte. Verificable: la conversión usa el texto **actual**, no el de disco, y el guardado ocurre
  antes de que se emita el callback.

- **RF-78**: CUANDO la conversión termina, EL SISTEMA crea **una nota nueva** con el nombre del
  archivo como título y la abre en el editor de notas. Verificable: `NotesViewModel` recibe
  `id = 0` (inserción, no actualización) y el título es el nombre del archivo **sin la extensión**.

- **RF-79**: SI la conversión no puede hacerse (por ejemplo, el motor falla), ENTONCES EL SISTEMA no
  crea ninguna nota, lo dice con un mensaje y **no** navega. Verificable: el camino de error no llama
  a `saveNoteAndGetId` ni al navegador.

- **RF-80**: CUANDO la acción se usa desde la pestaña del editor dentro del editor de notas, EL
  SISTEMA crea la nota y la abre **en una pestaña** de esa misma pantalla, sin lanzar otra Activity.
  Verificable: el anfitrión de la pestaña usa `OpenNoteTabs.openNote` + `selectTabByKey`, y no
  `startActivity`.

- **RF-81**: CUANDO la acción se usa desde `CodeEditorActivity`, que vive **fuera** del flujo de
  bloqueo de la bóveda, EL SISTEMA **no** escribe en la base de notas desde ahí: entrega el contenido
  a `MainActivity`, que sí respeta el bloqueo. Verificable: `CodeEditorActivity` no referencia el DAO
  ni el `NotesViewModel`; solo escribe un fichero en `cacheDir` y arranca `MainActivity`.

- **RF-82**: CUANDO `MainActivity` recibe el traspaso, EL SISTEMA crea la nota, la abre y **borra el
  fichero de traspaso**. Verificable: tras consumirlo, el fichero no existe, y un traspaso con la
  aplicación bloqueada no crea nada hasta que se desbloquea.

- **RF-83**: EL SISTEMA convierte el Markdown a bloques por la **misma vía** que usa el importador de
  la lista, para que el resultado sea idéntico al de importar el archivo desde el FAB. Verificable:
  ambos caminos llaman a `FileImporter.markdownToBlockContent`, y una prueba compara los dos
  resultados sobre el mismo texto.

## Requisitos no funcionales

- **RNF-33 — Un solo escritor de notas.** Ningún camino escribe en `NoteDao` por su cuenta: todo pasa
  por `NotesViewModel.saveNoteAndGetId`, que ya copia adjuntos, cifra y refresca widgets. Verificable:
  el código nuevo no llama a `noteDao.insertNote` en ningún sitio.

- **RNF-34 — El traspaso no viaja en el Intent.** El límite de Binder (~1 MB) se queda corto con el
  bloque JSON de una nota grande, y un Intent que revienta es una pantalla en blanco. Verificable: el
  `Intent` lleva **una ruta**, no el contenido, y el contenido va en un fichero de `cacheDir`.

- **RNF-35 — El fichero de traspaso no sobrevive al consumo.** Se borra después de leerse y también
  cuando se descarta por estar bloqueada la app, para que un Markdown no se quede indefinidamente en
  `cacheDir`. Verificable: tras el consumo, `File.exists()` es `false`.

- **RNF-36 — Nada de datos del usuario en logs.** La ruta del fichero de traspaso **no** se registra:
  su nombre lleva parte del nombre del archivo, que es del usuario. Verificable: `Log.e` del camino
  nuevo registra la **clase** de la excepción, nunca su mensaje.

- **RNF-37 — Sin dependencias nuevas y sin campos nuevos.** Esta spec no añade librería, ni toca
  `DataBlock`, ni cambia el esquema de Room. Verificable: `git diff` de `app/build.gradle.kts`,
  `libs.versions.toml`, `NoteDatabase.kt` y `DataBlock.kt` vacío.

- **RNF-38 — La lógica de conversión es pura y probada.** `markdownToBlockContent` no toca
  `ContentResolver` ni necesita un `Uri`, solo texto. Verificable: su prueba corre sin Robolectric.

## Casos límite

1. **Archivo sin extensión o con nombre vacío.** El título sale del nombre del archivo; si está en
   blanco, se usa un nombre por defecto, nunca una cadena vacía.
2. **Markdown vacío.** Se crea la nota igualmente, con el bloque vacío, en vez de fallar: el usuario
   pidió crearla.
3. **Título duplicado.** Se crea una nota **nueva** aunque exista otra con el mismo título. Decidido
   por la persona usuaria: evitar el diálogo y la búsqueda es preferible a la sorpresa de **sobrescribir**
   una nota que el usuario no sabía que existía.
4. **Contenido con anexos `![](…)` y `[](…)`.** Se copian a `filesDir` por la vía que ya usa
   `saveNoteAndGetId`; no hay conversión de rutas especial.
5. **El nombre del archivo tiene ruta (`carpeta/notas.md`).** El título es solo el nombre de
   fichero, sin directorios: una nota no debe heredar la carpeta del almacenamiento.
6. **La app está bloqueada y llega un traspaso.** No se crea nada hasta que se desbloquea, y el
   fichero no se borra hasta entonces.
7. **Dos «Enviar a notas» seguidos.** Cada uno crea su nota; el segundo no pisa al primero.

## Fuera de alcance

- **Elegir o buscar una nota existente** para actualizarla en vez de crear otra.
- **Etiquetas, color o categoría** en el envío: la nota nace con lo mínimo.
- **Exportar una nota a `.md`** y volver a importarla: la conversión inversa ya existe
  (`RichTextConverter.contentToMarkdown`) pero no se encadena con esto.
- **Markdown que no sea GFM** (`.txt` disfrazado): el envío está limitado a Markdown a propósito.
- **Sincronización en la nube** del envío: la nota creada entra por el camino normal y se sincroniza
  como cualquier otra.
- **Deshacer el envío**: una nota creada se borra como cualquier otra.

## Criterios de finalización

- [x] `./gradlew :app:testDebugUnitTest --tests '*Markdown*' --rerun` en verde.
- [x] `git diff -- app/build.gradle.kts gradle/libs.versions.toml` vacío (RNF-37).
- [x] `grep -rn "noteDao.insertNote" app/src/main/java/com/example/ui/` no devuelve nada nuevo (RNF-33).
- [x] `grep -rn "Log\." ` en el camino nuevo no imprime contenido del Markdown (RNF-36).
- [x] `./gradlew :app:assembleDebug` sin errores.
- [x] Los strings nuevos están en los **9 locales** de `:app`.
- [ ] `MEMORY.md` registra la regla del traspaso.

## Dudas abiertas

Ninguna. Las decisiones que podían tomarse por dos caminos —si reutiliza una nota o crea una nueva,
si el contenido viaja por Intent o por fichero, si el editor de código escribe en la base de
notas— las respondió la persona usuaria antes de redactar, y están argumentadas en el plan.