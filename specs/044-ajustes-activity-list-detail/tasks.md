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
