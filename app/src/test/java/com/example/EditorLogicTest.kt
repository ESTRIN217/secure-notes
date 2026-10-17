package com.example

import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.TextBaseline
import com.example.data.model.TextSegment
import com.example.ui.editor.ExternalTarget
import com.example.ui.editor.applyTagToSelection
import com.example.ui.editor.buildTableRows
import com.example.ui.editor.findUrlAt
import com.example.ui.editor.applyTransformForTag
import com.example.ui.editor.buildMatchRanges
import com.example.ui.editor.clearTagValue
import com.example.ui.editor.computeToolbarStyle
import com.example.ui.editor.convertBlockAt
import com.example.ui.editor.deleteBlockAt
import com.example.ui.editor.headingTypeForTag
import com.example.ui.editor.externalTargetFor
import com.example.ui.editor.insertPlanFor
import com.example.ui.editor.insertionIndex
import com.example.ui.editor.isAudioFile
import com.example.ui.editor.isTextLikeFile
import com.example.ui.editor.moveBlockBy
import com.example.ui.editor.removeTransformForTag
import com.example.ui.editor.setTagValue
import com.example.ui.editor.styleApplies
import com.example.ui.editor.tagValueOf
import com.example.util.RichTextConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de la lógica comprobable del editor de notas (spec 014, RF-87).
 *
 * Todo lo que se prueba aquí son **funciones puras**: reciben lo que necesitan por parámetro y
 * devuelven el resultado. Los ficheros de donde vienen **no importan nada de composición**, y eso
 * no es purismo: es la condición para poder ejercitarlos en la plataforma de desarrollo, donde la
 * mitad de la suite no corre por falta de runtime nativo de Robolectric. Si mañana alguien añade un
 * `import androidx.compose` a uno de ellos, estas pruebas dejan de correr sin que se note, y por eso
 * hay una prueba que lo comprueba (la última de esta clase).
 *
 * Ninguna de estas funciones **decide** nada de la interfaz: describen. Quién las usa y qué se pinta
 * con ellas es cosa de la pantalla.
 */
class EditorLogicTest {

    // ── Transformaciones de etiqueta sobre un tramo ───────────────────────────────────────

    @Test
    fun `una etiqueta se aplica al tramo que la sostiene`() {
        assertFalse(styleApplies("b", TextSegment("hola")))
        assertTrue(styleApplies("b", applyTransformForTag("b", TextSegment("hola"))))
    }

    @Test
    fun `cada etiqueta toca su campo y solo el suyo`() {
        val seg = TextSegment("hola")
        assertTrue(applyTransformForTag("b", seg).bold)
        assertTrue(applyTransformForTag("i", seg).italic)
        assertTrue(applyTransformForTag("u", seg).underline)
        assertTrue(applyTransformForTag("s", seg).strikethrough)
        assertTrue(applyTransformForTag("code", seg).code)
        assertEquals(TextBaseline.SUBSCRIPT, applyTransformForTag("sub", seg).baseline)
        assertEquals(TextBaseline.SUPERSCRIPT, applyTransformForTag("sup", seg).baseline)
    }

    @Test
    fun `sub y sup se miden por el mismo campo pero no se confunden`() {
        assertTrue(styleApplies("sub", TextSegment("x", baseline = TextBaseline.SUBSCRIPT)))
        assertFalse(styleApplies("sup", TextSegment("x", baseline = TextBaseline.SUBSCRIPT)))
        assertTrue(styleApplies("sup", TextSegment("x", baseline = TextBaseline.SUPERSCRIPT)))
    }

    @Test
    fun `quitar una etiqueta devuelve el tramo a su estado anterior`() {
        for (tag in listOf("b", "i", "u", "s", "code", "sub", "sup", "kbd", "var", "samp", "quote")) {
            val puesto = applyTransformForTag(tag, TextSegment("hola"))
            assertTrue("la etiqueta $tag no se aplicó", styleApplies(tag, puesto) || tag == "sub" || tag == "sup")
            assertEquals(
                "quitar $tag no devuelve el tramo a su estado anterior",
                TextSegment("hola"),
                removeTransformForTag(tag, puesto)
            )
        }
    }

    @Test
    fun `una etiqueta desconocida deja el tramo como estaba`() {
        val seg = TextSegment("hola", bold = true)
        assertEquals(seg, applyTransformForTag("inventada", seg))
        assertEquals(seg, removeTransformForTag("inventada", seg))
        assertFalse(styleApplies("inventada", seg))
    }

    @Test
    fun `normal apaga todos los formatos a la vez`() {
        val puesto = applyTransformForTag(
            "b",
            TextSegment("hola", italic = true, underline = true, code = true, colorHex = "FF0000")
        )
        val limpio = applyTransformForTag("normal", puesto)
        assertEquals(TextSegment("hola"), limpio)
    }

    // ── Estado activo de la barra de herramientas ─────────────────────────────────────────

    @Test
    fun `el tipo del bloque se refleja como estilo activo`() {
        assertTrue(computeToolbarStyle(BlockType.HEADING1, emptyList(), 0, true, null).contains("h1"))
        assertTrue(computeToolbarStyle(BlockType.HEADING3, emptyList(), 0, true, null).contains("h3"))
        assertTrue(
            "un bloque normal no enciende ninguna etiqueta de título",
            computeToolbarStyle(BlockType.TEXT, emptyList(), 0, true, null).none { it.startsWith("h") }
        )
    }

    @Test
    fun `el estilo activo es el del tramo donde esta el cursor`() {
        val tramos = listOf(TextSegment("ab"), TextSegment("cd", bold = true))
        assertFalse(
            "el cursor está en el primer tramo, que no es negrita",
            computeToolbarStyle(BlockType.TEXT, tramos, 0, true, null).contains("b")
        )
        assertTrue(computeToolbarStyle(BlockType.TEXT, tramos, 3, true, null).contains("b"))
        assertFalse(
            "con el cursor justo detrás del último carácter y nada pendiente, el tramo no pesa: " +
                "es el comportamiento que había, y sorprende",
            computeToolbarStyle(BlockType.TEXT, tramos, 4, true, null).contains("b")
        )
    }

    @Test
    fun `con el cursor encima pesan el tramo y lo pendiente`() {
        // Los estilos **se suman**: lo pendiente se añade a lo que hay debajo, no lo reemplaza.
        val tramos = listOf(TextSegment("ab", bold = true))
        val estilos = computeToolbarStyle(
            BlockType.TEXT,
            tramos,
            0,
            collapsed = true,
            pending = TextSegment(italic = true)
        )
        assertTrue("el tramo bajo el cursor debe seguir pesando", estilos.contains("b"))
        assertTrue("lo pendiente debe añadirse", estilos.contains("i"))
    }

    @Test
    fun `con texto seleccionado pesa el tramo y no lo pendiente`() {
        val tramos = listOf(TextSegment("ab", italic = true))
        val estilos = computeToolbarStyle(
            BlockType.TEXT,
            tramos,
            0,
            collapsed = false,
            pending = TextSegment(bold = true)
        )
        assertTrue(estilos.contains("i"))
        assertFalse("con selección el estilo pendiente no se aplica", estilos.contains("b"))
    }

    @Test
    fun `un cursor mas alla del texto no enciende estilos por el tramo`() {
        val tramos = listOf(TextSegment("ab", bold = true))
        assertFalse(computeToolbarStyle(BlockType.TEXT, tramos, 99, true, null).contains("b"))
    }

    @Test
    fun `el estado de la barra no calcula colores`() {
        // RF-86: se calculaban dos colores que nadie leía, con cuatro conversiones por recomposición.
        assertEquals(setOf("h1"), computeToolbarStyle(BlockType.HEADING1, emptyList(), 0, true, null))
    }

    // ── Resaltado de la búsqueda ─────────────────────────────────────────────────────────
    //
    // Los rangos llevan **final exclusivo**, igual que el rango de texto que usaba el editor: un
    // rango `0..3` sobre "abc" abarca los caracteres 0, 1 y 2. Por eso una coincidencia de tres
    // letras se escribe `0..3` y no `0..2`.

    /** Sin transformación, el índice limpio es el índice del original. */
    private val directo: (Int) -> Int = { it }

    @Test
    fun `la busqueda devuelve una coincidencia por aparicion`() {
        assertEquals(listOf(0..3, 3..6, 6..9), buildMatchRanges("abcabcabc", "abc", false, true, directo))
    }

    @Test
    fun `las apariciones se solapan cuando las letras se repiten`() {
        // "aaa" casa en 0 y en 1: la búsqueda se mueve de una en una, no salta la coincidencia.
        assertEquals(listOf(0..1, 1..2, 2..3), buildMatchRanges("aaa", "a", false, true, directo))
    }

    @Test
    fun `la busqueda ignora mayusculas cuando se le pide`() {
        assertEquals(listOf(0..3, 3..6), buildMatchRanges("abcABCx", "abc", false, false, directo))
        assertEquals(listOf(0..3), buildMatchRanges("abcABCx", "abc", false, true, directo))
    }

    @Test
    fun `la palabra completa no casa dentro de otra palabra`() {
        assertEquals(listOf(0..3, 4..7), buildMatchRanges("abc abc", "abc", true, true, directo))
        assertEquals(emptyList<IntRange>(), buildMatchRanges("abcdef", "abc", true, true, directo))
    }

    @Test
    fun `una busqueda vacia no casa nada`() {
        assertEquals(emptyList<IntRange>(), buildMatchRanges("abc", "", false, true, directo))
    }

    @Test
    fun `los indices se traducen al texto original cuando hay etiquetas que ocultas`() {
        // El texto que se lee son 9 caracteres; el que se guarda lleva 7 de etiqueta delante, así
        // que la segunda coincidencia, que empieza en el 5 limpio, cae en el 12 del original. El
        // final es exclusivo: la coincidencia va del 7 al 10 y se anota 7..11.
        val conSieteDeEtiquetaAntes: (Int) -> Int = { it + 7 }
        assertEquals(
            listOf(7..11, 12..16),
            buildMatchRanges("hola hola", "hola", false, true, conSieteDeEtiquetaAntes)
        )
    }

    @Test
    fun `una consulta con signos de expresion regular se busca literal`() {
        // Un punto es un comodín en una expresión regular; lo que se teclea se busca literal.
        assertEquals(listOf(1..2, 5..6), buildMatchRanges("a.c a.c", ".", false, true, directo))
    }

    @Test
    fun `la palabra completa con parentesis no rompe la busqueda`() {
        val rangos = buildMatchRanges("a[bc]b", "[bc]", true, true, directo)
        assertEquals(1, rangos.size)
        assertEquals(1..5, rangos.first())
    }

    // ── Clasificación de un archivo por su tipo ───────────────────────────────────────────

    @Test
    fun `un archivo de texto se abre en el editor`() {
        assertTrue(isTextLikeFile("text/plain", "txt"))
        assertTrue(isTextLikeFile("text/markdown", "md"))
        assertTrue(isTextLikeFile("application/json", "json"))
    }

    @Test
    fun `un archivo que no es de texto no se abre en el editor`() {
        assertFalse(isTextLikeFile("image/png", "png"))
        assertFalse(isTextLikeFile("application/pdf", "pdf"))
        assertFalse(isTextLikeFile("", ""))
    }

    // ── Dónde va un bloque nuevo ──────────────────────────────────────────────────────────

    @Test
    fun `un bloque vacio en el cursor se sustituye en vez de empujar hacia abajo`() {
        val plan = insertPlanFor(0, "", 3)
        assertTrue(plan.replaceInPlace)
        assertEquals(0, plan.index)
    }

    @Test
    fun `un bloque con texto en el cursor se empuja hacia abajo`() {
        val plan = insertPlanFor(1, "ya hay texto", 3)
        assertFalse(plan.replaceInPlace)
        assertEquals(2, plan.index)
    }

    @Test
    fun `sin bloque activo no hay nada que sustituir`() {
        assertFalse(insertPlanFor(0, null, 3).replaceInPlace)
    }

    @Test
    fun `un bloque que solo tiene espacios cuenta como vacio`() {
        assertTrue(insertPlanFor(0, "   \n ", 3).replaceInPlace)
    }

    @Test
    fun `el indice se aguanta cuando el cursor se sale de la lista`() {
        val plan = insertPlanFor(99, null, 3)
        assertFalse(plan.replaceInPlace)
        assertEquals(3, plan.index)
    }

    @Test
    fun `al final de la lista el bloque nuevo se anade al final`() {
        val plan = insertPlanFor(2, "con texto", 3)
        assertFalse(plan.replaceInPlace)
        assertEquals(3, plan.index)
    }

    @Test
    fun `el indice de insercion es el mismo con o sin regla de sustitucion`() {
        // Sustituir deja el bloque donde estaba; insertar, un hueco más abajo.
        assertEquals(1, insertionIndex(1, replaceInPlace = true, blockCount = 5))
        assertEquals(2, insertionIndex(1, replaceInPlace = false, blockCount = 5))
        assertEquals("sustituir no empuja hacia abajo", 0, insertionIndex(0, true, 5))
        assertEquals(5, insertionIndex(4, false, 5))
        assertEquals(
            "un índice por debajo de la lista se acota al 0 y, al ser inserción, cae en el 1",
            1, insertionIndex(-3, false, 5)
        )
        assertEquals(0, insertionIndex(0, true, 0))
    }

    // ── Aplicar una etiqueta a lo seleccionado ────────────────────────────────────────────

    /** «¿tiene negrita?» y «ponle o quítale negrita», que es el par que usaba el editor. */
    /**
     * Los dos mandos que usan esto, con las **dos** preguntas que cada uno hace.
     *
     * La primera es «¿este tramo ya lo tiene?», que decide cuando hay texto seleccionado. La segunda
     * es «¿ya lo tenía la letra de debajo?», que solo cuenta para el mando conmutable: el de valor
     * la ignora a propósito, y por eso está separada.
     */
    private data class Par(
        val isOn: (TextSegment) -> Boolean,
        val charLoTenia: (TextSegment?) -> Boolean,
        val cambia: (TextSegment, Boolean) -> TextSegment
    ) {
        fun aplica(
            tramos: List<TextSegment>,
            largo: Int,
            desde: Int,
            hasta: Int,
            pendiente: TextSegment? = null
        ) = applyTagToSelection(tramos, largo, desde, hasta, pendiente, isOn, { c, b ->
            charLoTenia(c) || isOn(b)
        }, cambia)
    }

    /** El mando conmutable: si la letra de debajo ya lo tiene, la pulsación lo quita. */
    private val conmutable = Par(
        { styleApplies("b", it) },
        { c -> c != null && styleApplies("b", c) },
        { seg, puesta -> if (puesta) removeTransformForTag("b", seg) else applyTransformForTag("b", seg) }
    )

    /** El mando con valor: decide **solo** por lo pendiente, sin mirar la letra de debajo. */
    private val conValor = Par(
        { it.colorHex == "#FF0000" },
        { _ -> false },
        { seg, puesta -> if (puesta) seg.copy(colorHex = null) else seg.copy(colorHex = "#FF0000") }
    )

    @Test
    fun `con el cursor encima de una letra se pone la etiqueta`() {
        val r = conmutable.aplica(listOf(TextSegment("hola")), 4, 0, 0)
        assertEquals("hola", RichTextConverter.segmentsToPlainText(r.segments!!))
        assertTrue("la letra del cursor queda en negrita", styleApplies("b", r.segments!!.first()))
        assertTrue("y se recuerda para lo que se escriba después", r.pending.bold)
    }

    @Test
    fun `con el cursor encima pulsar dos veces quita la etiqueta`() {
        val uno = conmutable.aplica(listOf(TextSegment("hola")), 4, 0, 0)
        val dos = conmutable.aplica(uno.segments!!, 4, 0, 0, uno.pending)
        assertFalse("la segunda pulsación quita", styleApplies("b", dos.segments!!.first()))
        assertFalse(dos.pending.bold)
    }

    @Test
    fun `con el cursor al final del texto no hay letra que tocar`() {
        val r = conmutable.aplica(listOf(TextSegment("hola")), 4, 4, 4)
        assertNull("no hay letra debajo, así que no se toca ningún tramo", r.segments)
        assertTrue("pero se recuerda el estilo para lo siguiente", r.pending.bold)
    }

    @Test
    fun `con un tramo seleccionado se pone si no lo tenia todo`() {
        assertTrue(conmutable.aplica(listOf(TextSegment("ab"), TextSegment("cd")), 4, 0, 4).segments!!.all { it.bold })
    }

    @Test
    fun `con un tramo seleccionado que ya lo tenia todo pulsar quita`() {
        val tramos = listOf(TextSegment("ab", bold = true), TextSegment("cd", bold = true))
        assertFalse(conmutable.aplica(tramos, 4, 0, 4).segments!!.any { it.bold })
    }

    @Test
    fun `con un tramo seleccionado a medias la primera pulsacion lo pone todo`() {
        val tramos = listOf(TextSegment("ab", bold = true), TextSegment("cd"))
        val r = conmutable.aplica(tramos, 4, 0, 4)
        assertTrue("si solo parte lo tenía, la pulsación lo pone en todo", r.segments!!.all { it.bold })
    }

    @Test
    fun `con texto seleccionado el estilo pendiente no se toca`() {
        val pendiente = TextSegment(italic = true)
        assertTrue(conmutable.aplica(listOf(TextSegment("ab")), 2, 0, 2, pendiente).pending.italic)
    }

    @Test
    fun `una seleccion que se sale del texto se acota`() {
        val r = conmutable.aplica(listOf(TextSegment("ab")), 2, -5, 99)
        assertEquals("ab", RichTextConverter.segmentsToPlainText(r.segments!!))
    }

    @Test
    fun `el mando conmutable mira la letra de debajo para decidir`() {
        // La letra ya está en negrita: la pulsación la quita, aunque lo pendiente no la tenga.
        assertFalse(conmutable.aplica(listOf(TextSegment("ab", bold = true)), 2, 0, 0).segments!!.first().bold)
    }

    @Test
    fun `el mando con valor NO mira la letra de debajo para decidir`() {
        // La letra ya está coloreada y lo pendiente no: contra lo que haría el conmutable, aquí la
        // pulsación **vuelve a colorear**. Es la diferencia real entre los dos mandos.
        val r = conValor.aplica(listOf(TextSegment("ab", colorHex = "#FF0000")), 2, 0, 0)
        assertEquals("#FF0000", r.segments!!.first().colorHex)
    }

    @Test
    fun `el mando con valor quita cuando lo pendiente ya lleva ese valor`() {
        val pendiente = TextSegment(colorHex = "#FF0000")
        val r = conValor.aplica(listOf(TextSegment("ab", colorHex = "#FF0000")), 2, 0, 0, pendiente)
        assertNull(r.segments!!.first().colorHex)
    }

    // ── Las etiquetas que llevan valor ───────────────────────────────────────────────────

    @Test
    fun `cada etiqueta con valor toca su campo y solo el suyo`() {
        val seg = TextSegment("hola")
        assertEquals("#FF0000", setTagValue(seg, "color", "#FF0000").colorHex)
        assertEquals("#00FF00", setTagValue(seg, "bg", "#00FF00").bgColorHex)
        assertEquals("Mono", setTagValue(seg, "font", "Mono").fontFamily)
        assertEquals(18f, setTagValue(seg, "size", "18").fontSizeSp)
        assertEquals("u", setTagValue(seg, "url", "u").linkUrl)
    }

    @Test
    fun `los colores con nombre son los de Material, no los del CSS`() {
        // Sorprende y conviene fijar: «blue» **no** es #0000FF, es el azul 700 de Material. Un
        // nombre desconocido sí da nulo, y un hexadecimal sin almohadilla también.
        assertEquals("#1976D2", setTagValue(TextSegment("x"), "color", "blue").colorHex)
        assertNull(setTagValue(TextSegment("x"), "color", "azulito").colorHex)
        assertNull(setTagValue(TextSegment("x"), "color", "FF0000").colorHex)
    }

    @Test
    fun `una etiqueta con valor desconocida no toca nada`() {
        val seg = TextSegment("hola", bold = true)
        assertEquals(seg, setTagValue(seg, "inventada", "x"))
        assertEquals(seg, clearTagValue(seg, "inventada"))
        assertNull(tagValueOf(seg, "inventada"))
    }

    @Test
    fun `quitar un valor deja el resto del tramo como estaba`() {
        val seg = TextSegment("hola", bold = true, colorHex = "FF0000", fontSizeSp = 18f)
        val limpio = clearTagValue(seg, "color")
        assertNull(limpio.colorHex)
        assertTrue("no toca lo de al lado", limpio.bold)
        assertEquals(18f, limpio.fontSizeSp)
    }

    @Test
    fun `un tamano entero se lee sin los decimales`() {
        // Si no, al teclear «14» sobre un tramo que ya tiene 14 no casarían y no se podría quitar.
        assertEquals("14", tagValueOf(TextSegment("x", fontSizeSp = 14f), "size"))
        assertEquals("14.5", tagValueOf(TextSegment("x", fontSizeSp = 14.5f), "size"))
    }

    @Test
    fun `un tamano que no es numero se guarda como nulo`() {
        assertNull(setTagValue(TextSegment("x"), "size", "enorme").fontSizeSp)
    }

    // ── Qué es un archivo y con qué se abre ───────────────────────────────────────────────

    @Test
    fun `un audio se reconoce por su tipo o por su extension`() {
        assertTrue(isAudioFile("audio/mpeg", "bin"))
        assertTrue("el tipo puede venir equivocado", isAudioFile("application/octet-stream", "m4a"))
        assertTrue("y la extension puede venir en mayusculas", isAudioFile("", "MP3"))
    }

    @Test
    fun `un archivo que no es audio no lo parece`() {
        assertFalse(isAudioFile("image/png", "png"))
        assertFalse(isAudioFile("", ""))
    }

    @Test
    fun `un pdf se abre en su propia pestana`() {
        assertEquals(ExternalTarget.PDF, externalTargetFor("application/pdf", "pdf"))
    }

    @Test
    fun `un archivo sin tipo no se le pasa a ninguna aplicacion`() {
        // Sin tipo no hay Intent que construir: es distinto de «que lo abra quien sepa».
        assertEquals(ExternalTarget.SIN_TIPO, externalTargetFor("application/octet-stream", ""))
    }

    @Test
    fun `un texto se abre en el editor de codigo`() {
        assertEquals(ExternalTarget.TEXTO, externalTargetFor("text/plain", "txt"))
        assertEquals(ExternalTarget.TEXTO, externalTargetFor("application/json", "json"))
    }

    @Test
    fun `lo demas se le pasa a la aplicacion que lo sepa abrir`() {
        assertEquals(ExternalTarget.VER_CON_OTRA_APP, externalTargetFor("image/png", "png"))
    }

    @Test
    fun `el pdf gana aunque su extension parezca de texto`() {
        // El orden importa: si se mirara la extensión primero, un `.txt` con tipo de PDF se abriría
        // en el editor en vez de en el visor.
        assertEquals(ExternalTarget.PDF, externalTargetFor("application/pdf", "txt"))
    }

    // ── El enlace que hay bajo el cursor ─────────────────────────────────────────────────

    private val conEnlaces = listOf(
        TextSegment("ver "),
        TextSegment("aquí", linkUrl = "https://ejemplo.es"),
        TextSegment(" y más")
    )

    @Test
    fun `se encuentra el enlace que hay bajo una posicion`() {
        val url = findUrlAt(conEnlaces, 5)
        assertEquals("https://ejemplo.es", url?.segment?.linkUrl)
        assertEquals(4, url?.start)
        assertEquals("aquí", url?.segment?.text)
        assertEquals(4..7, url?.range)
    }

    @Test
    fun `una posicion que no cae en ningun enlace no encuentra nada`() {
        assertNull("texto sin enlaces", findUrlAt(listOf(TextSegment("hola")), 2))
        assertNull("el enlace de despues", findUrlAt(conEnlaces, 0))
        assertNull("un texto que existe pero sin enlace", findUrlAt(conEnlaces, 12))
    }

    @Test
    fun `la posicion justo detras del enlace ya no es suya`() {
        // «aquí» ocupa de la 4 a la 7. El límite es `offset < fin`: con `<=`, la 8 —que ya es de la
        // siguiente— seguiría contando como el enlace y un clic al final lo editaría.
        assertNotNull("la última letra sí es del enlace", findUrlAt(conEnlaces, 7))
        assertNull("la posición siguiente ya es de otro sitio", findUrlAt(conEnlaces, 8))
    }

    @Test
    fun `un enlace al principio del texto tambien se encuentra`() {
        val lista = listOf(TextSegment("inicio", linkUrl = "https://a.es"), TextSegment(" fin"))
        assertEquals(0, findUrlAt(lista, 0)?.start)
    }

    // ── La forma de una tabla nueva ───────────────────────────────────────────────────────

    /** Como el diálogo: «Fila %1$d». */
    private val cabecera: (Int) -> String = { "Fila $it" }

    @Test
    fun `una tabla sin cabecera tiene exactamente las filas pedidas`() {
        val filas = buildTableRows(3, 3, hasHeader = false, headerCell = cabecera)
        assertEquals(3, filas.size)
        assertTrue("sin cabecera no queda ninguna fila fantasma", filas.none { it.isNotEmpty() && it.first().isNotEmpty() })
    }

    @Test
    fun `la cabecera es la primera fila y no una fila de mas`() {
        // 3 filas de datos con cabecera son 4 filas en total, no 5.
        val filas = buildTableRows(3, 3, hasHeader = true, headerCell = cabecera)
        assertEquals(4, filas.size)
        assertEquals(listOf("Fila 1", "Fila 2", "Fila 3"), filas.first())
    }

    @Test
    fun `la cabecera y las filas de datos tienen las mismas columnas`() {
        for (filas in listOf(
            buildTableRows(2, 5, hasHeader = true, headerCell = cabecera),
            buildTableRows(2, 5, hasHeader = false, headerCell = cabecera)
        )) {
            assertTrue("todas las filas miden lo mismo", filas.all { it.size == 5 })
        }
    }

    @Test
    fun `una tabla de una sola celda no se rompe`() {
        assertEquals(listOf(listOf("")), buildTableRows(1, 1, hasHeader = false, headerCell = cabecera))
        assertEquals(2, buildTableRows(1, 1, hasHeader = true, headerCell = cabecera).size)
    }

    // ── Mandos sobre la lista de bloques ──────────────────────────────────────────────────

    /** Tres bloques con contenido distinto, para reconocer en qué orden quedan. */
    private val tres = listOf(
        DataBlock(BlockType.TEXT, content = "uno"),
        DataBlock(BlockType.TEXT, content = "dos"),
        DataBlock(BlockType.TEXT, content = "tres")
    )

    @Test
    fun `subir un bloque lo intercambia con el de encima`() {
        val resultado = moveBlockBy(tres, 2, -1)
        assertEquals(listOf("uno", "tres", "dos"), resultado?.map { it.content })
    }

    @Test
    fun `bajar un bloque lo intercambia con el de debajo`() {
        val resultado = moveBlockBy(tres, 0, 1)
        assertEquals(listOf("dos", "uno", "tres"), resultado?.map { it.content })
    }

    @Test
    fun `el primer bloque no sube y el ultimo no baja`() {
        assertNull("el primero no tiene nada encima", moveBlockBy(tres, 0, -1))
        assertNull("el ultimo no tiene nada debajo", moveBlockBy(tres, 2, 1))
    }

    @Test
    fun `un bloque que no existe no se mueve`() {
        assertNull(moveBlockBy(tres, 9, -1))
        assertNull(moveBlockBy(tres, -1, 1))
    }

    @Test
    fun `borrar quita el bloque y solo el bloque`() {
        assertEquals(listOf("uno", "tres"), deleteBlockAt(tres, 1)?.map { it.content })
    }

    @Test
    fun `la nota nunca se queda sin un solo bloque`() {
        val uno = listOf(DataBlock(BlockType.TEXT, content = "unico"))
        assertNull("borrar el ultimo bloque dejaria la nota sin nada que editar", deleteBlockAt(uno, 0))
        assertNull(deleteBlockAt(emptyList(), 0))
        assertNull(deleteBlockAt(tres, 7))
    }

    @Test
    fun `convertir cambia el tipo del bloque y su meta`() {
        val resultado = convertBlockAt(tres, 1, BlockType.QUOTE, mapOf("k" to "v"))
        assertEquals(BlockType.QUOTE, resultado?.get(1)?.type)
        assertEquals(mapOf("k" to "v"), resultado?.get(1)?.meta)
        assertEquals("no toca el contenido", "dos", resultado?.get(1)?.content)
    }

    @Test
    fun `convertir un bloque que no existe no hace nada`() {
        assertNull(convertBlockAt(tres, 9, BlockType.QUOTE))
    }

    @Test
    fun `la lista que devuelve el mando no toca la original`() {
        moveBlockBy(tres, 2, -1)
        deleteBlockAt(tres, 0)
        assertEquals(listOf("uno", "dos", "tres"), tres.map { it.content })
    }

    @Test
    fun `una etiqueta de titulo se traduce a su tipo de bloque`() {
        assertEquals(BlockType.HEADING1, headingTypeForTag("h1"))
        assertEquals(BlockType.HEADING4, headingTypeForTag("h4"))
        assertNull(headingTypeForTag("h5"))
        assertNull(headingTypeForTag("b"))
        assertNull(headingTypeForTag(""))
    }

    // ── La lógica comprobable no depende de la pantalla ───────────────────────────────────

    @Test
    fun `la logica comprobable del editor no importa nada de composicion`() {
        val paquetes = listOf(
            "SegmentTagTransforms", "ToolbarStyleState", "NoteSearchMatches", "BlockFileKinds",
            "BlockInsertPlan", "BlockCommands", "TagApplication", "NoteFileImport", "TableShape",
            "LinkLookup", "SlashCommandSelection", "NoteBlockMovePlan"
        )
        for (nombre in paquetes) {
            val fuente = java.io.File("src/main/java/com/example/ui/editor/$nombre.kt").readText()
            val importaCompose = fuente.lines().any {
                it.startsWith("import androidx.compose") || it.startsWith("import android.")
            }
            assertFalse(
                "$nombre.kt importa algo de composición o de la plataforma: sin eso sus pruebas no " +
                    "corren en la plataforma de desarrollo",
                importaCompose
            )
        }
    }
}