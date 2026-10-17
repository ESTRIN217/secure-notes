# MEMORY.md — secure-notes

Memoria entre sesiones. Mantenerla breve (máximo ~50 líneas); las reglas permanentes viven en `AGENTS.md`.

## Estado actual
- **Spec 026 cerrada: implementada y verificada en dispositivo** (2026-10-17), T1–T7: en la lista de notas, el
  **toque alterna** la selección (antes la vaciaba) y **atrás cierra el modo**. Specs 016–026 implementadas y
  verificadas.
- Pruebas: `:app` **799/41** (786 + las 13 nuevas de la 026: `NoteSelectionTest` 9 y
  `NoteSelectionInvariantsTest` 4), `:tema` 53/0, `:editor-de-codigo` 149/2. Los 41 son los de siempre:
  Robolectric de plataforma + las 3 preexistentes. **Ninguno nuevo.** `:app:assembleDebug` compila.
- **`:tema` es un módulo nuevo**: el catálogo de temas se fue de `:editor-de-codigo` y de `:app/ui/theme/`. Al
  buscar paletas o el tema, ahora están ahí, no en el editor ni en la app.
- Ya no queda trabajo ajeno sin commitear: el `BackHandler` de `MainListScreen.kt` y la config de herramientas
  (`opencode.json`, `.opencode/agents/coordinator.md`) se commitearon el 2026-10-17.

## Decisiones que conviene recordar
- **Selección de notas: una sola regla (`ui/NoteSelection`)**. Alternar y «hay selección activa» viven en una
  pieza pura que comparten la barra, los dos toques, las dos pulsaciones largas y el atrás (RF-179); antes el
  toque y la pulsación larga hacían lo contrario. Alternativa descartada: repetir la comprobación en cada
  lambda, que es lo que produjo el defecto.
- **El atrás cierra la capa transitoria antes que la navegación**: primero la selección, y solo con la
  selección vacía vuelve a Inicio.
- **El tono de Material 3 es L\* de CIELAB, no OKLab L**: tomarlos por la misma escala dejó las superficies
  oscuras en `#000101` en vez de `#121316`. Traducción en `ligereza(tone)`.
- Un acento cumple **dos** contrastes: 3:1 con la superficie y 4.5:1 con `onPrimary`.
- **Catálogo compartido = módulo propio** cuando la dependencia va en un solo sentido.
- `dynamic` **no tiene paleta propia**: editor → `VSCodeDark`, nota → `Standard`. Por eso `LocalAppTheme` lleva
  el `AppTheme` entero y no una paleta derivada.
- Migración sin escribir nada: sin `theme_id` + interruptor apagado → `default`; nada → `dynamic`. La clave
  antigua solo se lee, nunca se borra.
- `ColorScheme` tiene 3 constructores: solo pasar los 48 roles con nombre es no ambiguo y no deja morados.
- El índice de archivos del editor se consulta por la fachada `CodeEditor`; comprobar `databaseExists` antes de
  abrir Room. El motor de IA solo se materializa mediante `AiModelHost`. Markdown vive en `:markdown`.

## Aprendizajes
- Un `BackHandler` que lee un estado obliga a **declarar ese estado por encima** de él: una variable local
  declarada más abajo no está en su ámbito (Kotlin lo dijo en la compilación). Es el primer consumidor, así que
  la declaración se subió junto a él.
- Un recorte de **invariante por marcadores** debe acabar antes de las reglas que **sí** deben cumplirse: el
  recorte largo (`hasta fun NoteCardItem(`) se llevaba las acciones por lotes, que limpian la selección al
  terminar, y daba rojo por algo correcto.
- `Color` es una value class: `assertEquals` con literales hexadecimales necesita `.toInt()`.
- Compilar Kotlin **no** valida `strings.xml`; el apóstrofo sin escapar sale en `mergeDebugResources`. Hay una
  prueba (`ThemeLabelsTest`) que mira los nueve ficheros antes.
- Las decisiones comprobables sin `Context` van en funciones puras; el `Context` se queda en quien llama.
- Los invariantes de fuente quitan comentarios y comprueban que realmente leyeron el archivo.
- Una superficie que no cambia un ajuste no necesita `ViewModel`: leer la preferencia una vez con `remember`.

## Próximos pasos y notas
- Verificar en dispositivo la spec 020 provocando un fallo real.
- Spec 002: generar los dos goldens pendientes y fijar `roborazzi.test.verify`.