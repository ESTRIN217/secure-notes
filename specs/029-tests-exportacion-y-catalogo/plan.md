# Spec 029 — Plan

> Plan de la spec 029, aprobada. No cambia la salida HTML ni las URL visibles; el seam PDF solo hace sustituible
> el backend en pruebas y mantiene la implementación Android como ruta por defecto.

## Ficheros y responsabilidades

### `app/src/main/java/com/example/util/export/PdfDocumentBackend.kt` — nuevo

- Define una interfaz interna pequeña para iniciar/cerrar páginas, escribir el documento y cerrar el backend.
- La implementación Android delega cada operación en `android.graphics.pdf.PdfDocument`.
- Expone el `Canvas` de la página activa para que `PdfBlockRenderer` siga pintando con el mismo código.
- Una fábrica interna crea el backend Android por defecto; la fábrica se puede sustituir desde los tests del módulo.

### `app/src/main/java/com/example/util/export/PdfExporter.kt` — modificado

- Recibe la fábrica con una ruta por defecto que crea el backend Android.
- `exportSingle` y `exportMultiple` usan la interfaz para el ciclo de vida del documento; el diseño, las medidas,
  el contenido y el uso compartido no cambian.
- Los fallos de producción mantienen el `catch`, registro y aviso actuales.

### `app/src/test/java/com/example/PdfExporterTest.kt` — modificado

- Añade un backend falso que registra páginas, finalización, escritura y cierre, y crea Canvas respaldados por
  bitmaps para que el renderer real se pueda ejecutar.
- Sustituye las llamadas directas a `PdfDocument` de los tests por el backend falso.
- Deja de afirmar que bytes fabricados por el doble son un PDF Android válido. Conserva la cobertura de estilos,
  bloques, adjuntos, render web, migración legacy, salto de página y entrega del fichero.
- El caso de exportación inyecta el doble en `PdfExporter` y comprueba el archivo y las operaciones observables.

### `app/src/test/java/com/example/HtmlExportMediaTest.kt` — modificado

- La prueba de `<item checked="true">` comprueba la casilla HTML marcada y el texto, sin exigir que el texto sea
  hijo inmediato de `<li>`.
- La producción de `RichTextConverter` no cambia.

### `app/src/test/java/com/example/OnDeviceModelSourceTest.kt` — modificado

- Ajusta los dos asserts de `sourceLabel` para el contrato acordado: la URL incluye `https://`.
- Mantiene las comprobaciones de repositorio, URL directa, URL de página, archivo y cuantización.
- `OnDeviceModel` no cambia.

### `MEMORY.md`

- Registra la causa de los fallos y la frontera de cobertura: el doble verifica el flujo, no sustituye la
  validación de un PDF real en Android.

## Algoritmo

```text
crear backend:
    en producción → AndroidPdfDocumentBackend(PdfDocument())
    en tests      → RecordingPdfDocumentBackend

exportar páginas:
    canvas = backend.startPage(pageInfo)
    renderer dibuja sobre canvas
    cuando corresponde:
        backend.finishPage()
        canvas = backend.startPage(siguientePageInfo)
    backend.finishPage()
    backend.writeTo(output)
    backend.close()
    compartir el fichero como hoy
```

## Decisiones

1. **Fábrica y backend, no sustituir `PdfDocument` por herencia.** La clase Android no ofrece un constructor de
   test fiable en Robolectric: la instancia nace cerrada porque el host no implementa su método nativo. El borde
   mínimo sustituible son las operaciones que usa el exportador. Se descarta cambiar el renderer o emular dentro
   del test los bytes internos de PDF.
2. **Canvas real respaldado por bitmap en el doble.** Permite ejecutar el renderer existente y verificar alturas y
   paginación sin fingir que el documento serializado sea válido. Se descarta no-op Canvas porque no ejercitaría
   las llamadas de dibujo.
3. **Actualizar dos expectativas, no la producción.** El HTML ya emite casilla accesible e interactiva; el origen
   del modelo ya expone una URL HTTPS. Se descarta retirar esos comportamientos solo para satisfacer tests
   obsoletos.
4. **No añadir dependencias ni instrumentación.** El seam y el doble usan APIs existentes, y la validación de
   bytes PDF reales queda para dispositivo.

## Estrategia de pruebas

- Primero comprobar que los asserts actuales fallan solo por la forma anidada del checklist y por el esquema URL.
- Ejecutar la prueba PDF aislada y confirmar que el falso captura el flujo de creación, paginación, escritura y
  cierre, sin invocar el constructor nativo `PdfDocument`.
- Ejecutar:
  `./gradlew :app:testDebugUnitTest --tests '*HtmlExportMediaTest*' --tests '*OnDeviceModelSourceTest*' --tests '*PdfExporterTest*' --rerun`
- Ejecutar `git diff --check`.
- No se usa `./gradlew test` como puerta de esta spec: la suite global tiene fallos preexistentes de plataforma y
  de otras áreas ya documentados en `AGENTS.md`.

## Cobertura de requisitos

| Parte | RF/RNF |
|---|---|
| Assert de HTML legacy | RF-194, RNF-125 |
| Expectativas de origen del catálogo | RF-195, RNF-125 |
| Backend Android intercambiable y tests de flujo PDF | RF-196, RF-197, RNF-125, RNF-126 |
| Sin dependencias ni recursos nuevos | RNF-127 |
