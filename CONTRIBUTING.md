# Contribuir — Secure Notes

## Flujo de trabajo

Trunk-based simplificado: ramas cortas desde `master`, un PR por cambio, merge
directo tras revisión. Cada PR describe **qué** cambia y **por qué**.

## Antes de abrir un PR

1. Lee `AGENTS.md` (operativa), `docs/GUIDELINES.md` (principios) y
   `docs/ARCHITECTURE.md` (flujo de datos).
2. Funciones de ≤ 20 líneas, guard clauses, cadenas de nulables de máximo 2,
   `Log.e()` mínimo en cada `catch`.
3. Nuevos strings en los **9 locales** (`values`, `es-rVE/rES`, `pt-rBR/rPT`,
   `fr`, `it`, `en-rGB`, `b+es+419`).
4. Nueva dependencia runtime → añadirla a `app/src/main/assets/oss-licenses.json`
   **y** a `THIRD-PARTY-NOTICES` (nombre, versión, SPDX, titular, URL, uso).
5. Cambios en `DataBlock`/markup → mantener `fromLegacyHtml` y las regex legacy.
6. Cambios KSP/Room/Moshi → `sh gradlew clean --no-configuration-cache`.
7. **Si el código cambia, la doc se actualiza en el mismo PR.**

## Tests y estilo

```bash
sh gradlew test   # JUnit 4 + Robolectric + Roborazzi
```

* Robolectric: `@Config(sdk = [36])` (= `targetSdk`).
* Screenshots: `@GraphicsMode(NATIVE)`; salidas en `app/src/test/screenshots/`.
* Estilo Kotlin oficial (`kotlin.code.style=official`).
* Sin tests instrumentados sin dispositivo/emulador.

## Plantilla de PR

Ver `.github/pull_request_template.md`. Todo PR marca el checklist (tests,
locales, licencias si aplica, doc actualizada).
