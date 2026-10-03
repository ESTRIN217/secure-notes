--- 
description: SDD · Entrevista y genera la spec (uso - /sdd-spec 002-nombre idea inicial) 
agent: plan 
--- 
NO escribas código en ningún momento. Lee docs/constitution.md y MEMORY.md, y usa la 
skill sdd. 
Carpeta de la spec: specs/$1/ 
Idea inicial (el texto que va después del nombre de la carpeta): $ARGUMENTS 
Tu trabajo: 
1. Hazme preguntas de UNA en UNA para eliminar ambigüedades (casos límite, 
 errores, qué queda fuera de esta versión). Máximo 5 preguntas. 
2. Con mis respuestas, genera specs/$1/spec.md siguiendo la plantilla de la 
 skill sdd, con los requisitos en EARS y "Estado: borrador". 
3. Solo el QUÉ y el POR QUÉ: nada de stack, arquitectura ni nombres de archivos. 