# Spec 003 — Dependencias e inventarios

Estado: **implementada** (2026-10-04) — T1 y T2 cerradas, los 8 criterios de cierre verificados

> **Nota de procedencia.** Esta spec es una hija de `specs/001-limites-constitucion/spec.md`
> (**aprobada**), que es la spec madre y sigue siendo la ley. No la sustituye ni la contradice.
> Hereda sin modificar: **RF-1, RF-2, RF-3, RF-4** y las RNF que les aplican. Los identificadores
> `RF-x` son los de la madre; aquí no se renumeran. Su plan y sus tareas están en `plan.md` y
> `tasks.md` de esta misma carpeta.

## Contexto y objetivo

El principio 1 de la constitución exige que cada dependencia declarada se justifique en una spec y
aparezca en el inventario de licencias, y que no quede nada comentado ni sin uso. El principio 5
prohíbe la telemetría. Hoy hay dos incumplimientos:

1. **Una dependencia de analítica declarada y sin usar.** Está en la configuración de
   dependencias y anunciada en los dos inventarios de licencias, pero ninguna funcionalidad la usa
   ni la inicializa. Su plataforma de versiones queda además huérfana cuando se retira, porque no
   queda ningún artefacto de ese servicio.
2. **Deriva entre los inventarios y lo que se compila.** El catálogo de versiones fija Coil 3.6.3,
   plataforma de Compose 2026.09.00 y Material 3 1.5.0-alpha28; los tres valores se anuncian
   anunciados en el catálogo de licencias embebido, en el inventario de licencias de terceros y en
   la lista de reserva que la propia pantalla de licencias muestra cuando el catálogo embebido no
   se puede leer. La deriva no la causa la analítica, pero impide que los inventarios coincidan con
   lo que compila, que es lo que RF-4 exige.

El objetivo es cerrar ambos, dejando la configuración y los tres inventarios diciendo exactamente
lo que el proyecto compila. Es la entrega más barata de las tres: seis líneas de configuración y
unas cifras en sendos inventarios.

## Usuarios

- **Persona desarrolladora o mantenedora.** Depende de que la configuración no le prometa
  protecciones que no existen y de que los inventarios no le indiquen versiones equivocadas.
- **Persona que audita la aplicación antes de distribuirla o publicarla.** Lee el inventario de
  licencias que la propia aplicación muestra; ese texto tiene que ser cierto.

## Historias de usuario

- HU-2. Como persona usuera, quiero que la aplicación no incluya telemetría, para que nadie pueda
  saber que la uso ni qué hago dentro.
- HU-4. Como persona desarrolladora, quiero que las dependencias sin uso no estén declaradas ni se
  anuncien en los inventarios, y que los inventarios de licencias —el embebido, el de terceros y la
  lista de reserva que la aplicación muestra— indiquen las versiones que el proyecto compila, para
  que la configuración no contradiga la constitución.

## Definiciones

- **Inventario de licencias.** Los tres sitios donde el proyecto declara qué dependencias de
  terceros usa: el catálogo de licencias embebido en la aplicación, el inventario de licencias de
  terceros del repositorio, y la lista de reserva que la pantalla de licencias muestra cuando el
  catálogo embebido no se puede leer. Los tres tienen que coincidir con lo que el proyecto compila.
- **Catálogo de versiones.** El archivo del proyecto que fija la versión de cada dependencia. Es la
  fuente de verdad de las versiones.
- **Plataforma de versiones.** El artefacto que agrupa versiones de una familia de dependencias.
  Si una familia se queda sin ningún artefacto, su plataforma queda huérfana y también se retira.
- **Artefacto de configuración del servicio de copias de seguridad.** El archivo que exige el
  plugin de configuración de Google aplicado al módulo principal. **No está versionado por
  diseño** y sin él la compilación falla. No es una dependencia y esta spec no lo toca.

## Requisitos funcionales

Heredados de la spec madre, sin modificación:

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
  catálogo de versiones, el catálogo de licencias embebido, el inventario de licencias de terceros
  y la lista de reserva que la pantalla de licencias muestra cuando el catálogo embebido no se
  puede leer, iguales a lo que el proyecto compila, versión incluida, y las licencias de
  autenticación del servicio de copias de seguridad siguen declaradas porque esa funcionalidad se
  usa.

## Requisitos no funcionales

Heredados de la spec madre, sin modificación:

- **RNF-1 — Sin telemetría en el artefacto.** El artefacto que se distribuye no contiene ningún
  componente de analítica ni de telemetría. Verificable: la resolución del árbol de dependencias del
  módulo principal para la variante de publicación, y el análisis del artefacto de esa misma
  variante, no reportan nada de analítica ni de telemetría.
- **RNF-4 — Verificabilidad.** Cada requisito funcional tiene una comprobación reproducible.
- **RNF-6 — Sin coste nuevo.** La entrega no añade dependencias, tareas de compilación ni procesos
  de compilación nuevos. Verificable: el conjunto de dependencias solo disminuye, el catálogo de
  versiones pierde entradas y no las gana.

## Casos límite

1. **Búsqueda que encuentra el artefacto de configuración del servicio de copias de seguridad.** Una
   búsqueda de analítica o telemetría en el repositorio devuelve legítimamente ese artefacto, que
   no se toca. Quien lea el resultado no debe deducir que hay que borrarlo: su ausencia rompe la
   compilación.
2. **Dependencia de telemetría comentada.** Una dependencia de analítica que solo sobrevive como
   entrada comentada y como anuncio en los inventarios desaparece por completo de la configuración y
   de los tres inventarios.
3. **Plataforma de versiones huérfana.** Retirado el último artefacto de una familia, su plataforma
   se retira también: dejarla sería una entrada que anuncia algo que el proyecto ya no compila.
4. **Elemento del inventario que no depende de la analítica.** Los elementos de los inventarios
   cuya dependencia se conserva siguen ahí, aunque su versión announced sea incorrecta: en ese
   caso lo que se corrige es la versión, no la entrada.
5. **Lista de reserva que nadie ve.** La lista que la pantalla de licencias muestra cuando el
   catálogo embebido no se puede leer casi nunca se ve, pero es lo que la persona usuaria lee si
   ocurre. Se corrige igual que los otros dos inventarios.

## Fuera de alcance

- **El cliente de llamadas HTTP declarado y sin usar.** Es la misma categoría que la analítica
  (declarado, sin usar, anunciado en los dos inventarios), pero retirarlo es una decisión de
  dependencia de proyecto, no un remedio de la constitución: quizá se quiera usar en el futuro. Tiene
  su propia spec. Sus licencias se conservan.
- **Las demás dependencias comentadas y sin usar** (permisos, cámara ×4, almacenamiento de
  preferencias, ubicación). Se quedan como están. Tienen spec pendiente, que es la que debe cerrar la
  comprobación completa del principio 1. Heredado de la madre.
- **La deriva de la plataforma de Compose en la guía de ingeniería.** La guía de ingeniería también
  anuncia la versión antigua. Queda fuera porque esta entrega no reescribe las guías de ingeniería;
  lo recoge la spec que las mantenga al día. Heredado de la madre.
- **Retirar el plugin de configuración del servicio de copias de seguridad ni el artefacto que
  exige.** El plugin está aplicado y sin el artefacto la compilación falla: la afirmación de la
  documentación operativa que lo declara obligatorio es correcta y no se toca.
- **Tocar las licencias y las dependencias de autenticación**: se usan en el respaldo de Drive y no
  entran en esta entrega.
- **Reintroducir telemetría**, aunque sea anónima, opcional o con interruptor.
- **El registro del sistema y los tests unitarios**: son las specs 004 y 002 respectivamente.

## Criterios de finalización

- [ ] La resolución del árbol de dependencias del módulo principal para la variante de publicación
      no lista ningún artefacto de analítica ni de telemetría, y el análisis del artefacto de esa
      variante tampoco los reporta.
- [ ] Una búsqueda de dependencias de analítica o telemetría en la configuración del proyecto y en
      el catálogo de versiones no devuelve ninguna entrada, ni activa ni comentada, y ninguna
      plataforma de versiones queda sin ningún artefacto asociado. La búsqueda excluye el
      artefacto de configuración del servicio de copias de seguridad, que debe seguir presente.
- [ ] El catálogo de licencias embebido, el inventario de licencias de terceros y la lista de
      reserva que la pantalla de licencias muestra anuncian las mismas versiones que el catálogo de
      versiones, y ninguno de los tres contiene entradas de telemetría.
- [ ] Las licencias de autenticación del servicio de copias de seguridad siguen declaradas en el
      catálogo de licencias embebido, en el inventario de terceros y en la lista de reserva.
- [ ] La tarea de ensamblado de la aplicación de depuración termina correctamente.
- [ ] Ninguna línea bajo `app/src/main/java` ha cambiado, salvo la lista de reserva de la pantalla de
      licencias.

## Dudas abiertas

Ninguna. Todas las decisiones de esta spec están tomadas en la spec madre y en su plan.