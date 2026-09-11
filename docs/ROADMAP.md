# Roadmap — Secure Notes

Visión: el bloc con editor de nivel Notion donde la privacidad no es un extra sino
la arquitectura (cifrado client-side + IA local).

## Corto plazo

* Cerrar v4.0 (ver `CHANGELOG.md` → Sin publicar): Drive, IA, tabs, burbuja.
* Publicar release firmada `arm64-v8a` y notas en GitHub Releases.

## Mediano plazo

* **Migraciones Room reales**: eliminar `fallbackToDestructiveMigration()` antes de
  cualquier cambio de esquema (hoy destruiría datos en silencio).
* Cobertura Roborazzi de Legal/About/Licenses y del editor (bloques + tablas).
* Unificar versiones (`versionName`/`versionCode` hoy incoherentes: `3.0` / `2`).

## Largo plazo

* Refactor de `NoteEditorScreen` (descomponer el editor gigante).
* Repositorio opcional si aparece una segunda fuente de datos (hoy YAGNI).
* Evaluar `google-services.json` opcional en buildType sin Drive.
