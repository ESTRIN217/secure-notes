# Spec 006 — Rediseño de la pantalla de chat con IA

Estado: **aprobada** (2026-10-05), con el plan aprobado en la misma puerta. Antes de eso pasó **dos
vueltas de revisión** con los 20 + 12 puntos y los 11 + 12 menores resueltos (ver «Revisión QA»), y una
**corrección posterior a la implementación** que ajustó cuatro criterios demostrados imposibles
(ver «Correcciones posteriores a la implementación»).

> **Nota de procedencia.** Esta spec es una hija de `specs/001-limites-constitucion/spec.md`
> (**aprobada**), igual que la 005. Hereda sin modificar **RF-3, RF-19, RF-20** y **RNF-4, RNF-5,
> RNF-6**, más los principios 1 (sin dependencias nuevas), 3 (lógica fuera de la interfaz), 4 (tests
> primero) y 6 (un idioma por capa) de `docs/constitution.md`.
> Los `RF-1`…`RF-5` del encargo original se numeran aquí **RF-33**…**RF-41** por continuidad del
> espacio de nombres: `RF-20` era el identificador más alto en uso en la madre y `RF-32` el más alto de
> la spec 005. Las `RNF` nuevas empiezan en **RNF-10**, el número más alto en uso hasta ahora.

## Contexto y objetivo

La parte de escritura del chat con IA se ha rediseñado **fuera del flujo de la spec**: tres archivos
nuevos (`FloatingChatInputField`, `EmptyChatWelcome`, `AdjuntosAiBottonSheet`) son maquetas del diseño
que se quiere adoptar —campo flotante redondeado con el botón «+» integrado, hoja inferior de adjuntos
y pantalla de bienvenida con saludo y sugerencias en fila— y **no se podían construir tal cual**: los
**siete** errores de compilación estaban **todos dentro de los `@Preview`** de las tres maquetas
(`EmptyChatWelcome.kt:91`, `FloatingChatInputField.kt:143` y `:145`), y **los borró la persona usuaria
antes de empezar la implementación**. El cuerpo de las tres maquetas compilaba.

Conviene decir **qué no era un bloqueo**, porque se creyó lo contrario durante dos vueltas: los dos
`fun EmptyChatWelcome` de `com.example.ui` **no** impedían compilar. Son **dos sobrecargas homónimas
con firmas distintas** —la vieja `(selectedText, fullContent, hasAttachments, onSuggestion,
onSummarize)` y la del prototipo `(userName, onSuggestionSelected, modifier)`—, y la llamada resolvía a
la vieja por argumentos con nombre; Kotlin lo acepta sin quejarse. Verificado: `./gradlew
:app:assembleDebug` dio `BUILD SUCCESSFUL` **antes de tocar nada**. Descolisionar el nombre sí era
necesario —RF-39 quiere **una sola** pantalla de bienvenida, y con la firma nueva la vieja habría
quedado como duplicada muerta—, pero no era una urgencia de compilación.

Adoptar las maquetas tal cual, sin más, **perdería cosas que hoy funcionan**. No son detalles: son
comportamientos que la persona usuaria ya usa y que las maquetas no reproducen.

1. **Se dejaría de poder enviar con solo adjuntos.** Hoy el botón de enviar está habilitado con
   texto vacío si hay adjuntos pendientes, y la comprobación que impide el envío vacío también
   contempla ese caso. La maqueta habilita el botón solo con texto no vacío o generando.
2. **Se perdería lo tecleado durante la generación.** Hoy el campo **no acepta escritura mientras el
   modelo responde**, y el botón se convierte en «detener». La maqueta deja el campo siempre editable,
   de modo que lo escrito durante la respuesta se queda tecleado y se pierde al pulsar detener.
3. **Se perdería el texto de ayuda dinámico.** Hoy el marcador de posición cambia: si hay historial, es
   el de seguimiento; si no, el de la acción activa (generar, resumir, corregir).
4. **Se perdería la edición de un mensaje ya enviado.** Hoy, editar un mensaje y volver a enviarlo
   **vuelve a preguntar** con la acción de generación en lugar de crear un mensaje nuevo, y después
   limpia la edición.
5. **Se perdería el reparto de la escritura entre los dos estados de texto del campo.** Hoy el campo
   muestra el mensaje en edición si lo hay y si no el texto normal, y su manejador de escritura
   enruta al mismo estado del que lee; al entrar en edición se fijan los dos estados. Una maqueta que
   solo pase el texto normal compila, cumple los criterios aparentes y **se rompe en la primera tecla**.
6. **Se perdería el vaciado del campo tras enviar.** Hoy, al enviar con la acción de generación, el
   texto se limpia. Un campo que lee su valor de fuera y no recibe el vaciado deja el texto escrito
   después de enviar.
7. **Se perdería la realimentación del envío:** el color del botón animado, el contenedor de color de
   error en modo detener, la vibración al enviar y el rebote de escala del botón.
8. **Se perderían las fichas de adjuntos pendientes** con su tipo de contenido estable y la posibilidad
   de quitar **una** de ellas por su posición.
9. **Se perderían las descripciones accesibles de los botones.** Hoy las tres salen de los recursos;
   la maqueta las borra (`contentDescription = null`) y escribe un literal (`"attach"`), lo que además
   es texto visible fuera de los recursos.
10. **Se perdería la elevación de la barra** (elevación tonal de 3 dp y sombra de 8 dp): la maqueta la
    sustituye por un color de superficie sin sombra.
11. **Se perdería la acción dedicada de resumir.** El chip «Resumir» de la bienvenida no envía una
    sugerencia como texto: lanza la acción de resumen, que es distinta de la de generar.

El objetivo es **adoptar el diseño de las maquetas sin perder nada de lo anterior**, dejar el código en
un estado que compile, y hacer que las decisiones nuevas —qué sugerencias se ven, qué acción lanza
cada una, qué saludo aparece, si se puede enviar, qué muestra el campo y adónde escribe, si se limpia
al enviar, qué hace cada opción de la hoja de adjuntos— sean **decisiones de datos comprobables por
pruebas puras**.

## Usuarios

- **Persona usuaria del chat con IA.** Escribe mensajes, adjunta notas y archivos, lanza sugerencias
  en pantalla vacía y edita mensajes ya enviados. No controla el código, así que no puede recuperar
  por su cuenta una función que el rediseño haya borrado.
- **Persona desarrolladora o mantenedora.** Cambia el código del proyecto y necesita poder comprobar
  la lógica de la pantalla sin depender de una pantalla ni del entorno de pruebas, que en la
  plataforma de desarrollo no se ejecuta.

## Historias de usuario

- HU-1. Como persona usuaria, quiero un campo de escritura flotante y redondeado con el botón «+»
  integrado, para que escribir y adjuntar sea una sola zona y no dos controles sueltos.
- HU-2. Como persona usuaria, quiero poder enviar **solo con adjuntos**, sin tener que escribir nada,
  para poder resumir o consultar lo que he adjuntado sin teclear.
- HU-3. Como persona usuaria, quiero que mientras el modelo responde no se pueda escribir, para no
  perder lo que he tecleado al tener que detener la respuesta.
- HU-4. Como persona usuaria, quiero que el campo se quede vacío después de enviar, para no volver a
  enviar lo mismo sin darme cuenta.
- HU-5. Como persona usuaria, quiero que editar un mensaje siga funcionando desde la primera tecla,
  para corregir lo que escribí sin que el campo se quede a medias.
- HU-6. Como persona usuaria, quiero que el botón «+» me ofrezca una hoja con las opciones que de
  verdad funcionan (nota y archivo), sin opciones que no hagan nada.
- HU-7. Como persona usuaria, quiero un saludo de bienvenida y sugerencias que sean datos y no un
  bloque de texto fijo, para no ver siempre la misma pantalla al entrar.
- HU-8. Como persona usuaria, quiero que «Resumir» siga siendo la acción de resumen y no una
  sugerencia escrita como texto, porque resumir con la acción correcta da un resultado distinto.
- HU-9. Como persona desarrolladora, quiero que la lógica de sugerencias, saludo, habilitación del
  envío, estado del campo y opciones de la hoja se pueda ejercitar sin levantar la interfaz, para que
  no vuelva a romperse en silencio.

## Convenciones de nombres

Por RNF-5 y por el principio 6, **todo identificador y todo nombre de archivo de esta entrega va en
inglés**, sin excepciones en castellano:

- La hoja de adjuntos pasa de `AdjuntosAiBottonSheet.kt` a **`AttachmentAiSheet.kt`**, que además
  elimina la errata «Botton».
- Los archivos de lógica que se crean (ver la tabla de lógicas con prueba) llevan nombre en inglés.
- Los identificadores y las funciones van en inglés; los comentarios y la documentación, en español.

## Marcadores de archivo usados en los criterios

Los criterios de abajo usan tres marcadores, fijados aquí para que sean ejecutables:

- **`<campo>`** = el archivo que declara `FloatingChatInputField` (hoy `FloatingChatInputField.kt`,
  nombre en inglés, no cambia).
- **`<hoja>`** = el archivo que declara la hoja de adjuntos, que tras esta entrega se llama
  **`AttachmentAiSheet.kt`**.
- **`<saludo>`** = el archivo que declara la pantalla de bienvenida del chat (hoy
  `EmptyChatWelcome.kt`, nombre en inglés, no cambia).

Los **cuatro archivos de lógica** están nombrados uno a uno en la tabla siguiente y se escriben con su
ruta completa en cada criterio que los menciona: «en cada archivo de lógica» significa **los cuatro**,
nunca un archivo sin ruta.

## Definiciones

- **Sugerencia.** Par de datos con **etiqueta** (lo que se ve) y **acción** (lo que se ejecuta al
  tocarla). No es texto con un manejador: es una entidad. Son **cuatro** en total.
- **Acción de sugerencia.** O bien la acción de **generación** con la etiqueta como petición, o bien la
  **acción de resumen** con el texto seleccionado, el contexto de la nota y la petición de resumen
  establecida. Son las dos que ya existen; no se añade ninguna tercera.
- **Hay algo que resumir.** Que el texto seleccionado no esté vacío, **o** que el contenido de la nota
  no esté vacío, **o** que haya adjuntos pendientes. Son tres condiciones disjuntas, y las tres cuentan.
- **Día.** Número entero de días transcurridos desde época en la zona horaria local. Se calcula **una
  sola vez**, con `remember` **sin claves** en el punto del árbol que pinta la pantalla de bienvenida
  —es decir, no en cada recomposición—, y se pasa a la función pura como parámetro. No se calcula
  dentro de una composición que se recompone, y no se guarda en el ViewModel porque RNF-14 prohíbe
  tocarlo.
- **Saludo.** Frase de bienvenida sin nombre de persona, elegida de un conjunto **cíclico** de al
  menos dos entradas disponibles en el idioma. Reglas de la elección: el mismo día produce **siempre**
  el mismo saludo; días consecutivos producen saludos **consecutivos** mientras el conjunto no se haya
  recorrido entero, y al recorrerlo **empieza de nuevo**. No se promete que dos días cualesquiera
  produzcan saludos distintos: con dos entradas y un recorrido completo eso es imposible, y prometerlo
  haría el requisito insatisfacible.
- **Hoja de adjuntos.** Hoja modal con **exactamente dos** opciones: **Nota** y **Archivo**. No es un
  menú desplegable.
- **Fichas de adjuntos.** Fila horizontal de los adjuntos pendientes, cada uno con su nombre y con la
  posibilidad de quitarlo por su posición.

## Lógicas con prueba (lista única y normativa)

Esta tabla es la **única** lista de decisiones con prueba de la entrega: **siete lógicas en cuatro
archivos**. Todas las referencias a «las lógicas con prueba» del resto de la spec remiten aquí, y
ninguna cuenta a un número distinto.

| # | Qué decide | RF que lo exige | Archivo de lógica (sin interfaz) | Casos mínimos que cubre su prueba |
|---|---|---|---|---|
| L1 | Qué sugerencias se ven, incluida la visibilidad condicional de la de resumen | RF-37, RF-38 | `app/src/main/java/com/example/ui/chat/ChatSuggestions.kt` | sin nada que resumir; con texto seleccionado; con contenido de nota; con adjuntos |
| L2 | Qué petición lanza cada sugerencia | RF-37 | `app/src/main/java/com/example/ui/chat/ChatSuggestions.kt` | una de generación, con su etiqueta como petición; la de resumen, con su acción, su petición, `selectedText`, `context` e idioma objetivo |
| L3 | Qué saludo se elige | RF-36 | `app/src/main/java/com/example/ui/chat/ChatGreeting.kt` | mismo día dos veces; días consecutivos; conjunto recorrido entero; conjunto más pequeño que el de otro idioma |
| L4 | Si se puede enviar y si el envío vacío se descarta; además **qué texto de ayuda corresponde** y **con qué clave se pinta cada ficha** | RF-33 a, c, h | `app/src/main/java/com/example/ui/chat/ChatInputState.kt` | cuatro combinaciones de habilitado; envío vacío; cuatro combinaciones de texto de ayuda; dos adjuntos con el mismo nombre |
| L5 | Qué acción corresponde a cada opción de la hoja | RF-34, RF-35 | `app/src/main/java/com/example/ui/chat/AttachmentSheetOption.kt` | Nota → selector de notas; Archivo → selector de archivos. Los **tipos de archivo no los decide L5**: por decisión D-4 se quedan escritos en la pantalla de chat y los mide su propio criterio |
| L6 | Qué texto muestra el campo, a qué estado enruta la escritura y qué pasa al cargar una edición al abrir la sesión | RF-33 e | `app/src/main/java/com/example/ui/chat/ChatInputState.kt` | sin edición; con edición, al leer y al escribir; edición recién cargada, con y sin edición pendiente. Las **dos vías de entrar en edición** no las cubre esta prueba: son dos manejadores de la pantalla, y las mide el criterio de RF-33 (d, e) |
| L7 | Si el campo se limpia tras enviar | RF-33 f | `app/src/main/java/com/example/ui/chat/ChatInputState.kt` | con la acción de generación; con la acción de resumen o de corrección |

Los cuatro archivos son Kotlin puro: **ninguno** importa `androidx.compose` y **ninguno** accede a
recursos. La función de L4 que decide el texto de ayuda devuelve un **identificador del propio
lógica** (una enumeración o una clase propia), no un identificador de recurso, y el Composable lo
traduce con `stringResource`; por eso ningún archivo de lógica necesita `R`. Ninguno lee el reloj. Por
eso se pueden ejercitar en la plataforma de desarrollo.

## Requisitos funcionales

Heredados de la spec madre, sin modificación:

- **RF-3**: SI una dependencia deja de usarse, ENTONCES EL SISTEMA la retira por completo.
- **RF-19**: EL SISTEMA no elimina, desactiva ni salta ningún caso de prueba para conseguir una suite
  verde, y mantiene el alcance de los existentes.
- **RF-20**: AL terminar la entrega, EL SISTEMA deja la memoria del proyecto en un máximo de 50 líneas y
  registra en ella el estado actual, las decisiones tomadas con su motivo y los errores a evitar.

Nuevos, propios de esta spec:

- **RF-33**: CUANDO se abre la pantalla de chat con IA, EL SISTEMA monta el **campo de entrada
  flotante** como barra inferior de esa pantalla, y ese campo conserva a la vez los diez
  comportamientos del campo al que sustituye:
  - **(a)** se puede enviar con el texto vacío si hay adjuntos pendientes, y el botón de enviar está
    habilitado en ese caso; sin texto ni adjuntos ni generación, el envío vacío **no** lanza petición;
  - **(b)** el campo **no acepta escritura** mientras el modelo está generando, y el botón pasa a
    detener la generación, de modo que no puede perderse texto tecleado durante la respuesta;
  - **(c)** el texto de ayuda es **dinámico**: el de seguimiento si hay historial de conversación, y si
    no, el de la acción activa. **La decisión de cuál de los cuatro es** vive en L4 y devuelve un
    identificador; **el texto** se resuelve en el Composable con recursos. Ninguna clave se escribe
    como texto en el código;
  - **(d)** si se está editando un mensaje ya enviado, al enviar se **vuelve a preguntar con la acción
    de generación** en lugar de crear un mensaje nuevo, y después la edición se limpia;
  - **(e)** el campo **muestra el valor efectivo** —el mensaje en edición si lo hay, y si no el texto
    normal— y su manejador de escritura **enruta al mismo estado del que lee**; al entrar en edición
    se fijan los dos estados, y la rama que carga una edición al abrir la sesión sigue funcionando.
    **Las dos decisiones —qué se muestra y adónde se escribe— las toma L6, y ninguna se resuelve
    dentro del Composable**;
  - **(f)** al enviar con la acción de generación, el campo **se limpia**; con las otras acciones no se
    limpia, como hoy. **La decisión la toma L7**;
  - **(g)** el envío da realimentación: color del botón **animado**, contenedor de color de error
    mientras está en modo detener, vibración al enviar y **rebote de escala** del botón;
  - **(h)** los adjuntos pendientes se muestran como **fichas** que se quitan **una a una por su
    posición**, y se pintan con **clave y tipo de contenido explícitos**, siendo la clave **única por
    construcción** (decisión 7);
  - **(i)** la barra conserva su elevación perceptible: elevación tonal de 3 dp **y** sombra de 8 dp;
  - **(j)** las tres **descripciones accesibles** de los botones —adjuntar, enviar, detener— siguen
    saliendo de los recursos, no de literales ni de `null`.
- **RF-34**: CUANDO se pulsa el botón «+» del campo de entrada, EL SISTEMA abre una **hoja modal de
  adjuntos con exactamente dos opciones, «Nota» y «Archivo»**, y **nada más**; la hoja se puede
  descartar. Verificable: la hoja contiene dos elementos de lista y ni una foto ni una cámara.
- **RF-35**: CUANDO se elige «Nota» en la hoja de adjuntos, EL SISTEMA **cierra la hoja antes de abrir
  el selector de notas** y abre el selector con la búsqueda vacía; CUANDO se elige «Archivo»,
  EL SISTEMA cierra la hoja y abre el selector de archivos con los mismos tipos de archivo que ya se
  ofrecían. Verificable: el mapa opción→acción lo cubre L5, y el **orden** —cerrar antes de abrir— y
  la **búsqueda vacía** se comprueban en la revisión del diff y en dispositivo, no con un comando
  (riesgo 11).
- **RF-36**: CUANDO se muestra la pantalla de bienvenida del chat, EL SISTEMA muestra un **saludo sin
  nombre de persona**, elegido cíclicamente de un conjunto de al menos dos entradas mediante L3, que
  recibe el número de día definido arriba; el mismo día produce siempre el mismo saludo y los días
  consecutivos recorren el conjunto. Verificable: el saludo no lleva ningún nombre concatenado ni lee
  ninguna preferencia, y la prueba cubre los cuatro casos de la tabla.
- **RF-37**: CUANDO se muestra la pantalla de bienvenida, EL SISTEMA resuelve **en ella** las cuatro
  **etiquetas** de sugerencia con recursos y entrega a `onSuggestion` **la copia con la etiqueta ya
  rellena**; CUANDO se toca una sugerencia, EL SISTEMA ejecuta la petición que L2 declara: las de
  generación envían su **etiqueta** como petición con la acción de generación, y la de resumen lanza
  la **acción de resumen** con el texto seleccionado, el contexto de la nota y la petición de resumen
  establecida, **conservando el idioma objetivo**. La **petición de la sugerencia de resumen**
  (`ai_chat_prompt_summarize`, el comportamiento actual) la resuelve **la pantalla de chat**, que se la
  pasa a `requestFor(...)`, y no la pantalla de bienvenida: `ChatSuggestion` lleva `id`, `label` y
  `action`, y ahí no cabe una petición. Verificable: L2 lo cubre con prueba, las cuatro etiquetas se
  comprueban con `grep` en `<saludo>` y la petición con `grep` en la pantalla de chat.
- **RF-38**: MIENTRAS haya texto seleccionado, contenido de nota o adjuntos pendientes, ENTONCES
  EL SISTEMA muestra la sugerencia de resumen; SI no hay nada de eso, ENTONCES EL SISTEMA **no** la
  muestra. Verificable: la visibilidad la decide L1, con prueba para los cuatro casos de la tabla, y
  en pantalla no aparece un botón de resumir sin nada que resumir.
- **RF-39**: CUANDO se abre la pantalla de bienvenida o la hoja de adjuntos, EL SISTEMA **no dibuja dos
  veces** ninguna de las dos: en el paquete de la interfaz hay **una sola** declaración de la pantalla
  de bienvenida, **una sola** declaración de la constante de tipo de contenido de las fichas —y esa
  declaración vive en el archivo de lógica, **no** en la pantalla de chat, para no tocar el bloque de
  código compartido con el selector de notas—, **ningún** preview de las tres maquetas, y ningún
  archivo cuyo nombre contenga la errata «Botton».
- **RF-40**: EL SISTEMA saca **todo** el texto visible de estas tres piezas —saludo, etiquetas de las
  sugerencias, títulos y subtítulos de las opciones de la hoja— de los recursos. Las cuatro
  sugerencias reutilizan claves ya traducidas: `ai_suggestion_poem`, `ai_suggestion_ideas`,
  `ai_summarize` (para la de resumen) y **una clave nueva** para «Explicar concepto». Esta entrega
  añade **cinco claves nuevas como mínimo**, y ninguna puede faltar en un idioma:
  1. la etiqueta de «Explicar concepto»;
  2. y 3. las **dos entradas de saludo** mínimas del conjunto (la segunda se añade si el conjunto
     inicial trae menos de dos);
  4. y 5. los **subtítulos de las dos opciones de la hoja** —hoy no existe ninguna clave para ellos—,
     cuyo texto exacto nombra `plan.md`.
  Esta entrega **rellena además** `attach_note`, `attach_file` y `remove` en el inglés británico, donde
  hoy faltan. Verificable: ninguna de las cadenas que estaban escritas a mano en las maquetas aparece
  en el código, el diff de recursos añade al menos cinco claves nuevas en el idioma por defecto, cada
  clave nueva existe en los nueve idiomas, y las tres claves rellenadas existen también en el inglés
  británico.
- **RF-41**: EL SISTEMA escribe **antes** la prueba de cada una de las siete lógicas de la tabla y
  después la hace existir, y esas siete lógicas se pueden ejercitar **sin la interfaz**. Verificable:
  en **cada uno** de los cuatro archivos de lógica, `grep -c '^import androidx.compose'` devuelve 0, y
  las pruebas se ejecutan en verde con el comando que fija `plan.md`.

## Requisitos no funcionales

Heredados de la spec madre, sin modificar: **RNF-4** (verificabilidad), **RNF-5** (un idioma por capa
en lo que se toca: identificadores, API y registros en inglés; comentarios y documentación en español),
**RNF-6** (sin coste nuevo).

Nuevos, propios de esta spec:

- **RNF-10 — Cero dependencias nuevas.** Esta entrega no añade ninguna dependencia, ni tarea de
  compilación, ni proceso de compilación. Verificable: el diff de `build.gradle.kts`,
  `app/build.gradle.kts` y `gradle/libs.versions.toml` no contiene ninguna dependencia añadida.
- **RNF-11 — Las decisiones viven fuera de la interfaz.** Las siete lógicas de la tabla se deciden en
  funciones puras de 20 líneas o menos, con cláusulas de guarda, y los Composables solo pintan el
  resultado y lanzan eventos. **El límite de 20 líneas del principio 3 acota a las funciones puras y a
  la lógica, no a los Composables**: un Composable que pinta el resultado de una decisión puede ser
  largo, y si se le aplicara el límite literal, ningún Composable de esta aplicación —empezando por los
  que ya existen— podría existir. Verificable: cada una de las siete lógicas existe como función pura
  en su archivo, con prueba, y ninguna está escrita dentro de un `@Composable`.
- **RNF-12 — Comprobable en la plataforma de desarrollo.** Las pruebas nuevas son pruebas puras de
  JUnit, sin entorno de pruebas y sin imagen, y se ejecutan aquí. Verificable: se ejecutan y terminan
  en verde con un comando de una línea, y ninguna necesita la suite con entorno de pruebas.
- **RNF-13 — Intocables en esta entrega.** No se **modifica** el comportamiento de `MessageBubble`,
  `MessageStatusIcon`, `ChatMessageList`, `StreamingBubble`, `TypingIndicator`,
  `ChatHistoryDrawerContent` ni `NoteAttachmentSheet`. Las **siete** viven en el mismo archivo que sí
  se modifica, así que la comprobación es **por declaración**, no por archivo. Verificable: el diff de
  esa pantalla no quita ninguna cabecera de esas siete declaraciones; si alguna aparece, la revisión
  es explícita y queda escrita, porque en ese caso la comprobación automática ya no basta.
- **RNF-14 — La capa de modelo e IA no se toca.** No cambia `AiRequest`, ni el modo en que los adjuntos
  llegan al modelo, ni el esquema de datos, ni las preferencias. Verificable: el diff de los paquetes
  de datos, del exportador y de las preferencias está vacío, y no hay archivos nuevos sin seguimiento en
  ellos.
- **RNF-15 — Sin listas que mezclen posiciones.** Toda lista que esta entrega mueva o cree declara
  clave y tipo de contenido explícitos. Aplica a **las dos** listas de la pantalla de chat: las fichas
  de adjuntos, en `<campo>`, y la fila de sugerencias, en `<saludo>`. Verificable: en los dos
  archivos, las cuatro líneas que siguen a la invocación de `items(` mencionan `key` y `contentType`.
  **Excepción consciente y declarada:** la clave de las fichas es posicional (decisión 7), así que su
  lista es una excepción a la regla de `AGENTS.md` de «toda `LazyRow` lleva `key` estable»; la
  nota de `AGENTS.md` queda corregida en esta entrega, igual que la de los tests.

## Casos límite

1. **Solo adjuntos, sin texto.** El botón está habilitado y al enviar se lanza la petición con los
   adjuntos; el mensaje de la conversación sale sin texto pero con los adjuntos listados.
2. **Ni texto ni adjuntos ni generación.** El botón está deshabilitado y, si se pulsara, la
   comprobación de envío vacío lo impide.
3. **Generando con texto escrito antes.** Al empezar la generación el campo deja de aceptar escritura;
   al terminar, el texto anterior sigue escrito y se puede enviar.
4. **Dos adjuntos con el mismo nombre.** Quitar el primero quita solo el primero; el segundo sigue
   mostrado con su contenido. La clave de la ficha es **única por construcción** (decisión 7): una
   clave por nombre lanzaría `IllegalArgumentException` al pintarse la segunda.
5. **Borrar todo el texto de un mensaje en edición.** Sin adjuntos pendientes no se lanza ninguna
   petición. **Con** adjuntos pendientes sí se lanza, con la acción de generación y la petición
   vacía: es el comportamiento actual —la comprobación de vacío solo corta cuando no hay adjuntos— y
   es consecuencia directa de (a).
6. **Enviar con la acción de resumen o de corrección.** El campo **no** se vacía: hoy el vaciado solo
   ocurre con la acción de generación, y se conserva.
7. **Abrir una sesión con una edición pendiente.** El texto de la edición se muestra en el campo y
   queda como destino de la escritura, y al enviar se limpia la edición (L6).
8. **Chat vacío, sin nada que resumir y sin adjuntos.** No aparece la sugerencia de resumen; solo las
   de generación.
9. **Chat vacío con adjuntos pendientes.** Aparece la sugerencia de resumen y resume el adjunto, porque
   los adjuntos pendientes forman parte del contexto de la petición.
10. **Chat vacío con contenido de nota pero sin adjuntos.** Aparece la sugerencia de resumen. Hoy esto
    no se da en la aplicación —los tres puntos de entrada pasan el contexto vacío—, y es justamente el
    caso que una implementación que ignorase el contenido de la nota no detectaría.
11. **Un idioma con menos entradas que otro, o sin ninguna.** La elección se hace sobre el conjunto
    disponible en ese idioma; si un idioma no tuviera ninguna clave, se cae al idioma por defecto
    (`values/`) **sin error y sin cadena vacía**. Verificable: L3 recibe el tamaño del conjunto que le
    pasa quien la llama y devuelve el índice válido para ese tamaño.
12. **El mismo día, dos veces.** El saludo es el mismo; al día siguiente es el siguiente del conjunto,
    y al recorrerlo entero vuelve al primero.
13. **Hoja de adjuntos cerrada sin elegir.** No se abre ni el selector de notas ni el de archivos.
14. **Elegir «Nota».** La hoja desaparece antes de que aparezca el selector de notas, y la búsqueda del
    selector arranca vacía con las notas cargadas.
15. **Elegir «Archivo».** La hoja desaparece y se abre el selector con los mismos tipos de archivo que
    antes (`text/*`, `application/json`, `application/xml`).
16. **Sin historial de conversación.** El texto de ayuda es el de la acción activa; en cuanto hay
    historial, pasa a ser el de seguimiento.
17. **Falta una traducción de una clave nueva.** La pantalla cae al idioma por defecto sin error de
    compilación: no es un fallo, pero tampoco es una traducción.
18. **Apóstrofo en una cadena nueva.** Rompe la compilación de recursos aunque el código Kotlin compile
    limpio; se escribe escapado siempre.

## Fuera de alcance

- **Adjuntar foto, cámara o cualquier adjunto binario.** La hoja se queda con **Nota** y **Archivo**.
  Motivo técnico: no hay visión en la capa de modelo; los adjuntos llegan al modelo serializados como
  texto y la petición solo lleva rol y contenido, y el motor en el dispositivo es de solo texto.
  También quedan fuera OCR y cualquier procesamiento de imagen.
- **Un nombre de persona usuaria en el saludo.** No se pide nombre ni se lee de las preferencias: no
  existe tal campo y añadirlo sería otra entrega.
- **La lista de mensajes y sus burbujas.** `MessageBubble`, `MessageStatusIcon`, `ChatMessageList`,
  `StreamingBubble`, `TypingIndicator`, `ChatHistoryDrawerContent` y `NoteAttachmentSheet` no cambian
  de comportamiento.
- **La capa de modelo e IA.** `AiRequest`, la construcción de mensajes y el servicio de inferencia
  siguen como están, incluida la ausencia de un campo de imagen.
- **El modelo de datos, las notas y las preferencias.** Ninguna nota existente cambia de aspecto.
- **Añadir previews de interfaz.** Los tres previews que traen las maquetas se borran y no se
  sustituyen: dependen de un viewmodel real que un preview no puede construir.
- **Retirar el código muerto de la hoja de acciones de IA.** Existe una hoja de acciones de IA
  (`AiBottomSheet.kt`) **sin ningún punto de entrada** que además duplica el estado de acción actual,
  el idioma objetivo y las fichas de acción. No se borra en esta entrega —borrarla es otra decisión—,
  pero es **el sitio donde unificar ese estado** cuando se retome, y esta entrega no crea un tercer
  duplicado.
- **Arreglar `PdfExporterTest` y `WidgetNotesTest`.** No están rotos: sus llamadas coinciden con las
  firmas reales. No hay nada que arreglar aquí, y si alguna vez lo hubiera, sería de la spec 002.
- **Mover a lógica pura el mapeo de la acción a la petición en la rama de envío.** Se queda en el
  Composable (decisión 11).
- **Las medidas exactas del nuevo campo.** Esta entrega fija qué se ve y qué hace, no cuántas
  décimas mide cada elemento del diseño.
- **Limpiar recursos de texto huérfanos ni otras traducciones ausentes.** Algunas claves de
  sugerencias ya existen y no se usan, y en el inglés británico hay más claves sin traducir que no son
  estas tres. Esta entrega no las borra, no las traduce y no las reordena.

## Criterios de finalización

Cada criterio es un comando o una comprobación con su resultado esperado. Los valores «hoy» son el
punto de partida y sirven para detectar que una comprobación no mide lo que dice.

- [ ] **Los tres `@Preview` borrados y `:app:assembleDebug` en verde**
      (`./gradlew :app:assembleDebug`). Los siete errores de compilación de las maquetas estaban todos
      en esos tres previews, y **los borró la persona usuaria** antes de la implementación; este criterio
      es la **comprobación** de que el módulo construye, no una tarea pendiente.
- [ ] **`./gradlew test` se ha ejecutado** y su resultado real está escrito en el criterio 2 del
      cierre de la spec: en verde, o el error que resulte con el regreso a las pruebas puras. No se da
      por verde lo que no se ha ejecutado, y no se acepta como premisa que la tarea de tests esté rota
      sin haberlo comprobado.
- [ ] Las notas desfasadas de `AGENTS.md` están corregidas en la misma entrega (D4 y RNF-15):
      `grep -n "BROKEN" AGENTS.md` no devuelve la línea sobre `PdfExporterTest`/`WidgetNotesTest`, y
      `grep -n "excepción" AGENTS.md` devuelve al menos una coincidencia, que es donde queda anotada la
      lista de fichas con clave posicional.
- [ ] `grep -rc "fun EmptyChatWelcome" app/src/main/java/com/example/ui/*.kt | grep -v ':0'` devuelve
      **una sola** coincidencia (RF-39).
- [ ] `grep -rn "ATTACHMENT_CHIP = " app/src/main/java` devuelve **una sola** coincidencia, y está en
      `app/src/main/java/com/example/ui/chat/ChatInputState.kt` (RF-39).
- [ ] `find app/src/main -name '*Botton*'` no devuelve nada, y `find app/src/main -name 'AttachmentAiSheet.kt'` devuelve **un** archivo (D5, RF-39). *La errata se busca por **nombre**: `grep -rn "Botton"` ya da 0 hoy, porque la errata está en el nombre del archivo y no en su contenido.*
- [ ] `grep -rc "@Preview" <campo> <saludo> <hoja>` devuelve **0** en los tres (RF-39). *Valor de
      partida: 1, 1 y 1.*
- [ ] En **cada uno** de los cuatro archivos de lógica —`ui/chat/ChatSuggestions.kt`,
      `ui/chat/ChatGreeting.kt`, `ui/chat/ChatInputState.kt`, `ui/chat/AttachmentSheetOption.kt`—,
      `grep -c '^import androidx.compose'` devuelve **0**, y ninguno importa `com.example.R`
      (RF-41, RNF-11).
- [ ] `git status --porcelain -- app/src/main/java/com/example/ui/chat` lista **exactamente** los cuatro
      archivos de lógica como nuevos, y
      `git status --porcelain -- app/src/main/java/com/example/ui | grep -v 'ui/table/'` no muestra
      ninguna otra entrada inesperada. *Se acota a lo que introduce esta entrega: el árbol ya tiene
      trabajo sin commitear de la spec 005 (`ui/table/` sin seguimiento y `EditableTableBlock.kt`
      modificado), que no es de aquí y no puede hacer fallar este criterio —riesgo 8.*
- [ ] `grep -n "FloatingChatInputField(" app/src/main/java/com/example/ui/AiChatScreen.kt` devuelve al
      menos una coincidencia, y `grep -c "DropdownMenu" app/src/main/java/com/example/ui/AiChatScreen.kt`
      devuelve **0** (RF-33, RF-34). *Valor de partida: 3.*
- [ ] `grep -n "enabled = !isProcessing" <campo>` devuelve una coincidencia (RF-33 b).
- [ ] `grep -c 'ai_chat_hint' <campo>` devuelve **4** —las cuatro claves del texto de ayuda, resueltas
      con recursos en el Composable— y `grep -n '"ai_chat_hint' <campo>` devuelve **0**: ninguna clave
      escrita como texto (RF-33 c). *La **decisión** de cuál corresponde no se mide aquí, sino en L4.*
      *Verificado en la implementación: la forma válida de esta comprobación es contar coincidencias, no
      buscarlas con comilla de cierre.*
- [ ] `grep -c "editingMessage = text; inputText = text" app/src/main/java/com/example/ui/AiChatScreen.kt`
      devuelve **2** (las dos vías de entrar en edición fijan los dos estados),
      `grep -c "editingMessage = null"` devuelve **1** (la rama de envío) y
      `grep -c "editingMessage = adoptedEditing"` devuelve **1** (la carga de la edición), y la prueba
      de L6 cubre que el `null` de la segunda lo produce `adoptPendingEditing` (RF-33 d, e).
      *Valor de partida: 2, 2 y —la tercera, que no existía— 0. El recuento total de `editingMessage`
      **no** es criterio: la implementación conforme puede reducirlo, porque parte de la lógica pasa a
      L6.*
      *Por qué la carga de la edición ya no se ve como `= null`.* Esa rama no decide nada por su cuenta:
      llama a `adoptPendingEditing`, y es **L6** quien devuelve el `null` —`adoptPendingEditing("texto")`
      da `"texto" to null` y `adoptPendingEditing(null)` da `"" to null`, sin inventar edición—. El
      Composable solo aplica lo que L6 le da. Por eso el criterio se parte en dos: el `grep` mide **dónde**
      está cada rama, y la prueba de L6 mide **qué** devuelve la decisión.
      *Por qué escribir `editingMessage = null` a mano para cuadrar el `grep` sería incorrecto.* Sacaría la
      decisión de vuelta al Composable —«si hay edición pendiente, carga el texto y no abras la edición»—
      que es exactamente lo que RNF-11 exige que viva en la lógica pura. El código compila igual y las
      dos ramas seguirían limpiando la edición, así que el fallo sería silencioso: la spec seguiría
      diciendo lo correcto y el código dejaría de cumplirla. El número no se ajusta al código: es el
      código el que va enrutado, y por eso el `grep` de la segunda rama busca `adoptedEditing`.
- [ ] `grep -n "inputText = \"\"" app/src/main/java/com/example/ui/AiChatScreen.kt` devuelve al menos
      una coincidencia, y la condición de vaciado la decide L7 con sus dos casos (RF-33 f).
- [ ] `grep -n 'R\.string\.attach)' <campo>`, `grep -n 'R.string.ai_send' <campo>` y `grep -n
      'R.string.ai_stop' <campo>` devuelven una coincidencia cada una, y `grep -c 'contentDescription =
      "' <campo>` devuelve **0** (RF-33 j). **Y el mismo patrón ancla da 0 sobre
      `app/src/main/java/com/example/ui/AttachmentAiSheet.kt`**, que es la comprobación negativa que exige
      la regla del punto 5 de «Correcciones posteriores».
      *Valor de partida: las tres descripciones están hoy en `AiChatScreen.kt` y la maqueta escribe el
      literal `"attach"`. El patrón de `attach` va anclado por el **paréntesis de cierre** porque
      `attach` es prefijo de `attach_note` y `attach_file`, y **no** puede anclarse por comilla: en
      Kotlin una referencia a recurso no termina nunca en `"`, así que `R.string.attach"` daría 0 incluso
      sobre el código que ya usa recursos.*
      *Por qué la negativa usa `<hoja>` y no un archivo cualquiera.* `<hoja>` es el peor caso posible
      para este patrón y por eso lo prueba: usa recursos por todas partes y sus cuatro referencias
      (`R.string.attach_note)`, `R.string.attach_note_subtitle)`, `R.string.attach_file)`,
      `R.string.attach_file_subtitle)`) **terminan todas en `)`, que es justo lo que el patrón exige**,
      y dos de ellas empiezan por el prefijo `attach`. Si el ancla no discriminara, ahí daría cuatro o
      más. Da **0**, porque `attach_note` y `attach_file` no van seguidos de `)` sino de `_`. Con eso,
      el 1 de `<campo>` no puede ser casualidad ni fruto de un patrón que aceptara cualquier cosa: el
      patrón distingue las cuatro claves de la hoja de la clave del botón «+» sin necesidad de saber
      qué hay escrito detrás de cada nombre.*
- [ ] En `<campo>` aparecen las cuatro piezas de realimentación: `animateColorAsState`,
      `errorContainer`, `HapticFeedbackType.LongPress` y `graphicsLayer` (RF-33 g).
- [ ] `grep -n -A4 'items(' <campo>` y `grep -n -A4 'items(' <saludo>` muestran en las cuatro líneas
      siguientes a cada invocación de `items(` las palabras `key` y `contentType` (RF-33 h, RNF-15).
      *Se acota a la invocación con `-A4`: `grep -n "key = "` sobre el archivo entero lo cumple
      cualquier otra clave y no serviría.*
- [ ] La clave de la ficha es **única por construcción**: dos adjuntos con el mismo nombre producen dos
      claves distintas. Lo cubre la prueba de L4 (RF-33 h, decisión 7).
- [ ] `<campo>` conserva `tonalElevation = 3.dp` **y** `shadowElevation = 8.dp` (RF-33 i).
- [ ] `grep -c "ModalBottomSheet" <hoja>` y `grep -c "onDismissRequest" <hoja>` devuelven 1, y
      `grep -c "SettingsListTile(" <hoja>` devuelve **2** (RF-34, RF-35).
- [ ] `grep -c "fun AttachmentListTile" <hoja>` devuelve **0**: el azulejo no se duplica (RF-34).
- [ ] `grep -n 'Icons.Default.Image\|Icons.Default.Camera\|PhotoCamera' <hoja>
      app/src/main/java/com/example/ui/AiChatScreen.kt <campo> <saludo>` no devuelve ninguna coincidencia
      (RF-34, fuera de alcance). *Se acota a los cuatro archivos del chat y se nombran los iconos
      exactos: en el resto del paquete hay acentos legítimos de imagen y cámara
      —`NoteEditorScreen.kt`, `FileBlockIcon.kt`, `SlashCommandMenu.kt`, `MainListScreen.kt`,
      `EditableImageBlock.kt`, `OpenNoteTabBar.kt`, el visor de medios y los ajustes— y buscarlos en
      todo el paquete daría ocho aciertos que no son de esta entrega.*
- [ ] `grep -c '"text/\*", "application/json", "application/xml"' app/src/main/java/com/example/ui/AiChatScreen.kt` devuelve **1**: los tipos de archivo no cambian (RF-35).
- [ ] `grep -n 'Manos a la obra\|Escribir un poema\|Generar ideas\|Resumir texto\|Explicar concepto\|Adjunta una' <campo> <saludo> <hoja>` no devuelve ninguna coincidencia (RF-40). *Un solo patrón y una sola búsqueda: el patrón repartido en varias líneas dependía de GNU grep y era frágil ante reformateo, y buscar en todo el código daría acertos de otras zonas. El literal de `contentDescription` lo mide el criterio de RF-33 (j), que es más estricto.*
- [ ] `grep -n "userName\|PreferencesRepository" <saludo>` devuelve 0 (RF-36).
- [ ] Las cuatro **etiquetas** se comprueban en la pantalla de bienvenida:
      `grep -cE 'R\.string\.(ai_suggestion_poem|ai_suggestion_ideas|ai_suggestion_explain_concept|ai_summarize)([^_a-zA-Z0-9]|$)' <saludo>`
      devuelve **4**, una por clave; y el **mismo patrón sobre `AiChatScreen.kt`** devuelve **0**, que es
      la mitad que demuestra que el patrón discrimina en vez de dar por buena cualquier cosa. La
      **petición** de la sugerencia de resumen se comprueba en la pantalla de chat:
      `grep -c 'R.string.ai_chat_prompt_summarize' app/src/main/java/com/example/ui/AiChatScreen.kt`
      devuelve **2** (RF-37).
      *Por qué este patrón y no el anterior.* Las cuatro etiquetas viven en un mapa
      `Map<String, Int>` y van seguidas de lo que toque: **coma** en las tres primeras y **fin de línea**
      en la última (`"summarize" to R.string.ai_summarize`). No hay ningún delimitador único detrás del
      nombre, así que anclar por `"` o por `,` da 0 en algún caso; se ancla por un **carácter que no
      pueda seguir a un identificador** —`([^_a-zA-Z0-9]|$)`, que acepta coma, cierre de línea y cualquier
      no-alfanumérico— y eso no da falsos positivos: rechaza `ai_summarize_selection`, `ai_summarize_full`
      y `ai_summarize_notes`, porque el guion bajo no pertenece al conjunto. La condición de la pantalla
      (**0** en `<saludo>` no es lo que se busca; es **2** en la pantalla) es la que descarta el
      comodín.
      *Por qué 2 y no 1 en la pantalla.* Las dos son legítimas y están en sitios distintos: el mapeo
      acción→petición de la rama de envío y el `summarizePrompt` que se le pasa a `requestFor` desde la
      sugerencia de resumen del welcome. RF-37 pide que la clave **se use**, no que se use una vez; el
      umbral anterior de «una coincidencia» convertía en fallo una implementación correcta.
      *Reparto justificado en RF-37:* `ChatSuggestion(id, label, action)` no tiene dónde llevar una
      petición, así que la resuelve quien llama a `requestFor(...)`. Ninguno de los dos patrones puede
      anclar por comilla de cierre, por lo mismo que `attach`: en Kotlin una referencia a recurso nunca
      termina en `"`.*
- [ ] `git diff -- app/src/main/res/values/strings.xml | grep -c '^+ *<string name='` devuelve **5 o
      más** (RF-40): al menos cinco claves nuevas en el idioma por defecto. *Las cinco son las de
      RF-40: etiqueta de «Explicar concepto», dos entradas de saludo y dos subtítulos de la hoja; los
      nombres exactos los fija `plan.md`.*
- [ ] Para **cada** clave de texto nueva, `grep -rl "<clave>" app/src/main/res/values*/strings.xml | wc -l` devuelve **9**, y `git diff --numstat -- app/src/main/res` muestra los **nueve** archivos de `strings.xml` modificados (RF-40).
- [ ] `grep -c 'name="attach_note"\|name="attach_file"\|name="remove"' app/src/main/res/values-en-rGB/strings.xml`
      devuelve **3** (D2, RF-40). *Valor de partida: 0; hoy esas tres claves faltan en el inglés
      británico.*
- [ ] El diff de la pantalla de chat **no toca** las siete declaraciones intocables (RNF-13):
      `git diff -U0 -- app/src/main/java/com/example/ui/AiChatScreen.kt | grep -E '^-[[:space:]]*(private[[:space:]]+)?fun (MessageBubble|MessageStatusIcon|ChatMessageList|StreamingBubble|TypingIndicator|ChatHistoryDrawerContent|NoteAttachmentSheet)\('`
      no devuelve nada. *Si devolviera algo, la revisión es explícita y queda escrita: la comprobación
      automática solo detecta la cabecera de la declaración, no su cuerpo.*
- [ ] `git diff -- app/src/main/java/com/example/data app/src/main/java/com/example/data/ai app/src/main/java/com/example/util/export` está vacío, y `git status --porcelain -- app/src/main/java/com/example/data` no lista archivos nuevos (RNF-14).
- [ ] `git diff -- app/src/test | grep '^-.*@Test'` no devuelve ninguna coincidencia, y
      `git status --porcelain -- app/src/test` no muestra archivos de prueba borrados (RF-19).
      *Se mira el **contenido** del diff y no `--numstat`, que no detecta un `@Test` quitado; y se
      añade `git status` porque el árbol de trabajo tiene archivos sin seguimiento que ningún
      `git diff` muestra —ver riesgo 8—.*
- [ ] Las siete lógicas de la tabla tienen prueba y se ejecutan en verde con el comando que fija
      `plan.md` (RF-37, RF-38, RF-41, RNF-11, RNF-12).
- [ ] La memoria del proyecto queda en 50 líneas como máximo y registra el estado, la decisión 5
      (visibilidad de la sugerencia de resumen), la decisión 6 (cadencia del saludo), la decisión 7
      (clave de las fichas) y los errores de esta zona (RF-20).

## Criterio de cierre de la spec

Esta spec se da por cerrada cuando se cumple todo lo siguiente, y no antes:

1. `./gradlew :app:assembleDebug` termina correctamente. Los siete errores de compilación de las
   maquetas estaban en sus tres `@Preview`, y **la persona usuaria los borró antes de la
   implementación**; hoy esto es una comprobación de que el módulo construye, no una tarea pendiente.
2. `./gradlew test` **se ha ejecutado** y su resultado real está escrito aquí: en verde, o con el
   error que resulte y el regreso a las pruebas puras. No se da por verde lo que no se ha ejecutado, y
   no se acepta como premisa que la tarea de tests esté rota sin haberlo comprobado.
3. Los criterios de finalización están **comprobados uno a uno**, no marcados sin comprobar: cada
   `grep` se ha ejecutado y su resultado es el que dice aquí, y los valores de partida anotados han
   servido para confirmar que la comprobación mide lo que dice.
4. Las **siete lógicas** de la tabla tienen prueba, se han **visto fallar antes** de existir el código
   —el rojo queda registrado en `tasks.md`, con el comando y su salida— y se ejecutan en verde con el
   comando que fija `plan.md`.
5. Las nueve traducciones de las claves nuevas existen, `attach_note`, `attach_file` y `remove`
   existen también en el inglés británico, y los textos que estaban escritos a mano en las maquetas
   han desaparecido del código.
6. Las notas desfasadas de `AGENTS.md` —la de los tests y la de la `key` estable de las fichas— están
   corregidas.
7. `plan.md` y `tasks.md` están aprobados y todas sus tareas marcadas, y cada una referencia su
   `RF-33`…`RF-41`.
8. `MEMORY.md` queda en 50 líneas como máximo y registra el estado, las decisiones 5, 6 y 7, y los
   errores de esta zona.

**No** es condición de cierre la verificación en dispositivo: en esta plataforma no es comprobable, y
queda como riesgo declarado, no como criterio incumplido.

## Riesgos y límites

1. **Lo que bloqueaba la compilación eran los `@Preview`, no la colisión de nombres.** Los siete errores
   estaban dentro de los tres previews y los borró la persona usuaria. Se creyó además que los dos
   `fun EmptyChatWelcome` impedían compilar, y **es falso**: son sobrecargas homónimas con firmas
   distintas, la llamada resolvía a la vieja por argumentos con nombre, y `assembleDebug` dio
   `BUILD SUCCESSFUL` antes de tocar nada. El riesgo que queda es de criterio: dar por bloqueado el
   repo algo que no lo estaba, y saltar comprobaciones que sí eran necesarias —RF-39 quiere una sola
   pantalla de bienvenida—.
2. **La premisa de que la tarea de tests estaba rota era falsa y se ha retirado.** Se afirmaba que
   `PdfExporterTest` y `WidgetNotesTest` no compilaban; sus llamadas **coinciden** con las firmas
   reales (`PdfExporter.kt:28`, `WidgetNotes.kt:19` y `:25`, `PdfBlockRenderer.kt:49` y `:52`) y la
   memoria del proyecto dice que los tests del módulo compilan. La nota desfasada está en `AGENTS.md`,
   y corregirla es parte de esta entrega (D4). El riesgo que queda no es de código sino de criterio:
   dar por rota una tarea que funciona y bajar el listón sin comprobar.
3. **La suite con entorno de pruebas no se ejecuta en esta plataforma** (sin runtime nativo para
   aarch64). Por eso las pruebas nuevas son puras y de JUnit, y por eso ninguna comprobación de esta
   entrega puede depender de una captura.
4. **Una prueba que no se ha visto fallar no está probada.** Cada caso nuevo se ejecuta en rojo antes
   de que exista el código, y ese rojo queda registrado en `tasks.md`. Es un requisito de proceso, y por
   eso vive en el criterio de cierre y no como verificación de RF-41.
5. **AAPT2 rechaza los apóstrofos crudos** en los recursos: rompen la compilación **después** de que el
   código Kotlin ya ha compilado limpio. Ningún éxito temprano de Kotlin demuestra que los recursos
   sean válidos.
6. **Las nueve traducciones no son obligatorias para compilar, y ahora hay dos controles.** Una clave
   nueva que falte en un idioma cae al idioma por defecto sin error. Los dos controles son el recuento
   de archivos de recursos modificados y el criterio de las tres claves rellenadas en el inglés
   británico; ninguno de los dos pertenece al código, así que los dos los comprueba quien revisa la
   rama de trabajo.
7. **La clave de las fichas es posicional, y no hay alternativa limpia hoy.** El adjunto no tiene
   identificador estable (`FileAttachment` no lo tiene) y quitarlo es por posición, así que **una clave
   por nombre lanzaría `IllegalArgumentException: Key was already used`** con dos adjuntos del mismo
   nombre (caso límite 4), y una clave por índice no es estable al quitar. Se usa una clave **compuesta
   por posición y nombre**, única por construcción: al quitar una ficha, las demás cambian de clave y
   Compose **destruye y recompone** esos elementos en lugar de reutilizar su nodo. Aquí no se pierde
   nada, porque la ficha es un `InputChip(selected = false)` sin estado interno y su texto se lee de la
   lista en cada composición, pero el coste es real si algún día la ficha gana estado interno. Añadir un
   identificador estable al adjunto **sí** sería la solución definitiva, y queda fuera de alcance porque
   toca el modelo de datos (RNF-14). Esta lista es una **excepción consciente** a la regla de
   `AGENTS.md`, y la nota de ese archivo se corrige en esta entrega.
8. **El árbol de trabajo está sucio y `git diff` no ve los archivos sin seguimiento.** Hay trabajo sin
   commitear de la spec 005: `ui/table/` y `TableSelectionGeometryTest.kt` sin seguimiento, y
   `EditableTableBlock.kt` modificado. Los criterios que usan `git diff` van acotados a una ruta, se
   complementan con `git status --porcelain` para lo que no está versionado, y el criterio que vigila
   los archivos nuevos **excluye `ui/table/`** explícitamente para que el trabajo ajeno no lo haga
   fallar.
9. **Tres decisiones quedan dentro del Composable, y se acepta a propósito.** La construcción del mapa
   opción→acción, el cálculo del número de día y el mapeo de la acción a la petición de la rama de
   envío siguen en el Composable; la tercera **no entra en la tabla** porque ninguna RF la exige (decisión
   11). El idioma objetivo se inicializa siempre en inglés y no se reasigna, y si la construcción de la
   petición pasa a función pura **debe seguir pasándose**: es un parámetro que hoy viaja y que se
   perdería en silencio. La acción activa empieza siempre como generación, así que las ramas de resumir
   y corregir del texto de ayuda no se ven desde esta pantalla; se conservan porque la decisión de la
   acción no es de esta pantalla.
10. **Los tres puntos de entrada de la pantalla pasan el contexto vacío.** No hay texto seleccionado ni
    contenido de nota, así que lo único que puede resumirse son los adjuntos pendientes. La condición
    de RF-38 cubre los tres casos por si algún día se pasa el contexto, y el caso límite 10 es
    precisamente el que hoy no se puede ver.
11. **Nada de esta zona se ha visto en un dispositivo.** El campo flotante puede quedar tapado por el
    teclado, la hoja puede no cerrarse a tiempo al elegir una opción y la elevación puede leerse
    distinta. Tampoco hay comprobación automática del **orden** de RF-35 —cerrar la hoja antes de abrir
    el selector— ni de que la búsqueda del selector de notas arranque vacía: se comprueban leyendo el
    diff y en dispositivo, y esa es su única verificación.
12. **Verificación limitada a `grep` y pruebas puras.** Los criterios que se comprueban con `grep`
    verifican que el texto está escrito, no que la pantalla se pinte bien. Es el límite de la
    plataforma, no una omisión: el diseño final se juzga en dispositivo.
13. **Existe una hoja de acciones de IA sin ningún punto de entrada** que duplica el estado de acción y
    el idioma objetivo. No bloquea nada, pero si alguien retoma esa hoja y la conecta, tendrá el mismo
    estado duplicado en tres sitios. Está anotado en «Fuera de alcance» como sitio de unificación
    futura.
14. **L1 y L2 están acopladas por un dato que una no rellena: la etiqueta.** `visibleSuggestions` (L1)
    devuelve las cuatro sugerencias con la etiqueta **vacía**, porque el archivo no sabe de recursos;
    `requestFor` (L2) usa esa etiqueta como petición de las tres de generación. El eslabón es el welcome,
    que devuelve **la copia con la etiqueta ya resuelta**, y ese contrato **no lo expresa ningún tipo**:
    `onSuggestion: (ChatSuggestion) -> Unit` admite indistintamente la sugerencia de L1 y la de
    `EmptyChatWelcome`. Quien mañana llame a `requestFor` con una sugerencia recién salida de
    `visibleSuggestions` —sin pasar por el welcome— mandará `prompt` vacío en las tres de generación, sin
    error de compilación y sin que nada falle en la prueba. **Hoy está mitigado** con un comentario en el
    punto de llamada de la pantalla y con que la ruta real pasa por el welcome; **queda como fragilidad
    conocida**, no como requisito: la holgura la cerraría un tipo que distinga «sugerencia sin
    etiqueta» de «sugerencia pintable», y eso sería otra entrega. Se registra para que nadie lo descubra
    por pantalla.

## Decisiones tomadas en la aclaración

1. **La hoja de adjuntos se queda con dos opciones, Nota y Archivo.** Se descartan «foto» y «cámara»
   del prototipo. Motivo técnico: la capa de modelo no tiene visión; los adjuntos llegan serializados
   como texto y el motor en el dispositivo es de solo texto. Añadirlas sería una hoja con opciones que
   no pueden funcionar. Queda como **fuera de alcance** con su motivo.
2. **El saludo es rotatorio y sin nombre de persona.** No se pide nombre ni se lee de las preferencias,
   porque no existe tal campo. Vive en los recursos, en los nueve idiomas.
3. **Las sugerencias son datos, y la de resumen conserva su acción.** La sugerencia de resumen sigue
   lanzando la acción de resumen con el texto seleccionado y el contexto; el resto, generación con su
   etiqueta como petición.
4. **Los tres previews se borran y no se añaden otros.** Dependen de un viewmodel real que un preview
   no puede construir.
5. **La sugerencia de resumen conserva su visibilidad condicional** (decisión propia). Se mantiene
   exactamente la condición actual: aparece si hay texto seleccionado, contenido de nota o adjuntos
   pendientes. Motivo: con la condición presente, la sugerencia **siempre tiene algo que resumir** —los
   adjuntos pendientes forman parte del contexto de la petición—, y sin ella aparecería un botón que
   pide resumir un texto vacío. Además, en los tres puntos de entrada de la pantalla el contexto llega
   vacío, así que quitar la condicional convertiría el botón en un caso muerto permanente.
6. **La rotación del saludo es diaria, determinista y cíclica.** El saludo se elige con una función pura
   que recibe el número de día, no al azar y no por número de composición, y el número de día se calcula
   **una sola vez** con `remember` sin claves. Motivo: es la única forma de que el conjunto se pueda
   ejercitar con una prueba sin entorno de pruebas, y evita que el texto cambie bajo los ojos de quien
   está mirando.
7. **La clave de la ficha se compone de posición y nombre** (decisión propia, punto 19 de la primera
   QA). Es única por construcción, que es lo que evita el `IllegalArgumentException` con dos adjuntos
   del mismo nombre. Se acepta que sea posicional y se acepta su coste (riesgo 7); la alternativa
   estable —un identificador en el adjunto— toca el modelo de datos y queda fuera de alcance.
8. **La decisión del texto de ayuda vive en el archivo de lógica, no en el Composable** (decisión
   propia, punto 7 de la primera QA). El Composable resuelve el texto con recursos; la función pura
   devuelve un identificador del propio dominio, no un `Int` de recurso, y por eso ningún archivo de
   lógica importa `R`.
9. **Cuatro sugerencias, con tres claves reutilizadas y una nueva** (D1): `ai_suggestion_poem`,
   `ai_suggestion_ideas` y `ai_summarize` para la de resumen, y una clave nueva para «Explicar
   concepto». La petición de la sugerencia de resumen es `ai_chat_prompt_summarize`, el comportamiento
   actual, y **la resuelve la pantalla de bienvenida** (RF-37).
10. **La constante de tipo de contenido de las fichas se declara una vez, en el archivo de lógica**
    (decisión propia, punto 14 de la primera QA). Si se quedara en la pantalla de chat, el cambio
    tocaría el bloque de código compartido con el selector de notas, que RNF-13 declara intocable.
11. **El mapeo de la acción a la petición en la rama de envío se queda en el Composable** (decisión
    propia, menor 11 de la segunda QA). Elige entre tres cadenas de recurso según la acción activa
    (`GENERATE`, `SUMMARIZE`, `FIX_GRAMMAR`), existe desde antes y **ninguna RF depende de él**, así que
    moverlo a la tabla sería cambiar más de lo pedido. Queda consignado como decisión de Composable
    aceptada, y vive en el riesgo 9 junto con las otras dos.

## Revisión QA

### Primera vuelta: 20 puntos y 12 menores

Los veinte puntos, y dónde quedó cada uno:

1. Búsqueda de foto y cámara **acotada a los cuatro archivos del chat**, con los iconos nombrados
   exactamente y `PhotoCamera` en la alternancia; se documenta que en el resto del paquete hay
   acentos legítimos.
2. `Botton` pasa a buscarse **por nombre** con `find`, no por contenido.
3. La comprobación de `@Test` borrados usa `git diff -- app/src/test | grep '^-.*@Test'` en vez de
   `--numstat`, y se complementa con `git status` por los archivos sin seguimiento.
4. RNF-13 se rehace **por declaración** con `git diff -U0` filtrando las cabeceras, y se declara que
   si aparece alguna, la revisión es explícita. `MessageStatusIcon` se añadió a la lista por ser parte
   del mismo bloque.
5. La lista de archivos de lógica **está fijada y con ruta** (cuatro archivos), y el criterio dice
   «en cada uno de los cuatro» con los nombres escritos, más una sección que define qué significa
   «cada archivo de lógica».
6. Las lógicas con prueba pasaron a ser **una lista única** con archivo y casos, y todas las
   referencias del documento apuntan a ella.
7. El texto de ayuda: **la decisión vive en el archivo de lógica** y el texto se resuelve en el
   Composable. El criterio se parte en dos: cuatro claves usadas con recursos y cero claves escritas
   como literal en el campo, y la decisión medida por la prueba de L4.
8. Definiciones y RF-36 reescritas con el recorrido **cíclico**: mismo día → mismo saludo; días
   consecutivos → saludos consecutivos; al recorrer el conjunto, vuelta al principio.
9. RF-36 ya no exige un número de entradas por idioma: exige **al menos dos** en el conjunto
   disponible, y el caso límite 11 cubre el idioma con menos entradas y el que no tiene ninguna.
10. «Día» queda **definido** (días desde época, zona horaria local, calculado una sola vez con
    `remember` sin claves) y la función pura recibe el número.
11. RF-40 ya no habla de «subtítulos de las sugerencias»: no existen. Habla de etiquetas.
12. RF-37 fija la **petición de la sugerencia de resumen** (`ai_chat_prompt_summarize`) y la
    conservación del idioma objetivo.
13. `attach_note`, `attach_file` y `remove` en el inglés británico entran en RF-40, con criterio propio
    y valor de partida 0.
14. RF-39 dice **dónde** queda `ATTACHMENT_CHIP`: en el archivo de lógica, precisamente para no tocar
    el bloque compartido con el selector de notas. Decisión 10.
15. La premisa de la tarea de tests rota se **retira** (verificada contra las firmas reales):
    `./gradlew test` vuelve a ser criterio de cierre, con resultado real documentado si no, y la
    corrección de la nota de `AGENTS.md` es acción de entrega. Riesgo 2 y criterio de cierre 2.
16. El **vaciado del campo tras enviar** entra como letra (f) de RF-33, y en la segunda vuelta como
    lógica L7 con criterio propio.
17. El **reparto de escritura entre los dos estados** entra como letra (e) de RF-33 y, en la segunda
    vuelta, como lógica L6 con prueba; la rama que carga una edición al abrir la sesión queda
    recogida en RF-33 (e), no en un riesgo.
18. Las **tres descripciones accesibles** desde recursos entran como letra (j) de RF-33.
19. El **crash por clave repetida** se resuelve con la decisión 7 (clave compuesta, única por
    construcción) y su coste se reconoce en el riesgo 7 y en el caso límite 4.
20. El caso límite 5 se **qualifica**: sin adjuntos no se lanza petición; con adjuntos sí, con la acción
    de generación y la petición vacía.

Los doce menores: la primera tarea es borrar los previews; RNF-10, RNF-12 y RNF-15 tienen criterio
propio, y RNF-15 mira las dos listas; los criterios de `git diff` van acotados y complementados con
`git status`; los marcadores de archivo están definidos; el grep multilínea se redujo a una sola
búsqueda; se anotaron los valores de partida; el `grep` de la clave se acotó a la invocación de
`items`; el límite de 20 líneas se aplica a las funciones puras, no a los Composables; «prueba en rojo
registrada» pasó al criterio de cierre como requisito de proceso; la rama de carga de la edición y el
idioma objetivo se recogieron en RF-33 y en el riesgo 9; la hoja de acciones de IA muerta se dice en
«Fuera de alcance» y en el riesgo 13.

### Segunda vuelta: 11 puntos y 12 menores

1. El criterio de archivos nuevos se **acota a lo que introduce esta entrega** y excluye `ui/table/`,
   trabajo sin commitear de la spec 005, con nota al criterio y al riesgo 8.
2. RF-33 (f) tiene **criterio propio**: el `grep` del vaciado más la prueba de L7.
3. El criterio de los recursos de las sugerencias se **ancla con la comilla de cierre** —porque
   `ai_summarize` es prefijo de tres claves— y RF-37 fija que los resuelve la pantalla de bienvenida,
   así que se buscan en `<saludo>`.
4. El valor de partida de las ramas de edición es el correcto y **el recuento total deja de ser
   criterio**: se comprueban las dos vías de entrar en edición y las dos de limpiar la edición.
5. RF-33 (e) se mide de verdad: lógica **L6** con sus casos, más los dos `grep` anteriores, que no
   pasan con la implementación defectuosa.
6. RF-40 **enumera las cinco claves nuevas como mínimo** por rol, y su criterio cuenta las líneas
   `<string name=` añadidas en el idioma por defecto, que es ejecutable sin conocer los nombres.
7. RF-33 dice **diez comportamientos**, que son las diez letras (a)–(j); la undécima pérdida del
   Contexto es de RF-37/RF-38.
8. Las **siete** declaraciones intocables, en los tres sitios que las contaban como seis.
9. El riesgo 7 **corrige el mecanismo**: al cambiar la clave, Compose destruye y recompone, no
   reutiliza; y se declara la lista de fichas **excepción consciente** a la regla de `AGENTS.md`, cuya
   nota se corrige en la misma entrega.
10. RNF-15 y el criterio de RF-33 (h) pasan de prosa a **comando**: `grep -n -A4 'items('` en los dos
    archivos.
11. L1 incluye el **tercer disyuncto** —contenido de nota—, que es el que hoy no se da y que una
    implementación que ignorase `fullContent` no detectaría; se añade también el caso límite 10.

Los doce menores: L4 devuelve un identificador del propio dominio y no un recurso, y el criterio
comprueba además que ningún archivo de lógica importa `R`; la definición de «día» dice `remember` sin
claves en un punto concreto, sin meter el cálculo en el ViewModel; se quitan los `-r` de un solo
archivo; el patrón de `contentDescription` aparece una sola vez, en el criterio de RF-33 (j), que es más
estricto; `R.string.attach` va anclado (el anclaje se corrigió después, ver «Correcciones posteriores»);
la casilla de `./gradlew test` deja de ser condicional y remite al cierre 2; el riesgo 6 reconoce los
**dos** controles de traducción; el cierre 5 dice «inglés británico» en vez de «noveno idioma»; el
punto 17 de esta revisión apunta a RF-33 (e) y no al riesgo 9; RF-35 consigna que el orden y la búsqueda
vacía solo se comprueban leyendo el diff y en dispositivo (riesgo 11); el riesgo 9 admite la tercera
decisión de Composable, la del mapeo acción→petición, que queda como decisión 11 y fuera de alcance; el
Contexto, verificado exacto, no se toca.

### Correcciones posteriores a la implementación

La implementación (T1–T10) demostró que cuatro criterios escritos eran imposibles o falsos. Se corrigen
aquí; **ningún RF, ni su numeración, ni la tabla de lógicas cambian.**

1. **`R.string.attach"` es imposible.** En Kotlin una referencia a recurso no puede terminar en `"`, así
   que el patrón daba 0 incluso sobre el código que ya usaba recursos (`AiChatScreen.kt:372`, que la
   implementación verificó). Se ancla por el **paréntesis de cierre**: `grep -n 'R\.string\.attach)'`,
   que da exactamente 1. Los tres sitios que lo decían —criterio de RF-33 (j), criterio de RF-37 y
   resumen de la segunda vuelta— quedan corregidos.
2. **`ai_chat_prompt_summarize` no puede estar en `<saludo>`.** `ChatSuggestion(id, label, action)` no
   tiene dónde llevar una petición, y `onSuggestion: (ChatSuggestion) -> Unit` no la admite. El reparto
   real y correcto es: **etiquetas** en la pantalla de bienvenida, **petición** en la pantalla de chat,
   que llama a `requestFor(...)` con ella. RF-37 y su criterio se reparten así. Esta corrección **no dice
   nada del anclaje**: lo resuelve el punto 5 siguiente, que es el que lo fija para todas las claves de
   sugerencia.
3. **La premisa «sin descolisión no compila» era falsa.** `assembleDebug` dio `BUILD SUCCESSFUL` antes
   de tocar nada: los dos `fun EmptyChatWelcome` son sobrecargas homónimas con firmas distintas y la
   llamada resolvía a la vieja por argumentos con nombre. Los 7 errores estaban **solo** en los
   `@Preview`, borrados por la persona usuaria. Se corrigen el Contexto, el riesgo 1 y el criterio de
   cierre 1. Descolisionar **sí** era necesario para RF-39; no era urgencia de compilación.
4. **`R.string.attach` no se puede quitar del paquete**, y `tasks.md` lo pedía. `FileAttachment` vive en
   `com.example.data.ai` (`AiModels.kt:14`) y es parámetro de la firma de `FloatingChatInputField`, así
   que el import no se puede eliminar: se sustituyó por un import explícito de `FileAttachment`.
5. **Tercera repetición del mismo error: el patrón de las etiquetas.** El criterio de RF-37 anclaba las
   cuatro claves con comilla de cierre, y en `EmptyChatWelcome.kt` las etiquetas viven en un mapa
   `Map<String, Int>` seguidas de **coma** (o de fin de línea), nunca de `"`. Este punto **resuelve
   también** la duda que dejó abierta la corrección 2: el prefijo `ai_summarize` frente a
   `ai_summarize_selection`, `ai_summarize_full` y `ai_summarize_notes` queda cerrado por el **mismo**
   ancla que el resto —`[^_a-zA-Z0-9]|$`—, porque el guion bajo no pertenece al conjunto negado; no
   hace falta ningún ancla especial por comilla. Patrón nuevo:
   `grep -cE 'R\.string\.(ai_suggestion_poem|ai_suggestion_ideas|ai_suggestion_explain_concept|ai_summarize)([^_a-zA-Z0-9]|$)' <saludo>`
   → **4**, y el mismo patrón sobre `AiChatScreen.kt` → **0**, que es lo que prueba que discrimina.
   Además, el criterio de la pantalla pasa de «una coincidencia» a **2**, porque las dos son
   legítimas (mapeo acción→petición y `summarizePrompt` del welcome).

   **Regla que sale de las tres repeticiones**, y que hay que aplicar a cualquier criterio nuevo: en
   Kotlin **una referencia a recurso no termina en `"`**, así que anclar por comilla de cierre es
   siempre un patrón roto, y anclar por un único delimitador tampoco vale cuando el uso varía (aquí
   `,` en unas líneas y fin de línea en otra). El ancla segura es un **carácter que no pueda seguir a
   un identificador** —`[^_a-zA-Z0-9]`— **o el fin de línea**, y el criterio que lo usa debe traer
   además la **comprobación negativa** (el mismo patrón en un archivo que no tiene las claves → 0).

## Dudas abiertas

Ninguna. Las dudas de las dos revisiones quedan cerradas por las decisiones D1 a D5 y por las
decisiones 5 a 11 de esta spec.