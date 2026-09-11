# Editor WYSIWYG por bloques y estilo MD3 Expressive — Skill

 Patrones de UI y edición de Secure Notes: editor por bloques estilo Notion,
 Material Design 3 Expressive y localización en 9 idiomas.

> Todo el código de esta skill sigue `AGENTS.md` y los principios de
> [docs/GUIDELINES.md](docs/GUIDELINES.md): DRY, SOLID, código limpio, KISS,
> YAGNI y manejo de errores con `Result` + `Log.e()`.
> Manual de usuario: [docs/EDITOR.md](docs/EDITOR.md) ·
> guía de desarrollo: [docs/EDITOR_DEV.md](docs/EDITOR_DEV.md).

---

## 1. Material Design 3 Expressive

- **Componentes expresivos**: tarjetas con esquinas generosas (`RoundedCornerShape`
  28dp en grupos de ajustes), bordes finos y tipografía marcada para títulos.
- **Widgets compartidos** (`com.example.ui.settings`): `SettingsSectionTitle`,
  `SettingsCardGroup`, `SettingsSwitchTile`, `SettingsListTile` — no duplicar
  patrones de ajustes fuera de ellos.
- **Indicadores dinámicos**: los botones del toolbar flotante reflejan el formato
  bajo el cursor (estado parseado real, no asumido).
- **Toques de 48dp** en botones interactivos; colores dinámicos y modo
  claro/oscuro según el sistema.

---

## 2. Arquitectura del editor por bloques

El editor es **presentacional (controlado)**: `BlockEditor` recibe los bloques y
reporta cambios hacia arriba. Nada se persiste dentro del editor.

```
NoteEditorScreen (estado local + persistencia)
  └─ BlockEditor(blocks, onBlocksChange, …)
       └─ BlockRow(block, …)              // dispatcher: when (block.type)
            ├─ EditableTextBlock          // base de todo bloque de texto
            ├─ Checklist / Collapsible / Table / Code / Image / Video / Audio…
            └─ ReadOnlyTextBlock
```

- **Fuente de verdad**: `richTextJson` (`TextSegment.serialize`); `content`
  (markup HTML-like) solo existe para compatibilidad legacy y exportación.
- **Regla de oro**: al escribir `onChange`, guardar SIEMPRE `richTextJson`; todo
  render y todo cambio pasa por `ensureSegments()`.
- **Flujo de escritura**: `EditableTextBlock` trabaja con `AnnotatedString`
  construido desde `TextSegment`s y conserva el cursor con `OffsetMapper`.
- **Menú de bloques**: tecla `/` (`SlashCommandMenu`, `BLOCK_COMMANDS`) o botón
  `+` de la barra flotante (`FloatingEditorToolbar`, modos MAIN / TEXT_FORMAT /
  SEARCH). Enter divide, Backspace en vacío fusiona/elimina, arrastrar reordena.
- **Conversión**: `RichTextConverter` (`markupToSegments`, `segmentsToMarkup/Html/Md`,
  `applySpanStyle`); parseo legacy en `RichTextParser`/`HtmlTagParser`.

---

## 3. Diálogos de color y formato

`ColorSelectionDialog` (color de letra y resaltado): paleta de colores predefinidos,
deslizador de tono con vista previa, entrada hexadecimal manual y opción de limpiar
formato. El formato se aplica a la **selección** o como **modo de escritura** si no
hay selección (ver tabla de formatos en `docs/EDITOR.md`).

---

## 4. Localización nativa (9 locales)

Todo string nuevo va en los 9 `strings.xml`: `values` (en), `values-es-rVE`,
`values-es-rES`, `values-b+es+419`, `values-pt-rBR`, `values-pt-rPT`, `values-fr`,
`values-it`, `values-en-rGB`. Las secciones legales están traducidas en todos.

---

## 5. Inserción de imagen y vídeo

Vía menú `/` o barra `+` como **bloques reales** (no tags sueltos): galería,
cámara o URL (con miniatura 16:9 para YouTube/shorts). Se guardan `fileUri`,
`fileName`, `caption`, `align` y `wysiwyg` en `meta`. Al añadir un bloque nuevo,
seguir el checklist de `docs/EDITOR_DEV.md` §4 (enum, `BlockRow`, slash menu,
preview, exportadores y test de round-trip).
