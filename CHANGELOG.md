# Changelog — Secure Notes

Formato [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/) y
[SemVer](https://semver.org/lang/es/). Migra y reemplaza los antiguos
`v2.0.md`, `v3.0.md` y `v4.0.md` sueltos de la raíz.

## [4.0.0] — 2026-10-18

La versión que convierte a Secure Notes en un bloc de notas completo: editor
WYSIWYG por bloques, visores y editor de código como módulos propios, IA 100 %
local, sincronización con Google Drive y temas para toda la app.

### Añadido

**Editor de notas por bloques (reescrito)**
- Editor WYSIWYG por bloques estilo Notion: párrafos, H1–H4, listas (viñetas,
  numerada y de tareas), colapsables, citas, destacados, divisor y enlaces a página.
- Bloques multimedia reales insertables con el menú `/` o la barra `+`: imagen,
  vídeo (local o web, con miniatura 16:9), audio, voz, archivo, marcador web
  (con favicon) y dibujo. Todos se pueden duplicar, mover, convertir y eliminar.
- Formato enriquecido en línea con barra flotante: negrita, itálica, subrayado,
  tachado, código, sub/superíndice, color de texto y de fondo, familia y tamaño;
  `richTextJson` es la fuente de verdad y todo render pasa por `ensureSegments()`.
- Ecuaciones LaTeX en línea, renderizadas y editables desde el propio bloque.
- Tablas: encabezado como fila, tiradores de 48 dp, celdas multilínea y anchos
  adaptativos con scroll horizontal.
- Markdown GFM en el editor (tablas, regla horizontal, auto-enlaces, escapado con
  backslash, blockquotes y listas anidadas) con **vista previa nativa, sin WebView**.
- Menú `/` de comandos, arrastrar y soltar para reordenar, deshacer/rehacer y
  autoguardado.

**Pestañas y visores**
- Pestañas múltiples de notas con LRU (máx. 10; el borrador nunca se cierra solo).
- Pestañas de multimedia, PDF y ficheros de texto dentro de la app, sin cambiar de
  pantalla; los visores exponen slots para integrarse como pestaña.
- Visor PDF (`:visor-pdf`, motor clásico + `androidx.pdf`) y visor multimedia
  (`:visor-media`, Media3/ExoPlayer y Coil3) como módulos propios.

**Editor de código**
- Módulo `:editor-de-codigo`: resaltado de sintaxis, numeración, ajuste de línea,
  pestañas estilo VS Code, búsqueda/reemplazo, barra rápida de símbolos y SAF, con
  Activity propia y también como pestaña del editor de notas.

**IA 100 % local**
- Asistente opcional, **desactivado por defecto**: Ollama / LM Studio en la LAN y
  llama.cpp on-device (catálogo GGUF y `ModelDownloader` desde Hugging Face).
- Chat con streaming, historial persistente y buscable, sesiones vinculadas a notas,
  adjuntos de notas o ficheros como contexto y acciones (escribir, resumir, reescribir,
  traducir, corregir) con vista previa e inserción en la nota.

**Captura rápida**
- Widgets de pantalla de inicio (Glance): nota única con configuración, lista de las
  últimas 7 y nota rápida.
- Burbuja flotante de nota rápida sobre otras apps (servicio en primer plano) y
  mosaico de ajustes rápidos.
- Cinco atajos en la pulsación larga del icono del lanzador, incluidos "nueva nota",
  búsqueda y el editor de código.
- Selección múltiple coherente en la lista (toque, pulsación larga y cierre con atrás).

**Cifrado, copia y datos**
- Cifrado por nota activo por defecto (AES-256-GCM + PBKDF2-HMAC-SHA256, 200 000
  iteraciones, salt e IV por nota) con interruptor en el propio editor.
- Sincronización opcional con Google Drive (`appDataFolder`, scope `drive.appdata`):
  login con Credential Manager, refresh de token, progreso por etapas, sync periódica
  con WorkManager y resolución de conflictos por `lastModified`.
- Copia de seguridad local (SAF) y en la nube que incluye **los ajustes, el tema y los
  adjuntos** (ZIP), con restauración.
- Exportación a TXT/Markdown/PDF/HTML/JSON con adjuntos; **PDF con formato enriquecido
  e imágenes**, **HTML autocontenido** con media web incrustada y **Markdown con
  carpeta `media/`** en ZIP.
- Administrador de almacenamiento con detección de huérfanos, protección de adjuntos
  referenciados y limpieza de caché.

**Diagnóstico, temas y privacidad**
- Registro de fallos **local** con informe revisable y envío a GitHub solo si lo decides.
- Temas de color para toda la app (claro, oscuro, dinámico y catálogo propio) en el
  módulo `:tema`, compartidos por el editor de código y el bloque de nota.
- Registro del sistema **sin contenido del usuario** (23 sitios) y sin Firebase Analytics.
- Actualizaciones con verificación **SHA-256**, selección de APK por ABI, caché de 24 h
  y progreso de descarga.

**Idiomas y documentación**
- **9 locales**: es-419 (neutro LATAM, base), es-VE, es-ES, pt-BR, pt-PT, fr, it,
  en-US y en-GB, incluidas las secciones legales.
- README reescrito con banner y capturas; `ARCHITECTURE`, `GUIDELINES`, `ROADMAP`,
  contribución y seguridad; ADR-0001 a ADR-0004; licencias centralizadas
  (`oss-licenses.json` + `THIRD-PARTY-NOTICES`).

### Cambiado
- El editor de notas se descompuso en capas (`BlockEditor`, `BlockRow` y un componente
  por tipo), con más estado en `StateFlow` y funciones puras para las decisiones.
- El Markdown lo pinta un motor propio (`:markdown`) en **todas** las superficies
  (chat, texto legal, changelog, actualización y vista previa del editor).
- El resaltado de código es **uno solo**, compartido entre el bloque de la nota y el
  editor de código (`:tema`).
- El motor de IA se materializa de forma perezosa a través de `AiModelHost`; no recarga
  los pesos al arrancar.
- La comunicación con la red usa OkHttp para todo (Drive y Ollama/LM Studio).

### Corregido
- Una **nota nueva ya no se duplica** al adjuntar o autoguardar; el editor recuerda el
  id de su propia escritura y atrás guarda sin preguntar por lo ya guardado.
- El editor de código **persiste las URI compartidas** y el administrador de
  almacenamiento respeta su índice de recientes (no borra lo referenciado).
- El **tema elegido viaja y se restaura** con la copia de seguridad.
- Una pestaña de **vídeo web ya no se cierra sola** y se rotula por su identificador.
- Las copias y exportaciones **preservan la estructura de ficheros** y siempre incluyen
  los adjuntos.

### Eliminado
- Firebase Analytics y dependencias declaradas sin uso (Retrofit, Moshi, la librería de
  Markdown de terceros).
- Código y recursos muertos: 69 ficheros y unas 3 500 líneas.

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
