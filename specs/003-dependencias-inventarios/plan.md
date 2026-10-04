# Plan — Spec 003 · Dependencias e inventarios

Spec: `specs/003-dependencias-inventarios/spec.md` · Madre: `specs/001-limites-constitucion/`
(**aprobada**)

Este plan es el bloque «Fase A» del `plan.md` de la spec madre (sección 3), movido tal cual y
ajustado a la nueva numeración de tareas.

---

## 1. Rastro de las tareas

La spec 001 numeró estas tareas T6-T7. Aquí se renumeran a **T1-T2** dentro de la 003:

| Tarea 003 | Era | Tarea 001 |
|---|---|---|
| **T1** | → | T6 |
| **T2** | → | T7 |

## 2. Dependencia entre specs

Esta spec **no depende de la 002**. Se puede ejecutar en paralelo. Termina antes que las otras
dos: son las únicas seis líneas de configuración de toda la entrega madre.

## 3. Archivos y responsabilidades

| Archivo | Cambio | RF |
|---|---|---|
| `app/build.gradle.kts` | borrar 3 líneas de Firebase (100, 131, 132) | RF-1, RF-2, RF-3 |
| `gradle/libs.versions.toml` | borrar 3 entradas + la versión `firebaseBom` (42, 113, 114, 115) | RF-3 |
| `app/src/main/assets/oss-licenses.json` | borrar la entrada Firebase (175-…) y corregir 3 versiones (16, 24, 120) | RF-4 |
| `app/src/main/java/com/example/util/OssLicenses.kt` | corregir 2 versiones en la lista de reserva (55, 56) | RF-4 |
| `THIRD-PARTY-NOTICES` | borrar la entrada Firebase (61) y corregir 3 versiones (17, 18, 39) | RF-4 |

## 4. Qué se borra y qué no

### 4.1 Se borra (RF-3)

| Archivo | Línea | Contenido |
|---|---|---|
| `app/build.gradle.kts` | 100 | `implementation(platform(libs.firebase.bom))` |
| `app/build.gradle.kts` | 131 | `// implementation(libs.firebase.ai)` |
| `app/build.gradle.kts` | 132 | `implementation(libs.firebase.analytics)` |
| `gradle/libs.versions.toml` | 42 | `firebaseBom = "34.16.0"` |
| `gradle/libs.versions.toml` | 113 | `firebase-bom` |
| `gradle/libs.versions.toml` | 114 | `firebase-ai` |
| `gradle/libs.versions.toml` | 115 | `firebase-analytics` |

**La plataforma de versiones se va porque queda huérfana.** Es el caso límite 3 de la spec: si se
retira el último artefacto de una familia, su plataforma deja de anunciar algo que el proyecto
compila. Si se dejara, el catálogo de versiones seguiría persuading de que existe una familia de
dependencias de ese servicio que ya no está.

`firebase-ai` estaba comentado, pero se borra igualmente: RF-3 dice «ni entrada comentada», y una
entrada comentada es exactamente lo que permite reintroducir la dependencia por accidente al
copiar y descomentar una línea.

### 4.2 No se toca

| Elemento | Por qué |
|---|---|
| `alias(libs.plugins.google.services)` (`app/build.gradle.kts:12`) | Está aplicado. Sin `app/google-services.json` la compilación **falla**: el plugin exige el archivo. |
| `app/google-services.json` | Configuración local, no versionada por diseño (`.gitignore` raíz y `app/.gitignore`). No es una dependencia; RF-3 dice explícitamente que no le aplica. |
| `implementation(libs.play.services.auth)` | Se usa en el respaldo de Drive. |
| `libs.googleid` | Se usa. |
| `libs.androidx.credentials.play.services.auth` | Se usa. |
| Sus licencias en los tres inventarios | RF-4 las exige: «siguen declaradas porque esa funcionalidad se usa». |
| `implementation(libs.retrofit)` y su entrada en los inventarios | Fuera de alcance por Decisión 9 de la madre: es una decisión de dependencia de proyecto, no un remedio de la constitución. |

**Aviso para quien ejecute la comprobación.** `grep -ri firebase .` sobre el repositorio
devuelve legítimamente `app/google-services.json`, que es la configuración del servicio de copias
de seguridad y debe seguir ahí. El criterio de finalización lo dice, pero conviene no "arreglar" el
resultado borrando un archivo cuya ausencia rompe la compilación.

## 5. La deriva de versiones (RF-4)

`gradle/libs.versions.toml` es la fuente de verdad. Los tres valores se corrigen en los tres sitios
donde se anuncian:

| Librería | En el catálogo (correcto) | En los inventarios (erróneo) | Dónde corregir |
|---|---|---|---|
| Coil 3 | `3.6.3` (línea 21) | `3.5.0` | `oss-licenses.json:120`, `THIRD-PARTY-NOTICES:39` |
| Plataforma de Compose | `2026.09.00` (línea 12) | `2026.06.01` | `oss-licenses.json:16`, `OssLicenses.kt:55`, `THIRD-PARTY-NOTICES:17` |
| Material 3 | `1.5.0-alpha28` (línea 55) | `1.5.0-alpha25` | `oss-licenses.json:24`, `OssLicenses.kt:56`, `THIRD-PARTY-NOTICES:18` |

### Por qué `OssLicenses.kt` entra en alcance

`OssLicenses.kt:53-70` es la función `fallbackLicenses()`: la lista que **la propia pantalla de
licencias muestra** cuando el catálogo embebido no se puede leer. No es un comentario interno ni una
constante de compilación: es contenido que ve la persona usuaria en la pantalla de licencias. Por
eso está en RF-4, que menciona la lista de reserva explícitamente.

Sin corregirla, una persona que abra la pantalla de licencias con el catálogo embebido ilegible
vería «Material 3 1.5.0-alpha25» y «Coil 3.5.0» cuando la app lleva 1.5.0-alpha28 y 3.6.3.

### Por qué `docs/GUIDELINES.md` queda fuera

La guía de ingeniería (línea 10) también anuncia `BOM 2026.06.01`. Queda fuera de alcance porque la
madre excluye reescribir las guías de ingeniería: mantener las guías al día es trabajo de su propia
spec, no de un remedio de la constitución. Se anota aquí para que la deriva sea visible y no se
pierda.

## 6. Verificación

### RF-1 y RF-2 — sin telemetría en el artefacto

Un solo criterio cubre los dos, para no duplicar la comprobación:

```
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -iE "firebase.analytics|firebase-ai"
```

Sin resultados. Y el análisis del artefacto de publicación:

```
./gradlew :app:assembleRelease
# y el análisis de dependencias del APK resultante
```

`key.properties` existe en el entorno de desarrollo, así que ambas son ejecutables aquí. En CI **no**
lo son: `app/google-services.json` y `key.properties` no están versionados, y por eso
`:app:assembleRelease` tampoco puede correr allí. Es la misma razón por la que `:app:test` no está
en el flujo de integración continua.

### RF-3 — retirada completa

```
grep -rn "firebase" app/build.gradle.kts gradle/libs.versions.toml
```

Sin resultados. Los dos archivos no contienen artefactos de configuración, así que la exclusión del
caso límite 1 no hace falta en esta búsqueda concreta; sí hace falta en cualquier búsqueda sobre
todo el repositorio.

### RF-4 — inventarios coherentes

```
grep -rniE "3\.5\.0|2026\.06\.01|1\.5\.0-alpha25" \
  app/src/main/assets/oss-licenses.json THIRD-PARTY-NOTICES \
  app/src/main/java/com/example/util/OssLicenses.kt
```

Sin resultados. Y las licencias de autenticación siguen presentes:

```
grep -c "Play Services Auth" THIRD-PARTY-NOTICES   # > 0
grep -c "googleid\|credentials-play-services-auth" THIRD-PARTY-NOTICES   # > 0
```

## 7. Decisiones con su alternativa descartada

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| D-1 | Borrar también la entrada comentada de `firebase-ai` | dejarla comentada «por si acaso» | RF-3 lo prohíbe; una entrada comentada es la vía de reintroducción por accidente |
| D-2 | Retirar la plataforma de versiones huérfana | dejarla como `platform(...)` vacío | Anunciaría una familia de dependencias que el proyecto ya no compila |
| D-3 | Corregir también `OssLicenses.kt` | solo el catálogo embebido y el inventario | Es la lista que **muestra la pantalla de licencias**; RF-4 la nombra |
| D-4 | No tocar `docs/GUIDELINES.md` | corregir la deriva también allí | El Fuera de alcance de la madre excluye reescribir las guías de ingeniería |
| D-5 | Conservar `Retrofit` y sus licencias | retirarlo por ser «declarado y sin usar» | Decisión 9 de la madre: es una decisión de dependencia, no un remedio de la constitución |
| D-6 | Conservar el plugin y el artefacto de configuración | retirarlos por estar «sin uso» | El plugin está aplicado y sin el artefacto la compilación falla; RF-3 lo excluye explícitamente |
| D-7 | Conservar las licencias de autenticación aunque no cambien | borrarlas por «simplificar» | Se usan en el respaldo; RF-4 las exige |

## 8. Lo que esta spec NO hace

- No añade ni quita ninguna dependencia que no sea de telemetría.
- No toca `Retrofit`, ni las dependencias ni licencias de autenticación.
- No toca el plugin `google-services` ni su artefacto de configuración.
- No toca `docs/GUIDELINES.md`.
- No toca ningún archivo Kotlin de `app/src/main` salvo `OssLicenses.kt`, y en él solo dos cadenas.
- No toca el registro del sistema (spec 004) ni los tests (spec 002).
- No depende de la spec 002: se puede hacer en cualquier momento.