package com.example.ui.chat

import com.example.data.ai.AiAction

/**
 * L4, L6 y L7 — estado del campo de escritura: si se puede enviar, si el envío vacío se descarta, qué
 * texto de ayuda corresponde, con qué clave se pinta cada ficha, qué muestra el campo, a qué estado
 * enruta la escritura y si se limpia tras enviar.
 *
 * Son **funciones puras**: no importan `androidx.compose`, no acceden a `R` y no leen el reloj. El
 * texto de ayuda sale como identificador del dominio ([ChatHint]) y lo traduce el Composable con
 * `stringResource`, y la constante de tipo de contenido vive aquí para que la haya **una sola**
 * (RF-39).
 */

/**
 * Tipo de contenido de las fichas de adjuntos (RF-33 h).
 *
 * Es la **única** declaración del repositorio: la usan la pantalla de chat y el campo, que ya no
 * declaran la suya.
 */
internal const val ATTACHMENT_CHIP = 4

/**
 * Los cuatro textos de ayuda del campo (RF-33 c). Con historial es [FOLLOWUP]; sin historial, el de
 * la acción activa. Son identificadores, **no** claves de recurso: el Composable los traduce.
 */
enum class ChatHint { FOLLOWUP, GENERATE, SUMMARIZE, FIX_GRAMMAR }

/** A qué estado del texto escribe el campo: al normal, o a la edición si la hay (RF-33 e). */
enum class WriteTarget { DRAFT, EDITING }

/**
 * L4 — si se puede enviar (RF-33 a): hay texto, **o** hay adjuntos pendientes, **o** el modelo está
 * generando —en ese momento el botón sirve para detener—. Sin ninguna de las tres, deshabilitado.
 */
fun canSend(text: String, hasAttachments: Boolean, isProcessing: Boolean): Boolean =
    isProcessing || text.isNotBlank() || hasAttachments

/**
 * L4 — si un envío vacío se descarta (RF-33 a): se descarta **solo** si no hay adjuntos pendientes.
 * Con adjuntos sí se envía, que es el comportamiento actual (caso límite 1 y caso límite 5: borrar
 * todo el texto de un mensaje en edición con adjuntos pendientes lanza la petición).
 */
fun shouldSend(text: String, hasAttachments: Boolean): Boolean =
    text.isNotBlank() || hasAttachments

/** L4 — qué texto de ayuda corresponde (RF-33 c): con historial siempre el de seguimiento. */
fun hintFor(hasHistory: Boolean, action: AiAction): ChatHint {
    if (hasHistory) return ChatHint.FOLLOWUP
    return when (action) {
        AiAction.GENERATE -> ChatHint.GENERATE
        AiAction.SUMMARIZE -> ChatHint.SUMMARIZE
        AiAction.FIX_GRAMMAR -> ChatHint.FIX_GRAMMAR
    }
}

/**
 * L4 — la clave con la que se pinta cada ficha de adjunto (RF-33 h, decisión 7).
 *
 * Se compone de **posición y nombre**, con lo que es única por construcción: dos adjuntos con el
 * mismo nombre dan dos claves distintas y una clave por nombre lanzaría
 * `IllegalArgumentException: Key was already used` (caso límite 4). El precio es que al quitar una
 * ficha las demás cambian de clave y Compose las destruye y recompone; aquí no se pierde nada porque
 * la ficha no tiene estado interno. Es una **excepción consciente** a la regla de `AGENTS.md` de
 * «toda `LazyRow` lleva `key` estable», porque `FileAttachment` no tiene identificador y quitar un
 * adjunto es por posición (riesgo 7 de la spec).
 */
fun chipKey(index: Int, name: String): String = "chip_${index}_$name"

/**
 * L6 — qué texto muestra el campo (RF-33 e): la edición si la hay y si no el texto normal.
 *
 * La edición vacía se respeta: si no la hay **no** se cae al texto normal, porque en la rama de
 * edición el texto normal es otro mensaje (caso límite 5).
 */
fun effectiveText(draft: String, editing: String?): String = editing ?: draft

/** L6 — a qué estado enruta el campo su manejador de escritura (RF-33 e): al que está leyendo. */
fun writeTarget(hasEditing: Boolean): WriteTarget =
    if (hasEditing) WriteTarget.EDITING else WriteTarget.DRAFT

/**
 * L6 — qué estados fija una edición recién cargada al abrir la sesión (RF-33 e, caso límite 7).
 *
 * Devuelve `(texto normal, edición)`: con la edición pendiente van los dos fijados, y **sin** ella
 * no se inventa ninguna —una `null` devuelve el texto normal vacío y `null` de edición—.
 */
fun adoptPendingEditing(pendingEditing: String?): Pair<String, String?> {
    if (pendingEditing == null) return "" to null
    return pendingEditing to null
}

/** L7 — si el campo se limpia tras enviar (RF-33 f): solo con la acción de generación. */
fun shouldClearInput(action: AiAction): Boolean = action == AiAction.GENERATE
