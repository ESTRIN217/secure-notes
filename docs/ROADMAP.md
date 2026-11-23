# Roadmap — Secure Notes

Visión: el bloc con editor de nivel Notion donde la privacidad no es un extra sino
la arquitectura (cifrado client-side + IA local).

## Corto plazo

* ✅ Publicada la release **v4.0.0** firmada `arm64-v8a` con sus notas en GitHub Releases (2026-10-18).
* ✅ Publicada la release **v4.1.0** (`full` + `independent`), tag y notas en GitHub Releases (2026-11-22).
* ✅ Informe de fallos (spec 020) verificado en dispositivo (2026-11-22).

## Mediano plazo

* **Migraciones Room reales**: eliminar `fallbackToDestructiveMigration()` antes de
  cualquier cambio de esquema (hoy destruiría datos en silencio).
* Cobertura Roborazzi de Legal/About/Licenses y del editor (bloques + tablas).
* ✅ Suite completa (`./gradlew test`) verificada en host `x86_64` (842 tests, 0 fallos). El host
  de desarrollo es `aarch64` y Robolectric no distribuye runtime nativo para esa arquitectura,
  así que allí se usan las pruebas puras. CI (`test.yml`) ejecuta en verde los tests de los módulos
  librería y `:app:testIndependentDebugUnitTest` (flavor `independent`, sin `:lib`/NDK).

## Largo plazo

* Repositorio opcional si aparece una segunda fuente de datos (hoy YAGNI).
* ✅ Resuelto: el flavor `independent` compila sin Google Drive ni `google-services.json` real
  (placeholder propio), con el mismo `applicationId`.
