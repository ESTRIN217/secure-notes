# Spec 045 — Accesibilidad y tokens M3 del asistente de IA

Estado: **implementada y verificada en dispositivo** (2026-11-22)

## Contexto y objetivo

La auditoría de diseño M3 del asistente de IA (móvil, tablet y PC) detectó problemas de accesibilidad
y de disciplina de tokens que impiden considerar la interfaz robusta: controles interactivos por
debajo del objetivo táctil mínimo, iconos interactivos sin nombre accesible, acciones alcanzables
solo por gesto de arrastre, y colores literales que no salen del esquema de roles.

Esta spec corrige esos problemas en las superficies del asistente de IA sin cambiar el alcance
funcional, la navegación entre pantallas ni el comportamiento de los modelos. El comportamiento
adaptativo de tablet/PC (breakpoints, list-detail, rail/drawer) queda fuera y se trata en una spec
posterior.

## Usuarios

- Usuario de teléfono que usa el chat con el dedo y necesita objetivos táctiles fiables.
- Usuario que usa lector de pantalla o navegación por foco/teclado y necesita nombres y acciones
  accesibles.
- Usuario con escala de texto grande o tema oscuro/dinámico que necesita contraste y colores
  coherentes con el tema.
- Usuario de tablet o PC que interactúa con puntero o teclado y necesita los mismos controles
  accesibles que en móvil.

## Historias de usuario

- HU-1. Como usuario con lector de pantalla, quiero que cada control interactivo del asistente tenga
  un nombre accesible, para entender qué hace sin depender del icono.
- HU-2. Como usuario táctil, quiero que los botones y controles del chat tengan un objetivo táctil
  suficiente, para poder pulsarlos sin tocar el control equivocado.
- HU-3. Como usuario que no puede usar gestos de arrastre, quiero una forma accesible de hacer las
  acciones que hoy dependen del deslizamiento en una burbuja, para no perder funcionalidad.
- HU-4. Como usuario con tema dinámico u oscuro, quiero que los estados y acentos del asistente usen
  colores del tema, para que se vean coherentes y mantengan contraste.

## Definiciones

- **Superficies del asistente de IA**: pantalla de chat, campo de entrada del chat, bienvenida del
  chat vacío, selector de modelo, panel de gestión de modelos, vista de detalles de modelo, hoja de
  adjuntos, hoja de herramientas, diálogo de mensaje del sistema, historial de chats mostrado en el
  drawer del asistente, pantalla de historial de chats y pantalla de ajustes de IA.
- **Control interactivo**: elemento que recibe pulsación, toque largo, cambio de valor o foco de
  teclado para ejecutar una acción.
- **Objetivo táctil**: área golpeable del control, independiente del tamaño visual del icono.
- **Nombre accesible**: texto expuesto a servicios de accesibilidad que describe la acción o el
  estado del control; no se muestra necesariamente en pantalla.

## Requisitos funcionales

- RF-353: EL SISTEMA expone un nombre accesible en cada control interactivo de las superficies del
  asistente de IA; un icono decorativo que no ejecuta acción ni aporta estado no expone nombre
  redundante.
- RF-354: CUANDO un control interactivo de las superficies del asistente de IA se muestra en pantalla,
  EL SISTEMA le da un objetivo táctil de al menos 48 dp en su eje menor, aunque el icono visible sea
  más pequeño.
- RF-355: EL SISTEMA no depende de gestos de arrastre horizontal en las burbujas de mensaje; las
  acciones de editar, copiar, reenviar, reintentar, insertar y cualquier otra acción de burbuja se
  realizan mediante controles accesibles por foco y lector de pantalla en la fila de acciones de la
  burbuja.
- RF-356: EL SISTEMA no usa colores literales fuera del esquema de roles de Material Theme para
  comunicar estado, acento o acción en las superficies del asistente de IA; los favoritos, estados de
  carga/error/listo y demás indicadores usan roles semánticos o colores derivados del tema.
- RF-357: CUANDO un control solo tiene icono, EL SISTEMA mantiene su significado disponible para
  lector de pantalla y no depende solo del color o de la posición para comunicarlo.
- RF-358: CUANDO el asistente muestra un estado de error, generación, carga, vacío o éxito, EL SISTEMA
  comunica el estado con texto o semántica accesible además de cualquier indicador visual.

## Requisitos no funcionales

- RNF-213: El cambio no añade dependencias nuevas.
- RNF-214: Todo texto visible nuevo o modificado sale de `strings.xml` en los 9 locales.
- RNF-215: La lógica nueva que pueda decidirse sin `Context`/UI se extrae a funciones puras y se
  cubre con pruebas que no dependan de Robolectric.
- RNF-216: Los controles afectados mantienen contraste suficiente según los roles del tema y no
  recortan contenido con escala de texto grande.
- RNF-217: Las burbujas de mensaje no registran gestos de arrastre horizontal para acciones; la fila
  de acciones visible es el único camino para esas acciones.

## Casos límite

- Burbuja de usuario: la acción de editar/reusar está disponible desde la fila de acciones, sin gesto
  de arrastre.
- Burbuja de asistente: copiar, compartir, reintentar, regenerar e insertar (cuando aplique) están
  disponibles desde la fila de acciones, sin gesto de arrastre.
- Botón que alterna entre enviar y detener: conserva objetivo táctil, nombre accesible y estado
  distinto en ambos modos.
- Indicador de contexto/razonamiento/modelo en el campo de entrada: el icono puede ser pequeño, pero
  el área golpeable cumple 48 dp.
- Tema dinámico, oscuro y claro: favorito, error y estado cargado no quedan fijados a un color
  literal incompatible.
- Escala de texto grande: etiquetas y acciones de burbuja no se recortan ni se solapan hasta perder
  la acción.
- Historial en drawer y pantalla de historial: las tarjetas/sesiones conservan nombre accesible,
  objetivo táctil y acciones sin depender solo del icono.

## Fuera de alcance

- Cambiar el layout adaptativo de tablet/PC, breakpoints, list-detail, rail o drawer persistente.
- Rediseñar la jerarquía visual completa del chat, historial o ajustes de IA.
- Cambiar el comportamiento funcional de envío, streaming, modelos, herramientas o adjuntos.
- Añadir nuevas funciones al asistente.
- `ChatSearchScreen`: el usuario indicó que es código muerto; no se corrige en esta spec salvo que se
  decida retirarlo en una tarea aparte.

## Criterios de finalización

- No quedan controles interactivos del asistente de IA con objetivo táctil menor de 48 dp.
- No quedan controles interactivos sin nombre accesible adecuado; los iconos decorativos no añaden
  ruido redundante.
- Las burbujas de mensaje no dependen de gestos de arrastre horizontal; sus acciones se realizan
  desde la fila de acciones accesible.
- No quedan colores literales de estado/acento en las superficies del asistente de IA; los estados
  usan roles del tema.
- Los textos nuevos/modificados están en los 9 locales.
- `./gradlew :app:compileDebugKotlin` y las pruebas puras nuevas pasan; ✅ verificación de TalkBack,
  escala de texto y temas completada en dispositivo (2026-11-22).

## Dudas abiertas

Ninguna. Decisiones confirmadas por el usuario: se quita el swipe horizontal de burbujas y se
mantiene la fila de acciones como camino accesible; `ChatSearchScreen` se considera código muerto y
queda fuera de esta spec; `ChatHistoryScreen` y el historial del drawer se incluyen como parte del
asistente.
