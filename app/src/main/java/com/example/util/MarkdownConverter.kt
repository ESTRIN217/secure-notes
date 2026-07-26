package com.example.util

class MarkdownConverter {

    private data class TagTransform(
        val regex: Regex,
        val replacement: (MatchResult) -> String
    )

    private val markdownTransforms = listOf(
        TagTransform(Regex("<h1>(.*?)</h1>")) { "# ${it.groupValues[1]}" },
        TagTransform(Regex("<h2>(.*?)</h2>")) { "## ${it.groupValues[1]}" },
        TagTransform(Regex("<b>(.*?)</b>")) { "**${it.groupValues[1]}**" },
        TagTransform(Regex("<i>(.*?)</i>")) { "*${it.groupValues[1]}*" },
        TagTransform(Regex("<s>(.*?)</s>")) { "~~${it.groupValues[1]}~~" },
        TagTransform(Regex("<u>(.*?)</u>")) { "<ins>${it.groupValues[1]}</ins>" },
        TagTransform(Regex("<code>(.*?)</code>")) { "`${it.groupValues[1]}`" },
        TagTransform(Regex("<color=#?([0-9A-Fa-f]+)>(.*?)</color>")) { it.groupValues[2] },
        TagTransform(Regex("<cl[^>]*>(.*?)</cl>")) { it.groupValues[1] },
        TagTransform(Regex("<hr\\s*/?>")) { "\n---\n" },
    )

    private fun convertTableToMarkdown(raw: String): String {
        return raw.replace(Regex("<table>([\\s\\S]*?)</table>")) { tableMatch ->
            val inner = tableMatch.groupValues[1]

            val headers = mutableListOf<String>()
            val rows = mutableListOf<List<String>>()

            val thMatch = Regex("<th>(.*?)</th>", RegexOption.DOT_MATCHES_ALL).find(inner)
            if (thMatch != null) {
                headers.addAll(thMatch.groupValues[1].split("</th><th>").map { it.trim() })
            }

            val trRegex = Regex("<tr>(.*?)</tr>", RegexOption.DOT_MATCHES_ALL)
            for (tr in trRegex.findAll(inner)) {
                val cells = Regex("<td>(.*?)</td>", RegexOption.DOT_MATCHES_ALL)
                    .findAll(tr.groupValues[1])
                    .map { it.groupValues[1].trim() }
                    .toList()
                if (cells.isNotEmpty()) rows.add(cells)
            }

            val colCount = maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 1)

            val sb = StringBuilder("\n")
            if (headers.isNotEmpty()) {
                sb.append("| ")
                sb.append(headers.joinToString(" | ") { it })
                sb.append(" |\n")
            }
            sb.append("| ")
            sb.append((1..colCount).joinToString(" | ") { "---" })
            sb.append(" |\n")
            for (row in rows) {
                sb.append("| ")
                sb.append(row.joinToString(" | ") { it })
                sb.append(" |\n")
            }
            sb.toString()
        }
    }

    private fun applyTransforms(input: String, transforms: List<TagTransform>): String {
        var result = input
        for (transform in transforms) {
            result = transform.regex.replace(result) { transform.replacement(it) }
        }
        return result
    }

    fun stripTags(raw: String): String {
        return raw
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
    }

    fun convertToMarkdown(raw: String): String {
        var result = raw
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
        result = convertTableToMarkdown(result)
        result = applyTransforms(result, markdownTransforms)
        return result.trim()
    }

    companion object {
        private val default = MarkdownConverter()

        fun stripTags(raw: String) = default.stripTags(raw)
        fun convertToMarkdown(raw: String) = default.convertToMarkdown(raw)
    }
}
