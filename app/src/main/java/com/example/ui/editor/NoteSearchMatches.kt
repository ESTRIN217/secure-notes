package com.example.ui.editor

/**
 * Dónde cae cada aparición de lo que se busca dentro del texto de la nota (spec 014, RF-87).
 *
 * El texto que se busca **no** es el que se ve: la nota guarda su formato entre etiquetas
 * (`<b>`, `<color=…>`), así que hay un texto limpio que se muestra y otro con las etiquetas dentro.
 * Por eso las coincidencias se buscan en el limpio y luego se **traducen** al original, que es
 * donde vive el cursor. Esa traducción es [toRawIndex] y la pone quien llama.
 *
 * Devuelve `IntRange` y no el tipo de rango de texto de la interfaz, para que este fichero no
 * importe nada de composición y se pueda probar sin pantalla.
 */

/**
 * @param cleanText el texto sin etiquetas, que es el que se lee.
 * @param query lo que se busca.
 * @param fullWord si tiene que casar **palabras enteras**.
 * @param caseSensitive si distingue mayúsculas.
 * @param toRawIndex traduce un índice del texto limpio a su índice en el original.
 */
fun buildMatchRanges(
    cleanText: String,
    query: String,
    fullWord: Boolean,
    caseSensitive: Boolean,
    toRawIndex: (Int) -> Int
): List<IntRange> {
    if (query.isEmpty()) return emptyList()
    val limpias = mutableListOf<IntRange>()
    if (fullWord) {
        wholeWordRanges(cleanText, query, caseSensitive, limpias)
    } else {
        everyOccurrence(cleanText, query, caseSensitive, limpias)
    }
    return limpias.map { toRawIndex(it.first)..toRawIndex(it.last + 1) }
}

/**
 * Busca **palabras enteras**, con una expresión regular y los límites `\b`.
 *
 * La consulta va escapada, así que un `(` o un `[]` que se teclee se busca **literalmente** y no
 * rompe la búsqueda. La captura es por si acaso: si aun así la expresión no compila, la búsqueda
 * cae a comparar a pelo en vez de dejar la pantalla sin resultados.
 */
private fun wholeWordRanges(
    cleanText: String,
    query: String,
    caseSensitive: Boolean,
    into: MutableList<IntRange>
) {
    val opciones = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
    val patron = "\\b${Regex.escape(query)}\\b"
    try {
        Regex(patron, opciones).findAll(cleanText).forEach {
            into.add(it.range.first..it.range.last)
        }
    } catch (_: Exception) {
        everyOccurrence(cleanText, query, caseSensitive, into)
    }
}

/** Todas las apariciones, seguidas: la búsqueda salta de coincidencia en coincidencia. */
private fun everyOccurrence(
    cleanText: String,
    query: String,
    caseSensitive: Boolean,
    into: MutableList<IntRange>
) {
    var desde = 0
    while (true) {
        val i = cleanText.indexOf(query, desde, ignoreCase = !caseSensitive)
        if (i == -1) return
        into.add(i until i + query.length)
        desde = i + 1
    }
}