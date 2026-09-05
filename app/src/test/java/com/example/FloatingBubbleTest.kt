package com.example

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.Lifecycle
import com.example.data.model.DataBlock
import com.example.data.model.Note
import com.example.ui.floating.FloatingLifecycleOwner
import com.example.ui.floating.FloatingTab
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FloatingBubbleTest {

    @Before
    fun setup() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
        })
    }

    @After
    fun tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    fun `floating lifecycle owner manages lifecycle states correctly`() {
        val owner = FloatingLifecycleOwner()
        assertEquals(Lifecycle.State.INITIALIZED, owner.lifecycle.currentState)

        owner.onCreate()
        assertEquals(Lifecycle.State.CREATED, owner.lifecycle.currentState)

        owner.onResume()
        assertEquals(Lifecycle.State.RESUMED, owner.lifecycle.currentState)

        owner.onPause()
        assertEquals(Lifecycle.State.CREATED, owner.lifecycle.currentState)

        owner.onDestroy()
        assertEquals(Lifecycle.State.DESTROYED, owner.lifecycle.currentState)
    }

    @Test
    fun `floating tabs have expected values`() {
        val tabs = FloatingTab.values()
        assertEquals(2, tabs.size)
        assertTrue(tabs.contains(FloatingTab.QUICK_NOTE))
        assertTrue(tabs.contains(FloatingTab.RECENT_NOTES))
    }

    @Test
    fun `quick note migration creates valid data blocks`() {
        val raw = "Meeting summary: discuss v4.0 floating bubble."
        val blocks = DataBlock.migrateLegacyContent(raw)
        assertFalse(blocks.isEmpty())
        val json = DataBlock.serialize(blocks)
        assertTrue(json.contains("discuss v4.0 floating bubble"))
    }

    @Test
    fun `empty quick note produces empty blocks list`() {
        val blocks = DataBlock.migrateLegacyContent("")
        assertTrue(blocks.isEmpty())
    }

    @Test
    fun `note created for floating mode has expected defaults`() {
        val note = Note(
            id = 0,
            title = "Quick Note",
            content = "Hello from floating bubble",
            isEncrypted = false
        )
        assertEquals("Quick Note", note.title)
        assertFalse(note.isEncrypted)
        assertFalse(note.isDeleted)
        assertFalse(note.isArchived)
    }
}
