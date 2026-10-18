# Spec 028 — El tema elegido viaja en la copia de seguridad

Estado: **implementada y verificada en dispositivo** (2026-10-18), T1–T8 cerradas. El guion de ocho pasos de
`plan.md` se ejecutó entero y cumplió. RF-187…RF-193, RNF-120…RNF-124.

> **Nota de procedencia.** No encadena con ninguna spec anterior. Los RF más altos en uso son `RF-186` y `RNF-119`
> (spec 027), así que aquí empiezan en `RF-187` y `RNF-120`.
>
> **No añade dependencias** ni claves de texto: es una preferencia más en el respaldo más una pieza compartida con
> sus pruebas.
>
> **Origen.** Pregunta del usuario sobre qué guarda la copia de seguridad y qué no: el tema elegido no estaba entre
> los ajustes que viajan, y se añadió en la spec 025 (RF-163) sin que la copia se enterara.

## Contexto y objetivo

La copia de seguridad de la app lleva un bloque `settings` con **trece** preferencias: modo oscuro, colores
dinámicos, idioma, comprobación de actualizaciones, avisos de actualización, orden personalizado, tipo de
contraseña, biometría, capturas de pantalla, tiempo de bloqueo, cifrar copias y copias automáticas con su
intervalo.

El **tema elegido** (spec 025, RF-163) no está entre ellas. Consecuencia: quien chose un tema, hace una copia,
reinstala o cambia de móvil y la restaura recibe sus notas y sus ajustes, pero la app se abre con el tema por
defecto de la instalación nueva. El tema es la única preferencia de aspecto que no sobrevive al cambio de
dispositivo.

Detrás del olvido hay una causa concreta: **la lista de thirteen preferencias está replicada en cinco sitios** —
tres que construyen la copia (la local que se comparte, la manual de Drive y la automática de Drive) y dos que la
restauran (importar un fichero y restaurar de Drive). Hoy las cinco listas son idénticas, y `theme_id` falta en
las cinco. La siguiente preferencia que se añada se olvidará igual.

A esto se añade que la copia de seguridad **propia de Android está totalmente excluida** (preferencias, base de
datos y ficheros), así que la copia de la app es la única que restaura alguien: no hay una segunda red que
respalde el tema.

**Objetivo:** que el tema elegido viaje en la copia y se restaure, y que la lista de preferencias que viajan esté
escrita **en un solo sitio** para que no vuelva a olvidarse ninguna.

## Usuarios

- **HU-1.** Como quien eligió un tema, quiero que vaya en la copia de seguridad, para que al cambiar de móvil la app
  se vea igual que en el anterior.
- **HU-2.** Como quien restaura una copia hecha con un tema que esta versión ya no tiene, quiero que conserve el
  tema que tengo ahora, para que una copia antigua no me cambie el aspecto de la app sin haberlo pedido.
- **HU-3.** Como quien restaura una copia anterior a que el tema existiera en las copias, quiero que no cambie nada
  del tema, para que restaurar siga siendo seguro con cualquier copia.

## Definiciones

- **Copia de seguridad**: el fichero que produce la app (ZIP con `backup.json` dentro, o el JSON suelto), sea la
  copia local que se comparte, la de Drive a mano o la automática de Drive. **No** es la copia de seguridad propia
  de Android, que está excluida por completo.
- **Tema**: el que se elige en Ajustes desde la spec 025. En la copia viaja su **identificador**, no su paleta.
- **Copia antigua**: una copia cuyo bloque de ajustes no trae tema, porque se hizo antes de esta spec.
- **Tema desconocido**: un identificador de tema que esta versión de la app no tiene en su catálogo, porque el
  tema se renombró, se borró, o porque la copia viene de una versión más nueva.

## Requisitos funcionales

- **RF-187**: CUANDO se cree una copia de seguridad —local, de Drive a mano o automática—, EL SISTEMA guarda en ella
  el identificador del tema elegido, junto al resto de preferencias.
- **RF-188**: CUANDO se restaure una copia, EL SISTEMA aplica el tema que trae, si esa versión de la app tiene ese
  tema en su catálogo.
- **RF-189**: SI la copia trae un tema desconocido para esta versión —o viene vacío—, ENTONCES EL SISTEMA **no
  escribe nada** y conserva el tema actual, sin error, sin aviso y sin que la restauración falle por ello.
- **RF-190**: SI la copia no trae tema, ENTONCES EL SISTEMA conserva el tema actual, como hoy.
- **RF-191**: EL SISTEMA decide **qué preferencias viajan y cuáles se aplican al restaurar** con **una sola**
  definición, y ninguna de las tres formas de copia ni de las dos restauraciones vuelve a escribir esa lista.
- **RF-192**: EL SISTEMA escribe el tema restaurado en las preferencias por el mismo camino que el resto de ajustes
  que restaura, de modo que se vea al abrir la app, sin repintar la pantalla en caliente.
- **RF-193**: EL SISTEMA mantiene el resto del comportamiento de la copia: mismos datos de notas, mismas
  etiquetas, misma fusión al restaurar y **misma versión de formato**, para que las copias ya existentes se
  sigan restaurando.

## Requisitos no funcionales

- **RNF-120**: La definición compartida es una pieza **pura** con sus pruebas, sin `android` ni `Context`: qué
  preferencias viajan, cómo se lee una de una copia y si un tema existe en el catálogo se deciden sin disco y sin
  Robolectric, que no se ejecuta en esta plataforma.
- **RNF-121**: Cero dependencias nuevas y cero textos visibles nuevos. Ningún `build.gradle.kts`, `libs.versions.toml`
  ni `strings.xml` cambia: el identificador del tema no es un texto de la interfaz.
- **RNF-122**: El formato **sigue siendo la versión 4**. Añadir una clave opcional es compatible hacia atrás: una
  copia nueva se restaura en esta versión y una copia vieja, en la que ya se restaura, se sigue restaurando aquí.
- **RNF-123**: `./gradlew test` no añade ninguna clase roja nueva: los fallos de plataforma y los preexistentes
  siguen siendo **los mismos** que antes de esta spec.
- **RNF-124**: Las tres formas de copia producen **exactamente las mismas** preferencias. Hoy también, pero por
  copia manual de la lista; con RF-191 pasa a ser una consecuencia de que hay una sola definición.

## Casos límite

- **Copia sin bloque de ajustes**: se restauran notas y etiquetas y el tema no se toca (RF-190).
- **Copia con el identificador de tema en blanco**: es un tema desconocido (RF-189), no un tema «sin elegir».
- **Copia con un tema desconocido**: se aplica todo lo demás de la copia, el tema actual **no** se toca y la
  restauración termina con éxito (RF-189).
- **Copia de una versión más nueva que tiene un tema que esta no**: mismo caso anterior; se conserva el actual.
- **Copia cifrada**: el tema viaja dentro del sobre cifrado, igual que el resto, y con él la contraseña no cambia
  de sitio.
- **Copia sin contraseña maestra**: el bloque de ajustes viaja en claro dentro del JSON, como ya pasa hoy. El
  identificador del tema no es un dato sensible.
- **Importar un `.json` suelto sin adjuntos**: mismo camino de ajustes (RF-188).
- **Restaurar dos veces la misma copia**: el tema se aplica las dos veces y el resultado es el mismo.
- **Restaurar una copia hecha con el tema por defecto de otra versión**: si esa versión lo llama `dynamic` y esta
  también, se aplica y no se ve cambio, que es lo correcto.
- **Preferencias heredadas de colores dinámicos**: una copia antigua trae `dynamic_colors` pero no trae tema. Se
  sigue aplicando la regla de la spec 025 —sin tema guardado, el interruptor antiguo apagado significa el tema
  `default`—, y esta spec no la cambia. En cuanto la copia trae tema, el tema manda sobre el interruptor, también
  como en la spec 025.

## Fuera de alcance

- **Las demás preferencias que hoy no viajan**: la configuración de IA (backend, endpoint, modelo, prompt,
  muestreo), la del editor de código, la de los widgets y el estado de la burbuja flotante. Esta spec añade **el
  tema** y arregla **la causa** por la que se olvidó una preferencia; con RF-191, añadir cualquiera de las demás
  pasa a ser una línea en la definición compartida, pero añadirlas es otra decisión.
- **La contraseña maestra y el vínculo con Drive**: siguen fuera de la copia, por diseño.
- **Aplicar el tema sin reiniciar**: el tema restaurado se ve al abrir la app, como el modo oscuro y el idioma que
  la copia ya restaura (RF-192).
- **Avisar de que el tema se aplicará al reiniciar**: sería un texto nuevo en los nueve locales para algo que el
  resto de los ajustes restaurados tampoco avisa (RNF-121).
- **La copia de seguridad propia de Android**: continúa excluida por completo.

## Criterios de finalización

- Una copia creada con un tema no trivial lo lleva dentro, tanto si es local, de Drive a mano o automática
  (RF-187, RNF-124).
- Al restaurarla en una instalación nueva, la app se abre con el tema que traía (RF-188).
- Al restaurar una copia con un tema desconocido o sin tema, el tema actual no cambia y la restauración no falla
  (RF-189, RF-190).
- La lista de preferencias que viajan está escrita en **un solo sitio**, y las tres formas de copia y las dos
  restauraciones no la repiten (RF-191, comprobado por invariante de fuentes).
- La definición compartida se prueba sin Robolectric, `:app` no suma ninguna clase roja nueva y no cambia ningún
  fichero de dependencias ni de textos (RNF-120, RNF-121, RNF-123).

## Dudas abiertas

- **[RESUELTA] ¿Qué hacer si la copia trae un tema que esta versión no tiene?** **No escribir nada y conservar el
  tema actual** (RF-189), decidido con quien Decide. La alternativa descartada: escribir el identificador tal cual
  y dejar que la resolución de la spec 025 caiga al tema dinámico. Sería más automático, pero una copia antigua
  pisaría el tema de alguien sin que lo hubiera pedido, y «el tema que elegiste en tu copia» no puede ser algo que
  la app no tiene.
- **[RESUELTA] ¿El tema se ve al restaurar o hay que reiniciar?** **Al abrir la app** (RF-192), igual que el modo
  oscuro y el idioma. La alternativa descartada: repintar en caliente. Exige un puente entre la restauración y el
  modelo de tema de cada `Activity`, que hoy no existe, y haría que el tema sea el único ajuste que se ve al
  instante y los otros trece no.

## Cierre

**Qué queda comprobado aquí.** La pieza pura decide, con sus pruebas, lo que es el corazón de la spec: qué viaja,
qué se aplica, y qué **no** se aplica cuando la copia trae un tema desconocido o no trae tema (RF-187…RF-190,
RNF-120). Los invariantes de fuentes comprueban el cableado que ninguna prueba de comportamiento puede ver
(RF-191), y `:app` queda en **842/41**: los 816/41 de antes más **26** pruebas nuevas, con los mismos 41 fallos
—38 de plataforma y 3 preexistentes—, ninguno en las clases de esta spec (RNF-123). El `version` del formato
sigue siendo **4** y no se tocó ni un `strings.xml`, ni un `build.gradle.kts`, ni un `res/xml` (RNF-121, RNF-122).

**Verificación en dispositivo.** El guion de ocho pasos de `plan.md` **se ejecutó entero** el 2026-10-18 y cumplió
(RF-187…RF-191): la copia creada con un tema no trivial lleva su identificador dentro; restaurada en una
instalación limpia, la app se abre con el tema que traía; una copia con `theme_id` desconocido o con el
`theme_id` borrado deja el tema actual intacto y aplica el resto de los ajustes; y la copia manual y la
automática de Drive traen el mismo `theme_id`.

**Lo que salió por el camino y no estaba previsto.** Tres cosas de T3, T5 y T8 quedaron escritas en `tasks.md`
porque son las que más costarían de repetir: el doble de `SharedPreferences` resultó **no** necesitar Robolectric (y
son 60 líneas, no 25); un invariante del plan era **falso** —`SharedPreferencesRepository.getAppTheme()` menciona
legítimamente el tema y el interruptor antiguo—, así que se reformuló a **declaraciones y no menciones**; y la
cuenta de fallos de plataforma que estaba en `AGENTS.md` era **40** cuando son **38**.