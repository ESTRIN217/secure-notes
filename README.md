# Secure Notes

Un bloc de notas moderno, elegante y seguro con cifrado de extremo a extremo (E2EE), organización avanzada y sincronización en la nube.

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

## 🛡️ Cifrado de Extremo a Extremo (E2EE)
* **Contraseña Maestra:** Protege tus notas confidenciales con una contraseña maestra única de alta seguridad.
* **Algoritmo de Grado Militar (AES-256):** Las notas marcadas como cifradas se encriptan de forma segura utilizando derivación de claves mediante PBKDF2, generando un **Salt** y un **Vector de Inicialización (IV)** aleatorios por cada nota.
* **Privacidad Absoluta:** Los datos cifrados se almacenan localmente en la base de datos de Room. Sin la contraseña maestra, es matemáticamente imposible descifrar o leer el contenido de las notas.

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
