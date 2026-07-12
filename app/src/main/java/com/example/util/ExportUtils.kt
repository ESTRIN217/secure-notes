package com.example.util

import android.content.Context
import com.example.data.model.DecryptedNote
import com.example.data.model.Note
import com.example.util.export.HtmlExporter
import com.example.util.export.JsonExporter
import com.example.util.export.MarkdownExporter
import com.example.util.export.PdfExporter
import com.example.util.export.TxtExporter

class ExportUtils {
    private val exporters = mutableMapOf<String, Exporter>()

    init {
        register(TxtExporter.instance)
        register(MarkdownExporter.instance)
        register(PdfExporter.instance)
        register(HtmlExporter.instance)
        register(JsonExporter.instance)
    }

    fun register(exporter: Exporter) {
        exporters[exporter.formatKey] = exporter
    }

    fun export(formatKey: String, context: Context, notes: List<DecryptedNote>) {
        exporters[formatKey]?.export(context, notes)
    }

    fun exportToMarkdown(context: Context, note: Note, decryptedTitle: String, decryptedContent: String) {
        val decryptedNote = DecryptedNote(note, decryptedTitle, decryptedContent, true)
        export("MD", context, listOf(decryptedNote))
    }

    fun exportToPdf(context: Context, note: Note, decryptedTitle: String, decryptedContent: String) {
        val decryptedNote = DecryptedNote(note, decryptedTitle, decryptedContent, true)
        export("PDF", context, listOf(decryptedNote))
    }

    fun exportSingleNoteToJson(context: Context, note: Note, decryptedTitle: String, decryptedContent: String) {
        val decryptedNote = DecryptedNote(note, decryptedTitle, decryptedContent, true)
        export("JSON", context, listOf(decryptedNote))
    }

    fun exportMultipleToTxt(context: Context, notes: List<DecryptedNote>) = export("TXT", context, notes)
    fun exportMultipleToMarkdown(context: Context, notes: List<DecryptedNote>) = export("MD", context, notes)
    fun exportMultipleToPdf(context: Context, notes: List<DecryptedNote>) = export("PDF", context, notes)
    fun exportMultipleToHtml(context: Context, notes: List<DecryptedNote>) = export("HTML", context, notes)
    fun exportMultipleToJson(context: Context, notes: List<DecryptedNote>) = export("JSON", context, notes)

    companion object {
        private val default = ExportUtils()

        fun registerExporter(exporter: Exporter) = default.register(exporter)
        fun exportByKey(formatKey: String, context: Context, notes: List<DecryptedNote>) = default.export(formatKey, context, notes)

        fun exportToMarkdown(context: Context, note: Note, decryptedTitle: String, decryptedContent: String) = default.exportToMarkdown(context, note, decryptedTitle, decryptedContent)
        fun exportToPdf(context: Context, note: Note, decryptedTitle: String, decryptedContent: String) = default.exportToPdf(context, note, decryptedTitle, decryptedContent)
        fun exportMultipleToTxt(context: Context, notes: List<DecryptedNote>) = default.exportMultipleToTxt(context, notes)
        fun exportMultipleToMarkdown(context: Context, notes: List<DecryptedNote>) = default.exportMultipleToMarkdown(context, notes)
        fun exportMultipleToHtml(context: Context, notes: List<DecryptedNote>) = default.exportMultipleToHtml(context, notes)
        fun exportMultipleToJson(context: Context, notes: List<DecryptedNote>) = default.exportMultipleToJson(context, notes)
        fun exportSingleNoteToJson(context: Context, note: Note, decryptedTitle: String, decryptedContent: String) = default.exportSingleNoteToJson(context, note, decryptedTitle, decryptedContent)
        fun exportMultipleToPdf(context: Context, notes: List<DecryptedNote>) = default.exportMultipleToPdf(context, notes)
    }
}
