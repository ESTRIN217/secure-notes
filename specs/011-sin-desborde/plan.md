# Plan — Spec 011 · El desborde del tirador no es pulsable

Spec: `specs/011-sin-desborde/spec.md` · Enmienda: `specs/010-carril-estrecho/` · Madre:
`specs/001-limites-constitucion/`

Cubre RF-63 y el RF-61 enmendado, más RNF-25 y los heredados RF-3, RF-19, RF-20, RNF-4, RNF-5,
RNF-6.

## 1. Archivos y responsabilidades

| Archivo | Cambio |
|---|---|
| `ui/EditableTableBlock.kt` | **Único archivo de producción.** `TableRowGutter` a 36 dp; fuera `HandleOverhang`, el desplazamiento de la capa y la compensación del tirador de columna. |
| `ui/table/` | **No se toca.** `gutterWidth` sigue siendo un parámetro de las medidas. |
| `specs/010-carril-estrecho/spec.md` | Nota de que su decisión 2 y su RF-61 quedan enmendados. |
| `MEMORY.md` | La regla y el fallo del desborde. |

## 2. La cuenta que lo cierra

```
0 … 36 dp ────  el carril, dentro de la tarjeta, sin texto
36 … 48 dp ────  el relleno de la primera celda, sin texto
48 dp ──────────  empieza el texto de la primera columna
└── tirador de 48 dp: de 0 a 48, ENTERO dentro de la tarjeta
```

`36 + 12 = 48`: el carril y el relleno suman exactamente lo que mide el tirador. Su borde derecho
cae justo donde empieza el texto, sin tocarlo, y **no hay nada fuera de la tarjeta** (RF-63).

## 3. Los tres cambios

1. **`TableRowGutter = 36.dp`** (era 24). Es el único cambio que sobrevive de la 010.
2. **Fuera `HandleOverhang`**, la constante derivada. Con el desborde a cero no hay nada que derivar.
3. **Fuera el desplazamiento de la capa** (`modifier.offset(x = -HandleOverhang)`) y **fuera la
   compensación del tirador de columna** (`+ overhangPx`). El tirador de fila vuelve a colocarse en
   `IntOffset(0, …)`, que es como estaba antes de la 010, y el de columna a su posición natural.

Con esto el fichero **pierde** código además de cambiarlo (RNF-25): desaparecen dos constantes
derivadas, dos locales y dos comentarios que los explicaban.

## 4. Por qué no se reintenta el carril de 24 dp

Aunque el error de aritmética esté corregido, la capa de tiradores quedaría medio fuera de la tarjeta
y **el mismo problema subiría un nivel**: un punto del dedo fuera de los límites de un padre no
siempre llega a sus hijos. No hay forma de comprobarlo desde aquí, y comprobarlo cuesta una compilación
y una captura. Se recupera lo seguro y se deja escrito por qué (decisión 2 de la spec).

## 5. Estrategia de tests

La geometría **no cambia**: `gutterWidth` es un parámetro, así que su suite debe seguir verde **sin
editar el fichero**. Eso es en sí la comprobación de que el carril no está cableado en ningún sitio.

| Prueba | Qué fija |
|---|---|
| `TableSelectionGeometryTest` sin editar, en verde | que el carril es un parámetro (RNF-23) |
| `the first column starts at the width of the gutter` | que la primera columna arranca en el ancho del carril |
| `a narrower gutter only shifts the outline` | que estrechar el carril desplaza y no redimensiona |
| `TableRowHeightsTest` sin tocar | que el alto por fila no depende del carril |

**Lo que no se puede comprobar aquí**: que el tirador responda en toda su superficie. Ya se comprobó
que **no** respondía con el desborde; con el desborde a cero el tirador está dentro de la tarjeta,
donde antes funcionaba, pero el disparador de pulsación es lo que hay que volver a mirar.

## 6. Decisiones y alternativas descartadas

**Mantener 24 dp y corregir solo la aritmética.** Descartado por la decisión 2 de la spec: el
problema no desaparece, sube de nivel, y no es comprobable desde aquí.

**Bajar el tirador a 36 dp para que quepa en el carril.** Descartado: deja el objetivo táctil por
debajo del mínimo de Material, que es justo lo que la spec 007 vino a corregir y lo que su RNF-16
prohíbe explícitamente.

**Dejar el tirador como estaba (carril de 48 dp).** Descartado: renuncia a los 12 dp sin motivo, ya
que con 36 dp el tirador sigue entero dentro y sin tocar texto.