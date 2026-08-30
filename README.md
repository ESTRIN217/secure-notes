# Secure Notes

Un bloc de notas moderno, elegante y seguro con cifrado de extremo a extremo (E2EE), organización avanzada y sincronización en la nube.

---

[![Latest release](https://img.shields.io/github/v/release/ESTRIN217/secure-notes?style=for-the-badge&labelColor=0d1117)](https://github.com/ESTRIN217/secure-notes/releases)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

## 📸 Capturas de Pantalla

Para mantener la consistencia visual y un diseño limpio en cualquier pantalla, puedes visualizar la interfaz aquí:

<table align="center">
  <tr>
    <td align="center">
      <img src="https://github.com/ESTRIN217/secure-notes/blob/main/assets/Screenshot_20260703-173051.png" width="220" alt="Vista Principal (Material 3)"/>
      <br><b>Vista Principal</b>
    </td>
    <td align="center">
      <img src="https://github.com/ESTRIN217/secure-notes/blob/main/assets/Screenshot_20260703-173055.png" width="220" alt="Editor Enriquecido"/>
      <br><b>Editor Flotante</b>
    </td>
    <td align="center">
      <img src="https://github.com/ESTRIN217/secure-notes/blob/main/assets/Screenshot_20260703-174048.png" width="220" alt="Búsqueda Dinámica"/>
      <br><b>Búsqueda y Filtros</b>
    </td>
  </tr>
</table>

> 💡 *Nota técnica sobre imágenes:* En este repositorio, para ajustar el tamaño de las imágenes de forma personalizada, utilizamos la etiqueta HTML `<img>` con el atributo `width="220"` dentro de tablas, lo que permite un alineado perfecto y responsivo en GitHub.

---

## 🌎 Idiomas Soportados / Supported Languages / Idiomas Suportados
* **Español (VE)** - Traducción nativa completa.
* **Português (BR)** - Tradução nativa completa.
* **Français (FR)** - Traduction complète des sections légales.
* **English (US/UK)** - Default locale with complete support.

---

## 🎨 Características de Diseño / Design & UI
* **Material Design 3 Expresivo:** Una interfaz limpia, moderna y altamente responsiva que sigue las directrices oficiales de diseño de Material Desing 3 Expresive.
* **Modo Oscuro / Claro Automático:** Soporte completo para temas claros y oscuros respetando la configuración del sistema, adaptando los colores de las notas para una lectura sumamente cómoda.
* **Vista en Rejilla y Lista:** Alternancia fluida entre visualización en cuadrícula o lista compacta (preferencia guardada automáticamente).

---

## ⚙️ Configuración y Widgets Compartidos
* **Settings Hub:** Pantalla central de configuración con secciones organizadas: Apariencia, Idioma, Privacidad y Seguridad, Almacenamiento y Datos, Información.
* **Widgets MD3 Expressive:** Sistema de componentes compartidos (`SettingsSectionTitle`, `SettingsIconContainer`, `SettingsCardGroup`, `SettingsSwitchTile`, `SettingsListTile`) con consistencia visual: bordes de 1.5dp, esquinas de 28dp, iconos de 44dp.
* **ViewModels Separados:** `ThemeViewModel` (tema/idioma), `BackupViewModel` (respaldos), `UpdaterViewModel` (actualizaciones) para mejor separación de responsabilidades.

---

## 🛡️ Privacidad y Seguridad
* **Contraseña Maestra:** Protege tus notas confidenciales con una contraseña maestra única de alta seguridad.
* **Algoritmo de Grado Militar (AES-256):** Las notas marcadas como cifradas se encriptan de forma segura utilizando derivación de claves mediante PBKDF2, generando un **Salt** y un **Vector de Inicialización (IV)** aleatorios por cada nota.
* **Privacidad Absoluta:** Los datos cifrados se almacenan localmente en la base de datos de Room. Sin la contraseña maestra, es matemáticamente imposible descifrar o leer el contenido de las notas.
* **Pantalla de Privacidad:** Sección dedicada para configurar o eliminar la contraseña maestra con confirmación de seguridad.

---

## 📝 Editor de Notas Avanzado
* **Editor WYSIWYG por Bloques:** Editor fluido estilo Notion con bloques (texto, títulos H1–H4, listas, tablas, citas…) y formato enriquecido en línea (negrita, color, subíndice, superíndice, ecuaciones, enlaces…).
* **Formato Limpio:** Editor de texto fluido y minimalista con soporte para títulos y cuerpo de notas amplios.
* **Asociación de Etiquetas:** Permite asignar múltiples etiquetas personalizadas para categorizar las notas.
* **Personalización de Fondo (Colores):** Cambia el color de fondo de tus notas individuales utilizando una paleta pastel optimizada para legibilidad (Azul, Verde, Amarillo, Rosa, Púrpura, Naranja) o el color por defecto del sistema.

> 📖 **Documentación completa del editor** (bloques, formato y referencia técnica): [docs/EDITOR.md](docs/EDITOR.md)

---

## 🔍 Búsqueda Inteligente y Filtros Avanzados (SearchScreen)
* **Historial de Búsquedas Recientes:** Guarda y gestiona de forma interactiva tus búsquedas previas con chips de sugerencias rápidas.
* **Búsqueda en Tiempo Real:** Busca instantáneamente dentro del título y contenido de tus notas (incluyendo notas cifradas si se ha desbloqueado la sesión).
* **Filtros Dinámicos e Interactivos:**
  * ⭐ **Favoritos:** Filtra rápidamente para mostrar solo notas destacadas.
  * 📦 **Archivadas:** Muestra u oculta notas archivadas para mantener tu espacio limpio.
  * 🏷️ **Etiquetas:** Menú desplegable interactivo para filtrar notas por cualquier etiqueta existente.
  * 🎨 **Colores:** Filtra notas de manera visual por su color de fondo específico.

---

## 🔄 Sincronización en la Nube y Exportación
* **Sincronización con Google Drive:** Vincula tu cuenta para realizar copias de seguridad automáticas y restaurar tus notas de forma segura en cualquier dispositivo.
* **Múltiples Formatos de Exportación:** Guarda tus notas localmente o compártelas en formatos estándar de la industria:
  * Texto Plano (`.txt`)
  * Markdown (`.md`)
  * PDF de alta fidelidad
  * HTML Web enriquecido
  * Respaldo crudo JSON

---

## 🤖 Asistente IA Local (AI Chat)
* **Chat IA 100% Local:** Conversa con un modelo de lenguaje directamente desde la app, sin que tus notas o mensajes lleguen a servicios en la nube. Función opcional, desactivada por defecto.
* **Dos Backends Seleccionables:**
  * 📱 **On-Device (llama.cpp):** Chat basado en llama.cpp — inferencia nativa de modelos GGUF ejecutada en el propio dispositivo (arm64-v8a), sin conexión de red.
  * 🖥️ **Backend llama.cpp / Ollama (HTTP Local):** Conexión configurable por URL a un servidor LLM en tu red local (Ollama, LM Studio). Ningún dato abandona tu LAN.
* **Acciones de Escritura Inteligente:** Generar texto, resumir, reescribir con estilos (formal, casual, poético, profesional), traducir, acortar, corregir gramática y explicar — sobre toda la nota o la selección actual.
* **Contexto por Adjuntos:** Adjunta otras notas o archivos de texto al chat como contexto adicional; se muestran como chips removibles antes de enviar el mensaje.
* **Historial de Chats:** Sesiones persistentes en Room con renombrado, fijado, borrado y exportación de conversaciones.
* **Streaming e Inserción:** Respuestas generadas token a token en tiempo real, con opción de insertar el resultado directamente en tu nota.

---

## ⚖️ Términos y Privacidad

Secure Notes incluye una pantalla **"Términos y Privacidad"** accesible desde Ajustes > Legal y desde Acerca de > Enlaces Útiles.

- **Autenticación**: Google OAuth maneja la autenticación. La pantalla de consentimiento de Google rige los permisos de cuenta.
- **Términos de Uso**: La app se proporciona "tal cual". El usuario es responsable de su contraseña maestra.
- **Privacidad**: Sin contraseña maestra, es matemáticamente imposible descifrar las notas.

## 🤖 Uso de Inteligencia Artificial

La asistencia de IA es **opcional** y se ejecuta íntegramente en tu dispositivo o en tu red local, según el backend seleccionado. Declaración completa en la app (Ajustes > Legal):

- Funciones de IA **OPCIONALES**, desactivadas por defecto y con consentimiento explícito.
- Backend **On-Device (llama.cpp):** el modelo se ejecuta en el dispositivo; no requiere conexión de red.
- Backend **Ollama / LM Studio:** conexión HTTP a un servidor LLM local; ningún dato sale de tu LAN.
- Nunca se envían datos a APIs en la nube ni a terceros, y nada se usa para entrenar modelos externos.

## 🛡️ Soberanía de Datos (Client-Side Absolute)

| Aspecto | Detalle |
|---------|---------|
| **Cifrado** | AES-256-GCM con autenticación integrada |
| **Derivación de clave** | PBKDF2 con HMAC-SHA256, 200,000 iteraciones |
| **Sal e IV** | Aleatorios por nota (SecureRandom) |
| **Contraseña maestra** | Nunca sale del dispositivo, solo en memoria durante la sesión |
| **Backups cloud** | Cifrados localmente *antes* de subir a Google Drive |
| **Acceso de terceros** | Imposible sin la contraseña maestra |

## ☁️ Google Drive

La integración con Google Drive es **opcional** y se usa exclusivamente para:

- **Copia de seguridad** en la carpeta AppData (inaccesible para el usuario).
- **Autenticación OAuth 2.0** via Credential Manager.
- **Cifrado client-side**: los backups se cifran con tu contraseña maestra antes de transmitirse.
- **Desconexión total**: puedes revocar el acceso en cualquier momento desde Ajustes.

## 📜 Licencias de Código Abierto

Pantalla dedicada en **Ajustes > Legal > Licencias** listando todas las dependencias principales:

| Librería | Licencia |
|----------|----------|
| Kotlin | Apache 2.0 |
| Jetpack Compose | Apache 2.0 |
| Material 3 | Apache 2.0 |
| Room | Apache 2.0 |
| OkHttp | Apache 2.0 |
| Retrofit | Apache 2.0 |
| Moshi | Apache 2.0 |
| Coil | Apache 2.0 |
| Firebase (Google) | Apache 2.0 |
| Kotlin Coroutines | Apache 2.0 |
| WorkManager | Apache 2.0 |
| Android Biometric | Apache 2.0 |
| llama.cpp | MIT |
| compose-markdown | Apache 2.0 |
| Robolectric | MIT |
| JUnit 4 | EPL 2.0 |
| App (MIT) | MIT — ESTRIN217 |

---

## 🛠️ Stack Tecnológico
* **Lenguaje:** Kotlin
* **UI:** Jetpack Compose (Material Design 3 Expressive)
* **Persistencia Local:** Room Database (SQLite con migraciones robustas)
* **Seguridad:** API de Criptografía de Android, PBKDF2 y AES-256
* **IA Local:** llama.cpp nativo (JNI/NDK, arm64-v8a) · OkHttp para backends HTTP locales (Ollama / LM Studio)
* **Arquitectura:** MVVM (Model-View-ViewModel) con flujos reactivos `StateFlow`

---

## 🧠 Software Engineering Principles

This project is developed following industry‑standard practices to ensure maintainability, testability, and long‑term quality:

### DRY (Don't Repeat Yourself)
Shared Compose widgets (`SettingsSectionTitle`, `SettingsCardGroup`, `SettingsSwitchTile`, `SettingsListTile`) centralize repeated UI patterns. Rich‑text parsing and rendering logic lives in `RichTextParser` — a single source of truth for the custom tag system.

### SOLID
| Principle | How it's applied |
|---|---|
| **SRP** | ViewModels handle one screen; `NoteDao` owns DB access; `EncryptionUtils` owns crypto; Composables are pure UI. |
| **OCP** | New note formats or export types are added by extending parameters, not modifying existing functions. |
| **LSP** | `DarkModeOption` enum values are fully substitutable; `BackupViewModel` treats local and cloud backups uniformly. |
| **ISP** | Fine‑grained composable parameters instead of wide interfaces. UI callbacks use single‑method Kotlin lambdas. |
| **DIP** | Dependencies injected via constructor (no static singletons). `GoogleDriveSyncService`, `NoteDao`, and `EncryptionUtils` are test‑friendly abstractions. |

### Clean Code
- Functions ≤ 20 lines with single responsibility.
- Intention‑revealing names (`encryptContent`, `toggleDarkMode`, `searchNotes`).
- Guard clauses replace deep nesting.
- Comments document *why* (edge cases, design trade‑offs), never *what*.

### KISS (Keep It Simple, Stupid)
- Single‑module app with manual constructor DI — no framework overhead.
- `StateFlow` + `collectAsState()` for reactive UI — no additional reactive libraries.
- Room as the sole persistence layer — no separate cache, no ORM.

### YAGNI (You Aren't Gonna Need It)
- No repository abstraction until a second data source is introduced.
- Interfaces declared only when substitution (testing, alternate impl) is actually required.
- No feature flags, dead code, or speculative navigation routes.

### Error Handling & Robustness
- All async operations return sealed `UiState` (Loading / Success / Error).
- Input validated at the UI boundary; encryption failures surfaced as explicit `EncryptionResult` types.
- No silent exception swallowing — every `catch` either logs or re‑wraps.
- Room I/O and file operations run on `Dispatchers.IO`; crypto failures never crash the UI.

---

## Licencia

Este proyecto está licenciado bajo la Licencia MIT. Consulta el archivo [LICENSE](LICENSE) para obtener más detalles.

---

<p align="center">
  Desarrollado con pasión por <b>ESTRIN217</b>.
</p>

<p align="center">
  Hecho con ❤️ en Venezuela.
</p>
