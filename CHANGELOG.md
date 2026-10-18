# Changelog — Secure Notes

Formato [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/) y
[SemVer](https://semver.org/lang/es/). Migra y reemplaza los antiguos
`v2.0.md`, `v3.0.md` y `v4.0.md` sueltos de la raíz.

## [4.0.0] — 2026-10-18

### Añadido
- Autenticación con Google (Credential Manager, `GetSignInWithGoogleOption`) y
  sincronización real con Google Drive (`appDataFolder`, scope `drive.appdata`,
  refresh automático de token, progreso por etapas, sync periódica con WorkManager
  y resolución de conflictos por `lastModified`).
- Asistente IA: `AIService` con backends Ollama/LM Studio (HTTP) y on-device
  (llama.cpp vía binding `llama.android`, catálogo GGUF, `ModelDownloader`);
  acciones Escribir/Resumir/Reescribir/Traducir con vista previa e inserción.
- Markdown avanzado: tablas, regla horizontal, auto-links, escapado con backslash,
  blockquotes y listas anidadas; exportadores HTML/Markdown actualizados.
- Herramientas de código (`CodeEditorActivity`, resaltado, números de línea, SAF).
- Gestor de archivos: importación (TXT/MD/HTML/JSON/PDF), visor PDF (`:visor-pdf`,
  motor clásico + `androidx.pdf`), visor multimedia (`:visor-media`, Media3).
- Pestañas múltiples de notas y multimedia (LRU, máx 10, borrador protegido).
- Burbuja flotante con nota rápida y permiso `SYSTEM_ALERT_WINDOW`.
- Registro de fallos local con informe revisable y envío a GitHub solo si lo decides
  (spec 020).
- Cinco atajos en la pulsación larga del icono del lanzador (spec 021).
- Resaltado de código compartido entre el bloque de la nota y el editor de código
  (spec 022).
- Reproducción de vídeo web dentro de la app: YouTube, *shorts*, `youtu.be` y
  archivos de vídeo directos; el resto abre fuera (spec 023).
- Temas de color para toda la app: catálogo compartido (`:tema`) con claro, oscuro
  y dinámico, y modo `dynamic` sin paleta propia (spec 025).
- Selección múltiple coherente en la lista (spec 026).
- Documentación: README reescrito, `ARCHITECTURE/CONTRIBUTING/SECURITY/ROADMAP`,
  ADRs, licencias centralizadas (`oss-licenses.json` + `THIRD-PARTY-NOTICES`).

### Corregido
- Una nota nueva ya no se duplica al adjuntar o autoguardar; el editor recuerda el
  id de su propia escritura y atrás guarda sin preguntar (spec 027).
- El editor de código persiste las URI compartidas y el administrador de
  almacenamiento respeta su índice de recientes (spec 024).
- El tema elegido viaja y se restaura con la copia de seguridad (spec 028).
- Una pestaña de vídeo web ya no se cierra sola y se rotula por su identificador
  (spec 023).

### Cambiado
- La vista previa del Markdown y el asistente usan un motor GFM propio (`:markdown`)
  en lugar de una librería de terceros (specs 017-019).
- Suite de pruebas desbloqueada y verificada en verde (spec 002); pruebas dirigidas
  de exportación, catálogo y PDF, y captura de bloqueo sin Android Keystore
  (specs 029, 030).

## [3.0.0] — Settings Overhaul

### Añadido
- Settings Hub central (Apariencia, Idioma, Privacidad y Seguridad, Almacenamiento
  y Datos, Información) con widgets MD3 Expressive compartidos.
- Pantallas dedicadas: Privacy & Security (contraseña maestra), Backup & Restore
  (local SAF + nube), Update Info (GitHub API) y About.
- Modo oscuro tri-estado (Sistema/Apagado/Encendido) y colores dinámicos
  configurables; selección de idioma con cambio instantáneo.
- Respaldo local: exportar/importar notas + etiquetas como JSON.

## [2.0.0] — Editor enriquecido

### Añadido
- Drag & drop para reordenar notas en rejilla (orden persistente).
- Suite WYSIWYG: negrita/itálica/subrayado/tachado, código en línea,
  sub/superíndices, color de texto y fondo, encabezados, listas, checklists,
  bloques de código, citas, sangrías, enlaces, búsqueda en el editor,
  deshacer/rehacer, familias tipográficas y tamaño de fuente.
- Barra flotante de estilo, más acciones (compartir TXT/Markdown/PDF/HTML/JSON),
  TTS, lienzo de dibujo, grabación de voz y adjuntos.
- Localización EN/ES-VE/PT-BR del editor.

[4.0.0]: https://github.com/ESTRIN217/secure-notes/releases/tag/v4.0.0
[3.0.0]: https://github.com/ESTRIN217/secure-notes/releases/tag/v3.0.0
[2.0.0]: https://github.com/ESTRIN217/secure-notes/releases/tag/v2.0.0
