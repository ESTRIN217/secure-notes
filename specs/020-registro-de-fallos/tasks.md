# Tareas — Spec 020: el registro de fallos

Estado: **T1-T9 cerradas** (2026-10-16). Sin verificación en dispositivo (RNF-70). Una tarea cada vez: pruebas en rojo, código, `./gradlew :app:testDebugUnitTest` en verde,
marcar y parar.

## Orden

- [x] **T1. Pruebas puras en rojo: informe, URL de incidencia y almacén.** RF-118, RF-121, RF-122, RF-123
  - `CrashReportTextTest`: el informe trae versión, Android, dispositivo, build, hilo y fecha; una traza de 500
    líneas se recorta a 200 y **dice** que se recorta; una traza de 5 líneas no dice nada.
  - `CrashIssueUrlTest`: la URL es `issues/new` del repositorio; un cuerpo con `\n`, `á`, `&`, `#` y `%`
    **vuelve igual** al descomprimirlo; el título lleva el tipo de excepción.
  - `CrashStoreTest`: guardar→leer; `hayPendiente` falso sin fichero; un segundo `guardar` sobrescribe;
    `consumir` borra y dice si había; leer tras consumir es `null`; fichero vacío no está pendiente.
  - **Hecho cuando**: los tres ficheros existen, **no compilan** (las tres clases de producción aún no), y el
    fallo es «no se encuentra el símbolo», no otra cosa.

- [x] **T2. La lógica pura, en verde.** RF-118, RF-121, RF-122, RF-123, RNF-64
  - `CrashReportText.kt`, `CrashIssueUrl.kt`, `CrashStore.kt` en `com.example.crash`, sin un solo `import android`.
  - **Hecho cuando**: `./gradlew :app:testDebugUnitTest --rerun --continue` deja las tres pruebas verdes y
    `grep -c '^import android'` vale **0** en los tres ficheros.

- [x] **T3. El manejador de excepciones y el aviso de arranque.** RF-117, RF-119, RNF-65
  - `CrashReporter.kt`: `instalar` una sola vez, guarda el anterior, registra en `runCatching`, **delega siempre**.
  - `SecureNotesApplication.onCreate`: `CrashReporter.instalar(this)` y `CrashNotifier.anunciar(this)`, en ese
    orden y **nada más** en el arranque.
  - **Hecho cuando**: un fallo real en un hilo secundario deja informe y la app se cierra igual; compila.

- [x] **T4. La notificación.** RF-119, RF-122, caso límite 3
  - `CrashNotifier.kt`: canal con sus dos textos, `PendingIntent` a `CrashReportActivity`, `notify` dentro de
    `runCatching` (sin permiso no rompe), y `cancelar`.
  - **Hecho cuando**: con el permiso concedido sale una notificación que abre la `Activity`; sin permiso, no
    rompe y el aviso de dentro es lo que avisa.

- [x] **T5. La pantalla del informe.** RF-121, RF-123, RNF-67
  - `CrashReportViewModel` (lee en `Dispatchers.IO`, expone `StateFlow`), `CrashReportScreen` y
    `CrashReportActivity` con idioma, tema y `FLAG_SECURE` como `CodeEditorActivity`.
  - Texto seleccionable y desplazable en monoespaciado, **aviso de privacidad** junto a «Enviar», Copiar /
    Compartir / Enviar / Descartar, y estado vacío con Cerrar.
  - **Hecho cuando**: la `Activity` se abre a mano y a pie de pantalla; sin informe no se queda en blanco;
    ninguna acción revienta.

- [x] **T6. El aviso dentro de la app.** RF-120, RF-122, caso límite 9
  - `AlertDialog` en `AppMainContent`, **después** del `if (isPasswordSet && !isUnlocked)`, una vez por arranque,
    con lectura en `Dispatchers.IO`: «Ver informe» y «Descartar».
  - Ambas acciones consumen el informe y cancelan la notificación.
  - **Hecho cuando**: con la bóveda bloqueada el aviso se ve igual; al descartar no vuelve a salir hasta que
    haya otro fallo.

- [x] **T7. Los nueve idiomas y el manifiesto.** RNF-68
  - Todas las claves nuevas en los **nueve** `strings.xml`, **sin apóstrofos crudos** (`\'`), y
    `CrashReportActivity` declarada en `AndroidManifest.xml` con `exported="false"`.
  - **Hecho cuando**: `./gradlew :app:assembleDebug` pasa `mergeDebugResources` (que es donde revienta un
    apóstrofo crudo, después de que Kotlin compile limpio).

- [x] **T8. Los invariantes.** RF-117, RF-118, RF-119, RF-120, RNF-63, RNF-64, RNF-66, RNF-67, RNF-69
  - `CrashInvariantsTest`: manejador instalado y delegando; los tres puros sin `android`; el paquete `crash`
    sin cliente de red; la `Activity` sin `data.local` ni `NotesViewModel`; manifiesto y aviso; los dos XML de
    respaldo; las nueve carpetas con todas las claves.
  - Cada recorrido **quita comentarios** antes de buscar y **autocomprueba que lee el fichero**.
  - **Hecho cuando**: verde **con el código correcto**, y un recorrido roto (ruta mala) lo detecta.

- [x] **T9. Cerrar: build, suite completa, memoria.** RNF-63, RNF-70
  - `./gradlew :app:assembleDebug` y `./gradlew :app:testDebugUnitTest --rerun --continue`; comparación **clase
    por clase** contra la base de `MEMORY.md` (`:app` 668/41) y el árbol de dependencias de `:app` (630).
  - `MEMORY.md` actualizado con el estado, las decisiones y **que nada de esto se ha visto en un dispositivo**.
  - **Hecho cuando**: los números están medidos y escritos, incluidos los que no cambian.