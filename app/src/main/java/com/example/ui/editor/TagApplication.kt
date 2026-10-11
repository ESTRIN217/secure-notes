package com.example.ui.editor

import com.example.data.model.TextSegment
import com.example.util.RichTextConverter

/**
 * Qué pasa cuando una etiqueta se aplica a lo que el usuario tiene seleccionado (spec 014, RF-87).
 *
 * Aquí estaba el corazón duplicado del editor. Dos mandos distintos —el de las etiquetas de formato
 * (`b`, `i`, `sub`…) y el de las que llevan valor (`color`, `font`, `size`…)— tenían **el mismo
 * esqueleto escrito dos veces**: Decide si lo que hay bajo el cursor es una letra suelta o un
 * tramo, calcula si la etiqueta ya está puesta, aplica el cambio y actualiza el estilo con el que se
 * sigue escribiendo. Lo único que cambiaba entre las dos copias era *qué* se le hace al tramo.
 *
 * Aquí eso es un parámetro ([transform]) y el esqueleto está escrito una vez.
 *
 * **Por qué el cursor y la selección se tratan distinto.** Con el cursor encima de una letra, lo
 * que se recuerda es el estilo con el que se *seguirá* escribiendo, y además se aplica a esa letra
 * suelta. Con un tramo seleccionado, lo que manda es lo que ya tenía dentro: si todo lo seleccionado
 * tiene la etiqueta, pulsarla la quita; si no, la pone. Sin selección, no hay nada a lo que quitar.
 *
 * **Y por qué la pregunta del cursor es un parámetro aparte.** Los dos mandos que usan esto
 * decidían de forma distinta, y **la diferencia es su comportamiento, no un descuido**:
 *
 * - El de las etiquetas de formato conmutaba mirando **la letra y lo pendiente**: si cualquiera de
 *   los dos tenía la etiqueta, la pulsación la quitaba.
 * - El de las etiquetas con valor (`color`, `font`…) decidía mirando **solo lo pendiente**: si el
 *   estilo con el que se estaba escribiendo ya llevaba ese valor, lo quitaba, aunque la letra de
 *   debajo lo llevara puesto. Con el cursor sobre una letra ya coloreada y sin color pendiente,
 *   pulsarla **vuelve a colorear** en vez de borrarlo.
 *
 * Unificarlos «arreglando» el segundo cambiaría lo que hace el editor, así que la diferencia se
 * mantiene y se deja escrita. Corregirla, si se quiere, es otra spec.
 */
data class TagApplication(
    /** Los tramos nuevos, o `null` si no había nada que tocar. */
    val segments: List<TextSegment>?,
    /** Con qué estilo se seguirá escribiendo. */
    val pending: TextSegment
)

/**
 * Aplica [transform] a lo seleccionado, decidiendo por separado el cursor de una letra y el tramo.
 *
 * @param segments los tramos del bloque.
 * @param plainLength longitud del texto plano, para acotar la selección.
 * @param selStart inicio de la selección.
 * @param selEnd fin de la selección. Si es igual a `selStart`, el cursor está suelto.
 * @param currentPending el estilo con el que se está escribiendo ahora, o `null`.
 * @param isOn si un tramo ya lleva la etiqueta. Decide en el caso del **tramo seleccionado**.
 * @param isAlreadyApplied si, con el cursor suelto, hay que poner o quitar. Recibe la letra que hay
 *   debajo (`null` si el cursor está al final) y el estilo pendiente, porque cada mando mira una
 *   cosa distinta: ver la nota de arriba.
 * @param transform qué se le hace a un tramo, según venga ya puesto o no.
 */
fun applyTagToSelection(
    segments: List<TextSegment>,
    plainLength: Int,
    selStart: Int,
    selEnd: Int,
    currentPending: TextSegment?,
    isOn: (TextSegment) -> Boolean,
    isAlreadyApplied: (charUnderCursor: TextSegment?, pending: TextSegment) -> Boolean,
    transform: (TextSegment, Boolean) -> TextSegment
): TagApplication {
    val start = selStart.coerceIn(0, plainLength)
    val end = selEnd.coerceIn(0, plainLength).coerceAtLeast(start)
    return if (start == end) {
        applyAtCursor(segments, start, plainLength, currentPending, isAlreadyApplied, transform)
    } else {
        applyAtRange(segments, start, end, currentPending, isOn, transform)
    }
}

/**
 * Cursor suelto: se mira la letra que hay debajo y, si existe, se le aplica el cambio.
 *
 * El estilo pendiente se calcula **además** sobre el tramo del cursor: se está escribiendo justo
 * ahí, así que lo que se acaba de pulsar se nota en lo que se teclee a continuación aunque el cursor
 * esté al final del texto.
 */
private fun applyAtCursor(
    segments: List<TextSegment>,
    start: Int,
    plainLength: Int,
    currentPending: TextSegment?,
    isAlreadyApplied: (TextSegment?, TextSegment) -> Boolean,
    transform: (TextSegment, Boolean) -> TextSegment
): TagApplication {
    val charSeg = if (start < plainLength) {
        RichTextConverter.rangeSegments(segments, start, start + 1).firstOrNull()
    } else {
        null
    }
    val base = currentPending ?: TextSegment()
    val already = isAlreadyApplied(charSeg, base)
    val nuevos = if (charSeg != null) {
        RichTextConverter.applySpanStyle(segments, start, start + 1) { transform(it, already) }
    } else {
        null
    }
    return TagApplication(nuevos, transform(base, already))
}

/**
 * Tramo seleccionado: si **todo** lo seleccionado lleva la etiqueta, se quita; si no, se pone.
 *
 * El estilo pendiente no se toca: con texto seleccionado lo que se va a escribir después no está
 * decidido todavía, y el código que había tampoco lo cambiaba.
 */
private fun applyAtRange(
    segments: List<TextSegment>,
    start: Int,
    end: Int,
    currentPending: TextSegment?,
    isOn: (TextSegment) -> Boolean,
    transform: (TextSegment, Boolean) -> TextSegment
): TagApplication {
    val inRange = RichTextConverter.rangeSegments(segments, start, end)
    val todosPuestos = inRange.isNotEmpty() && inRange.all(isOn)
    val nuevos = RichTextConverter.applySpanStyle(segments, start, end) { transform(it, todosPuestos) }
    return TagApplication(nuevos, currentPending ?: TextSegment())
}

// ── Las etiquetas que llevan valor ───────────────────────────────────────────────────
//
// `color`, `bg`, `font`, `size` y `url` no conmutan: poner un color es poner **un** color, así que
// la pregunta no es «¿lo tiene?» sino «¿tiene **este**?». Cada una vive en un campo distinto del
// tramo, y por eso van las tres funciones juntas: son la tabla de los campos de un tramo, y
// escribirlas separadas es lo que hace que una se quede atrás.

/** Pone el valor de una etiqueta con valor. Una etiqueta desconocida devuelve el tramo sin tocar. */
fun setTagValue(seg: TextSegment, tag: String, value: String): TextSegment = when (tag) {
    "color" -> seg.copy(colorHex = RichTextConverter.normalizeColorValue(value))
    "bg" -> seg.copy(bgColorHex = RichTextConverter.normalizeColorValue(value))
    "font" -> seg.copy(fontFamily = value)
    "size" -> seg.copy(fontSizeSp = value.toFloatOrNull())
    "url" -> seg.copy(linkUrl = value)
    else -> seg
}

/** Quita el valor de una etiqueta con valor, dejando el resto del tramo como estaba. */
fun clearTagValue(seg: TextSegment, tag: String): TextSegment = when (tag) {
    "color" -> seg.copy(colorHex = null)
    "bg" -> seg.copy(bgColorHex = null)
    "font" -> seg.copy(fontFamily = null)
    "size" -> seg.copy(fontSizeSp = null)
    "url" -> seg.copy(linkUrl = null)
    else -> seg
}

/**
 * El valor que lleva ahora la etiqueta, como texto, para poder compararlo con el tecleado.
 *
 * Los tamaños se comparan como cadena, y un `14f` se lee igual que un `14`: si no, al teclear un
 * tamaño que ya está puesto se compararía `"14"` contra `"14.0"` y nunca casarían, con lo que la
 * segunda pulsación no lo quitaría.
 */
fun tagValueOf(seg: TextSegment, tag: String): String? = when (tag) {
    "color" -> seg.colorHex
    "bg" -> seg.bgColorHex
    "font" -> seg.fontFamily
    "size" -> seg.fontSizeSp?.let { if (it == it.toInt().toFloat()) it.toInt().toString() else it.toString() }
    "url" -> seg.linkUrl
    else -> null
}