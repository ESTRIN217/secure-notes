# Secure Notes

Un bloc de notas moderno, elegante y seguro con cifrado de extremo a extremo (E2EE), organización avanzada y sincronización en la nube.

---

[![Latest release](https://img.shields.io/github/v/release/ESTRIN217/Bloc-de-notas?style=for-the-badge&labelColor=0d1117)](https://github.com/ESTRIN217/Bloc-de-notas/releases)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

## 📸 Capturas de Pantalla

Para mantener la consistencia visual y un diseño limpio en cualquier pantalla, puedes visualizar la interfaz aquí:

<table align="center">
  <tr>
    <td align="center">
      <img src="https://github.com/ESTRIN217/Bloc-de-notas/blob/master/assets/im%C3%A1genes/vista-principal.png" width="220" alt="Vista Principal (Material 3)"/>
      <br><b>Vista Principal</b>
    </td>
    <td align="center">
      <img src="https://github.com/ESTRIN217/Bloc-de-notas/blob/master/assets/im%C3%A1genes/editor.png" width="220" alt="Editor Enriquecido"/>
      <br><b>Editor Flotante</b>
    </td>
    <td align="center">
      <img src="https://github.com/ESTRIN217/Bloc-de-notas/blob/master/assets/im%C3%A1genes/busqueda-y-filtro.png" width="220" alt="Búsqueda Dinámica"/>
      <br><b>Búsqueda y Filtros</b>
    </td>
  </tr>
</table>

> 💡 *Nota técnica sobre imágenes:* En este repositorio, para ajustar el tamaño de las imágenes de forma personalizada, utilizamos la etiqueta HTML `<img>` con el atributo `width="220"` dentro de tablas, lo que permite un alineado perfecto y responsivo en GitHub.

---

## 🌎 Idiomas Soportados / Supported Languages / Idiomas Suportados
* **Español (VE)** - Traducción nativa completa.
* **Português (BR)** - Tradução nativa completa.
* **English (US/UK)** - Default locale with complete support.

---

## 🎨 Características de Diseño / Design & UI
* **Material Design 3 Expresivo:** Una interfaz limpia, moderna y altamente responsiva que sigue las directrices oficiales de diseño de Material 3 con bordes perfilados (*outlined*), contrastes óptimos y espaciado generoso.
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
* **Formato Limpio:** Editor de texto fluido y minimalista con soporte para títulos y cuerpo de notas amplios.
* **Asociación de Etiquetas:** Permite asignar múltiples etiquetas personalizadas para categorizar las notas.
* **Personalización de Fondo (Colores):** Cambia el color de fondo de tus notas individuales utilizando una paleta pastel optimizada para legibilidad (Azul, Verde, Amarillo, Rosa, Púrpura, Naranja) o el color por defecto del sistema.

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

## 🛠️ Stack Tecnológico
* **Lenguaje:** Kotlin
* **UI:** Jetpack Compose (Material Design 3)
* **Persistencia Local:** Room Database (SQLite con migraciones robustas)
* **Seguridad:** API de Criptografía de Android, PBKDF2 y AES-256
* **Arquitectura:** MVVM (Model-View-ViewModel) con flujos reactivos `StateFlow`

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
