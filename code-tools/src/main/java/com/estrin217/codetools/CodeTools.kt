package com.estrin217.codetools

import android.app.Application
import android.content.Context
import com.estrin217.codetools.data.db.CodeToolsDatabase
import com.estrin217.codetools.data.repository.CodeFileRepository
import com.estrin217.codetools.ui.CodeEditorViewModelFactory

/**
 * Public entry point of the code-tools library. The host app owns the
 * Activity (theme, lock flow, intent-filters and navigation intents);
 * this facade only wires the library's database and ViewModel factory.
 */
object CodeTools {

    fun viewModelFactory(context: Context): CodeEditorViewModelFactory {
        val database = CodeToolsDatabase.getInstance(context.applicationContext)
        return CodeEditorViewModelFactory(
            CodeFileRepository(database.codeFileDao()),
            context.applicationContext as Application
        )
    }
}
