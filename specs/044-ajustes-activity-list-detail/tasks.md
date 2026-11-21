- [x] **T1. Redactar spec, plan y tareas.** RF-316…RF-325, RNF-206…RNF-210
  - Hecho cuando: los tres archivos existen y la spec está aprobada.
- [x] **T2. Añadir las dependencias adaptativas y su licencia.** RNF-207
  - Hecho cuando: `libs.versions.toml`, `app/build.gradle.kts` y `oss-licenses.json` incluyen
    `adaptive` y `adaptive-navigation` (BOM 2026.09.00).
- [x] **T3. `SettingsDestination` puro y su test en rojo→verde.** RF-316, RF-324, RF-325, RNF-210
  - Hecho cuando: `SettingsDestinationTest` pasa sin Robolectric y desconoce valores → `HUB`.
- [x] **T4. Extraer los factories de ViewModels compartidos.** RNF-208
  - Hecho cuando: `MainActivity` usa los factories nuevos y compila sin cambiar su comportamiento.
- [x] **T5. Crear `SettingsActivity` con manifiesto, tema y candado.** RF-321, RNF-206, RNF-208, RNF-209
  - Hecho cuando: la Activity no está exportada, respeta el candado y compila.
- [x] **T6. Extraer las filas de categoría de `SettingsScreen.kt`.** RF-319
  - Hecho cuando: `SettingsCategoryDetail.kt` pinta las filas de las ocho categorías y reutiliza
    `SettingsWidgets`; el antiguo `SettingsScreen` queda fuera del árbol.
- [x] **T7. Montar el `NavigableListDetailPaneScaffold`.** RF-317, RF-318, RF-320, RF-322
  - Hecho cuando: lista y detalle conviven ≥600 dp, el detalle ocupa el ancho por debajo y atrás
    retrocede o cierra.
- [x] **T8. Redirigir todos los accesos y limpiar los `Screen` de ajustes.** RF-316, RF-323, RF-324
  - Hecho cuando: rail, atajo, lista y extra de actualización usan `launchSettings`, y `ScreenSaver`
    ya no conoce esas rutas.
- [x] **T9. Invariante de accesos y verificación final.** RNF-206, RNF-210
  - Hecho cuando: el test de fuentes pasa, `assembleDebug` da BUILD SUCCESSFUL y `MEMORY.md` queda
    actualizado.
- [x] **T10. Enmienda de interfaz: catálogo puro y test en rojo→verde.** RF-326…RF-328, RNF-212
  - Hecho cuando: `SettingsCategoryCatalogTest` valida ocho categorías, destinos únicos, grupos y
    `matchesQuery`, sin Robolectric.
- [x] **T11. Panel izquierdo: búsqueda, grupos, filas ricas y primero seleccionado.** RF-326…RF-329
  - Hecho cuando: el campo filtra, el vacío muestra mensaje, las filas llevan subtítulo y check, y
    la primera categoría queda seleccionada en dos paneles.
- [x] **T12. Panel de detalle: título + X y secciones agrupadas.** RF-330, RF-331
  - Hecho cuando: no hay `TopAppBar`, hay título grande con cierre y cabeceras de sección con tarjetas.
- [x] **T13. Idioma en el detalle y textos en nueve locales.** RF-332, RF-333, RNF-211
  - Hecho cuando: el detalle de Idioma pinta las opciones del bottom sheet compartidas con él, y los
    textos nuevos existen en los nueve `strings.xml`.
- [x] **T14. Verificación final de la enmienda.** RNF-212
  - Hecho cuando: compila, pasan las pruebas dirigidas, `assembleDebug` da BUILD SUCCESSFUL y
    `MEMORY.md` queda actualizado.
- [x] **T15. Previews de la interfaz de ajustes.** RF-326…RF-332 (herramienta de diseño)
  - Hecho cuando: `SettingsPreviews.kt` muestra lista, cabecera, tres detalles y opciones de idioma
    sin ViewModels, y compila.
- [x] **T16. Categoría IA directa a `AiSettingsScreen`.** RF-334
  - Hecho cuando: seleccionar IA en la lista abre la pantalla de IA sin página intermedia, el
    interruptor sigue disponible y compile/assemble pasan.
- [x] **T17. Categoría Privacidad directa a `PrivacySettingsScreen`.** RF-335
  - Hecho cuando: seleccionar Privacidad en la lista abre su pantalla sin página intermedia y
    compile/assemble pasan.
- [x] **T18. Sección Almacenamiento en la lista con Copia y Administrador.** RF-336
  - Hecho cuando: la lista muestra la nueva sección con dos entradas que abren sus screens en el
    detalle, sin página intermedia, y compile/assemble pasan.
- [x] **T19. Sección Legal en la lista con Términos y Licencias.** RF-337
  - Hecho cuando: la lista muestra la nueva sección con dos entradas que abren sus screens en el
    detalle, sin página intermedia, y compile/assemble pasan.
- [x] **T20. Sección Aplicación e información con Actualizaciones, Registro y Acerca.** RF-338
  - Hecho cuando: las tres entradas abren sus screens en el detalle, el changelog ya no es modal,
    los dos textos nuevos están en 9 locales y compile/assemble pasan.
- [x] **T21. Búsqueda con contenido del detalle.** RF-339
  - Hecho cuando: el índice puro cubre los 12 destinos, la lista filtra con él y las pruebas
    dirigidas y el assemble pasan.
- [x] **T22. Topbar global en móvil y detalle sin barras propias.** RF-340, RF-341
  - Hecho cuando: móvil tiene una MediumTopAppBar con back, las sub-pantallas no tienen barra ni
    back propios, tablet/PC muestra título + X en todo el detalle y compile/assemble pasan.
- [x] **T23. X para borrar la búsqueda y sin Text superior.** RF-342
  - Hecho cuando: el campo muestra la X con texto y la borra, no hay título sobre la barra y
    compile/assemble pasan.
- [x] **T24. Restaurar el scroll tras la topbar global.** RF-343
  - Hecho cuando: el Scaffold y su contenido tienen altura acotada, la lista usa weight(1f) y
    compile/assemble pasan (gesto pendiente de comprobar en dispositivo).
- [x] **T25. Chevron en las filas solo en móvil.** RF-344
  - Hecho cuando: el check desaparece, el chevron aparece solo por debajo de 600 dp y
    compile/assemble pasan.
- [x] **T26. Barra sin Scaffold global y nestedScroll en la lista.** RF-345
  - Hecho cuando: la topbar es primer hijo del Column, el scaffold adaptativo usa weight(1f), el
    LazyColumn recibe el nestedScroll y compile/assemble pasan (gesto pendiente en dispositivo).
- [x] **T27. Mover el nestedScroll al contenedor para que scrollee el list.** RF-346
  - Hecho cuando: el connection está en el Box común, el LazyColumn del list ya no lo lleva y
    compile/assemble pasan (gesto a confirmar en dispositivo).
  - **REVESADO** por RF-347: el connection en el Box rompió el scroll de ambos panes.
- [x] **T28. Efecto título sin nestedScroll (scroll pasivo).** RF-347
  - Hecho cuando: no hay ningún nestedScroll connection en ajustes, el título cambia de estilo con
    el `firstVisibleItemScrollOffset` de la lista y compile/assemble pasan.
- [x] **T29. Padding de barra de estado en el contenido.** RF-348
  - Hecho cuando: `consumeWindowInsets` se sustituye por `windowInsetsPadding` condicional (completo
    en tablet, laterales+abajo en móvil) y compile/assemble pasan.
