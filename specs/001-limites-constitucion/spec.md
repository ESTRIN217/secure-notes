# Spec 001 — Límites de la constitución

Estado: aprobada

## Contexto y objetivo

`docs/constitution.md` fija seis principios innegociables y, con ellos, una comprobación
para cada uno. Al redactarla aparecieron cuatro incumplimientos que hacen que la constitución
sea incoherente consigo misma: describe un proyecto que el repositorio todavía no es.

1. **Analítica declarada y sin usar.** Hay un SDK de analítica en la configuración de
   dependencias y anunciado en los inventarios de licencias de terceros, pero ninguna
   funcionalidad lo usa ni lo inicializa. El principio 5 prohíbe la telemetría y el
   principio 1 exige que nada quede declarado y sin uso. Mantenerlo contradice ambos y da una
   imagen falsa de qué hace la aplicación con la actividad de quien la usa. Al retirarlo, el
   catálogo de versiones queda además sin ningún artefacto de ese servicio, así que su
   plataforma de versiones queda huérfana.

2. **Registro del sistema con contenido de nota.** Al procesar bloques, exportar notas,
   respaldar, importar y leer medios, el registro del sistema del dispositivo imprime valores
   que escribió o importó la persona usuaria: la fórmula de un bloque de ecuación, la ruta, el
   identificador de recurso o la dirección web del medio de un bloque. El principio 5 prohíbe
   volcar contenido a logs. Medido sobre los cuatro módulos del proyecto con un parser que
   tolera llamadas repartidas en varias líneas, hay **143 llamadas** al registro del sistema, de
   las que **35 interpolan** algún valor y **112 reciben una excepción como último argumento**,
   cuyo texto el registro del sistema imprime íntegro. Sites que exponen contenido de nota o
   referencia de medio hay **al menos 15 en el módulo principal y al menos 7 en los módulos
   librería**; el recuento exacto lo fija el plan, que es donde se enumeran. Las llamadas que
   solo imprimen dato no sensible se dejan intactas.

3. **Los tests unitarios no compilan.** La compilación del conjunto de tests del módulo
   principal falla con 13 errores en 2 suites, de modo que hoy no se ejecuta ni un solo test.
   Mientras dure, el principio 4 no es exigible en el entorno de desarrollo: nadie puede
   verificar un cambio antes de entregarlo. La causa son dos suites que llaman a funciones de
   producción con firmas anteriores a las actuales. Al desbloquearse la compilación aparecen
   además dos capturas de imagen de prueba sin referencia, de modo que la ejecución en modo
   verificación exige generarlas y versionarlas antes de poder exigir verde.

4. **Deriva entre los inventarios de licencias y lo que se compila.** El catálogo de versiones
   fija Coil 3.6.3, plataforma de Compose 2026.09.00 y Material 3 1.5.0-alpha28. Esos mismos
   tres valores aparecenannounced como 3.5.0, 2026.06.01 y 1.5.0-alpha25 en el catálogo de
   licencias embebido, en el inventario de licencias de terceros y en la lista de reserva que
   la pantalla de licencias muestra cuando el catálogo embebido no se puede leer. La deriva no la
   causa la analítica, pero impide que los inventarios coincidan con lo que compila.

El objetivo es cerrar esos cuatro incumplimientos con el menor cambio de comportamiento posible
y desbloquear la política de tests primero en el entorno de desarrollo. Con ello quedan
ejecutables las comprobaciones de los principios 1 en lo que atañe a la telemetría, 4 y 5. Los
principios 1 en lo que atañe a las dependencias comentadas, 3 y 6 quedan pendientes en los puntos
enumerados en Fuera de alcance, cada uno con su spec. La corrección del registro del sistema no
puede consistir en borrarlo: cada fallo sigue teniendo que quedar registrado.

## Usuarios

- **Persona usuera de la aplicación.** Escribe y guarda material privado en sus notas
  (texto, fórmulas, archivos, imágenes). No controla el registro del sistema del dispositivo
  ni el código de la aplicación, así que no puede corregir lo que se imprime en él.
- **Persona desarrolladora o mantenedora.** Cambia el código del proyecto y depende de poder
  verificarlo antes de entregarlo, de que la configuración del proyecto no le prometa
  protecciones que no existen y de que la documentación no le exija pasos que no hacen falta.
- **Persona que audita la aplicación antes de distribuirla o publicarla.** Comprueba qué
  recopila y qué dependencias declara la app, leyendo la documentación y el inventario de
  licencias que la propia aplicación muestra.

## Historias de usuario

- HU-1. Como persona usuera, quiero que nada de lo que escribo o importo en mis notas
  aparezca en el registro del sistema del dispositivo, para que mi material no quede
  expuesto en el equipo fuera de mi control.
- HU-2. Como persona usuera, quiero que la aplicación no incluya telemetría, para que nadie
  pueda saber que la uso ni qué hago dentro.
- HU-3. Como persona desarrolladora, quiero que la tarea de tests del módulo principal compile
  y termine en verde en mi entorno de desarrollo, para poder comprobar cualquier cambio antes
  de entregarlo.
- HU-4. Como persona desarrolladora, quiero que las dependencias sin uso no estén declaradas
  ni se anuncien en los inventarios, y que los inventarios de licencias —el embebido, el de
  terceros y la lista de reserva que la aplicación muestra— indiquen las versiones que el
  proyecto compila, para que la configuración no contradiga la constitución.
- HU-5. Como persona desarrolladora, quiero que el fallo siga siendo diagnosticable después
  de quitar el contenido del mensaje, para no perder capacidad de depuración.
- HU-6. Como persona desarrolladora, quiero que los textos visibles que se comparan en los
  tests se obtengan de sus recursos traducidos, para que una traducción no rompa la suite.

## Definiciones

- **Alcance de la revisión.** Los cuatro módulos del proyecto: el módulo principal de la
  aplicación y los tres módulos librería. Toda búsqueda y toda revisión de esta spec recorre los
  cuatro, no solo el módulo principal.
- **Sitios del alcance.** Los puntos del alcance de la revisión que la entrega modifica, según
  enumera el plan. Un requisito que dice "los sitios del alcance" no obliga a nada fuera de esa
  lista.
- **Contenido de nota.** Valor escrito o importado por la persona usuaria y persistido en una
  nota: texto, título, marcado y fórmula. Se considera contenido de nota porque permite leer
  lo que la persona ha escrito.
- **Referencia de medio.** Ruta, identificador de recurso o dirección web que apunta a un
  archivo importado, incrustado o adjuntado en una nota. Se considera contenido de nota porque
  revela qué material se guarda y, si es una dirección web, puede apuntar a un recurso interno
  o privado.
- **Criterio de clasificación.** Una sola pregunta decide si un valor puede imprimirse en el
  registro del sistema: ¿el valor lo escribió o importó la persona usuaria, o lo genera la
  aplicación a partir de su propio estado? Si lo escribió o importó la persona, o describe un
  archivo suyo, es contenido de nota o referencia de medio y no se imprime. Si lo genera la
  aplicación, es dato no sensible y se imprime.
- **Dato no sensible.** Valor que genera la aplicación a partir de su propio estado: constantes,
  nombres internos de preferencias o de configuración, códigos de respuesta de un servicio, sumas
  de comprobación, métricas del dispositivo, identificadores de una versión publicada, rutas de
  los archivos que la propia aplicación descarga y guarda en su almacenamiento interno. Es
  admisible en el registro del sistema.
- **Casos ya clasificados.** El criterio anterior se aplicó al código real y estos son sus
  resultados, que esta spec da por buenos: el cuerpo libre de un error devuelto por un servicio
  local de inferencia al que la aplicación acaba de enviar el prompt **es contenido de nota**,
  porque reproduce lo que se le envió; la ruta interna del archivo de pesos de un modelo
  descargado, en los tres sitios donde se registra, **es dato no sensible** y se conserva; la
  ruta de un archivo adjunto de una nota, aunque la aplicación la haya generado al copiarlo,
  **es referencia de medio** y no se imprime.
- **Categorías de excepción pospuestas.** Los sitios donde una excepción puede llevar en su texto
  contenido de nota y quedan fuera del alcance de esta entrega, revisados y pospuestos a
  propósito: el cifrado y descifrado de notas, la sincronización con el servicio de copias de
  seguridad, la reproducción de medios y el acceso a archivos externos. No están olvidados:
  Revisionar cada uno exige decidir si su texto de excepción puede llegar a contener contenido
  de nota, y esa decisión pertenece a una spec por zona, no a una entrega que ya cubre el
  proceso, la exportación, el respaldo y la importación.
- **Identificador opaco.** Valor corto y opaco que la aplicación asigna a un elemento para poder
  correlacionar trazas sin revelar su contenido. No se deriva del contenido: no es un resumen del
  valor, no es reversible y no admite ataque por diccionario; dos contenidos distintos pueden
  compartir identificador. Su **ámbito** es la operación en curso: se genera una vez al empezar la
  operación y lo comparten todas las trazas de esa operación, de modo que dos operaciones
  distintas no comparten identificador. En un sitio donde no hay operación a la que colgarlo —
  una llamada aislada sin contexto de proceso—, el ámbito es la propia llamada. Puede generarse
  con los recursos del lenguaje y sin dependencias nuevas.
- **Traza técnica.** Conjunto mínimo de información que permite localizar y clasificar un fallo
  sin exponer contenido de nota: la categoría del elemento afectado, su identificador opaco y el
  tipo de la excepción, junto con el mensaje propio de la aplicación. **No** incluye el objeto de
  excepción ni el texto de la excepción cuando ese texto puede contener contenido de nota o una
  referencia de medio.
- **Mensaje de estado.** Texto que genera un componente de la aplicación, no la persona
  usuaria, para describir el resultado de una operación. Puede ser el mismo texto que la
  persona ve en pantalla.
- **Literal visible.** Texto que la persona usuera ve en pantalla y que la aplicación obtiene
  de sus recursos traducidos.
- **Suite verde.** Compilación y ejecución completas de todas las suites de test del proyecto
  que terminan sin fallos y sin casos descartados por error de compilación, con las pruebas de
  imagen ejecutadas en modo verificación y no en modo grabación.
- **Referencia de imagen.** Captura de referencia de una prueba de imagen, versionada en el
  repositorio. Sin ella, el modo verificación falla aunque el resultado sea correcto.

## Requisitos funcionales

### A. Dependencias e inventarios

- **RF-1**: EL SISTEMA no incluye ningún SDK de analítica o telemetría en el artefacto que se
  distribuye. Verificable: el análisis del artefacto de publicación no reporta componentes de
  telemetría.
- **RF-2**: CUANDO se resuelve el árbol de dependencias del módulo principal para la variante de
  publicación, EL SISTEMA no resuelve ningún artefacto de analítica ni de telemetría.
  Verificable: la resolución no lista ningún artefacto de esos dos grupos.
- **RF-3**: SI una dependencia de telemetría o analítica deja de usarse, ENTONCES EL SISTEMA la
  retira por completo: ni declaración activa, ni entrada comentada, ni entrada en el catálogo de
  versiones, ni anuncio en los inventarios. No aplica al artefacto de configuración del servicio
  de copias de seguridad, que no es una dependencia y no se toca.
- **RF-4**: SI cambia el conjunto de dependencias del proyecto, ENTONCES EL SISTEMA deja el
  catálogo de versiones, el catálogo de licencias embebido, el inventario de licencias de
  terceros y la lista de reserva que la pantalla de licencias muestra cuando el catálogo
  embebido no se puede leer, iguales a lo que el proyecto compila, versión incluida, y las
  licencias de autenticación del servicio de copias de seguridad siguen declaradas porque esa
  funcionalidad se usa.

### B. Registro del sistema

- **RF-5**: EL SISTEMA no escribe en el registro del sistema ningún valor que sea contenido de
  nota ni referencia de medio.
- **RF-6**: SI se produce un fallo al procesar el contenido de un bloque, ENTONCES EL SISTEMA
  registra la categoría del bloque y su identificador opaco, y no el texto, el marcado ni la
  fórmula del bloque.
- **RF-7**: SI se produce un fallo al procesar, importar, copiar, borrar o descargar un medio
  referenciado por una nota, ENTONCES EL SISTEMA registra la categoría del medio y su
  identificador opaco, y no la ruta, el identificador de recurso ni la dirección web del medio, ni
  nada derivado de esos valores.
- **RF-8**: SI un sitio del alcance registra un objeto de excepción cuyo mensaje pueda contener
  contenido de nota o referencia de medio, ENTONCES EL SISTEMA no registra ese objeto completo.
  Los sitios fuera del alcance quedan pendientes de la spec por zona que los recoja.
- **RF-9**: SI se produce un fallo en un sitio del alcance, ENTONCES EL SISTEMA conserva la
  traza técnica y el fallo no queda sin registrar en ningún camino de ejecución de ese sitio.
- **RF-10**: EL SISTEMA no sustituye el contenido de nota por una constante sin campos. En cada
  sitio modificado, el mensaje escrito al registro contiene la categoría del elemento afectado y
  su identificador opaco, o el tipo de la excepción y el mensaje propio de la aplicación.
  Sustituir una interpolación por un texto fijo que solo dice qué clase de fallo ocurrió
  **incumple** este requisito, aunque cumpla RF-5, RF-8 y RF-11, y aunque el número de
  escrituras al registro no baje.
- **RF-11**: SI la causa asociada a un fallo lleva en su propio texto una ruta, una dirección web
  o un fragmento de la nota, ENTONCES EL SISTEMA registra el tipo de la excepción y el mensaje
  propio de la aplicación, y no el objeto de excepción completo.
- **RF-12**: SI se registra un mensaje de estado generado por un componente, ENTONCES EL SISTEMA
  lo mantiene siempre que el mensaje no incluya texto de nota ni una referencia de medio.
- **RF-13**: CUANDO se revisa el código de los sitios del alcance, EL SISTEMA no contiene ninguna
  llamada al registro del sistema que interpole contenido de nota o referencia de medio, ni
  ninguna que registre el objeto de una excepción cuyo mensaje pueda contenerlos; la revisión se
  apoya en dos búsquedas sobre el alcance de la revisión: una de interpolaciones dentro de
  llamadas al registro del sistema y otra de llamadas cuyo último argumento es un objeto de
  excepción.
- **RF-14**: MIENTRAS una llamada al registro del sistema solo interpole datos no sensibles y no
  reciba un objeto de excepción, EL SISTEMA la mantiene sin cambios. Este requisito no obliga a
  conservar el número de llamadas al registro del sistema ni su forma: una llamada puede
  fusionarse con otra o dejar de interpolar, siempre que el fallo siga registrado y no baje el
  número de escrituras del sitio, como exige RNF-2.

### C. Tests

- **RF-15**: CUANDO se compila el conjunto de tests unitarios del módulo principal, EL SISTEMA
  completa la compilación sin errores.
- **RF-16**: CUANDO se ejecuta la tarea de tests del proyecto, EL SISTEMA compila y ejecuta todas
  las suites existentes y termina en verde, con las pruebas de imagen en modo verificación. Ese
  modo se fija con la propiedad `roborazzi.test.verify` del plugin de pruebas de imagen, no con
  `roborazzi.test.record` ni con `roborazzi.test.compare`, y todas las referencias de imagen que
  las pruebas necesitan existen y están versionadas antes de exigir verde. SI al desbloquear la
  compilación aparece un fallo preexistente y ajeno a esta entrega, distinto de la falta de
  referencias de imagen, ENTONCES EL SISTEMA lo documenta y abre una spec que lo recoja, sin
  desactivar el caso ni bloquear esta entrega.
- **RF-17**: SI una función ejercida por un test requiere el contexto de la aplicación, ENTONCES la
  suite lo obtiene del entorno de prueba y declara un nivel de API igual al objetivo de versión
  android del proyecto, que es 36. No se declara el nivel de compilación, que es 37: su artefacto
  de prueba no está disponible y la suite no se ejecutaría sin descargar nada. Aplica a las dos
  suites que hoy declaran un nivel distinto.
- **RF-18**: SI la suite del widget compara el resultado de una función con un literal visible,
  ENTONCES EL SISTEMA obtiene el valor esperado del recurso correspondiente y no de un literal
  escrito en el test.
- **RF-19**: EL SISTEMA no elimina, desactiva ni salta ningún caso de prueba para conseguir una
  suite verde, y mantiene el alcance de los existentes. La única excepción a no añadir casos es la
  que este mismo requisito necesita para ser comprobable: una función pura que construya el mensaje
  de reemplazo de un fallo puede acompañarse de los casos que prueben que ese mensaje
  contiene la categoría y el identificador opaco, y de que el texto de la excepción no aparece en
  él. No se añaden casos para ninguna otra cosa.

### D. Documentación de la entrega

- **RF-20**: AL terminar la entrega, EL SISTEMA deja la memoria del proyecto en un máximo de 50
  líneas y registra en ella el estado actual, las decisiones tomadas con su motivo y los errores a
  evitar, pasando al registro de decisiones lo que ya no aporte al estado actual y eliminando lo
  que sea redundante.

## Requisitos no funcionales

- **RNF-1 — Sin telemetría en el artefacto.** El artefacto que se distribuye no contiene ningún
  componente de analítica ni de telemetría. Verificable: la resolución del árbol de dependencias del
  módulo principal para la variante de publicación, y el análisis del artefacto de esa misma
  variante, no reportan nada de analítica ni de telemetría.
- **RNF-2 — Diagnóstico preservado en lo que se toca.** Ninguno de los bloques de captura de
  excepción modificados por esta entrega queda sin registrar. Verificable: el número de
  escrituras al registro del sistema de cada sitio modificado no baja del que tenía antes de la
  entrega, y el sitio sigue teniendo al menos una. Los bloques de captura que la entrega no modifica
  no quedan dentro del alcance: el proyecto tiene unos 205 en los cuatro módulos y una parte
  importante son rutas de reserva ante un fallo de análisis.
- **RNF-3 — Comportamiento inalterado.** El único efecto observable de la parte B sobre la
  ejecución de la aplicación es qué información aparece en el registro del sistema: no cambia el
  resultado de la exportación, del dibujado, del respaldo ni del widget. Verificable: las suites de
  esas zonas pasan y solo cambian las aserciones que esta spec exige cambiar, que son la del
  literal visible del widget y las de nivel de API de RF-17.
- **RNF-4 — Verificabilidad.** Cada requisito funcional tiene una comprobación reproducible, un
  comando de una línea o un caso de prueba, que se puede adjuntar al registro de la entrega.
  Verificable: la sección de criterios de finalización no contiene ninguna comprobación que solo
  pueda hacer una persona mirando la pantalla.
- **RNF-5 — Un idioma por capa en lo que se toca.** Los identificadores, las API y los mensajes
  del registro del sistema que esta entrega añada o modifique están en inglés; los comentarios y
  la documentación, en español. Verificable: una búsqueda, en los sitios del alcance, de las
  palabras del español más frecuentes en mensajes de registro del sistema no devuelve ningún
  resultado en los sitios modificados.
- **RNF-6 — Sin coste nuevo.** La entrega no añade dependencias, tareas de compilación ni procesos
  de compilación nuevos. Verificable: el conjunto de dependencias solo disminuye, el catálogo de
  versiones pierde entradas y no las gana, y la lista de tareas de compilación no crece.

## Casos límite

1. **Medio sin referencia.** Un bloque cuyo medio está vacío o cuya referencia no se puede
   resolver produce igualmente un fallo registrado, y el mensaje no contiene la referencia ni
   nada derivado de ella.
2. **Dos notas con el mismo archivo.** Dos notas distintas que apuntan al mismo archivo pueden
   compartir identificador opaco dentro de una misma operación: el registro no permite saber a
   qué nota pertenece el material.
3. **Dos operaciones distintas.** Dos exportaciones del mismo documento dan identificadores
   opacos distintos, porque el ámbito del identificador es la operación.
4. **Sitio sin operación.** Una llamada aislada que no pertenece a ninguna operación genera su
   identificador opaco en la propia llamada, de modo que el identificador existe y el mensaje no
   es una constante sin campos.
5. **Fallo repetido en bucle.** Exportar un documento con diez imágenes inaccesibles deja diez
   trazas y ninguna dirección web.
6. **Dirección web interna o privada.** Una dirección de la red local que falla al descargarse no
   aparece completa ni parcialmente en el registro, y su identificador opaco no se deriva de ella.
7. **Excepción de plataforma con ruta en su texto.** Un fallo del sistema cuyo texto es
   "<archivo> no encontrado" se registra con el tipo de la excepción y el mensaje propio de la
   aplicación, sin el objeto de excepción.
8. **Excepción cuyo mensaje reproduce parte del contenido.** Un fallo de análisis de una
   estructura serializada cuyo mensaje incluye un fragmento de la cadena analizada, que en este
   proyecto es el contenido de la nota, se registra sin ese fragmento.
9. **Dependencia de telemetría comentada.** Una dependencia de analítica que solo sobrevive como
   entrada comentada y como anuncio en los inventarios desaparece por completo de la configuración
   y de los tres inventarios.
10. **Búsqueda que encuentra el archivo de configuración del servicio de copias de seguridad.** Una
    búsqueda de analítica o telemetría en el repositorio devuelve legítimamente el artefacto de
    configuración de ese servicio, que no se toca. Quien lea el resultado no debe deducir que hay
    que borrarlo: su ausencia rompe la compilación.
11. **Suite sin contexto de aplicación.** Un test de una función que necesita recursos de Android
    sigue siendo ejecutable sin dispositivo ni instrumentación, y sin descargar artefactos de
    prueba de un nivel de API distinto del objetivo de versión android, que es 36.
12. **Prueba de imagen sin referencia.** Una prueba de imagen sin referencia versionada falla en
    modo verificación aunque el resultado sea correcto; la entrega genera y versiona las que
    faltan antes de exigir verde.
13. **Fallo preexistente al desbloquear la compilación.** Un test que ya estaba roto y que nada
    tiene que ver con esta entrega, y que no es la falta de una referencia de imagen, se documenta
    y abre su propia spec; no se desactiva ni se reescribe su aserción para que la suite quede
    verde.
14. **Texto visible dependiente del idioma.** El valor esperado del test del widget se obtiene del
    recurso, así que el resultado del test no cambia con el idioma configurado en la prueba.

## Fuera de alcance

- **Principio 1, dependencias comentadas y sin usar que no son de telemetría** (permisos, cámara
  ×4, almacenamiento de preferencias, ubicación): se quedan como están. Tienen spec pendiente,
  que es la que debe cerrar la comprobación completa del principio 1.
- **Principio 3, lógica fuera de la interfaz.** Esta entrega no audita los Composables del
  proyecto para detectar llamadas a la capa de datos o de cifrado, así que la comprobación del
  principio 3 sigue sin ser ejecutable.
- **Principio 6, sincronización de traducciones.** El locale inglés de Reino Unido tiene 843
  claves frente a 1149 en los otros ocho, es decir 306 ausentes, entre ellas la del texto de
  título sin título que usa el widget. No forma parte de esta entrega: la diferencia es anterior a
  ella y requiere su propia spec, que también debe decidir si ese locale se completa o se
  elimina. Esta entrega solo evita añadir una clave más a esa diferencia.
- **Los tests del módulo principal en integración continua.** El flujo de integración continua no
  ejecuta la tarea de tests del módulo principal y esta entrega no lo cambia. El motivo es que esa
  tarea depende de un artefacto de configuración del servicio de copias de seguridad que, por
  diseño, no está versionado, de modo que añadarla al flujo lo dejaría rojo de forma permanente.
  Desbloquearlo exige injectar esas credenciales en el entorno del flujo, que es una decisión de
  infraestructura ajena a esta entrega.
- **Las categorías de excepción pospuestas.** De las 112 llamadas que pasan una excepción como
  último argumento, solo unas 10 están en los sitios del alcance. Las que pertenecen al cifrado y
  descifrado de notas, a la sincronización con el servicio de copias de seguridad, a la
  reproducción de medios y al acceso a archivos externos quedan pendientes, con la spec por zona
  que las recoja.
- **El cliente de llamadas HTTP declarado y sin usar.** El mismo inventario anuncia un cliente de
  llamadas HTTP que ninguna función del proyecto usa, por el mismo motivo que la analítica. No se
  retira aquí: eliminar una dependencia que quizá se quiera usar en el futuro es una
  decisión de proyecto, no un remedio de la constitución, y por tanto tiene su propia spec.
- **La deriva de versiones en la guía de ingeniería.** El valor de la plataforma de Compose que
  anuncia la guía de ingeniería también está desviado. Queda fuera de alcance porque esta entrega
  no reescribe las guías; lo recoge la spec que las mantenga al día.
- El fallo conocido de representación de tablas (la tabla no aparece al previsualizar ni al leer
  en voz alta) y los defectos de selección de texto de los bloques editables. Son temas
  registrados en la memoria del proyecto y requieren su propia spec.
- La verificación visual en dispositivo de cualquier zona de la aplicación (tabla, refresco, jank,
  carga del modelo). Esta entrega se comprueba de forma automatizada.
- Retirar el plugin de configuración del servicio de copias de seguridad ni el artefacto que ese
  plugin exige. El plugin está aplicado y sin el artefacto la compilación falla: la afirmación de la
  documentación operativa que lo declara obligatorio es correcta y no se toca.
- Tocar las licencias y las dependencias de autenticación: se usan en el respaldo y no entran en
  esta entrega.
- Reintroducir telemetría, aunque sea anónima, opcional o con interruptor.
- Cambios de comportamiento en el editor de bloques, en el motor de IA, en la sincronización, en el
  respaldo o en el cifrado.
- Cambios en el esquema de la base de datos local.
- Sustituir el proveedor de identidad o el gestor de contraseñas.
- Reescribir la constitución, las guías de ingeniería o la documentación de arquitectura.
- Retirar el registro de métricas de rendimiento en compilaciones de depuración: no contiene
  contenido de nota y es la herramienta de verificación de las reglas de rendimiento.

## Criterios de finalización

- [ ] La compilación del conjunto de tests del módulo principal termina sin errores, y se han
      corregido los 13 errores de las 2 suites afectadas.
- [ ] La tarea de tests del proyecto termina en verde, con un recuento de casos ejecutados mayor
      que cero y no menor que el número de suites existentes.
- [ ] La propiedad `roborazzi.test.verify` está fijada en la ejecución de los tests, y no están
      fijadas `roborazzi.test.record` ni `roborazzi.test.compare`.
- [ ] Las dos referencias de imagen que faltaban existen y están versionadas, y la suite de
      capturas de imagen pasa en modo verificación.
- [ ] Si al desbloquear la compilación apareció un fallo preexistente distinto de la falta de
      referencias de imagen, está documentado y tiene una spec abierta; si no apareció, la
      condición no aplica.
- [ ] La tarea de ensamblado de la aplicación de depuración termina correctamente.
- [ ] La resolución del árbol de dependencias del módulo principal para la variante de publicación
      no lista ningún artefacto de analítica ni de telemetría, y el análisis del artefacto de esa
      variante tampoco los reporta.
- [ ] Una búsqueda de dependencias de analítica o telemetría en la configuración del proyecto y en
      el catálogo de versiones no devuelve ninguna entrada, ni activa ni comentada, y ninguna
      plataforma de versiones queda sin ningún artefacto asociado. La búsqueda excluye el
      artefacto de configuración del servicio de copias de seguridad, que debe seguir presente.
- [ ] El catálogo de licencias embebido, el inventario de licencias de terceros y la lista de
      reserva que la pantalla de licencias muestra anuncian las mismas versiones que el catálogo
      de versiones, y ninguno de los tres contiene entradas de telemetría.
- [ ] Las licencias de autenticación del servicio de copias de seguridad siguen declaradas en el
      catálogo de licencias embebido, en el inventario de terceros y en la lista de reserva.
- [ ] Una búsqueda, sobre el alcance de la revisión, de llamadas al registro del sistema que
      interpolen contenido de nota o referencia de medio no devuelve ningún resultado en los
      sitios del alcance.
- [ ] Una búsqueda, sobre el alcance de la revisión, de llamadas al registro del sistema cuyo
      último argumento sea un objeto de excepción no devuelve ningún resultado en los sitios del
      alcance.
- [ ] La entrega adjunta la salida de la búsqueda de interpolaciones sobre el alcance de la
      revisión, antes y después de los cambios, y el recuento de llamadas que interpolan puede
      bajar en los sitios modificados, sin que eso sea un incumplimiento por sí solo.
- [ ] La entrega adjunta la lista explícita de las llamadas al registro del sistema que se
      conservan sin cambios, y el diff de la entrega no toca ninguna de ellas.
- [ ] En cada sitio modificado, el mensaje escrito al registro contiene la categoría del elemento
      afectado y su identificador opaco, o el tipo de la excepción y el mensaje propio de la
      aplicación: cada mensaje de reemplazo declara al menos un campo interpolado y ninguno es una
      constante sin campos.
- [ ] El número de escrituras al registro del sistema de cada sitio modificado no baja del que
      tenía antes de la entrega.
- [ ] Ningún caso de prueba se ha eliminado, desactivado ni saltado. Los únicos casos añadidos, si
      los hay, son los de la función pura que construye el mensaje de reemplazo de un fallo, y
      cubren que ese mensaje contiene la categoría y el identificador opaco y que el texto de la
      excepción no aparece en él.
- [ ] El test del widget obtiene el valor esperado del recurso de texto de título sin título en
      lugar de un literal escrito en la suite.
- [ ] Las dos suites que declaraban un nivel de API distinto del 36 declaran 36.
- [ ] La memoria del proyecto tiene 50 líneas o menos, y registra el estado actual, las decisiones
      con su motivo y los errores a evitar.

## Decisiones tomadas

Resueltas el 2026-10-03 tras dos revisiones de calidad. No queda ninguna pregunta pendiente.

1. **Alcance de la parte B.** Los sitios que exponen contenido de nota o referencia de medio en
   los cuatro módulos: al menos 15 en el módulo principal y al menos 7 en los módulos librería. El
   recuento exacto lo fija el plan. Las llamadas que solo imprimen dato no sensible se conservan
   (RF-14).
2. **Qué se escribe en su lugar.** La categoría del elemento y un identificador opaco, con
   ámbito de la operación o de la llamada si no hay operación, que no se deriva del contenido y no
   es reversible (RF-6, RF-7).
3. **Excepciones.** El tipo de la excepción y el mensaje propio de la aplicación; nunca el objeto
   completo cuando su texto puede contener contenido de nota (RF-8, RF-11).
4. **Mensaje de estado del motor de IA.** Se mantiene: lo genera un componente, no es contenido de
   nota y es necesario para diagnosticar la carga del modelo (RF-12).
5. **Retirada de la analítica.** Se borran la declaración activa, la entrada comentada, la
   plataforma de versiones que queda huérfana y las tres entradas del catálogo de versiones, más
   los anuncios de los inventarios de licencias.
6. **Plugin de configuración del servicio de copias de seguridad.** Se conserva, con el artefacto
   que exige, porque está aplicado y sin él la compilación falla. La documentación operativa no se
   corrige en esa parte: su afirmación era correcta. Por eso se retiraron los dos requisitos que
   habían nacido de esa premisa falsa.
7. **Deriva de versiones.** Se corrige en el catálogo de versiones, en el catálogo de licencias
   embebido, en el inventario de terceros y en la lista de reserva de la pantalla de licencias
   (RF-4). La copia desviada de la guía de ingeniería queda fuera de alcance.
8. **Resto de dependencias comentadas.** Para otra spec; se quedan como están.
9. **Cliente de llamadas HTTP declarado y sin usar.** Fuera de alcance con motivo propio: es una
   decisión de dependencia de proyecto, no un remedio de la constitución.
10. **Disparadores de la parte B.** Ampliados a procesar, importar, copiar, borrar y descargar un
    medio referenciado, porque cinco sitios no fallan al leer ni al descargar (RF-7).
11. **Vector del objeto de excepción.** La revisión comprueba también el último argumento de las
    llamadas al registro del sistema, porque las interpolaciones no veían esa fuga (RF-8, RF-13).
12. **Criterio de clasificación.** Basado en la procedencia del valor y con tres casos ya
    decididos sobre el código real, en lugar de una lista enumerativa de datos no sensibles
    (Definiciones).
13. **Excepciones pospuestas.** Cifrado, sincronización, reproducción de medios y acceso a
    archivos externos quedan revisados y pospuestos, con el recuento de 112 llamadas y unas 10 en
    alcance, y spec por zona que los recoja.
14. **Rendimiento de la verificación.** La entrega no exige registro en los unos 205 bloques de
    captura de excepción del proyecto ni revisar todos los mensajes en español: RNF-2 y RNF-5
    quedan acotados a lo que la entrega modifica, con criterio ejecutable.
15. **Nivel de API de los tests.** Fijado al objetivo de versión android, que es 36, y no al de
    compilación, que es 37: 37 es el único sin artefacto de prueba disponible en caché y sin él la
    suite no corre sin red (RF-17). Aplica también a las dos suites que declaraban 28.
16. **Pruebas de imagen.** El modo verificación se fija con la propiedad `roborazzi.test.verify`, y
    las dos referencias de imagen ausentes se generan y versionan como parte de la entrega. No es
    aceptar una imagen que nadie miró: es la referencia que el modo verificación exige (RF-16).
17. **Fallos preexistentes.** La salida de documentar y abrir spec se reserva a fallos
    preexistentes distintos de la falta de referencias de imagen, que se resuelven en esta
    entrega (RF-16).
18. **Integración continua.** La tarea de tests del módulo principal **no** se añade al flujo que
    bloquea el cambio: depende de un artefacto de configuración no versionado por diseño y el flujo
    quedaría rojo de forma permanente. HU-3 promete la ejecución en el entorno de desarrollo, que
    es lo que es cierto.
19. **Aserciones y RNF-3.** RNF-3 admite los cambios de aserción que la propia spec exige: el
    literal visible del widget y el nivel de API de las dos suites que declaraban 28.
20. **Memoria del proyecto.** Condensar a 50 líneas o menos, registrando estado, decisiones con su
    motivo y errores a evitar, forma parte de la entrega (RF-20).
21. **Tests.** Se arreglan los 13 errores, se añaden el entorno de pruebas y el nivel de API que
    necesitan las suites afectadas, y el literal detectado pasa a leerse del recurso. Sin cobertura
    nueva, salvo la que necesita la función pura del mensaje de reemplazo, y sin extender la regla
    de literales al resto de suites (RF-15 a RF-19).
22. **Enmienda a RF-19 (2026-10-03, posterior a la aprobación).** RF-19 prohibía añadir casos de
    prueba, pero el principio 4 de la constitución exige test para toda función pura nueva y la
    función que construye el mensaje de reemplazo es pura. Sin esa excepción, RF-19 y el principio 4
    son incompatibles y el criterio de que el mensaje contenga la categoría y el identificador opaco
    no tiene forma de comprobarse. Se admite **solo** para esa función, y los casos añadidos se
    limitan a comprobar que el mensaje contiene la categoría y el identificador opaco y que el texto
    de la excepción no aparece en él. Las ramas de captura que la entrega modifica siguen sin
    cobertura directa, porque ninguna suite dispara hoy un fallo de lectura de medio: eso es un
    límite aceptado de esta entrega, no una omisión.