# Spec 025 — Un tema para toda la app

Estado: **implementada y verificada en dispositivo** (2026-10-17), T1–T11 cerradas. Guion al final.

> **Nota de procedencia.** No encadena con ninguna spec anterior. Los RF más altos en uso son `RF-158` y
> `RNF-98` (spec 024), así que aquí empiezan en `RF-159` y `RNF-99`.
>
> **Cero dependencias nuevas.** El catálogo vive en un módulo Gradle nuevo, no en una librería externa: todo lo
> que necesita (Compose `Color`/`ColorScheme`) ya está en el APK. La regla 1 de la constitución pide justificar
> cada `implementation(...)`; aquí no hay ninguna, solo una frontera interna más (ver «Fuera de alcance»).
>
> **Una decisión previa ya cerrada con quien pidió el cambio:** los bloques de código **dentro de las notas**
> también siguen al tema elegido. Su consecuencia se acepta a conciencia y está en «Casos límite».
>
> **Cuatro decisiones cerradas antes de escribir esto** (no son dudas abiertas):
> 1. El catálogo va en un **módulo compartido nuevo** (`:tema`), no en la carpeta `theme/` de `:app` — `:app`
>    depende de `:editor-de-codigo`, no al revés, así que un catálogo en `:app` dejaría al editor fuera.
> 2. Cada tema fijo trae **claro y oscuro**; el ajuste Sistema/Encendido/Apagado sigue decidiendo cuál se usa.
> 3. Elegir tema en Ajustes cambia **también** el resaltado del editor, y el selector propio del editor
>    desaparece: un solo camino para lo mismo.
> 4. El selector incluye **Dinámico (Material You)**, que es lo que hace el interruptor que se elimina.

## Contexto y objetivo

Hoy la app tiene un solo tema —dos colores morados— y, encima, un interruptor de «Colores dinámicos» que está
encendido por defecto. Los únicos temas de verdad del proyecto viven en el editor de código
(`SyntaxTheme`, cuatro paletas de resaltado), y **solo las usa el editor**: la app las ignora.

El resultado es que la misma app tiene cuatro temas que casi nadie ve y una paleta única que se aplica a todo.
Y el interruptor es una puerta doble: se puede activar Material You y, con ello, perder los colores propios sin
saberlo, sin dejar elegir ninguno.

El objetivo es uno: **que el tema sea una elección de la persona usuaria, no un interruptor**. Un catálogo de
temas visible en Ajustes, con los del editor incluidos, que pinte **toda** la app —pantallas, burbuja flotante,
visor de PDF, widget, editor de código y bloques de código de las notas— y que se recuerde entre sesiones.

## Usuarios

- Quien ya usa la app y quiere los colores que usa en su editor de código también en sus notas y pantallas.
- Quien no quiere Material You y hoy depende de un interruptor que no dice qué colores son los de verdad.
- Quien tiene el fondo del sistema configurado con una paleta y quiere que la app la siga.

## Historias de usuario

- HU-1. Como persona usuaria, quiero elegir entre varios temas en Ajustes para que la app se vea como yo elijo
  y no como el sistema o como el proveedor.
- HU-2. Como persona usuaria, quiero que el tema elegido pinte **todo**, editor incluido, para no tener dos
  paletas distintas en la misma sesión.
- HU-3. Como persona usuaria, quiero que mi elección se recuerde al cerrar la app, para no tener que elegirla
  cada vez que la abro.
- HU-4. Como persona usuaria, quiero que quien ya usaba la app no vea **ningún** cambio sin tocar nada, para no
  perder mis ajustes.

## Definiciones

- **Tema**: una entrada del catálogo con identificador, nombre y —si es fijo— un esquema Material claro, un
  esquema Material oscuro y una paleta de resaltado de código.
- **Paleta de resaltado**: los 22 colores con los que el editor y los bloques de nota pintan el código
  (la que hoy se llama `SyntaxTheme`).
- **Tema fijo**: un tema con colores propios. **Dinámico**: el tema sin colores propios, que toma los del
  sistema (Material You).
- **Modo**: el ajuste actual de tema oscuro (Sistema / Encendido / Apagado), que se conserva tal cual.

## Requisitos funcionales

- **RF-159. EL SISTEMA ofrece un catálogo de temas compartido** por la app y el editor de código, accesible
  desde los dos sin copiar paletas ni depender de una copia.
- **RF-160. EL CATÁLOGO contiene exactamente seis temas**: `dynamic` (Dinámico), `default` (Predeterminado,
  el morado actual) y los cuatro del editor (`vscode_dark`, `one_dark`, `monokai`, `github_light`).
- **RF-161. CADA tema fijo declara un esquema Material claro y un esquema Material oscuro**, derivados de los
  colores de su familia, y **el tema Dinámico no declara ninguno**: los toma del sistema.
- **RF-162. CUANDO se elige el tema activo, ENTONCES TODA la app se pinta con él**: pantalla principal,
  editor de notas, chat, búsqueda, bloqueo, configuración, informe de fallos, burbuja flotante, visor de PDF
  y configuración del widget.
- **RF-163. EL SISTEMA guarda el tema elegido y lo restaura** al volver a abrir la app, en cualquier
  superficie, incluida la burbuja flotante y el widget, que no pasan por la pantalla principal.
- **RF-164. CUANDO no hay tema guardado y el interruptor antiguo de colores dinámicos estaba apagado, ENTONCES
  el tema activo es `default`** (lo que esa persona estaba viendo, que no es Material You).
- **RF-165. CUANDO no hay tema guardado y no hay interruptor antiguo, o el identificador guardado no existe en
  el catálogo, ENTONCES el tema activo es `dynamic`**, que es lo que la app muestra hoy por defecto.
- **RF-166. CUANDO se abre el editor de código, ENTONCES su paleta de resaltado es la del tema activo**, sin
  que el editor tenga forma de cambiarla por su cuenta.
- **RF-167. CUANDO se muestra un bloque de código dentro de una nota, ENTONCES sus colores de resaltado son los
  del tema activo**, y el color base del texto y el fondo los sigue poniendo el tema Material del bloque.
- **RF-168. CUANDO el tema activo es `dynamic`, ENTONCES el editor y los bloques de nota usan las paletas que ya
  usan hoy**, porque un tema sin colores propios no puede tener paleta propia.
- **RF-169. EN Ajustes → Aspecto, el sistema muestra un selector de tema** con una fila por tema, cada una con
  dos muestras de color de la paleta y una marca en la activa, pulsable y con descripción accesible.
- **RF-170. EN Ajustes → Aspecto, ya no hay interruptor de colores dinámicos**, y sus dos textos desaparecen de
  la app porque nada los muestra.
- **RF-171. EL SISTEMA conserva el ajuste de modo (Sistema / Encendido / Apagado)** y sigue decidiendo cuál de
  los dos esquemas del tema activo se usa; un tema nunca cambia el modo por sorpresa.
- **RF-172. EL SISTEMA no tiene un segundo tema propio en la biblioteca del visor de PDF**: ese tema, sin
  usuarios, deja de existir, de modo que el nombre del tema de la app no pueda ambiguo en el classpath.

## Requisitos no funcionales

- **RNF-99. Cero dependencias nuevas.** El módulo compartido se apoya solo en lo que ya está en el APK
  (Compose `Color` y `ColorScheme`). Ni una `implementation(...)` nueva.
- **RNF-100. Toda decisión comprobable sin `Context` vive en una función pura del catálogo**, incluida la
  elección de esquema por modo, la traducción de identificador a tema, la paleta del editor, la paleta del
  bloque de nota y la regla de migración de la preferencia. Ninguna de ellas necesita `android`.
- **RNF-101. El contraste de los esquemas es verificable, no opinable.** Una prueba comprueba, para los
  ocho esquemas fijos, que el texto sobre su superficie y el color principal sobre su superficie alcanzan el
  mínimo WCAG AA. Si un esquema no lo alcanza, la prueba falla y el tema no entra.
- **RNF-102. Todo texto visible sale de `strings.xml`**, incluidos los nombres de los seis temas, en los nueve
  idiomas de la app. Ningún nombre de tema es un literal en el código.
- **RNF-103. Mover el catálogo no cambia un solo color.** Las cuatro paletas del editor llegan al módulo nuevo
  con los mismos valores, y `default` conserva hoy sus esquemas morados: una prueba lo comprueba.
- **RNF-104. La migración no necesita red, ni base de datos, ni escribir nada**: leer la preferencia antigua y
  decidir el tema es una función pura.
- **RNF-105. No se toca el contenido de las notas**, ni el formato de bloque de código, ni el esquema de la base
  de datos. Un tema no viaja con la nota.
- **RNF-106. Ninguna superficie de red nueva.** El catálogo es estático: ni descargas, ni androidx Dynamic,
  ni consultas de red.

## Casos límite

- **Tema fijo en modo claro u oscuro siempre resuelve un esquema.** El catálogo garantiza los dos; un tema sin
  ellos no es válido y la prueba de catálogo lo rechaza.
- **Identificador guardado corrupto o de una versión anterior.** Se ignora y se cae a `dynamic` (RF-165): un
  identificador desconocido nunca deja la app sin tema ni la manda a un crash al arrancar.
- **Persona que tenía el interruptor apagado.** Migra a `default` y no ve ningún cambio (RF-164, HU-4).
- **Bloque de nota con tema oscuro y nota clara.** Los colores del tema son los que son: `#CE9178` sobre blanco
  queda justo. Es una consecuencia **aceptada a conciencia** de que el bloque siga al tema, y la razón por la
  que el bloque sigue dejando el color base y el fondo al tema Material.
- **Tema Dinámico en un dispositivo sin colores dinámicos.** El sistema decide el resultado; no es de esta app.
- **Selección de tema desde el widget o la burbuja flotante.** No hay selector ahí: se repintan con el tema
  guardado, igual que con cualquier otro ajuste.
- **Idioma sin traducción de un nombre de tema.** Cae a `values/` como cualquier otra clave, sin pantalla rota.
- **El editor abierto mientras se cambia el tema.** El tema llega como parámetro de la pantalla del editor, así
  que el cambio se ve sin cerrar el archivo.

## Fuera de alcance

- **Los ajustes que ya existen dentro del editor** (tamaño de letra, ajuste de línea, números de línea, vista
  previa, modo): siguen igual y no se guardan como hasta ahora.
- **Tipografías y temas de texto**: nada de elegir fuente.
- **Los colores propios de cada nota** (`getNoteBackgroundColor`): siguen siendo los de siempre, sin relación con
  el tema.
- **Colores del chat de IA, del resaltado de fórmulas o de los hashtags**: no se tocan.
- **Previsualización en vivo mientras se recorre la lista**: el tema se aplica al elegirlo.
- **Guardar el tema por nota**: un tema es de la app, no del documento.
- **Migrar `visor-pdf` a Material 3 con su propio tema de marca**: si el tema duplicado sobraba, se quita
  (RF-172); no se diseña uno nuevo.

## Criterios de finalización

1. Los seis temas se ven en Ajustes → Aspecto, con muestra de color y marca de activo (RF-169). **Hecho.**
2. Elegir cualquiera repinta la app entera, editor de código y bloques de nota incluidos (RF-162, RF-166, RF-167).
   **Hecho.**
3. Cerrar y abrir la app mantiene el tema, también en el widget y en la burbuja flotante (RF-163). **Hecho.**
4. Quien tenía el interruptor apagado no ve cambios; quien no tenía nada sigue viendo Material You (RF-164,
   RF-165, HU-4). **Hecho.**
5. Ya no hay interruptor de colores dinámicos en Ajustes (RF-170). **Hecho.**
6. El ajuste de modo sigue funcionando con los seis temas (RF-171). **Hecho.**
7. Las cuatro paletas del editor conservan sus valores exactos y `default` conserva sus morados (RNF-103).
   **Hecho:** `ThemeCatalogPalettesTest` escribe los 22 valores de cada paleta.
8. La prueba de contraste de los ocho esquemas pasa (RNF-101). **Hecho:** `TonalSchemeContrastTest`; el más bajo
   de los ocho es 5.5:1 y todos los demás están por encima de 6:1.
9. `./gradlew :tema:test :editor-de-codigo:testDebugUnitTest :app:testDebugUnitTest` sin errores nuevos sobre la
   línea base; en este entorno la suite con Robolectric sigue sin poder ejecutarse. **Hecho:** `:tema` **53/0**,
   `:editor-de-codigo` **149/2** y `:app` **786/41** (base `746/41`), los mismos fallos y ninguno nuevo.

## Verificación

Implementada y compilada. `:tema` 53 pruebas en verde (las primeras del repo que corren sin Robolectric),
`:editor-de-codigo` 149 con los 2 fallos de plataforma de siempre, `:app` 786 con los 41 de siempre —38 de
plataforma y los 3 preexistentes de `HtmlExportMediaTest` y `OnDeviceModelSourceTest`—, o sea **ninguno nuevo**.
`:app:assembleDebug` y `:visor-pdf:assembleDebug` compilan.

El guion de dispositivo está en `verificacion.md` y se recorrió entero el 2026-10-17: **se ve bien**.

## Dudas abiertas

Ninguna. Las cuatro decisiones de diseño que podían interpretarse de varias formas se cerraron antes de
escribir esto y están al principio del documento.
## Guion de verificación en dispositivo (verificado el 2026-10-17)

Lo que aquí **no** se puede comprobar es el aspecto de un tema: ni con pruebas ni sin dispositivo. El guion
completo está en `verificacion.md` (26 puntos), y está escrito empezando por lo único que puede hacer fracasar
esta spec —**que quien ya usaba la app no vea ningún cambio**— y terminando por la robustez.

Los cuatro puntos que más importan, y por qué son estos:

1. **El aspecto no cambia** (RF-164, RF-165, HU-4). Con la app tal como estaba: el morado, el editor en VS Code
   Dark Modern y los acentos de siempre en los bloques de nota. Sin este punto, todo lo demás da igual.
2. **El tema llega a todas partes** (RF-162, RF-163, RF-166, RF-167). Los seis temas en claro y en oscuro, y el
   tema activo llegando también al visor de PDF, al widget, a la burbuja flotante, al editor y a los bloques de
   nota de las notas.
3. **Se recuerda** (RF-163). Cerrar la app del todo, no solo la Activity, y volver a abrirla.
4. **Cambiar con las cosas abiertas** (RF-167). Con el editor abierto y con una nota que tenga un bloque de
   código: si el bloque se queda con los colores viejos, es que la paleta no entró en la clave del `remember`, y
   eso solo se ve aquí.
