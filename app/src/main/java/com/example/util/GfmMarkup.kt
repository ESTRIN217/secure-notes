package com.example.util

/**
 * Gramática de GitHub Flavored Markdown para el markup heredado de las notas (spec 016).
 *
 * Aquí vive **todo lo que se puede decidir sin pantalla**: qué línea es un subrayado setext, dónde está una
 * definición de enlace por referencia, si un `*` pegado a una letra abre énfasis, dónde cierra una tirada de
 * acentos graves y qué línea es la separadora de una tabla.
 *
 * Está aparte del bucle que pinta el texto a propósito: el bucle mezcla «qué es esto» con «cómo se escribe en
 * el buffer», y la segunda mitad no se puede probar sin pantalla. Separadas, cada pregunta se responde con
 * dos cadenas de entrada y una respuesta, y eso corre en la plataforma de pruebas.
 *
 * **Sin `import androidx.compose`, sin `Context`, sin `R` y sin reloj** (RNF-45). Por eso sus pruebas son JUnit
 * puro y no llevan `RobolectricTestRunner`.
 */
object GfmMarkup {

    /**
     * Definiciones de enlace por referencia de un documento: `[etiqueta]: url`.
     *
     * Las guarda todas en una lista **y** en un índice por posición de línea, porque hacen falta las dos
     * cosas: el mapa para resolver un enlace que ya se está pintando, y el rango para poder ocultar la
     * definición cuando el bucle llega a su línea.
     */
    class ReferenceDefinitions private constructor(
        private val byLabel: Map<String, String>,
        private val byStart: Map<Int, ReferenceDefinition>
    ) {
        /** URL de la etiqueta, o `null` si el documento **no** la define. */
        fun urlFor(label: String): String? = byLabel[normalizeLabel(label)]

        /** Definición que empieza en esa posición, o `null` si ahí no hay ninguna. */
        fun at(start: Int): ReferenceDefinition? = byStart[start]

        val isEmpty: Boolean get() = byLabel.isEmpty()

        companion object {
            val EMPTY = ReferenceDefinitions(emptyMap(), emptyMap())

            fun of(definitions: List<ReferenceDefinition>): ReferenceDefinitions {
                if (definitions.isEmpty()) return EMPTY
                val byLabel = LinkedHashMap<String, String>(definitions.size)
                val byStart = LinkedHashMap<Int, ReferenceDefinition>(definitions.size)
                for (d in definitions) {
                    // Gana la **primera** definición repetida (caso límite 4), igual que en GFM: si una nota
                    // trajera una definición falsa al final, sobreescribir con ella una legítima al principio
                    // cambiaría a dónde lleva un enlace que el usuario escribió.
                    byLabel.putIfAbsent(d.label, d.url)
                    byStart.putIfAbsent(d.start, d)
                }
                return ReferenceDefinitions(byLabel, byStart)
            }
        }
    }

    /**
     * Recorre el documento **una vez** y devuelve sus definiciones de referencia.
     *
     * Es una pre-pasada porque el bucle que pinta es de una sola pasada y una definición puede estar **después**
     * del enlace que la usa: sin esto, `[texto][r]` no encontraría nunca un `[r]:` de abajo.
     *
     * Sale temprano si el documento no tiene ni un corchete: la mayoría de las notas son HTML propio y no
     * Markdown, y recorrerlas dos veces para nada es un coste que se nota en documentos largos.
     */
    fun collectReferenceDefinitions(text: String): ReferenceDefinitions {
        if (!text.contains('[')) return ReferenceDefinitions.EMPTY

        val found = mutableListOf<ReferenceDefinition>()
        var lineStart = 0
        while (lineStart <= text.length) {
            val newlineAt = text.indexOf('\n', lineStart)
            val lineEnd = if (newlineAt == -1) text.length else newlineAt
            readDefinition(text, lineStart, lineEnd)?.let { found.add(it) }
            if (newlineAt == -1) break
            lineStart = newlineAt + 1
        }
        return ReferenceDefinitions.of(found)
    }

    private fun readDefinition(text: String, lineStart: Int, lineEnd: Int): ReferenceDefinition? {
        // Se busca sobre **la línea**, no sobre el documento entero: el ancla `^` de Kotlin solo casa al
        // principio de la cadena, y con `Regex.find(text, start)` sobre el documento no se movería nunca de
        // la primera línea. Es la misma trampa que ya se pagó en la spec 014.
        val line = text.substring(lineStart, lineEnd)
        val match = referenceDefinitionRegex.find(line) ?: return null
        val label = normalizeLabel(match.groupValues[1])
        if (label.isEmpty()) return null
        return ReferenceDefinition(label, match.groupValues[2], lineStart, lineEnd)
    }

    /**
     * Qué encabezado setext,**si alguno**, es el subrayado de la línea anterior.
     *
     * La decisión se toma con **la línea de arriba**, no con la forma de esta: `---` debajo de un párrafo es un
     * encabezado de segundo nivel, y `---` debajo de una línea vacía es una raya de separación (casos límite 1
     * y 2). Por eso el parámetro no es el texto de la línea anterior sino la respuesta a «¿la de arriba tenía
     * texto?», que es lo que el bucle ya sabe.
     */
    fun setextUnderlineKind(underline: String, previousLineIsPlainText: Boolean): SetextKind? {
        if (!previousLineIsPlainText) return null
        val trimmed = underline.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.all { it == '=' }) return SetextKind.H1
        if (trimmed.all { it == '-' }) return SetextKind.H2
        return null
    }

    // ---------------------------------------------------------------------------------------------------
    // Puente con el bucle del analizador
    //
    // Lo que sigue son las tres preguntas que el bucle **no puede responder solo**, porque dependen de
    // mirar la línea anterior o de un índice del documento. Se dejan aquí para que la decisión siga siendo
    // comprobable sin pantalla.
    // ---------------------------------------------------------------------------------------------------

    /**
     * Si la línea que empieza en [lineStart] es un subrayado setext, y de qué nivel.
     *
     * Devuelve `null` —y entonces la línea es una raya, como siempre— cuando no hay línea de arriba con
     * texto (casos límite 1 y 2). La respuesta no depende del texto de esa línea anterior, sino de lo que el
     * bucle ya sabe: si la línea pintada antes teve contenido.
     */
    fun setextAt(text: String, lineStart: Int, previousLineHadText: Boolean): SetextKind? {
        val lineEnd = lineEndOf(text, lineStart)
        if (lineEnd <= lineStart) return null
        return setextUnderlineKind(text.substring(lineStart, lineEnd), previousLineHadText)
    }

    /**
     * Si la línea que empieza en [lineStart] es la separadora de una tabla **y** la línea de arriba trae al
     * menos una tubería (RF-97).
     *
     * Las dos condiciones van juntas porque **la que hace tabla es el par**, no una fila sola: una fila de
     * `---` suelta es una raya.
     */
    fun tableDelimiterAt(text: String, lineStart: Int): Boolean {
        val lineEnd = lineEndOf(text, lineStart)
        if (lineEnd <= lineStart) return false
        if (!isTableDelimiterRow(text.substring(lineStart, lineEnd))) return false
        if (lineStart == 0) return false
        // La línea de arriba va de un `\n` **antes** del que cierra la actual, hasta el `\n` anterior a ese.
        // Buscar solo el `\n` de `lineStart - 1` daría el final de la línea de arriba, no su principio.
        val previousEnd = text.lastIndexOf('\n', lineStart - 1)
        val beforePrevious = if (previousEnd <= 0) -1 else text.lastIndexOf('\n', previousEnd - 1)
        val previousStart = beforePrevious + 1
        if (previousStart >= previousEnd) return false
        return pipeCount(text.substring(previousStart, previousEnd)) > 0
    }

    /**
     * Índice del primer `#` de cierre de un encabezado, o `null` si la línea no cierra ninguno (RF-93).
     *
     * Cierra solo lo que lleva **al menos un espacio** delante: `## Título ##` cierra y `## Título#` no,
     * porque ahí los `#` son parte del título.
     */
    fun closingHashStart(text: String, lineStart: Int, contentStart: Int): Int? {
        val lineEnd = lineEndOf(text, lineStart)
        var i = lineEnd
        while (i > contentStart && text[i - 1] == '#') i--
        val hashes = lineEnd - i
        if (hashes == 0 || i == contentStart) return null
        return if (text[i - 1] == ' ') i else null
    }

    /** Longitud de la tirada de acentos graves que empieza en [index]. */
    fun backtickRunLength(text: String, index: Int): Int {
        var count = 0
        var i = index
        while (i < text.length && text[i] == '`') {
            count++
            i++
        }
        return count
    }

    /** Fin de la línea que empieza en [lineStart], sin contar el `\n`. */
    private fun lineEndOf(text: String, lineStart: Int): Int {
        val newline = text.indexOf('\n', lineStart)
        return if (newline == -1) text.length else newline
    }

    /**
     * Autolink con ángulos en la posición [index], o `null` si ahí no lo hay.
     *
     * Devolver `null` **no es un error**: es lo que deja pasar el `<` al reconocimiento de etiquetas HTML que
     * ya existía, y por eso ninguna etiqueta existente cambia de aspecto. Hoy `<https://ejemplo.com>` se
     * pierde, porque `HtmlTagParser` lee la URL como nombre de etiqueta y oculta el tramo entero (RF-92).
     */
    fun angleAutolink(text: String, index: Int): AngleAutolink? {
        if (index < 0 || index >= text.length || text[index] != '<') return null
        val match = angleAutolinkRegex.matchAt(text, index) ?: return null
        return AngleAutolink(
            url = match.groupValues[1],
            start = match.range.first,
            end = match.range.last + 1
        )
    }

    /**
     * Si un delimitador de énfasis de [markerLength] caracteres en [index] puede **abrir** énfasis.
     *
     * Abre si lo que viene detrás **no** es un espacio y lo de delante **no** es letra ni dígito. Y cierra
     * si lo de delante no es un espacio.
     *
     * Es la **misma** regla que `MarkdownInline` de la spec 012, y a propósito: el editor de código y el de
     * notas son dos superficies del mismo dialecto, y si decidieran distinto, el mismo texto se vería distinto
     * según dónde esté escrito. `a*b*c` no es énfasis porque el delimitador haría de apertura y de cierre a la
     * vez, y CommonMark lo veta; `a *b* c` sí lo es porque son dos delimitadores distintos.
     */
    fun emphasisDelimiterAppliesAt(text: String, index: Int, markerLength: Int): Boolean {
        val after = index + markerLength
        if (after >= text.length) return false
        if (text[after].isWhitespace()) return false
        return !charBefore(text, index).isLetterOrDigit()
    }

    /** Si un delimitador de énfasis de [markerLength] caracteres en [index] puede **cerrar** énfasis. */
    fun emphasisDelimiterClosesAt(text: String, index: Int, markerLength: Int): Boolean {
        if (index >= text.length) return false
        if (!charBefore(text, index).isWhitespace()) return true
        return index + markerLength >= text.length
    }

    /**
     * Índice del **primer** acento grave que forma una tirada del tamaño de la que abrió el código, o `null`
     * si no hay (RF-95).
     *
     * Con N acentos hay que cerrar con N. Hoy el acento grave alterna el estilo cada vez que aparece, y con
     * dos de apertura el resultado es un estilo que se abre y se cierra en el acto y un interior sin pintar.
     */
    fun closingBacktickRun(text: String, openEnd: Int): Int? {
        val runLength = backtickRunEndingAt(text, openEnd - 1)
        if (runLength == 0) return null
        var searchFrom = openEnd
        while (true) {
            val candidate = text.indexOf('`', searchFrom)
            if (candidate == -1) return null
            if (countBackticksAt(text, candidate) == runLength) return candidate
            searchFrom = candidate + 1
        }
    }

    /**
     * Longitud de la tirada de acentos graves que **termina** en [lastIndex].
     *
     * [closingBacktickRun] recibe el final **exclusivo** de la tirada de apertura, así que hay que volver
     * hacia atrás para saber dónde empezaba: contar hacia delante desde el último acento contaría uno solo.
     */
    private fun backtickRunEndingAt(text: String, lastIndex: Int): Int {
        if (lastIndex < 0 || lastIndex >= text.length || text[lastIndex] != '`') return 0
        var start = lastIndex
        while (start - 1 >= 0 && text[start - 1] == '`') start--
        return countBackticksAt(text, start)
    }

    /** Carácter en [index], o espacio si el índice está al principio del documento. */
    private fun charBefore(text: String, index: Int): Char =
        if (index <= 0) ' ' else text[index - 1]

    private fun countBackticksAt(text: String, index: Int): Int {
        if (index < 0 || index >= text.length || text[index] != '`') return 0
        var count = 0
        var i = index
        while (i < text.length && text[i] == '`') {
            count++
            i++
        }
        return count
    }

    /**
     * Si la línea es la fila de separadores de una tabla con tubería.
     *
     * No basta con «una línea con tuberías»: lo que hace tabla a una línea es **tener la forma** de la
     * separadora, y el que dice que hay tabla es el par separadora + cabecera (RF-97).
     *
     * Se exige `|` inicial porque la capa de bloques, en `preprocessMarkdownBlocks`, solo convierte tablas que
     * empiezan por `|`. Aceptar aquí lo que allí se rechaza haría que las dos capas decidieran distinto sobre
     * el mismo texto, y entonces habría que preguntar «¿en qué capa estoy?».
     */
    fun isTableDelimiterRow(line: String): Boolean {
        val trimmed = line.trim()
        if (!trimmed.startsWith('|')) return false
        return pipeCount(trimmed) - 1 >= 2 && trimmed.all { it == '|' || it == '-' || it == ':' || it == ' ' || it == '\t' }
    }

    /** Cuántas tuberías **de columna** tiene la línea. Las escapadas con barra no cuentan. */
    fun pipeCount(line: String): Int {
        var count = 0
        var i = 0
        while (i < line.length) {
            if (line[i] == '\\' && i + 1 < line.length && line[i + 1] == '|') {
                i += 2
                continue
            }
            if (line[i] == '|') count++
            i++
        }
        return count
    }

    /**
     * Cuántos `#` de cierre hay al final de un encabezado, **contando solo los que llevan espacio delante**
     * (RF-93).
     *
     * `## Título ##` cierra; `## Título#` no, porque ahí los `#` son parte del título y no su cierre.
     */
    fun closingHashCount(headingText: String): Int {
        var end = headingText.length
        while (end > 0 && headingText[end - 1] == '#') end--
        val hashes = headingText.length - end
        if (hashes == 0 || end == 0) return 0
        return if (headingText[end - 1] == ' ') hashes else 0
    }

    private fun normalizeLabel(label: String): String =
        label.trim().replace(whitespaceRegex, " ").lowercase()

    private fun isWordChar(char: Char): Boolean = char.isLetterOrDigit()

    private val whitespaceRegex = Regex("\\s+")

    // Hasta tres espacios de sangría, `[etiqueta]:` y la URL: entre ángulos completa, o hasta el primer
    // espacio. El rango se acota a la línea porque una definición no puede pasar de ella.
    private val referenceDefinitionRegex = Regex("^ {0,3}\\[([^\\]]+)\\]:[ \\t]*(<[^<>\\n]*>|\\S+)")

    // Alternativa 1: esquema conocido o `mailto:`. Alternativa 2: correo. Todo lo demás (`<b>`, `<url=…>`)
    // no casa y vuelve al camino de etiquetas. El grupo 1 captura la URL **sin** ángulos, y el `<`/`>` van
    // dentro del patrón porque el rango que sale los incluye y son los que hay que ocultar. Sin ancla `^`,
    // porque `angleAutolink` comprueba a mano que la coincidencia empieza en la posición que se le pasó.
    private val angleAutolinkRegex = Regex(
        "<((?:https?://|mailto:)[^\\s<>]+|[^\\s<>@]+@[^\\s<>@]+\\.[^\\s<>@]+)>"
    )
}

/** Nivel de un encabezado setext. */
enum class SetextKind { H1, H2 }

/** Una definición de enlace por referencia: la etiqueta, la URL y el rango que ocupa en el documento. */
data class ReferenceDefinition(val label: String, val url: String, val start: Int, val end: Int)

/** Autolink con ángulos: la URL **sin** los ángulos, y el rango completo con ellos. */
data class AngleAutolink(val url: String, val start: Int, val end: Int)
