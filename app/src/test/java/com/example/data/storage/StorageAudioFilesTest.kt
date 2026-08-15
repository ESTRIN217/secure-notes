package com.example.data.storage

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NoteDatabase
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.Note
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StorageAudioFilesTest {

    private lateinit var context: Context
    private lateinit var database: NoteDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createAudioFile(name: String, content: String = "audio"): File {
        val file = File(context.filesDir, name)
        file.writeText(content)
        return file
    }

    @Test
    fun `audio referenced by block json is not orphan and is attached`() = runBlocking {
        val file = createAudioFile("audio_7_1000.m4a")
        val content = DataBlock.serialize(
            listOf(
                DataBlock(type = BlockType.TEXT, content = "note body"),
                DataBlock(type = BlockType.AUDIO, content = file.absolutePath)
            )
        )
        database.noteDao.insertNote(Note(id = 7, title = "My Note", content = content))

        val (_, items) = StorageAnalyzer.scan(context, database)
        val item = items.firstOrNull { it.path == file.absolutePath }
        assertTrue("audio file should be scanned", item != null)
        assertEquals(StorageCategory.AUDIO, item!!.category)
        assertFalse("referenced audio must not be orphan", item.isOrphan)

        val audioFiles = StorageAnalyzer.findAudioFiles(context, database)
        val info = audioFiles.firstOrNull { it.item.path == file.absolutePath }
        assertTrue(info != null)
        assertTrue(info!!.isAttached)
        assertEquals("My Note", info.noteTitle)
        assertEquals(7, info.noteId)
    }

    @Test
    fun `orphan audio is not attached`() = runBlocking {
        val file = createAudioFile("audio_999_1001.wav")
        database.noteDao.insertNote(Note(id = 1, title = "Unrelated", content = "text"))

        val audioFiles = StorageAnalyzer.findAudioFiles(context, database)
        val info = audioFiles.firstOrNull { it.item.path == file.absolutePath }
        assertTrue(info != null)
        assertFalse(info!!.isAttached)
        assertEquals(null, info.noteId)
    }

    @Test
    fun `audio of encrypted note matched by filename id is protected`() = runBlocking {
        val file = createAudioFile("voice_42_2000.3gp")
        database.noteDao.insertNote(
            Note(id = 42, title = "enc", content = "ciphertext", isEncrypted = true)
        )

        val audioFiles = StorageAnalyzer.findAudioFiles(context, database)
        val info = audioFiles.firstOrNull { it.item.path == file.absolutePath }
        assertTrue(info != null)
        assertTrue(info!!.isAttached)
        assertEquals(42, info.noteId)
        assertEquals(null, info.noteTitle)
        assertEquals(StorageCategory.VOICE, info.item.category)
    }

    @Test
    fun `voice file without matching note stays orphaned`() = runBlocking {
        createAudioFile("voice_2000.3gp")
        database.noteDao.insertNote(Note(id = 1, title = "t", content = "x"))

        val audioFiles = StorageAnalyzer.findAudioFiles(context, database)
        val info = audioFiles.firstOrNull { it.item.name == "voice_2000.3gp" }
        assertTrue(info != null)
        assertFalse(info!!.isAttached)
        assertEquals(StorageCategory.VOICE, info.item.category)
    }

    @Test
    fun `audio referenced by legacy media tag is protected`() = runBlocking {
        val file = createAudioFile("voice_2001.3gp")
        database.noteDao.insertNote(
            Note(id = 1, title = "Legacy", content = "<audio src=\"${file.absolutePath}\" />")
        )

        val audioFiles = StorageAnalyzer.findAudioFiles(context, database)
        val info = audioFiles.firstOrNull { it.item.path == file.absolutePath }
        assertTrue(info != null)
        assertTrue(info!!.isAttached)
        assertEquals("Legacy", info.noteTitle)
    }
}
