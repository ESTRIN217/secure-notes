# ADR-0001 — IA local, sin nube

**Estado:** aceptado · **Fecha:** 2026-09-11

## Contexto

Se quería asistencia de escritura sin traicionar el cifrado client-side: cualquier
API cloud invalidaría la promesa de privacidad.

## Decisión

Dos backends, ambos locales: llama.cpp on-device (GGUF, `:lib`) y HTTP a LAN
(Ollama/LM Studio). Función opcional, desactivada por defecto, con consentimiento
explícito. Historial en Room. Nada sale a terceros ni entrena modelos externos.

## Consecuencias

* Solo `arm64-v8a`; modelos GGUF descargados por el usuario (peso y batería).
* Semántica singleton del binding (ver `GUIDELINES.md` §10): aislamiento vía
  `LlamaCppEngine`, jamás `destroy()`.
