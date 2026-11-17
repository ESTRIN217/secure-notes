# Spec 037 — Detalle de la ventana de contexto

Fecha: 2026-11-15
Estado: **implementada y verificada en dispositivo** (2026-11-15)
RF-265…RF-272 · RNF-160…RNF-164 · HU-1 · HU-2

## Contexto y objetivo

El anillo de contexto del campo de entrada (spec 034) se limita a pintar una fracción con color.
El usuario quiere, al tocarlo, una tarjeta de detalle al estilo de la referencia de llama.cpp:
ocupación actual del KV cache de la conversación (cuánto se llena, cuánto queda), acumulado por
turnos, el turno actual, el total del cache y la velocidad media de generación.

Objetivo: que el tap en el anillo despliegue ese desglose sin salir del chat, sin tocar la semántica
ya cerrada del anillo (spec 034, RF-248…RF-257) ni la persistencia por turno (spec 036).

## Usuarios

Quien usa el chat de IA: interés en saber cuánto del contexto de la conversación está ocupado,
cuánto llevan los turnos (prompt/generación) y qué velocidad mantiene el modelo — sin abrir ajustes
ni herramientas externas.

## Historias de usuario

- **HU-1.** Como usuario, quiero tocar el anillo y ver de cuántos tokens dispone la conversación
  y cuántos lleva ocupados, para decidir si sigo en este chat o empiezo uno.
- **HU-2.** Como usuario, quiero ver el desglose de dónde se fueron mis tokens (evaluados vs
  generados, por turno y en total) y la velocidad, para entender el comportamiento del modelo.
- **HU-3.** Como usuario, quiero que la tarjeta cierre con un tap fuera o con atrás, y que no
  tape la zona de escritura más allá de lo necesario.

## Definiciones

- **Detalle de contexto** (`ContextDetail`): foto derivada y pura de la conversación abierta
  en el momento de pintar el anillo: usado/ventana + acumulado de turnos + último turno.
- **ACROSS ALL TURNS** (encabezado localizado): suma acumulada de los tokens del histórico
  de la conversación (prompt evaluado + generación), no del cache.
- **THIS TURN • KV CACHE**: el último par completo de la conversación (turno de usuario →
  turno de asistente): prompt del usuario y generado del asistente.
- **KV cache total**: la ocupación real del KV cache de la conversación actual (la misma cifra
  que usa el anillo de spec 034).
- **Avg speed**: `Σ eval_count / Σ eval_duration` de los turnos del asistente de la conversación;
  es una media ponderada, no una media de tok/s por turno.

## Requisitos funcionales

- **RF-265**: CUANDO `contextDetail` ≠ null (lo mismo que el indicador visible) y el usuario toca
  el anillo, EL SISTEMA abre la tarjeta de detalle; CUANDO `contextDetail` == null (spec 034,
  RF-253), el anillo no se pinta ni reacciona al toque.
- **RF-266**: La tarjeta es un `OutlinedCard` (200×190 dp, esquinas 28, borde) que muestra: «Ventana
  de contexto» con `usado / ventana` en compacto (`42 / 2.05k`), una `LinearWavyProgressIndicator()`
  determinada con la misma fracción del anillo, `% usado` y restante en compacto (`2.01k restante`),
  divisor y la sección **Token usage details**.
- **RF-267**: La sección «ACROSS ALL TURNS» muestra la suma acumulada del histórico de la
  conversación: tokens evaluados (suma de `promptEvalCount`) y tokens generados (suma de `evalCount`).
  〈enmendado — spec 041: sub-línea «%1$d reused from KV cache» condicional bajo «tokens evaluados» (solo si hay Σ cached)〉
- **RF-268**: La sección «THIS TURN • KV CACHE» muestra, del último par completo, `Prompt` = tokens
  del turno de usuario y `Generated` = tokens del turno de asistente.
  〈enmendado — spec 041: `Prompt` además valor cached+fresh cuando el turno trae ambos; sub-línea «%1$d fresh + %2$d cached»〉
- **RF-269**: EL SISTEMA muestra además `KV cache total` = `usedTokens` del detalle, y `Avg speed`
  = `Σ eval_count / Σ eval_duration` (tok/s, un decimal); la fila se omite si ningún turno del
  asistente aporta duración/conteo.
- **RF-270** *(enmendado — spec 039, RF-280 supersede)*:
  CUANDO el detalle es exacto (en ambos backends, por medición real), EL SISTEMA pinta
  las cifras sin ningún marcador. Sin prefijos «~» quedan.
  ~~EL SISTEMA prefija con «~» los números visibles como métricas de tokens y velocidad.~~
- **RF-271**: La tarjeta se abre como hoja inferior en ancho compacto (<600 dp) y como menú
  anclado junto al indicador en ancho amplio (≥600 dp), siguiendo los patrones ya usados en el
  campo (botón de esfuerzo); toque fuera o atrás la cierra.
- **RF-272**: El `contextUsage` actual del campo se sustituye por `contextDetail` (una sola fuente):
  el anillo determina su fracción/color/ocultamiento leyendo del mismo detalle; al reemplazar, el
  comportamiento del anillo (RF-248…RF-257) no cambia salvo los tipos internos de la API.

## Requisitos no funcionales

- **RNF-160**: Agregación, extracción del último par, formatos `Compacto / k / m`, `% usado`,
  `restante`, `Avg speed` y composición del detalle viven en piezas puras sin `android` con tests JVM.
- **RNF-161**: La tarjeta no porta su propia lógica de decisión de estado (abre/cierra): solo pinta
  y lanza eventos; la visibilidad se decide en el campo como estado `remember` (UI transitorio).
- **RNF-162**: Ningún número visible se compone por concatenación en el código: textos por
  `strings.xml` en los 9 locales; símbolos y unidades desde recursos.
- **RNF-163**: Sin dependencias nuevas; el teclado no cambia el anclaje; la tarjeta usa el ancho de
  200 dp del diseño y despliega su contenido con scroll interno si no cabe; nada desborda en 320 dp.
- **RNF-164**: Durante la generación, la tarjeta (como el anillo) enseña el valor previo al envío;
  se actualiza al terminar la respuesta. No se calcula nada por frame ni por token.

## Casos límite

- Conversación abierta sin turnos todavía: ACROSS ALL TURNS en `0 tok` para ambos; THIS TURN sin par
  completo → filas con `0 tok`; KV cache total y % usado igual reflejan el sistema + memorias
  (spec 034, RF-257); Avg speed sin voces → fila omitida.
- Turno de usuario sin métricas (on-device siempre estima > 0; Ollama sin trozo `done`): la
  acumulación lo ignora (cuenta 0, no suma); la extracción de «estos 2 valores» toma el último no nulo.
- Chat restaurado de histórico anterior a la spec 036: acumulados en 0 (no hay columnas); sus
  filas muestran `0 tok`; KV cache total y % usado se pintan igual (estimación spec 034 vigente).
- Ventana desconocida (Ollama sin `/api/show`): detalle `null` → sin anillo ni tarjeta.
- Errores a mitad: el acumulado ignora turnos de error (no tienen contadores); el último par
  completo sigue siendo el turno sanado anterior.
- `eval_duration` = 0 o null en el acumulado: Avg speed no se pinta (sin división).

## Fuera de alcance

- Cambiar la ventana de contexto, compactar ni truncar la conversación.
- Métricas de histórico de **otras** sesiones; el total es solo de la conversación abierta.
- Persistir el detalle (es derivado, siempre en memoria; KV cache incluido).
- Cambiar iconos o la tipografía del campo; la tarjeta se ciñe a Material 3 del tema.
- Marcar «~» al porcentaje/fracción (usan la misma base del anillo); «~» aplica solo a las métricas de tokens y velocidad.

## Criterios de finalización

`./gradlew :app:compileDebugKotlin` y los tests JVM puros del detalle en verde; con backend Ollama
y conversación de varios turnos: abrir la tarjeta y comprobar acumulados vs el histórico,
% usado restante y KV total coherentes; con backend on-device: «~» en todas las cifras; sin dato
de ventana (Ollama sin `/api/show`): sin anillo ni tarjeta; verificación en dispositivo;
`MEMORY.md` actualizado.

## Historial de cambios
- 2026-11-17 — RF-267/RF-268 enmendados por spec 041 (sub-línea «reused from KV cache» condicional; RF-268 además valor cached+fresh). Ver specs/041-detalle-kv-cache/
- 2026-11-16 — RF-270 enmendado (spec 039): sin marcadores «~»; cifras exactas en ambos backends.

- 2026-11-15 — Creación a partir del diseño adjunto (OutlinedCard con detalle de contexto).
