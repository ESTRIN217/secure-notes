package com.example

import com.example.util.BookmarkMetadataFetcher
import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkMetadataFetcherTest {

    @Test
    fun `parses og title description and icon`() {
        val html = """
            <html><head>
              <meta property="og:title" content="Open Graph Title">
              <meta property="og:description" content="Open Graph Description">
              <link rel="icon" href="/favicon.png">
            </head></html>
        """.trimIndent()

        val meta = BookmarkMetadataFetcher.parseMetadata(html, "https://example.com/page")

        assertEquals("Open Graph Title", meta.title)
        assertEquals("Open Graph Description", meta.description)
        assertEquals("https://example.com/favicon.png", meta.favicon)
    }

    @Test
    fun `falls back to title tag and meta description`() {
        val html = """
            <html><head>
              <title>Fallback Title</title>
              <meta name="description" content="Fallback Description">
            </head></html>
        """.trimIndent()

        val meta = BookmarkMetadataFetcher.parseMetadata(html, "https://example.com/")

        assertEquals("Fallback Title", meta.title)
        assertEquals("Fallback Description", meta.description)
    }

    @Test
    fun `without metadata falls back to host and google favicon`() {
        val meta = BookmarkMetadataFetcher.parseMetadata("<html></html>", "https://www.example.com/x")

        assertEquals("example.com", meta.title)
        assertEquals("", meta.description)
        assertEquals("https://www.google.com/s2/favicons?domain=example.com&sz=64", meta.favicon)
    }

    @Test
    fun `decodes html entities in title`() {
        val html = "<html><head><title>Caf&amp;eacute; &amp; Notes</title></head></html>"

        val meta = BookmarkMetadataFetcher.parseMetadata(html, "https://example.com/")

        assertEquals("Caf&eacute; & Notes", meta.title)
    }

    @Test
    fun `absolutizes protocol relative favicon`() {
        val html = """
            <html><head><link rel="shortcut icon" href="//cdn.example.com/fav.ico"></head></html>
        """.trimIndent()

        val meta = BookmarkMetadataFetcher.parseMetadata(html, "https://example.com/")

        assertEquals("https://cdn.example.com/fav.ico", meta.favicon)
    }

    @Test
    fun `prefers raster favicon over svg like github`() {
        val html = """
            <html><head>
              <title>secure-notes</title>
              <link rel="alternate icon" class="js-site-favicon" type="image/png" href="https://github.githubassets.com/favicons/favicon.png">
              <link rel="icon" class="js-site-favicon" type="image/svg+xml" href="https://github.githubassets.com/favicons/favicon.svg">
            </head></html>
        """.trimIndent()

        val meta = BookmarkMetadataFetcher.parseMetadata(html, "https://github.com/ESTRIN217/secure-notes")

        assertEquals("https://github.githubassets.com/favicons/favicon.png", meta.favicon)
    }

    @Test
    fun `keeps svg favicon when no raster is available`() {
        val html = """
            <html><head>
              <title>SVG Only</title>
              <link rel="icon" type="image/svg+xml" href="/icon.svg">
            </head></html>
        """.trimIndent()

        val meta = BookmarkMetadataFetcher.parseMetadata(html, "https://example.com/page")

        assertEquals("https://example.com/icon.svg", meta.favicon)
    }
}
