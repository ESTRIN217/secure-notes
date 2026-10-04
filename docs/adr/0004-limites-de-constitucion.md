# ADR-0004 — Límites de constitución: registro del sistema sin contenido

**Estado:** aceptado · **Fecha:** 2026-10-04

## Contexto

`docs/constitution.md` fija 6 principios innegociables, cada uno con su comprobación
ejecutable. Al aplicar el principio 5 —los datos del usuario no salen del dispositivo—
apareció la fuga más grande del proyecto: **143 llamadas `Log.*`** en los 4 módulos, **35**
interpolando contenido de nota o referencia de medio y **114** pasando un objeto de excepción
cuyo texto puede llevar lo mismo (`PlaybackException` con el URI del medio,
`FileNotFoundException` con la ruta, `org.json.JSONException` con un fragmento de
`note.content`). El alcance acordado fue de **23 sitios**, con 13 que interpolan, 10 que pasan
excepción y solapes entre ambos.

## Decisión

1. **El identificador opaco es un contador monotónico por operación** (`op3/i7`), no un hash:
   no tiene que ser reversible ni admitir ataque por diccionario porque no se deriva del
   contenido, así que dos contenidos distintos comparten identificador y el registro correlaciona
   sin identificar. **No lleva `noteId`**: un id de nota es estable y único del material de la
   persona, y volver a ponerlo en el registro reintroduce la correlación que la spec elimina. Un
   sitio sin proceso usa `isolatedNext()`: la llamada es su propia operación.

2. **`failureMessage(accion, categoria, idOpaque, hoy: Class<*>?)` es una función pura que recibe
   la clase de la excepción, nunca la excepción.** Así `e.message` no puede llegar al registro
   por construcción y no por disciplina; la fachada `registrarFallo` solo extrae `e?.javaClass`.
   El mensaje propio describe la **acción** que falló, no el síntoma, y siempre lleva al menos un
   campo interpolado. Efecto aceptado: es la **única** función con casos de prueba autorizada
   fuera de RF-19 (Decisión 22 de `specs/001-limites-constitucion/spec.md`), y las ramas `catch`
   que modifican no tienen cobertura directa.

3. **El helper se replica en cada módulo con sitios del alcance** (`:app`, `:visor-media`,
   `:editor-de-codigo`; ~25 líneas cada uno, el diff entre copias es la línea de `package`) en
   lugar de crear un módulo Gradle compartido: `:app` **depende** de las librerías, así que el de
   `:app` no es visible desde ellas, y un módulo nuevo rompe «sin coste nuevo» (principio 1).
   `visor-pdf` no recibe copia porque no tiene sitios.

4. **Los inventarios de licencias son tres y se corrigen a la vez**:
   `app/src/main/assets/oss-licenses.json`, las versiones que muestra `app/src/main/java/com/
   example/util/OssLicenses.kt` y `THIRD-PARTY-NOTICES`. Ya habían derivado (entradas de
   telemetría, `Retrofit` declarado sin usar) y se corrigieron juntos en `specs/003`.

5. **`docs/constitution.md`**: 6 principios innegociables —stack mínimo · la spec manda ·
   lógica fuera de la UI · tests primero · datos solo en el dispositivo · un idioma por capa—,
   cada uno con la comprobación que lo hace verificable, no solo enunciado. Es la cabecera que ya
   imponían `AGENTS.md`, `GUIDELINES.md` y la skill `sdd`, reunida en un sitio.

## Consecuencias

* El principio 5 queda comprobable: sobre la lista explícita de los 23 sitios, la búsqueda de
  «último argumento = objeto de excepción» da 0 resultados. La lista de los 24 sitios
  **conservados** (`plan.md` 004 §4.2) es la que hace comprobable que el diff no los tocó.
* Un criterio de «cero en todo el árbol» sería incumplible: 94 de las 114 llamadas con excepción
  quedan fuera por decisión expresa de la spec (cifrado, sincronización, reproducción de medios y
  acceso a archivos externos), cada zona con su spec.
* Replicar el helper obliga a aplicar tres veces cualquier cambio de formato; a cambio, ni un
  módulo Gradle nuevo ni una dependencia nueva.
* Cambiar la versión de una dependencia obliga a tocar los tres inventarios en el mismo cambio.
* Queda una puerta manual: `./gradlew test` necesita un host x86_64, porque Robolectric 4.17 no
  distribuye runtime nativo para `linux/aarch64` y toda suite falla antes de ejecutar.
