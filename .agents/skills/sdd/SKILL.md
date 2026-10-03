--- 
name: sdd 
description: Úsala siempre que trabajes con Spec-Driven Development en este proyecto (docs/constitution.md o cualquier archivo dentro de specs/) - redactar, revisar o cambiar specs, planes y tareas, o implementar y validar tareas de una spec.
---
# Spec-Driven Development (SDD) 
## Flujo 
Constitución → Spec → Clarificación → Plan → Tareas → Implementación → Validación →
Cambio. 
- Nunca pases a la siguiente fase sin la aprobación explícita del usuario. 
- La spec manda: si algo no está en la spec, no se implementa. Si falta una decisión, 
para y pregunta. 
- Un cambio de requisitos se hace primero en la spec, luego en el plan y las tareas, y 
por último en el código. 
- Cada spec vive en su carpeta: `specs/NNN-nombre/` con `spec.md`, `plan.md` y 
`tasks.md`. 
- Al terminar cada fase, actualiza `MEMORY.md`. 
## Plantilla de spec (spec.md) 
``` 
# Spec NNN — <Nombre> 
Estado: borrador | aprobada | implementada 
## Contexto y objetivo 
## Usuarios 
## Historias de usuario 
- HU-1. Como <rol>, quiero <acción> para <beneficio>. 
## Definiciones (solo si hay términos que puedan interpretarse de varias formas) 
## Requisitos funcionales 
## Requisitos no funcionales 
## Casos límite 
## Fuera de alcance 
## Criterios de finalización 
## Dudas abiertas 
- [NECESITA ACLARACIÓN] <duda> 
``` 
La spec describe el QUÉ y el POR QUÉ. Nada de stack, arquitectura ni nombres de archivos. 
## Requisitos en EARS (en español) 
- RF-x: CUANDO <evento>, EL SISTEMA <respuesta>. 
- RF-x: SI <condición no deseada>, ENTONCES EL SISTEMA <respuesta>. 
- RF-x: MIENTRAS <estado>, EL SISTEMA <respuesta>. 
- RF-x: EL SISTEMA <comportamiento permanente>. 
Cada RF debe ser verificable: nada de "rápido", "intenso" o "bonito" sin un criterio medible. 
## Plan (plan.md) 
Archivos y responsabilidades · Funciones puras (con "hoy" como parámetro) · Algoritmo en pseudocódigo · Interfaz · Decisiones justificadas con su alternativa descartada · Estrategia de tests con `./gradlew test`. Indica qué RF cubre cada parte. 
## Tareas (tasks.md) 
``` 
- [ ] **Tn. <Descripción>.** RF-x, RF-y 
 - Hecho cuando: <comprobación verificable>. 
``` 
Máximo 20-30 min por tarea, en orden de dependencia. Si salen más de 10, propón dividir 
la spec. 
## Implementación 
Una sola tarea cada vez: tests primero (en rojo), después el código, `./gradlew test` en verde, marcar la tarea y parar.
