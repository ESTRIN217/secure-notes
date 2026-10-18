# Tareas 030 — Captura de bloqueo sin Android Keystore

Estado: **completada**

- [x] **T1. Desacoplar la captura de bloqueo del ViewModel y validar el render.** RF-198, RF-199, RF-200, RNF-128, RNF-129, RNF-130.
  - Hecho: el test adaptado primero falló en compilación por la API de presentación inexistente; tras extraer el render, el screenshot dirigido pasa sin crear `NotesViewModel`, Room ni Keystore. La captura mantiene su golden; la compilación de tests y `git diff --check` pasan.
