package com.estrin217.editordecodigo

import android.app.Application
import android.content.Context
import com.estrin217.editordecodigo.data.db.AppDatabase
import com.estrin217.editordecodigo.data.repository.CodeFileRepository
import com.estrin217.editordecodigo.ui.CodeEditorViewModelFactory
import com.estrin217.editordecodigo.utils.CodeFilePathIndex

/**
 * Public entry point of the editor-de-codigo library. The host app owns the
 * Activity (theme, lock flow, intent-filters and navigation intents);
 * this facade only wires the library's database and ViewModel factory.
 *
 * The repository needs an application [Context] because file content lives on
 * disk (`filesDir/code_files`); Room only keeps the metadata index.
 */
object CodeEditor {

    fun viewModelFactory(context: Context): CodeEditorViewModelFactory {
        val appContext = context.applicationContext
        val database = AppDatabase.getInstance(appContext)
        return CodeEditorViewModelFactory(
            CodeFileRepository(database.codeFileDao(), appContext),
            appContext as Application
        )
    }

    /**
     * Rutas locales que el editor todavía lista en sus recientes (spec 024, RF-152).
     *
     * Es la puerta por la que `:app` lee el índice del editor sin tocar sus internos. Si la base
     * no existe no hay recientes que leer, y **no se crea** solo por preguntar (RNF-94): quien
     * consulta es un escaneo de almacenamiento, no el editor.
     */
    suspend fun recentLocalFilePaths(context: Context): Set<String> {
        val appContext = context.applicationContext
        if (!AppDatabase.databaseExists(appContext)) return emptySet()
        val files = AppDatabase.getInstance(appContext).codeFileDao().getAllFilesOnce()
        return CodeFilePathIndex.localPaths(files)
    }
}
