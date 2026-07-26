package com.example.util

class HtmlConverter {

    fun convertToHtml(raw: String): String {
        return raw
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "<br>\n")
            .replace(Regex("<b>(.*?)</b>")) { "<strong>${it.groupValues[1]}</strong>" }
            .replace(Regex("<i>(.*?)</i>")) { "<em>${it.groupValues[1]}</em>" }
            .replace(Regex("<u>(.*?)</u>")) { "<u>${it.groupValues[1]}</u>" }
            .replace(Regex("<s>(.*?)</s>")) { "<s>${it.groupValues[1]}</s>" }
            .replace(Regex("<code>(.*?)</code>")) { "<code>${it.groupValues[1]}</code>" }
            .replace(Regex("<color=#?([0-9A-Fa-f]+)>(.*?)</color>")) {
                "<span style=\"color:#${it.groupValues[1]}\">${it.groupValues[2]}</span>"
            }
            .replace(Regex("<cl[^>]*>(.*?)</cl>")) { it.groupValues[2] }
            .replace(Regex("<img src=\"(.*?)\" />")) {
                "<br><img src=\"${it.groupValues[1]}\" style=\"max-width:100%; height:auto;\" /><br>"
            }
            .replace(Regex("<hr\\s*/?>")) {
                "<hr>"
            }
            .replace(Regex("<table>([\\s\\S]*?)</table>")) { tableMatch ->
                val inner = tableMatch.groupValues[1]
                val sb = StringBuilder("<table style=\"border-collapse:collapse; width:100%;\">\n")
                val thMatch = Regex("<th>(.*?)</th>", RegexOption.DOT_MATCHES_ALL).find(inner)
                if (thMatch != null) {
                    sb.append("<thead><tr>")
                    val cells = thMatch.groupValues[1].split("</th><th>").map { it.trim() }
                    for (cell in cells) {
                        sb.append("<th style=\"border:1px solid #ddd; padding:8px; text-align:left; background:#f5f5f5;\">${escapeHtml(cell)}</th>")
                    }
                    sb.append("</tr></thead>\n")
                }
                sb.append("<tbody>\n")
                val trRegex = Regex("<tr>(.*?)</tr>", RegexOption.DOT_MATCHES_ALL)
                for (tr in trRegex.findAll(inner)) {
                    sb.append("<tr>")
                    Regex("<td>(.*?)</td>", RegexOption.DOT_MATCHES_ALL)
                        .findAll(tr.groupValues[1])
                        .forEach { td ->
                            sb.append("<td style=\"border:1px solid #ddd; padding:8px;\">${escapeHtml(td.groupValues[1])}</td>")
                        }
                    sb.append("</tr>\n")
                }
                sb.append("</tbody>\n</table>")
                sb.toString()
            }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    fun convertHtmlToSecureNotes(html: String): String {
        var s = html
        s = s.replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("</?(html|head|body|meta|link|script|style)[^>]*>", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        s = s.replace(Regex("</?(p|div|section|article|nav|header|footer|main|aside)(\\s[^>]*)?>", RegexOption.IGNORE_CASE), "\n")
        s = s.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        s = s.replace(Regex("<h1[^>]*>(.*?)</h1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<h1>$1</h1>")
        s = s.replace(Regex("<h2[^>]*>(.*?)</h2>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<h2>$1</h2>")
        s = s.replace(Regex("<h3[^>]*>(.*?)</h3>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<h3>$1</h3>")
        s = s.replace(Regex("<strong[^>]*>(.*?)</strong>", RegexOption.IGNORE_CASE), "<b>$1</b>")
        s = s.replace(Regex("<b[^>]*>(.*?)</b>", RegexOption.IGNORE_CASE), "<b>$1</b>")
        s = s.replace(Regex("<em[^>]*>(.*?)</em>", RegexOption.IGNORE_CASE), "<i>$1</i>")
        s = s.replace(Regex("<i[^>]*>(.*?)</i>", RegexOption.IGNORE_CASE), "<i>$1</i>")
        s = s.replace(Regex("<ins[^>]*>(.*?)</ins>", RegexOption.IGNORE_CASE), "<u>$1</u>")
        s = s.replace(Regex("<u[^>]*>(.*?)</u>", RegexOption.IGNORE_CASE), "<u>$1</u>")
        s = s.replace(Regex("<del[^>]*>(.*?)</del>", RegexOption.IGNORE_CASE), "<s>$1</s>")
        s = s.replace(Regex("<s[^>]*>(.*?)</s>", RegexOption.IGNORE_CASE), "<s>$1</s>")
        s = s.replace(Regex("<code[^>]*>(.*?)</code>", RegexOption.IGNORE_CASE), "<code>$1</code>")
        s = s.replace(Regex("<pre[^>]*>(.*?)</pre>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<pre>$1</pre>")
        s = s.replace(Regex("<blockquote[^>]*>(.*?)</blockquote>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<quote>$1</quote>")
        s = s.replace(Regex("<a\\s+[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<url=$1>$2</url>")
        s = s.replace(Regex("<a\\s+[^>]*href='([^']+)'[^>]*>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<url=$1>$2</url>")
        s = s.replace(Regex("<img\\s+[^>]*src=\"([^\"]+)\"[^>]*>", RegexOption.IGNORE_CASE), "<img src=\"$1\" />")
        s = s.replace(Regex("<img\\s+[^>]*src='([^']+)'[^>]*>", RegexOption.IGNORE_CASE), "<img src=\"$1\" />")
        s = s.replace(Regex("<span\\s+[^>]*style=\"([^\"]+)\"[^>]*>(.*?)</span>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { match ->
            val style = match.groupValues[1]
            val content = match.groupValues[2]
            val parts = style.split(";").map { it.trim() }
            var result = content
            for (part in parts) {
                when {
                    part.startsWith("color:", true) -> {
                        val c = part.substringAfter(":").trim()
                        result = "<color=$c>$result</color>"
                    }
                    part.startsWith("background", true) -> {
                        val c = part.substringAfter(":").trim()
                        result = "<bg=$c>$result</bg>"
                    }
                    part.startsWith("font-weight:", true) -> {
                        if (part.contains("bold", true)) result = "<b>$result</b>"
                    }
                    part.startsWith("font-style:", true) -> {
                        if (part.contains("italic", true)) result = "<i>$result</i>"
                    }
                    part.startsWith("text-decoration:", true) -> {
                        if (part.contains("underline", true)) result = "<u>$result</u>"
                        if (part.contains("line-through", true)) result = "<s>$result</s>"
                    }
                    part.startsWith("font-family:", true) -> {
                        val fam = part.substringAfter(":").trim().removeSurrounding("\"").removeSurrounding("'")
                        result = "<font=$fam>$result</font>"
                    }
                    part.startsWith("font-size:", true) -> {
                        val size = part.substringAfter(":").trim().replace("px", "").replace("pt", "").trim()
                        result = "<size=$size>$result</size>"
                    }
                }
            }
            result
        }
        s = s.replace(Regex("<ol[^>]*>(.*?)</ol>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { match ->
            val items = match.groupValues[1]
            val lis = items.replace(Regex("<li[^>]*>(.*?)</li>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { liMatch ->
                liMatch.groupValues[1]
            }
            "<ol>\n${lis.lines().joinToString("\n") { "  <li>$it</li>" }}\n</ol>"
        }
        s = s.replace(Regex("<ul[^>]*>(.*?)</ul>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { match ->
            val items = match.groupValues[1]
            val lis = items.replace(Regex("<li[^>]*>(.*?)</li>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { liMatch ->
                liMatch.groupValues[1]
            }
            "<ul>\n${lis.lines().joinToString("\n") { "  <li>$it</li>" }}\n</ul>"
        }
        s = s.replace(Regex("\\n{3,}"), "\n\n")
        return s.trim()
    }

    companion object {
        private val default = HtmlConverter()

        fun convertToHtml(raw: String) = default.convertToHtml(raw)
        fun convertHtmlToSecureNotes(html: String) = default.convertHtmlToSecureNotes(html)
    }
}
