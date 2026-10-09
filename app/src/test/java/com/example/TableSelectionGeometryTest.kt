package com.example

import com.example.ui.table.CellKey
import com.example.ui.table.OutlineRect
import com.example.ui.table.SelectionState
import com.example.ui.table.TableMetrics
import com.example.ui.table.columnHandleLeft
import com.example.ui.table.isSignaled
import com.example.ui.table.nextFocusSet
import com.example.ui.table.rowHandleCenters
import com.example.ui.table.selectionOutline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de la geometría de la selección del bloque de tabla.
 *
 * Son pruebas puras a propósito: sin entorno de pruebas ni superficie de dibujo, para que se
 * ejecuten en la plataforma de desarrollo, donde la suite que sí lo necesita está bloqueada.
 *
 * La fila de cabecera es la fila 0 (spec 008), así que aquí no hay ninguna geometría especial para
 * ella: se mide y se coloca como cualquier otra.
 */
class TableSelectionGeometryTest {

    private val a = CellKey(row = 0, col = 0)
    private val b = CellKey(row = 1, col = 0)

    // RF-22: el resultado no depende del orden de llegada de las notificaciones.

    @Test
    fun `moving focus gives the same set whether the blur arrives first`() {
        val blurFirst = nextFocusSet(nextFocusSet(setOf(a), a, false), b, true)
        assertEquals(setOf(b), blurFirst)
    }

    @Test
    fun `moving focus gives the same set whether the focus arrives first`() {
        val focusFirst = nextFocusSet(nextFocusSet(setOf(a), b, true), a, false)
        assertEquals(setOf(b), focusFirst)
    }

    @Test
    fun `both arrival orders agree with each other`() {
        val blurFirst = nextFocusSet(nextFocusSet(setOf(a), a, false), b, true)
        val focusFirst = nextFocusSet(nextFocusSet(setOf(a), b, true), a, false)
        assertEquals(blurFirst, focusFirst)
    }

    /**
     * El conjunto queda vacío en un paso intermedio cuando el desenfoque llega antes que el foco.
     * No es un fallo: las dos notificaciones caen en la misma vuelta del bucle de eventos y las
     * escrituras se agrupan, así que ninguna recomposición observa ese instante. Lo que no puede
     * pasar es que el estado final dependa del orden, y eso es lo que comprueban los tres casos
     * anteriores.
     */
    @Test
    fun `focus stays on the new cell once both notifications have arrived`() {
        val afterBlur = nextFocusSet(setOf(a), a, false)
        val afterFocus = nextFocusSet(afterBlur, b, true)
        assertTrue(afterFocus.contains(b))
        assertFalse(afterFocus.contains(a))
    }

    // RF-23: sin ninguna celda enfocada no hay conjunto.

    @Test
    fun `blurring the last focused cell empties the set`() {
        assertTrue(nextFocusSet(setOf(a), a, false).isEmpty())
    }

    @Test
    fun `blurring a cell that was never focused leaves the set untouched`() {
        val c = CellKey(row = 2, col = 1)
        assertEquals(setOf(a), nextFocusSet(setOf(a), c, false))
    }

    @Test
    fun `an empty set stays empty when a cell is blurred`() {
        assertTrue(nextFocusSet(emptySet(), a, false).isEmpty())
    }

    // RF-30, RF-31: la tabla está señalada con el foco, con el puntero, o con ambos.

    @Test
    fun `not signaled without focus and without pointer`() {
        assertFalse(isSignaled(SelectionState()))
    }

    @Test
    fun `signaled by focus alone`() {
        assertTrue(isSignaled(SelectionState(focused = a, active = true)))
    }

    @Test
    fun `signaled by pointer alone`() {
        assertTrue(isSignaled(SelectionState(hovered = true)))
    }

    @Test
    fun `signaled by focus and pointer together`() {
        assertTrue(isSignaled(SelectionState(focused = a, hovered = true, active = true)))
    }

    // RF-26, RF-27, RF-32: una sola región, continua, con la celda enfocada por delante.

    /**
     * Columnas de 100, 150 y 80 px. Filas de 44, 60 y 44 px.
     *
     * Bordes de columna: 0, 100, 250 y 330. Bordes de fila: 0, 44, 104 y 148.
     */
    private val metrics = TableMetrics(
        columnWidths = listOf(100f, 150f, 80f),
        rowHeights = listOf(44f, 60f, 44f),
    )

    @Test
    fun `focused cell outlines only itself`() {
        val outline = selectionOutline(metrics, SelectionState(focused = CellKey(1, 1)))
        assertEquals(OutlineRect(100f, 44f, 250f, 104f), outline)
    }

    @Test
    fun `the first row is outlined like any other and starts at the top edge`() {
        val outline = selectionOutline(metrics, SelectionState(focused = a))
        assertEquals(OutlineRect(0f, 0f, 100f, 44f), outline)
    }

    @Test
    fun `every row of the first column has its own outline`() {
        assertEquals(OutlineRect(0f, 0f, 100f, 44f), selectionOutline(metrics, SelectionState(focused = a)))
        assertEquals(
            OutlineRect(0f, 44f, 100f, 104f),
            selectionOutline(metrics, SelectionState(focused = b)),
        )
        assertEquals(
            OutlineRect(0f, 104f, 100f, 148f),
            selectionOutline(metrics, SelectionState(focused = CellKey(2, 0))),
        )
    }

    @Test
    fun `selected row outlines the whole row in one region`() {
        val outline = selectionOutline(metrics, SelectionState(selectedRow = 1))
        assertEquals(OutlineRect(0f, 44f, 330f, 104f), outline)
    }

    @Test
    fun `the first row can be selected like any other`() {
        val outline = selectionOutline(metrics, SelectionState(selectedRow = 0))
        assertEquals(OutlineRect(0f, 0f, 330f, 44f), outline)
    }

    @Test
    fun `selected row spans every column and only its own height`() {
        val outline = selectionOutline(metrics, SelectionState(selectedRow = 2))
        assertEquals(OutlineRect(0f, 104f, 330f, 148f), outline)
        assertEquals(metrics.columnWidths.sum(), outline?.width)
        assertEquals(metrics.rowHeights[2], outline?.height)
    }

    @Test
    fun `focused cell wins over the selected row`() {
        val outline = selectionOutline(
            metrics,
            SelectionState(focused = CellKey(1, 1), selectedRow = 2),
        )
        assertEquals(OutlineRect(100f, 44f, 250f, 104f), outline)
    }

    @Test
    fun `nothing selected gives no outline`() {
        assertNull(selectionOutline(metrics, SelectionState()))
    }

    @Test
    fun `a focused cell in a row that no longer exists gives no outline`() {
        assertNull(selectionOutline(metrics, SelectionState(focused = CellKey(9, 0))))
    }

    @Test
    fun `a focused cell in a column that no longer exists gives no outline`() {
        assertNull(selectionOutline(metrics, SelectionState(focused = CellKey(0, 9))))
    }

    @Test
    fun `a negative row has no outline because there is no row before the first`() {
        // La cabecera es la fila 0, así que un índice negativo no puede señalar ninguna celda.
        assertNull(selectionOutline(metrics, SelectionState(focused = CellKey(-1, 0))))
    }

    @Test
    fun `a stale focused cell does not fall back to the selected row`() {
        assertNull(
            selectionOutline(metrics, SelectionState(focused = CellKey(9, 0), selectedRow = 1))
        )
    }

    @Test
    fun `a selected row that no longer exists gives no outline`() {
        assertNull(selectionOutline(metrics, SelectionState(selectedRow = 9)))
    }

    @Test
    fun `a selected row below the last one gives no outline`() {
        assertNull(selectionOutline(metrics, SelectionState(selectedRow = 3)))
    }

    @Test
    fun `the first column starts at the width of the gutter`() {
        // El carril es un PARAMETRO, no una constante cableada: por eso este test, con su
        // `gutter = 24f` local, sigue verde sin editar cuando producción estrecha el carril
        // (spec 010, RNF-24). Con el carril a 24 la primera columna arranca en 24, no en 48.
        val estrecho = metrics.copy(gutterWidth = 24f)
        assertEquals(24f, selectionOutline(estrecho, SelectionState(focused = a))?.left)
        assertEquals(124f, selectionOutline(estrecho, SelectionState(focused = CellKey(1, 1)))?.left)
    }

    @Test
    fun `a narrower gutter only shifts the outline, it does not resize the row`() {
        val conCarril = metrics.copy(gutterWidth = gutter)
        val estrecho = metrics.copy(gutterWidth = 24f)
        val conPista = selectionOutline(conCarril, SelectionState(selectedRow = 1))!!
        val sinPista = selectionOutline(estrecho, SelectionState(selectedRow = 1))!!
        assertEquals(conPista.width, sinPista.width, 0.01f)
        assertEquals(gutter - 24f, conPista.left - sinPista.left, 0.01f)
    }

    // El carril de los manejadores desplaza las celdas, y el contorno con ellas.

    private val gutter = 24f
    private val withGutter = metrics.copy(gutterWidth = gutter)

    @Test
    fun `the gutter shifts the focused cell outline`() {
        val outline = selectionOutline(withGutter, SelectionState(focused = CellKey(1, 1)))
        assertEquals(OutlineRect(124f, 44f, 274f, 104f), outline)
    }

    @Test
    fun `the gutter shifts the first column outline`() {
        val outline = selectionOutline(withGutter, SelectionState(focused = a))
        assertEquals(OutlineRect(gutter, 0f, gutter + 100f, 44f), outline)
    }

    @Test
    fun `the gutter does not move the rows vertically`() {
        val plain = selectionOutline(metrics, SelectionState(focused = a))!!
        val shifted = selectionOutline(withGutter, SelectionState(focused = a))!!
        assertEquals(plain.top, shifted.top)
    }

    @Test
    fun `the row outline starts at the gutter, not at the table edge`() {
        val outline = selectionOutline(withGutter, SelectionState(selectedRow = 1))
        assertEquals(OutlineRect(gutter, 44f, gutter + 330f, 104f), outline)
    }

    @Test
    fun `the row outline keeps its width with a gutter`() {
        val plain = selectionOutline(metrics, SelectionState(selectedRow = 1))!!
        val shifted = selectionOutline(withGutter, SelectionState(selectedRow = 1))!!
        assertEquals(plain.width, shifted.width)
        assertEquals(gutter, shifted.left - plain.left)
    }

    @Test
    fun `the column handle clears the gutter`() {
        // El manejador de columna se centra en la columna, así que también arranca
        // desplazado por el carril y nunca cae encima de los manejadores de fila.
        assertEquals(gutter + 38f, columnHandleLeft(withGutter, 0, handleWidth))
        assertEquals(gutter + 163f, columnHandleLeft(withGutter, 1, handleWidth))
    }

    // RF-28, RF-29: un manejador por fila, centrado en su fila.

    private val handleWidth = 24f

    @Test
    fun `one handle per row`() {
        assertEquals(listOf(22f, 74f, 126f), rowHandleCenters(metrics))
        assertEquals(metrics.rowHeights.size, rowHandleCenters(metrics).size)
    }

    @Test
    fun `the first handle sits at half the first row with no gap above it`() {
        assertEquals(metrics.rowHeights[0] / 2f, rowHandleCenters(metrics).first())
    }

    @Test
    fun `every handle center falls inside its own row when heights differ`() {
        val centers = rowHandleCenters(metrics)
        centers.forEachIndexed { row, center ->
            val top = center - metrics.rowHeights[row] / 2f
            assertTrue("fila $row: $center", center > top)
            assertTrue("fila $row: $center", center < top + metrics.rowHeights[row])
        }
    }

    /**
     * RF-57: con filas desiguales, el borde superior de cada celda es la suma de los altos REALES de
     * las de arriba. Antes el bloque repartía un único alto a todas las filas, así que en cuanto una
     * crecía el contorno caía sobre otra fila (spec 009).
     */
    @Test
    fun `the top edge of a cell sums the real heights of the rows above it`() {
        val desiguales = metrics.copy(rowHeights = listOf(44f, 160f, 44f))
        // Bordes de fila con esos altos: 0, 44 y 204.
        assertEquals(
            OutlineRect(0f, 44f, 100f, 204f),
            selectionOutline(desiguales, SelectionState(focused = CellKey(1, 0))),
        )
        assertEquals(
            OutlineRect(0f, 204f, 100f, 248f),
            selectionOutline(desiguales, SelectionState(focused = CellKey(2, 0))),
        )
    }

    @Test
    fun `the outline of the taller row is as tall as that row`() {
        val desiguales = metrics.copy(rowHeights = listOf(44f, 160f, 44f))
        val outline = selectionOutline(desiguales, SelectionState(focused = CellKey(1, 1)))!!
        assertEquals(160f, outline.height, 0.01f)
    }

    @Test
    fun `a selected taller row is outlined with its own height`() {
        val desiguales = metrics.copy(rowHeights = listOf(44f, 160f, 44f))
        val outline = selectionOutline(desiguales, SelectionState(selectedRow = 1))!!
        assertEquals(44f, outline.top, 0.01f)
        assertEquals(204f, outline.bottom, 0.01f)
    }

    @Test
    fun `handles land in their own row when one row is much taller`() {
        val desiguales = metrics.copy(rowHeights = listOf(44f, 160f, 44f))
        // Centro de la fila 1: la mitad de su alto, más lo que miden las de arriba.
        assertEquals(124f, rowHandleCenters(desiguales)[1])
        assertEquals(3, rowHandleCenters(desiguales).size)
    }

    @Test
    fun `handles follow rows that are added`() {
        val grown = metrics.copy(rowHeights = listOf(44f, 60f, 44f, 30f))
        val centers = rowHandleCenters(grown)
        assertEquals(grown.rowHeights.size, centers.size)
        // El borde superior de la fila nueva es el borde inferior de la anterior.
        assertEquals(centers[2] + grown.rowHeights[2] / 2f, centers[3] - grown.rowHeights[3] / 2f)
    }

    @Test
    fun `handles follow rows that are removed`() {
        val shrunk = metrics.copy(rowHeights = listOf(60f))
        assertEquals(listOf(30f), rowHandleCenters(shrunk))
    }

    @Test
    fun `the column handle is centered over its column`() {
        assertEquals(38f, columnHandleLeft(metrics, 0, handleWidth))
        assertEquals(163f, columnHandleLeft(metrics, 1, handleWidth))
        assertEquals(278f, columnHandleLeft(metrics, 2, handleWidth))
    }

    @Test
    fun `the column handle midpoint lands on the column center`() {
        val left = columnHandleLeft(metrics, 1, handleWidth)!!
        val columnCenter = 100f + 150f / 2f
        assertEquals(columnCenter, left + handleWidth / 2f)
    }

    @Test
    fun `a column handle for a column that no longer exists gives null`() {
        assertNull(columnHandleLeft(metrics, 9, handleWidth))
    }

    @Test
    fun `a table with a single column centers its handle on that column`() {
        val single = metrics.copy(columnWidths = listOf(200f))
        assertEquals(100f - handleWidth / 2f, columnHandleLeft(single, 0, handleWidth))
    }
}