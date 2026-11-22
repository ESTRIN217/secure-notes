# Spec 038 — Chats vacíos se descartan

Fecha: 2026-11-16
Estado: **implementada y verificada en dispositivo** (2026-11-22)
RF-273…RF-276 · RNF-165…RNF-169 · HU-1 · HU-2

## Contexto y objetivo

Cada sesión del chat se inserta en Room al crearla (vacía, sin turnos). No hay ninguna purga ni
al salir del chat ni al cerrar la app: el historial acumula «New Chat» vacíos que no contienen nada,
el mismo problema que la spec 032 resolvió para la nota nueva vacía.

Objetivo: descartar de forma silenciosa y definitiva los chats vacíos, sin tocar las sesiones
históricas ni nada que el usuario haya tocado — como la spec 032 hace con la nota vacía.

## Usuarios

Quien usa el chat de IA: alguien cuya lista de historial queda inundada de sesiones vacías
que no recuerda ni necesita.

## Historias de usuario

- **HU-1.** Como usuario, quiero que al volver atrás de un chat vacío este desaparezca, para no
  ver en el historial cascarones que no he usado.
- **HU-2.** Como usuario, quiero que al cerrar la app no queden en el disco los chats vacíos creados
  y abandonados en esa sesión, sin que las conversaciones históricas se pierdan.

## Definiciones

- **Vacío**: la sesión no tiene ningún turno (ninguna fila en `conversations` con su `sessionId`),
  en memoria o en base de datos.
- **Draft con contenido**: hay texto tecleado en el campo (o algo en edición) o adjuntos pendientes
  de enviar — el chat tiene contenido potencial aunque no haya turnos todavía.
- **Proceso actual**: sesiones cuya fila fue insertada en disco mientras la app está abierta en
  este proceso. Las sesiones antiguas (de procesos anteriores) nunca se tocan.
- **Purge**: borrado definitivo de la sesión y sus conversaciones, con el mismo borrado que un
  «Eliminar» manual del historial.

## Requisitos funcionales

- **RF-273**: CUANDO el usuario sale del chat por el control de atrás y la sesión abierta **no tiene
  turnos** y **no hay draft con contenido** (texto tecleado ni adjuntos pendientes), EL SISTEMA
  borra la sesión y navega atrás, silenciosamente, como si nunca hubiera existido.
- **RF-274**: MIENTRAS la actividad raíz se destruye y **no** es un cambio de configuración
  (rotación, idioma), EL SISTEMA purga todas las sesiones creadas en el proceso actual que sigan
  vacías (`messageCount == 0` y `createdAt >= inicioDelProceso`). La rotación nunca dispara el
  descarte ni la purga. Esta vía no observa el campo (la composición ya no existe): solo borra
  sesiones sin un solo turno, así que no pierde contenido enviable — el guard de draft (RF-275)
  vive en el camino de salida (RF-273).
- **RF-275**: SI hay texto tecleado en el campo, texto en edición, o adjuntos pendientes, ENTONCES
  la sesión no se descarta aunque no tenga turnos.
- **RF-276**: EL SISTEMA nunca borra ni modifica sesiones ajenas al proceso actual; las históricas
  permanecen intactas aunque estén vacías, «abiertas o no».

## Requisitos no funcionales

- **RNF-165**: La regla «¿descartar?» vive en una función pura sin `android` con tests JVM puros.
- **RNF-166**: El descarte es silencioso (sin diálogos ni toasts) y definitivo; no existe papelera
  ni deshacer — la sesión vacía no era contenido.
- **RNF-167**: El borrado usa la misma operación en cascada que un «Eliminar» manual
  (`conversations` + `chat_sessions`); no crea ni borra nada fuera.
- **RNF-168**: Ninguna consulta adicional dentro del flujo de render ni de la composición; el
  criterio se evalúa al salir, con los datos ya observados.
- **RNF-169**: Sin migración de Room ni cambios de esquema.

## Casos límite

- Chat abierto y el usuario pega/adjunta pero no envía: hay draft con contenido → no se descarta.
- Chat vacío y el usuario pulsa atrás: se borra (RF-273).
- Chat vacío, luego se escribe algo y se envía: hay turnos → sobrevive.
- Rotación de pantalla estando en el chat vacío: no se descarta (RF-274).
- Salida por deep-link/redirección a otra pantalla: el hook de «atrás» cubre el camino de salida;
  la purga del proceso cubre el cierre de la app con el chat vacío de fondo.
- Sesión creada, anotada por draft con contenido y app cerrada: la purga la respeta (RF-275).
- Drawer: «+» con 0 turnos y atrás inmediato → se descarta (RF-273) igualmente; es la misma regla.
- Sesión creada en un proceso anterior y abierta hoy para verla y cerrar sin escribir: se conserva
  (RF-276) aunque siga vacía.

## Fuera de alcance

- Notificación, toast, snackbar ni diálogo en el descarte (silencio estricto).
- Purgar históricos de procesos anteriores (nunca tocar lo ajeno al proceso actual).
- Distinguir saltar entre «salir» y «cerrar» a mitad de un turno: la purga del cierre (RF-274) es un borrado de sesiones creadas y jamás escritas, no una papelera.

## Criterios de finalización

`./gradlew :app:compileDebugKotlin` y los tests JVM puros del predicado en verde; verificación en
dispositivo: (1) abrir chat, atrás sin escribir → la sesión no aparece en el historial;
(2) con texto o adjuntos pendientes, atrás → conserva; (3) enviar un mensaje → conserva;
(4) crear chat vacío, cerrar la app por completo, reabrir → no aparece en el historial;
(5) rotar con el chat vacío abierto → conserva; `MEMORY.md` actualizado.

## Historial de cambios

- 2026-11-16 — Creación, análoga a la spec 032 (nota nueva vacía se descarta).
