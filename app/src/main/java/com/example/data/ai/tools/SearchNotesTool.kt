package com.example.data.ai.tools

import com.example.data.ai.ToolSpec
import com.example.data.ai.ToolParam

object SearchNotesTool {
    val spec = ToolSpec(
        name = "search_notes",
        description = "Search user's notes by query. Returns matching note titles and excerpts.",
        parameters = listOf(
            ToolParam("query", "string", "Search query to find notes", true),
            ToolParam("max_results", "integer", "Maximum number of results (default 5)", false)
        )
    )
}
