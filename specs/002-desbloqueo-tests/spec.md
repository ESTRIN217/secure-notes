# Spec 002 — Desbloqueo de los tests unitarios

Estado: **parcial** (2026-10-04) — T1-T3 hechas; T4-T5 bloqueadas por la plataforma, ver "Bloqueo de plataforma"

> **Nota de procedencia.** Esta spec es una hija de `specs/001-limites-constitucion/spec.md`
> (**aprobada**), que es la spec madre y sigue siendo la ley. No la sustituye ni la contradice.
> Hereda sin modificar: **RF-15, RF-16, RF-17, RF-18, RF-19** y las RNF que les aplican, más el
> criterio «los tests del módulo principal no se ejecutan en integración continua» que la madre
> fija en su Fuera de alcance. Los identificadores `RF-x` son los de la madre; aquí no se
> renumeran, para que cualquier referencia siga siendo resoluble. Su plan y sus tareas están en
> `plan.md` y `tasks.md` de esta misma carpeta.

## Contexto y objetivo

`./gradlew test` es la única comprobación que puede validar cualquier otro cambio del proyecto
(principio 4 de la constitución: *tests primero, `./gradlew test` en verde*). Hoy **no corre**: la
compilación del conjunto de tests del módulo principal falla con **13 errores repartidos en 2
suites**, y como Kotlin compila todo el source set de test de una vez, **no se ejecuta ni un solo
test unitario del proyecto**. Mientras dure, el principio 4 no es exigible y ninguna otra entrega
puede declararse verificada.

Al desbloquear la compilación aparecen además dos capturas de imagen de prueba sin referencia
versionada, de modo que exigir la suite en modo verificación exige generarlas antes.

El objetivo es que `./gradlew test` termine en verde en el entorno de desarrollo, con las
condiciones de entorno que la madre fija (nivel de API correcto, modo verificación, referencias
versionadas) y sin tocar una sola línea de código de producción.

## Usuarios

- **Persona desarrolladora o mantenedora.** Depende de poder ejecutar la suite antes de entregar
  cualquier cambio, hoy imposible.
- **Persona usuera de la aplicación.** No le afecta directamente, pero se beneficia de que las
  comprobaciones que protegen sus datos existan y se ejecuten.

## Historias de usuario

- HU-3. Como persona desarrolladora, quiero que la tarea de tests del módulo principal compile y
  termine en verde en mi entorno de desarrollo, para poder comprobar cualquier cambio antes de
  entregarlo.

## Definiciones

- **Objetivo de versión android.** El nivel de API al que la aplicación apunta al ejecutarse, que
  es **36**. No es el nivel de compilación, que es 37.
- **Referencia de imagen.** Captura de referencia de una prueba de imagen, versionada en el
  repositorio. Sin ella, el modo verificación falla aunque el resultado sea correcto.
- **Suite verde.** Compilación y ejecución completas de todas las suites de test del proyecto que
  terminan sin fallos y sin casos descartados por error de compilación, con las pruebas de imagen
  en modo verificación y no en modo grabación.

## Requisitos funcionales

Heredados de la spec madre, sin modificación:

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
  de reemplazo de un fallo puede acompañarse de los casos que prueben que ese mensaje contiene la
  categoría y el identificador opaco, y de que el texto de la excepción no aparece en él. No se
  añaden casos para ninguna otra cosa.

La excepción de RF-19 **no se usa en esta spec**: la función pura del mensaje de reemplazo la
introduce la spec 004. Aquí RF-19 se aplica entero.

## Requisitos no funcionales

Heredados de la spec madre, sin modificación:

- **RNF-4 — Verificabilidad.** Cada requisito funcional tiene una comprobación reproducible, un
  comando de una línea o un caso de prueba. Verificable: la sección de criterios de finalización no
  contiene ninguna comprobación que solo pueda hacer una persona mirando la pantalla.
- **RNF-5 — Un idioma por capa en lo que se toca.** Identificadores, API y mensajes en inglés;
  comentarios y documentación en español. Verificable: revisión de los textos añadidos.
- **RNF-6 — Sin coste nuevo.** La entrega no añade dependencias, tareas de compilación ni procesos
  de compilación nuevos.

## Casos límite

1. **Test de un componente sin contexto de aplicación.** Sigue siendo ejecutable sin dispositivo ni
   instrumentación, y sin descargar artefactos de prueba de un nivel de API distinto del objetivo
   de versión android.
2. **Prueba de imagen sin referencia.** Una prueba de imagen sin referencia versionada falla en
   modo verificación aunque el resultado sea correcto; la referencia se genera y se versiona antes
   de exigir verde.
3. **Fallo preexistente al desbloquear la compilación.** Un test que ya estaba roto y que nada
   tiene que ver con esta entrega, y que no es la falta de una referencia de imagen, se documenta
   y abre su propia spec; no se desactiva ni se reescribe su aserción para que la suite quede verde.
4. **Texto visible dependiente del idioma.** El valor esperado del test del widget se obtiene del
   recurso, así que el resultado no cambia con el idioma configurado en la prueba.
5. **Literal en inglés que sigue siendo correcto.** Corregir la aserción del widget no cambia el
   valor que la aplicación muestra: el idioma por capa sigue siendo el de la constitución.

## Fuera de alcance

- **Retirar `NoteContentUtils` ni ninguna otra función aunque su test no la cubra.** Esta spec no
  cambia el alcance de los tests existentes, solo los hace ejecutables.
- **Añadir `:app:test` al flujo de integración continua.** El artefacto de configuración del
  servicio de copias de seguridad que exige el plugin aplicado no está versionado, por diseño;
  añadirlo al flujo lo dejaría rojo de forma permanente. Desbloquearlo exige inyectar esas
  credenciales en el entorno del flujo, que es una decisión de infraestructura ajena a esta spec.
  Heredado del Fuera de alcance de la madre.
- **La regla de literales visibles más allá de la suite del widget.** RF-18 se aplica a la suite
  del widget; no se auditan las demás suites buscando literales.
- **Tests instrumentados.** No hay dispositivo; `connectedCheck` no es ejecutable.
- **Arreglar la representación de tablas, la selección de los bloques editables o cualquier otro
  bug funcional.** Heredado de la madre.
- **Todo lo de las specs 003 y 004**: dependencias e inventarios, y registro del sistema.

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
- [ ] Ningún caso de prueba se ha eliminado, desactivado ni saltado.
- [ ] El test del widget obtiene el valor esperado del recurso de texto de título sin título en
      lugar de un literal escrito en la suite.
- [ ] Las dos suites que declaraban un nivel de API distinto del 36 declaran 36.
- [ ] `git diff -- app/src/main` no muestra ninguna modificación en código de producción.

## Dudas abiertas

Ninguna. Todas las decisiones de esta spec están tomadas en la spec madre y en su plan.

## Bloqueo de plataforma (2026-10-03)

Al ejecutar las tareas se encontró un límite del **entorno**, no del código: la máquina de
desarrollo es **aarch64** y **Robolectric 4.17 no distribuye runtime nativo para `linux/aarch64`**.
Comprobado en el artefacto `nativeruntime-dist-compat-1.0.19`, que solo contiene `linux/x86_64`,
`mac/x86_64`, `mac/aarch64` y `windows/x86_64`. No es un problema de caché ni de configuración: el
binario no existe para esta arquitectura.

Consecuencia: **toda suite Robolectric falla en `beforeTest`**, antes de ejecutar una línea, con
`The Robolectric native runtime is not supported on Linux (aarch64)`. Afecta igual a las suites con
y sin `@GraphicsMode(NATIVE)`.

**Qué se pudo verificar y quedó hecho**:

- [x] La compilación del conjunto de tests del módulo principal termina sin errores, y se han
      corregido los 13 errores de las 2 suites afectadas. *(T1, T2)*
- [x] Ningún caso de prueba se ha eliminado, desactivado ni saltado. *(266 `@Test` antes y después)*
- [x] El test del widget obtiene el valor esperado del recurso de texto de título sin título en
      lugar de un literal escrito en la suite. *(T1)*
- [x] Las dos suites que declaraban un nivel de API distinto del 36 declaran 36. *(T3; 8
      coincidencias de `sdk = [36]`, 0 de `sdk = [28]`)*
- [x] `git diff -- app/src/main` no muestra ninguna modificación en código de producción.

**Qué queda sin poder verificar aquí, y por qué**:

- [ ] La tarea de tests del proyecto termina en verde. **Bloqueado**: ninguna suite puede
      ejecutarse en `linux/aarch64`. No es un fallo preexistente de una suite que una spec pueda
      arreglar: es la plataforma.
- [ ] `roborazzi.test.verify` fijada y las dos referencias de imagen ausentes generadas y
      versionadas. **Bloqueado** por lo mismo: generar goldens exige ejecutar los tests de imagen.

**Cómo se resuelve**: en un host `x86_64` (o `mac/aarch64`, que sí tiene runtime nativo), o en un
runner de integración continua de esas arquitecturas. La mother's Decisión sobre el flujo de
integración continua ya excluido `:app:test` del pipeline por el archivo de configuración que
exige el plugin, así que hoy no hay ninguna vía automática: hay que hacerlo desde un host x86_64.

**Sobre la premisa de T3**: el plan justificaba subir las dos suites de SDK 28 a 36 porque 28 está
roto (`PackageParserException` sobre `apk_for_local_tests`). El cambio es correcto como código y se
hizo, pero **en este host no se puede comprobar que arregle algo**: a 36 fallarían por el motivo de
arm64. Queda anotado para que quien lo ejecute en x86_64 lo confirme.