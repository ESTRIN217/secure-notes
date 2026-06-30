# Secure Notes WYSIWYG Rich Text Editor & Color Picker Skill

This document details the architecture, design guidelines, and code patterns utilized in the Secure Notes WYSIWYG rich text editor, Material Design 3 Expressive Outlined styling, custom color selector dialogs, and native regional localization.

---

## 1. Material Design 3 Expressive Outlined Styling

Following modern Material Design 3 guidelines:
- **Expressive Components**: High-contrast, rounded outlines (`RoundedCornerShape` / `CircleShape`), custom card layouts (`OutlinedCard` with thin outlines and crisp borders).
- **Aesthetic Pairings**: Bold typography for structural headings paired with clean, readable fonts. Standard 48dp touch targets on interactive buttons.
- **Dynamic Color Indicators**: Formatting toolbar icons dynamically display the color at the cursor's current position to represent the actual parsed state of the note.

---

## 2. WYSIWYG Rich Text Editor Architecture

The rich text editor is built on top of a single `BasicTextField` / `OutlinedTextField` containing the raw Markdown/HTML tags, coupled with a customized bidirectional index mapper to preserve cursor placement and tag visibility.

### Parsing and Rendering Flow:
1. **Raw Text Input**: The user interacts with a single, unified text input containing formatted inline tags (e.g., `<color=#1976D2>Text</color>`, `<normal>Normal Text</normal>`).
2. **Concealed Visual Mapping**: The `RichTextParser` strips raw formatting tags to construct a visually clean `AnnotatedString` while applying proper `SpanStyle` styling for font, size, foreground color, background color, superscript, subscript, bold, italic, and underline.
3. **Bidirectional Offset Mapping**: A custom tracker maps index position `originalToTransformed` (from raw input text containing tags to clean rendered text) and `transformedToSource` to ensure perfect cursor movement and selections.

---

## 3. Dynamic Color Selector Dialogs

A custom modular dialog (`ColorSelectionDialog`) is implemented for both Font Color and Background Color selection. It offers three distinct inputs:
1. **Predefined Material Colors**: Quick cards presenting preset hex codes for Red (`#D32F2F`), Blue (`#1976D2`), and Green (`#388E3C`).
2. **Color Picker Slider**: A continuous hue-based slider track combined with a real-time visual preview circle and manual hex input box.
3. **Clear Option**: A dedicated button to remove custom formatting tags and revert to the default theme colors.

---

## 4. Native Localized Translations

To support multi-national deployment, strings are extracted natively across three regional configuration files:
- **English (Default)**: `app/src/main/res/values/strings.xml`
- **Spanish (Venezuela - VE)**: `app/src/main/res/values-es-rVE/strings.xml`
- **Portuguese (Brazil - BR)**: `app/src/main/res/values-pt-rBR/strings.xml`

---

## 5. Rich Image and Video Insertion Options

When inserting rich media items into notes, the editor presents an expressive options dialog containing three pathways:
1. **Gallery Integration**: Launches a system-native file and content selection picker (`GetContent` contract) supporting `image/*` and `video/*` MIMEs to load stored files and insert them automatically as rendered HTML tags.
2. **Camera Capture**: Registers specific standard intent contracts (`TakePicture` and `CaptureVideo`) pointing directly to temporary workspace cache paths. Checks and requests dynamic CAMERA permissions at runtime for compliance.
3. **Web Link Input**: Toggles an expressive inline `OutlinedTextField` form allowing immediate manual URL entry.
