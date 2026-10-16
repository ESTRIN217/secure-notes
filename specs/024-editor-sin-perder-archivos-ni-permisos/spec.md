# Spec 024 — Los archivos del editor no se pierden: ni en el administrador ni al reiniciar

Estado: **implementada y verificada en dispositivo** (2026-10-16), T1-T8 cerradas. Guion al final.

> **Nota de procedencia.** No encadena con ninguna spec anterior. Los RF más altos en uso son `RF-151` y
> `RNF-91` (spec 023), así que aquí empiezan en `RF-152` y `RNF-92`.
>
> **No añade dependencias.** La app ya depende en un solo sentido del editor (spec 022), así que leer su índice
> de recientes no estrena ninguna coordenada.
>
> **No cambia el formato de los datos.** Los recientes del editor y las notas se guardan igual; no hay
> migración. La pantalla del administrador sigue mostrando las mismas categorías.
>
> **Dos defectos, una misma raíz.** El editor de código dejó a medias su integración con el almacenamiento: el
> administrador no sabe que sus archivos existen (y los borra) y el editor no guarda el permiso del
> almacenamiento compartido (y los pierde al reiniciar). Se documentan juntos porque se descubrieron juntos y
> viven en el mismo borde editor ↔ almacenamiento, pero cada uno se implementa y se prueba por separado.

## Contexto y objetivo

El editor de código guarda los archivos en una carpeta propia (`filesDir/code_files`) y en su base de datos
(`code_tools_db`, tabla `code_files`) solo conserva el índice de recientes: nombre, ruta local y/o URI externa.
Hay dos consecuencias que hoy son defectos:

1. **El administrador de almacenamiento borra lo del editor.** El analizador recorre `filesDir`, decide que un
   archivo es «huérfano» si ninguna **nota** lo referencia y lo ofrece para borrado. Los archivos del editor no
   los referencia ninguna nota —los referencia el editor—, así que quedan marcados como huérfanos y «Borrar
   huérfanos» los elimina. El índice de recientes del editor sigue apuntando a archivos que ya no están.

   El detalle importa: la carpeta `code_files` cae en la categoría `OTHER`, y `OTHER` **sí** entra en la lista
   de categorías que pueden ser huérfanas. No es un archivo suelto: con la limpieza automática activada, el
   borrado puede ocurrir sin que nadie lo pida en el momento.

2. **El editor pierde el permiso del almacenamiento compartido al reiniciar.** Cuando se abre un archivo o una
   carpeta con el selector del sistema, el editor se queda con la URI (`content://…`) y la guarda en los
   recientes, pero nunca pide al sistema que **persista** el permiso. Mientras la app vive, la lectura funciona;
   al cerrarla y volver a abrirla, la URI sigue en los recientes pero el permiso ya no, la lectura falla y el
   archivo se abre **vacío** (el fallo se traga y devuelve cadena vacía).

El objetivo es que el editor no pierda nada de lo suyo: ni sus archivos (el administrador deja de ofrecerlos) ni
el acceso a los archivos que abrió del almacenamiento compartido (el permiso se guarda).

## Usuarios

- Quien crea y edita archivos con el editor de código: espera encontrarlos donde los dejó.
- Quien usa el administrador de almacenamiento para liberar espacio: espera que lo que se borre esté **de
  verdad** huérfano.
- Quien abre un archivo del almacenamiento compartido (Descargas, Drive, una carpeta) y lo edita: espera poder
  volver a abrirlo sin elegirlo otra vez.

## Historias de usuario

- **HU-1.** Como quien trabaja con el editor de código, quiero que el administrador de almacenamiento no borre
  mis archivos para no perder trabajo.
- **HU-2.** Como quien libera espacio, quiero que el borrado de huérfanos respete los archivos que el editor
  todavía lista, para no borrar por error algo que se puede volver a abrir.
- **HU-3.** Como quien abre un archivo del almacenamiento compartido, quiero poder volver a abrirlo después de
  cerrar la app, sin volver a buscarlo en el selector.

## Definiciones

- **Archivo del editor:** fichero físico que vive en la carpeta del editor (`filesDir/code_files`) y que figura
  en su índice de recientes.
- **Recientes del editor:** el índice de metadatos que el editor guarda en su base de datos (`code_tools_db`,
  tabla `code_files`): nombre, ruta local y/o URI externa, idioma, fechas.
- **Referenciado:** que algo lo use. Un archivo está referenciado si lo usa una nota (adjunto, imagen de fondo,
  medio del contenido) **o** si figura en los recientes del editor.
- **Huérfano:** archivo en disco que no está referenciado por nada. Solo los huérfanos se ofrecen para borrado.
- **Permiso persistente:** autorización que el sistema concede sobre una URI del almacenamiento compartido y que
  sobrevive a que se cierre la app.

## Requisitos funcionales

- **RF-152.** CUANDO el administrador de almacenamiento decide si un archivo es huérfano, EL SISTEMA debe
  considerar referenciados también los archivos que figuran en los recientes del editor de código.
- **RF-153.** SI un archivo vive en la carpeta del editor y figura en sus recientes, ENTONCES EL SISTEMA NO debe
  marcarlo como huérfano ni ofrecerlo para borrado (ni en la lista ni en el borrado automático).
- **RF-154.** EL SISTEMA solo debe ofrecer para borrado un archivo que no esté referenciado ni por una nota ni
  por los recientes del editor.
- **RF-155.** CUANDO el editor recibe del sistema la URI de un archivo o de una carpeta del almacenamiento
  compartido, EL SISTEMA debe guardar el permiso persistente de esa URI.
- **RF-156.** MIENTRAS exista el permiso persistente, EL SISTEMA debe poder volver a leer el archivo tras cerrar
  y reabrir la app.
- **RF-157.** SI el sistema no concede el permiso persistente (por ejemplo, la URI viene de otra app y no es
  persistible), ENTONCES el archivo se importa igual que hoy y el fallo queda registrado, sin interrumpir la
  operación ni mostrarlo al usuario.
- **RF-158.** EL SISTEMA solo debe pedir el permiso persistente para URIs del almacenamiento compartido
  (`content://`); una ruta local (`file://` o ruta absoluta) no lo necesita.

## Requisitos no funcionales

- **RNF-92.** La decisión de **qué** rutas locales protege el editor debe vivir en una función **pura**, sin
  `android` ni `androidx`, para poder probarse sin dispositivo (regla de `AGENTS.md`). El `Context` se queda en
  quien consulta el índice.
- **RNF-93.** Consultar los recientes del editor no debe correr en el hilo principal.
- **RNF-94.** La consulta debe tolerar que la base del editor no exista, esté vacía o se haya recreado: sin
  recientes, cero rutas protegidas, sin excepción y sin que el escaneo del administrador falle.
- **RNF-95.** Cero dependencias nuevas: `git diff` no toca ningún `build.gradle.kts`.
- **RNF-96.** No se añaden textos visibles nuevos: ni `strings.xml` ni los 9 locales. El único aviso del permiso
  no concedido va al registro (RF-157).
- **RNF-97.** El comportamiento actual del administrador no cambia para nada que no sea del editor: los
  huérfanos de notas se siguen detectando igual (RF-154).
- **RNF-98.** Pedir el permiso persistente de una URI ya persistida no debe fallar ni duplicar nada.

## Casos límite

- **Recientes vacíos o base recién creada:** nada protegido, nada roto (RNF-94).
- **Fila de recientes borrada desde el editor:** el archivo físico se borra con ella; si quedara en disco, vuelve
  a ser huérfano de verdad (RF-154).
- **Ruta guardada como `file://…` en vez de ruta absoluta:** se protege igual; el administrador normaliza la
  forma antes de comparar.
- **Fila de recientes que solo tiene URI externa (sin ruta local):** no aporta ninguna ruta dentro de `filesDir`,
  así que no protege nada del escaneo (y no debe hacerlo).
- **URI que viene por «abrir con»/«compartir» desde otra app:** puede no ser persistible; la importación sigue y
  el fallo queda registrado (RF-157).
- **Permiso revocado luego por el usuario en Ajustes:** la lectura falla como hoy; el editor no recupera el
  acceso por su cuenta.
- **Archivo del editor que además es grande:** sigue apareciendo en la sección de archivos grandes; borrarlo ahí
  es una acción explícita del usuario y no la cambia esta spec.

## Fuera de alcance

- Una categoría o sección propia para los archivos del editor dentro del administrador (siguen contando en el
  total de «otros», pero no como huérfanos).
- Liberar el permiso persistente cuando se borra la fila de recientes (`releasePersistableUriPermission`).
- Recuperar el acceso a URIs que el sistema no concedió como persistibles.
- Limpiar de los recientes las filas cuya ruta ya no existe.
- Cifrar los archivos del editor.
- Cambiar cuándo el editor reabre solo el último archivo al arrancar.

## Criterios de finalización

- Pruebas puras en verde en `:editor-de-codigo` y `:app` (RNF-92). **Hecho:** `:editor-de-codigo` **149/2**
  (base `136/2`, +13) y `:app` **746/41** (base `739/41`, +7); los fallos son **los mismos** y de plataforma.
- `./gradlew :app:compileDebugKotlin` y `./gradlew :editor-de-codigo:compileDebugKotlin` compilan. **Hecho.**
- `git diff` no toca ningún `build.gradle.kts` (RNF-95) y no añade claves a ningún `strings.xml` (RNF-96).
- Verificación en dispositivo (guion al final del `plan.md`): el archivo del editor sobrevive a «Borrar
  huérfanos» y un archivo del almacenamiento compartido se relee tras cerrar la app.

## Dudas abiertas

> Resueltas por quien decide el 2026-10-16, antes de redactar el plan. Se conservan para que la respuesta quede
> escrita.

- **¿Sección propia en el administrador?** No. Los archivos del editor solo se **excluyen** del borrado de
  huérfanos (RF-152, RF-153). Siguen contando en el total de «otros», sin sección ni categoría nueva.
- **¿Lectura o lectura y escritura?** **Lectura y escritura** (RF-155). Es lo que permite releer el archivo tras
  reiniciar **y** seguir guardando en el original con el botón guardar; si el proveedor solo concede lectura,
  se cae a lectura y se sigue (RF-157).

## Guion de verificación en dispositivo (verificado el 2026-10-16)

Los dos comportamientos que **no** se pueden comprobar en esta plataforma (el permiso persistente lo concede el
sistema; el guard de la base necesita Robolectric, que aquí no corre) se verifican así:

1. **Editor de código** → crear un archivo (`prueba.md`), escribir y guardar.
2. **Ajustes → Almacenamiento** → `prueba.md` **no** debe aparecer en «Archivos huérfanos» (RF-153). Pulsar
   «Borrar huérfanos» y, si procede, «Limpiar ahora» con la limpieza automática activada.
3. **Volver al editor** → el archivo sigue abriéndose con su contenido (HU-1).
4. Abrir con el **selector del sistema** un archivo de Descargas y editarlo.
5. **Cerrar la app por completo** (quitarla de recientes) y volver a abrirla.
6. **Abrir ese archivo desde los recientes del editor** → se ve el contenido (antes se abría vacío) (RF-156).
