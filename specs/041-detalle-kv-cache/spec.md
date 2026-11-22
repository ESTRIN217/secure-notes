# Spec 041 — Desglose de ocupación de caché del contexto

Fecha: 2026-11-17
Estado: **implementada y verificada en dispositivo** (2026-11-22)
RF-293…RF-299 · RNF-179…RNF-185 · HU-1 · HU-2 · HU-3

Spec de referencia: la Web UI del servidor llama.cpp (http://0.0.0.0:9931, abierta en el
navegador del teléfono; captura adjunta del 2026-11-17). Muestra, para la conversación:
`Context · 40 / 2.05K`, `2% used` / `2.01K remaining`, sección **Token usage details** con
**ACROSS ALL TURNS** (`Prompt tokens evaluated 1 tok` + sub-línea `29 reused from KV cache`;
`Tokens generated 10 tok`), **THIS TURN • KV CACHE** (`Prompt 30 tok` + sub-línea
`1 fresh + 29 cached`; `Generated 10 tok`), `KV cache total 40 tok` y `Avg speed 4.7t/s`.

## Contexto y objetivo

La tarjeta de detalle actual (`OutlinedCard` 200×245 dp en la pantalla de chat) ya muestra:
«Ventana de contexto» con usado/ventana y `LinearWavyProgressIndicator`, `% usado` + restante,
sección **Token usage details** con **ACROSS ALL TURNS** (prompt evaluado y tokens generados
acumulados de la conversación), **THIS TURN • KV CACHE** (Prompt del turno de usuario y
Generated del asistente), **KV cache total** y **Avg speed** (media ponderada).

Le faltan las dos sub-líneas de caché de la Web UI, y las cifras no existen en el binding ni
se persisten: hoy la tarjeta pinta dos placeholders hardcodeados en inglés («29 reused from
KV cache»), que son datos inventados y además violan la regla 6 de la constitución (textos
visibles solo desde `strings.xml`). Esta spec los sustituye por datos reales.

Objetivo: que cada turno on-device recuerde cuántos tokens reutilizó del KV cache al entrar
(cached) y cuántos decodificó (fresh), que la tarjeta muestre las dos sub-líneas de la
referencia con esos datos, y que donde el dato no exista (Ollama, histórico anterior a esta
spec, turno cancelado/error) la sub-línea simplemente no aparezca — nunca un número inventado.

La spec 037 define la tarjeta y sus filas; la spec 039 define las métricas reales del motor
y la regla «dato ausente ⇒ elemento omitido». Esta spec enmienda RF-267 de la spec 037
(sub-línea acumulada, valor intacto) y RF-268 (valor de la fila «Prompt» del turno y su
sub-línea), como fija RF-298, y convive con ellas en lo demás.

## Usuarios

Quien usa Secure Notes con el chat de IA, en particular con backend on-device, y quiere ver
en el chat el mismo desglose de ocupación de contexto que muestra la Web UI de llama.cpp:
cuántos tokens se decodificaron en este turno, cuántos salieron del KV cache reutilizado y
cuántos lleva reutilizados en total la conversación.

## Historias de usuario

- **HU-1.** Como usuario de un modelo on-device, quiero ver bajo la fila «Prompt» del turno
  actual cuántos tokens se decodificaron y cuántos salieron del KV cache («1 fresh + 29 cached»),
  para entender cuánto trabajo está haciendo realmente el motor.
- **HU-2.** Como usuario, quiero ver bajo «Prompt tokens evaluated» cuántos tokens reutilizados
  del KV cache acumula la conversación («29 reused from KV cache»), para valorar cuánto se
  está aprovechando la caché entre turnos.
- **HU-3.** Como usuario de Ollama o con un histórico anterior a esta versión, quiero que las
  sub-líneas simplemente no aparezcan en vez de ver cifras inventadas.

## Definiciones

- **fresh**: tokens decodificados en este turno = los «prompt tokens» reportados por el motor
  (el mismo contador que persiste la spec 039 para el turno de usuario; `user_tokens.size()`
  en el binding). No incluye nada residente en el KV.
- **cached**: tokens ya residentes en el KV cache **al entrar** el turno = `current_position`
  en el punto de entrada de la evaluación del turno, ANTES de decodificar nada (system prompt
  + turnos anteriores). La conversación nunca se re-decodifica, así que el KV residente al
  entrar es exactamente lo que la Web UI reporta como «reused from KV cache».
- **entry position**: snapshot de la posición del KV al entrar el turno. Si el motor no la
  conoce, el valor es −1 (o el turno no la persiste) y las sub-líneas no se pintan.
- **Coherencia del turno**: cuando fresh, cached y generated provienen del mismo turno del
  motor, `usedTokens` (ocupación actual del KV) = cached + fresh + generated
  (p.ej. 29+1+10 = 40, exacto como en la captura). El detalle **no impone ni valida** esta
  coherencia: pinta las cifras medidas; la igualdad es consecuencia de tomar el snapshot al
  entrar y de las métricas del turno (spec 039), no un cálculo de la tarjeta.
- **Fila «Prompt» del turno (THIS TURN)**: la fila muestra **cached + fresh** — la posición
  del KV al ENTRAR el turno más lo decodificado, es decir el nkv en el momento de enviarse
  el mensaje — exactamente como en la Web UI de la referencia. Se deriva sin exports nuevos
  (entry position + CSV del motor) y la composición se hace en la pieza pura, no en el
  Composable. Si el último turno con métricas no trae cached (histórico anterior a esta
  spec u Ollama), la fila pinta **solo fresh**, como hasta hoy — degradación silenciosa, no
  un hueco ni un cero presunto.
- **Fila «Prompt tokens evaluated» (ACROSS ALL TURNS)**: se mantiene **Σ fresh** (RF-267,
  sin cambio de valor); el doble conteo vive **únicamente** en la sub-línea «reused from KV
  cache».
- **Acumulado ACROSS ALL TURNS de caché («reused»)**: Σ cached de los turnos del histórico,
  con **doble conteo consciente** (un token reutilizado cuenta una vez por turno que lo
  reutiliza). Es un contador acumulado, **puede superar la ventana** de contexto y **no** es
  una fracción ni se usa para el anillo ni para `% usado`. Documentado tal cual: «reused» =
  número de tokens servidos desde caché acumulados en la conversación. Este doble conteo
  vive solo en la sub-línea «reused from KV cache»; la fila «Prompt tokens evaluated» suma
  solamente fresh.
- **Turno sin dato**: turno sin cached persistido — turno heredado de antes de esta spec,
  turno de error, turno cancelado/sin CSV finalizado, o turno de un motor que no lo reporta.
  Contribuye 0 al acumulado y **no** cuenta como «dato» para la sub-línea acumulada.
- **Sub-línea de caché**: línea auxiliar bajo la fila de prompt (tipografía y color ya usados
  en la tarjeta para el texto secundario), solo presente si hay dato.
- **Histórico pre-041**: turnos anteriores a esta spec llevan `promptCachedTokens` en vacío
  (NULL) para siempre; la acumulación los ignora como dato, no como cero de hecho.

## Requisitos funcionales

- **RF-293**: EL SISTEMA añade al binding nativo (vendorizado en `lib/`, fuente de verdad) un
  nuevo dato: la **entry position** del turno — snapshot de la posición actual del KV cache
  **al entrar** en la evaluación del turno, antes de decodificar nada. Se expone a la capa
  Kotlin del motor como un entero (`lastPromptEntryPosition()`, análogo a `nkvPosition()` de
  la spec 039), con valor −1 cuando no hay modelo cargado o la posición no está disponible,
  y se reinicia por petición siguiendo el mismo patrón que los contadores de la spec 039
  (reset en `reset_short_term_states()`). El cambio del binding es aditivo: ninguna firma
  existente cambia. El motor externo no se vendoriza (decisión firme vigente); si el host
  necesita un espejo local de la modificación para compilar, se mantiene como espejo, nunca
  incorporado al repo.
- **RF-294**: CUANDO el backend es on-device y una respuesta termina con normalidad, EL SISTEMA
  toma del motor la entry position del turno y la propaga hacia la capa de la app junto con
  las métricas del turno. CUANDO el backend es Ollama (o el motor no la reporta), el valor
  queda **ausente** (null), jamás un cero presunto.
- **RF-295**: EL SISTEMA persiste la entry position del turno de usuario en la tabla de
  conversaciones como columna nueva, nullable (`promptCachedTokens`), mediante una migración
  Room **aditiva y registrada**: `MIGRATION_10_11` ejecuta `ALTER TABLE conversations ADD
  COLUMN promptCachedTokens INTEGER`, con bump de la versión de la base de datos de 10 a 11
  y registro en `.addMigrations(...)` — el mismo patrón real con el que la spec 036 añadió
  las métricas de turno (`MIGRATION_9_10`). **Nunca** es destructiva (columna nullable) ni
  depende de `fallbackToDestructiveMigration` como mecanismo. Los turnos históricos y los
  turnos sin dato quedan con la columna en NULL; el valor se escribe en la misma escritura
  que las métricas del turno de usuario (spec 036/039).
- **RF-296**: EL SISTEMA incorpora el cached del turno a las piezas puras ya existentes del
  detalle de contexto: las métricas por turno y sus totales ganan el campo de caché; el
  acumulado ACROSS ALL TURNS suma Σ cached de los turnos **que traen dato** (los turnos sin
  dato contribuyen 0 y no son dato; si ningún turno trae dato, el total de caché es null);
  el detalle por turno expone el cached del último turno que trae dato y el total acumulado;
  la construcción del detalle propaga ambos al detalle pintado y expone además el valor
  compuesto de la fila «Prompt» del turno (**cached + fresh** cuando ambos existen; **fresh**
  solo si cached falta) para que el Composable se limite a pintarlo (constitución, principio
  3). El doble conteo consciente del acumulado es el documentado en Definiciones.
- **RF-297**: CUANDO la tarjeta de detalle se pinta y el total de caché acumulado no es null,
  EL SISTEMA pinta bajo la fila «Prompt tokens evaluated» (sección ACROSS ALL TURNS) una
  sub-línea «%1$d reused from KV cache» con el total acumulado. CUANDO la tarjeta se pinta y
  el cached del último turno que trae dato y su fresh no son null, EL SISTEMA pinta bajo la
  fila «Prompt» (sección THIS TURN) una sub-línea «%1$d fresh + %2$d cached» con fresh y
  cached del turno. SI falta el dato de una sub-línea, esa sub-línea no se pinta —
  ocultamiento total, sin marcadores, sin ceros inventados, sin cifras «~» — siguiendo la
  regla de dato ausente de la spec 039.
- **RF-298**: EL SISTEMA enmienda **RF-267** de la spec 037 únicamente para añadirle la
  sub-línea condicional acumulada de RF-296/RF-297: el valor de la fila «Prompt tokens
  evaluated» (ACROSS ALL TURNS) **no cambia** — sigue siendo **Σ fresh**, y el doble
  conteo vive únicamente en la sub-línea «reused from KV cache». EL SISTEMA enmienda
  **RF-268** de la spec 037 (a) cambiando el valor de la fila «Prompt» del último turno:
  deja de mostrar solo fresh y muestra **cached + fresh** del último turno que trae ambos
  (el nkv al enviarse el mensaje, derivado sin exports nuevos: entry position + CSV del
  motor); si el último turno con métricas no trae cached —histórico anterior a esta spec
  u Ollama—, la fila pinta **solo fresh**, como hasta hoy; y (b) añadiéndole la sub-línea
  condicional de RF-297. La composición de la tarjeta y el resto de las filas (ACROSS ALL
  TURNS, Generated, KV cache total, Avg speed) no cambian, y ningún otro requisito de las
  specs 037 ni 039 cambia. La enmienda sigue el flujo de la spec 039 (RNF-173): los RFs se
  enmiendan vía la spec que los supersede y queda constancia en el Historial de cambios
  de esta spec.
- **RF-299**: EL SISTEMA elimina del código los placeholders hardcodeados en inglés
  («29 reused from KV cache»); las dos sub-líneas salen únicamente de `strings.xml` en los
  9 locales a la vez (identificadores `chat_context_*`, apóstrofos escapados como `\'`),
  coherente con la constitución (regla 6) y con el patrón de la tarjeta existente; ninguna
  cifra de caché se compone por concatenación en el código ni se estima por heurística
  (chars, etc.) en ninguna ruta.

## Requisitos no funcionales

- **RNF-179**: Las piezas puras que acumulan el caché por conversación y construyen el
  detalle viven sin `android`/`androidx` y llevan tests JVM. La validación de esta spec se
  ejecuta en linux/aarch64, donde Robolectric no tiene runtime nativo: los tests JVM puros
  corren, la suite con entorno de pruebas no; ningún test de esta spec exige Robolectric.
- **RNF-180**: Sin dependencias nuevas (constitución, principio 1). El patch del binding es
  **aditivo y selectivo**: globals (init −1), reset en `reset_short_term_states()`, un export
  JNI. Ninguna firma existente cambia; la interfaz Kotlin del motor gana un método, no pierde
  ni redefine nada.
- **RNF-181**: Ocultamiento total sin dato en ambas vías (Ollama y histórico/error/cancelado
  heredado): ningún número inventado, ningún marcador «~», ningún placeholder — consecuencia
  directa de RF-297/RF-299 y de la regla de la spec 039 (RF-280, RNF-171/172).
- **RNF-182**: Textos visibles solo desde `strings.xml` en los 9 locales a la vez; sin
  literales en `Text(`/`contentDescription`; símbolos y unidades desde recursos; AAPT2 exige
  `\'` para apóstrofos.
- **RNF-183**: La columna nueva se añade con la migración Room aditiva **registrada**
  (`MIGRATION_10_11`, 10→11, `ALTER TABLE conversations ADD COLUMN promptCachedTokens
  INTEGER`, nullable), nunca con migración destructiva ni apoyándose en
  `fallbackToDestructiveMigration` como mecanismo; el bump de versión y su registro en
  `.addMigrations(...)` son parte obligatoria del cambio — sin ellos, Room aborta la
  apertura por el hash de esquema. Los datos existentes (notas, métricas de turnos,
  conversaciones) no se tocan.
- **RNF-184**: `./gradlew :app:compileDebugKotlin` y los tests JVM puros nuevos (acumulado de
  caché y construcción del detalle) en verde son el gate de esta spec en este entorno; la
  verificación en dispositivo (fresh/cached/total vs la Web UI y vs el KV cache total,
  turno a turno) queda como
  **pendiente explícita**, igual que en otras specs hasta que haya dispositivo.
- **RNF-185**: `AGENTS.md` recibe una nota breve del patch 041 junto a la nota del patch 039
  (el binding vendorizado lleva ambos); `MEMORY.md` se actualiza al terminar; la enmienda a
  RF-267/RF-268 de 037 y la convivencia con 039 quedan anotadas en el Historial de cambios.

## Casos límite

- **Turno cancelado** (sin CSV de finalización, spec 039): no se persiste caché para ese
  turno (el snapshot de entrada existe en memoria, pero sin métricas de turno no hay fila de
  métricas dónde escribirlo) → las sub-líneas se ocultan, como en la regla de cancelación.
- **Turno de error**: sin métricas y sin caché, como siempre; el acumulado lo ignora; el
  último turno con dato (THIS TURN) sigue siendo el anterior sano completo.
- **Cambio de modelo / reload / motor sin cargar**: entry position −1 → el turno no persiste
  caché → sub-líneas omitidas; el anillo sigue rigiéndose por spec 039 (sin modelo, no se pinta).
- **Context shift por overflow** (`shift_context()`): el snapshot se toma al entrar en el
  turno, antes de cualquier shift de ese turno; la cifra es la posición de entrada exacta del
  turno y no se ve afectada por el shift que ocurra durante su decodificación. El histórico
  tampoco se ve afectado: cada turno persiste su propio cached.
- **Conversación sin turnos**: ACROSS ALL TURNS sin dato de caché → sub-línea acumulada
  omitida; THIS TURN sin último turno con dato → sub-línea de turno omitida; el resto de la
  tarjeta (usado/ventana, %, KV cache total) se pinta como hoy.
- **Ollama sin `/api/show`**: ni anillo ni tarjeta (como hoy, spec 037 RF-265); en cualquier
  caso Ollama no produce dato de caché → con tarjeta abierta, ambas sub-líneas se omiten.
- **Prompt que cabe sin decodificar nada** (fresh = 0): «0 fresh + C cached» se pinta con
  normalidad — fresh = 0 es un dato, no una ausencia.
- **cached = 0 persistido** (sesión vacía): dato válido; las sub-líneas se pintan
  («0 reused from KV cache» / «0 fresh + 0 cached»). Solo NULL omite.
- **Histórico pre-041**: `promptCachedTokens` NULL en todas las filas heredadas → ambas
  sub-líneas omitidas; con turnos mezclados (algunos nuevos con dato, otros heredados), el
  acumulado suma solo los turnos con dato.
- **Acumulado con doble conteo puede superar la ventana**: no se acota, no se pinta como %,
  no alimenta el anillo; «KV cache total» sigue siendo la medida actual del motor.
- **Dato de métricas nuevo siempre pareado**: un turno nuevo persiste fresh y cached juntos
  (misma escritura); un histórico inconsistente (p.ej. tras un downgrade) se pinta tal cual:
  se omite solo lo que falte de verdad.
- **fresh + cached + generated = usedTokens**: se cumple por construcción tras un turno
  normal del motor; la tarjeta no lo recalcula ni lo exige — en datos mixtos o tras un cambio
  de modelo no se garantiza, y no pasa nada: se pinta lo medido.

## Fuera de alcance

- Compactar, truncar o persistir el desglose (es un dato derivado del turno y de la
  conversación, siempre derivado en memoria al pintar).
- Cambiar la ventana de contexto (8192 del motor), el cálculo del anillo o los refresh
  fijos (specs 034/039).
- Portar el desglose de caché a Ollama (el servidor no reporta el KV cache reutilizado).
- Tocar el motor externo en `/root/llama.cpp` fuera del patch aditivo del binding; mover,
  vendorizar o «solo copiar lo necesario» del motor (decisión firme vigente).
- Reimplementar prefix-reuse re-enviando la conversación token a token (innecesario: el KV
  ya está residente al entrar el turno; de ahí sale el snapshot).
- Discovery/verificación de páginas web.
- Cambiar el valor de otras filas: «Prompt tokens evaluated» (ACROSS ALL TURNS) conserva
  Σ fresh y «Generated» conserva su valor (ninguna enmienda de valor para ellas); la única
  fila cuyo valor cambia es «Prompt» (THIS TURN), a cached + fresh (RF-298).
- Backfill de `promptCachedTokens` para históricos (imposible sin re-decodificar).

## Criterios de finalización

- Spec aprobada por el usuario (esta entrega queda en estado **borrador** hasta entonces).
- `plan.md` y `tasks.md` derivados en fases posteriores, con los tests primero (rojo) para
  las piezas puras.
- `./gradlew :app:compileDebugKotlin` OK.
- Tests JVM puros nuevos en verde: acumulado de caché (suma con doble conteo, turnos sin
  dato, caso todo-null, pareo fresh/cached del turno), construcción del detalle
  (propagación a `turnCachedTokens`/`totalCachedTokens`, y coherencia del total del turno
  con 29+1+10=40 como dato de prueba) y la composición de la fila «Prompt» del turno
  (**cached + fresh** con ambos datos; **fresh** solo cuando cached falta). En linux/aarch64
  la suite con entorno no corre; los puros sí.
- Placeholders hardcodeados eliminados del código; 9 locales sincronizados.
- Verificación en dispositivo con backend on-device (enviar mensaje y comprobar fresh/cached
  vs la Web UI y vs el KV total): **pendiente**, anotada explícitamente como en otras specs.
- `AGENTS.md` (nota del patch 041 junto a la del 039) y `MEMORY.md` actualizados; constancia
  en el Historial de cambios de 037/039 de la enmienda.

## Historial de cambios

- 2026-11-17 — Creación a petición del usuario (spec 041), a partir de la referencia de la
  Web UI de llama.cpp (captura 2026-11-17); los placeholders hardcodeados en inglés quedan
  eliminados.
- 2026-11-17 — Dudas resueltas por el usuario e incorporadas como texto normativo:
  DU-1 → migración Room aditiva **registrada** `MIGRATION_10_11` (10→11, `ALTER TABLE
  conversations ADD COLUMN promptCachedTokens INTEGER`, nunca destructiva, sin try/catch,
  no apoyada en `fallbackToDestructiveMigration`). DU-2 → THIS TURN, fila «Prompt» muestra
  **cached + fresh** (como la captura) y enmienda RF-268 también en valor; histórico sin
  cached conserva fresh. DU-3 → «Prompt tokens evaluated» (ACROSS ALL TURNS) = Σ fresh
  (RF-267 intacto salvo la sub-línea); el doble conteo vive SOLO en la sub-línea «reused
  from KV cache». Dudas abiertas cerradas; sin sección Dudas abiertas.
- 2026-11-17 — Enmienda de spec 037: RF-267 recibe solo la sub-línea acumulada condicional
  (valor Σ fresh intacto) y RF-268 recibe la sub-línea por turno y el nuevo valor de la
  fila «Prompt» (cached + fresh; fresh solo si cached falta). Convive con la spec 039 sin
  redefinir sus RFs (flujo de enmienda de RNF-173). La spec sigue en estado **borrador**
  hasta aprobación del usuario.
- 2026-11-17 — Spec aprobada por el usuario; se deriva plan.md.
