# Roadmap — Secure Notes

Visión: el bloc con editor de nivel Notion donde la privacidad no es un extra sino
la arquitectura (cifrado client-side + IA local).

## Corto plazo

* Publicar la release **v4.0.0** firmada `arm64-v8a` y sus notas en GitHub Releases.
* Validar en dispositivo la exportación PDF real y el informe de fallos (spec 020).

## Mediano plazo

* **Migraciones Room reales**: eliminar `fallbackToDestructiveMigration()` antes de
  cualquier cambio de esquema (hoy destruiría datos en silencio).
* Cobertura Roborazzi de Legal/About/Licenses y del editor (bloques + tablas).
* Ejecutar la suite completa (`./gradlew test`) en un host `x86_64` o en CI: el de
  desarrollo es `aarch64` y Robolectric no distribuye runtime nativo para esa
  arquitectura (por eso CI solo ejecuta los módulos librería, ver `AGENTS.md`).

## Largo plazo

* Repositorio opcional si aparece una segunda fuente de datos (hoy YAGNI).
* Evaluar `google-services.json` opcional en buildType sin Drive.
