package com.example

import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.NoteContentBlock
import com.example.data.model.TableData
import com.example.ui.toRenderingBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class TablePreviewDataTest {

    @Test
    fun `preview separates configured header row from data rows`() {
        val block = tableBlock(
            TableData(
                rows = listOf(
                    listOf("Name", "Price"),
                    listOf("Tea", "£2"),
                    listOf("Coffee", "£3")
                )
            )
        )

        val preview = block.toRenderingBlock() as NoteContentBlock.TableBlock

        assertEquals(listOf("Name", "Price"), preview.headers)
        assertEquals(listOf(listOf("Tea", "£2"), listOf("Coffee", "£3")), preview.rows)
    }

    @Test
    fun `preview retains every row when header is disabled`() {
        val rows = listOf(listOf("One", "Two"), listOf("Three"))
        val block = tableBlock(TableData(rows = rows, showHeader = false))

        val preview = block.toRenderingBlock() as NoteContentBlock.TableBlock

        assertEquals(emptyList<String>(), preview.headers)
        assertEquals(rows, preview.rows)
    }

    @Test
    fun `preview keeps an empty table empty`() {
        val block = tableBlock(TableData(rows = emptyList()))

        val preview = block.toRenderingBlock() as NoteContentBlock.TableBlock

        assertEquals(emptyList<String>(), preview.headers)
        assertEquals(emptyList<List<String>>(), preview.rows)
    }

    @Test
    fun `preview parses legacy table markup when metadata is absent`() {
        val block = DataBlock(
            type = BlockType.TABLE,
            content = "<table><th>Name</th><th>Price</th><tr><td>Tea</td><td>£2</td></tr></table>"
        )

        val preview = block.toRenderingBlock() as NoteContentBlock.TableBlock

        assertEquals(listOf("Name", "Price"), preview.headers)
        assertEquals(listOf(listOf("Tea", "£2")), preview.rows)
    }

    @Test
    fun `preview retains fallback for invalid table data`() {
        val block = DataBlock(type = BlockType.TABLE, content = "unparsed table")

        val preview = block.toRenderingBlock() as NoteContentBlock.TableBlock

        assertEquals(emptyList<String>(), preview.headers)
        assertEquals(listOf(listOf("unparsed table")), preview.rows)
    }

    private fun tableBlock(tableData: TableData) = DataBlock(
        type = BlockType.TABLE,
        meta = mapOf("table" to tableData.toJson())
    )
}
