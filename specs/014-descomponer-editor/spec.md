# Spec 014 — El editor de notas, partido en capas

Estado: **implementada** (2026-10-11), T1-T8 cerradas ✅ **VERIFICADA EN DISPOSITIVO por quien Decide (2026-10-16)**.

> **Nota de procedencia.** Hija de `specs/001-limites-constitucion/spec.md` (**aprobada**), que sigue
> siendo la ley. Hereda sin modificar **RF-3, RF-19, RF-20** y **RNF-4, RNF-5, RNF-6**.
> `RF-84`…`RF-89` y `RNF-39`…`RNF-43` son nuevos: los más altos en uso son `RF-83` (spec 013) y
> `RNF-38` (spec 013).
> **No es una cadena de las specs 005-011** (tabla) ni de las 012-013 (Markdown): esas ya están
> cerradas y esta no las toca. Nace porque el editor de notas creció hasta un único bloque de 3504
> líneas del que no se puede saber qué depende de qué.

## Contexto y objetivo

El editor de notas cabe hoy en un solo archivo de 3504 líneas, y 3380 de ellas son **una sola
función**. Dentro de esa función conviven 95 declaraciones de estado, 49 funciones locales, 8
efectos y la interfaz entera.

El síntoma no es el tamaño del archivo: es que **nadie puede razonar sobre él**. Para saber qué
depende de qué hay que leer las 3504 líneas en orden, porque nada está agrupado por Concern y nada
está terminado por una frontera. Cuando hay que tocar el editor, el coste no es «editar un archivo
grande», es «descubrir el grafo de dependencias a mano», y eso es lo que hace que un cambio pequeño
sea arriesgado.

Encima hay tres deudas concretas que el tamaño esconde:

1. **Código que nadie lee.** Se declara, se calcula y no se consume.
2. **Un guardado repetido siete veces.** El mismo envío al almacén de **la nota que se está
   editando**, con los mismos diez campos, copiado en siete sitios distintos: cada uno es una
   oportunidad de que uno se quede atrás. Hay una octava llamada que **no** cuenta aquí, porque
   guarda otra nota —la destino del bloque que se mueve— con los campos de aquella.
3. **Cinco insertadores de bloque que hacen lo mismo.** Cada uno abre con la misma comprobación de
   «¿sustituyo el bloque vacío que está activo o inserto después?» y la cierra con la misma
   normalización de cursor.

El objetivo es que el editor se pueda tocar sin leerlo entero: que la lógica que se pueda comprobar
sin pantalla esté comprobada, que lo muerto desaparezca, y que lo que queda tenga una función
declarada por zonas.

## Usuarios

- Quien mantenga el editor: encuentra dónde vive cada cosa sin recorrer 3500 líneas, y puede
  comprobar en segundos que lo que extrajo sigue siendo lo mismo.
- Quien lea el repositorio: ve fronteras con nombre en lugar de un bloque monolithico.

## Definiciones

- **Lógica comprobable**: cálculo que no necesita pantalla, recursos ni reloj. Es la que puede
  ejercitarse en una máquina sin emulador — que es la única forma de probar aquí.
- **Estado muerto**: declaración que se escribe o se lee cero veces fuera de su propia declaración.
- **Línea base**: el número de pruebas y el tamaño que se miden **antes** de tocar nada, para poder
  afirmar después que no se ha perdido ninguna.

## Requisitos funcionales

Heredados sin modificar: **RF-3**, **RF-19**, **RF-20**.

- **RF-84**: EL SISTEMA **no declara estado muerto** en el editor de notas. Verificable: una prueba
  automática recorre el código del editor y falla si encuentra una declaración local cuyo nombre no
  aparece en ninguna otra línea del editor.
- **RF-85**: CUANDO se guarde la nota desde **cualquier** camino, EL SISTEMA compone el mismo envío al
  almacén mediante **un único** camino. Verificable: en todo el editor de notas hay **una sola**
  llamada al guardado con el juego completo de campos, y las demás la reutilizan.
- **RF-86**: MIENTRAS se compone el editor, EL SISTEMA **no calcula lo que no se pinta**. Verificable:
  no queda ninguna llamada a la conversión de color dentro del cálculo del estado de la barra de
  herramientas, y ese cálculo no produce ningún valor de color.
- **RF-87**: EL SISTEMA mantiene la **lógica comprobable fuera de la interfaz**: el cálculo del
  estado de la barra, las transformaciones de estilo de un tramo de texto, el resaltado de la
  búsqueda y la clasificación de un archivo por su tipo se ejecutan **sin levantar pantalla**.
  Verificable: esos cuatro ficheros **no importan nada de composición**, y cada uno tiene pruebas
  que corren sin emulador.
- **RF-88**: EL SISTEMA expone el **tamaño del editor como un invariante comprobable**, no como una
 costumbre. Verificable: una prueba falla si el editor vuelve a crecer por encima del umbral que fija,
  y ese umbral **baja** en esta entrega respecto del punto de partida.
- **RF-89**: CUANDO se inserte un bloque de medio —imagen, vídeo, audio, archivo o dibujo—, EL
  SISTEMA decide **igual** en los cinco casos si sustituye el bloque activo o inserta después.
  Verificable: los cinco caminos llaman a **una** función, y esa función tiene pruebas para los dos
  desenlaces.

## Requisitos no funcionales

Heredados: **RNF-4**, **RNF-5**, **RNF-6**.

- **RNF-39 — Cero cambio de comportamiento.** Esto es un recorte, no una mejora: lo que se ve, lo
  que se toca y lo que se guarda es exactamente lo mismo. Verificable: el diff **no toca** ni una
  línea de lógica de la que dependa un resultado observable; lo que cambia es **dónde** vive cada
  cosa. La única excepción es RF-84, porque lo muerto no se puede observar.
- **RNF-40 — La lógica extraída se prueba aquí.** Lo que se mueve no es solo movido: gana pruebas que
  corren **en esta plataforma**, que es lo que permite afirmar que no se rompió. Verificable: el número
  de pruebas comprobables **sube** respecto de la línea base, y todas en verde.
- **RNF-41 — Menos código que antes.** Verificable: el recuento de líneas del editor de notas **baja**
  de 3504, y además de moverse se **eliminan** las copias del guardado y los cinco insertadores
  repetidos.
- **RNF-42 — Cero dependencias nuevas, cero librería de estado.** Verificable: ningún
  `implementation(...)` nuevo, y el estado sigue siendo `remember` + `StateFlow`, sin `ViewModel`
  inventado ni biblioteca externa.
- **RNF-43 — Las fronteras son legibles desde arriba.** Verificable: ningún fichero del editor de
  notas supera las 600 líneas, coherente con los mayores que ya hay en el repositorio.

## Casos límite

1. **El editor vacío.** La línea base se mide sobre el archivo tal como está, con su código muerto
   incluido; si no, el invariante de RF-88 mediría lo que esta entrega va a quitar y no valdría.
2. **Una prueba que no se ha visto fallar.** El invariante de tamaño nace en rojo a propósito, con
   el umbral puesto al tamaño actual más uno, y se baja a mano hasta el tamaño real. Sin ese paso en
   rojo, un invariante que nunca falla no está probado.
3. **Pruebas que no se ejecutan aquí.** Una de las partes de la suite necesita emulador y no corre en
   esta plataforma. La línea base que se toma es **la de las pruebas que sí corren**, y se dice
   cuál es, para que nadie la lea como «la suite completa está en verde».
4. **Funciones locales sin referencia adelantada.** Kotlin no las admite: al mover un bloque hay que
   colocarlo **después** de lo que llama. Un movimiento que compila en un orden y no en otro es la
   trampa más probable de esta entrega.
5. **Recomposición.** Eleven de los manejadores del editor se crean como lambdas sueltas y se pasan a
   los hijos en cada recomposición. Moverlos sin recordarlos no cambia el comportamiento, pero
   **empeora** la recomposición, así que su recuerdo es parte del trabajo y no una mejora opcional.

## Fuera de alcance

Se registran para que consten. **No se hacen en esta entrega.**

- **Agrupar las 95 declaraciones de estado** en uno o varios contenedores con nombre, y dar nombre a
  los efectos. Es lo que queda del editor, pero es la parte de mayor riesgo: el estado se comparte
  **por identidad** con el editor de bloques, y ese cruce no se toca hasta que el resto esté quieto.
- **Recordar los manejadores de las barras**, por el mismo motivo.
- Los dos defectos conocidos del editor de bloques (la tabla que sale vacía en la lectura, y la
  selección que se comparte entre bloques): son de otro fichero.
- Unificar el segundo modelo de tabla, que sigue guardando los títulos aparte.

## Criterios de finalización

- [x] La prueba de tamaño del editor pasa con el umbral **bajo** respecto del punto de partida, y se
      documenta el antes y el después.
- [x] La búsqueda de estado muerto en el editor de notas no devuelve nada.
- [x] El editor de notas tiene **una** llamada al guardado con el juego completo de campos.
- [x] Los cuatro ficheros de lógica comprobable no importan nada de composición, y sus pruebas
      pasan sin emulador.
- [x] El recuento de pruebas comprobables es **mayor** que el de la línea base, con todo en verde.
- [x] `:app:compileDebugKotlin` y `:app:assembleDebug` sin errores ni avisos nuevos.
- [x] `MEMORY.md` registra el corte y lo que queda para la 015.

## Dudas abiertas

Ninguna. Las dos decisiones que la planteaban quedaron resueltas por quien Decide: el cálculo de
colores de la barra es **código muerto** y se borra (no un botón a medio hacer), y el **corte** de la
spec se acepta tal como se propuso — lógica, limpieza y superficie ahora; estado y barras después.