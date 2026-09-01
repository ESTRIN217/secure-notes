package com.example

import com.example.util.FileImporter
import com.example.util.ImportFileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileImporterTest {

    @Test
    fun `detects type by extension`() {
        assertEquals(ImportFileType.TXT, FileImporter.detectFileType("notes.txt", null))
        assertEquals(ImportFileType.MARKDOWN, FileImporter.detectFileType("readme.md", null))
        assertEquals(ImportFileType.MARKDOWN, FileImporter.detectFileType("doc.markdown", null))
        assertEquals(ImportFileType.HTML, FileImporter.detectFileType("index.html", null))
        assertEquals(ImportFileType.HTML, FileImporter.detectFileType("page.htm", null))
        assertEquals(ImportFileType.JSON, FileImporter.detectFileType("config.json", null))
        assertEquals(ImportFileType.PDF, FileImporter.detectFileType("manual.pdf", null))
    }

    @Test
    fun `extension matching is case insensitive`() {
        assertEquals(ImportFileType.MARKDOWN, FileImporter.detectFileType("README.MD", null))
        assertEquals(ImportFileType.JSON, FileImporter.detectFileType("CONFIG.JSON", null))
        assertEquals(ImportFileType.PDF, FileImporter.detectFileType("MANUAL.PDF", null))
    }

    @Test
    fun `falls back to mime when extension unknown`() {
        assertEquals(ImportFileType.TXT, FileImporter.detectFileType("readme", "text/plain"))
        assertEquals(ImportFileType.MARKDOWN, FileImporter.detectFileType("doc", "text/markdown"))
        assertEquals(ImportFileType.MARKDOWN, FileImporter.detectFileType("doc", "text/x-markdown"))
        assertEquals(ImportFileType.HTML, FileImporter.detectFileType("page", "text/html"))
        assertEquals(ImportFileType.HTML, FileImporter.detectFileType("page", "application/xhtml+xml"))
        assertEquals(ImportFileType.JSON, FileImporter.detectFileType("data", "application/json"))
        assertEquals(ImportFileType.PDF, FileImporter.detectFileType("doc", "application/pdf"))
    }

    @Test
    fun `unknown extension and mime map to other`() {
        assertEquals(ImportFileType.OTHER, FileImporter.detectFileType("archive.zip", "application/zip"))
        assertEquals(ImportFileType.OTHER, FileImporter.detectFileType(null, null))
        assertEquals(ImportFileType.OTHER, FileImporter.detectFileType("", "image/png"))
    }

    @Test
    fun `extension takes precedence over mime`() {
        assertEquals(ImportFileType.JSON, FileImporter.detectFileType("data.json", "text/plain"))
        assertEquals(ImportFileType.PDF, FileImporter.detectFileType("file.pdf", "application/octet-stream"))
    }

    @Test
    fun `text-like excludes pdf and other`() {
        assertTrue(FileImporter.isTextLike(ImportFileType.TXT))
        assertTrue(FileImporter.isTextLike(ImportFileType.MARKDOWN))
        assertTrue(FileImporter.isTextLike(ImportFileType.HTML))
        assertTrue(FileImporter.isTextLike(ImportFileType.JSON))
        assertFalse(FileImporter.isTextLike(ImportFileType.PDF))
        assertFalse(FileImporter.isTextLike(ImportFileType.OTHER))
    }

    @Test
    fun `importable excludes other only`() {
        assertTrue(FileImporter.isImportable(ImportFileType.TXT))
        assertTrue(FileImporter.isImportable(ImportFileType.MARKDOWN))
        assertTrue(FileImporter.isImportable(ImportFileType.HTML))
        assertTrue(FileImporter.isImportable(ImportFileType.JSON))
        assertTrue(FileImporter.isImportable(ImportFileType.PDF))
        assertFalse(FileImporter.isImportable(ImportFileType.OTHER))
    }
}