# Changelog — Secure Notes

Formato [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/) y
[SemVer](https://semver.org/lang/es/). Migra y reemplaza los antiguos
`v2.0.md`, `v3.0.md` y `v4.0.md` sueltos de la raíz.

## [Sin publicar] — v4.0 en desarrollo

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
- Documentación: README reescrito, `ARCHITECTURE/CONTRIBUTING/SECURITY/ROADMAP`,
  ADRs, licencias centralizadas (`oss-licenses.json` + `THIRD-PARTY-NOTICES`).

## [3.0] — Settings Overhaul

### Añadido
- Settings Hub central (Apariencia, Idioma, Privacidad y Seguridad, Almacenamiento
  y Datos, Información) con widgets MD3 Expressive compartidos.
- Pantallas dedicadas: Privacy & Security (contraseña maestra), Backup & Restore
  (local SAF + nube), Update Info (GitHub API) y About.
- Modo oscuro tri-estado (Sistema/Apagado/Encendido) y colores dinámicos
  configurables; selección de idioma con cambio instantáneo.
- Respaldo local: exportar/importar notas + etiquetas como JSON.

## [2.0] — Editor enriquecido

### Añadido
- Drag & drop para reordenar notas en rejilla (orden persistente).
- Suite WYSIWYG: negrita/itálica/subrayado/tachado, código en línea,
  sub/superíndices, color de texto y fondo, encabezados, listas, checklists,
  bloques de código, citas, sangrías, enlaces, búsqueda en el editor,
  deshacer/rehacer, familias tipográficas y tamaño de fuente.
- Barra flotante de estilo, más acciones (compartir TXT/Markdown/PDF/HTML/JSON),
  TTS, lienzo de dibujo, grabación de voz y adjuntos.
- Localización EN/ES-VE/PT-BR del editor.

[3.0]: https://github.com/ESTRIN217/secure-notes/releases/tag/v3.0
[2.0]: https://github.com/ESTRIN217/secure-notes/releases/tag/v2.0
