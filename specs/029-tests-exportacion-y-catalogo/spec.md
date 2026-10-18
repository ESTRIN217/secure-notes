# Spec 029 — Pruebas de exportación y catálogo de modelos

Estado: **implementada** (2026-10-18), T1-T3 cerradas.

## Contexto y objetivo

Al habilitar los tests de Robolectric en Java 25 reaparecieron fallos previos que estaban ocultos por el error
de acceso al JDK. Hay dos expectativas de tests desfasadas respecto al comportamiento acordado y pruebas PDF que
dependen de una implementación nativa de Android que Robolectric no proporciona. El objetivo es recuperar una
cobertura ejecutable y precisa sin reducir las garantías de los exportadores.

## Usuarios

- Quien exporta notas y espera que sus listas de comprobación conserven el estado marcado.
- Quien consulta el origen de un modelo y espera una dirección HTTPS válida de Hugging Face.
- Quien mantiene las pruebas y necesita comprobar el recorrido de exportación PDF sin que el host finja tener
  el backend nativo del dispositivo.

## Historias de usuario

- **HU-1.** Como quien exporta una nota, quiero que el HTML de los elementos `<item>` conserve texto y estado de
  casilla para poder leer la lista exportada.
- **HU-2.** Como quien consulta un modelo, quiero que su origen visible conserve la URL HTTPS canónica.
- **HU-3.** Como quien mantiene la exportación PDF, quiero probar su flujo en el host sin depender de que
  Robolectric implemente la creación nativa de documentos PDF.

## Requisitos funcionales

- **RF-194.** CUANDO se exporte un elemento legacy `<item checked="true">texto</item>` a HTML, EL SISTEMA
  representa una casilla marcada y conserva el texto del elemento.
- **RF-195.** CUANDO el catálogo exponga el origen de un modelo de Hugging Face, EL SISTEMA incluye el esquema
  HTTPS y el host en la etiqueta y en las URL de descarga y de página del modelo.
- **RF-196.** CUANDO se pruebe la exportación PDF en el host, EL SISTEMA permite comprobar la creación de páginas,
  la escritura del documento y la entrega del fichero sin requerir la implementación nativa Android de
  `PdfDocument`.
- **RF-197.** EL SISTEMA mantiene la generación real de PDF en Android; el doble de pruebas no se usa en la
  aplicación ni altera el formato ni el flujo visible de compartir.

## Requisitos no funcionales

- **RNF-125.** Ninguna prueba se elimina ni se desactiva para ocultar un fallo.
- **RNF-126.** El doble de PDF se limita al entorno de pruebas; producción conserva su backend Android.
- **RNF-127.** No se añaden dependencias ni cadenas visibles.

## Casos límite

- Un `<item>` no marcado conserva una casilla desmarcada y el contenido.
- Cada elemento del catálogo tiene repositorio, URL de descarga HTTPS, URL de página HTTPS y etiqueta de
  cuantización no vacía.
- Las pruebas PDF deben seguir cubriendo documentos de varias páginas y notas con varios tipos de bloque.

## Fuera de alcance

- Cambiar el HTML de producción o la apariencia de la casilla.
- Cambiar las URL de descarga o de página de modelos.
- Validar bytes de PDF producidos por una implementación falsa como sustituto de una prueba en dispositivo.
- Corregir fallos de plataforma o aserciones fuera de los casos identificados.

## Criterios de finalización

- Las pruebas de HTML validan tanto la casilla como el texto sin exigir una estructura interna obsoleta.
- Las pruebas de catálogo fijan que `sourceLabel`, `downloadUrl` y `sourcePageUrl` conservan HTTPS.
- Los casos PDF enumerados pasan en JVM mediante un doble limitado a tests, sin omitir métodos de prueba:
  **9/0**; se mantiene pendiente validar un PDF real en dispositivo.
- `./gradlew :app:testDebugUnitTest --tests '*HtmlExportMediaTest*' --tests '*OnDeviceModelSourceTest*' --tests '*PdfExporterTest*'`
  termina sin fallos: **26/0**.
- `git diff --check` no informa errores y el exportador conserva el backend Android por defecto.

## Dudas abiertas

- Ninguna. El alcance del doble de PDF y las expectativas de HTML y URL se aclararon con la persona usuaria.
