# Spec 035 — Carga de modelo en el chat

Fecha: 2026-11-15
Estado: **implementada y verificada en dispositivo** (2026-11-15)
RF-258 · RF-259 · RNF-153 · HU-1 · HU-2

## Contexto y objetivo

El chat de IA usa hoy un banner rojo de «modelo no disponible» (`ai_model_not_available`) cuando
el modelo on-device no está listo. Ese banner también aparece mientras el modelo **se está cargando**
(`ModelState.LOADING`), porque durante la carga `isAvailable()` es `false` y, en una conversación
vaciada, es lo único que la pantalla enseña: el usuario ve un aviso de error engañoso cuando en
realidad la app está haciendo lo correcto (cargar pesos del GGUF).

Objetivo: mostrar una señal honesta y discreta durante la carga —una barra lineal indeterminada
encima del contenido— y reservar el banner de error para los estados en los que de verdad no hay
modelo disponible (`NOT_LOADED`, `ERROR`).

## Usuarios

El dueño de la nota: persona que pulsa generar/enviar con un modelo on-device que aún no está en
memoria, o que abre el chat tras abrir la app.

## Historias de usuario

- **HU-1.** Como usuario, quiero ver que el modelo se está cargando (no que «falló»), para no
  pensar que tengo que hacer algo cuando solo hay que esperar unos segundos.
- **HU-2.** Como usuario, quiero que el error vuelva solo cuando de verdad no hay modelo listo,
  para no desactivar sin querer el modelo cargado o reintentar sin motivo.

## Definiciones

- **Carga (LOADING)**: estado del motor on-device mientras los pesos del GGUF se leen y quedan
  listos para inferir. No aplica a Ollama (el servidor no expone un estado local de carga) ni a
  la descarga del modelo (tiene su propia UI en ajustes/hojas).

## Requisitos funcionales

- **RF-258**: CUANDO el chat está abierto con backend on-device y `onDeviceModelState` es
  `LOADING`, EL SISTEMA muestra encima del contenido un `LinearWavyProgressIndicator()`
  indeterminado, ancho completo y sin texto, justo debajo de la barra superior (en el mismo lugar
  del antiguo indicador suelto).
- **RF-259**: SI `onDeviceModelState` es `LOADING` (con backend on-device), ENTONCES EL SISTEMA
  no pinta el banner `ai_model_not_available`, aunque la conversación esté vacía; el banner queda
  reservado a `NOT_LOADED` y `ERROR`, o a backend Ollama sin conexión.

## Requisitos no funcionales

- **RNF-153**: La decisión «mostrar la barra» y «suprimir el banner» vive en piezas puras, sin
  dependencias de Android, cubiertas por tests JVM puros.
- **RNF-154**: Sin dependencias nuevas; sin strings visibles nuevas (la barra no lleva texto);
  el comportamiento durante `READY`, `NOT_LOADED`, `ERROR` y en Ollama no cambia fuera de lo
  declarado.

## Casos límite

- Backend Ollama + motor on-device cargando en segundo plano: **no** se muestra la barra (la
  inferencia no pasa por él).
- Backend on-device + usuario pulsa enviar con conversación vacía y modelo cargándose: se ve la
  barra; el envío sigue el comportamiento actual (rechaza hasta READY con su mensaje existente).
- Transición LOADING → READY: la barra desaparece sola (estado observable); no hace falta estado
  adicional en la pantalla.
- LOADING → ERROR: desaparece la barra y vuelve el banner de error.
- Descarga del modelo (`downloadingModelId != null`): fuera de alcance; mantiene su UI actual.

## Fuera de alcance

- Barra durante la descarga del modelo (visible en Ajustes/panel de modelos).
- Indicador de carga en Ollama (no expone estado local de carga).
- Texto/label o botón de acción junto a la barra.
- Persistencia o registro del tiempo de carga más allá de lo que ya existe.

## Criterios de finalización

Spec aprobada; `./gradlew :app:compileDebugKotlin` y los tests JVM puros de las piezas nuevas en
verde; verificación en dispositivo (abrir chat con on-device sin cargar → enviar → ver la barra
mientras carga y la desaparición al terminar; sin banner rojo durante la carga); `MEMORY.md`
actualizado.

## Historial de cambios

- 2026-11-15 — Creación. Reutiliza los estados existentes (`onDeviceModelState`, visible en la
  pantalla desde la spec 033); sustituye al indicador suelto no condicionado introducido como stub.
