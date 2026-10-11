package com.example.ui.editor

import com.example.data.model.TextBaseline
import com.example.data.model.TextSegment

/**
 * Qué le hace cada etiqueta de formato a un tramo de texto (spec 014, RF-87).
 *
 * Son **funciones puras**: reciben la etiqueta y el tramo y devuelven el tramo. No saben nada de
 * la pantalla, del cursor ni de la nota; quien las usa decide cuándo se aplican y a qué selección.
 *
 * Aquí viven **las tres**, juntas, porque son las tres caras de la misma tabla: si una etiqueta se
 * puede poner, se puede quitar y se puede preguntar si está puesta, entonces escribirlas en un solo
 * sitio es lo que evita que una de las tres se quede atrás.
 *
 * El fichero no importa nada de composición a propósito: es la condición para poder ejercitarlo sin
 * levantar interfaz, que en la plataforma de desarrollo es la única forma de probar.
 */

/** ¿La etiqueta está puesta en este tramo? */
fun styleApplies(tag: String, seg: TextSegment): Boolean = when (tag) {
    "b" -> seg.bold
    "i" -> seg.italic
    "u" -> seg.underline
    "s" -> seg.strikethrough
    "code" -> seg.code
    "sub" -> seg.baseline == TextBaseline.SUBSCRIPT
    "sup" -> seg.baseline == TextBaseline.SUPERSCRIPT
    "mark" -> seg.bgColorHex == "FFEB3B"
    "small" -> seg.fontSizeSp != null && seg.fontSizeSp < 14f
    "kbd", "var", "samp" -> seg.code
    "quote" -> seg.italic
    else -> false
}

/** El tramo con la etiqueta puesta. Una etiqueta desconocida devuelve el tramo sin tocar. */
fun applyTransformForTag(tag: String, seg: TextSegment): TextSegment = when (tag) {
    "b" -> seg.copy(bold = true)
    "i" -> seg.copy(italic = true)
    "u" -> seg.copy(underline = true)
    "s" -> seg.copy(strikethrough = true)
    "code" -> seg.copy(code = true)
    "sub" -> seg.copy(baseline = TextBaseline.SUBSCRIPT)
    "sup" -> seg.copy(baseline = TextBaseline.SUPERSCRIPT)
    "mark" -> seg.copy(bgColorHex = "FFEB3B")
    "small" -> seg.copy(fontSizeSp = 12f)
    "kbd", "var", "samp" -> seg.copy(code = true)
    "normal" -> seg.copy(
        bold = false, italic = false, underline = false, strikethrough = false,
        code = false, colorHex = null, bgColorHex = null, fontSizeSp = null,
        baseline = TextBaseline.NORMAL
    )
    "quote" -> seg.copy(italic = true)
    else -> seg
}

/** El tramo con la etiqueta quitada. Una etiqueta desconocida devuelve el tramo sin tocar. */
fun removeTransformForTag(tag: String, seg: TextSegment): TextSegment = when (tag) {
    "b" -> seg.copy(bold = false)
    "i" -> seg.copy(italic = false)
    "u" -> seg.copy(underline = false)
    "s" -> seg.copy(strikethrough = false)
    "code" -> seg.copy(code = false)
    "sub" -> seg.copy(baseline = TextBaseline.NORMAL)
    "sup" -> seg.copy(baseline = TextBaseline.NORMAL)
    "mark" -> seg.copy(bgColorHex = null)
    "small" -> seg.copy(fontSizeSp = null)
    "kbd", "var", "samp" -> seg.copy(code = false)
    "quote" -> seg.copy(italic = false)
    else -> seg
}