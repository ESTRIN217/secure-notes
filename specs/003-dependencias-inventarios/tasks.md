# Tasks — Spec 003 · Dependencias e inventarios

Plan: `specs/003-dependencias-inventarios/plan.md` · Madre: `specs/001-limites-constitucion/`
2 tareas, en orden de dependencia. **No depende de la spec 002.**

Cada «Hecho cuando» es un comando o un resultado observable.

## Rastro

Estas tareas son **T6-T7** de la spec 001, renumeradas aquí. Ver `plan.md` §1.

---

- [x] **T1. Retirar Firebase de la configuración y del catálogo de versiones.** RF-1, RF-2, RF-3
  - Borrar `app/build.gradle.kts:100` (`implementation(platform(libs.firebase.bom))`),
    `:131` (`// implementation(libs.firebase.ai)`) y `:132`
    (`implementation(libs.firebase.analytics)`).
  - Borrar de `gradle/libs.versions.toml` las líneas 42 (`firebaseBom = "34.16.0"`), 113
    (`firebase-bom`), 114 (`firebase-ai`) y 115 (`firebase-analytics`).
  - **No tocar** el plugin `google-services` (línea 12) ni `app/google-services.json`: el plugin
    está aplicado y sin el artefacto la compilación falla.
  - **No tocar** Play Services Auth, GoogleId ni credentials-play-services-auth: se usan en el
    respaldo.
  - Hecho cuando: `grep -rn "firebase" app/build.gradle.kts gradle/libs.versions.toml` no devuelve
    ningún resultado, y
    `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -icE "firebase.analytics|firebase-ai"`
    devuelve 0.

- [x] **T2. Inventarios de licencias: quitar la entrada de telemetría y corregir la deriva de versiones.** RF-4
  - Borrar la entrada «Firebase Analytics (BOM)» de `app/src/main/assets/oss-licenses.json`
    (línea 175) y de `THIRD-PARTY-NOTICES` (línea 61).
  - Coil `3.5.0` → `3.6.3`: `oss-licenses.json:120`, `THIRD-PARTY-NOTICES:39`.
  - Plataforma de Compose `2026.06.01` → `2026.09.00`: `oss-licenses.json:16`,
    `OssLicenses.kt:55`, `THIRD-PARTY-NOTICES:17`.
  - Material 3 `1.5.0-alpha25` → `1.5.0-alpha28`: `oss-licenses.json:24`, `OssLicenses.kt:56`,
    `THIRD-PARTY-NOTICES:18`.
  - Conservar las licencias de autenticación de Drive y la entrada de Retrofit.
  - Hecho cuando:
    `grep -rniE "3\.5\.0|2026\.06\.01|1\.5\.0-alpha25" app/src/main/assets/oss-licenses.json THIRD-PARTY-NOTICES app/src/main/java/com/example/util/OssLicenses.kt`
    no devuelve ningún resultado;
    `grep -rn "Firebase Analytics" app/src/main/assets/oss-licenses.json THIRD-PARTY-NOTICES`
    no devuelve ningún resultado;
    `grep -c "Play Services Auth" THIRD-PARTY-NOTICES` sigue siendo mayor que 0; y
    `./gradlew :app:assembleDebug` termina en `BUILD SUCCESSFUL`.

---

## Comprobaciones de cierre de la spec

- [ ] `grep -rn "firebase" app/build.gradle.kts gradle/libs.versions.toml` → vacío
- [ ] `test -f app/google-services.json` → el archivo sigue presente (no se toca)
- [ ] `grep -rn "google-services\|google.services" app/build.gradle.kts` → el plugin sigue aplicado
- [ ] `grep -rn "play.services.auth\|googleid\|credentials.play.services.auth" app/build.gradle.kts` → siguen declaradas
- [ ] `grep -rniE "3\.5\.0|2026\.06\.01|1\.5\.0-alpha25"` sobre los 3 inventarios → vacío
- [ ] `grep -c "Retrofit" app/src/main/assets/oss-licenses.json THIRD-PARTY-NOTICES` → > 0 (conservado)
- [ ] `./gradlew :app:assembleDebug` en verde
- [ ] `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` sin artefactos de telemetría

## Pendiente

Ninguno.