# ADR-0002 — Sin repository, DI manual

**Estado:** aceptado · **Fecha:** 2026-09-11

## Contexto

App de un solo módulo con Room como única persistencia. Un framework DI y una capa
repository añadirían indirección sin segunda fuente de datos.

## Decisión

ViewModels consumen DAOs directamente (YAGNI); interfaces solo con sustitución real
(`CipherService`, `PreferencesRepository`, `CloudSyncManager`, `AIService`).
Inyección por constructor con `ViewModelProvider.Factory` en `MainActivity`.

## Consecuencias

* Menos magia, tests directos. Si aparece una segunda fuente, se introduce el
  repository entonces (y solo entonces).
