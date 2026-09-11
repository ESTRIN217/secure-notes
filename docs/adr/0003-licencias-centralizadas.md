# ADR-0003 — Licencias centralizadas en JSON

**Estado:** aceptado · **Fecha:** 2026-09-11

## Contexto

`THIRD-PARTY-NOTICES` y `LicensesScreen` mantenían listas duplicadas y
desactualizadas (ej.: `compose-markdown` figuraba como Apache-2.0 siendo MIT;
faltaban `latex`, `media3`, `androidx.pdf`, `googleid`).

## Decisión

Fuente única: `app/src/main/assets/oss-licenses.json` (parseado con `JSONObject`
del framework, sin parser nuevo). `LicensesScreen` la carga en `Dispatchers.IO`
con fallback mínimo; `THIRD-PARTY-NOTICES` refleja el mismo contenido en formato
humano. Alcance: solo runtime del APK (test y plugins de build excluidos).

## Consecuencias

* Toda dependencia runtime nueva exige actualizar JSON + NOTICES en el mismo PR.
* Correcciones verificadas contra upstream: MIT para `compose-markdown` y `latex`;
  licencias propietarias Google para Firebase/Play Services/GoogleId.
