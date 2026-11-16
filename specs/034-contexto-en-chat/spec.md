# Spec 034 — Contexto en el chat

Fecha: 2026-11-15
Estado: **implementada y verificada en dispositivo** (2026-11-15)
RF-248…RF-257 · RNF-148…RNF-152 · HU-1 · HU-2

## Contexto y objetivo

El campo de entrada del chat (RF-33 de la spec 006, spec 033) reserva a la izquierda
un hueco tras el botón de adjuntar. Ese hueco muestra hoy un indicador circular
ondead (`CircularWavyProgressIndicator()`) sin estado, decorativo e imposible de
leer. El usuario no tiene ninguna señal de cuánto de la ventana de contexto de la
conversación está ocupado, — el fallo típico de los modelos locales: el historial
crece hasta que el modelo empieza a truncarlo en silencio.

Objetivo: convertir ese indicador en una lectura honesta de la ocupación del
contexto, sin añadir texto a la interfaz y sin tocar el resto del campo.

## Usuarios

El dueño de la nota: persona que usa el chat de IA de Secure Notes con su modelo
local (on-device) u Ollama.

## Historias de usuario

- **HU-1.** Como usuario, quiero ver de un vistazo cuánto queda de la ventana de
  contexto de la conversación actual, para saber cuándo conviene empezar un chat
  nuevo antes de que el modelo pierda el hilo.
- **HU-2.** Como usuario, quiero que el indicador me avise cuando estoy a punto de
  llenar el contexto, para no gastar una respuesta larga en un historial ya
  desbordado.

## Definiciones

- **Ventana de contexto**: el presupuesto de tokens de la conversación servida por
  el modelo activo. On-device es el presupuesto fijo del motor (8192); en Ollama es
  el `context_length` reportado por `/api/show` para ese modelo (el mismo dato que
  muestra el panel de modelos).
- **Uso de contexto**: tokens que ocupa la conversación en esa ventana, incluyendo
  el prompt de sistema y las memorias inyectadas.
- **Valor exacto**: reportado por el servidor Ollama (trozo `done` de la respuesta).
- **Valor estimado**: derivado del texto (≈4 caracteres por token), para on-device
  y para todo lo anterior a la primera respuesta.

## Requisitos funcionales

- **RF-248**: MIENTRAS hay una conversación abierta en el chat, EL SISTEMA muestra en
  el pie del campo de entrada un indicador circular determinado que pinta la
  fracción de ventana de contexto ocupada por la conversación actual.
- **RF-249**: EL SISTEMA calcula la fracción como `tokens usados / ventana del modelo
  activo`, acotada a [0, 1].
- **RF-250**: SI el backend es Ollama y la respuesta acaba en trozo `done` con
  `prompt_eval_count` y `eval_count`, ENTONCES EL SISTEMA toma el uso de contexto
  como `prompt_eval_count + eval_count` y lo marca como exacto.
- **RF-251**: SI no hay exacto disponible (backend on-device, o aún no ha terminado
  ninguna respuesta), ENTONCES EL SISTEMA estima el uso del contexto completo —prompt
  de sistema, memorias inyectadas e histórico de la conversación— con un factor
  aproximado de 4 caracteres por token, y lo marca como estimado.
- **RF-252**: MIENTRAS se genera una respuesta, EL SISTEMA mantiene el indicador en la
  fracción previa al envío; el valor se actualiza al terminar la respuesta.
- **RF-253**: EL SISTEMA usa como ventana de contexto: backend on-device → 8192 tokens
  fijos; Ollama → el `context_length` cache por modelo (dato de `/api/show` ya usado
  en el panel de modelos); SI no existe ese dato para el modelo, ENTONCES el
  indicador no se muestra en absoluto.
- **RF-254**: CUANDO la fracción supera el 85 %, EL SISTEMA pinta el indicador en el
  color de aviso/error del tema, sin cambiar su geometría ni su comportamiento.
- **RF-255**: EL SISTEMA no muestra ningún texto numérico junto al indicador en la
  interfaz; el indicador declara su porcentaje por `contentDescription`, marcando si
  el valor es aproximado.
- **RF-256**: CUANDO se abre una sesión histórica, EL SISTEMA recalcula el indicador
  como estimado; CUANDO se cambia de modelo, EL SISTEMA recalcula la fracción con la
  ventana del nuevo modelo.
- **RF-257**: CUANDO la conversación está vacía, EL SISTEMA pinta el indicador en 0 %
  (anillo vacío), no lo oculta.

## Requisitos no funcionales

- **RNF-148**: La estimación, el cálculo de la fracción y el umbral del aviso viven
  en funciones puras sin dependencias de Android, cubiertas por tests JVM puros.
- **RNF-149**: Todo texto visible al usuario (el `contentDescription` del indicador)
  se resuelve desde `strings.xml` en los 9 locales; sin literales en el código.
- **RNF-150**: Sin dependencias nuevas; el campo no incorpora texto ni cambia su
  altura ni el anclaje del botón de enviar a la derecha (RF-225), y no desborda a
  320 dp.
- **RNF-151**: El indicador no altera el estado ni la semántica del botón de enviar
  ni del botón de adjuntar.
- **RNF-152**: La estimación es determinista y barata: se recalcula solo al cambiar la
  conversación o el modelo, nunca por frame.

## Casos límite

- Ventana desconocida para el modelo (Ollama sin `/api/show`, o modelo no listado):
  no se pinta el indicador (RF-253).
- Uso estimado mayor que la ventana (histórico truncado en cliente): la fracción
  acota a 1.0 (RF-249); el aviso del 85 % salta igualmente (RF-254).
- Respuesta que termina en error o se cancela: se conserva el último valor conocido
  (anterior al envío), sin marcarse exacto.
- Sesión restaurada del historial: valor estimado (RF-256).
- Modelo sin respuesta todavía (conversación nueva): estimación del prompt de sistema
  y memorias, que puede ser > 0 % (RF-251/RF-257).

## Fuera de alcance

- Persistir el uso exacto en base de datos (al reabrir se estima; RF-256).
- Exigir `num_ctx` explícito a Ollama o cambiar el contexto del motor on-device.
- Medición exacta on-device (el motor no expone el recuento de tokens).
- Hoja/tooltip con el detalle "N de M tokens"; barra textual; botón para compactar o
  truncar la conversación.
- Telemetría o registro del uso fuera del dispositivo.

## Criterios de finalización

`./gradlew :app:compileDebugKotlin` y los tests JVM puros en verde; el indicador se
muestra en ambos backends con su umbral de aviso; el `contentDescription` resuelto en
9 locales; el layout no desborda a 320 dp; verificación manual en dispositivo
(`assembleDebug`, conversación larga hasta el aviso); `MEMORY.md` actualizado.

## Dudas abiertas

Ninguna: numerador híbrido (exacto Ollama / chars÷4), denominador 8192 on-device +
`context_length` de `/api/show`, anillo solo con aviso al 85 %, y sin persistir.
