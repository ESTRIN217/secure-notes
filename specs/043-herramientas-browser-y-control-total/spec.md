# Spec 043 — Herramientas de fecha/hora e información y control total de herramientas

Estado: **implementada y verificada en dispositivo** (2026-11-22)

## Contexto y objetivo

La spec 042 añadió a la hoja de adjuntos del chat una superficie de herramientas con un control por
herramienta. Al compararla con la captura de la app de referencia (L Chat) faltan dos piezas:

- Un **checkbox maestro** que permite desactivar todas las herramientas de una sola vez, con un
  contador del tipo `3/5` activas.
- Las dos herramientas del grupo *Browser* que la referencia ofrece: **`get_datetime`** y
  **`get_info`**.

El checkbox maestro es **global**: abarca el conjunto completo de herramientas (las tres de notas
existentes y las dos nuevas), no por grupos. Se decide así para que el usuario tenga un único
interruptor de «apagar todo» y un único número que diga cuántas están activas, sin tener que
entender cómo se agrupan las herramientas. No se implementan grupos.

Las dos herramientas nuevas se comportan como cualquier otra: se anuncian al modelo solo donde hoy
se anuncian las demás, y se filtran por el mismo conjunto de deshabilitadas. `get_datetime` copia la
forma de llama.cpp (hora local del usuario, no del servidor). `get_info` adapta la forma del
*fallback* de navegador de llama.cpp al entorno Android, sin cwd ni identificadores del dispositivo.

## Usuarios

- Usuario del chat que quiere apagar o encender todas las herramientas de una vez y ver de un
  vistazo cuántas siguen activas.
- Usuario del chat que quiere que el asistente sepa la fecha/hora local y el sistema sobre el que
  corre, igual que en la app de referencia.
- Usuario que restaura una copia y espera que su selección global de herramientas siga siendo la
  misma (comportamiento ya garantizado por la spec 042, que esta spec no cambia).

## Historias de usuario

- HU-1. Como usuario del chat, quiero un checkbox que desactive todas las herramientas a la vez para
  no tener que ir una por una.
- HU-2. Como usuario del chat, quiero ver un contador `n/m` para saber cuántas herramientas están
  activas sin contarlas.
- HU-3. Como usuario del chat, quiero que el asistente pueda consultar la fecha y hora locales para
  responder preguntas temporales con el reloj correcto.
- HU-4. Como usuario del chat, quiero que el asistente pueda consultar información básica del
  entorno sin que por eso se expongan datos del dispositivo ni de mis notas.

## Definiciones

- **Herramienta**: función registrada que el modelo puede invocar por su nombre. Tras esta spec son
  **cinco**: `search_notes`, `get_note`, `create_note`, `get_datetime` y `get_info`. Su nombre visible
  sigue siendo el identificador, como dato (decisión D6 de la spec 042).
- **Conjunto de deshabilitadas**: nombres de herramientas que no se anuncian al modelo; viaja en la
  copia y persiste entre sesiones (spec 042, RNF-194). Esta spec no cambia su formato ni su viaje.
- **Checkbox maestro / control global**: único control de la cabecera del submenú que actúa sobre
  **todas** las herramientas registradas a la vez. No existen controles por grupo.
- **Activas**: herramientas registradas cuyo nombre no está en el conjunto de deshabilitadas. El
  contador `n/m` es `n` activas sobre `m` registradas.
- **`get_datetime`**: herramienta sin parámetros que devuelve la hora local del usuario en formato
  ISO-8601 con desplazamiento y el nombre de zona IANA, como en llama.cpp.
- **`get_info`**: herramienta sin parámetros que devuelve un objeto con una nota fija y una cadena
  de sistema operativo abreviada, adaptada del *fallback* de navegador de llama.cpp.

## Enmienda a la spec 042

La spec 042 queda enmendada en dos puntos, sin cambiar nada más de ella:

- **Fuera de alcance**: donde decía «Herramientas nuevas o cambiar las tres existentes; tampoco
  cambiar a qué backend se anuncian herramientas», pasa a decir «**Cambiar las tres herramientas de
  notas existentes**; tampoco cambiar a qué backend se anuncian herramientas». Se permiten, por tanto,
  **dos herramientas nuevas** (`get_datetime` y `get_info`) y se conservan las tres de notas sin
  cambios. El resto del fuera de alcance de la 042 (backend, prompt en la copia, Ajustes de IA,
  comandos slash) sigue en pie.
- **Invariante de «tres herramientas» a «cinco»**: la definición de la 042 que decía «hoy: buscar
  notas, obtener nota, crear nota» pasa a «hoy: buscar notas, obtener nota, crear nota, fecha y hora,
  información del entorno». Toda comprobación que asumiera tres herramientas pasa a asumir **cinco**;
  el maestro y el contador cubren las cinco.

Los RF-300…RF-307 y RNF-190…RNF-196 de la 042 siguen vigentes. En particular el filtrado por nombre
(RF-306), la tolerancia a nombres muertos (RF-307), el viaje del conjunto en la copia (RNF-194) y el
hecho de que las herramientas solo se anuncien con Ollama no se tocan.

## Enmienda (RF-313 / RNF-204) — toque individual

La spec 043 ya estaba implementada cuando se detectó un defecto en la fila de cada herramienta:
`ToolsRow` tenía **dos** manejadores de pulsación sobre la misma fila —un `.clickable` en el `Row` y el
`onCheckedChange` del `Checkbox`—, de modo que un solo toque podía producir **dos toggles** y devolver
la herramienta al estado en que ya estaba. La enmienda elimina el `.clickable` y deja el `Checkbox`
como único punto de interacción (decisión D12 del plan), y añade un invariante de fuentes que impide
reintroducir el doble handler.

Por qué no cambia RF-308…RF-312: el checkbox maestro, el contador, las dos herramientas nuevas, su
wire format y el registro de cinco quedan exactamente igual. El defecto está en el cableado de la fila
individual, no en ninguna de las decisiones ya tomadas; el toggle individual de la 042 (RF-305) sigue
siendo «desactivar añade / activar quita», solo que ahora se dispara una vez por toque. El estado de la
spec sigue siendo `implementada`.

## Requisitos funcionales

- **RF-308**: CUANDO se abre el submenú de herramientas con al menos una registrada, EL SISTEMA
  muestra una cabecera con un **checkbox maestro global** y un **contador `n/m`** de herramientas
  activas sobre registradas. El checkbox aparece marcado solo cuando **todas** las registradas están
  activas. Verificable: con las cinco herramientas activas el contador lee `5/5` y el checkbox está
  marcado; con una deshabilitada lee `4/5` y no está marcado.
- **RF-309**: CUANDO se pulsa el checkbox maestro, EL SISTEMA deshabilita **todas** las herramientas
  registradas si estaba marcado y las **habilita todas** si no lo estaba, y persiste el cambio en el
  acto sin paso de guardado. SI no hay ninguna herramienta registrada, ENTONCES EL SISTEMA muestra el
  checkbox deshabilitado y la pulsación no cambia nada. Verificable: tras «desactivar todas» el
  contador lee `0/5` y ninguna se anuncia; tras «activar todas» lee `5/5` y vuelven a anunciarse. El
  cambio rige a partir de la siguiente petición; la que esté en vuelo no se toca.
- **RF-310**: CUANDO el modelo invoca `get_datetime`, EL SISTEMA devuelve un objeto con la hora
  **local del dispositivo** en ISO-8601 (`AAAA-MM-DDTHH:mm:ss±HH:MM`, con signo incluso a desplazamiento
  cero) y el **nombre de zona IANA** del dispositivo. Verificable: el desplazamiento corresponde a la
  zona y a la fecha del instante (incluido horario de verano), y el resultado no depende de la hora
  del servidor.
- **RF-311**: CUANDO el modelo invoca `get_info`, EL SISTEMA devuelve un objeto con una **nota fija** que
  explica que la app solo ofrece herramientas de notas, sin acceso a intérprete de comandos ni a
  sistema de ficheros del host, y una cadena `os` con la forma `Android <versión> (API <nivel>)`.
  El objeto **no contiene** directorio de trabajo, marca, modelo, IMEI ni ningún otro identificador.
- **RF-312**: EL SISTEMA mantiene exactamente **cinco** herramientas registradas — las tres de notas y
  `get_datetime` y `get_info` — y todas ellas quedan sujetas al mismo conjunto global de
  deshabilitadas y a la misma regla de anuncio que las de la spec 042. El maestro y el contador las
  cubren a las cinco; el backend al que se anuncian no cambia.
- **RF-313**: CUANDO se pulsa la casilla de una herramienta, EL SISTEMA alterna el estado de **esa sola**
  herramienta y produce exactamente **un** toggle por toque; la fila no tiene ningún otro manejador de
  pulsación que pueda producir un segundo toggle. Verificable: el cuerpo de `ToolsRow` contiene
  exactamente una llamada a `onToggle(name)` y ningún `.clickable`, comprobado por
  `ToolsSheetInvariantsTest` (RNF-204).

## Requisitos no funcionales

- **RNF-197**: Sin dependencias nuevas (constitución, principio 1). Las facilities de fecha y hora que
  ya trae la plataforma bastan; no se añade una librería de serialización para construir los dos
  objetos de respuesta.
- **RNF-198**: Las decisiones — estado y cambio del maestro, contador y formato de las dos respuestas —
  viven en piezas **puras**, sin dependencias del sistema operativo ni de recursos, con pruebas JVM
  sin Robolectric. La obtención del instante, la zona, los valores de versión y el texto localizado
  se inyectan como parámetros y solo se recogen en el código que llama (regla permanente: lo que no
  necesita `Context` no lo toca).
- **RNF-199**: Todo texto visible nuevo sale de recursos en los **9 locales de una vez**:
  `tools_all`, `tools_enabled_count` (formato `n/m`) y `get_info_note` (nota fija de `get_info`). Los
  apóstrofes van escapados (`\'`). Los nombres de herramienta siguen siendo dato, no recurso (D6 de la
  042). La nota de `get_info` es recurso y no dato de protocolo: a diferencia de las descripciones de
  las herramientas, que son instrucciones para el modelo, la nota forma parte de la respuesta que el
  usuario lee en el chat, así que es texto visible y cae bajo el principio 6 de la constitución.
- **RNF-200**: El submenú sigue siendo una pieza «tonta»: recibe datos y callbacks; cero lógica de
  envío y cero escritura de preferencias dentro de un Composable; funciones ≤ 20 líneas con guard
  clauses (constitución, principio 3).
- **RNF-201**: El flujo de envío no cambia: `AiRequest`, el momento del anuncio, el hecho de que solo
  se anuncie con Ollama y el viaje del conjunto en la copia quedan como en la 042. Con backend
  on-device las herramientas siguen sin anunciarse; las dos nuevas están registradas pero no se
  envían, igual que las tres de notas. Sin escrituras enRoom ni migraciones.
- **RNF-202**: Privacidad: `get_datetime` solo revela hora y zona del dispositivo; `get_info` solo
  revela versión de Android y el texto fijo, sin identificadores ni rutas; ninguna de las dos lee
  contenido de notas. Ningún dato de nota nuevo sale del dispositivo (constitución, principio 5).
- **RNF-203**: Verificación: `./gradlew :app:compileDebugUnitTestKotlin` sin errores y los tests JVM
  dirigidos nuevos en verde; los invariantes de recuento (cinco registros, 9/9 claves de recursos)
  pasan y ningún literal aparece dentro de `Text(`. `./gradlew test` mantiene aquí los 38 fallos de
  plataforma conocidos de `linux/aarch64`; la suite completa se verifica en verde en host `x86_64`.
- **RNF-204**: Invariante de fuentes: `ToolsRow` en `ToolsSheet.kt` tiene **exactamente un**
  `onToggle(name)` y **ningún** `.clickable`, de modo que un toque no pueda disparar dos toggles. Lo
  vigila `ToolsSheetInvariantsTest`, JUnit4 puro sin Robolectric, que lee la fuente Kotlin igual que
  `BackupSettingsInvariantsTest` y `EditorInvariantsTest`; el recorrido quita primero comentarios y
  textos para que un KDoc que nombra `.clickable` no lo haga pasar por código.

## Casos límite

- **Todas deshabilitadas** → contador `0/5`, checkbox maestro sin marcar y petición sin herramientas;
  ninguna se ejecuta porque ninguna se anunció (RF-306 intacto, RF-309).
- **Ninguna herramienta registrada** → se muestra el mensaje de lista vacía de la 042 (RF-304), el
  checkbox maestro aparece deshabilitado y el contador lee `0/0`; no se puede «activar todas» (RF-309).
- **Nombre muerto al activar todas** → «activar todas» deja el conjunto de deshabilitadas vacío, con
  lo que un nombre que ya no corresponde a ninguna herramienta (catálogo cambiado o copia de otra
  versión) desaparece sin efecto; sigue siendo inofensivo según RF-307 (RF-309, RF-312).
- **Zona con desplazamiento negativo** (p. ej. `America/New_York` en verano) → `result` termina en
  `-04:00`; el signo forma parte del formato, no se asume `+` (RF-310).
- **Media noche y cambio de año** → el formato respeta el instante y la zona: `00:00:00` y el día/mes/año
  correctos, sin desbordes, y con el desplazamiento vigente en esa fecha (RF-310).
- **Petición en vuelo** → un cambio del maestro no la toca; rige a partir de la siguiente, igual que el
  toggle individual de la 042 (RF-309, RF-306).
- **Backend on-device** → las dos herramientas nuevas están registradas pero no se anuncian; el
  comportamiento de la 042 intacto; el maestro y el contador siguen reflejando el estado global (RF-312).
- **`get_info` sin directorio de trabajo** → no se inventa una ruta ni se rellena con un valor del
  dispositivo; el objeto lleva solo `note` y `os` (RF-311).
- **Copia antigua sin el ajuste** → conjunto vacío, cinco herramientas activas, contador `5/5` y
  checkbox marcado, como el resto de ajustes de la 042 (RNF-194 intacta).

## Fuera de alcance

- Cambiar las tres herramientas de notas existentes (`search_notes`, `get_note`, `create_note`): sus
  nombres, descripciones, parámetros y comportamiento siguen igual.
- Cambiar a qué backend se anuncian las herramientas: solo Ollama, como hoy (spec 042).
- Agrupar herramientas por categorías (Browser, Notes…) ni mostrar grupos en el submenú: el control es
  global por decisión del usuario.
- Implementar las otras herramientas del grupo Browser de llama.cpp (p. ej. navegación o búsqueda en
  web): solo entran `get_datetime` y `get_info`.
- Dar a `get_info` acceso a un intérprete de comandos, sistema de ficheros, directorio de trabajo,
  red o identificadores del dispositivo; ni replicar el `cwd` del `get_info` del servidor de llama.cpp.
- Cambiar el formato del conjunto de deshabilitadas, su clave de preferencia ni su viaje en la copia
  (spec 042, RNF-194).
- Modificar el prompt personalizado, su editor ni la hoja de adjuntos de la 042.

## Criterios de finalización

- El submenú muestra la cabecera con checkbox maestro global y contador `n/m`, deshabilitado cuando no
  hay herramientas; desactivar/activar todas persiste en el acto y actualiza el anuncio desde la
  siguiente petición.
- El registro contiene cinco herramientas; `get_datetime` devuelve ISO-8601 local con desplazamiento y
  zona IANA, y `get_info` devuelve `note` + `os` sin cwd ni identificadores.
- Las dos nuevas se filtran por el mismo conjunto global y no se anuncian con on-device, igual que las
  de la 042.
- Recursos en los 9 locales; tests JVM dirigidos en verde; invariantes de recuento y greps pasan;
  `MEMORY.md` actualizado.
- Un toque en la casilla de una herramienta produce exactamente un toggle (RF-313) y el invariante de
  fuentes `ToolsSheetInvariantsTest` lo vigila (RNF-204); el maestro y el contador siguen igual.

## Dudas abiertas

- Ninguna: el alcance global del checkbox, la forma JSON de las dos herramientas y el uso de recurso
  para la nota de `get_info` están decididos y aprobados; las justificaciones viven en `plan.md`.
