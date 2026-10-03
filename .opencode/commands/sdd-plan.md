--- 
description: SDD · Genera el plan técnico de una spec aprobada 
agent: plan 
--- 
Lee docs/constitution.md, AGENTS.md y specs/$1/spec.md. Usa la skill sdd. 
NO escribas código. 
Genera specs/$1/plan.md con: archivos que se crean o modifican y la 
responsabilidad de cada uno, funciones puras de lógica (con "hoy" como 
parámetro), algoritmo en pseudocódigo, cómo se pinta en la interfaz, 
decisiones técnicas justificadas (con su alternativa descartada) y estrategia 
de tests con ./gradlew test. 
Todo debe respetar la constitución y cubrir todos los RF. Marca qué RF cubre 
cada parte. Si la spec no está aprobada o tiene dudas abiertas, para y avísame. 