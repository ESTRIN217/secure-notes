# Spec 036 — Metadatos de mensaje

Fecha: 2026-11-15
Estado: **implementada y verificada en dispositivo** (2026-11-15)
RF-260…RF-264 · RNF-155…RNF-159 · HU-1 · HU-2

## Contexto y objetivo

La burbuja de mensaje pinta hoy, bajo el texto, una fila gris de metadatos: icono de estado, hora,
un **«• 30 tokens»** fijo, la **duración** solo en el asistente (si la hay), un **«• 9,20 tok/s»**
fijo y el modelo (asistente). Los dos textos fijos son prototipos hardcodeados que no representan
nada real — y es exactamente donde llama.cpp sí muestra datos útiles: bajo el mensaje del usuario
la métrica del **prompt** (`30 tokens · 3.3s · 9.20 tokens/s`) y bajo la respuesta del asistente la
de **generación** (`12 tokens · 2.8s · 4.32 t/s`).

Objetivo: que esa fila muestre métricas **reales del turno**, separadas por burbuja como en la
referencia, con exactitud de Ollama donde exista y con estimación honesta (marcada «~») donde no.

## Usuarios

Quien usa el chat de IA: alguien que quiere saber cuánto le costó cada turno (tokens procesados,
cuánto tardó, a qué velocidad) sin salir a ajustes o logs.

## Historias de usuario

- **HU-1.** Como usuario, quiero ver en la burbuja del asistente cuántos tokens generó y cuánto
  tardó, para valorar si el modelo rinde como espero (tokens/s).
- **HU-2.** Como usuario, quiero ver en mi propia burbuja los tokens del prompt que envié y su
  tiempo de evaluación, para entender por qué la respuesta tardó lo que tardó (el prompt largo es
  la causa típica) — y que esas métricas sobrevivan al reabrir la sesión.

## Definiciones

- **Métrica de prompt**: tokens procesados por el modelo al evaluar el mensaje de entrada,
  incluyendo sistema e histórico; lo reporta Ollama como `prompt_eval_count` + `prompt_eval_duration`.
- **Métrica de generación**: tokens producidos por el modelo, `eval_count` + `eval_duration` en Ollama.
- **tok/s**: `count / (duration / 1e9)` — tokens por segundo según la propia medida del servidor.
- **Estimado (on-device)**: el binding nativo no expone tokens ni tiempos por fase; se estiman
  con ~4 caracteres por token y `processingTimeMs`, marcados con el prefijo «~».
- **Turno**: un intercambio usuario → modelo. Los metadatos de cada burbuja describen su parte.

## Requisitos funcionales

- **RF-260**: EL SISTEMA muestra en la fila de metadatos de cada burbuja, cuando hay datos, sus
  métricas como texto gris pequeño con separadores `·`: tokens, duración y tok/s — la burbuja del
  usuario con la métrica de **prompt** y la del asistente con la de **generación**.
- **RF-261**: CUANDO el backend es Ollama, EL SISTEMA toma las métricas de los campos
  `prompt_eval_count`, `prompt_eval_duration`, `eval_count` y `eval_duration` del trozo `done`
  de la respuesta (los mismos que usa el anillo de contexto, spec 034), calcula
  `tok/s = count / (duration_ns / 1e9)` con un decimal, y formatea la duración con el formato
  existente (`seconds`). SI falta algún contador, la parte que falta no se muestra.
- **RF-262**: CUANDO el backend es on-device, EL SISTEMA estima con ~4 caracteres por token los
  tokens del texto de cada burbuja (usuario: su mensaje + adjuntos no expandidos; asistente: la
  respuesta), usa `processingTimeMs` como duración y deriva tok/s; las cifras se marcan con el
  prefijo «~» para distinguirlas de una medida real.
- **RF-263**: EL SISTEMA persiste por turno las cuatro métricas en la tabla `conversations`
  (columnas anulables `promptEvalCount`, `promptEvalDurationMs`, `evalCount`, `evalDurationMs`)
  mediante una migración aditiva (`ALTER TABLE ADD COLUMN`), conservando el histórico existente
  (versión 9 → 10). Los mensajes guardados antes de esta spec no muestran métricas, como hoy.
- **RF-264**: EL SISTEMA inserta el turno de usuario antes de la petición y actualiza ese turno con
  los números del prompt cuando la respuesta completa (rowId devuelto por el insert); el turno del
  asistente se inserta con sus números directamente.

## Requisitos no funcionales

- **RNF-155**: La migración es aditiva e idempotente (`ALTER TABLE ADD COLUMN` sobre columnas
  anulables); no puede perder el histórico ni exigir exportar/reimportar.
- **RNF-156**: La derivación (`tok/s`), la estimación (chars÷4), el clamp de duración y el marcado
  «~» viven en piezas puras sin dependencias de Android, con tests JVM puros.
- **RNF-157**: El texto nuevo (unidad «tokens»/«tok/s» si procede) llega por `strings.xml` en los 9
  locales; cualquier símbolo usado se rinde desde recursos, sin literales en el código.
- **RNF-158**: Sin dependencias nuevas; sin cambios en el binding nativo de llama.cpp.
- **RNF-159**: La fila de metadatos no desborda: tokens/duración/tok-s con `maxLines` truncado como
  el resto de la fila, y contenido oculto (no huecos) cuando un dato falta.

## Casos límite

- Backend Ollama con respuesta que termina en error o cancelación: el turno de error **no** se
  actualiza con métricas (conserva su contenido vacío; no pinta huecos).
- Ollama sin `done` completo (stream interrumpido o herramienta `TOOL_CALLS` con segunda
  respuesta): se usan los datos del último trozo `done` recibido; si la segunda respuesta trae
  otros, **prevalecen los de la segunda** (lo último que realmente evaluó el modelo).
- Sesión restaurada: las métricas persistidas se pintan; los turnos sin dato (anteriores a la
  spec) pintan solo hora (y duración/modelo donde aplicaba).
- Turno de usuario enviado con adjuntos grandes: la medición del prompt la da Ollama (con
  adjuntos incluidos); en on-device la estimación podría infraestimar el prompt; se acepta y se
  marca «~».
- `eval_duration = 0`, `count` negativo u ofuscados: sin división ni sesión ambigua — no se
  pinta tok/s.
- Mensajes legacy sin `processingTimeMs`: no pinta duración, como antes.

## Fuera de alcance

- Modificar el esquema con operaciones destructivas (tablas recreadas, drops).
- Exponer tokens on-device exactos (requeriría tocar el binding nativo; fuera de este repo).
- Métricas por turno en el export de la conversación.
- Separador/nuevo tratamiento visual más allá de la fila ya existente.
- Cambiar la semántica del anillo de contexto (spec 034).

## Criterios de finalización

Spec implementada según tasks; `./gradlew :app:compileDebugKotlin`, tests JVM puros y suite
dirigida en verde; migración 9→10 verificada en dispositivo (abrir chat histórico anterior al
cambio: los mensajes viejos siguen ahí y los nuevos llevan métricas en ambas burbujas);
verificación de split prompt/generación en backend Ollama; `MEMORY.md` actualizado.

## Historial de cambios

- 2026-11-15 — Creación a partir de la referencia de llama.cpp Android (hoja de métricas bajo
  burbuja de usuario/asistente) y de los placeholders fijos introducidos en la burbuja durante
  la iteración del chat.
