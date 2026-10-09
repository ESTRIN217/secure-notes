# Tasks — Spec 010 · El carril del tirador de fila se estrecha

Plan: `specs/010-carril-estrecho/plan.md` · Suelo: `specs/005-`, `007-`, `008-`, `009-`

3 tareas. El cambio cabe en un solo archivo de producción, así que se hace de una vez y se verifica
al final.

---

- [x] **T1. Constantes y posición del tirador de fila.** RF-60, RNF-23
  - `TableRowGutter` pasa a 24 dp y deja de ser `HandleWidth`.
  - `HandleOverhang` **derivado** de `HandleWidth - TableRowGutter`, escrito como resta y no como
    número, para que no pueda quedar inconsistente si el carril cambia.
  - `HandleWidth` sigue en 48 dp.
  - El tirador de fila se coloca en `gutter − 48` en vez de en `0`: su borde derecho llega al final
    del carril y se detiene donde empieza el texto.
  - `CellHorizontalPadding` como constante, para que la cuenta del plan sea legible en el código.
  - Hecho cuando: `grep -n "HandleWidth = \|TableRowGutter = \|HandleOverhang = " app/src/main/java/com/example/ui/EditableTableBlock.kt`
    muestra 48 dp en el tirador y un carril menor.

- [x] **T2. Que el desborde reciba el toque.** RF-61
  - La capa de tiradores se desplaza a la izquierda lo mismo que el desborde, para que sus límites
    contengan el tirador completo.
  - El tirador de columna suma ese mismo desfase a su posición, para que siga centrado en su columna.
  - Hecho cuando: `compileDebugKotlin` termina sin errores y el diff muestra el desfase **en los dos
    sitios**: en la capa y en la posición del tirador de columna. Desplazar solo uno de los dos
    descoloca el tirador de columna.

- [x] **T3. Cierre técnico, suite y memoria.** RF-62, RNF-23, RNF-24
  - La suite de geometría en verde **sin editar `TableSelectionGeometryTest.kt`**, y un caso nuevo que
    fije que la primera columna empieza en el ancho del carril.
  - `./gradlew :app:compileDebugUnitTestKotlin` y `./gradlew :app:assembleDebug`.
  - `git diff -- app/src/main/java/com/example/ui/table/` y `git diff -- app/src/main/res` vacíos.
  - `MEMORY.md`: un carril más estrecho que el tirador obliga a alinearlo por su borde derecho.
  - Hecho cuando: todo lo anterior termina bien y `MEMORY.md` nombra la regla.
  - **Pendiente que no se puede cerrar aquí**: en dispositivo, que los 12 dp desbordados reciban el
    toque. Si no responden, el plan alternative es ensanchar el carril a 36 dp, con lo que los 48 dp
    entran justos dentro de la tarjeta sin tocar texto y se recuperan 12 dp en vez de 24.