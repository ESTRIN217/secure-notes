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
    )

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
        result = applyTransforms(result, markdownTransforms)
        return result
    }

    companion object {
        private val default = MarkdownConverter()

        fun stripTags(raw: String) = default.stripTags(raw)
        fun convertToMarkdown(raw: String) = default.convertToMarkdown(raw)
    }
}
