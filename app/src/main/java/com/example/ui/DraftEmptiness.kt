package com.example.ui

import com.example.data.model.Attachment
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.TableData
import com.example.data.model.TextSegment
import org.json.JSONArray

/**
 * «Una nota está vacía» — la regla de la spec 032.
 *
 * Existe porque `DataBlock.serialize(blocks)` produce `"[]"` con la lista vacía, que no es blank, así que
 * el editor consideraba «con contenido» una nota nueva sin nada dentro (RF-210). La decisión es **pura**:
 * título, bloques y adjuntos, sin `android`, sin `Context`, sin Compose; sus pruebas corren sin
 * Robolectric, que no se ejecuta en esta plataforma (RNF-135).
 *
 * Quién la consulta: el editor, a través de `hasContentToSave()`, y nadie más. Los consumidores **no** la
 * reescriben (RF-210).
 *
 * Conservadora por diseño: cuando un dato está roto pero dice algo (un `richTextJson` o una tabla que no
 * se deja parsear), cuenta como contenido. Mejor guardar una nota rara que borrar la de alguien.
 */

/** Vacía = sin título, sin adjuntos y sin bloques con contenido (spec 032). */
fun isNoteEmpty(title: String, blocks: List<DataBlock>, attachments: List<Attachment>): Boolean =
    title.isBlank() && attachments.isEmpty() && blocks.none(::blockHasContent)

/** ¿Este bloque aporta algo que valga la pena persistir? */
fun blockHasContent(block: DataBlock): Boolean = when (block.type) {
    BlockType.HORIZONTAL_RULE -> false
    BlockType.IMAGE, BlockType.VIDEO, BlockType.AUDIO, BlockType.VOICE,
    BlockType.FILE, BlockType.BOOKMARK -> block.content.isNotBlank()
    BlockType.PAGE, BlockType.PAGE_LINK -> block.meta["noteId"]?.isNotBlank() == true
    BlockType.TABLE -> tableHasCell(block.meta["table"])
    BlockType.DRAWING -> drawingHasStrokes(block)
    else -> textBlockHasContent(block)
}

/** Bloques de texto: contenido si algún segmento lleva texto o ecuación (spec 032, «Definiciones»). */
private fun textBlockHasContent(block: DataBlock): Boolean {
    val richText = block.richTextJson ?: return legacyText(block.content).isNotBlank()
    return segmentsHaveText(richText)
}

private fun segmentsHaveText(richTextJson: String): Boolean {
    if (richTextJson.isBlank()) return false
    val segments = TextSegment.deserialize(richTextJson)
    if (segments == null) return true // no parseable, pero no está vacío: conservador
    return segments.any { it.text.isNotBlank() || it.equationLatex != null }
}

/** Markup legacy sin `richTextJson`: se quitan las etiquetas y se mira lo que queda. */
private fun legacyText(content: String): String =
    content.replace(Regex("<[^>]*>"), " ").trim()

/** Una tabla 3×3 vacía serializa `"rows":[["","",""]]` — no vacía por string, vacía por datos. */
private fun tableHasCell(tableJson: String?): Boolean {
    if (tableJson.isNullOrBlank()) return false
    val table = TableData.fromJson(tableJson) ?: return true // rota, pero dice algo: conservador
    return table.rows.any { row -> row.any { it.isNotBlank() } }
}

/** Un dibujo WYSIWYG recién insertado tiene `content = "[]"`: trazos, no texto. */
private fun drawingHasStrokes(block: DataBlock): Boolean =
    if (block.isWysiwygDrawing) {
        runCatching { JSONArray(block.content).length() > 0 }.getOrDefault(block.content.isNotBlank())
    } else {
        block.content.isNotBlank()
    }
