package com.example

import com.example.data.model.TableData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del modelo del bloque de tabla.
 *
 * Son pruebas puras a propósito: sin entorno de pruebas ni pantalla, para que se ejecuten en la
 * plataforma de desarrollo, donde la suite que sí lo necesita está bloqueada.
 *
 * La tabla tiene una sola lista de filas y la cabecera es la primera de ellas (RF-46), así que no
 * hay ninguna lista de títulos aparte. Las tablas se montan **leyendo JSON o HTML**, no con el
 * constructor: montado por el constructor, un modelo con los títulos aparte se parecería al nuevo
 * (sin ninguna fila de cabecera) y estas pruebas pasarían sin comprobar nada.
 */
class TableDataTest {

    /** Tabla leída de una nota escrita con el formato anterior, con su fila de cabecera. */
    private fun read(json: String): TableData = TableData.fromJson(json)!!

    private fun readHtml(html: String): TableData = TableData.fromLegacyHtml(html)!!

    /** Una tabla de dos columnas con cabecera y una fila de datos, en el formato antiguo. */
    private val twoByTwo = """{"headers":["A","B"],"rows":[["1","2"]]}"""

    // RNF-19: leer una nota escrita con el formato anterior no pierde texto.

    @Test
    fun `legacy headers become the first row`() {
        assertEquals(
            listOf(listOf("A", "B"), listOf("1", "2")),
            read(twoByTwo).rows,
        )
    }

    @Test
    fun `legacy headers come first even when the header row was turned off`() {
        // Los títulos estaban guardados pero invisibles. Descartarlos sería borrar texto del
        // usuario, así que pasan a ser la primera fila, visible y como dato (decisión 3).
        val data = read("""{"headers":["A","B"],"rows":[["1","2"]],"showHeader":false}""")
        assertEquals(listOf(listOf("A", "B"), listOf("1", "2")), data.rows)
        assertFalse(data.showHeader)
    }

    @Test
    fun `a legacy table with no titles does not gain a phantom row`() {
        assertEquals(
            listOf(listOf("1", "2")),
            read("""{"headers":[],"rows":[["1","2"]]}""").rows,
        )
    }

    @Test
    fun `a table already written is read without duplicating its first row`() {
        assertEquals(
            listOf(listOf("A", "B"), listOf("1", "2")),
            read("""{"rows":[["A","B"],["1","2"]],"showHeader":true}""").rows,
        )
    }

    @Test
    fun `legacy rows keep their order and their content`() {
        val data = read("""{"headers":["H"],"rows":[["1"],["2"],["3"]]}""")
        assertEquals(listOf(listOf("1"), listOf("2"), listOf("3")), data.rows.drop(1))
    }

    @Test
    fun `the weights, the background and the two options survive the read`() {
        val data = read(
            """{"headers":["A"],"rows":[["1"]],"columnWeights":[2.0],
               "bgColorHex":"FFF8E1","showHeader":false,"showHeaderColumn":true}""",
        )
        assertEquals(listOf(2f), data.columnWeights)
        assertEquals("FFF8E1", data.bgColorHex)
        assertFalse(data.showHeader)
        assertTrue(data.showHeaderColumn)
    }

    // RF-46: el dato no guarda ninguna lista de títulos aparte.

    @Test
    fun `writing and reading back gives the same table`() {
        val original = read(twoByTwo)
        assertEquals(original, TableData.fromJson(original.toJson()))
    }

    @Test
    fun `a written table carries no separate list of titles`() {
        assertFalse(read(twoByTwo).toJson().contains("headers"))
    }

    @Test
    fun `writing a table that had a header row keeps it as the first row`() {
        val json = read(twoByTwo).toJson()
        assertEquals(listOf(listOf("A", "B"), listOf("1", "2")), read(json).rows)
    }

    // RF-52: el número de columnas sale de las filas.

    @Test
    fun `the column count comes from the widest row`() {
        assertEquals(3, read("""{"headers":["a"],"rows":[["1"],["2","3","4"]]}""").columnCount)
    }

    @Test
    fun `a table with no rows has no columns`() {
        assertEquals(0, read("""{"rows":[]}""").columnCount)
    }

    // RF-50: cada fila de la exportación HTML va dentro de su fila.

    @Test
    fun `the first row is written inside a row of its own when the header row is on`() {
        val html = read(twoByTwo).toHtml()
        assertTrue(html, html.contains("<tr><th>A</th><th>B</th></tr>"))
    }

    @Test
    fun `no title cell escapes the row that contains it`() {
        val html = read(twoByTwo).toHtml()
        // Antes de quitar las filas tiene que haber celdas de cabecera, o la comprobación
        // de abajo pasaría sin comprobar nada.
        assertTrue(html, html.contains("<th"))
        // Se quitan las filas bien formadas: lo que quede fuera de ellas no pertenece a la tabla.
        val outsideRows = Regex("<tr>.*?</tr>", RegexOption.DOT_MATCHES_ALL).replace(html, "")
        assertFalse(outsideRows, outsideRows.contains("<th"))
        assertFalse(outsideRows, outsideRows.contains("<td"))
    }

    @Test
    fun `every row of the table is inside its own row`() {
        val html = read("""{"headers":["A"],"rows":[["1"],["2"],["3"]]}""").toHtml()
        assertEquals(4, Regex("<tr>").findAll(html).count())
        assertEquals(4, Regex("</tr>").findAll(html).count())
    }

    @Test
    fun `the rows come out in order`() {
        val html = read("""{"headers":["H"],"rows":[["1"],["2"],["3"]]}""").toHtml()
        val order = listOf("H", "1", "2", "3").map { html.indexOf(">$it<") }
        assertEquals(order.sorted(), order)
    }

    // RF-48: desactivar la cabecera no borra la fila.

    @Test
    fun `with the header row off the first row is still exported as a data row`() {
        val html = read("""{"headers":["A","B"],"rows":[["1","2"]],"showHeader":false}""").toHtml()
        assertTrue(html, html.contains("<tr><td>A</td><td>B</td></tr>"))
        assertFalse(html, html.contains("<th"))
    }

    @Test
    fun `with the header row off no row is lost`() {
        val on = read(twoByTwo).toHtml()
        val off = read("""{"headers":["A","B"],"rows":[["1","2"]],"showHeader":false}""").toHtml()
        assertEquals(Regex("<tr>").findAll(on).count(), Regex("<tr>").findAll(off).count())
    }

    // RF-51: las cuatro exportaciones cuentan las mismas filas.

    @Test
    fun `every row of the model reaches the html export`() {
        val data = read("""{"headers":["A"],"rows":[["1"],["2"],["3"]]}""")
        assertEquals(data.rows.size, Regex("<tr>").findAll(data.toHtml()).count())
    }

    // El escapado de celdas no cambia con la forma de la cabecera.

    @Test
    fun `header cells are html escaped`() {
        val html = read("""{"headers":["A <b>H</b>"],"rows":[["x & y"]]}""").toHtml()
        assertTrue(html, html.contains("&lt;b&gt;H&lt;/b&gt;"))
        assertTrue(html, html.contains("x &amp; y"))
        assertFalse(html, html.contains("<b>H</b>"))
    }

    @Test
    fun `a cell that needs no escaping is left alone`() {
        assertTrue(read(twoByTwo).toHtml().contains(">A<"))
    }

    // La lectura del HTML antiguo: los títulos iban fuera de cualquier fila.

    @Test
    fun `legacy html without a row around the titles still reads`() {
        assertEquals(
            listOf(listOf("A", "B"), listOf("1", "2")),
            readHtml("<table><th>A</th><th>B</th><tr><td>1</td><td>2</td></tr></table>").rows,
        )
    }

    @Test
    fun `html with the titles inside a row reads the same`() {
        assertEquals(
            listOf(listOf("A", "B"), listOf("1", "2")),
            readHtml("<table><tr><th>A</th><th>B</th></tr><tr><td>1</td><td>2</td></tr></table>").rows,
        )
    }

    @Test
    fun `html without titles is read as data rows`() {
        assertEquals(
            listOf(listOf("1", "2")),
            readHtml("<table><tr><td>1</td><td>2</td></tr></table>").rows,
        )
    }

    @Test
    fun `an empty table is not read as a table`() {
        assertNull(TableData.fromLegacyHtml("<table></table>"))
        assertNull(TableData.fromJson(null))
    }

    // La tabla por defecto trae su primera fila, como las demás.

    @Test
    fun `the default table is a header row plus three data rows`() {
        val data = TableData.default3x3()
        assertEquals(4, data.rows.size)
        assertEquals(3, data.columnCount)
        assertTrue(data.rows.all { it.size == 3 })
    }
}