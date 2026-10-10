package com.estrin217.editordecodigo.syntax

/**
 * Tipo de una construcción de Markdown reconocida por [MarkdownTokenizer].
 *
 * Cada token lleva **un solo** tipo, nunca una lista. Si pudiera llevar dos, la pregunta de «cuál
 * gana» quedaría abierta en el dibujado, que es la trampa que ya se pagó con el contorno de
 * selección de la tabla (spec 008).
 */
enum class MdTokenKind {
    FRONT_MATTER,
    HEADING_MARK,
    HEADING,
    CODE_FENCE,
    CODE_BLOCK,
    TABLE_DELIM,
    TABLE_HEADER,
    TABLE_ROW,
    THEMATIC_BREAK,
    QUOTE_MARK,
    BULLET_MARK,
    ORDERED_MARK,
    TASK_MARK,
    LINK_REF,
    ESCAPE,
    INLINE_CODE,
    STRONG,
    EM,
    STRIKETHROUGH,
    LINK_TEXT,
    LINK_URL,
    AUTOLINK,
    HTML_TAG
}

/**
 * Tramo `[start, end)` del documento con su construcción.
 *
 * Dos tokens **nunca** se solapan: cuando dos construcciones se pisan, gana la de mayor prioridad
 * (bloque sobre en línea) y la perdedora no se emite.
 *
 * `MdToken` es **inmutable** a propósito. Al tokenizar en línea hay que interrogar si un tramo ya
 * está cubierto —un token de bloque no se pisa con uno en línea— y hacerlo con un buffer mutable
 * obligaría a un escaneo lineal por carácter. Con el token ya construido, la pregunta es un `when`.
 */
data class MdToken(val start: Int, val end: Int, val kind: MdTokenKind) {
    init {
        require(start >= 0) { "MdToken con start negativo: $this" }
        require(end >= start) { "MdToken invertido: $this" }
    }

    fun length(): Int = end - start
}