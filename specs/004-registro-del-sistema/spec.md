# Spec 004 — Registro del sistema

Estado: **implementada** (2026-10-04) — T1-T7 cerradas; queda la puerta `./gradlew test` en un host x86_64

> **Nota de procedencia.** Esta spec es una hija de `specs/001-limites-constitucion/spec.md`
> (**aprobada**), que es la spec madre y sigue siendo la ley. No la sustituye ni la contradice.
> Hereda sin modificar: **RF-5, RF-6, RF-7, RF-8, RF-9, RF-10, RF-11, RF-12, RF-13, RF-14** y
> **RF-20**, más las RNF que les aplican. Los identificadores `RF-x` son los de la madre; aquí no
> se renumeran. Su plan y sus tareas están en `plan.md` y `tasks.md` de esta misma carpeta.
>
> **RF-20 viaja aquí** — condensing la memoria del proyecto — porque su contenido depende del
> estado final de esta spec y de las otras dos: escribarla antes significaría describir un estado
> que después habría que corregir.

## Contexto y objetivo

El principio 5 de la constitución exige que los datos del usuario no salgan del dispositivo y que
no se vuelque contenido a logs. Hoy el registro del sistema del dispositivo imprime valores que
escribieron o importaron las personas usuarias.

Medido sobre los cuatro módulos del proyecto con un parser que tolera llamadas repartidas en
varias líneas, hay **143 llamadas** al registro del sistema, de las que **35 interpolan** algún
valor y **114 reciben una excepción como último argumento** cuyo texto el registro imprime íntegro.
De esas, **23 sitios** exponen contenido de nota o referencia de medio: **16 en el módulo principal
y 7 en los módulos librería**. La lista exacta, con el motivo de cada entrada, está en `plan.md`
§4.1, que es donde la spec madre dejó la decisión.

Cada sitio se resuelve de una de dos formas, y la elección depende de por dónde llega la fuga:

- Cuando la fuga llega por **interpolación** —la llamada imprime la fórmula, la ruta, el
  identificador de recurso o la dirección web del medio— el mensaje de reemplazo lleva la
  **categoría del elemento afectado y un identificador opaco**.
- Cuando la fuga llega por el **objeto de excepción** —el texto de la excepción incluye un fragmento
  de la nota o la ruta del archivo— el mensaje de reemplazo lleva el **tipo de la excepción y el
  mensaje propio de la aplicación**, y nunca el objeto.

24 sitios más se han revisado y **se conservan** sin cambios: imprimen dato no sensible (rutas
internas, códigos HTTP, sumas de comprobación, métricas del dispositivo, nombres internos de
preferencia). La lista está en `plan.md` §4.2 y es lo que hace comprobable el criterio de que el
diff no los toque.

La corrección **no puede consistir en borrar el log**: cada fallo sigue teniendo que quedar
registrado y diagnosticable.

## Usuarios

- **Persona usuera de la aplicación.** Escribe y guarda material privado en sus notas. No controla
  el registro del sistema del dispositivo ni el código, así que no puede corregir lo que se imprime
  en él.
- **Persona desarrolladora o mantenedora.** Depende de que el fallo siga siendo diagnosticable
  después de quitar el contenido del mensaje.

## Historias de usuario

- HU-1. Como persona usuera, quiero que nada de lo que escribo o importo en mis notas aparezca en
  el registro del sistema del dispositivo, para que mi material no quede expuesto en el equipo
  fuera de mi control.
- HU-5. Como persona desarrolladora, quiero que el fallo siga siendo diagnosticable después de quitar
  el contenido del mensaje, para no perder capacidad de depuración.

## Definiciones

- **Alcance de la revisión.** Los cuatro módulos del proyecto: el módulo principal de la aplicación
  y los tres módulos librería. Toda búsqueda y toda revisión recorre los cuatro.
- **Sitios del alcance.** Los 23 puntos del alcance de la revisión que esta entrega modifica, según
  enumera `plan.md` §4.1. Un requisito que dice «los sitios del alcance» no obliga a nada fuera de
  esa lista.
- **Contenido de nota.** Valor escrito o importado por la persona usuaria y persistido en una nota:
  texto, título, marcado y fórmula.
- **Referencia de medio.** Ruta, identificador de recurso o dirección web que apunta a un archivo
  importado, incrustado o adjuntado en una nota. Se considera contenido de nota porque revela qué
  material se guarda y, si es una dirección web, puede apuntar a un recurso interno o privado.
- **Criterio de clasificación.** Una sola pregunta decide si un valor puede imprimirse en el
  registro del sistema: ¿el valor lo escribió o importó la persona usuaria, o lo genera la
  aplicación a partir de su propio estado? Si lo escribió o importó la persona, o describe un
  archivo suyo, es contenido de nota o referencia de medio y no se imprime. Si lo genera la
  aplicación, es dato no sensible y se imprime.
- **Dato no sensible.** Valor que genera la aplicación a partir de su propio estado: constantes,
  nombres internos de preferencias o de configuración, códigos de respuesta de un servicio, sumas de
  comprobación, métricas del dispositivo, identificadores de una versión publicada, rutas de los
  archivos que la propia aplicación descarga y guarda en su almacenamiento interno.
- **Identificador opaco.** Valor corto y opaco que la aplicación asigna a un elemento para poder
  correlacionar trazas sin revelar su contenido. No se deriva del contenido: no es un resumen del
  valor, no es reversible y no admite ataque por diccionario; dos contenidos distintos pueden
  compartir identificador. Su **ámbito** es la operación en curso: se genera una vez al empezar la
  operación y lo comparten todas las trazas de esa operación. En un sitio donde no hay operación a
  la que colgarlo —una llamada aislada sin contexto de proceso— el ámbito es la propia llamada.
- **Traza técnica.** Conjunto mínimo de información que permite localizar y clasificar un fallo sin
  exponer contenido de nota: la categoría del elemento afectado, su identificador opaco y el tipo de
  la excepción, junto con el mensaje propio de la aplicación. **No** incluye el objeto de excepción
  ni el texto de la excepción cuando ese texto puede contener contenido de nota o una referencia de
  medio.
- **Mensaje de estado.** Texto que genera un componente de la aplicación, no la persona usuaria, para
  describir el resultado de una operación. Puede ser el mismo texto que la persona ve en pantalla.

## Requisitos funcionales

Heredados de la spec madre, sin modificación:

- **RF-5**: EL SISTEMA no escribe en el registro del sistema ningún valor que sea contenido de nota
  ni referencia de medio.
- **RF-6**: SI se produce un fallo al procesar el contenido de un bloque, ENTONCES EL SISTEMA
  registra la categoría del bloque y su identificador opaco, y no el texto, el marcado ni la
  fórmula del bloque.
- **RF-7**: SI se produce un fallo al procesar, importar, copiar, borrar o descargar un medio
  referenciado por una nota, ENTONCES EL SISTEMA registra la categoría del medio y su identificador
  opaco, y no la ruta, el identificador de recurso ni la dirección web del medio, ni nada derivado
  de esos valores.
- **RF-8**: SI un sitio del alcance registra un objeto de excepción cuyo mensaje pueda contener
  contenido de nota o referencia de medio, ENTONCES EL SISTEMA no registra ese objeto completo. Los
  sitios fuera del alcance quedan pendientes de la spec por zona que los recoja.
- **RF-9**: SI se produce un fallo en un sitio del alcance, ENTONCES EL SISTEMA conserva la traza
  técnica y el fallo no queda sin registrar en ningún camino de ejecución de ese sitio.
- **RF-10**: EL SISTEMA no sustituye el contenido de nota por una constante sin campos. En cada sitio
  modificado, el mensaje escrito al registro contiene la categoría del elemento afectado y su
  identificador opaco, o el tipo de la excepción y el mensaje propio de la aplicación. Sustituir una
  interpolación por un texto fijo que solo dice qué clase de fallo ocurrió **incumple** este
  requisito, aunque cumpla RF-5, RF-8 y RF-11, y aunque el número de escrituras al registro no baje.
- **RF-11**: SI la causa asociada a un fallo lleva en su propio texto una ruta, una dirección web o
  un fragmento de la nota, ENTONCES EL SISTEMA registra el tipo de la excepción y el mensaje propio de
  la aplicación, y no el objeto de excepción completo.
- **RF-12**: SI se registra un mensaje de estado generado por un componente, ENTONCES EL SISTEMA lo
  mantiene siempre que el mensaje no incluya texto de nota ni una referencia de medio.
- **RF-13**: CUANDO se revisa el código de los sitios del alcance, EL SISTEMA no contiene ninguna
  llamada al registro del sistema que interpole contenido de nota o referencia de medio, ni ninguna
  que registre el objeto de una excepción cuyo mensaje pueda contenerlos; la revisión se apoya en dos
  búsquedas sobre el alcance de la revisión: una de interpolaciones dentro de llamadas al registro
  del sistema y otra de llamadas cuyo último argumento es un objeto de excepción.
- **RF-14**: MIENTRAS una llamada al registro del sistema solo interpole datos no sensibles y no
  reciba un objeto de excepción, EL SISTEMA la mantiene sin cambios. Este requisito no obliga a
  conservar el número de llamadas al registro del sistema ni su forma: una llamada puede fusionarse
  con otra o dejar de interpolar, siempre que el fallo siga registrado y no baje el número de
  escrituras del sitio, como exige RNF-2.

Y, viajando con esta spec:

- **RF-20**: AL terminar la entrega, EL SISTEMA deja la memoria del proyecto en un máximo de 50
  líneas y registra en ella el estado actual, las decisiones tomadas con su motivo y los errores a
  evitar, pasando al registro de decisiones lo que ya no aporte al estado actual y eliminando lo que
  sea redundante.

### La excepción de RF-19 aplicada aquí

RF-19 de la spec madre admite, **solo para la función pura que construye el mensaje de reemplazo de
un fallo**, los casos que prueben que ese mensaje contiene la categoría y el identificador opaco y
que el texto de la excepción no aparece en él. **Esa función la introduce esta spec**, así que la
excepción se usa aquí y en ningún otro sitio. No se añade ningún otro caso de prueba.

## Requisitos no funcionales

Heredados de la spec madre, sin modificación:

- **RNF-2 — Diagnóstico preservado en lo que se toca.** Ninguno de los bloques de captura de
  excepción modificados por esta entrega queda sin registrar. Verificable: el número de escrituras al
  registro del sistema de cada sitio modificado no baja del que tenía antes de la entrega, y el
  sitio sigue teniendo al menos una. Los bloques de captura que la entrega no modifica no quedan
  dentro del alcance: el proyecto tiene unos 205 en los cuatro módulos y una parte importante son
  rutas de reserva ante un fallo de análisis.
- **RNF-3 — Comportamiento inalterado.** El único efecto observable de la parte B sobre la
  ejecución de la aplicación es qué información aparece en el registro del sistema: no cambia el
  resultado de la exportación, del dibujado, del respaldo ni del widget. Verificable: las suites de
  esas zonas pasan y solo cambian las aserciones que la spec 002 exige cambiar.
- **RNF-4 — Verificabilidad.** Cada requisito funcional tiene una comprobación reproducible, un
  comando de una línea o un caso de prueba. Verificable: la sección de criterios de finalización no
  contiene ninguna comprobación que solo pueda hacer una persona mirando la pantalla.
- **RNF-5 — Un idioma por capa en lo que se toca.** Los identificadores, las API y los mensajes del
  registro del sistema que esta entrega añada o modifique están en inglés; los comentarios y la
  documentación, en español. Verificable: una búsqueda, en los sitios del alcance, de las palabras
  del español más frecuentes en mensajes de registro del sistema no devuelve ningún resultado en los
  sitios modificados.
- **RNF-6 — Sin coste nuevo.** La entrega no añade dependencias, tareas de compilación ni procesos
  de compilación nuevos. Verificable: el conjunto de dependencias solo disminuye, el catálogo de
  versiones pierde entradas y no las gana, y la lista de tareas de compilación no crece.

## Casos límite

1. **Medio sin referencia.** Un bloque cuyo medio está vacío o cuya referencia no se puede resolver
   produce igualmente un fallo registrado, y el mensaje no contiene la referencia ni nada derivado de
   ella.
2. **Dos notas con el mismo archivo.** Dos notas distintas que apuntan al mismo archivo pueden
   compartir identificador opaco dentro de una misma operación: el registro no permite saber a qué
   nota pertenece el material.
3. **Dos operaciones distintas.** Dos exportaciones del mismo documento dan identificadores opacos
   distintos, porque el ámbito del identificador es la operación.
4. **Sitio sin operación.** Una llamada aislada que no pertenece a ninguna operación genera su
   identificador opaco en la propia llamada, de modo que el identificador existe y el mensaje no es
   una constante sin campos.
5. **Fallo repetido en bucle.** Exportar un documento con diez imágenes inaccesibles deja diez trazas
   y ninguna dirección web.
6. **Dirección web interna o privada.** Una dirección de la red local que falla al descargarse no
   aparece completa ni parcialmente en el registro, y su identificador opaco no se deriva de ella.
7. **Excepción de plataforma con ruta en su texto.** Un fallo del sistema cuyo texto es «<archivo> no
   encontrado» se registra con el tipo de la excepción y el mensaje propio de la aplicación, sin el
   objeto de excepción.
8. **Excepción cuyo mensaje reproduce parte del contenido.** Un fallo de análisis de una estructura
   serializada cuyo mensaje incluye un fragmento de la cadena analizada, que en este proyecto es el
   contenido de la nota, se registra sin ese fragmento.
9. **Texto visible dependiente del idioma.** No aplica a esta spec: no se toca ningún texto
   visible.

## Fuera de alcance

- **Las categorías de excepción pospuestas.** De las 114 llamadas que pasan una excepción como
  último argumento, solo 20 están en los sitios del alcance. Las que pertenecen al cifrado y
  descifrado de notas, a la sincronización con el servicio de copias de seguridad, a la reproducción
  de medios en general y al acceso a archivos externos quedan pendientes, con la spec por zona que
  las recoja. No están olvidadas: revisar cada una exige decidir si su texto de excepción puede
  llegar a contener contenido de nota, y esa decisión pertenece a una spec por zona.
- **Los 24 sitios que solo imprimen dato no sensible.** Se conservan intactos (RF-14) y su lista
  está en `plan.md` §4.2.
- **Los mensajes de estado de los componentes.** Se mantienen (RF-12): el del motor de IA es el
  caso representativo, porque es necesario para diagnosticar la carga del modelo.
- **Exigir registro en todos los bloques de captura de excepción del proyecto.** RNF-2 queda acotado
  a lo que esta entrega modifica.
- **Revisar todos los mensajes del proyecto en español.** RNF-5 queda acotado a lo que esta entrega
  modifica; los que ya estaban en español y no se tocan se quedan.
- **Cambios de comportamiento** en el editor de bloques, en el motor de IA, en la sincronización, en
  el respaldo o en el cifrado.
- **Retirar el registro de métricas de rendimiento en compilaciones de depuración**: no contiene
  contenido de nota y es la herramienta de verificación de las reglas de rendimiento.
- **El fallo conocido de representación de tablas y los defectos de selección de texto de los
  bloques editables.** Requieren su propia spec.
- **La verificación visual en dispositivo.**
- **Las dependencias y los inventarios** (spec 003) y **el desbloqueo de los tests** (spec 002).

## Criterios de finalización

- [ ] Una búsqueda, sobre el alcance de la revisión, de llamadas al registro del sistema que
      interpolen contenido de nota o referencia de medio no devuelve ningún resultado en los sitios
      del alcance.
- [ ] Una búsqueda, sobre el alcance de la revisión, de llamadas al registro del sistema cuyo último
      argumento sea un objeto de excepción no devuelve ningún resultado en los sitios del alcance.
- [ ] La entrega adjunta la salida de la búsqueda de interpolaciones sobre el alcance de la revisión,
      antes y después de los cambios, y el recuento de llamadas que interpolan puede bajar en los
      sitios modificados, sin que eso sea un incumplimiento por sí solo.
- [ ] La entrega adjunta la lista explícita de las llamadas al registro del sistema que se conservan
      sin cambios, y el diff de la entrega no toca ninguna de ellas.
- [ ] En cada sitio modificado, el mensaje escrito al registro contiene la categoría del elemento
      afectado y su identificador opaco, o el tipo de la excepción y el mensaje propio de la
      aplicación: cada mensaje de reemplazo declara al menos un campo interpolado y ninguno es una
      constante sin campos.
- [ ] El número de escrituras al registro del sistema de cada sitio modificado no baja del que tenía
      antes de la entrega.
- [ ] `./gradlew test` en verde, con las suites de exportación, dibujado, respaldo y widget en verde.
- [ ] Ningún caso de prueba se ha eliminado, desactivado ni saltado. Los únicos casos añadidos son los
      de la función pura que construye el mensaje de reemplazo de un fallo, y cubren que ese mensaje
      contiene la categoría y el identificador opaco y que el texto de la excepción no aparece en él.
- [ ] La memoria del proyecto tiene 50 líneas o menos, y registra el estado actual, las decisiones con
      su motivo y los errores a evitar.

## Dudas abiertas

Ninguna.