# Plan 030 — Captura de bloqueo sin Android Keystore

Estado: **implementado**

## Archivos y responsabilidades

- `app/src/main/java/com/example/ui/LockScreen.kt`
  - Mantener `LockScreen(viewModel)` como adaptador de producción: recopila los mismos flujos, conserva el estado local de contraseña/error y deja la autenticación biométrica y los `Toast` en esta capa.
  - Extraer el render a un Composable de presentación interno que reciba el estado visible y callbacks explícitos para cambiar contraseña, desbloquear e iniciar biometría.
  - Mantener sin cambios los textos, tags, accesibilidad, disposición y condiciones de disponibilidad de los controles. Separar en subcomposables si hace falta para respetar la convención de funciones breves.
- `app/src/test/java/com/example/GreetingScreenshotTest.kt`
  - Renderizar el Composable de presentación con datos controlados que representen el caso inicial aprobado.
  - Eliminar la creación del `Application`, `NoteDatabase` y `NotesViewModel`; usar callbacks no-op y conservar la regla Robolectric, el dispositivo, el modo gráfico, el nombre del test y la ruta del golden.
- `MEMORY.md`
  - Registrar el resultado de la spec una vez implementada y validada; durante esta fase dejar constancia de que la spec fue aprobada y el plan espera aprobación.

## Funciones puras

No se añade lógica de decisión independiente: estado de contraseña, límite de intentos y autenticación siguen perteneciendo al ViewModel/adaptador actual. El Composable de presentación solo recibe valores ya resueltos y emite eventos.

## Algoritmo

```text
LockScreen(viewModel):
  conservar password y hasError como estado local
  recoger los mismos StateFlow que hoy
  conservar BiometricAuthManager, launcher, efectos y función de autenticación
  renderizar LockScreenContent con valores actuales
  pasar callbacks que mantengan exactamente la lógica de unlock/error/Toast existente

LockScreenContent(estado, callbacks):
  construir Scaffold y los mismos elementos visuales
  mostrar error y límite solo cuando los flags del estado lo indican
  mostrar acción biométrica solo cuando esté habilitada y disponible
  emitir callbacks en cambios y acciones; no consultar Android ni ViewModel

GreetingScreenshotTest:
  instalar MyApplicationTheme
  renderizar LockScreenContent con contraseña vacía, contraseña normal,
  sin error, sin límite, cero segundos y biometría deshabilitada/no disponible
  capturar el root en la ruta Roborazzi existente
```

## Interfaz

- Se conserva la firma pública `LockScreen(viewModel: NotesViewModel)`.
- Se añade un Composable de presentación interno con estado explícito (texto de contraseña, tipo, error, rate limit y datos biométricos) y callbacks explícitos (cambio de contraseña, desbloqueo y autenticación biométrica).
- No se modifica `NotesViewModel`, `SecurePrefsStore`, la autenticación ni el contrato de navegación.

## Decisiones

- **Presentación separada de la conexión Android/ViewModel.** Evita que el screenshot inicialice Keystore y hace explícita la entrada visual. Alternativa descartada: sustituir o inyectar el almacenamiento seguro del ViewModel solo para construir el test; eso probaría inicialización que la captura no necesita y ampliaría el cambio a seguridad/persistencia.
- **El adaptador de producción conserva la lógica existente.** Así se limita el riesgo de cambiar comportamiento de autenticación y del ciclo de vida biométrico. Alternativa descartada: trasladar el flujo biométrico al nuevo Composable, porque mezclaría efectos Android con presentación.
- **Callbacks no-op en la captura y golden existente.** La prueba valida el render, no el desbloqueo, y no debe actualizarse el golden salvo que la comparación demuestre una diferencia visual no intencional que haya que corregir.

## Estrategia de tests

1. Ejecutar el test actual para confirmar el fallo reproducible de Keystore antes de cambiarlo:
   `./gradlew :app:testDebugUnitTest --tests '*GreetingScreenshotTest*' --rerun`.
2. Tras la extracción, ejecutar el test dirigido con el mismo comando; debe pasar, mantener la captura y no construir `NotesViewModel`, `NoteDatabase` ni `SecurePrefsStore`.
3. Ejecutar `./gradlew :app:compileDebugUnitTestKotlin` para comprobar compilación de producción y tests.
4. Ejecutar `git diff --check`.

RF-198 se cubre al probar el contenido con estado explícito sin infraestructura persistente; RF-199 al conservar el adaptador y sus callbacks de producción; RF-200 al mantener el test Roborazzi y golden existentes. RNF-128 se verifica inspeccionando que el Composable de presentación no acceda a servicios/contexto; RNF-129 con la comparación del golden y revisión de los tags/textos; RNF-130 al no añadir dependencias ni recursos visibles.
