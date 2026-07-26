package com.example

import com.example.util.RichTextParser
import com.example.util.MediaBlock
import com.example.data.model.NoteContentBlock
import com.example.util.parseToContentBlocks
import com.example.util.toggleNthChecklistItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextParserTest {

    @Test
    fun testBasicStylesHideTags() {
        val raw = "This is <b>bold</b> and <i>italic</i>."
        val parsed = RichTextParser.parse(raw, hideTags = true)
        assertEquals("This is bold and italic.", parsed.text)
    }

    @Test
    fun testHeadingStylesHideTags() {
        val raw = "<h1>Main Heading</h1> and <h2>Sub Heading</h2>"
        val parsed = RichTextParser.parse(raw, hideTags = true)
        assertEquals("Main Heading and Sub Heading", parsed.text)
    }

    @Test
    fun testInlineCodeHideTags() {
        val raw = "Please run <code>npm run test</code> in terminal."
        val parsed = RichTextParser.parse(raw, hideTags = true)
        assertEquals("Please run npm run test in terminal.", parsed.text)
    }

    @Test
    fun testSubscriptAndSuperscriptHideTags() {
        val raw = "H<sub>2</sub>O and E=mc<sup>2</sup>"
        val parsed = RichTextParser.parse(raw, hideTags = true)
        assertEquals("H2O and E=mc2", parsed.text)
    }

    @Test
    fun testFontColorAndBackgroundTags() {
        val raw = "<color=red>Red Text</color> and <bg=blue>Blue Background</bg>"
        val parsed = RichTextParser.parse(raw, hideTags = true)
        assertEquals("Red Text and Blue Background", parsed.text)
    }

    @Test
    fun testNumberedAndBulletedLists() {
        val rawOl = "<ol><li>First</li><li>Second</li></ol>"
        val parsedOl = RichTextParser.parse(rawOl, hideTags = true)
        assertTrue(parsedOl.text.contains("1. First"))
        assertTrue(parsedOl.text.contains("2. Second"))

        val rawUl = "<ul><li>Apple</li><li>Banana</li></ul>"
        val parsedUl = RichTextParser.parse(rawUl, hideTags = true)
        assertTrue(parsedUl.text.contains("• Apple"))
        assertTrue(parsedUl.text.contains("• Banana"))
    }

    @Test
    fun testNestedAndSequentialLists() {
        val rawNested = "<ol><li>Outer One<ul><li>Inner Bullet</li></ul></li></ol>"
        val parsedNested = RichTextParser.parse(rawNested, hideTags = true)
        assertTrue(parsedNested.text.contains("1. Outer One"))
        assertTrue(parsedNested.text.contains("• Inner Bullet"))

        val rawSequential = "<ol><li>Number</li></ol><ul><li>Bullet</li></ul>"
        val parsedSequential = RichTextParser.parse(rawSequential, hideTags = true)
        assertTrue(parsedSequential.text.contains("1. Number"))
        assertTrue(parsedSequential.text.contains("• Bullet"))
    }

    @Test
    fun testChecklistParsing() {
        val rawCl = "<cl><item checked=\"true\">Task A</item><item checked=\"false\">Task B</item></cl>"
        val parsedCl = RichTextParser.parse(rawCl, hideTags = true)
        assertTrue(parsedCl.text.contains("☑ Task A"))
        assertTrue(parsedCl.text.contains("☐ Task B"))
    }

    @Test
    fun testQuotesAndIndentation() {
        val rawQuote = "<quote>To be or not to be</quote>"
        val parsedQuote = RichTextParser.parse(rawQuote, hideTags = true)
        assertEquals("To be or not to be", parsedQuote.text)

        val rawIndent = "<indent>Hello\nWorld</indent>"
        val parsedIndent = RichTextParser.parse(rawIndent, hideTags = true)
        assertTrue(parsedIndent.text.contains("    ")) // Indented line break
    }

    @Test
    fun testUrlLinkParsing() {
        val rawUrl = "Go to <url=https://google.com>Google</url> search."
        val parsedUrl = RichTextParser.parse(rawUrl, hideTags = true)
        assertEquals("Go to Google search.", parsedUrl.text)
    }

    @Test
    fun testParseMediaBlocks() {
        val raw = "Check this out:\n<img src=\"https://example.com/pic.png\" />\nAnd the video:\n<video src=\"https://example.com/clip.mp4\" />"
        val blocks = RichTextParser.parseMediaBlocks(raw)
        
        // Should parse into 4 blocks: Text, Image, Text, Video
        assertTrue(blocks.size >= 3)
        
        val firstBlock = blocks[0] as MediaBlock.TextBlock
        assertTrue(firstBlock.text.contains("Check this out:"))
        
        val secondBlock = blocks[1] as MediaBlock.ImageBlock
        assertEquals("https://example.com/pic.png", secondBlock.src)
        
        val thirdBlock = blocks[2] as MediaBlock.TextBlock
        assertTrue(thirdBlock.text.contains("And the video:"))
        
        val fourthBlock = blocks[3] as MediaBlock.VideoBlock
        assertEquals("https://example.com/clip.mp4", fourthBlock.src)
    }

    @Test
    fun testLiveChecklistTogglingAndParsing() {
        val raw = "My checklist:\n<cl>\n  <item checked=\"false\">Task One</item>\n  <item checked=\"true\">Task Two</item>\n</cl>"
        
        // Test parsing to NoteContentBlocks
        val blocks = parseToContentBlocks(raw)
        assertEquals(3, blocks.size) // TextBlock (My checklist:\n), 2 ChecklistItemBlocks
        
        val firstItem = blocks[1] as NoteContentBlock.ChecklistItemBlock
        assertEquals(false, firstItem.isChecked)
        assertEquals(0, firstItem.globalIndex)
        
        val secondItem = blocks[2] as NoteContentBlock.ChecklistItemBlock
        assertEquals(true, secondItem.isChecked)
        assertEquals(1, secondItem.globalIndex)
        
        // Test toggling the first item (global index 0)
        val toggledRaw = toggleNthChecklistItem(raw, 0)
        assertTrue(toggledRaw.contains("<item checked=\"true\">Task One</item>"))
        assertTrue(toggledRaw.contains("<item checked=\"true\">Task Two</item>"))
        
        // Test toggling the second item (global index 1)
        val toggledRaw2 = toggleNthChecklistItem(raw, 1)
        assertTrue(toggledRaw2.contains("<item checked=\"false\">Task One</item>"))
        assertTrue(toggledRaw2.contains("<item checked=\"false\">Task Two</item>"))
    }

    @Test
    fun testHorizontalRuleMarkdown() {
        val raw = "Text above\n---\nText below"
        val parsedHide = RichTextParser.parse(raw, hideTags = true)
        assertTrue(parsedHide.text.contains("Text above"))
        assertTrue(parsedHide.text.contains("Text below"))

        val raw2 = "Text above\n***\nText below"
        val parsedHide2 = RichTextParser.parse(raw2, hideTags = true)
        assertTrue(parsedHide2.text.contains("Text above"))
        assertTrue(parsedHide2.text.contains("Text below"))
    }

    @Test
    fun testAutoLinkParsing() {
        val raw = "Visit https://example.com/path for info."
        val parsed = RichTextParser.parse(raw, hideTags = true)
        assertEquals("Visit https://example.com/path for info.", parsed.text)
        assertTrue(parsed.text.contains("https://example.com/path"))
    }

    @Test
    fun testBackslashEscaping() {
        val raw = "This is \\*not\\* italic and \\_not\\_ emphasized."
        val parsedHide = RichTextParser.parse(raw, hideTags = true)
        assertEquals("This is *not* italic and _not_ emphasized.", parsedHide.text)

        val raw2 = "Escaped \\# heading"
        val parsedHide2 = RichTextParser.parse(raw2, hideTags = true)
        assertEquals("Escaped # heading", parsedHide2.text)
    }

    @Test
    fun testNestedBlockquote() {
        val raw = "> Single\n> > Double\n> > > Triple"
        val parsedHide = RichTextParser.parse(raw, hideTags = true)
        assertEquals("SingleDoubleTriple", parsedHide.text.trim().replace("\n", ""))

        val parsedGray = RichTextParser.parse(raw, hideTags = false, showTagsGray = true)
        assertTrue(parsedGray.text.contains(">"))
    }

    @Test
    fun testNestedLists() {
        val raw = "- Item 1\n  - Sub item\n- Item 2"
        val parsedHide = RichTextParser.parse(raw, hideTags = true)
        assertTrue(parsedHide.text.contains("•"))
        assertTrue(parsedHide.text.contains("Item 1"))
        assertTrue(parsedHide.text.contains("Sub item"))
    }

    @Test
    fun testPipeTablePreprocessing() {
        val raw = "Before\n| Header 1 | Header 2 |\n| --- | --- |\n| Cell 1 | Cell 2 |\nAfter"
        val blocks = parseToContentBlocks(raw)
        assertTrue(blocks.any { it is NoteContentBlock.TableBlock })
        val tableBlock = blocks.find { it is NoteContentBlock.TableBlock } as NoteContentBlock.TableBlock
        assertEquals(listOf("Header 1", "Header 2"), tableBlock.headers)
        assertEquals(1, tableBlock.rows.size)
        assertEquals(listOf("Cell 1", "Cell 2"), tableBlock.rows[0])
    }

    @Test
    fun testTableTagParsing() {
        val raw = "Before\n<table><th>H1</th><th>H2</th><tr><td>A</td><td>B</td></tr></table>\nAfter"
        val blocks = parseToContentBlocks(raw)
        assertTrue(blocks.any { it is NoteContentBlock.TableBlock })
        val tableBlock = blocks.find { it is NoteContentBlock.TableBlock } as NoteContentBlock.TableBlock
        assertEquals(listOf("H1", "H2"), tableBlock.headers)
        assertEquals(1, tableBlock.rows.size)
        assertEquals(listOf("A", "B"), tableBlock.rows[0])
    }

    @Test
    fun testHorizontalRuleInParseToContentBlocks() {
        val raw = "Text\n<hr/>\nMore text"
        val blocks = parseToContentBlocks(raw)
        assertTrue(blocks.any { it is NoteContentBlock.HorizontalRuleBlock })
    }

    @Test
    fun testCombinedMarkdownFeatures() {
        val raw = "# Heading\n> Quote with **bold**\n\n| Col A | Col B |\n| --- | --- |\n| 1 | 2 |\n\n---\n\nFinal text with auto-link https://example.com"
        val parsed = RichTextParser.parse(raw, hideTags = true)
        assertTrue(parsed.text.contains("Heading"))
        assertTrue(parsed.text.contains("Quote with bold"))
        assertTrue(parsed.text.contains("Final text with auto-link"))
        assertTrue(parsed.text.contains("https://example.com"))

        val blocks = parseToContentBlocks(raw)
        assertTrue(blocks.any { it is NoteContentBlock.TableBlock })
        assertTrue(blocks.any { it is NoteContentBlock.HorizontalRuleBlock })
    }
}
