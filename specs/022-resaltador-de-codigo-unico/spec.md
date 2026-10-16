# Spec 022 — Un solo resaltador de código: el bloque de la nota y el editor comparten el mismo

Estado: **implementada y verificada en dispositivo** (2026-10-16) — T1-T9

> **Nota de procedencia.** Encadena con la **019** (`:editor-de-codigo` existe como módulo y `:app` ya depende
> de él **en un solo sentido**). Los RF más altos en uso son `RF-130` y `RNF-75` (spec 021), así que aquí
> empiezan en `RF-131` y `RNF-76`.
>
> **No es un rediseño.** Es **de-duplicación**: hoy hay dos resaltadores y dos catálogos de idiomas, y el
> resultado visible del bloque de código **no debe cambiar** (RF-134).
>
> **No toca datos.** El formato de la nota no cambia; el idioma del bloque sigue guardándose como hoy
> (RF-133).

## Contexto y objetivo

Hoy la app resalta sintaxis con **dos implementaciones distintas** y **dos listas de idiomas distintas**:

- La del **bloque de código** de la nota (`BlockType.CODE_BLOCK`): regex, colores fijos, ~24 lenguajes.
- La del **editor de código** (`:editor-de-codigo`): por tema, ~26 lenguajes, con su propio enum de lenguajes.

Divergen en tres cosas, y las tres se pagan caras:

1. **Reconocimiento.** Hay lenguajes que uno entiende y el otro no (`jsx`, `jsonc`, `xsl`, `fish`, `ktm` solo
   por un lado; `env`, `plain text` por el otro). El mismo código se ve resaltado en el editor y plano en la
   nota, o al revés.
2. **Reglas de token.** El mismo `if`, la misma cadena o el mismo comentario se colorean con reglas escritas
   dos veces, que ya no coinciden.
3. **Mantenimiento.** Añadir o corregir un lenguaje obliga a tocar dos sitios, y nada avisa si solo se toca
   uno. Encima hay un **tercer** mapa de lenguajes por nombre de fichero.

El objetivo es que exista **una sola** implementación del resaltado y **un solo** catálogo de idiomas, propiedad
del módulo del editor de código, y que el bloque de código de la nota **la reutilice** en vez de tener su copia.
El cambio no debe alterar lo que el usuario ve al leer o escribir una nota.

## Usuarios

- Quien escribe notas con bloques de código: quiere que un mismo lenguaje se entienda igual en la nota y en el
  editor de código.
- Quien ya tiene notas guardadas con bloques de código: espera que **se sigan viendo igual** después del cambio.
- Quien mantiene la app: quiere **una** lista de idiomas y **un** resaltador, para no arreglar dos veces lo
  mismo.

## Historias de usuario

- **HU-1.** Como quien escribe una nota con un bloque de código, quiero que el resaltado reconozca los mismos
  lenguajes que el editor de código, para que el mismo código se vea igual en los dos sitios.
- **HU-2.** Como quien mantiene la app, quiero una sola lista de idiomas y un solo resaltador, para no tener
  que corregir cada lenguaje dos veces.
- **HU-3.** Como quien ya tiene notas con bloques de código, quiero que sigan resaltándose igual tras el cambio,
  para no perder nada de lo que ya tenía.

## Definiciones

- **Resaltado**: aplicar estilos de color por token a un texto, **solo para mostrarlo**. No cambia el texto.
- **Resaltador**: la pieza que produce ese texto con estilos a partir de un texto plano y un idioma. **No**
  incluye la superficie de edición (el campo de texto, los números de línea, el desplazamiento).
- **Catálogo de idiomas**: la lista única de lenguajes soportados, con su identificador y su etiqueta visible.
- **Bloque de código**: el bloque `BlockType.CODE_BLOCK` de una nota.
- **Editor de código**: el módulo `:editor-de-codigo` y sus superficies.
- **Identificador de idioma**: el valor que la nota guarda asociado a un bloque de código para saber cómo
  resaltarlo.

## Requisitos funcionales

- **RF-131**: EL SISTEMA resalta la sintaxis del bloque de código con **la misma implementación** que usa el
  editor de código, sin una segunda copia de las reglas de token. Verificable: un invariante comprueba que el
  camino que resalta un bloque de código llama a la pieza compartida y que **no existe** en `:app` ningún
  resaltador propio.

- **RF-132**: EL SISTEMA obtiene la definición de los idiomas del bloque de código de **una única fuente
  compartida** con el editor de código; `:app` solo puede conservar el mapeo entre el identificador guardado y
  su idioma, y las etiquetas visibles, **nunca** una segunda definición de los lenguajes. Verificable: un
  invariante comprueba que cada opción del selector se resuelve a un idioma del catálogo compartido y que `:app`
  no enumera lenguajes por su cuenta (ni conjuntos de palabras clave, ni tablas de tipos).

- **RF-133**: CUANDO una nota guardada antes del cambio tiene un identificador de idioma, EL SISTEMA lo sigue
  interpretando como el mismo idioma y lo sigue resaltando igual. Verificable: una prueba pura por cada
  identificador que la app guarda hoy, comprobando que se resuelve al idioma correcto del catálogo compartido y
  que **ninguno** cae en «desconocido».

- **RF-134**: EL SISTEMA conserva el aspecto actual del bloque de código: el fondo es el de la nota, el color
  base del texto es el del tema de la app y la **paleta** de colores por token es la de hoy, en claro y en
  oscuro. Qué palabra concreta se colorea pasa a decidirlo la implementación compartida (RF-131): eso es el
  objetivo, no una regresión. Verificable: el bloque no pinta fondo propio y la paleta de tokens sigue saliendo
  del tema de la app; en dispositivo, un bloque de código se ve coherente y legible en claro y en oscuro.

- **RF-135**: SI el identificador de idioma de un bloque no existe en el catálogo compartido, ENTONCES EL
  SISTEMA muestra el contenido **sin resaltar** (texto plano) y con la etiqueta de texto plano, sin fallar.
  Verificable: prueba pura del idioma «desconocido» y del identificador vacío, que dan texto plano sin lanzar.

- **RF-136**: CUANDO el usuario elige un idioma en el selector del bloque de código, EL SISTEMA guarda un
  identificador que el catálogo compartido reconoce. Verificable: prueba pura que recorre **todas** las opciones
  del selector y comprueba que cada una se resuelve a un idioma del catálogo.

- **RF-137**: SI el bloque de código no tiene idioma asignado, ENTONCES EL SISTEMA no aplica ningún resaltado.
  Verificable: prueba pura del caso «sin idioma», que devuelve el texto sin estilos de token.

- **RF-138**: EL SISTEMA mantiene el resaltado como **solo visualización**: el texto plano guardado en la nota
  no cambia por resaltarlo. Verificable: prueba pura que compara el texto antes y después de resaltar: es el
  mismo.

- **RF-139**: EL SISTEMA muestra las etiquetas de idioma con **el mismo texto que hoy**, incluidas las que están
  traducidas. Verificable: una prueba pura comprueba que cada opción del selector resuelve a la misma etiqueta
  que hoy (las que hoy salen de una traducción incluidas), y un invariante comprueba que las claves de cadena
  usadas existen en `values/` — las claves son las de hoy, así que ningún locale empeora.

## Requisitos no funcionales

- **RNF-76**: **Cero dependencias nuevas** y la dirección de la dependencia no cambia: el bloque de código
  reutiliza el módulo del editor, **nunca al revés**, sin ciclos. Verificable: `git diff` no toca ningún
  `build.gradle.kts` y un invariante comprueba que el módulo del editor no conoce nada del bloque de la nota.

- **RNF-77**: **No quedan copias muertas.** El resaltador y el catálogo que hoy viven en `:app` se eliminan; no
  se dejan «por si acaso» ni comentados. Verificable: un invariante comprueba que ya no existen.

- **RNF-78**: **La huella de memoria no aumenta.** El resaltado se calcula **una vez por render** y se memoriza
  por (texto, idioma); no se añade ninguna caché sin límite ni se resalta en cada fotograma. Verificable: el
  cálculo va dentro de un `remember` con esas claves y no queda ninguna llamada al resaltado fuera de él en los
  caminos de dibujo.

- **RNF-79**: La compilación unitaria queda en verde y **la cuenta de fallos de la suite no empeora** respecto a
  la base (`:app` 742 / 41, `:editor-de-codigo` 136 / 2). Verificable: `--rerun` y `--continue`, comparando los
  fallos **por clase**, no en cantidad.

- **RNF-80**: **Cero cadenas nuevas.** Si hiciera falta alguna, se añade a los nueve locales a la vez.
  Verificable: `git diff` sobre `strings.xml` (idealmente vacío) y un invariante de paridad de los nueve.

- **RNF-81**: Funciones de ≤ 20 líneas, guard clauses, y el resaltado **solo se decide en la interfaz**: el
  bloque no lee DAO ni cifra nada.

## Casos límite

1. **Notas guardadas con identificadores que hoy existen.** `kotlin`, `bash`, `toml`, `ini`, `markdown`, etc.
   Deben seguir resolviéndose al mismo lenguaje (RF-133). Es el caso que más se puede romper en silencio.
2. **`toml` e `ini` como dos opciones, un solo idioma en el catálogo.** Hoy el selector los ofrece por separado;
   el catálogo compartido los agrupa. Se mantienen las dos opciones visibles apuntando al mismo resaltado
   (ver dudas abiertas).
3. **`bash` con etiqueta traducida («Shell»).** El catálogo compartido lo llama por su nombre; la etiqueta
   visible debe seguir saliendo de la traducción (RF-139).
4. **Idioma vacío o ausente.** Sin resaltado, sin fallo (RF-137).
5. **Identificador desconocido** (nota de una versión futura, o un valor manipulado). Texto plano sin fallo
   (RF-135).
6. **Texto muy largo** en un bloque. El resaltado se calcula una vez por render y se memoriza; no debe provocar
   recompilaciones de patrón por fotograma (RNF-78).
7. **Tema claro y oscuro.** El bloque no pinta fondo propio y su texto sigue contrastando en los dos (RF-134).
8. **Bloque inactivo frente a bloque en edición.** Los dos caminos (lectura y edición) deben resaltar **igual**,
   porque hoy ambos llaman al resaltador.
9. **Palabras que cambian de color.** Al pasar la clasificación de tokens a la compartida, alguna palabra puede
   colorearse distinto (o dejar de hacerlo). Es el efecto **esperado** de unificar las reglas: la **paleta** no
   cambia (RF-134), la **clasificación** sí (RF-131).

## Fuera de alcance

- **La superficie de edición.** El bloque sigue usando su campo de texto y sus números de línea; **no** se
  sustituye por el editor de código completo (pestañas, buscar/reemplazar, panel de símbolos).
- **El formateador de código.** El bloque no gana «formatear»; no se expone al usuario la spec de formateo.
- **El detector de lenguajes por nombre de fichero.** Queda como está; unificarlo es otra spec (hoy solo lo usan
  las pruebas).
- **El selector de temas del editor.** La nota no gana temas de editor: el color sigue saliendo del tema de la
  app (RF-134).
- **Cambiar el formato de la nota o el valor guardado del idioma.** No hay migración de datos (RF-133).
- **La previsualización, la exportación (PDF/HTML/JSON) y la lectura en voz alta.** No resaltan; no se tocan.
- **Reducir lenguajes.** No se quita ninguno de los que hoy ofrece el selector.

## Criterios de finalización

- En `:app` **no queda** ningún resaltador de sintaxis ni ninguna lista de idiomas: los dos salen del módulo del
  editor.
- Pruebas puras en verde: compatibilidad de los identificadores guardados, idioma desconocido, idioma vacío,
  texto plano tras resaltar y todas las opciones del selector resolviendo a un idioma.
- Invariantes en verde: no quedan copias en `:app`, el selector sale de la fuente compartida, la dirección de la
  dependencia no cambia, y las etiquetas existen en los nueve locales.
- `:app:compileDebugUnitTestKotlin` en verde y **ninguna regresión** en la cuenta de fallos (`:app` 742 / 41,
  `:editor-de-codigo` 136 / 2, con los mismos 41 de plataforma). El **total** de `:app` baja: se eliminan los
  tests de la implementación borrada; lo que no cambia es **qué falla**.
- `git diff` no toca ningún `build.gradle.kts`.
- **Verificación en dispositivo** (lo que ninguna prueba pura cubre): un bloque de código de una nota **guardada
  antes del cambio** se ve **coherente y legible, con la misma paleta y el mismo fondo, en claro y en oscuro**,
  y una palabra que se coloreaba antes se sigue coloreando.

## Dudas abiertas

- [NECESITA ACLARACIÓN] **`toml` e `ini` en el selector.** Hoy son dos opciones; el catálogo del editor los
  agrupa en una sola entrada («INI / TOML»). Propuesta: **mantener las dos opciones** en el selector, ambas
  apuntando al mismo resaltado, para no cambiar lo que ve el usuario (RF-134, RF-139). La alternativa es
  agruparlas en una, como el editor, y entonces el selector cambia a la vista.
- [NECESITA ACLARACIÓN] **Etiquetas del catálogo en inglés vs traducidas.** El catálogo del editor nombra en
  inglés algunos idiomas («Bash / Shell», «Config / ENV», «Plain Text», «INI / TOML»). Propuesta: conservar las
  etiquetas **traducidas** que hoy usa el bloque (RF-139), mapeando solo los casos que hoy tienen traducción
  propia.
