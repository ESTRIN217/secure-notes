package com.estrin217.editordecodigo.syntax

/**
 * Segunda pasada del tokenizador: construcciones **en línea** dentro de una misma línea.
 *
 * Se aplica a lo que la pasada de bloques dejó libre. Nunca pisa un token que ya existe: si un
 * tramo está cubierto, se salta entero (RF-65). Por eso el interior de una cerca, el texto ya
 * marcado de un encabezado o el contenido de una celda de tabla no pueden recibir un token encima.
 *
 * ## La precedencia entre en línea
 *
 * El orden es el de CommonMark y va de mayor a menor:
 *
 * 1. **Escape** `\x` — una barra invertida anula cualquier construcción posterior.
 * 2. **Código en línea** — `` `**no**` `` es código, no énfasis. Es la regla que más se sorprende.
 * 3. **Autolink** `<https://…>` y **HTML** `<b>` — por la angled bracket.
 * 4. **Enlace o imagen** — `_[a](b)_` marca el enlace, no el énfasis que lo envuelve.
 * 5. **Tachado** `~~…~~`, **fuerte** `**…**`, **simple** `*…*`.
 *
 * ## La regla que más código cuesta: abrir y cerrar
 *
 * Un delimitador de énfasis solo abre si lo que viene detrás no es un espacio, y solo cierra si lo
 * que viene delante no lo es. Por eso `a * b` y `a*b*c` no son énfasis: el primero tiene un espacio
 * detrás, y en el segundo los dos caracteres hacen de apertura y de cierre a la vez.
 *
 * Lógica pura: sin `androidx.compose`, sin `Context`, sin `R`, sin reloj (RNF-26).
 */
internal object MarkdownInline {

    private val AUTOLINK_RE = Regex("^<((https?|ftp|mailto):[^ <>]+|[^ <>@]+@[^ <>@]+)>")
    private val HTML_TAG_RE = Regex("^</?[a-zA-Z][a-zA-Z0-9-]*(\\s+[a-zA-Z_:][a-zA-Z0-9_.:-]*(\\s*=\\s*([^\\s\"'>]+|\"[^\"]*\"|'[^']*'))?)*\\s*/?>")

    /** Caracteres que la barra invertida puede escapar. */
    private val ESCAPABLE = "\\`*_{}[]()#+-.!<>|~"

    /**
     * Tokeniza el tramo `[from, to)`. El buffer es el mismo de la pasada de bloques, y por eso
     * cualquier tramo que ya tenga token se respeta sin comprobar nada más.
     */
    fun tokenize(md: String, from: Int, to: Int, tokens: TokenBuffer) {
        var i = from
        while (i < to) {
            if (tokens.covers(i)) {
                i = tokens.endOfCoveredFrom(i, to)
                continue
            }
            val consumed = consumeAt(md, i, to, tokens)
            i = if (consumed > i) consumed else i + 1
        }
    }

    /**
     * Intenta reconocer una construcción en línea que empiece en `i`. Devuelve el índice siguiente,
     * o el mismo `i` si aquí no empieza ninguna. El orden de los `when` **es** la precedencia.
     */
    private fun consumeAt(md: String, i: Int, limit: Int, tokens: TokenBuffer): Int {
        val c = md[i]
        return when (c) {
            '\\' -> consumeEscape(md, i, limit, tokens)
            '`' -> consumeInlineCode(md, i, limit, tokens)
            '~' -> consumeStrike(md, i, limit, tokens)
            '<' -> consumeAngle(md, i, limit, tokens).takeIf { it > i } ?: consumeHtml(md, i, limit, tokens)
            '[' -> consumeLinkLike(md, i, limit, tokens, image = false)
            '!' -> if (i + 1 < limit && md[i + 1] == '[') {
                // El `!` es parte del texto de la imagen: se le pasa el inicio en `i` para que el
                // token cubra `![alt]` entero y no solo `[alt]`.
                consumeLinkLike(md, i, limit, tokens, image = true)
            } else i
            '*', '_' -> consumeEmphasis(md, i, limit, tokens, c)
            else -> i
        }
    }

    // ==========================================
    // Escape y codigo en linea
    // ==========================================

    private fun consumeEscape(md: String, i: Int, limit: Int, tokens: TokenBuffer): Int {
        if (i + 1 >= limit) return i
        if (!ESCAPABLE.contains(md[i + 1])) return i
        tokens.add(i, i + 2, MdTokenKind.ESCAPE)
        return i + 2
    }

    /**
     * Código en línea. La cuenta de acentos graves de la apertura tiene que coincidir **exactamente**
     * con la del cierre: `` ``a ` b`` `` sí es código, y `` `a``b` `` no lo es.
     *
     * La regla real de CommonMark también ignora los espacios que rozan el contenido; aquí se marca el
     * tramo entero, delimitadores incluidos, que es lo que se ve coloreado en un editor.
     */
    private fun consumeInlineCode(md: String, i: Int, limit: Int, tokens: TokenBuffer): Int {
        var fence = 0
        while (i + fence < limit && md[i + fence] == '`') fence++
        var cursor = i + fence
        while (cursor < limit) {
            if (md[cursor] != '`') {
                cursor++
                continue
            }
            var closing = 0
            while (cursor + closing < limit && md[cursor + closing] == '`') closing++
            if (closing == fence) {
                tokens.add(i, cursor + closing, MdTokenKind.INLINE_CODE)
                return cursor + closing
            }
            cursor += closing
        }
        return i
    }

    // ==========================================
    // Autolink y HTML
    // ==========================================

    private fun consumeAngle(md: String, i: Int, limit: Int, tokens: TokenBuffer): Int {
        val rest = md.substring(i, limit)
        val match = AUTOLINK_RE.find(rest) ?: return i
        tokens.add(i, i + match.value.length, MdTokenKind.AUTOLINK)
        return i + match.value.length
    }

    /**
     * `<b>` y `</b>` sí son HTML incrustado; `5 > 3` no. Se resuelve con el mismo patrón: lo que no
     * casa con el autolink se prueba contra la etiqueta y, si tampoco, no es nada.
     */
    private fun consumeHtml(md: String, i: Int, limit: Int, tokens: TokenBuffer): Int {
        val match = HTML_TAG_RE.find(md.substring(i, limit)) ?: return i
        tokens.add(i, i + match.value.length, MdTokenKind.HTML_TAG)
        return i + match.value.length
    }

    // ==========================================
    // Enlaces e imagenes
    // ==========================================

    /**
     * `[texto](destino)`, `![alt](destino)`, `[texto][ref]` y el colapsado `[ref]`.
     *
     * [bracketStart] es donde empieza el texto que se colorea: el `[` de un enlace, y el `!` de una
     * imagen, que forma parte de su etiqueta.
     *
     * El corchete que abre cuenta como parte del texto, y el texto que hay dentro **no se vuelve a
     * tokenizar**: `[**fuerte**](x)` es un enlace cuyo texto tiene énfasis, no un énfasis suelto.
     */
    private fun consumeLinkLike(md: String, bracketStart: Int, limit: Int, tokens: TokenBuffer, image: Boolean): Int {
        val i = if (image) bracketStart + 1 else bracketStart
        var cursor = i + 1
        var depth = 1
        while (cursor < limit && depth > 0) {
            when (md[cursor]) {
                '\\' -> cursor++
                '[' -> depth++
                ']' -> depth--
            }
            if (depth > 0) cursor++
        }
        if (cursor >= limit || depth > 0) return i
        val labelEnd = cursor
        val after = cursor + 1

        // Colapsado o por referencia: `[texto][ref]` y `[ref]`.
        if (after < limit && md[after] == '[') {
            var refEnd = after + 1
            while (refEnd < limit && md[refEnd] != ']') refEnd++
            if (refEnd >= limit) return i
            tokens.add(bracketStart, labelEnd + 1, MdTokenKind.LINK_TEXT)
            tokens.add(after, refEnd + 1, MdTokenKind.LINK_REF)
            return refEnd + 1
        }

        // En línea: `[texto](destino)`.
        if (after < limit && md[after] == '(') {
            var destEnd = after + 1
            var depthParen = 1
            while (destEnd < limit && depthParen > 0) {
                when (md[destEnd]) {
                    '\\' -> destEnd++
                    '(' -> depthParen++
                    ')' -> depthParen--
                }
                if (depthParen > 0) destEnd++
            }
            if (destEnd >= limit || depthParen > 0) return i
            val dest = md.substring(after + 1, destEnd).trim()
            tokens.add(bracketStart, labelEnd + 1, MdTokenKind.LINK_TEXT)
            if (dest.isNotEmpty()) {
                val destStart = after + 1 + md.substring(after + 1, destEnd).indexOfFirst { !it.isWhitespace() }
                tokens.add(destStart, destStart + dest.length, MdTokenKind.LINK_URL)
            }
            return destEnd + 1
        }

        // Colapsado: `[ref]` sin nada detrás. El texto es la propia referencia.
        val reference = md.substring(i + 1, labelEnd)
        if (reference.isBlank() || reference.startsWith("^")) return i
        tokens.add(bracketStart, labelEnd + 1, MdTokenKind.LINK_REF)
        return labelEnd + 1
    }

    // ==========================================
    // Enfasis y tachado
    // ==========================================

    private fun consumeStrike(md: String, i: Int, limit: Int, tokens: TokenBuffer): Int {
        if (i + 1 >= limit || md[i + 1] != '~') return i
        val close = md.indexOf("~~", i + 2)
        if (close < 0 || close >= limit) return i
        if (close == i + 2) return i
        tokens.add(i, close + 2, MdTokenKind.STRIKETHROUGH)
        return close + 2
    }

    /**
     * Énfasis con `*` o `_`, en su forma fuerte (`**`) o simple (`*`).
     *
     * La forma fuerte se prueba antes que la simple porque `**` empieza igual que `*`: si se
     * probara al revés, `**fuerte**` se comería solo la primera mitad.
     */
    private fun consumeEmphasis(md: String, i: Int, limit: Int, tokens: TokenBuffer, marker: Char): Int {
        val strong = i + 1 < limit && md[i + 1] == marker
        val openLen = if (strong) 2 else 1
        if (!canOpen(md, i, limit, openLen)) return i

        val close = findClosing(md, i + openLen, limit, marker, openLen)
        if (close < 0) return i
        tokens.add(i, close + openLen, if (strong) MdTokenKind.STRONG else MdTokenKind.EM)
        return close + openLen
    }

    /**
     * Un delimitador abre si lo que viene detrás no es un espacio. Que se admita dentro de una
     * palabra depende del carácter: `_` no abre dentro de una palabra (`snake_case`), `*` tampoco
     * (`a*b*c`), porque en los dos casos el delimitador haría de apertura y de cierre a la vez y
     * CommonMark lo veta. La diferencia real entre `*` y `_` es la pareja: `a *b* c` sí emphasises.
     *
     * El carácter anterior se mira con [charBefore], que devuelve un espacio al principio del
     * documento: mirar `md[i - 1]` en crudo revienta con índice −1 justo en el caso más frecuente,
     * un énfasis en la primera línea.
     */
    private fun canOpen(md: String, i: Int, limit: Int, len: Int): Boolean {
        val after = i + len
        if (after >= limit) return false
        if (md[after].isWhitespace()) return false
        if (charBefore(md, i).isLetterOrDigit()) return false
        return true
    }

    /** El cierre busca el mismo carácter y longitud, y no puede estar pegado a un espacio por delante. */
    private fun findClosing(md: String, from: Int, limit: Int, marker: Char, len: Int): Int {
        var cursor = from
        while (cursor < limit) {
            if (md[cursor] != marker) {
                cursor++
                continue
            }
            var run = 0
            while (cursor + run < limit && md[cursor + run] == marker) run++
            if (run >= len && !charBefore(md, cursor).isWhitespace()) return cursor
            cursor += run
        }
        return -1
    }

    /** Carácter en [index], o espacio si el índice está al principio del documento. */
    private fun charBefore(md: String, index: Int): Char =
        if (index <= 0) ' ' else md[index - 1]
}