# MEMORY.md — secure-notes 
Memoria entre sesiones. Máximo ~50 líneas: resume o elimina lo que ya no aporte. 
## Estado actual 
- **Spec 005 implementada** (`specs/005-tabla-seleccion/`): selección y foco del bloque de tabla. Arregla la barra que no aparecía y quita la escalera del resaltado. 34 pruebas puras en verde, `assembleDebug` correcto, datos intactos. **Sin verificación visual.**
- **Tests del módulo principal compilan** (spec 002, T1-T3). La **suite completa sigue bloqueada por la plataforma**: Robolectric no tiene runtime nativo para `linux/aarch64`. Los tests **puros sí se ejecutan** aquí (`RichTextParserTest`, 35 casos, 0 fallos).
- **Rendimiento implementado** (`assembleDebug` OK, sin verificar en dispositivo): `AiModelHost` lazy y ref-counted, `perf/`, `StrokeGeometryCache`, listas con `key`+`contentType`, cachés de Coil, `SimpleDateFormat` cacheado.

## Decisiones (y por qué) 
- El host de IA vive en la `Application`, no en el ViewModel: el ViewModel se destruye al salir de la Activity y el host debe sobrevivir. Descarga por **referencias + inactividad**: recargar re-lee el GGUF.
- La barra de la tabla va en un **margen constante propio**. El salto que hacía bajar la tabla al enfocar venía de que `AnimatedVisibility` **cambiaba el alto**, no de reservar sitio. Y no se puede superponer en «el hueco» porque **el editor no pone espacio entre bloques**: taparía el bloque de arriba.
- Foco de un grupo con un **conjunto** de elementos enfocados, no con «el elemento enfocado»: el desenfoque de A puede llegar tras el foco de B y con un solo valor se borrarían. Queda vacío un instante en un orden, pero las escrituras se agrupan: lo que no puede pasar es que el estado **final** dependa del orden.
- Contorno de selección en **capa superpuesta**, no borde por celda: por eso `selectionOutline` devuelve **una** región o `null`. Mientras pueda devolver varias, el dibujado no puede saber si las une, y no unirlas es la escalera.

## Aprendizajes y errores a evitar 
- **`Modifier.onFocusChanged` no observa el foco de los descendientes** si el elemento al que se aplicado no es destino de foco. No da error: no se monta nada. No volver a asumirlo.
- Si Coil recibe una factoría `diskCache`, `.directory(...)` es **obligatorio**. No hay crash: las imágenes **no cargan** y no se ve por qué. `remember` en un `for` **exige `key()`**: sin él las iteraciones comparten el slot de memoria.
- material3 1.5.0-alpha28: `FloatingToolbarDefaults.containerShape` es **internal** y el `DragHandle` horizontal no es público. `TextRange` vive en `...ui.text`. `AnimatedVisibility` dentro de un `Box` resuelve a la sobrecarga de `ColumnScope` y no compila.
- AAPT2 **rechaza apóstrofos crudos** en `strings.xml` → escribir `\'`. Falla en `mergeDebugResources`, cuando Kotlin ya compiló bien.
- Una prueba que no se ha visto fallar no está probada: mutar el código y comprobar que el caso se pone rojo. Cuesta una vuelta de Gradle y evita capturas verdes que no comprueban nada. La geometría de la selección va **sin imports de Compose** a propósito, para poder ejercitarla sin el entorno de pruebas; criterio de cierre: `grep -c '^import androidx.compose'` → 0.

## Próximos pasos 
- **Verificar en dispositivo**: el margen de 60dp por tabla puede resultar excesivo, y una tabla como primer bloque (nada de la spec 005 se ha visto); 90/120 Hz y jank con `FrameMetrics`; que abrir/cerrar el asistente carga/libera el modelo (`logcat -s AiModelHost`).
- **Defecto conocido, sin spec**: `BlockEditor.kt:1011` tiene un stub para `BlockType.TABLE` que ignora `meta["table"]` → la tabla sale vacía en TTS y previsualización. Son ~4 líneas.
- **Defecto conocido, sin spec**: `BlockEditor.kt:92` comparte un único `pendingSelection` entre todos los bloques y `EditableChecklistBlock` no informa de su selección.
- Spec 002, T4-T5: generar los dos goldens que faltan y fijar `roborazzi.test.verify`.