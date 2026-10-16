# MEMORY.md — secure-notes

Memoria entre sesiones. Mantenerla breve (máximo ~50 líneas); las reglas permanentes viven en `AGENTS.md`.

## Estado actual
- **Spec 024 cerrada, implementada y verificada en dispositivo**, T1–T8: archivos del editor excluidos de
  huérfanos; permisos persistentes para URI `content://`. Decisiones y guion: `specs/024-.../`.
- **Specs 023 y 022 implementadas y verificadas en dispositivo.** Specs 016–019 también verificadas.
- **Spec 021 implementada y verificada en dispositivo**: la persona usuaria confirmó los atajos
  manteniendo pulsado el icono del lanzador.
- **Spec 020 implementada**, falta la verificación manual provocando un fallo real.
- `./gradlew test` no pasa en este entorno: Robolectric carece de runtime nativo para `linux/aarch64`.
  Línea base tras 024: `:app` 746/41; `:editor-de-codigo` 149/2; fallos conocidos documentados en specs.
- Para 024, compilan `:editor-de-codigo` y `:app`; las pruebas `StorageEditorFiles` pasan. Las dos fallas
  del editor son las mismas de plataforma (`DefaultNativeRuntimeLoader`).

## Decisiones que conviene recordar
- El índice de archivos del editor se consulta por la fachada `CodeEditor`; comprobar `databaseExists` antes
  de abrir Room, pues abrirla crea la base. Las reglas de almacenamiento están en `AGENTS.md`.
- Persistir permisos dentro de `readExternalFile`/`readExternalFolder`: cubre picker e intents externos;
  pedir lectura/escritura con caída a lectura. No crear categoría nueva para archivos del editor.
- La app reutiliza módulos existentes; no añadir dependencias sin justificarlo en la spec.
- El motor de IA se materializa solo mediante `AiModelHost` en `SecureNotesApplication`; no cargarlo desde
  `AiViewModel.init`.
- Markdown vive en `:markdown`, se comparte por pantalla; solo el chat adapta el markup heredado. El cargador
  de imágenes locales no tiene red.

## Aprendizajes
- Las decisiones comprobables sin `Context` deben vivir en funciones puras; el `Context` se queda en quien llama.
- Los invariantes de fuente quitan comentarios y comprueban que realmente leyeron el archivo; medir el repo
  antes de fijar umbrales.
- Comparar regresiones por clase de test, no solo por número; `--rerun` evita confundir un build sin tests con verde.
- `strings.xml` requiere apóstrofo escapado (`\'`); compilar Kotlin no valida recursos.
- En el XML de atajos, los extras son literales; `LaunchedEffect` puede repetirse tras rotación, retirar el extra.
- Consultar `AGENTS.md` para reglas de listas Compose, I/O, crash log, pestañas, almacenamiento y rendimiento.

## Próximos pasos y notas
- Verificar en dispositivo la spec 020 provocando un fallo real.
- Spec 015 redactada como borrador: incluye estado/selección del editor y tablas en preview/TTS;
  excluye el rediseño de diálogos de medios. Falta aprobarla antes de planificar o implementar.
- Spec 002: generar los dos goldens pendientes y fijar `roborazzi.test.verify`.
- `values-en-rGB` ahora contiene las 1.179 claves del inglés base; `docs/EDITOR.md` apunta al resaltador
  compartido de la spec 022. Los defectos de tabla/TTS/preview y selección quedan descritos en la 015.
