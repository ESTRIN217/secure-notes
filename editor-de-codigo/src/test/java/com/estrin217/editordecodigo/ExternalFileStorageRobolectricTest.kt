package com.estrin217.editordecodigo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.estrin217.editordecodigo.data.db.AppDatabase
import com.estrin217.editordecodigo.data.model.CodeFile
import com.estrin217.editordecodigo.data.repository.CodeFileRepository
import com.estrin217.editordecodigo.syntax.SupportedLanguage
import com.estrin217.editordecodigo.utils.FileStorageManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExternalFileStorageRobolectricTest {

    @Test
    fun testFileStoredExternallyAndDatabaseKeepsMetadataOnly() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val repo = CodeFileRepository(db.codeFileDao(), context)

        val testName = "test_script.py"
        val testContent = "print('Hola desde almacenamiento externo')"

        val file = CodeFile(
            name = testName,
            language = SupportedLanguage.PYTHON
        )

        // Guardar archivo
        val id = repo.saveFile(file, testContent)
        assertTrue(id > 0)

        // Verificar en disco
        val savedMeta = repo.getFileById(id)
        assertNotNull(savedMeta)
        assertEquals(testName, savedMeta?.name)
        assertNotNull(savedMeta?.filePath)

        val diskFile = File(savedMeta!!.filePath!!)
        assertTrue(diskFile.exists())
        assertEquals(testContent, diskFile.readText())

        // Verificar que en Room la columna content queda vacía y el contenido se lee de disco
        val daoFile = db.codeFileDao().getFileById(id)
        assertNotNull(daoFile)
        assertEquals("", daoFile?.content) // En base de datos vive solo el historial/metadato

        // Verificar que el repositorio lee el contenido real desde el disco
        val fullFile = repo.getFileById(id)
        assertEquals(testContent, fullFile?.content)
    }

    @Test
    fun testFileStorageManagerDirect() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = FileStorageManager.createPhysicalFile(context, "test_file.sh", "echo 'ok'")
        assertTrue(file.exists())
        assertEquals("echo 'ok'", file.readText())
    }
}
