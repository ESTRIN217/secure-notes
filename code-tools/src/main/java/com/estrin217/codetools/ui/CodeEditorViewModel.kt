package com.estrin217.codetools.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.estrin217.codetools.R
import com.estrin217.codetools.data.model.CodeFile
import com.estrin217.codetools.data.repository.CodeFileRepository
import com.estrin217.codetools.data.samples.CodeSamples
import com.estrin217.codetools.formatter.CodeFormattingManager
import com.estrin217.codetools.formatter.FormatOptions
import com.estrin217.codetools.formatter.FormattingResult
import com.estrin217.codetools.syntax.SupportedLanguage
import com.estrin217.codetools.syntax.SyntaxTheme
import com.estrin217.codetools.syntax.SyntaxThemes
import com.estrin217.codetools.utils.FileImportExportHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class CodeEditorViewModel(
    private val repository: CodeFileRepository,
    application: Application
) : AndroidViewModel(application) {

    val files: StateFlow<List<CodeFile>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentFile = MutableStateFlow<CodeFile?>(null)
    val currentFile: StateFlow<CodeFile?> = _currentFile.asStateFlow()

    private val _textFieldValue = MutableStateFlow(TextFieldValue(""))
    val textFieldValue: StateFlow<TextFieldValue> = _textFieldValue.asStateFlow()

    private val _language = MutableStateFlow(SupportedLanguage.BASH)
    val language: StateFlow<SupportedLanguage> = _language.asStateFlow()

    private val _isEditMode = MutableStateFlow(true)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _showLineNumbers = MutableStateFlow(true)
    val showLineNumbers: StateFlow<Boolean> = _showLineNumbers.asStateFlow()

    private val _wordWrap = MutableStateFlow(false)
    val wordWrap: StateFlow<Boolean> = _wordWrap.asStateFlow()

    private val _fontSizeSp = MutableStateFlow(13f)
    val fontSizeSp: StateFlow<Float> = _fontSizeSp.asStateFlow()

    private val _syntaxTheme = MutableStateFlow<SyntaxTheme>(SyntaxThemes.OneDark)
    val syntaxTheme: StateFlow<SyntaxTheme> = _syntaxTheme.asStateFlow()

    // Search and replace state
    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _replaceQuery = MutableStateFlow("")
    val replaceQuery: StateFlow<String> = _replaceQuery.asStateFlow()

    private val _currentMatchIndex = MutableStateFlow(0)
    val currentMatchIndex: StateFlow<Int> = _currentMatchIndex.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _formatError = MutableStateFlow<String?>(null)
    val formatError: StateFlow<String?> = _formatError.asStateFlow()

    private val _formatOptions = MutableStateFlow(FormatOptions())
    val formatOptions: StateFlow<FormatOptions> = _formatOptions.asStateFlow()

    private var defaultInitJob: Job? = null
    @Volatile
    private var hasExplicitFileSelected = false
    @Volatile
    private var isExternalImportInProgress = false
  
    init {
        defaultInitJob = viewModelScope.launch {
            repository.ensureSamplesLoaded()
            // Si ya hay un archivo seleccionado por el usuario o una importación en curso,
            // jamás lo pisamos con plantillas de ejemplo.
            if (_currentFile.value == null && !hasExplicitFileSelected && !isExternalImportInProgress) {
                val latest = repository.getLatestFile()
                if (_currentFile.value == null && !hasExplicitFileSelected && !isExternalImportInProgress && latest != null) {
                    selectFile(latest)
                }
            }
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun clearFormatError() {
        _formatError.value = null
    }

    fun updateTextFieldValue(newValue: TextFieldValue) {
        _textFieldValue.value = newValue
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun setEditMode(isEdit: Boolean) {
        _isEditMode.value = isEdit
    }

    fun toggleLineNumbers() {
        _showLineNumbers.value = !_showLineNumbers.value
    }

    fun toggleWordWrap() {
        _wordWrap.value = !_wordWrap.value
    }

    fun increaseFontSize() {
        if (_fontSizeSp.value < 24f) {
            _fontSizeSp.value += 1f
        }
    }

    fun decreaseFontSize() {
        if (_fontSizeSp.value > 10f) {
            _fontSizeSp.value -= 1f
        }
    }

    fun setSyntaxTheme(theme: SyntaxTheme) {
        _syntaxTheme.value = theme
    }

    fun setLanguage(lang: SupportedLanguage) {
        _language.value = lang
    }

    fun selectFile(file: CodeFile) {
      hasExplicitFileSelected = true
        defaultInitJob?.cancel()
        _currentFile.value = file
        _textFieldValue.value = TextFieldValue(file.content, TextRange(0))
        _language.value = file.language
    }

    fun createNewFile(name: String, language: SupportedLanguage) {
      hasExplicitFileSelected = true
        defaultInitJob?.cancel()
        val initialContent = when (language) {
            SupportedLanguage.KOTLIN -> "fun main() {\n    println(\"Hola Mundo desde Kotlin!\")\n}\n"
            SupportedLanguage.JAVA -> "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hola Mundo\");\n    }\n}\n"
            SupportedLanguage.JAVASCRIPT -> "// Script JavaScript\nconsole.log('Hola Mundo');\n"
            SupportedLanguage.TYPESCRIPT -> "// Script TypeScript\nconst saludo: string = 'Hola Mundo';\nconsole.log(saludo);\n"
            SupportedLanguage.HTML -> "<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n    <meta charset=\"UTF-8\">\n    <title>Título</title>\n</head>\n<body>\n    <h1>Hola Mundo</h1>\n</body>\n</html>\n"
            SupportedLanguage.CSS -> "/* Estilos CSS */\nbody {\n    margin: 0;\n    font-family: sans-serif;\n    background-color: #f5f5f5;\n}\n"
            SupportedLanguage.XML -> "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<config>\n    <param name=\"version\">1.0</param>\n</config>\n"
            SupportedLanguage.SQL -> "-- Consulta SQL\nSELECT * FROM usuarios WHERE activo = 1;\n"
            SupportedLanguage.C -> "#include <stdio.h>\n\nint main() {\n    printf(\"Hola Mundo\\n\");\n    return 0;\n}\n"
            SupportedLanguage.CPP -> "#include <iostream>\n\nint main() {\n    std::cout << \"Hola Mundo\" << std::endl;\n    return 0;\n}\n"
            SupportedLanguage.CSHARP -> "using System;\n\nclass Program {\n    static void Main() {\n        Console.WriteLine(\"Hola Mundo\");\n    }\n}\n"
            SupportedLanguage.GO -> "package main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"Hola Mundo\")\n}\n"
            SupportedLanguage.RUST -> "fn main() {\n    println!(\"Hola Mundo\");\n}\n"
            SupportedLanguage.SWIFT -> "import Foundation\n\nprint(\"Hola Mundo\")\n"
            SupportedLanguage.PHP -> "<?php\necho \"Hola Mundo\";\n"
            SupportedLanguage.RUBY -> "# Script Ruby\nputs \"Hola Mundo\"\n"
            SupportedLanguage.DART -> "void main() {\n    print('Hola Mundo');\n}\n"
            SupportedLanguage.LUA -> "-- Script Lua\nprint(\"Hola Mundo\")\n"
            SupportedLanguage.PYTHON -> "#!/usr/bin/env python3\n\ndef main():\n    print(\"Listo\")\n\nif __name__ == \"__main__\":\n    main()\n"
            SupportedLanguage.BASH -> "#!/usr/bin/env bash\n# Script de inicio\n\necho \"Hola Mundo\"\n"
            SupportedLanguage.JSON -> "{\n  \"name\": \"app\",\n  \"version\": \"1.0.0\"\n}\n"
            SupportedLanguage.YAML -> "# Configuración\nversion: \"1.0\"\nsettings:\n  enabled: true\n"
            SupportedLanguage.ENV -> "APP_ENV=development\nPORT=3000\nDEBUG=true\n"
            SupportedLanguage.INI_TOML -> "[general]\nenabled = true\ntimeout = 30\n"
            SupportedLanguage.MARKDOWN -> "# Documento\n\nDescripción del archivo de configuración o script.\n"
            SupportedLanguage.PLAIN_TEXT -> ""
        }

        viewModelScope.launch {
            val newFile = CodeFile(
                name = name.ifBlank { "script.${language.fileExtension}" },
                content = initialContent,
                language = language,
                updatedAt = System.currentTimeMillis()
            )
            val newId = repository.saveFile(newFile)
            val saved = newFile.copy(id = newId)
            selectFile(saved)
            _toastMessage.value = getApplication<Application>().getString(R.string.toast_file_created, saved.name)
        }
    }
    fun importExternalFile(name: String, content: String, detectedLanguage: SupportedLanguage) {
                hasExplicitFileSelected = true
        isExternalImportInProgress = true
        defaultInitJob?.cancel()

        viewModelScope.launch {
            try {
                val cleanName = name.ifBlank { "archivo_importado.${detectedLanguage.fileExtension}" }
                val existing = repository.getFileByName(cleanName)

                val fileToSave = if (existing != null) {
                    existing.copy(
                        content = content,
                        language = detectedLanguage,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    CodeFile(
                        name = cleanName,
                content = content,
                language = detectedLanguage,
                updatedAt = System.currentTimeMillis()
            )
                }

                val savedId = repository.saveFile(fileToSave)
                val finalFile = fileToSave.copy(id = savedId)

                selectFile(finalFile)
                _toastMessage.value = getApplication<Application>().getString(R.string.toast_file_opened, finalFile.name)
            } finally {
                isExternalImportInProgress = false
            }
        }
    }

    /**
     * Handles host Activity intents (ACTION_VIEW with Uri data, ACTION_SEND
     * with EXTRA_STREAM or EXTRA_TEXT). The host forwards onCreate/onNewIntent
     * here so the library stays Activity-agnostic.
     */
    fun handleIntent(context: Context, intent: Intent?) {
        if (intent == null) return
        val uri: Uri? = intent.data ?: streamExtra(intent)
        if (intent.action == Intent.ACTION_VIEW && uri != null) {
            importUri(context, uri)
        } else if (intent.action == Intent.ACTION_SEND) {
            if (uri != null) {
                importUri(context, uri)
            } else {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                        ?: "texto_compartido.txt"
                    importExternalFile(subject, sharedText, SupportedLanguage.fromFileName(subject))
                }
            }
        }
    }

    private fun importUri(context: Context, uri: Uri) {
        if (FileImportExportHelper.isOversize(context, uri)) {
            _toastMessage.value = getApplication<Application>().getString(R.string.toast_file_too_large)
            return
        }
        val info = FileImportExportHelper.readExternalFile(context, uri)
        if (info == null) {
            _toastMessage.value = getApplication<Application>().getString(R.string.toast_file_read_error)
            return
        }
        importExternalFile(info.name, info.content, info.language)
    }

    private fun streamExtra(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }
    }

    fun saveCurrentFile() {
        val current = _currentFile.value ?: return
        val updated = current.copy(
            content = _textFieldValue.value.text,
            language = _language.value,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveFile(updated)
            _currentFile.value = updated
            _toastMessage.value = getApplication<Application>().getString(R.string.toast_file_saved, updated.name)
        }
    }

    fun deleteFile(file: CodeFile) {
        viewModelScope.launch {
            repository.deleteFile(file)
            if (_currentFile.value?.id == file.id) {
                val remaining = files.value.filter { it.id != file.id }
                if (remaining.isNotEmpty()) {
                    selectFile(remaining.first())
                } else {
                    createNewFile("script.sh", SupportedLanguage.BASH)
                }
            }
            _toastMessage.value = getApplication<Application>().getString(R.string.toast_file_deleted)
        }
    }

    // Quick symbol / snippet insertion into active cursor position
    fun insertTextAtCursor(insert: String) {
        val current = _textFieldValue.value
        val text = current.text
        val selection = current.selection

        val newText = buildString {
            append(text.substring(0, selection.min))
            append(insert)
            append(text.substring(selection.max))
        }

        val newCursor = selection.min + insert.length
        _textFieldValue.value = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    fun insertIndentation(spaces: Int = 2) {
        insertTextAtCursor(" ".repeat(spaces))
    }

    // Line manipulations
    fun duplicateCurrentLine() {
        val current = _textFieldValue.value
        val text = current.text
        val cursor = current.selection.min.coerceIn(0, text.length)

        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let {
            if (it == -1) 0 else it + 1
        }
        val lineEnd = text.indexOf('\n', cursor).let {
            if (it == -1) text.length else it
        }

        val lineText = text.substring(lineStart, lineEnd)
        val newText = buildString {
            append(text.substring(0, lineEnd))
            append("\n")
            append(lineText)
            append(text.substring(lineEnd))
        }

        _textFieldValue.value = TextFieldValue(
            text = newText,
            selection = TextRange(lineEnd + 1 + lineText.length)
        )
        _toastMessage.value = getApplication<Application>().getString(R.string.toast_line_duplicated)
    }

    fun deleteCurrentLine() {
        val current = _textFieldValue.value
        val text = current.text
        if (text.isEmpty()) return

        val cursor = current.selection.min.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let {
            if (it == -1) 0 else it + 1
        }
        val lineEnd = text.indexOf('\n', cursor).let {
            if (it == -1) text.length else it
        }

        val removeEnd = if (lineEnd < text.length && text[lineEnd] == '\n') lineEnd + 1 else lineEnd
        val newText = text.substring(0, lineStart) + text.substring(removeEnd)

        _textFieldValue.value = TextFieldValue(
            text = newText,
            selection = TextRange(lineStart.coerceAtMost(newText.length))
        )
        _toastMessage.value = getApplication<Application>().getString(R.string.toast_line_deleted)
    }

    fun toggleCommentOnCurrentLine() {
        val current = _textFieldValue.value
        val text = current.text
        if (text.isEmpty()) return

        val cursor = current.selection.min.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let {
            if (it == -1) 0 else it + 1
        }
        val lineEnd = text.indexOf('\n', cursor).let {
            if (it == -1) text.length else it
        }

        val lineText = text.substring(lineStart, lineEnd)
        val commentPrefix = _language.value.commentPrefix.trimEnd() + " "
        val commentSymbol = _language.value.commentPrefix.trim()

        val newLineText = if (lineText.trimStart().startsWith(commentSymbol)) {
            // Remove comment
            val commentIndex = lineText.indexOf(commentSymbol)
            val afterComment = lineText.substring(commentIndex + commentSymbol.length)
            val cleanAfter = if (afterComment.startsWith(" ")) afterComment.substring(1) else afterComment
            lineText.substring(0, commentIndex) + cleanAfter
        } else {
            // Add comment
            val leadingWhitespace = lineText.takeWhile { it == ' ' || it == '\t' }
            leadingWhitespace + commentPrefix + lineText.substring(leadingWhitespace.length)
        }

        val newText = text.substring(0, lineStart) + newLineText + text.substring(lineEnd)
        _textFieldValue.value = TextFieldValue(
            text = newText,
            selection = TextRange((cursor + (newLineText.length - lineText.length)).coerceIn(0, newText.length))
        )
    }

    fun jumpToLine(lineNumber: Int) {
        val text = _textFieldValue.value.text
        val lines = text.split("\n")
        val targetLine = (lineNumber - 1).coerceIn(0, (lines.size - 1).coerceAtLeast(0))

        var charOffset = 0
        for (i in 0 until targetLine) {
            charOffset += lines[i].length + 1 // +1 for \n
        }

        _textFieldValue.value = _textFieldValue.value.copy(
            selection = TextRange(charOffset.coerceIn(0, text.length))
        )
        _toastMessage.value = getApplication<Application>().getString(R.string.toast_go_to_line, lineNumber)
    }

    fun updateFormatOptions(options: FormatOptions) {
        _formatOptions.value = options
    }

    /**
     * Formatea el código fuente actual según el lenguaje y las opciones establecidas.
     */
    fun formatCode(customOptions: FormatOptions? = null) {
        val text = _textFieldValue.value.text
        if (text.isBlank()) {
            _toastMessage.value = getApplication<Application>().getString(R.string.toast_nothing_to_format)
            return
        }

        val lang = _language.value
        val options = customOptions ?: run {
            val base = _formatOptions.value
            if (base.isCustom) base else base.copy(indentSize = CodeFormattingManager.defaultIndentForLanguage(lang))
        }

        when (val result = CodeFormattingManager.format(text, lang, options)) {
            is FormattingResult.Success -> {
                val oldCursor = _textFieldValue.value.selection.start
                val formatted = result.formattedCode
                val newCursor = oldCursor.coerceIn(0, formatted.length)
                _textFieldValue.value = TextFieldValue(formatted, TextRange(newCursor))
                _toastMessage.value = getApplication<Application>().getString(R.string.toast_code_formatted, lang.displayName)
                _formatError.value = null
            }
            is FormattingResult.Error -> {
                _formatError.value = result.message
            }
        }
    }

    /**
     * Mantiene retrocompatibilidad con llamadas o pruebas existentes a formatJson.
     */
    fun formatJson() {
        formatCode()
    }

    /**
     * Compacta el código JSON si el archivo actual es de tipo JSON.
     */
    fun minifyJson() {
        val text = _textFieldValue.value.text
        if (text.isBlank()) return

        when (val result = CodeFormattingManager.minify(text, _language.value)) {
            is FormattingResult.Success -> {
                _textFieldValue.value = TextFieldValue(result.formattedCode, TextRange(0))
                _toastMessage.value = getApplication<Application>().getString(R.string.toast_json_minified)
                _formatError.value = null
            }
            is FormattingResult.Error -> {
                _formatError.value = result.message
            }
        }
    }

    // Search and Replace
    fun toggleSearch() {
        _isSearchVisible.value = !_isSearchVisible.value
        if (!_isSearchVisible.value) {
            _searchQuery.value = ""
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _currentMatchIndex.value = 0
        if (query.isNotBlank()) {
            findNextMatch(forward = true)
        }
    }

    fun setReplaceQuery(query: String) {
        _replaceQuery.value = query
    }

    fun getMatchesCount(): Int {
        val query = _searchQuery.value
        val text = _textFieldValue.value.text
        if (query.isBlank() || query.length > text.length) return 0
        var count = 0
        var idx = 0
        val queryLower = query.lowercase()
        val textLower = text.lowercase()
        while (idx < textLower.length) {
            val found = textLower.indexOf(queryLower, idx)
            if (found == -1) break
            count++
            idx = found + query.length.coerceAtLeast(1)
        }
        return count
    }

    fun findNextMatch(forward: Boolean = true) {
        val query = _searchQuery.value
        val text = _textFieldValue.value.text
        if (query.isBlank()) return

        val textLower = text.lowercase()
        val queryLower = query.lowercase()
        val matches = mutableListOf<Int>()
        var idx = 0
        while (idx < textLower.length) {
            val found = textLower.indexOf(queryLower, idx)
            if (found == -1) break
            matches.add(found)
            idx = found + query.length.coerceAtLeast(1)
        }

        if (matches.isEmpty()) return

        val currentIdx = _currentMatchIndex.value
        val nextIdx = if (forward) {
            (currentIdx + 1) % matches.size
        } else {
            if (currentIdx - 1 < 0) matches.size - 1 else currentIdx - 1
        }
        _currentMatchIndex.value = nextIdx
        val matchPos = matches[nextIdx]

        _textFieldValue.value = _textFieldValue.value.copy(
            selection = TextRange(matchPos, matchPos + query.length)
        )
    }

    fun replaceSingle() {
        val query = _searchQuery.value
        val replacement = _replaceQuery.value
        val current = _textFieldValue.value
        val text = current.text

        if (query.isBlank()) return

        val selection = current.selection
        if (selection.length > 0 && text.substring(selection.min, selection.max).equals(query, ignoreCase = true)) {
            val newText = text.substring(0, selection.min) + replacement + text.substring(selection.max)
            _textFieldValue.value = TextFieldValue(
                text = newText,
                selection = TextRange(selection.min + replacement.length)
            )
            findNextMatch(forward = true)
        } else {
            findNextMatch(forward = true)
        }
    }

    fun replaceAll() {
        val query = _searchQuery.value
        val replacement = _replaceQuery.value
        val text = _textFieldValue.value.text
        if (query.isBlank()) return

        val regex = Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
        val newText = regex.replace(text, replacement)
        val count = regex.findAll(text).count()

        _textFieldValue.value = TextFieldValue(newText, TextRange(0))
        _toastMessage.value = getApplication<Application>().getString(R.string.toast_replaced, count)
    }

    // Cursor position info helper
    fun getCursorPositionInfo(): Pair<Int, Int> {
        val text = _textFieldValue.value.text
        val selection = _textFieldValue.value.selection.min.coerceIn(0, text.length)
        val beforeCursor = text.substring(0, selection)
        val line = beforeCursor.count { it == '\n' } + 1
        val lastNewline = beforeCursor.lastIndexOf('\n')
        val col = if (lastNewline == -1) selection + 1 else selection - lastNewline
        return Pair(line, col)
    }
}

class CodeEditorViewModelFactory(
    private val repository: CodeFileRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CodeEditorViewModel::class.java)) {
            return CodeEditorViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
