# Tasks — Spec 011 · El desborde del tirador no es pulsable

Plan: `specs/011-sin-desborde/plan.md` · Enmienda: `specs/010-carril-estrecho/`

2 tareas. El cambio cabe en un solo archivo de producción.

---

- [x] **T1. Carril de 36 dp y fuera el desborde.** RF-63, RNF-25
  - `TableRowGutter` pasa a 36 dp: el carril y el relleno de la celda suman los 48 dp del tirador, así
    que su borde derecho cae donde empieza el texto.
  - Fuera la constante `HandleOverhang`, el desplazamiento de la capa de tiradores y la compensación
    del tirador de columna. El tirador de fila vuelve a colocarse en el borde izquierdo, que es como
    estaba antes de la 010.
  - El comentario de `TableRowGutter` cuenta por qué no se baja más y qué se comprobó en dispositivo.
  - Hecho cuando: `grep -n "HandleOverhang\|overhangPx\|handleLeftPx" app/src/main/java/com/example/ui/EditableTableBlock.kt`
    no devuelve nada, `TableRowGutter = 36.dp`, y el diff **elimina** líneas además de cambiarlas.

- [x] **T2. Cierre técnico, suite y memoria.** RF-63, RNF-23, RNF-25
  - La suite de geometría, de altos y del modelo en verde **sin editar** `TableSelectionGeometryTest.kt`.
  - `./gradlew :app:compileDebugUnitTestKotlin` y `./gradlew :app:assembleDebug`.
  - `git diff -- app/src/main/java/com/example/ui/table/` y `git diff -- app/src/main/res` vacíos.
  - `specs/010-carril-estrecho/spec.md`: nota de que su decisión 2 y su RF-61 quedan enmendados.
  - `MEMORY.md`: un tirador más ancho que el carril solo funciona entero dentro.
  - Hecho cuando: todo termina bien y `MEMORY.md` nombra la regla y el fallo.
  - **Pendiente que no se puede cerrar aquí**: en dispositivo, que el tirador de fila responda en
    **toda** su superficie. Con el desborde a cero está entero dentro de la tarjeta, que es donde antes
    funcionaba, pero es el mismo disparador de pulsación que hay que volver a mirar.