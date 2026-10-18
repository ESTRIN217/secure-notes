# Spec 029 — Tareas

## Rastro

Tareas aprobadas para corregir solo las pruebas identificadas y el seam aislado de PDF.

- [x] **T1. Alinear las expectativas de checklist HTML y URL de Hugging Face.** RF-194, RF-195, RNF-125
  - Actualizar las pruebas para comprobar el checkbox marcado y el texto, y para esperar HTTPS en `sourceLabel`.
  - Hecho cuando:
    `./gradlew :app:testDebugUnitTest --tests '*HtmlExportMediaTest*' --tests '*OnDeviceModelSourceTest*' --rerun`
    termina en `BUILD SUCCESSFUL`, sin cambiar código de producción.
  - **Hecho.** 17 pruebas, 0 fallos; solo cambiaron las expectativas de test.

- [x] **T2. Probar exportación PDF mediante un backend sustituible en tests.** RF-196, RF-197, RNF-126
  - Primero sustituir en `PdfExporterTest` el uso directo de `PdfDocument` por el backend de prueba previsto y
    comprobar el rojo de compilación por la interfaz ausente; después añadir la interfaz/fábrica y adaptar el
    exportador para conservar el backend Android por defecto.
  - El doble registra las páginas, escritura y cierre; sus bytes no se validan como PDF real. Mantener las pruebas
    de render, paginación y exportación sin saltar casos.
  - Hecho cuando:
    `./gradlew :app:testDebugUnitTest --tests '*PdfExporterTest*' --rerun`
    termina en `BUILD SUCCESSFUL` y el diff confirma que no se quitó ninguna anotación `@Test`.
  - **Hecho.** Las 9 pruebas pasan. El backend Android real queda por defecto; el doble prueba render, número de
    páginas, escritura y cierre sin afirmar que sus bytes sean un PDF.

- [x] **T3. Validar juntas las pruebas afectadas y cerrar la spec.** RNF-125, RNF-127
  - Hecho cuando:
    `./gradlew :app:testDebugUnitTest --tests '*HtmlExportMediaTest*' --tests '*OnDeviceModelSourceTest*' --tests '*PdfExporterTest*' --rerun`
    termina en `BUILD SUCCESSFUL`; `git diff --check` queda limpio y `MEMORY.md` refleja los resultados medidos.
  - **Hecho.** 26 tests, 0 fallos; `git diff --check` limpio.

## Cierre

- [x] No se añadieron dependencias ni cadenas visibles.
- [x] `PdfExporter` usa el backend Android real por defecto.
- [x] Las pruebas no presentan los bytes del doble como un PDF real.
