package com.example.util

class HtmlConverter {

    fun convertToHtml(raw: String): String {
        return raw
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "<br>\n")
            .replace(Regex("<h4>(.*?)</h4>")) { "<h4>${it.groupValues[1]}</h4>" }
            .replace(Regex("<h5>(.*?)</h5>")) { "<h5>${it.groupValues[1]}</h5>" }
            .replace(Regex("<h6>(.*?)</h6>")) { "<h6>${it.groupValues[1]}</h6>" }
            .replace(Regex("<b>(.*?)</b>")) { "<strong>${it.groupValues[1]}</strong>" }
            .replace(Regex("<i>(.*?)</i>")) { "<em>${it.groupValues[1]}</em>" }
            .replace(Regex("<u>(.*?)</u>")) { "<u>${it.groupValues[1]}</u>" }
            .replace(Regex("<s>(.*?)</s>")) { "<s>${it.groupValues[1]}</s>" }
            .replace(Regex("<code>(.*?)</code>")) { "<code>${it.groupValues[1]}</code>" }
            .replace(Regex("<mark>(.*?)</mark>")) { "<mark>${it.groupValues[1]}</mark>" }
            .replace(Regex("<small>(.*?)</small>")) { "<small>${it.groupValues[1]}</small>" }
            .replace(Regex("<kbd>(.*?)</kbd>")) { "<kbd>${it.groupValues[1]}</kbd>" }
            .replace(Regex("<var>(.*?)</var>")) { "<var>${it.groupValues[1]}</var>" }
            .replace(Regex("<samp>(.*?)</samp>")) { "<samp>${it.groupValues[1]}</samp>" }
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
            .replace(Regex("<details>([\\s\\S]*?)</details>")) { detailsMatch ->
                val inner = detailsMatch.groupValues[1]
                val summaryMatch = Regex("<summary>(.*?)</summary>", RegexOption.DOT_MATCHES_ALL).find(inner)
                val summary = summaryMatch?.groupValues?.get(1)?.trim() ?: ""
                val body = inner.replace(Regex("<summary>.*?</summary>", RegexOption.DOT_MATCHES_ALL), "").trim()
                "<details>\n<summary>$summary</summary>\n$body\n</details>"
            }
            .replace(Regex("<align\\s*=\\s*\"?(center|left|right|justify)\"?\\s*>(.*?)</align>", RegexOption.DOT_MATCHES_ALL)) {
                "<div align=\"${it.groupValues[1]}\">${it.groupValues[2]}</div>"
            }
            .replace(Regex("<table([^>]*)>([\\s\\S]*?)</table>")) { tableMatch ->
                val attrs = tableMatch.groupValues[1]
                val inner = tableMatch.groupValues[2]
                val tableAlign = Regex("align=\"([^\"]+)\"").find(attrs)?.groupValues?.get(1)
                val alignStyle = if (tableAlign != null) " text-align:$tableAlign; margin-${if (tableAlign == "center") "left:auto;margin-right:auto" else "$tableAlign:0"}" else ""
                val sb = StringBuilder("<table style=\"border-collapse:collapse; width:100%;$alignStyle\">\n")
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
                    Regex("<td([^>]*)>(.*?)</td>", RegexOption.DOT_MATCHES_ALL)
                        .findAll(tr.groupValues[1])
                        .forEach { td ->
                            val tdAttrs = td.groupValues[1]
                            val tdAlign = Regex("align=\"([^\"]+)\"").find(tdAttrs)?.groupValues?.get(1)
                            val tdStyle = if (tdAlign != null) " text-align:$tdAlign;" else ""
                            sb.append("<td style=\"border:1px solid #ddd; padding:8px;$tdStyle\">${escapeHtml(td.groupValues[2])}</td>")
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
        s = s.replace(Regex("<p\\s+[^>]*align=\"([^\"]+)\"[^>]*>(.*?)</p>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<align=$1>$2</align>")
        s = s.replace(Regex("<p\\s+[^>]*align='([^']+)'[^>]*>(.*?)</p>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<align=$1>$2</align>")
        s = s.replace(Regex("</?(p|div|section|article|nav|header|footer|main|aside)(\\s[^>]*)?>", RegexOption.IGNORE_CASE), "\n")
        s = s.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        s = s.replace(Regex("<h1[^>]*>(.*?)</h1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<h1>$1</h1>")
        s = s.replace(Regex("<h2[^>]*>(.*?)</h2>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<h2>$1</h2>")
        s = s.replace(Regex("<h3[^>]*>(.*?)</h3>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<h3>$1</h3>")
        s = s.replace(Regex("<h4[^>]*>(.*?)</h4>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<h4>$1</h4>")
        s = s.replace(Regex("<h5[^>]*>(.*?)</h5>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<h5>$1</h5>")
        s = s.replace(Regex("<h6[^>]*>(.*?)</h6>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<h6>$1</h6>")
        s = s.replace(Regex("<mark[^>]*>(.*?)</mark>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<mark>$1</mark>")
        s = s.replace(Regex("<small[^>]*>(.*?)</small>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<small>$1</small>")
        s = s.replace(Regex("<kbd[^>]*>(.*?)</kbd>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<kbd>$1</kbd>")
        s = s.replace(Regex("<var[^>]*>(.*?)</var>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<var>$1</var>")
        s = s.replace(Regex("<samp[^>]*>(.*?)</samp>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<samp>$1</samp>")
        s = s.replace(Regex("<details[^>]*>(.*?)</details>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<details>$1</details>")
        s = s.replace(Regex("<summary[^>]*>(.*?)</summary>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<summary>$1</summary>")
        s = s.replace(Regex("<div\\s+[^>]*align=\"([^\"]+)\"[^>]*>(.*?)</div>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<align=$1>$2</align>")
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
