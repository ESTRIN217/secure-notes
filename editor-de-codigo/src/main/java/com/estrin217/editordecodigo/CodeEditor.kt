package com.estrin217.editordecodigo

import android.app.Application
import android.content.Context
import com.estrin217.editordecodigo.data.db.AppDatabase
import com.estrin217.editordecodigo.data.repository.CodeFileRepository
import com.estrin217.editordecodigo.ui.CodeEditorViewModelFactory

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
}
