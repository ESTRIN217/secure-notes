# Plan — Spec 020: el registro de fallos

## 1. Archivos y responsabilidades

| Fichero | Paquete | Responsabilidad | Android? |
|---|---|---|---|
| `CrashReportText.kt` | `com.example.crash` | **Puro**: formatea el informe y recorta la traza | no |
| `CrashIssueUrl.kt` | `com.example.crash` | **Puro**: título, cuerpo y URL de la incidencia de GitHub | no |
| `CrashStore.kt` | `com.example.crash` | **Puro**: escribir, leer, consumir y responder si hay informe pendiente. Solo `java.io.File` | no |
| `CrashReporter.kt` | `com.example.crash` | Une el fallo de la app con el almacén: instala el manejador, delega, avisa del arranque | sí |
| `CrashNotifier.kt` | `com.example.crash` | Canal, notificación, `PendingIntent` y `cancelar` | sí |
| `CrashReportViewModel.kt` | `com.example.ui.viewmodel` | Carga el informe en `Dispatchers.IO` y lo expone en un `StateFlow` | sí |
| `CrashReportScreen.kt` | `com.example.ui.crash` | La pantalla: texto seleccionable, tres acciones, aviso y vacío | Compose |
| `CrashReportActivity.kt` | `com.example` | **Activity** propia: idioma, tema y `FLAG_SECURE`, como `CodeEditorActivity` | sí |
| `CrashInvariantsTest.kt` | `app/src/test/.../com/example` | Invariantes que leen el texto de los ficheros | no |
| `CrashReportTextTest.kt`, `CrashIssueUrlTest.kt`, `CrashStoreTest.kt` | ídem | Pruebas puras | no |

**Tocados de paso, y solo lo imprescindible**: `SecureNotesApplication` (dos llamadas), `MainActivity`
(el aviso), `AndroidManifest.xml` (una `activity`), los nueve `strings.xml`. Nada más.

## 2. Funciones puras

### `CrashReportText` (RF-118, RF-118-recorte, caso límite 5)

```kotlin
data class DatosInforme(
    val versionApp: String, val versionAndroid: String, val dispositivo: String,
    val tipoBuild: String, val hilo: String, val marcaTiempo: Long, val traza: String,
)

data class TrazaRecortada(val texto: String, val recortada: Boolean)

object CrashReportText {
    const val MAX_LINEAS_TRAZA = 200
    fun recortarTraza(traza: String, maximo: Int = MAX_LINEAS_TRAZA): TrazaRecortada
    fun construir(datos: DatosInforme): String
}
```

`construir` no recibe el `Throwable`: recibe la traza ya stringifyada. Motivo — el `Throwable` se convierte a
texto **en el hilo que se está muriendo**, y construirlo aquí haría que el formateo quedara en el camino
crítico de la muerte. Además `DatosInforme` no necesita `Throwable` para nada y así el test no necesita
excepciones reales salvo para la traza.

El **recorte va antes** de construir: si la traza se recorta, el informe dice `[traza recortada: N de M líneas]`
en la última línea. Es lo que separa «el informe no trae ese tramo» de «el fallo no tenía eso» (caso límite 5).

### `CrashIssueUrl` (RF-121, caso límite 6)

```kotlin
object CrashIssueUrl {
    const val REPOSITORIO = "https://github.com/ESTRIN217/secure-notes"
    fun titulo(primeraLinea: String): String          // "[Crash] <tipo de excepción>" o "Crash en Secure Notes"
    fun cuerpo(texto: String): String                  // bloque cercado + nota de privacidad
    fun url(titulo: String, cuerpo: String, base: String = REPOSITORIO): String
    private fun codificar(valor: String): String       // URLEncoder + "UTF-8", y "+" → "%20"
}
```

`URLEncoder` es de `java.net`: **no es `android`**, y existe en el SDK de Android. El `+` → `%20` es porque
`URLEncoder` codifica el espacio como `+`, que es válido en el *form body* pero raro en una query y confunde al
que lee la URL.

### `CrashStore` (RF-117, RF-118, RF-122, RF-123, caso límite 1)

```kotlin
class CrashStore(private val fichero: File) {
    fun guardar(texto: String)                        // temporal + rename; atómico
    fun leer(): String?                                // null si no hay o si está vacío
    fun hayPendiente(): Boolean
    fun consumir(): Boolean                            // borrar y decir si había algo
}
```

**Solo `java.io.File`.** Por eso sus pruebas **sí corran en esta plataforma**: `TemporaryFolder` de JUnit 4 no
necesita Robolectric, y `renameTo` dentro de un directorio real se comporta como en el dispositivo. Es la
decisión que más pruebas gratis da de toda la spec.

`guardar` escribe en `fichero.tmp` y renombra. Si alguien lee **mientras** se escribe —imposible en la
práctica, porque el proceso muere, pero no cuesta nada— ve el informe **anterior** completo o nada, nunca medio.

Cada `guardar` **sobrescribe**: el informe es el último (caso límite 1).

## 3. El manejador de excepciones (RF-117, RNF-65)

```kotlin
object CrashReporter {
    fun instalar(context: Context) {
        if (yaInstalado) return                      // onCreate puede repetirse en un proceso de pruebas
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { hilo, fallo ->
            runCatching { registrar(context, hilo, fallo) }   // NUNCA revienta: el proceso va a morir
            anterior?.uncaughtException(hilo, fallo)          // SIEMPRE se delega
        }
        yaInstalado = true
    }
}
```

**Por qué el manejador anterior y no `Process.killProcess`:** delegar es lo que garantiza RF-117. Si el sistema
ya tenía puesto un manejador (Play Console, un test runner), sigue funcionando; y si no lo tenía, el
comportamiento por defecto de la plataforma —`RuntimeInit` mata el proceso— es exactamente el de hoy.

`registrar` **no** lanza nunca: cada paso va en su propio `runCatching`, porque un `OutOfMemoryError` registrando
el fallo tiene que poder delegar igual (caso límite 4).

## 4. Arranque (RF-119, RF-120)

`SecureNotesApplication.onCreate`, **en este orden y nada más**:

```kotlin
CrashReporter.instalar(this)          // 1º: antes de que nada más pueda caer
CrashNotifier.anunciar(this)          // 2º: lee el pendiente y publica si se puede
```

`anunciar` **nunca pide permiso**: la app hoy solo lo pide cuando hay una actualización disponible, y pedirlo en
cada arranque molesta. Si no hay permiso, `notify` lanza `SecurityException`, se captura y se registra, y el
canal que de verdad avisa es el aviso de dentro (RF-120, caso límite 3).

El aviso de dentro vive en `AppMainContent` (`MainActivity`), **después** del `if (isPasswordSet && !isUnlocked)`:
así se ve con la bóveda bloqueada (caso límite 9). Lectura en `Dispatchers.IO` (regla de I/O de `AGENTS.md`),
una vez por arranque (`LaunchedEffect(Unit)`), sin bucle.

## 5. La pantalla (RF-121, RF-123)

`CrashReportActivity` es una `ComponentActivity` **fuera** de `MainActivity`, sin `NotesViewModel` y sin acceso a
la base de datos: repite el patrón de `CodeEditorActivity` —idioma, tema y `FLAG_SECURE`— y por construcción no
puede saltarse la bóveda ni leer una nota.

`CrashReportScreen` pinta:

1. Cabecera con fecha y versión (del propio texto del informe).
2. El informe en `SelectionContainer` + `verticalScroll` + `FontFamily.Monospace`: seleccionable, como el resto
   de superficies de texto técnico de la app.
3. **El aviso de privacidad**, justo encima de «Enviar», siempre (RNF-67).
4. Tres botones: **Copiar** (`ClipboardManager`, como `AiSettingsScreen`), **Compartir**
   (`ACTION_SEND` + `createChooser`, como `TxtExporter`), **Enviar** (`ACTION_VIEW` con la URL pura).
5. Estado vacío cuando no hay informe (RF-123): texto explicativo y solo «Cerrar».

«Copiar», «Compartir» y «Enviar» **consumen** el informe (RF-122): si no, la app volvería a avisar del mismo
fallo la próxima vez. Copiar es la excepción parcial: **copia pero no consume**, porque el paso siguiente del
usuario es precisamente mandarlo, y consume que copia lo tira. Se dice en el texto del botón.

Hmm. Decision needed: does copying consume? Let me decide: **no**, and the UI says so? That's a weird UX. Better: **copy consumes too** — consistent, simple, and the user can copy from the notification... no, they can't. 

Think about the real flow: user taps notification → screen shows report → they tap "Copiar" → they go to their bug-report channel of choice and paste. If copy consumed, then: consistent with the other two actions. If they want to share elsewhere later, they'd have to wait for the next crash. Since it's "solo el último fallo" (decision), consuming on copy is acceptable and consistent. But there's an argument the copy is the least "final" action.

Simplest rule, and the one I'll take: **las tres acciones consumen**, because any of them means "me he-carryido este informe". Consistency beats a special case. I'll note it in the plan and in caso límite. Actually hold on, there's a real risk: a user taps "Copiar" by accident to peek, loses the report, and the next crash is months away. Versus: not consuming means the notification/dialog comes back tomorrow, nagging.

The dialog reappearing is the actual annoyance risk. The user chose "aviso dentro de la app" as the fallback, so nagging is already accepted. But losing the report is worse than a re-shown dialog.

Decision: **copiar no consume; compartir y enviar sí**. And the copy button says nothing about it; instead, after copying, the screen shows a Toast "copiado" and offers a visible "Descartar" button so the user can clear it explicitly. Hmm, that's 4 actions.

Cleaner: keep **"Cerrar"** in the top bar which **does not consume** (back = dismiss, report stays), and **"Descartar"** as an explicit destructive action that consumes. Then: copiar/compartir/enviar all consume EXCEPT... no.

Final decision (simplest, defensible, and stated): 
- **Copiar → no consume.** Se muestra un aviso de «copiado» y sigue el informe en pantalla.
- **Compartir y Enviar → consumen**: ya salió de la app, el aviso ya no tiene sentido.
- **Cerrar (barra) → no consume**: el usuario solo estaba mirando.
- **«Descartar» (botón explícito, con confirmación) → consume**: es la única forma de silenciar el aviso sin mandar nada.

This is coherent: **consume lo que saca el informe de la app; no consume lo que solo lo deja a la vista**. And it needs a confirmation dialog for "Descartar"? A second dialog is heavy. Instead: "Descartar" is a text button at the bottom with error color; tapping it consumes and finishes. Hmm, accidental tap loses the report. I'll put it in the top bar as an overflow/close... 

Simplify: **the top bar has «Cerrar» (back, no consume).** The report is discarded only by taking an action out of the app (share/send) or by the in-app notice's «Descartar». And the in-app notice is exactly the "stop nagging me" valve, and it's right there. But if the user opened from the notification, the notice isn't there... 

OK: add a **«Descartar»** text button at the bottom next to the three actions, with error color. It consumes and finishes the screen. No confirmation dialog: it's an error-colored destructive action and the consequence is only "you'll stop seeing this crash reported", and the next crash will show again. That's acceptable and I'll write it in the plan as a decision with the trade-off.

Hmm, wait. Four buttons: Copiar, Compartir, Enviar, Descartar. That's fine — a bottom row of 3 filled/outlined + 1 text.

### Empty state (RF-123)
If `hayPendiente()` is false at open time → show "no hay fallos registrados" + Cerrar. Don't crash.

## 6. Decisiones, con la alternativa descartada

1. **`Activity` propia, no pantalla del Navigator** (decidido por quien decide). La alternativa —un `Screen`
   más— obliga a que `MainActivity` esté viva **y desbloqueada**: justo cuando la app ha死人 justo después de
   caer, el estado de la bóveda es el peor posible. Con `Activity` propia el camino es: notificación → informe,
   sin contraseña y sin base de datos.

2. **`CrashStore` con `java.io.File`, sin `Context`.** La alternativa obvia —`context.filesDir` dentro del
   almacén— habría hecho sus pruebas depender de Robolectric, y aquí **no se ejecutan** (no hay runtime nativo
   para `linux/aarch64`). Con la ruta como parámetro, escribir/leer/consumir/sobrescribir se prueban de verdad.

3. **Delegar en el manejador anterior en vez de matar el proceso.** Matar a mano perdería cualquier manejador que
   el sistema o un runner hayan puesto, y rompería la premisa de RF-117: «la app se cierra como se cerraba».

4. **Escribir sincrónico y atómico, y sinDeque nada.** La alternativa —escribir en un hilo aparte y esperar—
   no cabe: el proceso muere en cuanto el manejador devuelve.

5. **Notificar en `Application.onCreate` y no pedir el permiso.** Pedirlo en cada arranque es la peor opción
   posible para el usuario; pedirlo solo cuando hay un fallo es raro y、Android no da contexto. El aviso de
   dentro cubre el hueco y por eso **es obligatorio**, no un extra (caso límite 3).

6. **El aviso de privacidad va siempre, no solo cuando se sospecha contenido del usuario.** No hay forma de
   saberlo sin mirar el informe, y mirarlo para decidir si avisar es exactamente lo que no se debe hacer con un
   dato del usuario. Además se podría equivocar y dejaría pasar justo lo que tenía que tapar.

7. **Guardar el informe en `filesDir` y no tocar las reglas de respaldo.** Los dos XML ya excluyen
   `domain="file"` entero (RNF-66): un informe nuevo **no viaja** a la nube ni en un traspaso, sin una línea
   más de configuración. La alternativa —excluir solo `crash/`— sería más fina y más frágil:Depends de que la
   exclusión por defecto siga ahí.

8. **Nada de historial.** Decidido por quien decide. Añadido: el **recorte** de traza sí es necesario, porque
   sin él un `StackOverflowError` produce una URL de incidencia de varios megabytes.

9. **Copiar no consume; compartir y enviar sí.** La alternativa —las tres consumen— tira el informe con un toque
   sin querer y, con «solo el último», ese fallo no vuelve a verse. Y «cerrar» tampoco consume: mirar no es
   llevarse el informe. Lo único que lo consume sin sacarlo de la app es **«Descartar»**, explícito.

10. **Sin dependencia nueva** (RNF-63). Crashlytics traería una clave de API que además es un dato que filtrar
    en una app que promete privacidad; ACRA necesita un servidor. Las dos contradicen el espíritu de la
    constitución 5 aunque la Cumplan.

## 7. Estrategia de pruebas

**Puras (corren aquí, sin `RobolectricTestRunner`) — RNF-64:**

| Prueba | Qué fija |
|---|---|
| `CrashReportTextTest` | El informe trae versión, Android, dispositivo, build, hilo y fecha; **no trae** `data.local`; el recorte corta a 200 líneas y **dice** que recorta; una traza corta no dice nada. |
| `CrashIssueUrlTest` | La URL apunta a `issues/new` del repositorio; el cuerpo con `\n`, `á`, `&`, `#` y `%` **vuelve igual** al descomprimirlo; el título lleva el tipo de excepción. |
| `CrashStoreTest` | `guardar`→`leer` devuelve lo escrito; `hayPendiente` es `false` sin fichero; un segundo `guardar` **sobrescribe** (caso límite 1); `consumir` borra y devuelve si había; `leer` tras consumir es `null`; un fichero vacío no está pendiente. |

**Invariantes que leen el texto de los ficheros** (`CrashInvariantsTest`, el patrón de `MarkdownSurfacesInvariantsTest`):

| Invariante | Qué fija |
|---|---|
| El manejador se instala en `onCreate` | RF-117, RNF-69 |
| Guarda el anterior **y** delega al final, con todo el cuerpo en `runCatching` | RF-117, RNF-65 |
| Los tres ficheros puros **no** importan `android` | RNF-64 |
| El paquete `crash` **no** importa ningún cliente de red | RNF-67 |
| La `Activity` del informe **no** importa nada de `data.local` ni el `NotesViewModel` | RF-118, caso límite 9 |
| `AndroidManifest` declara la `Activity` y el aviso aparece en `AppMainContent` | RF-119, RF-120 |
| Los dos XML de respaldo excluyen `domain="file"` | RNF-66 |
| Las nueve carpetas tienen todas las claves nuevas | RNF-68 |

Cada recorrido **quita comentarios antes de buscar** —si no, un invariante que busca `UncaughtExceptionHandler`
se pone verde con el KDoc que explica por qué existe—, y **autocomprueba que está leyendo el fichero**, porque un
recorrido roto pasa **por vacío**.

**Comando:** `./gradlew :app:testDebugUnitTest --rerun --continue`, y la comparación **clase por clase** contra
la base de `MEMORY.md` (`:app` 668/41). Un `BUILD SUCCESSFUL` con `UP-TO-DATE` no cuenta.

**Lo que ninguna prueba de aquí puede comprobar** (RNF-70): que la notificación aparezca, que la `Activity` abra
desde ella, que el navegador acepte la URL de la incidencia y que el aviso se vea con la bóveda bloqueada.
Hace falta un fallo real en un dispositivo.

## 8. Qué cubre cada RF

| RF | Dónde |
|---|---|
| RF-117 | `CrashReporter.instalar`, `CrashStore.guardar`, invariante del manejador |
| RF-118 | `CrashReportText.construir`, invariante de `data.local` |
| RF-119 | `CrashNotifier.anunciar` + `SecureNotesApplication.onCreate`, invariante del manifiesto |
| RF-120 | `AppMainContent` + `CrashNotifier.cancelar`, invariante del aviso |
| RF-121 | `CrashReportScreen`, `CrashIssueUrl`, prueba del viaje redondo |
| RF-122 | `CrashStore.consumir` + `CrashNotifier.cancelar` |
| RF-123 | `CrashReportViewModel` (vacío) + `CrashStore.leer() == null`, invariante del estado vacío |