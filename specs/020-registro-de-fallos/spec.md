# Spec 020 — El registro de fallos: guardar el crash, mostrarlo y poder mandarlo a GitHub

Estado: **implementada** (2026-10-16), T1-T9 cerradas; **sin verificación en dispositivo**

> **Nota de procedencia.** No encadena con ninguna spec anterior: es la primera que toca el **ciclo de vida
> del proceso**. Los RF más altos en uso son `RF-116` y `RNF-62` (spec 019), así que aquí empiezan en `RF-117`
> y `RNF-63`.
>
> **No reescribe** ningún RF vigente. Lo único que roza es el aviso de «los datos del usuario no salen del
> dispositivo» (constitución 5), y por eso el informe se envía **a mano**, nunca solo: ver `RNF-67`.

## Contexto y objetivo

Hoy la app **no registra nada** cuando se cae. No hay `Thread.setDefaultUncaughtExceptionHandler` en todo el
proyecto: un fallo mata el proceso, el usuario vuelve a abrir la app y ve la lista de notas como si nada,
sin ninguna pista de qué pasó. Para quien mantiene una app sin servidor de telemetría, eso es el peor modo de
fallo posible: **el quenofalla y nadie puede reportar**.

El objetivo es un **registro del último fallo** que:

1. se escriba **antes** de que el proceso muera,
2. se ofrezca en cuanto la app vuelve a arrancar (notificación, y aviso dentro de la app si no hay permiso),
3. se pueda **leer, copiar, compartir o mandar a una incidencia de GitHub** con tres toques y sin salir de la app.

## Usuarios

- Quien usa la app y se le cae: quiere saber **qué pasó** sin saber lo que es una traza de pila, y poder
  mandársela a quien la mantiene sin escribir nada.
- Quien mantiene la app: recibe una incidencia con la versión exacta, el dispositivo y la traza, y **sin** datos
  de las notas del usuario.
- Quien lee el repositorio sin tener dispositivo: ve que la app tiene un camino de reporte, y no necesita
  instalar Crashlytics ni una clave de API paraarlo.

## Definiciones

- **Fallo**: una excepción **no capturada** que llega al manejador por defecto de un hilo y mata el proceso. No
  es un `try/catch` que la app registra con `Log.e()`, ni un cierre por falta de memoria del sistema, ni un
  ANR.
- **Informe**: el fichero de texto plano con los datos técnicos del fallo y la traza completa.
- **Informe pendiente**: un informe escrito en el disco que el usuario todavía no ha abierto ni descartado.
- **Consumir**: borrar el informe pendiente y cancelar su notificación.

## Requisitos funcionales

- **RF-117**: CUANDO una excepción no capturada llegue al manejador por defecto de cualquier hilo, EL SISTEMA
  **escribe el informe en el disco antes de devolver el control** y después **delega en el manejador que
  hubiera sin él**, de modo que la app se cierra exactamente igual que ahora. Verificable: un invariante que
  comprueba que el manejador se instala en `SecureNotesApplication.onCreate()`, que guarda el anterior antes de
  sustituirlo, y que **todo** su cuerpo va dentro de un `runCatching` cuya última línea es la delegación.

- **RF-118**: CUANDO se construye el informe, EL SISTEMA lo rellena **solo con datos técnicos**: versión de la
  app, versión de Android, modelo y fabricante del dispositivo, si es `DEBUG` o `RELEASE`, fecha y hora, nombre del
  hilo, clase de la excepción, su mensaje y la traza completa; y **no** con contenido de notas, títulos, etiquetas
  ni nada que venga de la base de datos. Verificable: una función pura `construir(...)` con una excepción de
  prueba que devuelve el texto entero, y un invariante que comprueba que el módulo del informe **no** importa
  ninguna clase de `data.local`.

- **RF-119**: CUANDO la app arranca y hay un informe pendiente, EL SISTEMA **publica una notificación** que, al
  pulsarse, **abre la pantalla del informe**. Verificable: un invariante que comprueba que existe el canal, que el
  `PendingIntent` apunta a la Activity del informe y que se llama **antes** de leer el informe pendiente.

- **RF-120**: CUANDO hay un informe pendiente, EL SISTEMA muestra además un **aviso dentro de la app** con dos
  salidas —«Ver informe» y «Descartar»— **también con la bóveda bloqueada**, porque el informe no contiene datos
  del usuario. Verificable: el aviso se pinta en el contenido principal, no en la lista de notas, y una prueba
  pura que decide si toca mostrarlo.

- **RF-121**: CUANDO el usuario abre el informe, EL SISTEMA lo muestra **completo, desplazable y seleccionable**,
  con tres acciones: **copiar** al portapapeles, **compartir** con el diálogo de compartir del sistema, y
  **enviar**, que abre en el navegador una **incidencia nueva** del repositorio con el título y el cuerpo ya
  rellenos con el informe. Verificable: una función pura que construye la URL de la incidencia y la prueba que
  comprueba que un informe con saltos de línea, acentos y `&` vuelve **igual** al descomprimirla.

- **RF-122**: SI el usuario abre o descarta el informe, ENTONCES EL SISTEMA lo **consume**: borra el fichero,
  cancela la notificación y no vuelve a avisar. Verificable: una función pura que dice qué queda pendiente
  después de consumir, y un invariante que comprueba que se llama a `NotificationManager.cancel`.

- **RF-123**: SI **no** hay informe pendiente, ENTONCES EL SISTEMA **no** publica notificación ni aviso, y la
  pantalla del informe explica que no hay nada que mostrar en vez de quedarse en blanco. Verificable: la función
  pura de pendiente con el fichero ausente, más un invariante de que la pantalla tiene estado vacío.

## Requisitos no funcionales

- **RNF-63 — Cero dependencias nuevas.** Ni Crashlytics, ni ACRA, ni Bugsnag: todo con la plataforma
  (`UncaughtExceptionHandler`, `NotificationManager`, `ClipboardManager`, `Intent.ACTION_VIEW`). Verificable: el
  árbol de dependencias de `:app` no gana ningún artefacto y `oss-licenses.json` no se toca.

- **RNF-64 — La lógica es pura y sus pruebas **sí** corren aquí.** El texto del informe, el recorte de trazas,
  la URL de la incidencia, la pregunta «¿hay informe pendiente?» y la regla de aviso viven en ficheros **sin
  `android` y sin Compose**, porque Robolectric no tiene runtime nativo para `linux/aarch64` y una prueba de
  eso no se ejecutaría nunca en esta plataforma. Verificable: `grep -c '^import android'` vale **0** en el
  paquete del informe y sus pruebas no llevan `RobolectricTestRunner`.

- **RNF-65 — Registrar el fallo nunca cambia cómo se muere la app.** La escritura es **sincrónica** —el proceso
  va a morir— y **atómica** (fichero temporal y renombrado), y va envuelta: si escribir o renombrar falla, se
  delega igual. Verificable: el invariante de RF-117 más un `runCatching` en la escritura.

- **RNF-66 — El informe se guarda en `filesDir`, que ya está fuera del respaldo.** Los dos XML de reglas
  excluyen `domain="file"` entero, así que un informe con la traza de un fallo **no viaja** a la nube de respaldo
  ni en un traspaso de dispositivo. Verificable: un invariante que lee `backup_rules.xml` y
  `data_extraction_rules.xml` y comprueba la exclusión.

- **RNF-67 — Nada sale del dispositivo solo, y el botón de enviar avisa.** La app no hace red con el informe: lo
  manda **el usuario**, con un gesto. Y **siempre** se muestra, junto a «Enviar», el aviso de que **el mensaje de
  la excepción puede llevar un fragmento de una nota** —es el único campo que no controla esta app—, para que
  nadie publique nada sin leerlo. Verificable: un invariante que comprueba que el aviso está en la pantalla, y
  otro que comprueba que el paquete del informe **no** importa ningún cliente de red.

- **RNF-68 — Los nueve idiomas, a la vez.** Todos los textos nuevos salen de `strings.xml` y están en los nueve
  locales (constitución 6). Verificable: un invariante que recorre las nueve carpetas y falla si falta una clave.

- **RNF-69 — Todo lo demás sigue igual.** La lista de notas, la bóveda, el editor, el chat, el modelo de IA y
  los widgets **no** cambian. La única pieza global es el manejador de excepciones. Verificable: `git diff` solo
  toca los ficheros de esta spec, y un invariante comprueba que el manejador se instala en `onCreate` **sin**
  sustituir nada más del arranque.

- **RNF-70 — Nada de esto se ha visto en un dispositivo.** No hay forma de comprobar aquí que la notificación
  aparece, que se abre la Activity o que el navegador acepta la URL de la incidencia: hace falta un fallo real.
  Queda anotado como pendiente, con su lista, al final de `plan.md`.

## Casos límite

1. **La app se cae otra vez antes de mirar el informe.** Cada fallo **sobrescribe** el anterior: el informe es
   siempre **el último**, que es lo que hace falta para depurar. Un fallo en bucle deja el último, no una lista.
2. **La app se muere sin excepción** —el sistema la mata por falta de memoria—. No hay informe y no hay aviso:
   se dice «no hay fallos registrados» y no se inventa nada.
3. **Sin permiso de notificaciones.** La notificación no sale —y la app hoy solo pide ese permiso cuando hay una
   actualización disponible—, pero el aviso de RF-120 **sí**. Por eso el aviso no es opcional: es el canal que
   funciona siempre.
4. **`OutOfMemoryError` u otro fallo del propio registro.** Si escribir el informe revienta, se delega igual: la
   app se cierra como se cerraba y no hay un crash del crash.
5. **Traza infinitamente larga** —`StackOverflowError` repite el mismo marco miles de veces—. El informe se
   **recorta** a un máximo de líneas y **dice que está recortado**: un informe enorme no se puede copiar ni
   mandar por URL.
6. **El mensaje de la excepción trae saltos de línea, acentos o `&`.** El cuerpo va **codificado por
   parámetro**, nunca concatenado: si no, la URL se parte y la incidencia sale vacía.
7. **No hay ningún navegador instalado** (o el usuario lo bloqueó). El fallo se captura y se muestra un aviso
   corto; el informe **sigue en pantalla** y se puede copiar igual.
8. **La notificación se pulsa después de consumir el informe** —se quedó en el cajón—. La pantalla explica que no
   hay nada y ofrece cerrar. No revienta.
9. **Bóveda bloqueada.** El aviso aparece igualmente y el informe se abre **sin** contraseña, porque no hay nada
   que proteger. La `Activity` del informe **no** toca la base de datos ni el `NotesViewModel`.
10. **Fallo en un hilo secundario** —el sincronizador, un `WorkManager`, el modelo de IA—. Se registra igual,
    con el nombre del hilo, y la app sigue muriéndose como antes.
11. **Dos fallos en el mismo arranque.** Gana el último; el aviso aparece una vez.

## Fuera de alcance

- **Historial de fallos.** Se guarda **solo el último** (decisión de quien decide). Una lista sería otra pantalla,
  otra rotación y otro orden de borrado.
- **ANR y cierres del sistema.** No son excepciones y no hay forma honesta de registrarlos sin un watchdog que
  también puede morir.
- **Crashlytics, ACRA o cualquier SDK de reporte.** Añade una dependencia, una clave y una clave más que
  filtrar (RNF-63).
- **Envío automático del informe.** Ni a un servidor propio ni a GitHub: la constitución 5 manda y el usuario
  manda.
- **Entrada permanente en Ajustes** para ver fallos antiguos. El aviso y la notificación son las dos puertas;
  una tercera sería especulativa (`YAGNI`).
- **Cambios en el gestor de actualizaciones**, aunque también publica notificaciones: su canal y su permiso ya
  existen y no se tocan.

## Criterios de finalización

- [x] Pruebas puras nuevas en verde **sin** `RobolectricTestRunner` (RNF-64) y sin fallos nuevos en `:app`.
- [x] El manejador se instala en `onCreate`, guarda el anterior y delega siempre (RF-117).
- [x] El informe no importa nada de `data.local` ni ningún cliente de red (RF-118, RNF-67).
- [x] La URL de la incidencia sobrevive a un informe con saltos de línea, acentos y `&` (RF-121).
- [x] La traza se recorta y **dice** que está recortada (caso límite 5).
- [x] La pantalla del informe tiene los tres botones, el aviso de privacidad y el estado vacío (RF-121, RF-123).
- [x] La notificación abre la pantalla del informe y desaparece al consumirla (RF-119, RF-122).
- [x] `filesDir` está excluido de los dos XML de respaldo (RNF-66).
- [x] Las nueve carpetas de `strings.xml` tienen todas las claves nuevas (RNF-68).
- [x] `:app:assembleDebug` sin errores y la suite de `:app` sin fallos nuevos.
- [x] `MEMORY.md` actualizado, **incluido que nada de esto se ha visto en un dispositivo** (RNF-70).

### Medido, no supuesto

- **43 pruebas nuevas, todas verdes y todas sin Robolectric**: 25 puras (`CrashReportTextTest` 8,
  `CrashIssueUrlTest` 7, `CrashStoreTest` 10) y 18 invariantes (`CrashInvariantsTest`).
- **`:app` 711 / 41** contra la base **668 / 41**. Los 41 son **los mismos 41**: 38 de plataforma
  (`DefaultNativeRuntimeLoader`, sin runtime nativo para `linux/aarch64`) y los 3 ajenos de siempre
  (`HtmlExportMediaTest` y los dos de `OnDeviceModelSourceTest`). Los +43 son exactamente las pruebas nuevas.
- **Cero dependencias nuevas (RNF-63)**, y aquí no hay ni un número que comparar: **`git diff` no toca
  `build.gradle.kts`, `libs.versions.toml` ni `oss-licenses.json`**, que es una prueba por construcción —un
  árbol de dependencias no puede cambiar si no se ha declarado nada—. El recuento de coordenadas del árbol
  sale en 645 con el método de esta sesión, **no comparable** con el 630 que dejaron las specs 018 y 019,
  porque el método de conteo no era el mismo.
- **Dos fallos que cometió la propia implementación y los pilló un invariante**, no el compilador:
  1. «Ver informe» **consumía el fichero antes de abrir la pantalla**, así que la `Activity` lo habría leído
     vacío. In consumía antes de leer; ahora consume lo que **se lleva** el informe.
  2. La prueba de apóstrofos marcaba rojo `d\'erreur`, que **es** la forma correcta: buscaba `'` sin mirar
     la barra de delante. Un invariante demasiado burdo es ruido, y ruido se acaba silenciando.
- **Un invariante que escribí mal y el código tenía razón**: «`onCreate` no debe crear el canal». Sí debe,
  y además **antes** de notificar, porque `notify()` sobre un canal inexistente es un no-op silencioso.
  El invariante ahora mide el **orden**, que es lo que importa.
- **No hay nada medido del tamaño del APK**: sin reconstruir la base, comparar sería inventar.

> **Lo que falta, y solo se puede cerrar en un dispositivo, en este orden:** (1) provocar un fallo real —un
> `throw` en un `onClick` sirve— y comprobar que **la app se cierra igual que antes**, sin diálogo del
> sistema; (2) que al reabrir **salga la notificación** (con el permiso concedido) y que abra la pantalla;
> (3) que sin permiso **salga el aviso dentro de la app**, y también con la bóveda bloqueada; (4) que «Enviar»
> abra GitHub **con el cuerpo relleno** —lo único que de verdad no puede comprobar una prueba pura—; (5) que
> copiar y compartir funcionen, y que tras ellos el aviso **no** vuelva a salir.

## Dudas abiertas

Ninguna. Las cuatro decisiones que podían tomarse por dos caminos —dónde vive la pantalla, cuántos fallos se
guardan, qué abre «Enviar» y qué pasa sin permiso de notificaciones— las respondió la persona usuaria antes de
redactar:

| Decisión | Respuesta |
|---|---|
| ¿`Activity` propia o pantalla del Navigator? | **`Activity` propia.** Así la abre la notificación sin depender de `MainActivity`, del `Navigator` ni del estado de la bóveda. |
| ¿Historial o solo el último? | **Solo el último.** |
| ¿Qué abre «Enviar»? | **Incidencia nueva de GitHub con el informe precargado.** |
| ¿Y sin permiso de notificaciones? | **Aviso dentro de la app además de la notificación.** |