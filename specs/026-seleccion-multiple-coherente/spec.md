# Spec 026 — La selección múltiple de notas se comporta como la de siempre

Estado: **implementada y verificada en dispositivo** (2026-10-17), T1–T7 cerradas. Los tres gestos (toque,
pulsación larga y atrás) no se pueden probar sin Robolectric, que no se ejecuta en esta plataforma, así que se
verificaron a mano con el guion de ocho pasos del final.

> **Nota de procedencia.** No encadena con ninguna spec anterior. Los RF más altos en uso son `RF-172` y
> `RNF-106` (spec 025), así que aquí empiezan en `RF-173` y `RNF-107`.
>
> **No añade dependencias** ni claves de texto: es una corrección de comportamiento en una sola pantalla
> (`MainListScreen`) más una pieza pura con sus pruebas.
>
> **Origen.** Reporte del usuario: «al mantener presionado una nota se activa la barra, pero si toco otra nota
> ese modo se quita». La comprobación en el código confirmó las **tres** piezas del defecto (ver Contexto).

## Contexto y objetivo

En la lista de notas hay tres formas de entrar o salir del modo selección (pulsación larga sobre una nota,
toque sobre una nota, botón «atrás») y hoy **no dicen lo mismo**:

- **Pulsación larga** sobre una nota (`MainListScreen.kt:1104` y `:1171`) **alterna** esa nota en la selección:
  la añade si no estaba y la quita si estaba.
- **Toque** sobre una nota con algo seleccionado (`:1092` y `:1159`) **vacía la selección y no hace nada más**:
  no abre la nota, no alterna. La única forma de seleccionar una segunda nota por toque es pulsarla largo.
- **Atrás** (`BackHandler`, `:165`) no contempla la selección: con notas seleccionadas en la sección de inicio,
  atrás **no** cierra el modo.

Las tres consecuencias prácticas:

1. Tocar otra nota **destruye** la selección que se acaba de hacer, en vez de ampliarla o reducirla.
2. Leer una nota que está seleccionada cuesta **dos** toques: uno que cancela la selección y otro que abre.
3. La única salida del modo distinta del toque que cancela es la **X** de la barra superior (`:466`), porque
   atrás hace otra cosa.

El objetivo es que la selección múltiple se comporte **como en el resto de Android** (Keep, Gmail, Drive,
Fotos, Archivos) y como el propio gesto de pulsación larga de esta app: **un toque alterna** mientras hay una
selección, y el modo se abandona **al vaciar la selección, con la X o con atrás**.

## Usuarios

- Quien selecciona varias notas para moverlas, fijarlas, archivarlas o exportarlas en lote: espera poder
  añadir y quitar notas **de una en una**, indistintamente con toque o con pulsación larga.
- Quien quiere leer una nota y se encuentra el modo activo: espera poder salir de él con **atrás**, sin irse
  de la pantalla ni cerrar la app.
- Quien nunca usa la selección múltiple: no debe notar ningún cambio, salvo que ahora el modo tiene una salida
  más (atrás) que antes no tenía.

## Historias de usuario

- **HU-1.** Como quien quiere seleccionar tres notas, quiero que tocar la segunda y la tercera las **añada** a
  la selección, para no tener que mantener pulsado en cada una.
- **HU-2.** Como quien se equivocó y seleccionó una nota de más, quiero que tocarla (o pulsarla largo) la
  **quite** de la selección, para corregir sin salir del modo.
- **HU-3.** Como quien quiere abrir una nota teniendo el modo activo, quiero que **atrás** cierre el modo para
  volver a poder tocar la nota y abrirla de una vez.
- **HU-4.** Como quien usa la app sin selección múltiple, quiero que el toque siga abriendo la nota y la
  papelera siga igual, para no notar el cambio.

## Definiciones

- **Selección activa** (o **modo selección**): hay al menos una nota seleccionada. Es la condición que hoy
  muestra la barra superior con el número y las acciones por lotes. La regla es **una sola** y es la que
  comparten la barra, el toque y el atrás (RF-179).
- **Alternar** una nota: si estaba seleccionada, quitarla; si no, añadirla. No es «seleccionar» ni
  «deseleccionar» a secas: la misma acción hace las dos cosas según el estado.
- **Toque**: pulsación corta sobre la tarjeta de una nota.
- **Pulsación larga**: mantener pulsada la tarjeta. Es la que **entra** en el modo desde cero.
- **Barra de acciones por lotes**: la barra superior que sustituye al título de la sección mientras hay
  selección activa (número de seleccionadas, X de cerrar, fijar, favorito, archivar, etiquetar, compartir,
  borrar).
- **Papelera**: la sección de notas eliminadas. Hoy no tiene modo selección (la pulsación larga allí no hace
  nada y el toque restaura la nota). Esta spec **no** se lo añade.

## Requisitos funcionales

- **RF-173**: CUANDO el usuario toque una nota mientras haya una selección activa, EL SISTEMA **alterna** esa
  nota en la selección y **no** abre el editor ni restaura nada.
- **RF-174**: SI no hay selección activa, ENTONCES el toque mantiene el comportamiento actual: abre el editor
  de la nota, o restaura la nota si la sección es la papelera.
- **RF-175**: CUANDO el usuario mantenga pulsada una nota fuera de la papelera, EL SISTEMA alterna esa nota en
  la selección aplicando **la misma** regla que el toque (RF-173), de modo que toque y pulsación larga no
  puedan discrepar.
- **RF-176**: CUANDO la última nota seleccionada deje de estarlo —por toque, por pulsación larga o al
  completarse una acción por lotes—, EL SISTEMA cierra el modo selección: la barra superior vuelve a ser la
  de la sección.
- **RF-177**: CUANDO el usuario pulse atrás con una selección activa, EL SISTEMA cierra el modo selección y
  **no** cambia de sección, **no** abre ni cierra el cajón de navegación y **no** sale de la app.
- **RF-178**: SI hay una selección activa y la sección **no** es la de inicio, ENTONCES el primer atrás cierra
  el modo selección (RF-177) y un atrás posterior aplica el comportamiento actual de esa sección (volver a la
  de inicio). El modo se cierra **antes** que la navegación, nunca a la vez.
- **RF-179**: EL SISTEMA decide «hay selección activa» con **una sola** regla, y la barra de acciones, el
  toque, la pulsación larga y el atrás deciden con esa misma regla; ninguna de esas superficies vuelve a
  escribir la comprobación por su cuenta.

## Requisitos no funcionales

- **RNF-107**: La alternancia y la condición de «selección activa» viven en una pieza **pura** (sin `android`
  ni `androidx`), con sus casos de prueba, para que se prueben sin Robolectric, que no se ejecuta en esta
  plataforma.
- **RNF-108**: Cero dependencias nuevas: ningún `build.gradle.kts` ni `oss-licenses.json` cambia.
- **RNF-109**: Cero textos visibles nuevos. La X de cerrar ya existe (`menu_cancel_selection`) y se conserva.
  Si hiciera falta algún texto, iría a los **nueve** `strings.xml` a la vez.
- **RNF-110**: `./gradlew test` no añade ninguna clase roja nueva: los fallos de plataforma y los
  preexistentes siguen siendo **los mismos** que antes de esta spec.
- **RNF-111**: La papelera no cambia: allí el toque restaura la nota y la pulsación larga no selecciona
  (RF-174). No se añade modo selección a la papelera.
- **RNF-112**: Las demás superficies con pulsación larga —el historial de chat y las pestañas del editor— no
  cambian; esta spec es solo la lista de notas.

## Casos límite

- **Toque sobre la única nota seleccionada**: la deselecciona y el modo termina (RF-176).
- **Toque sobre una nota ya seleccionada habiendo otras**: solo la quita; el modo sigue, con una menos.
- **Pulsación larga sobre una nota ya seleccionada**: la quita (RF-175), comportamiento que ya existía.
- **Toque sobre una nota con el modo activo**: **nunca** abre el editor, aunque la nota no estuviera
  seleccionada (RF-173).
- **Atrás con el modo activo en la sección de inicio**: cierra el modo; un segundo atrás hace lo de siempre
  (salir de la app).
- **Atrás con el modo activo en una sección distinta de la de inicio**: cierra el modo; un segundo atrás
  vuelve a la sección de inicio (RF-178).
- **Atrás sin selección**: exactamente lo de antes de esta spec.
- **Cambio de sección con el modo activo**: la selección se limpia y el modo se cierra, como hoy.
- **Acción por lotes completada** (fijar, favorito, archivar, etiquetar, borrar): la selección se limpia, como
  hoy.
- **Nota seleccionada que desaparece de la lista** por una acción por lotes: la selección se limpia con la
  acción, así que no queda una selección apuntando a una nota ausente.
- **Papelera**: toque sobre una nota → la restaura; pulsación larga → nada. Sin cambios (RNF-111).
- **Selección vacía en la sección de inicio**: atrás sigue sin estar capturado por la pantalla.

## Fuera de alcance

- **Añadir modo selección a la papelera** (hoy inalcanzable: la pulsación larga allí no selecciona).
- **«Seleccionar todo»**, selección por rango o por arrastre (marquesina).
- **Cambiar los iconos, el orden o los textos** de la barra de acciones por lotes.
- **La barra contextual del historial de chat y las pestañas del editor**: tienen su propia pulsación larga y
  no se tocan (RNF-112).
- **Deshacer** una acción por lotes.
- **Selección múltiple con el ratón o el teclado** (pantallas grandes): se comporta como el toque, sin
  selección por rango.

## Criterios de finalización

- Con el modo activo, tocar otra nota la **añade** a la selección; tocarla otra vez la **quita**.
- Tocar la nota seleccionada cuando es la única **cierra** el modo y la barra superior vuelve a la sección.
- Con el modo activo, tocar una nota **no** abre el editor en ningún caso.
- Atrás con el modo activo **cierra** el modo (en cualquier sección) y un segundo atrás hace lo de siempre.
- Sin selección, tocar una nota abre el editor, y tocar una nota de la papelera la restaura, como antes.
- La condición de «selección activa» está escrita **una sola vez**, en la pieza pura, y la pantalla no vuelve
  a escribirla (comprobado por invariante de fuentes, porque la interfaz no se ejecuta en esta plataforma).
- La pieza pura tiene sus casos de prueba en verde y `:app` no suma ninguna clase roja nueva.

## Dudas abiertas

- **[RESUELTA] ¿Atrás debe cerrar el modo selección?** Sí. Se cerró con quien Decide al aprobar esta spec:
  sin un cierre por atrás, la única salida distinta del toque que cancela es la X, y el modo se siente
  atrapado. Es **RF-177** y **RF-178**.
- **[RESUELTA] ¿El toque debe alternar también en la papelera?** No aplica: la papelera no tiene modo
  selección y esta spec no se lo añade (RNF-111). Allí el toque sigue restaurando y el atrás sigue igual.

## Verificación en dispositivo

Ninguna prueba pura ve el gesto real ni la barra superior, y la interfaz necesita Robolectric para ejecutarse,
que no corre en esta plataforma. Con la app instalada y varias notas en la sección de inicio:

1. **Mantener pulsada** una nota: se activa la barra con «1» y la X.
2. **Tocar otra nota**: la barra pasa a «2» y la segunda nota se marca; el editor **no** se abre (RF-173).
3. **Tocar la primera** (ya marcada): la barra vuelve a «1». Tocar de nuevo la que queda: la barra vuelve a ser
   la de la sección y el modo se cierra solo (RF-176).
4. Repetir con **pulsación larga** sobre la segunda nota: el resultado es el mismo que con el toque (RF-175).
5. Repetir la selección y pulsar **atrás**: la barra vuelve a la de la sección, la app **no** se cierra y la
   sección **no** cambia (RF-177).
6. Ir a Favoritos, mantener pulsada una nota y pulsar **atrás**: primero se cierra el modo; un segundo atrás
   vuelve a Inicio (RF-178).
7. Sin ninguna nota seleccionada: tocar una nota **abre** el editor; en la papelera, tocar una nota la
   **restaura**; mantener pulsada una nota en la papelera **no** hace nada (RF-174, RNF-111).
8. Seleccionar dos notas y usar Fijar/Favorito/Archivar/Compartir/Borrar: la acción se aplica a las dos y el
   modo se cierra.

**Cerrada.** El guion se cumplió entero en dispositivo el 2026-10-17: el toque alterna en vez de cancelar, las
cuatro formas (toque y pulsación larga, en cuadrícula y en lista) dan el mismo resultado, atrás cierra el modo
antes que la navegación, y sin selección la lista y la papelera siguen como antes.
