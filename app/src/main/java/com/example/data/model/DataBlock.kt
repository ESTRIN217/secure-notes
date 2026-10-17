package com.example.data.model

import com.example.util.NoteContentBlockConverter
import com.example.util.RichTextConverter
import com.example.util.preprocessMarkdownBlocks
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tabla del bloque de tabla.
 *
 * **La cabecera es la primera fila.** No hay ninguna lista de títulos aparte de las filas: las dos
 * opciones de cabecera, [showHeader] para la fila y [showHeaderColumn] para la columna, pintan una
 * fila o una columna, igual que la otra, y ninguna de las dos guarda datos propios (spec 008).
 */
data class TableData(
    val rows: List<List<String>> = emptyList(),
    val columnWeights: List<Float> = emptyList(),
    val bgColorHex: String? = null,
    val showHeader: Boolean = true,
    val showHeaderColumn: Boolean = false
) {
    fun normalizedWeights(): List<Float> {
        if (columnWeights.size == columnCount && columnWeights.all { it > 0f }) return columnWeights
        return List(columnCount) { 1f }
    }

    val columnCount: Int
        get() = rows.maxOfOrNull { it.size } ?: 0

    fun toJson(): String = JSONObject().apply {
        put("rows", JSONArray(rows.map { JSONArray(it) }))
        if (columnWeights.isNotEmpty()) {
            put("columnWeights", JSONArray(columnWeights))
        }
        if (bgColorHex != null) put("bgColorHex", bgColorHex)
        put("showHeader", showHeader)
        put("showHeaderColumn", showHeaderColumn)
    }.toString()

    fun toHtml(): String {
        val sb = StringBuilder("<table>")
        appendColumnGroup(sb)
        rows.forEachIndexed { index, row ->
            sb.append(rowToHtml(row, if (index == 0 && showHeader) HEADER_TAG else DATA_TAG))
        }
        sb.append("</table>")
        return sb.toString()
    }

    private fun appendColumnGroup(sb: StringBuilder) {
        val weights = normalizedWeights()
        if (weights.size != columnCount) return
        sb.append("<colgroup>")
        weights.forEach { sb.append("<col width=\"$it\"/>") }
        sb.append("</colgroup>")
    }

    /**
     * Una fila entera dentro de su `<tr>`, con la etiqueta que le toca.
     *
     * La cabecera se emitía antes sin la fila que la contiene, de modo que solo un parser HTML5 la
     * salvaba y cualquier otra herramienta la perdía (spec 008, RF-50).
     */
    private fun rowToHtml(cells: List<String>, tag: String): String {
        if (cells.isEmpty()) return "<tr></tr>"
        val body = cells.joinToString("</$tag><$tag>") { escapeCell(it) }
        return "<tr><$tag>$body</$tag></tr>"
    }

    private fun escapeCell(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    companion object {
        private const val HEADER_TAG = "th"
        private const val DATA_TAG = "td"

        fun fromJson(json: String?): TableData? {
            if (json.isNullOrBlank()) return null
            return try {
                val obj = JSONObject(json)
                TableData(
                    rows = readRows(obj),
                    columnWeights = readWeights(obj),
                    bgColorHex = obj.optString("bgColorHex").takeIf { it.isNotBlank() },
                    showHeader = obj.optBoolean("showHeader", true),
                    showHeaderColumn = obj.optBoolean("showHeaderColumn", false)
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Lee las filas de las dos formas en que se ha escrito una tabla.
         *
         * La antigua guardaba los títulos de la cabecera en su propia lista, aparte de las filas, y
         * los emitía fuera de toda fila. La nueva los guarda como la primera fila. Cuando esa lista
         * no está vacía se antepone sin mirar [showHeader]: esos títulos eran texto del usuario y
         * descartarlos los perdería en la siguiente escritura (spec 008, RNF-19). Una lista vacía no
         * antepone nada, así que una tabla que nunca tuvo cabecera no gana una fila fantasma.
         */
        private fun readRows(obj: JSONObject): List<List<String>> {
            val legacyTitles = obj.stringList("headers")
            val stored = obj.stringMatrix("rows")
            return if (legacyTitles.isEmpty()) stored else listOf(legacyTitles) + stored
        }

        private fun readWeights(obj: JSONObject): List<Float> =
            obj.optJSONArray("columnWeights")?.let { arr ->
                (0 until arr.length()).map { arr.getDouble(it).toFloat() }
            } ?: emptyList()

        fun fromLegacyHtml(html: String?): TableData? {
            if (html.isNullOrBlank()) return null
            return try {
                val rows = readLegacyRows(html)
                if (rows.isEmpty()) null else TableData(rows = rows)
            } catch (e: Exception) {
                null
            }
        }

        /** Los `<th>` se buscan en toda la tabla porque en el HTML antiguo iban fuera de su `<tr>`. */
        private fun readLegacyRows(html: String): List<List<String>> {
            val titles = Regex("<th[^>]*>(.*?)</th>", RegexOption.DOT_MATCHES_ALL)
                .findAll(html).map { it.groupValues[1].trim() }.toList()
            val dataRows = Regex("<tr>(.*?)</tr>", RegexOption.DOT_MATCHES_ALL)
                .findAll(html)
                .map { tr ->
                    Regex("<td[^>]*>(.*?)</td>", RegexOption.DOT_MATCHES_ALL)
                        .findAll(tr.groupValues[1]).map { it.groupValues[1].trim() }.toList()
                }
                .filter { it.isNotEmpty() }
                .toList()
            return if (titles.isEmpty()) dataRows else listOf(titles) + dataRows
        }

        private fun JSONObject.stringList(key: String): List<String> =
            optJSONArray(key)?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()

        private fun JSONObject.stringMatrix(key: String): List<List<String>> =
            optJSONArray(key)?.let { arr ->
                (0 until arr.length()).map { index ->
                    val cells = arr.optJSONArray(index)
                    if (cells == null) emptyList()
                    else (0 until cells.length()).map { cells.getString(it) }
                }
            } ?: emptyList()

        fun default3x3(): TableData = TableData(rows = List(4) { List(3) { "" } })
    }
}

enum class BlockType {
    TEXT, HEADING1, HEADING2, HEADING3, HEADING4,
    BULLET_LIST, NUMBERED_LIST, CHECKLIST_ITEM,
    QUOTE, CODE_BLOCK, CALLOUT, PAGE, PAGE_LINK,
    IMAGE, VIDEO, AUDIO, DRAWING, VOICE, FILE, BOOKMARK,
    TABLE, HORIZONTAL_RULE, COLLAPSIBLE
}

data class DataBlock(
    val type: BlockType,
    val content: String = "",
    val meta: Map<String, String> = emptyMap(),
    val richTextJson: String? = null
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("type", type.name)
        obj.put("content", content)
        val metaObj = JSONObject()
        meta.forEach { (k, v) -> metaObj.put(k, v) }
        obj.put("meta", metaObj)
        richTextJson?.let { obj.put("richTextJson", it) }
        return obj
    }

    fun segments(): List<TextSegment>? = TextSegment.deserialize(richTextJson)

    /** Dibujo autocontenido WYSIWYG: `content` = JSON de trazos normalizados. */
    val isWysiwygDrawing: Boolean
        get() = type == BlockType.DRAWING && meta["wysiwyg"] == "true"

    /** Dibujo legado: `content` = ruta a archivo JSON + `meta["previewPath"]` = PNG. */
    val isLegacyDrawing: Boolean
        get() = type == BlockType.DRAWING && !isWysiwygDrawing

    /** Tipos cuyo `content` almacenaba markup legacy y migran a richTextJson. */
    val contentIsText: Boolean get() = type in segmentsBlockTypes

    fun ensureSegments(): List<TextSegment> {
        segments()?.let { return RichTextConverter.canonicalizeEquations(it) }
        return if (RichTextConverter.containsMarkup(content)) {
            RichTextConverter.markupToSegments(content)
        } else {
            listOf(TextSegment(text = content))
        }
    }

    companion object {
        val segmentsBlockTypes = setOf(
            BlockType.TEXT, BlockType.HEADING1, BlockType.HEADING2, BlockType.HEADING3, BlockType.HEADING4,
            BlockType.BULLET_LIST, BlockType.NUMBERED_LIST, BlockType.CHECKLIST_ITEM,
            BlockType.QUOTE, BlockType.CODE_BLOCK, BlockType.CALLOUT, BlockType.COLLAPSIBLE
        )

        fun fromJson(obj: JSONObject): DataBlock {
            val type = try {
                BlockType.valueOf(obj.getString("type"))
            } catch (e: Exception) {
                BlockType.TEXT
            }
            val content = obj.optString("content", "")
            val metaObj = obj.optJSONObject("meta")
            val meta = if (metaObj != null) {
                mutableMapOf<String, String>().apply {
                    metaObj.keys().forEach { k -> put(k, metaObj.optString(k, "")) }
                }
            } else emptyMap()
            val richText = obj.optString("richTextJson").takeIf { it.isNotEmpty() }
            return DataBlock(type = type, content = content, meta = meta, richTextJson = richText)
        }

        fun serialize(blocks: List<DataBlock>): String {
            val arr = JSONArray()
            blocks.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }

        fun deserialize(json: String): List<DataBlock>? {
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
            } catch (e: Exception) {
                null
            }
        }

        /** Migra bloques de texto legacy (markup en `content`, sin richTextJson) a segments.
         *  Devuelve (bloques, huboCambios). `content` se vacía; media/PAGE/TABLE conservan rutas. */
        fun migrateToSegments(blocks: List<DataBlock>): Pair<List<DataBlock>, Boolean> {
            val needsMigration = blocks.any {
                it.contentIsText && it.richTextJson.isNullOrBlank() && it.content.isNotBlank()
            }
            if (!needsMigration) return blocks to false
            val migrated = blocks.map { block ->
                if (block.contentIsText && block.richTextJson.isNullOrBlank() && block.content.isNotBlank()) {
                    block.copy(
                        content = "",
                        richTextJson = TextSegment.serialize(block.ensureSegments())
                    )
                } else {
                    block
                }
            }
            return migrated to true
        }

        fun migrateLegacyContent(rawContent: String): List<DataBlock> {
            val (textPart, attachments) = parseNoteContentAndAttachments(rawContent)
            val processed = preprocessMarkdownBlocks(textPart)

            val blocks = mutableListOf<DataBlock>()
            if (processed.isBlank() && attachments.isEmpty()) return blocks

            val regex = Regex(
                "\\[!\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\)\\]\\(([^\\)]+)\\)" +
                "|!\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\)" +
                "|!video\\s*\\[[^\\]]*\\]\\(([^\\)]+)\\)" +
                "|!audio\\s*\\[[^\\]]*\\]\\(([^\\)]+)\\)" +
                "|<(img|video|audio)\\s+src=\"([^\"]+)\"\\s*/>" +
                "|<(img|video|audio)=([^>]+)>" +
                "|<item\\s+checked=\"(true|false)\">[\\s\\S]*?</item>" +
                "|<table[^>]*>[\\s\\S]*?</table>" +
                "|<callout>[\\s\\S]*?</callout>" +
                "|<ol>[\\s\\S]*?</ol>" +
                "|<ul>[\\s\\S]*?</ul>" +
                "|<hr\\s*/?>" +
                "|<details>[\\s\\S]*?</details>" +
                "|<cl>[\\s\\S]*?</cl>" +
                "|<align\\s*=\\s*\"?(center|left|right|justify)\"?\\s*>|</align>" +
                "|<split\\s*/?>"
            )

            var lastEnd = 0
            val alignStack = mutableListOf<String>()

            for (match in regex.findAll(processed)) {
                if (lastEnd < match.range.first) {
                    val text = processed.substring(lastEnd, match.range.first).trim()
                    if (text.isNotEmpty()) {
                        blocks.addAll(NoteContentBlockConverter.convertTextToBlocks(text))
                    }
                }

                val matchVal = match.value
                when {
                    matchVal == "</align>" -> alignStack.removeLastOrNull()
                    matchVal.startsWith("<align") -> {
                        val a = Regex("""<align\s*=\s*"?(center|left|right|justify)"?\s*>""").find(matchVal)
                        alignStack.add(a?.groupValues?.get(1) ?: "left")
                    }
                    matchVal.startsWith("<ol>") -> {
                        val content = matchVal.substringAfter(">").substringBeforeLast("</ol>")
                        Regex("<li>([\\s\\S]*?)</li>").findAll(content).forEach { li ->
                            val liContent = li.groupValues[1].trim()
                            if (liContent.isNotEmpty()) blocks.add(DataBlock(type = BlockType.NUMBERED_LIST, content = liContent))
                        }
                    }
                    matchVal.startsWith("<ul>") -> {
                        val content = matchVal.substringAfter(">").substringBeforeLast("</ul>")
                        Regex("<li>([\\s\\S]*?)</li>").findAll(content).forEach { li ->
                            val liContent = li.groupValues[1].trim()
                            if (liContent.isNotEmpty()) blocks.add(DataBlock(type = BlockType.BULLET_LIST, content = liContent))
                        }
                    }
                    matchVal.startsWith("<cl>") -> {
                        val content = matchVal.removePrefix("<cl>").removeSuffix("</cl>")
                        Regex("<item\\s+checked=\"(true|false)\">([\\s\\S]*?)</item>").findAll(content).forEach { itemMatch ->
                            val checked = itemMatch.groupValues[1] == "true"
                            val text = itemMatch.groupValues[2].trim()
                            blocks.add(DataBlock(type = BlockType.CHECKLIST_ITEM, content = text, meta = mapOf("checked" to checked.toString())))
                        }
                    }
                    matchVal.startsWith("<item") -> {
                        val checkedMatch = Regex("""<item\s+checked="(true|false)">""").find(matchVal)
                        val checked = checkedMatch?.groupValues?.get(1) == "true"
                        val text = matchVal.substringAfter(">").substringBeforeLast("</item>").trim()
                        if (text.isNotEmpty()) {
                            blocks.add(DataBlock(type = BlockType.CHECKLIST_ITEM, content = text, meta = mapOf("checked" to checked.toString())))
                        }
                    }
                    matchVal.startsWith("<hr") -> blocks.add(DataBlock(type = BlockType.HORIZONTAL_RULE))
                    matchVal.startsWith("<table") -> {
                        val tContent = matchVal.substringAfter(">").substringBeforeLast("</table>")
                        blocks.add(DataBlock(type = BlockType.TABLE, content = tContent))
                    }
                    matchVal.startsWith("<callout") -> {
                        val cContent = matchVal.removePrefix("<callout>").removeSuffix("</callout>")
                        blocks.add(DataBlock(type = BlockType.CALLOUT, content = cContent.trim()))
                    }
                    matchVal.startsWith("<details") -> {
                        val dContent = matchVal.substringAfter(">").substringBeforeLast("</details>")
                        val summaryRegex = Regex("<summary>(.*?)</summary>", RegexOption.DOT_MATCHES_ALL)
                        val summary = summaryRegex.find(dContent)?.groupValues?.get(1)?.trim() ?: ""
                        val body = dContent.replace(summaryRegex, "").trim()
                        blocks.add(DataBlock(type = BlockType.COLLAPSIBLE, content = body, meta = mapOf("summary" to summary)))
                    }
                    matchVal.startsWith("<split") -> { }
                    else -> {
                        val mediaBlock = NoteContentBlockConverter.matchToMediaBlock(match, matchVal)
                        if (mediaBlock != null) blocks.add(mediaBlock)
                    }
                }
                lastEnd = match.range.last + 1
            }

            if (lastEnd < processed.length) {
                val text = processed.substring(lastEnd).trim()
                if (text.isNotEmpty()) {
                    blocks.addAll(NoteContentBlockConverter.convertTextToBlocks(text))
                }
            }

            attachments.forEach { att ->
                when (att.type) {
                    "drawing" -> blocks.add(DataBlock(type = BlockType.DRAWING, content = att.path, meta = mapOf("previewPath" to att.name)))
                    "voice" -> blocks.add(DataBlock(type = BlockType.VOICE, content = att.path))
                    "file" -> blocks.add(DataBlock(type = BlockType.FILE, content = att.path, meta = mapOf("name" to att.name)))
                }
            }

            return blocks.ifEmpty {
                val trimmed = processed.trim()
                if (trimmed.isNotEmpty()) listOf(DataBlock(type = BlockType.TEXT, content = trimmed))
                else emptyList()
            }
        }
    }
}
