# Constitución — Secure Notes

Principios innegociables. Si un cambio los contradice, se corrige el cambio o se
revisa este documento primero; nunca se relajan en silencio. Detalles operativos
en `AGENTS.md`, principios de ingeniería en `docs/GUIDELINES.md`.

1. **Stack mínimo.** Cero dependencias nuevas sin motivo real: sin framework DI, sin ORM
   adicional, sin librería reactiva (Compose + Room + StateFlow bastan). Verificable: cada
   `implementation(...)` se justifica en la spec y aparece en `oss-licenses.json`; nada commented-out ni sin uso.

2. **La spec manda.** Ningún cambio de comportamiento sin `specs/NNN-*/spec.md` aprobada; el
   código implementa lo que la spec dice y nada más. Verificable: todo PR con cambio de
   comportamiento referencia sus RF-x y actualiza `MEMORY.md` en la misma entrega.

3. **Lógica fuera de la interfaz.** Un `@Composable` pinta estado y lanza eventos; decidir,
   cifrar y persistir vive en ViewModel/servicio y devuelve `Result<T>`. Verificable: 0 llamadas
   a DAO o `CipherService` desde un Composable; funciones ≤ 20 líneas con guard clauses.

4. **Tests primero, `./gradlew test` en verde.** Toda función pura, parser o exportador nuevo
   nace con su test en rojo antes de existir el código. Verificable: salida verde adjunta al PR;
   sin tests instrumentados sin dispositivo.

5. **Los datos del usuario no salen del dispositivo.** Cifrado y descifrado solo en local, sin
   telemetría y sin volcar contenido a logs; los respaldos se cifran antes de subirse. Verificable:
   `grep -rn "Log\."` no imprime texto de nota; ningún SDK de analítica en el APK.

6. **Un idioma por capa.** Identificadores, API y mensajes de log en inglés; comentarios y docs
   en español; todo texto visible sale de `strings.xml` en los 9 locales. Verificable: sin
   literales en `Text(`/`contentDescription`; los 9 `strings.xml` sincronizados.
