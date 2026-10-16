package com.example.ui

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenNoteTabsTest {

    private lateinit var tabs: OpenNoteTabs

    @Before
    fun setUp() {
        tabs = OpenNoteTabs()
    }

    @Test
    fun `openNote adds tab and selects it`() {
        tabs.openNote(1, now = 1000L)

        assertEquals(listOf(OpenTab.Note(1, 1000L)), tabs.tabs.value)
        assertEquals(0, tabs.selectedIndex.value)
    }

    @Test
    fun `openNote dedupes existing note`() {
        tabs.openNote(1, now = 1000L)
        tabs.openNote(2, now = 2000L)
        tabs.openNote(1, now = 3000L)

        assertEquals(2, tabs.tabs.value.size)
        assertEquals(OpenTab.Note(1, 3000L), tabs.selectedTab)
    }

    @Test
    fun `only one draft tab allowed`() {
        tabs.openNote(0, now = 1000L)
        tabs.openNote(0, now = 2000L)

        assertEquals(1, tabs.tabs.value.size)
        assertEquals("note:0", tabs.selectedTab?.key)
    }

    @Test
    fun `evicts least recently used beyond max`() {
        for (id in 1..OpenNoteTabs.MAX_TABS) {
            tabs.openNote(id, now = id.toLong())
        }
        tabs.openNote(99, now = 9999L)

        assertEquals(OpenNoteTabs.MAX_TABS, tabs.tabs.value.size)
        assertEquals(null, tabs.tabs.value.find { it.key == "note:1" })
        assertEquals("note:99", tabs.selectedTab?.key)
    }

    @Test
    fun `eviction never removes draft`() {
        tabs.openNote(0, now = 1L)
        for (id in 1..OpenNoteTabs.MAX_TABS) {
            tabs.openNote(id, now = 100L + id)
        }

        assertEquals(null, tabs.tabs.value.find { it.key == "note:1" })
        assertEquals("note:0", tabs.tabs.value.find { it.key == "note:0" }?.key)
    }

    @Test
    fun `closeTab selects left neighbor`() {
        tabs.openNote(1, now = 1L)
        tabs.openNote(2, now = 2L)
        tabs.openNote(3, now = 3L)

        val next = tabs.closeTab(2)

        assertEquals(OpenTab.Note(2, 2L), next)
        assertEquals(2, tabs.tabs.value.size)
    }

    @Test
    fun `closeTab last returns null`() {
        tabs.openNote(1, now = 1L)

        val next = tabs.closeTab(0)

        assertNull(next)
        assertEquals(0, tabs.tabs.value.size)
    }

    @Test
    fun `closeTab out of bounds keeps state`() {
        tabs.openNote(1, now = 1L)

        tabs.closeTab(5)

        assertEquals(1, tabs.tabs.value.size)
    }

    @Test
    fun `closeOthers keeps selected`() {
        tabs.openNote(1, now = 1L)
        tabs.openNote(2, now = 2L)
        tabs.openNote(3, now = 3L)

        val next = tabs.closeOthers(1)

        assertEquals(OpenTab.Note(2, 2L), next)
        assertEquals(1, tabs.tabs.value.size)
    }

    @Test
    fun `promoteDraft swaps draft id`() {
        tabs.openNote(0, now = 1L)
        tabs.openNote(5, now = 2L)

        tabs.promoteDraft(7, now = 3L)

        assertEquals(null, tabs.tabs.value.find { it.key == "note:0" })
        assertEquals("note:7", tabs.tabs.value.find { it.key == "note:7" }?.key)
    }

    @Test
    fun `pruneMissing drops deleted notes but keeps draft`() {
        tabs.openNote(0, now = 1L)
        tabs.openNote(1, now = 2L)
        tabs.openNote(2, now = 3L)

        tabs.pruneMissing(setOf(2)) { true }

        assertEquals(listOf("note:0", "note:2"), tabs.tabs.value.map { it.key })
    }

    @Test
    fun `restore filters drafts and caps max`() {
        tabs.restore(
            listOf(OpenTab.Note(0), OpenTab.Note(1), OpenTab.Note(2), OpenTab.Note(3)),
            selected = 2
        )

        assertEquals(listOf("note:1", "note:2", "note:3"), tabs.tabs.value.map { it.key })
        assertEquals(2, tabs.selectedIndex.value)
    }

    @Test
    fun `openMedia dedupes by type and src`() {
        tabs.openMedia("image", "/a/img.png", 1, now = 1L)
        tabs.openMedia("image", "/a/img.png", 1, now = 2L)
        tabs.openMedia("video", "/a/img.png", 1, now = 3L)

        assertEquals(2, tabs.tabs.value.size)
        assertEquals("media:video:/a/img.png", tabs.selectedTab?.key)
    }

    @Test
    fun `openPdf and openText add tabs`() {
        tabs.openNote(1, now = 1L)
        tabs.openPdf("content://pdf/1", "doc.pdf", now = 2L)
        tabs.openText("content://txt/1", "notes.txt", now = 3L)

        assertEquals(
            listOf("note:1", "pdf:content://pdf/1", "text:content://txt/1"),
            tabs.tabs.value.map { it.key }
        )
    }

    @Test
    fun `mixed tabs evict oldest non-draft first`() {
        tabs.openNote(0, now = 1L)
        for (id in 1..OpenNoteTabs.MAX_TABS) {
            tabs.openMedia("image", "/img$id.png", null, now = 100L + id)
        }

        assertEquals(OpenNoteTabs.MAX_TABS, tabs.tabs.value.size)
        assertEquals("note:0", tabs.tabs.value.find { it.key == "note:0" }?.key)
        assertEquals(null, tabs.tabs.value.find { it.key == "media:image:/img1.png" })
    }

    @Test
    fun `pruneMissing drops media with missing file`() {
        tabs.openMedia("image", "/gone.png", null, now = 1L)
        tabs.openMedia("image", "/here.png", null, now = 2L)

        tabs.pruneMissing(emptySet()) { src -> src == "/here.png" }

        assertEquals(listOf("media:image:/here.png"), tabs.tabs.value.map { it.key })
    }

    // ── RF-150, RNF-89: la pestaña de un vídeo web no se cierra sola ─────────────────────────

    @Test
    fun `pruneMissing keeps media whose source is a web video`() {
        tabs.openMedia("video", "https://cdn.ejemplo.com/v.mp4", null, now = 1L)
        tabs.openMedia("video", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", null, now = 2L)
        tabs.openMedia("video", "youtu.be/dQw4w9WgXcQ", null, now = 3L)

        // El lambda dice que **ningún** archivo existe, que es lo que responde de verdad para una URL.
        tabs.pruneMissing(emptySet()) { false }

        assertEquals(
            listOf(
                "media:video:https://cdn.ejemplo.com/v.mp4",
                "media:video:https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "media:video:youtu.be/dQw4w9WgXcQ"
            ),
            tabs.tabs.value.map { it.key }
        )
    }

    @Test
    fun `pruneMissing does not ask the disk about a web source`() {
        tabs.openMedia("video", "https://cdn.ejemplo.com/v.mp4", null, now = 1L)
        tabs.openMedia("image", "/gone.png", null, now = 2L)
        var consultas = 0

        tabs.pruneMissing(emptySet()) { consultas++; false }

        // RNF-89: preguntar por una fuente web sería una petición de red por cada cambio de pestañas.
        assertEquals("solo se debe preguntar por la fuente local", 1, consultas)
    }

    @Test
    fun `pruneMissing still drops a local media file that is gone`() {
        tabs.openMedia("video", "/local/gone.mp4", null, now = 1L)
        tabs.openMedia("video", "/local/here.mp4", null, now = 2L)

        tabs.pruneMissing(emptySet()) { src -> src == "/local/here.mp4" }

        assertEquals(listOf("media:video:/local/here.mp4"), tabs.tabs.value.map { it.key })
    }

    @Test
    fun `a media tab with a web source is not a local source`() {
        // La regla, en corto: lo que no es un fichero local no se comprueba como fichero.
        assertTrue(isRemoteMediaSource("https://cdn.ejemplo.com/v.mp4"))
        assertTrue(isRemoteMediaSource("http://cdn.ejemplo.com/v.mp4"))
        assertTrue(isRemoteMediaSource("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(isRemoteMediaSource("youtu.be/dQw4w9WgXcQ"))
        assertFalse(isRemoteMediaSource("/local/v.mp4"))
        assertFalse(isRemoteMediaSource("content://media/external/video/1"))
        assertFalse(isRemoteMediaSource("file:///storage/emulated/0/v.mp4"))
    }

    @Test
    fun `selectTabByKey selects matching tab`() {
        tabs.openNote(1, now = 1L)
        tabs.openMedia("image", "/a.png", null, now = 2L)

        tabs.selectTabByKey("note:1", now = 3L)

        assertEquals("note:1", tabs.selectedTab?.key)
    }

    @Test
    fun `closeTabByKey removes and returns neighbor`() {
        tabs.openNote(1, now = 1L)
        tabs.openMedia("image", "/a.png", null, now = 2L)

        val next = tabs.closeTabByKey("media:image:/a.png")

        assertEquals(OpenTab.Note(1, 1L), next)
        assertEquals(1, tabs.tabs.value.size)
    }

    @Test
    fun `restore keeps media pdf text tabs`() {
        tabs.restore(
            listOf(
                OpenTab.Note(1),
                OpenTab.Media("image", "/a.png", 1),
                OpenTab.Pdf("content://p", "d.pdf"),
                OpenTab.TextFile("content://t", "n.txt")
            ),
            selected = 3
        )

        assertEquals(4, tabs.tabs.value.size)
        assertEquals(3, tabs.selectedIndex.value)
    }

    @Test
    fun `saver roundtrips heterogeneous tabs`() {
        tabs.openNote(4, now = 1L)
        tabs.openMedia("video", "/v.mp4", 4, now = 2L)
        tabs.openPdf("content://p", "d.pdf", now = 3L)

        val scope = object : SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }
        val saved = with(TabsSaver) { scope.save(tabs) }!!
        val restored = TabsSaver.restore(saved)!!

        assertTrue(restored.tabs.value.map { it.key }.containsAll(
            listOf("note:4", "media:video:/v.mp4", "pdf:content://p")
        ))
        assertEquals(tabs.selectedIndex.value, restored.selectedIndex.value)
    }
}
