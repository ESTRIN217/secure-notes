# Spec 047 — Semántica y expresividad del asistente de IA

Estado: implementada (verificación en dispositivo pendiente)

## Contexto y objetivo

Las specs 045 y 046 cubren accesibilidad/tokens básicos y comportamiento adaptativo. Aun así, la
auditoría M3 identificó oportunidades de limpieza semántica y expresiva: algunas superficies usan
patrones menos canónicos, barras superiores con jerarquía mezclada y filas construidas con componentes
custom donde M3 ofrece patrones más claros.

Esta spec alinea las superficies del asistente de IA con semántica M3 y aplica una expresividad
**más marcada** que la contención inicial: además de los momentos de jerarquía y feedback (entrada,
generación, carga, bienvenida), se extiende forma y movimiento a los estados y transiciones de
lista. Las superficies densas siguen siendo legibles y las reglas de accesibilidad de la 045 se
mantienen intactas.

> Nota de numeración: los RF/RNF de este borrador se renumeran a partir de RF-380/RNF-226 porque
> los números RF-369…379 y RNF-223…225 ya los ocupan las specs 046 y su enmienda.

## Usuarios

- Usuario de móvil que necesita entender rápido qué acciones son principales, secundarias o
  destructivas.
- Usuario de tablet/PC que necesita jerarquía clara en historial, paneles de modelo y ajustes de IA.
- Usuario con lector de pantalla o teclado que se beneficia de componentes con semántica M3 más
  predecible.
- Usuario que valora una interfaz pulida con expresividad visible pero sin perder claridad.

## Historias de usuario

- HU-1. Como usuario, quiero que las acciones del chat y del historial sean claras y no compitan
  entre sí, para saber qué tocar sin dudar.
- HU-2. Como usuario, quiero que las barras superiores y listas del asistente sigan patrones
  reconocibles, para orientarme rápido.
- HU-3. Como usuario, quiero que la expresividad visual resalte lo importante también en estados y
  transiciones de lista, para que la interfaz se sienta viva sin distraer.
- HU-4. Como usuario de accesibilidad, quiero que filas, selecciones y estados usen semántica
  predecible, para navegar con lector de pantalla o teclado.

## Definiciones

- **Superficies del asistente de IA**: las definidas en la 045, los paneles adaptativos de la 046 y
  el rail colapsable de su enmienda.
- **Expresividad marcada**: uso intencionado de forma, color, tamaño y movimiento para reforzar
  jerarquía y feedback, incluyendo estados y transiciones de lista, sin convertir cada fila en un
  elemento llamativo.
- **Patrón canónico**: componente o composición M3 cuya semántica y comportamiento son reconocibles
  para la tarea.

## Requisitos funcionales

- RF-380: EL SISTEMA presenta las acciones de burbuja con un patrón M3 claro, **manteniendo todas las
  acciones visibles en la fila** definida en la 045, sin overflow y sin duplicación confusa.
- RF-381: EL SISTEMA usa barras superiores con jerarquía clara: título/contexto, navegación y acciones
  no compiten por el mismo espacio sin prioridad visible.
- RF-382: CUANDO una lista muestra filas de sesión, modelo, herramienta o ajuste, EL SISTEMA usa
  componentes de lista/fila con semántica de selección, estado y acción coherente con M3.
- RF-383: EL SISTEMA aplica expresividad marcada en la entrada, generación, carga y bienvenida, y
  **también** en estados y transiciones de lista (selección, inserción, cambio de destino) con forma
  y movimiento que expliquen el cambio, sin animación decorativa.
- RF-384: CUANDO una acción es destructiva, EL SISTEMA la distingue visual y semánticamente de las
  acciones rutinarias, usando roles de error y texto/no-color.
- RF-385: EL SISTEMA evita contenedores innecesarios; no envuelve filas simples en tarjetas si el
  agrupamiento puede comunicarse con espaciado, divisores o componentes de lista.
- RF-386: CUANDO un control icon-only permanece por densidad, EL SISTEMA le da nombre accesible,
  objetivo táctil suficiente y contexto visual coherente; no introduce icon-only ambiguos.
- RF-387: EL SISTEMA conserva el comportamiento funcional existente de chat, historial, modelos,
  herramientas y ajustes mientras aplica la limpieza semántica.
- RF-388: EL SISTEMA **no modifica `ChatHistoryScreen`** como pantalla separada; su jerarquía,
  barra y filas se dejan igual que tras la 045/046.

## Requisitos no funcionales

- RNF-226: El cambio no añade dependencias nuevas sin justificarlo en la spec y en la lista de
  licencias del APK.
- RNF-227: Todo texto visible nuevo o modificado sale de `strings.xml` en los 9 locales.
- RNF-228: Los cambios de semántica/componente mantienen accesibilidad: nombre, rol, estado, foco,
  contraste y objetivo táctil (RF-353..358 de la 045 siguen vigentes).
- RNF-229: La expresividad respeta `ReduceMotion` y no introduce animación obligatoria para
  comprender el estado.
- RNF-230: Las decisiones visuales nuevas usan roles/tokens del tema; no se introducen colores
  literales para estados o acentos.

## Casos límite

- Burbuja con todas las acciones visibles en ventana estrecha: la fila no se vuelve ambigua, no
  pierde objetivos táctiles y no recorta etiquetas críticas.
- Pantalla de historial con fijados y recientes: la jerarquía distingue vacío, fijados y recientes
  sin exceso de contenedores; `ChatHistoryScreen` no se toca (RF-388).
- Panel de modelos con estados descargando/cargado/error/oculto: el estado se entiende por semántica
  y texto, no solo por color o icono, con expresividad en el cambio de estado.
- Cambio de destino Chat↔Modelos en el rail: la transición explica el cambio sin retardar la tarea.
- Escala de texto grande y ReduceMotion: top bars, filas y chips no recortan etiquetas y la
  expresividad se reduce a lo estático cuando el sistema lo pide.

## Fuera de alcance

- Reintroducir swipe en burbujas o reemplazar la fila accesible definida en la 045; tampoco añadir
  overflow (decisión del usuario: todas visibles).
- Modificar `ChatHistoryScreen` (decisión del usuario: déjala igual).
- Cambiar la lógica de negocio de IA, sincronización, cifrado o almacenamiento.
- Rediseñar áreas fuera del asistente de IA.
- `ChatSearchScreen`.

## Criterios de finalización

- Las acciones de burbuja siguen todas visibles y accesibles por fila, sin duplicación.
- Las top bars y listas principales del asistente usan patrones M3 más canónicos y semántica clara.
- La expresividad está marcada en entrada/carga/bienvenida y en estados/transiciones de lista, con
  respeto a ReduceMotion.
- No se introducen colores literales ni regresiones de accesibilidad.
- `ChatHistoryScreen` queda sin cambios.
- Los textos nuevos/modificados están en los 9 locales.
- `:app:compileDebugKotlin` y pruebas relevantes pasan; revisión visual/accesibilidad en dispositivo
  queda pendiente.

## Dudas abiertas

Ninguna. Decisiones confirmadas por el usuario: todas las acciones de burbuja visibles (sin
overflow), `ChatHistoryScreen` se deja igual, y expresividad más marcada (también en estados y
transiciones de lista).
