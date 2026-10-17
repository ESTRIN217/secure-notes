# Spec 015 — Editor de notas: estado aislado y tablas completas

Estado: **implementada** (2026-10-16).

> **Procedencia.** Continuación de `specs/014-descomponer-editor/`: completa el trabajo que quedó
> fuera de aquella entrega. Los identificadores nuevos empiezan en **RF-153** y **RNF-93**, tras
> `RF-152` y `RNF-92` de la spec 024.
>
> **Alcance acordado.** Incluye el aislamiento del estado del editor y de la selección, las tablas
> en la vista previa y en la lectura en voz alta, y la continuación del corte del editor de 014.
> Los diálogos de inserción de medios conservan su alcance actual: no se rediseñan en esta spec.

## Contexto y objetivo

La spec 014 redujo y separó partes del editor, pero dejó para una entrega posterior el estado que
comparten la pantalla, el editor de bloques y las barras de herramientas. Esa identidad compartida
hace posible que una selección pendiente termine aplicándose en otro bloque. También quedaron
pendientes algunas operaciones del editor que dependen de ese estado.

Hay además dos fallos observables con tablas: la vista previa puede mostrarlas vacías porque no
convierte sus filas almacenadas al modelo que pinta la tabla, y la lectura en voz alta no incorpora
sus celdas porque las tablas no tienen segmentos de texto ordinarios.

El objetivo es completar ese trabajo sin alterar el contenido persistido ni el comportamiento ya
estable de edición, y hacer que las tablas se vean y se lean con sus datos.

## Usuarios

- Quien edita varias notas o bloques: espera que la selección, el cursor y las acciones de formato
  correspondan al bloque que está usando.
- Quien consulta una nota: espera ver las tablas con sus celdas y escuchar su contenido al usar la
  lectura en voz alta.
- Quien mantiene el editor: necesita que las operaciones que dependen de estado tengan límites
  claros y que puedan modificarse sin reabrir el monolito anterior.

## Historias de usuario

- **HU-1.** Como persona que lee una nota, quiero ver las filas y columnas de cada tabla para poder
  consultar su contenido.
- **HU-2.** Como persona que usa la lectura en voz alta, quiero que se lean las celdas de las tablas
  en orden para no perder información.
- **HU-3.** Como persona que edita bloques distintos, quiero que la selección pendiente pertenezca
  al bloque que la originó para no cambiar texto por accidente.
- **HU-4.** Como persona que mantiene el editor, quiero completar la separación del estado y sus
  operaciones sin cambiar el comportamiento visible de las acciones existentes.

## Definiciones

- **Vista previa:** presentación de solo lectura del contenido de una nota.
- **Selección pendiente:** rango de texto que una acción del editor debe aplicar o restaurar.
- **Celda no vacía:** celda cuyo contenido, después de quitar espacios en blanco, no está vacío.

## Requisitos funcionales

- **RF-153:** CUANDO se muestre una tabla en la vista previa de una nota, EL SISTEMA muestra los
  valores almacenados en todas sus filas y columnas, en el mismo orden que la tabla del editor. La
  fila de cabecera respeta la configuración de cabecera de la tabla. Verificable: una tabla con
  cabecera, al menos dos filas de datos y más de una columna conserva los valores y dimensiones al
  pasar a la vista previa.
- **RF-154:** CUANDO se lea una nota en voz alta, EL SISTEMA incluye el contenido de las tablas en
  el orden del documento y lee las celdas no vacías por filas, de izquierda a derecha. EL SISTEMA no
  anuncia separadores ni genera fragmentos vacíos. Verificable: una tabla con celdas vacías y no
  vacías produce fragmentos solo para las celdas no vacías, en orden de fila, después del título y
  de los bloques anteriores.
- **RF-155:** CUANDO una selección pendiente se origine en un bloque de texto, EL SISTEMA solo la
  aplica a ese mismo bloque. SI cambia el bloque activo o la nota antes de consumirla, ENTONCES EL
  SISTEMA descarta la selección anterior y no la aplica a otro contenido. Verificable: al cambiar
  de bloque y de nota con una selección pendiente, el texto de destino permanece intacto y no se
  restaura un rango ajeno.
- **RF-156:** CUANDO se use una acción existente del editor —incluidos el menú de comandos, la
  inserción o apertura de archivos, el movimiento de bloques y las acciones de formato—, EL SISTEMA
  ejecuta la acción sobre la nota, el bloque y la posición que estaban activos, con las reglas de
  sustitución e inserción ya establecidas. Verificable: las acciones conservan sus resultados y no
  cambian de destino tras recomposición o cambio de foco.
- **RF-157:** EL SISTEMA conserva la secuencia existente de bloques y el título al construir la
  lectura en voz alta; añadir las celdas de tabla no altera ni duplica el texto de los demás bloques.
  Verificable: una nota con título, texto, tabla y texto posterior se lee en ese orden y cada
  contenido aparece una sola vez.

## Requisitos no funcionales

- **RNF-93 — Sin cambio de formato ni pérdida de datos.** Los bloques de tabla conservan su
  representación persistida y las notas existentes no requieren migración. Verificable: los datos
  serializados de una tabla se leen sin modificación y no se introducen cambios de esquema.
- **RNF-94 — Sin regresiones en la edición.** Las acciones que ya funcionaban mantienen sus
  resultados, incluidas la elección de comandos, la inserción de medios, la edición de texto y el
  formato. Verificable: las pruebas previas relacionadas siguen presentes y no aparecen fallos
  nuevos en las suites pertinentes.
- **RNF-95 — Lógica comprobable sin pantalla.** La conversión de tablas para la lectura en voz alta
  y las decisiones sobre rangos de selección pueden probarse sin `Context` ni componentes de
  interfaz. Verificable: las pruebas de esas decisiones se ejecutan como pruebas puras.
- **RNF-96 — Sin coste de dependencias.** No se añaden bibliotecas ni se cambia el formato de datos.
  Verificable: no hay nuevas dependencias de producción ni cambios de esquema de persistencia.

## Casos límite

1. Una tabla sin celdas no vacías no añade fragmentos a la lectura en voz alta.
2. Las celdas vacías intercaladas no detienen la lectura ni alteran el orden de las celdas
   posteriores.
3. Una selección pendiente cuyo bloque de origen se eliminó o dejó de estar activo no se aplica a
   otro bloque.
4. Una tabla sin una configuración válida conserva el comportamiento de recuperación existente y
   no hace fallar la vista previa ni la lectura.
5. Cambiar de nota mientras hay una selección pendiente no transfiere esa selección a la nota nueva.

## Fuera de alcance

- Rediseñar la interfaz o el modelo de acciones de los diálogos de inserción de medios.
- Cambiar el formato JSON de `DataBlock` o `TableData`, o migrar notas existentes.
- Modificar exportadores, otras superficies de lectura o el editor de código.
- Cambiar el comportamiento visual o textual de los bloques que no sean tablas.

## Criterios de finalización

- [x] La vista previa conserva contenido, orden y dimensiones de las tablas.
- [x] La lectura en voz alta incluye solo celdas no vacías, por filas, y mantiene el orden y el
      contenido del resto de la nota.
- [x] Una selección pendiente nunca se aplica a un bloque o nota distintos de su origen.
- [x] Las acciones existentes del editor conservan su destino, posición y resultado.
- [x] Las pruebas puras cubren celdas vacías, orden de filas, selección obsoleta y cambio de nota.
- [x] Las pruebas existentes relacionadas siguen presentes; no se eliminan ni se saltan casos.
- [x] Se ejecutan las validaciones aplicables y se distinguen los fallos conocidos de plataforma de
      cualquier regresión nueva.
- [x] `MEMORY.md` queda actualizado con el resultado de la implementación y la verificación manual.

## Dudas abiertas

Ninguna sobre el comportamiento: la lectura recorre celdas no vacías por filas, la vista previa
muestra la tabla real y la selección solo pertenece al bloque que la originó. El rediseño de los
diálogos de inserción de medios queda excluido.
