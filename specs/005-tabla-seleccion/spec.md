# Spec 005 — Selección y foco del bloque de tabla

Estado: implementada (2026-10-04) — T1-T9 cerradas; **sin verificación visual**

> **Nota de procedencia.** Esta spec es una hija de `specs/001-limites-constitucion/spec.md`
> (**aprobada**), que es la spec madre y sigue siendo la ley. No la sustituye ni la contradice.
> Hereda sin modificar: **RF-3, RF-19, RF-20** y las **RNF-4, RNF-5, RNF-6**, más el principio 3
> (lógica fuera de la interfaz) y el principio 4 (tests primero) de `docs/constitution.md`.
> Los identificadores `RF-x` y `RNF-x` heredados son los de la madre; aquí no se renumeran.
> **RF-21 a RF-27 son nuevos y propios de esta spec**: la madre no regula el bloque de tabla, así
> que no numeran a partir de la serie de la madre por reutilización, sino por continuidad del
> espacio de nombres (`RF-20` era el identificador más alto en uso). Su plan y sus tareas están en
> `plan.md` y `tasks.md` de esta misma carpeta.

## Contexto y objetivo

El bloque de tabla se rediseñó sin spec, lo cual ya incumple el principio 2 de la constitución. Al
recuperar el resultado con dos grabaciones de pantalla —la aplicación y una aplicación de referencia
que la persona usuaria indicaba como modelo— se vio que el rediseño **no funciona**: la barra
flotante y los manejadores de fila que debía introducir **no aparecen nunca**, y el resaltado de
selección produce un resultado que la referencia no tiene.

Tres defectos, en orden de gravedad:

1. **La barra flotante y los manejadores de fila nunca se muestran.** La tabla decide si está activa
   preguntando a un contenedor por el estado del foco. El sistema de diseño notifica a un elemento
   del estado del foco **solo cuando ese elemento es un destino de foco**, y un contenedor que solo
   agrupa celdas no lo es: nunca recibe la notificación. La consecuencia es doble y silenciosa: la
   barra flotante no se monta y los manejadores de fila tampoco, pero **el espacio que la barra
   reservaba sí se queda reservado**. La tabla aparece con 32 dp de margen vacío a la izquierda que
   no contiene nada, y sin barra. Nada falla, no hay error ni aviso: simplemente falta la mitad de
   lo implementado. El fallo existe desde antes de esta spec y lo introduce el rediseño, de modo que
   la primera parte de esta entrega es devolver el bloque al estado en que su propio diseño dice que
   debe estar.

2. **El resaltado de selección dibuja un borde por celda.** Hoy, cuando una celda tiene el foco,
   cada celda de su columna recibe un borde grueso de color de acento, y cada celda de la fila
   seleccionada también. El resultado son rectángulos sueltos por celda que no se unen: una
   escalera. La referencia dibuja **un único contorno continuo** que rodea lo seleccionado, de modo
   que una columna se lee como un bloque y no como una tira de cajas. En tablas anchas, con scroll
   horizontal, la escalera es también la única pista de dónde está la columna activa, porque el
   scroll deja fuera la cabecera.

3. **El manejador de columna agranda la fila de cabecera.** El manejador se dibuja dentro de la
   celda de cabecera, así que al aparecer empuja la fila y la tabla salta bajo el dedo de quien la
   está editando. La referencia lo dibuja centrado sobre el borde superior de la columna, sobreponiéndolo
   y sin ocupar alto.

El objetivo es que el bloque de tabla se comporte como su diseño describe y como la referencia
muestra: barra y manejadores visibles cuando la tabla está señalada, contorno continuo en lugar de
escalera, un manejador también para la fila de cabecera, y ningún salto de maquetación al enfocar,
seleccionar o mover el foco dentro de la tabla.

## Usuarios

- **Persona usuaria de la aplicación.** Escribe y organiza material en tablas dentro de sus notas.
  Toca celdas,tabs entre ellas, redimensiona columnas y añade o quita filas. No controla el
  sistema de diseño ni el código, así que no puede corregir que una barra que el diseño declara no
  aparezca.
- **Persona desarrolladora o mantenedora.** Cambia el código del proyecto. Necesita poder comprobar
  el comportamiento sin depender de una pantalla, porque este proyecto ya tiene documentado un
  bloqueo de plataforma que impide ejecutar parte de su suite.

## Historias de usuario

- HU-1. Como persona usuaria, quiero que al tocar una celda aparezca la barra con el menú de la
  tabla, para poder cambiar sus propiedades sin salir de donde estoy.
- HU-2. Como persona usuaria, quiero ver la fila y la columna sobre las que estoy trabajando
  distinguidas del resto, para no equivocarme al escribir en una tabla ancha con scroll horizontal.
- HU-3. Como persona usuaria, quiero que mover el foco entre celdas y añadir o quitar filas no
  desplace la tabla bajo mis dedos, para poder seguir escribiendo sin volver a located el texto.
- HU-4. Como persona desarrolladora, quiero que la decisión de qué se dibuja en función del foco de
  la tabla sea comprobable con una prueba, para que no vuelva a romperse en silencio como ya pasó.

## Definiciones

- **Tabla activa.** Bloque de tabla en el que al menos una celda tiene el foco del sistema de
  escribir. Una tabla no activa es una tabla en la que nadie está escribiendo.
- **Celda enfocada.** Celda que tiene el foco del sistema de escribir. Solo hay una a la vez en toda
  la aplicación. Puede ser una celda de cabecera, identificada como la fila `-1`.
- **Fila seleccionada.** Fila sobre la que la persona usuaria ha actuado con su manejador, por
  ejemplo al abrir su menú. Es distinta de la fila que contiene la celda enfocada: puede haber una
  celda enfocada en la fila 2 y una fila seleccionada en la fila 4.
- **Columna activa.** Columna que contiene la celda enfocada.
- **Contorno de selección.** Trazo continuo de color de acento que rodea una región rectangular de
  la tabla y **no se interrumpe en las fronteras entre celdas de esa región**. Una región de una
  sola celda y una región de una fila completa se dibujan con el mismo mecanismo y se distinguen solo
  en su tamaño. La palabra «continuo» es lo que separa este requisito de lo que hay hoy: hoy cada
  celda dibuja su propio rectángulo cerrado y el resultado es una escalera. Solo hay un contorno de
  selección a la vez: cuando hay celda enfocada, su contorno sustituye al de la fila seleccionada.
- **Fila de cabecera.** Fila `-1` de la geometría de la tabla, la que contiene los títulos de las
  columnas. Puede estar desactivada, en cuyo caso la tabla no la tiene.
- **Zona de la barra.** Región del bloque de tabla donde se dibuja la barra flotante. No es una
  franja reservada: la barra se **superpone** al hueco que ya existe por encima de la tabla, de
  manera que no ocupa sitio propio ni desplaza la tabla.
- **Tabla señalada.** Tabla activa **o** sobre la que hay un puntero. En una tabla señalada se
  muestran la barra flotante y los manejadores; en una que no lo está, ninguno.
- **Puntero.** Cualquier puntero del sistema de entrada: dedo, lápiz o ratón. Sobre una pantalla
  táctil, el contacto con la tabla cuenta como puntero sobre ella.
- **Manejador.** Control pequeño que permite actuar sobre una fila o una columna: arrastrarla para
  reordenarla o abrir su menú.
- **Geometría de la selección.** Decisión, aislada de cualquier dibujado, de qué regiones de la tabla
  llevan contorno de selección y dónde se sitúan los manejadores, dada la situación de la tabla.
- **Situación de la tabla.** Celda enfocada o ninguna, fila seleccionada o ninguna, número de filas,
  número de columnas, y si la tabla tiene fila de cabecera.

## Requisitos funcionales

Heredados de la spec madre, sin modificación:

- **RF-3**: SI una dependencia deja de usarse, ENTONCES EL SISTEMA la retira por completo.
- **RF-19**: EL SISTEMA no elimina, desactiva ni salta ningún caso de prueba para conseguir una
  suite verde, y mantiene el alcance de los existentes.
- **RF-20**: AL terminar la entrega, EL SISTEMA deja la memoria del proyecto en un máximo de 50
  líneas y registra en ella el estado actual, las decisiones tomadas con su motivo y los errores a
  evitar.

Nuevos, propios de esta spec:

- **RF-21**: CUANDO una celda del bloque de tabla recibe el foco, EL SISTEMA muestra la barra
  flotante de la tabla y los manejadores de fila. Verificable: con una celda enfocada, la barra está
  en el árbol de composición y hay un manejador por fila.
- **RF-22**: CUANDO el foco pasa de una celda a otra, EL SISTEMA mantiene la tabla activa y **no**
  depende del orden en que llegan las notificaciones de foco de la celda que pierde y la que gana.
  Verificable: para los dos órdenes posibles de llegada de las notificaciones, el resultado es el
  mismo, y en ninguno la tabla se marca como inactiva de forma transitoria.
- **RF-23**: SI la tabla deja de estar señalada, ENTONCES EL SISTEMA oculta la barra flotante y los
  manejadores, y ninguna región de la tabla queda marcada como seleccionada. Verificable: sin celda
  enfocada y sin puntero sobre la tabla, la barra no está en el árbol y no hay manejadores.
- **RF-24**: EL SISTEMA mantiene en el bloque de tabla un **margen vertical de alto constante** por
  encima de la tabla, y dibuja la barra flotante **superpuesta dentro de ese margen**, sin que su
  visibilidad cambie la altura. Verificable: el borde inferior de la barra queda por encima del borde
  superior de la tabla, y la posición y el tamaño de la tabla son los mismos con la barra presente
  que sin ella. El margen existe siempre, tenga la barra o no la tenga.
- **RF-25**: MIENTRAS la tabla se mantenga señalada, ENTONCES EL SISTEMA no altera la posición ni el
  tamaño de la tabla al enfocar una celda, al seleccionar una fila, al mover el foco entre celdas,
  ni al aparecer o desaparecer la barra, el manejador de cabecera o los manejadores de fila.
  Verificable: la posición y el tamaño de la tabla antes y después de cada una de esas cinco
  situaciones son iguales.
- **RF-26**: MIENTRAS una celda tenga el foco, ENTONCES EL SISTEMA dibuja el contorno de selección
  rodeando esa celda y **ninguna otra celda de la fila ni de la columna**. Verificable: la geometría
  de la situación de tabla con una celda enfocada produce exactamente una región, y es la de esa
  celda.
- **RF-27**: MIENTRAS haya una fila seleccionada, ENTONCES EL SISTEMA dibuja el contorno de selección
  rodeando esa fila completa como **una sola región continua**, y ninguna celda de la fila dibuja un
  contorno propio. Verificable: la geometría produce exactamente una región que abarca todas las
  celdas de la fila, y ninguna celda de la fila lleva contorno individual.
- **RF-28**: MIENTRAS haya una fila de cabecera y una celda de ella tenga el foco, ENTONCES EL
  SISTEMA muestra el manejador de esa columna y lo dibuja centrado sobre el borde superior de la
  columna, **sin alterar la altura de la fila de cabecera**. Verificable: la altura de la fila de
  cabecera es la misma con el manejador visible que sin él.
- **RF-29**: EL SISTEMA ofrece un manejador para la fila de cabecera, con las mismas acciones que el de
  una fila de datos y sin alterar la altura de la fila de cabecera. Verificable: con la tabla
  señalada, hay exactamente un manejador por fila contando la de cabecera, y la altura de la fila de
  cabecera no depende de si el manejador se está mostrando.
- **RF-30**: CUANDO hay un puntero sobre la tabla y ella no está activa, EL SISTEMA la marca como
  señalada y muestra la barra flotante y los manejadores. Verificable: al entrar un puntero sobre la
  tabla sin celda enfocada, la barra aparece.
- **RF-31**: SI la tabla está señalada solo por un puntero y ese puntero sale, ENTONCES EL SISTEMA la
  deja de marcar como señalada, salvo que alguna celda siga enfocada. Verificable: al salir el puntero
  sin celda enfocada la barra desaparece, y con una celda enfocada sigue presente tras salir el
  puntero.
- **RF-32**: MIENTRAS la tabla esté activa, ENTONCES EL SISTEMA dibuja **un solo** contorno de
  selección, y el de la celda enfocada tiene prioridad sobre el de la fila seleccionada. Verificable:
  con la fila de cabecera seleccionada y una celda de datos enfocada, la geometría produce una sola
  región, la de la celda enfocada.

## Requisitos no funcionales

Heredados de la spec madre, sin modificación:

- **RNF-4 — Verificabilidad.** Cada requisito funcional tiene una comprobación reproducible, un
  comando de una línea o un caso de prueba. Verificable: la sección de criterios de finalización no
  contiene ninguna comprobación que solo pueda hacer una persona mirando la pantalla.
- **RNF-5 — Un idioma por capa en lo que se toca.** Los identificadores, las API y los mensajes de
  registro que esta entrega añada o modifique están en inglés; los comentarios y la documentación,
  en español.
- **RNF-6 — Sin coste nuevo.** La entrega no añade dependencias, tareas de compilación ni procesos
  de compilación nuevos. Verificable: el conjunto de dependencias no cambia y la lista de tareas de
  compilación no crece.

Nuevos, propios de esta spec:

- **RNF-7 — La geometría es comprobable sin pantalla.** La decisión de qué regiones llevan contorno
  de selección y dónde se sitúan los manejadores está aislada del dibujado, en funciones puras que
  reciben la situación de la tabla y devuelven regiones y posiciones. Verificable: existe una prueba
  que las ejercita sin levantar interfaz, y las pruebas se ejecutan en el entorno de desarrollo.
- **RNF-8 — La entrega no depende de la parte bloqueada de la suite.** Las pruebas de esta entrega
  se ejecutan sin el entorno de pruebas que el proyecto tiene documentado como no disponible en la
  plataforma de desarrollo. Verificable: las pruebas nuevas se ejecutan y terminan en verde con un
  comando de una línea, sin tocar las suites que no se pueden ejecutar.
- **RNF-9 — Sin salto perceptible.** Ninguna de las situaciones de RF-25 produce un desplazamiento
  de la tabla superior a un píxel. Verificable: la comparación de posiciones e imposing tamaños da
  diferencia cero.

## Casos límite

1. **Tabla sin fila de cabecera.** Con la fila de cabecera desactivada, no hay manejador de cabecera
   y los manejadores de fila se alinean con la primera fila, sin espacio reservado arriba.
2. **Salto de foco entre dos celdas.** Al pasar de una celda a otra, la tabla no llega a marcarse
   inactiva en ningún fotograma intermedio, y el contorno no desaparece.
3. **Foco que sale de la tabla.** Cuando el foco pasa a un bloque que no es la tabla, la barra y los
   manejadores desaparecen y la fila seleccionada se limpia.
4. **Tipografía del sistema grande.** Con una escala de fuente alta, las filas miden más que su
   mínimo y los manejadores siguen centrados en su fila.
5. **Tabla con scroll horizontal.** Al desplazarse horizontalmente, los manejadores de fila y la
   barra no se desplazan con el contenido, porque no pertenecen a la zona desplazable.
6. **Añadir o quitar filas con la tabla señalada.** Tras añadir o quitar filas, hay exactamente un
   manejador por fila contando la de cabecera, y cada uno está en su fila.
7. **Reordenar filas arrastrando.** Tras reordenar, los manejadores siguen correspondiendo a su fila
   y el contorno de la fila seleccionada rodea a la fila en su posición nueva.
8. **Última celda de la cabecera.** La celda de cabecera de la última columna es la que la barra
   cubría antes de esta entrega; queda descubierta y su manejador no se sale del bloque.
9. **Tabla de una sola columna.** El contorno de selección de una columna única rodea la columna
   entera, y su apariencia no se distingue de la de una fila.
10. **Bloque primero de la nota.** Una tabla al principio de la nota no tiene nada por encima. La barra
    se dibuja igualmente dentro del margen de la propia tabla, sin cubrir ninguna celda, porque el
    margen es de la tabla y no del editor.
11. **Puntero sobre una tabla ya activa.** Entrar y salir repetidamente de una tabla que ya está
    activa no cambia lo que se ve.
12. **Desplazamiento con el dedo sobre la tabla.** Un dedo que recorre la tabla para desplazarla la
    marca como señalada y la desmarca al levantarse, sin dejar la barra pegada.

## Fuera de alcance

- **Seleccionar una columna completa como acción propia.** La referencia permite seleccionar una
  columna entera desde su manejador y la dibuja con contorno continuo. Aquí la columna activa se
  deduce de la celda enfocada y se señala con su manejador, y no hay contorno de columna completa.
  Esta entrega no añade esa interacción.
- **La forma y el color concretos de la barra.** Esta entrega exige **dónde** se dibuja la barra y
  que no tape la tabla, no que tenga una forma determinada. Elegir la forma es una decisión de
  diseño que no necesita un requisito funcional.
- **El modelo de datos de la tabla y su serialización.** No cambia ni una clave de lo que se guarda
  en una nota. Ninguna nota existente cambia de aspecto al actualizarse.
- **El dibujado de la tabla en solo lectura y en las exportaciones.** El renderizador de solo
  lectura y el exportador a PDF siguen como están, incluido su incumplimiento actual de la
  representación de anchos de columna.
- **El defecto de la previsualización y del texto hablado.** La conversión del bloque de tabla para
  lectura asistida está hoy incompleta y produce una tabla vacía. Es un defecto real y separate, con
  su propia entrega.
- **Los defectos de selección de los bloques de texto.** El estado de selección se comparte entre
  todos los bloques de texto de la nota desde una sola fuente, y hay bloques que nunca informan de
  su selección. Es otra zona, con su propia entrega.
- **Los avisos de texto visible.** Esta entrega no añade, ni cambia, ni retira ningún texto visible,
  así que no altera la diferencia de claves entre locales.

## Criterios de finalización

- [ ] `./gradlew :app:compileDebugUnitTestKotlin` termina sin errores.
- [ ] La prueba de geometría de la selección se ejecuta y termina en verde con
      `./gradlew :app:testDebugUnitTest --tests '*TableSelectionGeometry*'`.
- [ ] La prueba cubre RF-22 con los dos órdenes de llegada de las notificaciones, RF-26, RF-27, RF-28,
      RF-29 y RF-32, y los casos límite 1, 2, 6, 7 y 9.
- [ ] `grep -rn "setTableFocus" app/src/main/java/com/example/ui/EditableTableBlock.kt` no devuelve
      ninguna coincidencia: el contenedor de la tabla no vuelve a preguntar por el foco. Las celdas
      **sí** siguen usando `onFocusChanged`, porque cada celda es un destino de foco legítimo.
- [ ] La geometría de la selección no se calcula dentro de una función que dibuja: la prueba la
      ejercita sin levantar interfaz.
- [ ] `grep -rn "TableGutterWidth" app/src/main/java/com/example/ui/EditableTableBlock.kt` no devuelve
      ninguna aparición en un modificador de relleno o tamaño: el gutter no reserva ancho.
- [ ] `git diff --numstat app/src/test` no elimina ninguna anotación de caso de prueba.
- [ ] `git diff -- app/src/main/java/com/example/data/model` está vacío: el modelo de datos no cambia.
- [ ] `./gradlew :app:assembleDebug` termina correctamente.
- [ ] La memoria del proyecto queda en 50 líneas como máximo y registra el estado, la decisión de
      superponer la barra y el error del detector de foco.

## Decisiones tomadas en la aclaración

1. **La barra se superpone, no reserva franja.** La primera versión reservaba una franja vertical de
   alto fijo, como pedía el encargo original. La grabación mostró el coste: la tabla baja al enfocar
   una celda y vuelve a subir al salir. La referencia superpone la barra en el hueco que ya existe
   por encima de la tabla. Se adopta la superposición, con lo que la barra nunca cubre una celda y
   la tabla no se mueve nunca.
2. **Los manejadores aparecen también al pasar el puntero.** Además de mostrarse cuando la tabla
   está activa, se muestran al haber un puntero sobre ella, y desaparecen al salir el puntero si
   no hay ninguna celda enfocada.
3. **Se añade el manejador de la fila de cabecera.** Con las mismas acciones que el de una fila de
   datos, y superpuesto al borde izquierdo, sin alterar la altura de la cabecera.
4. **El margen por encima de la tabla es constante y pertenece a la tabla, no al editor.** Al
   implementar T7 se comprobó que la premisa de RF-24 era falsa: **el editor no tiene espaciado
   entre bloques**, así que no existe el hueco en el que apoyar la barra y superponerla obligaba a
   tapar el bloque de arriba. La barra va, por tanto, dentro de un margen que la tabla tiene siempre,
   y solo la visibilidad de la barra cambia dentro de él. El margen es un coste permanente de unos
   60 dp por tabla, y es el precio de que la barra no tape nada y la tabla no se mueva. Afecta **solo
   a las tablas**: no se toca el ritmo vertical del resto de bloques.