package com.estrin217.editordecodigo.syntax

/**
 * Tokenizador de **nivel de bloque** de Markdown.
 *
 * Recorre el documento línea a línea y emite tokens de las construcciones que empiezan por el
 * principio de la línea: *front matter*, cercas de código, tablas con tubería, encabezados ATX y
 * setext, rayas de separación, citas, listas, marcadores de tarea y definiciones de referencia.
 *
 * ## Por qué un tokenizador y no más expresiones regulares
 *
 * `SyntaxHighlighter.applyRegex` aplica su `SpanStyle` **encima** de lo que ya haya, así que el
 * resultado depende del orden de las llamadas y no de la gramática del lenguaje. Con siete regex el
 * orden todavía se revisa a ojo; con veinte, no. Aquí el orden son **datos**: cada construcción
 * consume un rango exacto y devuelve el índice por el que sigue el recorrido.
 *
 * ## El contrato de cada rama
 *
 * Toda función `consumeX` devuelve **el índice donde sigue el recorrido**, o el mismo `start` que
 * recibió si no reconoce la construcción. Un mismo índice en las dos señales significa «no era yo»,
 * así que las ramas se prueban en cadena sin efectos doble.
 *
 * ## La garantía de no solape
 *
 * Los tokens salen ordenados y sin solapes **por construcción**, no por un filtro posterior: cada
 * rama consume su rango entero y devuelve el cursor ya avanzado. Un rango degenerado es un error de
 * programación y lo delata `require` en [MdToken], no una aserción escondida.
 *
 * Lógica pura: sin `androidx.compose`, sin `Context`, sin `R`, sin reloj (RNF-26).
 */
object MarkdownTokenizer {

    fun tokenize(markdown: String): List<MdToken> {
        if (markdown.isEmpty()) return emptyList()
        val tokens = TokenBuffer()
        var cursor = consumeFrontMatter(markdown, tokens)
        while (cursor < markdown.length) {
            val lineEnd = endOfLine(markdown, cursor)
            val next = consumeLine(markdown, cursor, tokens)
            // La pasada en línea se aplica a lo que el bloque dejó libre. Si la línea abrió una
            // construcción de bloque multilínea (una cerca), `next` ya está más allá y el tramo
            // queda vacío: por eso el interior de una cerca no recibe nunca un token en línea.
            val inlineEnd = minOf(lineEnd + 1, next)
            if (inlineEnd > cursor) {
                MarkdownInline.tokenize(markdown, cursor, inlineEnd, tokens)
            }
            cursor = if (next > cursor) next else cursor + 1
        }
        return tokens.build()
    }

    // ==========================================
    // Recorrido principal
    // ==========================================

    /**
     * Prueba las ramas de bloque en orden de prioridad y devuelve el índice donde sigue el
     * recorrido. Una línea que no casa con nada avanza una línea y no emite nada.
     */
    private fun consumeLine(md: String, start: Int, tokens: TokenBuffer): Int {
        consumeFence(md, start, tokens).takeIf { it > start }?.let { return it }
        consumeTable(md, start, tokens).takeIf { it > start }?.let { return it }
        consumeAtxHeading(md, start, tokens).takeIf { it > start }?.let { return it }
        consumeSetextHeading(md, start, tokens).takeIf { it > start }?.let { return it }
        consumeThematicBreak(md, start, tokens).takeIf { it > start }?.let { return it }
        consumeBlockquote(md, start, tokens).takeIf { it > start }?.let { return it }
        consumeListItem(md, start, tokens).takeIf { it > start }?.let { return it }
        consumeLinkRef(md, start, tokens).takeIf { it > start }?.let { return it }
        return nextLine(md, start)
    }

    // ==========================================
    // Front matter
    // ==========================================

    private const val FRONT_MATTER_FENCE = "---"

    /**
     * `---` en la primera línea **con** un `---` de cierre. Sin línea de cierre no hay *front
     * matter*: es una raya de separación, y la prueba lo fija. No se acepta en ninguna otra
     * posición.
     */
    private fun consumeFrontMatter(md: String, tokens: TokenBuffer): Int {
        val firstEnd = endOfLine(md, 0)
        if (md.substring(0, firstEnd).trim() != FRONT_MATTER_FENCE) return 0

        var cursor = nextLine(md, 0)
        while (cursor < md.length) {
            val end = endOfLine(md, cursor)
            if (md.substring(cursor, end).trim() == FRONT_MATTER_FENCE) {
                tokens.add(0, end, MdTokenKind.FRONT_MATTER)
                return nextLine(md, cursor)
            }
            cursor = nextLine(md, cursor)
        }
        return 0
    }

    // ==========================================
    // Cercas de código
    // ==========================================

    private val FENCE_RE = Regex("^ {0,3}(`{3,}|~{3,})(.*)$")

    /**
     * Cerca de código: apertura, cuerpo y cierre. Una cerca **sin cerrar llega hasta el final del
     * documento** (caso límite 1).
     *
     * El cuerpo se consume entero y el recorrido salta a la línea siguiente al cierre: por eso la
     * segunda pasada (en línea, T2) no puede tocar lo que hay dentro (RF-65).
     */
    private fun consumeFence(md: String, start: Int, tokens: TokenBuffer): Int {
        val lineEnd = endOfLine(md, start)
        val match = FENCE_RE.find(md.substring(start, lineEnd)) ?: return start
        val marker = match.groupValues[1]
        val info = match.groupValues[2]
        // Una cadena de lenguaje con acentos graves cierra la cerca antes de abrirla: es texto.
        if (marker.startsWith("`") && info.contains('`')) return start

        tokens.add(start, lineEnd, MdTokenKind.CODE_FENCE)

        val bodyStart = nextLine(md, start)
        var cursor = bodyStart
        while (cursor < md.length) {
            val end = endOfLine(md, cursor)
            if (isClosingFence(md.substring(cursor, end), marker)) {
                tokens.add(bodyStart, withoutTrailingNewline(md, bodyStart, cursor), MdTokenKind.CODE_BLOCK)
                tokens.add(cursor, end, MdTokenKind.CODE_FENCE)
                return nextLine(md, cursor)
            }
            cursor = nextLine(md, cursor)
        }
        tokens.add(bodyStart, withoutTrailingNewline(md, bodyStart, md.length), MdTokenKind.CODE_BLOCK)
        return md.length
    }

    /**
     * El cuerpo de una cerca no incluye el salto que lo separa de la línea siguiente: el token
     * abarca lo escrito, no el espacio en blanco de detrás.
     */
    private fun withoutTrailingNewline(md: String, from: Int, end: Int): Int {
        var last = end
        while (last > from && (md[last - 1] == '\n' || md[last - 1] == '\r')) last--
        return last
    }

    /** Cierre de cerca: el mismo caracter de apertura, al menos tan largo, y nada más en la línea. */
    private fun isClosingFence(text: String, marker: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        return trimmed.all { it == marker[0] } && trimmed.length >= marker.length
    }

    // ==========================================
    // Tablas con tubería
    // ==========================================

    /** Fila separadora: solo tubería, guiones y los dos puntos de la alineación. */
    private val TABLE_DELIM_RE =
        Regex("^\\s*\\|?\\s*:?-{1,}:?(\\s*\\|\\s*:?-{1,}:?)*\\s*\\|?\\s*$")

    /**
     * Tabla GFM: fila de cabecera seguida de fila separadora. Emite tubería y celdas por fila, y
     * sigue con filas de datos hasta una línea sin moderación o vacía (caso de tabla 4).
     */
    private fun consumeTable(md: String, start: Int, tokens: TokenBuffer): Int {
        val headerEnd = endOfLine(md, start)
        val header = md.substring(start, headerEnd)
        if (!header.contains('|')) return start

        val delimStart = nextLine(md, start)
        val delimEnd = endOfLine(md, delimStart)
        if (delimStart >= md.length) return start
        val delim = md.substring(delimStart, delimEnd)
        if (!delim.contains('|') || !TABLE_DELIM_RE.matches(delim)) return start

        emitTableRow(md, start, headerEnd, MdTokenKind.TABLE_HEADER, tokens)
        emitTableRow(md, delimStart, delimEnd, MdTokenKind.TABLE_DELIM, tokens)

        var cursor = nextLine(md, delimStart)
        while (cursor < md.length) {
            val end = endOfLine(md, cursor)
            val row = md.substring(cursor, end)
            if (row.isBlank() || !row.contains('|')) break
            emitTableRow(md, cursor, end, MdTokenKind.TABLE_ROW, tokens)
            cursor = nextLine(md, cursor)
        }
        return cursor
    }

    /**
     * Una tubería por celda delimitadora y una por celda de contenido. La celda se recorta al primer
     * y al último carácter no blanco, así que el relleno no se colorea.
     *
     * El recorrido es de izquierda a derecha y alterna celda, tubería, celda…: si se emitieran todas
     * las tuberías primero y después las celdas, los tokens llegarían desordenados y la garantía de
     * no solape del recorrido se rompería sin que nadie lo notara.
     */
    private fun emitTableRow(md: String, start: Int, end: Int, cellKind: MdTokenKind, tokens: TokenBuffer) {
        var cellStart = start
        while (cellStart <= end) {
            val pipe = md.indexOf('|', cellStart)
            // `indexOf` devuelve -1 cuando no hay tubería, y -1 no es una posición: sin esta guarda
            // se colaría como un token de la fila anterior.
            if (pipe < 0 || pipe > end) {
                addTrimmed(md, cellStart, end, cellKind, tokens)
                return
            }
            addTrimmed(md, cellStart, pipe, cellKind, tokens)
            tokens.add(pipe, pipe + 1, MdTokenKind.TABLE_DELIM)
            cellStart = pipe + 1
        }
    }

    private fun addTrimmed(md: String, start: Int, end: Int, kind: MdTokenKind, tokens: TokenBuffer) {
        if (end <= start) return
        val first = md.indexOfFirstIn(start, end) { !it.isWhitespace() }
        if (first == -1) return
        val last = md.indexOfLastIn(start, end) { !it.isWhitespace() }
        tokens.add(first, last + 1, kind)
    }

    // ==========================================
    // Encabezados
    // ==========================================

    private val ATX_RE = Regex("^ {0,3}(#{1,6})(?:[ \\t]+(.*))?$")

    /**
     * Encabezado ATX: la almohadilla por un lado y el texto por otro, fuera los `#` de cierre. Sin
     * espacio tras la almohadilla no es encabezado (`#NoHeading`).
     */
    private fun consumeAtxHeading(md: String, start: Int, tokens: TokenBuffer): Int {
        val lineEnd = endOfLine(md, start)
        val line = md.substring(start, lineEnd)
        val match = ATX_RE.matchEntire(line) ?: return start
        val marker = match.groupValues[1]
        val body = match.groupValues[2].orEmpty().trim().trimEnd('#').trim()
        if (body.isEmpty()) return start

        val markerStart = start + line.indexOf('#')
        tokens.add(markerStart, markerStart + marker.length, MdTokenKind.HEADING_MARK)
        // El cuerpo se localiza por búsqueda, no por aritmética: los `#` de cierre ocupan el final.
        val bodyStart = line.indexOf(body, marker.length)
        tokens.add(start + bodyStart, start + bodyStart + body.length, MdTokenKind.HEADING)
        return nextLine(md, start)
    }

    private val SETEXT_RE = Regex("^ {0,3}(=+|-+)[ \\t]*$")

    /**
     * Encabezado setext: la línea de texto y su subrayado. El subrayado **no** llega a la rama de la
     * raya de separación, porque aquí ya se consumieron las dos líneas.
     */
    private fun consumeSetextHeading(md: String, start: Int, tokens: TokenBuffer): Int {
        val lineEnd = endOfLine(md, start)
        val text = md.substring(start, lineEnd)
        if (text.isBlank()) return start

        val underlineStart = nextLine(md, start)
        if (underlineStart >= md.length) return start
        val underlineEnd = endOfLine(md, underlineStart)
        val underline = md.substring(underlineStart, underlineEnd)
        if (!SETEXT_RE.matches(underline)) return start
        // Una lista o una cita seguidas de `---` siguen siendo su propia cosa: el subrayado de
        // setext solo se lleva texto que de verdad puede ser título.
        val trimmed = text.trimStart()
        if (trimmed.startsWith("-") || trimmed.startsWith(">") || trimmed.startsWith("*")) return start

        tokens.add(start, lineEnd, MdTokenKind.HEADING)
        val markerOffset = underlineStart + underline.indexOfFirst { it == '=' || it == '-' }
        tokens.add(markerOffset, underlineEnd, MdTokenKind.HEADING_MARK)
        return nextLine(md, underlineStart)
    }

    // ==========================================
    // Rayas, citas y listas
    // ==========================================

    private val THEMATIC_RE = Regex("^ {0,3}([-*_])([ \\t]*\\1){2,}[ \\t]*$")

    private fun consumeThematicBreak(md: String, start: Int, tokens: TokenBuffer): Int {
        val lineEnd = endOfLine(md, start)
        if (!THEMATIC_RE.matches(md.substring(start, lineEnd))) return start
        tokens.add(start, lineEnd, MdTokenKind.THEMATIC_BREAK)
        return nextLine(md, start)
    }

    private val QUOTE_RE = Regex("^ {0,3}(>+)")

    private fun consumeBlockquote(md: String, start: Int, tokens: TokenBuffer): Int {
        val lineEnd = endOfLine(md, start)
        val match = QUOTE_RE.find(md.substring(start, lineEnd)) ?: return start
        tokens.add(start, start + match.range.last + 1, MdTokenKind.QUOTE_MARK)
        return nextLine(md, start)
    }

    private val LIST_RE = Regex("^([ \\t]*)([-+*]|(\\d{1,9})[.)])([ \\t]+)(.*)$")
    private val TASK_RE = Regex("^\\[([ xX])\\](?=[ \\t])")

    /**
     * Elemento de lista: viñeta o numeración, y marcador de tarea si el texto empieza por `[ ]` o
     * `[x]`. La sangría vive en la posición del token, así que el anidamiento se lee del `start`.
     */
    private fun consumeListItem(md: String, start: Int, tokens: TokenBuffer): Int {
        val lineEnd = endOfLine(md, start)
        val match = LIST_RE.find(md.substring(start, lineEnd)) ?: return start

        val indent = match.groupValues[1].length
        val marker = match.groupValues[2]
        val markerStart = start + indent
        val markerEnd = markerStart + marker.length
        val isOrdered = match.groupValues[3].isNotEmpty()
        tokens.add(markerStart, markerEnd, if (isOrdered) MdTokenKind.ORDERED_MARK else MdTokenKind.BULLET_MARK)

        val restStart = markerEnd + match.groupValues[4].length
        val task = TASK_RE.find(match.groupValues[5])
        if (task != null) {
            tokens.add(restStart, restStart + task.value.length, MdTokenKind.TASK_MARK)
        }
        return nextLine(md, start)
    }

    // ==========================================
    // Definiciones de referencia
    // ==========================================

    private val LINK_REF_RE = Regex("^ {0,3}\\[([^\\]]+)\\]:[ \\t]*(\\S+)[ \\t]*(\"[^\"]*\")?[ \\t]*$")

    private fun consumeLinkRef(md: String, start: Int, tokens: TokenBuffer): Int {
        val lineEnd = endOfLine(md, start)
        if (!LINK_REF_RE.matches(md.substring(start, lineEnd))) return start
        tokens.add(start, lineEnd, MdTokenKind.LINK_REF)
        return nextLine(md, start)
    }

    // ==========================================
    // Utilidad
    // ==========================================

    /** Índice del `\n` que cierra la línea, o la longitud del documento si es la última. */
    private fun endOfLine(md: String, start: Int): Int {
        val nl = md.indexOf('\n', start)
        return if (nl == -1) md.length else nl
    }

    /** Inicio de la línea siguiente. Nunca se queda atrás: el recorrido siempre avanza. */
    private fun nextLine(md: String, start: Int): Int {
        val end = endOfLine(md, start)
        return if (end < md.length) end + 1 else md.length
    }

    private inline fun String.indexOfFirstIn(from: Int, until: Int, predicate: (Char) -> Boolean): Int {
        for (i in from until until) if (predicate(this[i])) return i
        return -1
    }

    private inline fun String.indexOfLastIn(from: Int, until: Int, predicate: (Char) -> Boolean): Int {
        for (i in (until - 1) downTo from) if (predicate(this[i])) return i
        return -1
    }
}
