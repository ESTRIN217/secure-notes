# Spec 021 — Los atajos del lanzador: cinco entradas al app sin abrirlo antes

Estado: **implementada** (2026-10-16), T1-T7 cerradas con 31 pruebas en verde; **T8 con la suite completa**;
**sin verificación en dispositivo** (el guion está al final)

> **Nota de procedencia.** No encadena con ninguna spec anterior: es la primera que toca el **punto de
> entrada** de la app por fuera del icono. Los RF más altos en uso son `RF-123` y `RNF-70` (spec 020), así que
> aquí empiezan en `RF-124` y `RNF-71`.
>
> **No reescribe** ningún RF vigente. Lo único que roza es el candado de la bóveda (constitución 5), y por eso
> los cinco atajos pasan por el mismo sitio que los widgets: ver RF-127.
>
> **Atajos del lanzador, no de teclado.** La primera lectura de la petición fue «atajos de teclado» y no es lo
> que se va a construir: esto es la pulsación larga sobre el icono de la app. La app **no tiene hoy ninguna capa
> de atajos de teclado** y esta spec no la crea.

## Contexto y objetivo

La app solo se abre por un camino: tocar su icono. Todo lo demás —la lista, el editor, los widgets— son
superficies a las que se llega **desde dentro**. Eso deja tres acciones muy frecuentes a un pulso largo de
distancia: escribir una nota nueva, buscar algo, y abrir el chat con la IA.

El sistema ya ofrece la infraestructura: los *app shortcuts*. Son declarativos —un fichero XML y una línea en el
manifiesto—, no enredan con la base de datos ni con el cifrado, y aparecen en la pulsación larga del icono de
cualquier lanzador. El objetivo es que la app exponga **cinco de esos atajos**, y que se comporten como si
hubieran sido pulsados dentro de la app: con el mismo candado delante y la misma pantalla de destino.

Hay dos límites del sistema que condicionan el diseño y conviene tener escritos antes de implementar:

- **Solo las Activities `MAIN`/`LAUNCHER` pueden tener atajos**, y `CodeEditorActivity` solo declara
  `SEND`/`VIEW`. Por eso los cinco atajos apuntan a `MainActivity`, que sí es principal, y el del editor de
  código se resuelve **desde dentro**, lanzando esa `Activity`. No se puede apuntar a ella directamente sin
  convertirla en principal, y eso significaría un **segundo icono en el lanzador**.
- **La mayoría de lanzadores muestran cuatro atajos a la vez** —el de un Infinix muestra **tres**—, aunque se
  declaren cinco. Por eso el orden importa: los tres primeros son los que se ven, y por eso el del editor de
  código va el último.

## Usuarios

- Quien usa la app a diario: quiere **escribir una nota o buscar algo sin recorrer la app**, y lo hace con la
  pantalla apagada en la mano, sin desbloqueo previo.
- Quien protege la bóveda con contraseña: espera que **un atajo no sea una puerta trasera**. Hoy la app ya
  bloquea la pantalla entera; un atajo no puede ser la excepción.
- Quien mantiene la app: quiere los atajos **declarados, no cableados**, para que se puedan quitar o reordenar
  tocando un XML.

## Definiciones

- **Atajo**: una entrada de la pulsación larga del icono de la app. **Estático** significa declarado en un
  recurso, no publicado en tiempo de ejecución.
- **Destino**: la pantalla a la que un atajo lleva. Es un valor de un conjunto cerrado (`new_note`, `search`,
  `ai_chat`, `settings`, `code_editor`), no una ruta ni un nombre de clase que la app interprete por su cuenta.
- **Pendiente**: un destino recibido mientras la bóveda está bloqueada, que espera a que se desbloquee. No se
  guarda en disco y no sobrevive a la muerte del proceso.
- **Cerrada** / **abierta**: estado de la bóveda. Sin contraseña maestra configurada, la bóveda está siempre
  abierta y no hay candado que enseñe.

## Requisitos funcionales

- **RF-124**: CUANDO se consulte la pulsación larga del icono de la app, EL SISTEMA ofrece **cinco atajos
  estáticos** en este orden: **chat con la IA, ajustes, nueva nota**, buscar y editor de código; y cada uno lleva
  una etiqueta corta **en el idioma configurado por el usuario**. Verificable: un invariante que lee
  `res/xml/shortcuts.xml` y comprueba que hay exactamente cinco `<shortcut>` **en ese orden**, con `shortcutId`
  **literal** (no puede ser un `@string`, según la plataforma), único, y con `shortcutShortLabel` apuntando a una
  cadena que existe en `values/` **y en los nueve locales**; y otro invariante que comprueba que el manifiesto
  declara `android.app.shortcuts` dentro de una Activity que tenga `MAIN` + `LAUNCHER`.
  > **El orden lo fijó quien Decide mirando su lanzador** (2026-10-16): el suyo, un Infinix, muestra **tres**
  > atajos, no cuatro. Los tres primeros del XML son los que importan de verdad y los otros dos quedan al final,
  > alcanzables arrastrándolos al escritorio.

- **RF-125**: CUANDO el usuario pulse cualquiera de los cinco atajos, EL SISTEMA abre la app y la lleva al
  **destino** correspondiente: una nota nueva, la búsqueda, una conversación nueva con la IA, los ajustes o el
  editor de código. Verificable: una función pura `destinoDesde(valor: String?)` que devuelve el destino de los
  cinco valores y `null` para cualquier otra cosa, con una prueba por cada valor **y** un invariante que
  comprueba que los cinco valores del XML son exactamente los que el parser acepta —un literal del XML sin brazo
  en el parser es un atajo que no hace nada y no se rompería nunca por sí solo—.

- **RF-126**: CUANDO se pulse el atajo de chat con la IA, EL SISTEMA abre **una conversación nueva**, igual que
  el botón de la lista, y no reanuda la última. Verificable: la ruta de navegación del destino crea la sesión
  antes de navegar, y un invariante comprueba que reutiliza el mismo camino que el botón de la lista en lugar
  de duplicarlo.

- **RF-127**: MIENTRAS la bóveda esté **cerrada**, EL SISTEMA **no ejecuta el destino**: muestra el candado y
  guarda el destino como pendiente; y en cuanto la bóveda se abre, ejecuta el pendiente **una sola vez**. Esto
  incluye al atajo del editor de código: `MainActivity` abre el editor **después** de desbloquear, aunque la
  `Activity` del editor no tenga candado propio. Verificable: pruebas puras de un almacén del destino pendiente
  —guardar mientras está cerrada, escupir al abrirse, quedar vacío después de escupir, y **gana el último** si
  llegan dos— más un invariante que comprueba que el volcado ocurre en el efecto que reacciona al
  desbloqueo y no en uno que corre una sola vez.

- **RF-128**: EL SISTEMA ejecuta el destino tanto si la app **no estaba abierta** como si **ya lo estaba y está
  en primer plano en cualquier pantalla**, por el mismo camino de código en los dos casos. Verificable: un
  invariante que comprueba que el arranque en frío y el `Intent` posterior llaman **a la misma función**.

- **RF-129**: SI el Intent llega sin destino, con un destino vacío o con uno que el sistema no reconoce —que
  es lo que pasa si otra app construye el Intent a mano—, ENTONCES EL SISTEMA abre la app en la lista y **no
  falla**. Verificable: la función pura del parser devuelve `null` en los tres casos y no lanza.

- **RF-130**: EL SISTEMA nunca confunde un atajo con una apertura de archivo. Verificable: un invariante que
  comprueba que ningún `<intent>` de `shortcuts.xml` declara `android:data` ni `android:mimeType`, porque la app
  ya tiene filtros `VIEW` que abrirían el importador de archivos con el mismo `Intent` de acción.

## Requisitos no funcionales

- **RNF-71**: **Cero dependencias nuevas.** Ni `ShortcutManagerCompat`, ni la librería de Google Shortcuts: los
  atajos son estáticos y declarativos. Verificable: `git diff` no toca ningún `build.gradle.kts`, igual que en
  la spec 020.

- **RNF-72**: **Cero cadenas nuevas.** Las cinco etiquetas salen de claves que ya existen y que ya están en los
  nueve locales. Verificable: `git diff` no toca ningún `strings.xml`, y un invariante comprueba que las cinco
  claves usadas existen en los nueve.

- **RNF-73**: La lógica que decide qué hacer con un destino (parsear el valor, decidir si se ejecuta o queda
  pendiente, recordar el último) vive en ficheros **que se prueban sin Robolectric**, porque aquí la suite con
  entorno no se ejecuta y son las pruebas puras las que corren. El fichero que decide destinos **no importa
  `androidx.compose`**.

- **RNF-74**: Resolver un atajo **no lee ni descifra notas**: el parseo del destino y la decisión de
  ejecutarlo o guardarlo **no tocan la base de datos ni `CipherService`**, porque abrir la lista, la búsqueda o
  los ajustes no necesita leer nada. El único destino que escribe algo es el de la IA, y **solo porque reutiliza
  el camino del botón de la lista**, que ya crea una conversación (RF-126): venir de un atajo no añade ninguna
  escritura. Verificable: un invariante que comprueba que el camino de la decisión no llama a ningún DAO ni a
  `CipherService`.

- **RNF-75**: Funciones de ≤ 20 líneas, guard clauses y **cero datos del usuario en los registros de log**
  (constitución 5). Un destino no lleva ningún dato: es una palabra de un conjunto cerrado.

## Casos límite

1. **App cerrada y proceso muerto**: el atajo arranca la app en frío y se ejecuta el destino. Es el camino más
   frecuente y el que no se puede probar con pruebas puras.
2. **App abierta en cualquier pantalla**: el atajo navega desde donde esté, incluido el editor de una nota.
3. **Bóveda sin contraseña maestra**: no hay candado, así que el destino se ejecuta en el acto. No es un caso
   especial en el código: es «ya abierta».
4. **Dos atajos seguidos con la bóveda cerrada**: gana el último. El primero se descarta; ejecuta los dos sería
   saltar de una pantalla a otra sin que nadie lo pidiera.
5. **El usuario nunca desbloquea**: el destino se queda pendiente y no ocurre nada. No hay expiración, ni aviso,
   ni escritura en disco.
6. **Rotación con un destino pendiente**: sigue pendiente. El pendiente vive en el `companion` de
   `MainActivity`, no en el estado de la pantalla, que es lo que sobrevive a una rotación.
7. **El proceso muere entre el atajo y el desbloqueo**: el destino se pierde y el usuario aterriza en la lista.
   Es una limitación **aceptada**, idéntica a la del sobre de notas que ya espera en `cacheDir`.
8. **Destino ausente, vacío o desconocido**: lista, sin fallo (RF-129).
9. **Chat con la IA sin modelo descargado**: la pantalla de chat enseña su propio camino, sin nada nuevo aquí.
10. **Lanzador que solo muestra cuatro**: el del editor de código puede no verse sin desplazar. Es el orden
    indicado a propósito, y cambiarlo es mover una línea del XML.
11. **El atajo se pulsa con el editor de código abierto**: `MainActivity` es `singleTop`, así que no está en la
    cima de la pila y se crea una instancia nueva encima. El destino se cumple; la instancia anterior queda
    debajo. `shortcuts.xml` **no admite banderas** de `Intent`, así que no se puede pedir `CLEAR_TOP`.
12. **Otra app construye el Intent**: `MainActivity` es `exported="true"` desde antes, por los filtros de
    apertura de archivos. Fabricar un destino no salta el candado (RF-127).

## Fuera de alcance

- **Atajos de teclado.** Se preguntó por ellos primero y no hay ninguno en la app: construirlos es otra spec.
- **Atajos dinámicos o fijados** (`ShortcutManagerCompat`, `requestPinShortcut`) y sugerencias del Asistente
  (`capability-binding`).
- **Un candado dentro de `CodeEditorActivity`.** Es un hueco **preexistente** y real: la `Activity` es
  `exported="true"` con filtros `SEND`/`VIEW`, así que hoy se puede abrir desde fuera sin contraseña. Esta spec
  lo **no** arregla ni lo empeora, y queda anotado como defecto conocido para su propia spec.
- **Convertir `CodeEditorActivity` en `Activity` principal** para que pueda declarar sus propios atajos: eso
  pone un segundo icono en el lanzador y es una decisión de producto, no un atajo.
- **Más de cinco atajos.** Cinco es lo que la plataforma admite de forma estable.
- **Cualquier texto nuevo**: ni etiquetas, ni mensajes de atajo deshabilitado.

## Criterios de finalización

- `res/xml/shortcuts.xml` con cinco atajos, cada uno con `shortcutId` literal, etiqueta corta de las nueve
  traducciones, icono vectorial propio y un `<intent>` que apunta a `MainActivity`.
- `AndroidManifest.xml` declara `android.app.shortcuts` en la `Activity` principal.
- Pruebas puras del parser de destinos y del destino pendiente en verde; invariantes del XML, del manifiesto y
  de las etiquetas en verde.
- `:app:compileDebugUnitTestKotlin` en verde y **ninguna regresión** en la cuenta de fallos de la suite
  (`:app` 711 / 41, `:editor-de-codigo` 145 / 2, con los mismos 41 de plataforma).
- `git diff` no toca ningún `build.gradle.kts` ni ningún `strings.xml`.
- **Sin verificar en dispositivo.** Lo único que ninguna prueba pura comprueba es que el lanzador muestre los
  cinco y que el destino se cumpla de verdad: mantener pulsado el icono.

## Dudas abiertas

- Ninguna bloqueante para implementar.
- **Pendiente de mirar en dispositivo**: cuántos atajos muestra de verdad el lanzador del dispositivo de quien
  Decide. Si muestra cuatro, el del editor de código queda fuera y habrá que decidir si se sube de sitio.

## Verificación en dispositivo (T8, 2026-10-16)

Lo único que **ninguna** prueba puede comprobar es que el lanzador muestre los atajos y que el destino se cumpla
de verdad. En este orden, con la app instalada:

1. Mantener pulsado el icono del lanzador: **deben salir cinco**, en el orden de la spec, con sus etiquetas en
   el idioma configurado.
2. Con el móvil **sin tocar la app**, pulsar cada uno y comprobar que abre en su destino.
3. **Sin bóveda** (sin contraseña maestra): el destino se cumple en el acto.
4. **Con bóveda cerrada**: sale el candado y **no** se cumple nada; al desbloquear, se cumple **una sola vez**.
   Girar el móvil con el candado puesto **no debe** cambiar nada (es el `removeExtra`, RF-128).
5. Con la app **abierta y en el editor** de una nota, pulsar un atajo: navega desde donde esté.
6. Repetir el atajo de la IA y **girar el móvil**: no debe aparecer una conversación nueva (si aparece, el
   `removeExtra` ha dejado de funcionar).
7. El atajo del **editor de código**: si sale el candado, la bóveda manda; si abre directo, es el hueco
   preexistente de `CodeEditorActivity` documentado en «Fuera de alcance».