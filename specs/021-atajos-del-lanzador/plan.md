# Plan — Spec 021: los atajos del lanzador

> Implementa `specs/021-atajos-del-lanzador/spec.md` (RF-124…RF-130, RNF-71…RNF-75). La spec es la que manda;
> aquí solo van archivos, algoritmo y decisiones.

## 1. Archivos y responsabilidades

### Nuevos

| Archivo | Responsabilidad | Cubre |
|---|---|---|
| `app/src/main/res/xml/shortcuts.xml` | Los cinco atajos estáticos, en orden | RF-124, RF-130 |
| `app/src/main/res/drawable/sc_*.xml` (5) | Un vector de un color por atajo, **sin `android:tint`** | RF-124 |
| `app/src/main/java/com/example/shortcut/ShortcutDestination.kt` | `enum` de los cinco destinos + `desde(clave)` + `EXTRA_DESTINO`. **Puro**: sin `android`, sin Compose | RF-125, RF-129 |
| `app/src/main/java/com/example/shortcut/ShortcutPending.kt` | `ShortcutPending` (guardar/drenar) + `ShortcutDecision` + `decidir(...)`. **Puro** | RF-127, RF-129 |
| `app/src/main/java/com/example/ui/ShortcutDestinations.kt` | `ejecutarDestino(...)`: el `when` que traduce un destino a navegación. Toca Compose, no se prueba solo | RF-125, RF-126 |

### Modificados

| Archivo | Cambio | Cubre |
|---|---|---|
| `app/src/main/AndroidManifest.xml` | Un `<meta-data android:name="android.app.shortcuts">` dentro de `MainActivity` | RF-124 |
| `app/src/main/java/com/example/MainActivity.kt` | Un `ShortcutPending` en el `companion`; `resolverAtajo(...)` usada por **los dos** caminos; volcado en `LaunchedEffect(isUnlocked)`; `removeExtra` tras consumir | RF-127, RF-128 |
| `app/src/main/java/com/example/ui/Screen.kt` | `launchCodeEditor` pasa de `private` a `internal` para que el atajo lo use | RF-125 |

### Nuevos de prueba

| Archivo | Tipo | Cubre |
|---|---|---|
| `app/src/test/java/com/example/shortcut/ShortcutDestinationTest.kt` | pura | RF-125, RF-129 |
| `app/src/test/java/com/example/shortcut/ShortcutDecisionTest.kt` | pura | RF-127, RF-129 |
| `app/src/test/java/com/example/shortcut/ShortcutPendingTest.kt` | pura | RF-127 |
| `app/src/test/java/com/example/ShortcutInvariantsTest.kt` | invariantes | RF-124, RF-128, RF-130, RNF-72…RNF-74 |

**Ninguna tarea toca `build.gradle.kts` ni ningún `strings.xml`** (RNF-71, RNF-72).

## 2. Funciones puras

Las tres viven en `com.example.shortcut` y **no importan `android` ni `androidx.compose`**, que es lo que las
hace ejecutables aquí (RNF-73: la suite con Robolectric no corre en esta plataforma; las puras sí).

```kotlin
// ShortcutDestination.kt
const val EXTRA_DESTINO = "shortcut_destination"

enum class ShortcutDestination(val clave: String) {
    NEW_NOTE("new_note"),
    SEARCH("search"),
    AI_CHAT("ai_chat"),
    SETTINGS("settings"),
    CODE_EDITOR("code_editor");

    companion object {
        fun desde(clave: String?): ShortcutDestination? = entries.firstOrNull { it.clave == clave }
    }
}
```

`desde` es un `firstOrNull` sobre `entries` a propósito: **devuelve `null` para `null`, para vacío y para
cualquier cosa desconocida sin escribir un `when` que mantener** (RF-129), y no lanza.

```kotlin
// ShortcutPending.kt
sealed interface ShortcutDecision {
    data class Ejecutar(val destino: ShortcutDestination) : ShortcutDecision
    data class Pendiente(val destino: ShortcutDestination) : ShortcutDecision
    data object Ignorar : ShortcutDecision
}

class ShortcutPending {
    var destino: ShortcutDestination? = null
        private set

    fun registra(nuevo: ShortcutDestination) { destino = nuevo }   // el último gana (caso límite 4)

    fun drena(): ShortcutDestination? = destino.also { destino = null }  // escupir **vacía** (RF-127)
}

fun decidir(clave: String?, abierta: Boolean, pendiente: ShortcutPending): ShortcutDecision {
    val destino = ShortcutDestination.desde(clave) ?: return ShortcutDecision.Ignorar
    if (abierta) return ShortcutDecision.Ejecutar(destino)
    pendiente.registra(destino)
    return ShortcutDecision.Pendiente(destino)
}
```

**Por qué una clase y no un `object`**: cada prueba arranca con una instancia limpia. Un `object` con estado
mutable compartido filtra estado entre pruebas y el fallo sale en la prueba siguiente, no en la que lo introdujo.

**Por qué un `sealed interface` y no `ShortcutDestination?`**: `null` significa dos cosas distintas —«no hay
destino» y «queda pendiente»— y confundirlas es justo el fallo que dejaría un atajo esperando para siempre. Con
tres casos el compilador obliga a cubrirlos todos.

## 3. Algoritmo

### 3.1 `resolverAtajo` — un solo camino para los dos arrivals (RF-128)

Se declara **dentro de `AppMainContent`**, después de `navigator` (línea 377) y de `chatHistoryViewModel`
(línea 391), que es donde ya se pueden usar. Por debajo de `LaunchedEffect(Unit)` (405), que es el arranque en
frío, y se reutiliza tal cual dentro del `intentRelay` (481), que es `onNewIntent`.

```
fun resolverAtajo(intent: Intent?, abierta: Boolean) {
    when (val d = decidir(intent?.getStringExtra(EXTRA_DESTINO), abierta, atajoPendiente)) {
        is Ejecutar -> ejecutarDestino(d.destino, navigator, chatHistoryViewModel, aiViewModel.backend.value, context)
        else -> Unit          // Pendiente ya quedó registrado; Ignorar no hace nada
    }
    // Sin esto, el Intent sigue carrying el extra y `LaunchedEffect(Unit)` **se vuelve a ejecutar en cada
    // rotación**: el atajo de la IA crearía una conversación nueva cada vez que el usuario gira el móvil.
    intent?.removeExtra(EXTRA_DESTINO)
}
```

- **Arranque en frío** (`LaunchedEffect(Unit)`): `resolverAtajo(activity?.intent, isUnlocked)`.
- **Con la app viva** (`intentRelay`): `resolverAtajo(intent, isUnlocked)`. `onNewIntent` hace `setIntent(intent)`,
  así que es el **mismo objeto**: quitar el extra en el helper cubre los dos casos.
- **Al desbloquear** (`LaunchedEffect(isUnlocked)`, que ya existe para el sobre de notas): se añade
  `atajoPendiente.drena()?.let { ejecutarDestino(it, …) }` (RF-127). Ojo: **dentro** de `if (isUnlocked)`, y el
  efecto **ya** tiene la clave correcta `isUnlocked`, así que no repite el error del reloj que se sufrió con el
  handoff.

**Por qué `removeExtra` y no confiar en que `LaunchedEffect(Unit)` corre una vez**: corre una vez *por
composición*, y una rotación crea una composición nueva con el mismo `Intent`. El código existente ya aprendió
esto con `new_note` (línea 414); el atajo de la IA es donde se nota, porque `createSession()` inserta fila.

### 3.2 `ejecutarDestino` — destino a navegación (RF-125, RF-126)

```
fun ejecutarDestino(destino, navigator, chatHistoryViewModel, backend, context) = when (destino) {
    NEW_NOTE    -> navigator.onNavigateTo(Screen.NoteEditor(0))
    SEARCH      -> navigator.onNavigateTo(Screen.Search)
    AI_CHAT     -> { chatHistoryViewModel.createSession(backend = backend)
                     navigator.onNavigateTo(Screen.AiChatStandalone) }
    SETTINGS    -> navigator.onNavigateTo(Screen.SettingsHub)
    CODE_EDITOR -> launchCodeEditor(context)
}
```

Cuatro de los cinco son una línea: **reutilizan exactamente las pantallas que los botones ya navegan**
(`Screen.Search`, `Screen.SettingsHub`, `Screen.AiChatStandalone`), así que «atrás» se comporta como ya está
cableado — de Búsqueda, a la lista (decisión tomada). `CODE_EDITOR` es el único que sale de la app hacia otra
`Activity`, y lo hace **después** de desbloquear (RF-127), reutilizando `launchCodeEditor`.

`AI_CHAT` copia el par `createSession` + navegar del botón de la lista (`Screen.kt:61-64`). **No** se reutiliza
`latestSessionId`: la decisión fue sesión nueva siempre (RF-126).

## 4. Interfaz

`res/xml/shortcuts.xml`, cinco `<shortcut>` en este orden (el último es el que un lanzador de 4 puede no
mostrar):

| # | `shortcutId` | `shortcutShortLabel` | Clave ya existente |
|---|---|---|---|
| 1 | `new_note` | `@string/btn_new_note` | «New Note» |
| 2 | `search` | `@string/search_notes` | «Search notes» |
| 3 | `ai_chat` | `@string/ai_chat_title` | «AI Chat» |
| 4 | `settings` | `@string/nav_settings` | «Settings» |
| 5 | `code_editor` | `@string/code_editor` | «Code Editor» |

Cada `<intent>`: `android:action="android.intent.action.VIEW"`, `android:targetPackage` y
`android:targetClass` apuntando a `MainActivity`. **Sin `android:data` ni `android:mimeType`** (RF-130): los
`<intent>` de `shortcuts.xml` no admiten recursos de cadena para extras, así que el destino viaja en un extra
**literal**, que es la razón de que `EXTRA_DESTINO` sea una `const val` y no un recurso.

Restricciones de la plataforma que **no** se pueden ignorar y que un invariante vigila:

- `shortcutId` es **literal**: no puede ser `@string`.
- `shortcutShortLabel` **sí** es un `@string` (RNF-72).
- No existe atributo `monochrome` y la documentación avisa de que el icono *can't include tints*: los cinco
  vectores son de un solo color y **sin `android:tint`**.

## 5. Decisiones y alternativas descartadas

1. **Atajo del editor de código vía `MainActivity`, no directo.** Descartado el directo porque la plataforma
   **solo** deja declarar atajos en una Activity `MAIN`/`LAUNCHER` y `CodeEditorActivity` solo tiene
   `SEND`/`VIEW`; lo otro —darle `MAIN`/`LAUNCHER`— pondría un **segundo icono** en el lanzador.
2. **El candado manda también para el editor de código.** `CodeEditorActivity` no tiene candado y es
   `exported="true"`: hoy se puede abrir desde fuera sin contraseña. No se arregla aquí (es otra spec) pero el
   atajo **no añade** una vía más, porque pasa por `MainActivity` (RF-127).
3. **Reutilizar los extras de los widgets (`new_note`, `open_note_id`) en vez de uno nuevo.** Descartado: esos
   son un contrato con `WidgetActions.kt` y solo cubren dos destinos. Un `EXTRA_DESTINO` con conjunto cerrado
   cubre los cinco y se puede **comparar con el enum en una prueba**, que es lo que RF-125 pide.
4. **`ShortcutPending` como `object` con estado global.** Descartado por lo de §2: las pruebas se ensucian.
5. **`ShortcutManagerCompat` / atajos dinámicos.** Descartado: no hay nada que varíe en tiempo de ejecución y
   añadiría dependencias (RNF-71).
6. **`ShortcutDestination` sin `internal`.** Descartado: es el contrato entre el XML y el parser, y las
   pruebas lo necesitan.

## 6. Estrategia de tests

`./gradlew test` **no puede estar verde aquí** (Robolectric sin runtime nativo para `linux/aarch64`). La
estrategia es, por tanto, en tres capas:

1. **Puras primero (las que corren).**
   `ShortcutDestinationTest`, `ShortcutDecisionTest`, `ShortcutPendingTest`: JUnit sin Android.
   Se ejecutan con
   `./gradlew :app:testDebugUnitTest --tests 'com.example.shortcut.*' --rerun`
   → deben salir **todas** en verde y **sin ninguna clase de plataforma**.
2. **Invariantes** (`ShortcutInvariantsTest`), que leen los ficheros y miden **texto**, no comportamiento:
   forma del XML, los ids literales y únicos, las etiquetas en los **nueve** locales, el `meta-data` dentro de
   la Activity `MAIN`/`LAUNCHER`, que no hay `data`/`mimeType`, que los dos caminos llaman a la misma función,
   que el volcado está en el efecto que observa `isUnlocked`, que está `removeExtra`, que el fichero puro no
   importa Compose y que el camino de decisión no toca DAO ni `CipherService`.
   **Dos reglas heredadas** (`MEMORY.md`): quitar comentarios antes de buscar, y **autocomprobar que el
   recorrido leyó el fichero**.
3. **Compilación y recuento**:
   `./gradlew :app:compileDebugUnitTestKotlin --rerun` en verde, y la suite completa con `--continue` para
   comparar **por clase de fallo**, no en cantidad: base **`:app` 711 / 41** y **`:editor-de-codigo` 145 / 2**.
   Los 41 son de plataforma salvo 3 preexistentes (`HtmlExportMediaTest`, dos de `OnDeviceModelSourceTest`).
   **Si aparece un fallo nuevo, la spec no está implementada aunque compile.**

Verificación en dispositivo (no automatizable, queda anotada en la spec): mantener pulsado el icono, ver los
cinco, y comprobar que con la bóveda cerrada **salen el candado** y luego el destino.