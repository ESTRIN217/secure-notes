# Spec 030 — Captura de bloqueo sin Android Keystore

Estado: **implementada**

## Contexto y objetivo

El último fallo de `:app:testDebugUnitTest` ocurre antes de capturar la pantalla: el screenshot test construye
`NotesViewModel`, que inicializa `SecurePrefsStore` y requiere el proveedor Android Keystore. Robolectric no ofrece
ese proveedor en este entorno. El objetivo es conservar la captura visual de la pantalla de bloqueo y permitir
que se ejecute en el host sin inicializar almacenamiento seguro ni estado persistente.

## Usuarios

- Quien mantiene la pantalla de bloqueo y necesita una captura visual repetible.
- Quien ejecuta las pruebas unitarias en un host sin Android Keystore.

## Historias de usuario

- **HU-1.** Como mantenedor, quiero probar visualmente el contenido de bloqueo con estado determinista, para que
  el test no dependa de servicios de seguridad del dispositivo.
- **HU-2.** Como usuario de la app, quiero que el flujo real siga conectando ese contenido con el ViewModel y la
  autenticación, sin cambios en el comportamiento de desbloqueo.

## Requisitos funcionales

- **RF-198.** CUANDO se capture la pantalla de bloqueo en una prueba de JVM, EL SISTEMA renderiza el mismo contenido
  visual de bloqueo a partir de un estado explícito, sin crear `NotesViewModel`, abrir Room ni acceder a
  `AndroidKeyStore`.
- **RF-199.** CUANDO la app muestre la pantalla de bloqueo, EL SISTEMA conserva el enlace entre el estado y los
  eventos reales del `NotesViewModel`, incluida autenticación por contraseña y la vía biométrica cuando está
  habilitada y disponible.
- **RF-200.** EL SISTEMA mantiene una captura Roborazzi del contenido visual de bloqueo; la prueba no se omite ni
  se sustituye por una captura de otra pantalla.

## Requisitos no funcionales

- **RNF-128.** El contenido visual recibe estado y callbacks; no instancia ViewModels, DAOs, cifrado ni
  almacenamiento.
- **RNF-129.** La extracción no cambia textos, jerarquía visual, accesibilidad, etiquetas de prueba ni lógica de
  autenticación.
- **RNF-130.** Cero dependencias nuevas y cero cadenas visibles nuevas.

## Casos límite

- El estado inicial de captura representa una cuenta bloqueada con contraseña, sin error, sin limitación de
  intentos y con biometría desactivada.
- La prueba sigue usando tema, densidad y tamaño de dispositivo fijados por su configuración Roborazzi.
- Los callbacks de la captura pueden ser no-op; la prueba verifica el render y no simula ni certifica el
  desbloqueo.

## Fuera de alcance

- Probar la generación de claves Android Keystore en Robolectric.
- Cambiar `SecurePrefsStore`, el esquema de seguridad o la autenticación.
- Cambiar el diseño de la pantalla o actualizar el golden visual salvo diferencias causadas por variabilidad
  que se demuestre durante la validación.
- Añadir tests de comportamiento de autenticación; estos pertenecen a las pruebas de la lógica/ViewModel.

## Criterios de finalización

- El test de captura renderiza el contenido de bloqueo sin instanciar `NotesViewModel`, `NoteDatabase` ni
  `SecurePrefsStore`.
- `./gradlew :app:testDebugUnitTest --tests '*GreetingScreenshotTest*' --rerun` termina en
  `BUILD SUCCESSFUL` y conserva el test.
- El render conectado a `LockScreen(viewModel)` sigue compilando con todos los callbacks reales.
- `git diff --check` no informa errores.

## Dudas abiertas

- Ninguna. Se eligió extraer el contenido visual con estado/callbacks explícitos, en vez de introducir una
  implementación falsa de preferencias seguras en el ViewModel.
