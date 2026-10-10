package com.estrin217.editordecodigo.syntax

/**
 * Acumulador de tokens. **No filtra ni ordena**: añade lo que le piden. El no-solape lo
 * garantiza el recorrido, y si alguna vez se violara hay un test que lo detecta. Prefiero que lo
 * detecte el test a esconderlo detrás de un `if`.
 *
 * Sabe además qué tramos cubre, que es lo que necesita la pasada en línea para no pisar un
 * token de bloque. La lista está ordenada por construcción, así que la búsqueda es binaria.
 */
internal class TokenBuffer {
    private val tokens = mutableListOf<MdToken>()

    fun add(start: Int, end: Int, kind: MdTokenKind) {
        if (end <= start) return
        tokens += MdToken(start, end, kind)
    }

    /** ¿Hay algún token que cubra la posición [index]? */
    fun covers(index: Int): Boolean = findCovering(index) != null

    /**
     * Fin del tramo cubierto que empieza en [index], acotado a [limit]. Devuelve [index] si no
     * hay cobertura, para que quien llama avance de uno en uno.
     */
    fun endOfCoveredFrom(index: Int, limit: Int): Int {
        val token = findCovering(index) ?: return index
        return minOf(token.end, limit)
    }

    private fun findCovering(index: Int): MdToken? {
        var low = 0
        var high = tokens.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val token = tokens[mid]
            when {
                index < token.start -> high = mid - 1
                index >= token.end -> low = mid + 1
                else -> return token
            }
        }
        return null
    }

    fun build(): List<MdToken> = tokens.toList()
}
