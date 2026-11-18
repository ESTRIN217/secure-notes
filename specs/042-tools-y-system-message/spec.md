# Spec 042 — Hoja de adjuntos del chat: System Message y Tools

Estado: implementada

## Contexto y objetivo

La hoja modal de adjuntos del chat de la app de referencia (captura de L Chat) ofrece cuatro
opciones: Nota, Archivo, **System Message** y **Tools**. La nuestra (spec 006) se quedó en dos, y
hay dos ajustes del chat que hoy solo se alcanzan por otro camino o que no existen:

- El **prompt personalizado** (System Message) ya se puede guardar y ya se aplica a la siguiente
  petición, pero el único editor está en la pantalla de Ajustes de IA: cambiarlo a mitad de
  conversación obliga a salir del chat.
- Las **herramientas** del chat (buscar notas, obtener nota, crear nota) están todas registradas y
  **todas se anuncian al modelo en cada petición**: no existe el concepto de habilitar o
  deshabilitar una.

El objetivo es replicar el menú de la app de referencia: la hoja pasa de dos a cuatro opciones y
cada opción nueva abre su propia superficie. El flujo de envío del chat **no cambia**: el prompt
guardado se resuelve en el siguiente turno por el camino de siempre, y el recargo que provoca en el
motor on-device (releer los pesos cuando el prompt difiere del activo) se avisa, no se provoca al
guardar.

## Usuarios

- Usuario del chat con backend Ollama o on-device que quiere ajustar el comportamiento del
  asistente sin salir de la conversación.
- Usuario que restaura una copia de seguridad y espera recuperar la configuración de su chat.

## Historias de usuario

- HU-1. Como usuario del chat, quiero editar el System Message desde la hoja de adjuntos para
  cambiar cómo se comporta el asistente sin ir a los Ajustes.
- HU-2. Como usuario del chat, quiero elegir qué herramientas puede usar el asistente para que no
  llame a las que no quiero.
- HU-3. Como usuario que restaura una copia, quiero que mi selección de herramientas viaje en la
  copia para no rehacerla a mano.

## Definiciones

- **Herramienta**: función registrada en el registro de herramientas del chat que el modelo puede
  invocar por su nombre (hoy: buscar notas, obtener nota, crear nota). Su nombre visible es el
  identificador registrado, como dato — igual que los nombres de modelo (spec 033) —, no un
  recurso.
- **Deshabilitada**: herramienta que **no se anuncia** al modelo en las peticiones. Sigue
  registrada y puede reactivarse; no se borra nada.
- **System Message / prompt personalizado**: texto que, cuando no está en blanco, sustituye al
  prompt por defecto de la acción en la siguiente petición. Guardarlo en blanco devuelve el
  comportamiento al prompt por defecto.

## Enmienda a la spec 006

- **RF-34** queda enmendado: la hoja de adjuntos pasa de «exactamente dos opciones» a **exactamente
  cuatro** — Nota, Archivo, System Message y Tools, en ese orden — y nada más. RF-35 no cambia para
  Nota y Archivo, y su **orden** (cerrar la hoja antes de abrir lo siguiente) se extiende a las dos
  opciones nuevas.
- **Invariantes de verificación de la 006 actualizados por esta spec**:
  - `grep -c "SettingsListTile(" <hoja>` pasa de devolver **2** a devolver **4** (RF-300).
  - `grep -c "fun AttachmentListTile" <hoja>` sigue devolviendo **0**: el azulejo no se duplica.
  - `grep -c "ModalBottomSheet"` en el fichero de la hoja sigue devolviendo **1**: el submenú de
    Tools es otra superficie, no una segunda hoja dentro de la misma.
  - `grep -c '"text/\*", "application/json", "application/xml"'` en la pantalla de chat sigue
    devolviendo **1**: los tipos de archivo no cambian (RF-35 intacto).

## Requisitos funcionales

- **RF-300** (enmienda a RF-34): CUANDO se pulsa el botón «+» del campo de entrada, EL SISTEMA abre
  la hoja modal de adjuntos con **exactamente cuatro opciones** — «Nota», «Archivo», «System
  Message» y «Tools», en ese orden — y nada más; la hoja se puede descartar. Verificable: la hoja
  contiene cuatro azulejos de lista y ni una foto ni una cámara (invariantes de la enmienda).
- **RF-301**: CUANDO se elige «System Message», EL SISTEMA **cierra la hoja antes de abrir** el
  editor del prompt personalizado, y el editor se abre mostrando el prompt guardado actual (vacío
  si no hay). Verificable: el orden se lee en el sitio, como el de RF-35; el texto inicial es el
  valor guardado.
- **RF-302**: CUANDO se confirma el editor, EL SISTEMA persiste el prompt y **no envía ninguna
  petición**: el cambio se aplica a la siguiente petición por el camino de resolución de siempre.
  SI se confirma con el texto en blanco, ENTONCES EL SISTEMA borra el prompt personalizado y las
  siguientes peticiones vuelven al prompt por defecto de la acción. CUANDO se cancela o se
  descarta el editor, EL SISTEMA no cambia nada.
- **RF-303**: MIENTRAS el editor del prompt está abierto, EL SISTEMA muestra un aviso informativo
  de que el cambio se aplica al siguiente mensaje y de que, con el motor on-device, cambiar el
  prompt puede recargar el modelo al enviar (releer pesos).
- **RF-304**: CUANDO se elige «Tools», EL SISTEMA **cierra la hoja antes de abrir** el submenú de
  herramientas, que lista **todas las herramientas registradas**, cada una con su estado
  (habilitada/deshabilitada) y un control para cambiarlo. SI no hay ninguna herramienta registrada,
  ENTONCES EL SISTEMA muestra el submenú con un mensaje de lista vacía.
- **RF-305**: CUANDO se cambia el estado de una herramienta en el submenú, EL SISTEMA persiste el
  cambio en el acto, sin paso de guardado ni confirmación, y el submenú refleja el estado nuevo.
- **RF-306**: SI una herramienta está deshabilitada, ENTONCES EL SISTEMA **no incluye su
  especificación en ninguna petición al modelo** mientras siga deshabilitada, y la mantiene
  registrada: al reactivarla vuelve a anunciarse en la siguiente petición. Verificable: la lista
  de especificaciones enviadas no contiene su nombre; reactivada, vuelve a contenerlo.
- **RF-307**: EL SISTEMA trata un nombre deshabilitado que ya no corresponde a ninguna herramienta
  registrada como inofensivo: no produce error ni afecta a las demás. Esto cubre el caso de
  reactivar después de que el catálogo cambie y de restaurar una copia de otra versión.

## Requisitos no funcionales

- **RNF-190**: Sin dependencias nuevas (constitución, principio 1).
- **RNF-191**: Las decisiones — qué especificaciones se anuncian y cómo se guarda el conjunto de
  deshabilitadas — viven en piezas **puras** sin `android`/`androidx`, con pruebas JVM sin
  Robolectric (regla permanente: lo que no necesita `Context` no lo toca).
- **RNF-192**: Todo texto visible de la hoja, del editor y del submenú sale de recursos, en los
  **9 locales de una vez**; los apóstrofes van escapados (AAPT2 rechaza los crudos). Los nombres de
  herramienta se enseñan como dato, no como recurso.
- **RNF-193**: La hoja, el editor y el submenú son piezas «tontas»: reciben datos y callbacks;
  cero lógica de envío y cero escritura de preferencias dentro de un Composable; funciones
  ≤ 20 líneas con guard clauses (constitución, principio 3).
- **RNF-194**: El conjunto de herramientas deshabilitadas **viaja en la copia de seguridad**
  sumando **una línea** a la lista única de ajustes (convención de la spec 028), más su valor por
  defecto en la lista de defectos; la copia antigua que no lo trae restaura el conjunto vacío.
- **RNF-195**: El flujo de envío no cambia: el prompt se resuelve en el siguiente turno como hoy y
  no hay escrituras en Room ni migraciones.
- **RNF-196**: Verificación: `./gradlew :app:compileDebugUnitTestKotlin` sin errores y los tests
  JVM dirigidos nuevos en verde (`./gradlew test` mantiene aquí los 38 fallos de plataforma
  conocidos de `linux/aarch64`; la suite completa se verifica en verde en host `x86_64`). Los
  invariantes grep de la enmienda pasan, y ningún literal aparece dentro de `Text(`.

## Casos límite

- **Prompt confirmado en blanco** → se borra el prompt personalizado y vuelve el prompt por
  defecto de la acción (RF-302).
- **Todas las herramientas deshabilitadas** → la lista de especificaciones enviadas queda vacía y
  la petición sale sin herramientas; ninguna se ejecuta porque ninguna se anunció (RF-306).
- **Cambio mientras hay una petición en vuelo** → la petición en vuelo no se toca; el cambio rige
  a partir de la siguiente (RF-302, RF-306).
- **Prompt editado en Ajustes y en el chat** → es la misma preferencia: el editor del chat se abre
  con el valor actual y gana el último guardado; ninguno de los dos guarda por su cuenta al
  abrirse (RF-301).
- **Nombre deshabilitado sin herramienta** → inofensivo (RF-307); el filtrado por nombre no puede
  fallar por un nombre que ya no existe.
- **Submenú sin herramientas registradas** → mensaje de lista vacía, no un submenú en blanco
  (RF-304).
- **Copia antigua sin el ajuste** → conjunto vacío, todas habilitadas, como el resto de ajustes
  (RNF-194).
- **Descartar la hoja sin elegir nada** → no cambia prompt ni herramientas.

## Fuera de alcance

- Foto y cámara como adjuntos (decisión D1 de la 006, sigue en pie: el motor on-device es de texto).
- Herramientas nuevas o cambiar las tres existentes; tampoco cambiar a qué backend se anuncian
  herramientas (comportamiento actual intacto).
- Que el prompt personalizado viaje en la copia de seguridad (hoy no viaja; sumarlo es otro
  requisito y aquí solo entra el conjunto de herramientas, RNF-194).
- Reescribir la pantalla de Ajustes de IA ni su editor del prompt: quedan como están.
- Comandos «slash», subagentes o configuración por conversación: el ajuste es global.

## Criterios de finalización

- La hoja muestra las cuatro opciones en orden y las dos nuevas abren su superficie con el orden
  cerrar-antes-de-abrir; los invariantes grep de la enmienda pasan.
- Deshabilitar una herramienta la quita de las especificaciones enviadas y reactivarla la
  devuelve; el estado persiste entre sesiones y viaja en la copia.
- El editor guarda o descarta sin enviar nada y con el aviso del siguiente mensaje visible.
- Strings en los 9 locales; tests dirigidos en verde; `MEMORY.md` actualizado.

## Dudas abiertas

- Ninguna: las decisiones del submenú, del filtrado, de la copia y del aviso de recarga están
  tomadas y justificadas en `plan.md`.
