package com.example.util

import android.content.Context
import com.example.data.model.DecryptedNote
import com.example.data.model.Note
import com.example.util.export.HtmlExporter
import com.example.util.export.JsonExporter
import com.example.util.export.MarkdownExporter
import com.example.util.export.PdfExporter
import com.example.util.export.TxtExporter

fun exportMultipleToTxt(context: Context, notes: List<DecryptedNote>) =
    TxtExporter().export(context, notes)

fun exportMultipleToMarkdown(context: Context, notes: List<DecryptedNote>) =
    MarkdownExporter().export(context, notes)

fun exportMultipleToPdf(context: Context, notes: List<DecryptedNote>) =
    PdfExporter().export(context, notes)

fun exportMultipleToHtml(context: Context, notes: List<DecryptedNote>) =
    HtmlExporter().export(context, notes)

fun exportMultipleToJson(context: Context, notes: List<DecryptedNote>) =
    JsonExporter().export(context, notes)

fun exportToMarkdown(context: Context, note: Note, decryptedTitle: String, decryptedContent: String) {
    val decryptedNote = DecryptedNote(note, decryptedTitle, decryptedContent, true)
    exportMultipleToMarkdown(context, listOf(decryptedNote))
}

fun exportToPdf(context: Context, note: Note, decryptedTitle: String, decryptedContent: String) {
    val decryptedNote = DecryptedNote(note, decryptedTitle, decryptedContent, true)
    exportMultipleToPdf(context, listOf(decryptedNote))
}

fun exportSingleNoteToJson(context: Context, note: Note, decryptedTitle: String, decryptedContent: String) {
    val decryptedNote = DecryptedNote(note, decryptedTitle, decryptedContent, true)
    exportMultipleToJson(context, listOf(decryptedNote))
}
