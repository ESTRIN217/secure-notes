# Plan 037 — Detalle de la ventana de contexto

Spec: `specs/037-detalle-contexto/spec.md` (aprobada 2026-11-15)
Depende de: spec 034 (anillo/`ContextUsage`), spec 036 (métricas por turno en `ConversationTurn`)

## Archivos y responsabilidades

| Archivo | Responsabilidad | RFs |
|---|---|---|
| `app/src/main/java/com/example/ui/chat/ContextDetail.kt` (**nuevo**, puro) | `formatTokensCompact(n)` (k/m, ≤2 decimales), `formatPercentUsed(f)`, `TurnMetrics`, `TurnTotals`, `accumulateTurnTokens(turns)`, `ContextDetail`, `buildContextDetail(contextUsage, turns): ContextDetail?` | RF-265…RF-270, RF-272, RNF-160 |
| `app/src/test/java/com/example/ui/chat/ContextDetailTest.kt` (**nuevo**) | Tests JVM: formateos, acumulación con nulos, avg ponderado (sin voces → null), composición, `buildContextDetail(null,…)` → null | RNF-160 |
| `app/src/main/java/com/example/ui/chat/ContextDetailCard.kt` (**nuevo**, @Composable) | La `OutlinedCard` del diseño (200×190 dp, shape 28, border, scroll interno): título, barra `LinearWavyProgressIndicator` determinada, filas % usado / restante, divisor, «Token usage details», bloques ACROSS ALL TURNS / THIS TURN / KV total / Avg speed con «~» si `exact == false` | RF-266…RF-270, RNF-162…RNF-164 |
| `app/src/main/java/com/example/ui/FloatingChatInputField.kt` | **Sustituir** el parámetro `contextUsage: ContextUsage?` por `contextDetail: ContextDetail?` (una fuente); tap en el anillo → abre/cierra; **hoja `<600 dp`** (`ModalBottomSheet`) / **menú anclado `≥600 dp`** (patrón esfuerzo); estado `remember` (`showContextDetail`); cierra con tap fuera/atrás/back; el anillo no cambia de tamaño ni pinta estado extra | RF-265, RF-266, RF-271, RF-272 |
| `app/src/main/java/com/example/ui/AiChatScreen.kt` | Construir y memoizar el detalle: `remember(conversationHistory, contextUsage) { buildContextDetail(contextUsage, conversationHistory.map { turn -> TurnMetrics(...) }) }`; pasar solo `contextDetail` al campo; eliminar el `collectAsStateWithLifecycle()` del detalle si ya no pinta nada directo | RF-265, RF-272 |
| `app/src/main/res/values*/strings.xml` (9 locales) | Nuevas claves del card: título, % usado, restante, cabecera de detalles, encabezados de sección, etiquetas de fila, unidad «tok»; ≈13 claves × 9 locales | RNF-162 |

## Algoritmo

```
formatTokensCompact(n: Int, locale = Locale.US): String
  n < 1_000 → "$n"
  n < 1_000_000 → k con %.2f, recorte de ceros ("2.05k", "1.5k", "2k")
  else → m con %.2f, recorte de ceros

TurnMetrics(promptTokens: Int?, evalTokens: Int?, evalDurationMs: Long?)

accumulateTurnTokens(turns: List<TurnMetrics>): TurnTotals
  promptT = sum(promptTokens or 0)         // ignora nulos
  generated = sum(evalTokens or 0)
  avg = tokensPerSecond(sum(evalTokens or 0), sum(evalDurationMs or 0) * 1_000_000L)

buildContextDetail(usage: ContextUsage?, turns: List<TurnMetrics>): ContextDetail?
  usage == null → null                                     // RF-265
  totals = accumulateTurnTokens(turns)
  thisTurnPair = last(t in turns: promptTokens != null) to last(t in turns: evalTokens != null)
  ContextDetail(usedTokens=usage.usedTokens, windowTokens=usage.windowTokens, exact=usage.exact,
                totals=…, turnPrompt=…, turnGenerated=…)
```

- RF-271: `<600 dp` → `ModalBottomSheet` contenedor sólo con la card; `≥600 dp` → `DropdownMenu` anclado sobre la Box del anillo con la misma card (patrón `ReasoningEffortButton`).
- RNF-164: el detalle se calcula en `remember` (sólo cuando cambia la conversación o el `contextUsage`), nunca por frame ni durante streaming parcial.

## Interfaz

```kotlin
// ui/chat/ContextDetail.kt (pura)
data class TurnMetrics(val promptTokens: Int?, val generatedTokens: Int?, val evalDurationMs: Long?)
data class TurnTotals(val promptTokens: Int, val generatedTokens: Int, val avgTokensPerSecond: Float?)
data class ContextDetail(
    val usedTokens: Int, val windowTokens: Int, val exact: Boolean,
    val totalPromptTokens: Int, val totalGeneratedTokens: Int, val avgTokensPerSecond: Float?,
    val turnPromptTokens: Int?, val turnGeneratedTokens: Int?
)
fun formatTokensCompact(n: Int): String
fun accumulateTurnTokens(turns: List<TurnMetrics>): TurnTotals
fun buildContextDetail(usage: ContextUsage?, turns: List<TurnMetrics>): ContextDetail?
```

## Decisiones con su alternativa descartada

- **Una sola fuente (`ContextDetail`) sustituye a `ContextUsage`** en el campo: dos parámetros con
  estado solapado obligan dos cálculos y dos nulls; una única derivación derivada resuelve ambas.
- **Acumulado de filas y no de `usage` del KV**: el KV puede truncarse y compactar; el histórico
  acumulado por turno es lo que dice el título
   `ACROSS ALL TURNS`; cargos de turnos con métrica ausente se ignoran (no falsean con estimación).
- **Avg ponderado** (`Σeval/Σduración`) en vez de media de tok/s por turno: un turno corto no pesa
  igual que uno largo; la suite lo cubre.
- **Estimado marcado «~»** en vez de pintar cifras exactas falsas en on-device (regla del proyecto
  desde spec 034/036).
- **Card de 200×190 dp como el diseño + scroll interno** en vez de crecer la altura: mantiene el
  lenguaje visual acordado y evita que tape la zona de escritura.
- **Ahora los textos del card se localizan al inglés en los 9 ficheros** (mock iba en EN pero el
  proyecto exige strings en recursos): se acepta el coste de 27 claves extras.

## Estrategia de tests

`./gradlew :app:testDebugUnitTest --tests '*ContextDetail*'` — verde antes de cablear la UI.

Casos (rojos primero): `formatTokensCompact` (0, 42, 999, 1k, 1234, 2048, 1024, 1_500_000);
`accumulateTurnTokens` vacío (0 / 0 / null), todos nulos, suma mixta, `avgTokensPerSecond == null`
sin duración; `buildContextDetail(null,…)` → null; conversación vacía con usage fijo (filas a 0 tok,
nada omitido); par `THIS TURN` extraído del último aunque haya turnos de error entre medias.

`./gradlew :app:compileDebugKotlin` incluye `processDebugResources` (AAPT) para las nuevas claves.

## Cobertura de RFs

RF-265 · RF-266 (card) · RF-267 (acumulado) · RF-268 (this turn) · RF-269 (KV total + avg) · RF-270 (~ prefijo) · RF-271 (hoja/anclado) · RF-272 (una fuente) · RNF-160 (tests) · RNF-161 (estado en campo) · RNF-162 (strings) · RNF-163 (sin deps) · RNF-164 (remember)
