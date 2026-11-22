# Spec 046 — Asistente de IA adaptativo en tablet y PC

Estado: **implementada y verificada en dispositivo** (2026-11-22)

## Contexto y objetivo

Tras la spec 045, el asistente de IA cumple los objetivos básicos de accesibilidad y tokens M3, pero
su comportamiento adaptativo sigue siendo de dos modos: compacto (<600 dp) y grande (≥600 dp). En
tablet y PC no hay una estructura canónica que aproveche el ancho disponible: el historial se abre
como drawer modal o pantalla aparte, el panel de gestión de modelos cubre toda la pantalla y la
navegación no cambia de presentación según la ventana.

Esta spec define el comportamiento adaptativo del asistente de IA por ventana disponible, para que
móvil, tablet y PC ofrezcan la misma capacidad con una estructura adecuada a su espacio.

**Enmienda 2026-11-22 (RF-370…RF-379 / RNF-223…RNF-225)**: tras ver la app en tablet y compararla
con las referencias (Gemini y llama.cpp), la estructura de tres paneles y el panel de modelos como
side sheet estrecho están mal resueltos: las cabeceras de la tabla de modelos se parten letra por
letra y el espacio se reparte entre tres zonas. Se sustituye por un **rail lateral permanente
colapsable** con todos los destinos y conversaciones, y un **área principal única** que cambia de
destino (chat o modelos a ancho completo). Esta enmienda deja sin efecto los RF-362/RF-363/RF-364
originales y su implementación.

## Usuarios

- Usuario de teléfono que necesita el flujo actual de chat, historial y modelos sin perder claridad.
- Usuario de tablet que quiere ver historial y conversación con más contexto, sin navegar por
  overlays completos.
- Usuario de PC o ventana ancha que quiere productividad: contexto visible, paneles secundarios
  acotados y navegación persistente cuando el espacio lo permite.
- Usuario que cambia el tamaño de ventana, rota el dispositivo o usa pantalla dividida.

## Historias de usuario

- HU-1. Como usuario de tablet, quiero ver historial y chat con una relación clara, para cambiar de
  conversación sin perder el contexto actual.
- HU-2. Como usuario de PC, quiero que los paneles secundarios no tapen toda la conversación, para
  seguir trabajando mientras gestiono modelos o veo detalles.
- HU-3. Como usuario de móvil, quiero que el chat siga siendo simple y de una sola tarea principal,
  para no perder foco en pantallas pequeñas.
- HU-4. Como usuario que redimensiona la ventana, quiero que el asistente conserve mi conversación,
  selección y estado de entrada, para no perder trabajo.

## Definiciones

- **Ventana disponible**: ancho/alto utilizable por el asistente en el momento actual, no la clase
  física del dispositivo.
- **Compacto**: ventana con ancho menor de 600 dp.
- **Medio**: ventana con ancho de 600 dp a 839 dp.
- **Expandido**: ventana con ancho de 840 dp a 1199 dp.
- **Ancho/Extra ancho**: ventana con ancho de 1200 dp o más.
- **Panel secundario**: superficie que complementa la tarea principal (historial, gestión de modelos,
  detalles de modelo, detalle de contexto) sin sustituir la conversación como tarea primaria.

## Requisitos funcionales

- RF-359: EL SISTEMA adapta la estructura del asistente de IA según la ventana disponible, usando al
  menos los cortes Compacto, Medio, Expandido y Ancho/Extra ancho.
- RF-360: MIENTRAS la ventana es Compacta, EL SISTEMA presenta la conversación como tarea principal y
  muestra historial, selector de modelo y paneles secundarios como superficies temporales o pantallas
  de navegación, sin layout de varios paneles permanentes.
- RF-361: MIENTRAS la ventana es Media o mayor, EL SISTEMA ofrece un patrón canónico que permita
  acceder al historial sin ocultar completamente la conversación, salvo que la ventana cambie a
  Compacta.
- RF-362: MIENTRAS la ventana es Expandida o mayor, EL SISTEMA muestra historial y conversación en
  relación list-detail, manteniendo visible la conversación activa cuando el usuario navega por el
  historial.
- RF-363: CUANDO el usuario abre el panel de gestión de modelos en ventana Media o mayor, EL SISTEMA
  lo muestra como side sheet o panel secundario acotado, manteniendo visible el contexto de la
  conversación.
- RF-364: CUANDO el usuario abre la vista de detalles de un modelo en ventana Media o mayor, EL
  SISTEMA la muestra como side sheet o panel secundario acotado, sin cubrir toda la ventana.
- RF-365: CUANDO cambia el tamaño de la ventana, la orientación o el modo de pantalla dividida, EL
  SISTEMA conserva la conversación activa, el texto en edición, los adjuntos pendientes, la selección
  de modelo visible y el panel secundario equivalente cuando siga siendo válido.
- RF-366: CUANDO la ventana pasa de ancha a Compacta, EL SISTEMA conserva la tarea en curso y
  transforma los paneles persistentes en su equivalente compacto sin perder el estado.
- RF-367: EL SISTEMA mantiene un ancho de lectura cómodo para la conversación en ventanas anchas,
  evitando que burbujas y contenido de mensaje se estiren a todo el ancho disponible.
- RF-368: EL SISTEMA mantiene la identidad y el orden de los destinos/contextos del asistente al
  cambiar de presentación adaptativa.
- RF-369: MIENTRAS la ventana es Compacta, EL SISTEMA mantiene la pantalla de historial como destino
  de navegación separado; MIENTRAS la ventana es Media o mayor, EL SISTEMA usa la experiencia
  adaptativa de historial integrada al asistente, sin obligar a salir a una pantalla aparte para
  cambiar de conversación.

## Requisitos no funcionales

- RNF-218: El cambio no añade dependencias nuevas sin justificarlo en la spec y en la lista de
  licencias del APK.
- RNF-219: Todo texto visible nuevo o modificado sale de `strings.xml` en los 9 locales.
- RNF-220: La lógica de decisión de layout adaptativo que no dependa de UI se extrae a funciones
  puras con pruebas sin Robolectric.
- RNF-221: Los paneles secundarios no ocultan acciones esenciales del chat en ventanas Medias o
  mayores.
- RNF-222: El comportamiento adaptativo se verifica en bordes de breakpoint, rotación, pantalla
  dividida, teclado/IME visible y escala de texto grande.

## Casos límite

- Ventana justo por debajo y por encima de 600/840/1200 dp: la estructura cambia sin perder estado.
- Pantalla dividida en tablet: puede pasar de Expandido a Compacto o Medio.
- Teclado visible en ventana Media: el campo de entrada y el contenido visible siguen siendo usables.
- Panel de modelos abierto y ventana reducida a Compacto: el panel pasa a su equivalente compacto.
- Conversación con streaming activo durante redimensionado: el contenido visible y el scroll se
  conservan de forma razonable.
- Historial vacío en layout de dos paneles: el panel de historial muestra estado vacío sin romper el
  panel de conversación.

## Fuera de alcance

- Cambiar el comportamiento funcional de envío, generación, modelos, herramientas o adjuntos.
- Rediseñar el contenido visual de las burbujas más allá de lo necesario para el layout adaptativo.
- `ChatSearchScreen`, salvo decisión explícita de retirarlo o incorporarlo.
- Adaptar otras áreas de la app fuera del asistente de IA.

## Criterios de finalización

- El asistente usa cortes por ventana disponible, no solo <600/≥600.
- En Media/Expandida/Ancha hay patrón canónico para historial y paneles secundarios que no tapa toda
  la conversación.
- El redimensionado conserva estado de conversación, entrada y selección relevante.
- Los textos nuevos/modificados están en los 9 locales.
- `:app:compileDebugKotlin` y pruebas puras nuevas pasan; ✅ verificación de breakpoints, rotación,
  split screen e IME completada en dispositivo/emulador (2026-11-22).

## Enmienda (2026-11-22, RF-370…RF-379 / RNF-223…RNF-225)

Tras ver la app en tablet y compararla con las referencias, se corrige la estructura adaptativa.
**Dejan de aplicar** RF-362 (list-detail historial+chat), RF-363 (modelos como side sheet acotado) y
RF-364 (detalle de modelo como side sheet); la implementación anterior de esos tres requisitos se
retira. RF-361, RF-365…RF-369 siguen vigentes.

- RF-370: MIENTRAS la ventana es Media o mayor, EL SISTEMA muestra un **rail lateral permanente** en
  el asistente de IA, con los destinos Nuevo chat, Búsqueda, Modelos, Ajustes y el resto de destinos
  de la app, más las conversaciones recientes y fijadas.
- RF-371: EL SISTEMA permite **colapsar el rail** a una versión de solo iconos en Media o mayor, y
  conserva el destino activo y su selección al colapsar/expandir.
- RF-372: CUANDO el usuario selecciona **Modelos** en el rail en Media o mayor, EL SISTEMA muestra la
  gestión de modelos como **destino del área principal a ancho completo**, sustituyendo al chat, con
  la tabla Model/Contexto/Estado/Acciones legible y sin columnas partidas.
- RF-373: CUANDO el usuario selecciona **Chat** en el rail en Media o mayor, EL SISTEMA muestra la
  conversación en el área principal con el saludo y las sugerencias existentes, conservando el
  campo de entrada flotante.
- RF-374: EL SISTEMA no muestra simultáneamente tres paneles (historial + chat + modelos) en ningún
  ancho; en Media o mayor como máximo rail + un área principal.
- RF-375: EL SISTEMA mantiene el ancho de lectura cómodo del chat (RF-367) y acota la tabla de
  modelos al ancho disponible sin partir cabeceras ni contenido.
- RF-376: EL SISTEMA conserva las sugerencias del chat vacío (spec 045) en todos los anchos; la
  enmienda no las sustituye por un saludo estático.
- RF-377: MIENTRAS la ventana es Compacta, EL SISTEMA conserva el drawer de historial, el selector
  de modelo y el panel de gestión de modelos como superficies temporales; el rail permanente y el
  cambio de destino en el área principal son solo de Media o mayor.
- RF-378: EL SISTEMA reutiliza los destinos y su orden de la navegación principal de la app en el
  rail del asistente (identidad de destinos coherente con `NavigationRailContent`).
- RF-379: CUANDO cambia el destino en el rail en Media o mayor, EL SISTEMA conserva la conversación
  activa, el texto en edición y los adjuntos pendientes para volver al chat sin pérdida.

- RNF-223: El rail colapsable, el cambio de destino y el ancho de la tabla se deciden con funciones
  puras sin `android`, probables sin Robolectric.
- RNF-224: Todo texto visible nuevo o modificado sale de `strings.xml` en los 9 locales.
- RNF-225: La tabla de modelos usa anchos relativos/ponderados que no parten palabras; se verifica
  en los bordes de breakpoint y con escala de texto grande.

## Dudas abiertas

Ninguna. Decisiones confirmadas por el usuario para la enmienda: enmendar la 046 (no spec nueva),
modelos como destino de ancho completo tipo llama.cpp (074005), rail con Ajustes y el resto de
destinos, rail colapsable tipo 074019, y conservar las sugerencias del chat vacío.