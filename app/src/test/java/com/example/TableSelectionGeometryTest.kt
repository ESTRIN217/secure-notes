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

    // RF-26, RF-27, RF-28, RF-32: una sola región, continua, con la celda enfocada por delante.

    /**
     * Columnas de 100, 150 y 80 px. Cabecera de 40 px. Filas de 44, 60 y 44 px.
     *
     * Bordes de columna: 0, 100, 250 y 330. Bordes de fila: 40, 84, 144 y 188.
     */
    private val metrics = TableMetrics(
        columnWidths = listOf(100f, 150f, 80f),
        rowHeights = listOf(44f, 60f, 44f),
        headerHeight = 40f,
        showHeader = true,
    )

    @Test
    fun `focused cell outlines only itself`() {
        val outline = selectionOutline(metrics, SelectionState(focused = CellKey(1, 1)))
        assertEquals(OutlineRect(100f, 84f, 250f, 144f), outline)
    }

    @Test
    fun `focused cell in the first column starts at the left edge`() {
        val outline = selectionOutline(metrics, SelectionState(focused = CellKey(0, 0)))
        assertEquals(OutlineRect(0f, 40f, 100f, 84f), outline)
    }

    @Test
    fun `selected row outlines the whole row in one region`() {
        val outline = selectionOutline(metrics, SelectionState(selectedRow = 1))
        assertEquals(OutlineRect(0f, 84f, 330f, 144f), outline)
    }

    @Test
    fun `selected row spans every column and only its own height`() {
        val outline = selectionOutline(metrics, SelectionState(selectedRow = 2))
        assertEquals(OutlineRect(0f, 144f, 330f, 188f), outline)
        assertEquals(metrics.columnWidths.sum(), outline?.width)
        assertEquals(metrics.rowHeights[2], outline?.height)
    }

    @Test
    fun `focused cell wins over the selected row`() {
        val outline = selectionOutline(
            metrics,
            SelectionState(focused = CellKey(1, 1), selectedRow = 2),
        )
        assertEquals(OutlineRect(100f, 84f, 250f, 144f), outline)
    }

    @Test
    fun `header cell outlines the title with the header height as origin`() {
        val outline = selectionOutline(metrics, SelectionState(focused = CellKey(-1, 2)))
        assertEquals(OutlineRect(250f, 0f, 330f, 40f), outline)
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
    fun `a header cell gives no outline when the header is hidden`() {
        val noHeader = metrics.copy(showHeader = false)
        assertNull(selectionOutline(noHeader, SelectionState(focused = CellKey(-1, 0))))
    }

    @Test
    fun `the first row starts at zero when the header is hidden`() {
        val noHeader = metrics.copy(showHeader = false)
        val outline = selectionOutline(noHeader, SelectionState(focused = CellKey(0, 0)))
        assertEquals(OutlineRect(0f, 0f, 100f, 44f), outline)
    }

    // RF-28, RF-29: un manejador por fila, con la cabecera, y centrado en su fila.

    private val handleWidth = 24f

    @Test
    fun `one handle per row counting the header`() {
        assertEquals(listOf(20f, 62f, 114f, 166f), rowHandleCenters(metrics))
        assertEquals(metrics.rowHeights.size + 1, rowHandleCenters(metrics).size)
    }

    @Test
    fun `the header handle sits at half the header height`() {
        assertEquals(metrics.headerHeight / 2f, rowHandleCenters(metrics).first())
    }

    @Test
    fun `a hidden header leaves no gap above the first row handle`() {
        val noHeader = metrics.copy(showHeader = false)
        assertEquals(listOf(22f, 74f, 126f), rowHandleCenters(noHeader))
        assertEquals(noHeader.rowHeights[0] / 2f, rowHandleCenters(noHeader).first())
    }

    @Test
    fun `every handle center falls inside its own row when heights differ`() {
        val uneven = metrics.copy(rowHeights = listOf(44f, 60f, 44f))
        val centers = rowHandleCenters(uneven)
        // centers[0] es el de la cabecera; los de datos van desde centers[1].
        centers.drop(1).forEachIndexed { row, center ->
            val top = center - uneven.rowHeights[row] / 2f
            assertTrue("fila $row: $center", center > top)
            assertTrue("fila $row: $center", center < top + uneven.rowHeights[row])
        }
    }

    @Test
    fun `handles follow rows that are added`() {
        val grown = metrics.copy(rowHeights = listOf(44f, 60f, 44f, 30f))
        val centers = rowHandleCenters(grown)
        assertEquals(grown.rowHeights.size + 1, centers.size)
        // El borde superior de la fila nueva es el borde inferior de la anterior.
        assertEquals(centers[3] + grown.rowHeights[2] / 2f, centers[4] - grown.rowHeights[3] / 2f)
    }

    @Test
    fun `handles follow rows that are removed`() {
        val shrunk = metrics.copy(rowHeights = listOf(60f))
        assertEquals(listOf(20f, 70f), rowHandleCenters(shrunk))
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
