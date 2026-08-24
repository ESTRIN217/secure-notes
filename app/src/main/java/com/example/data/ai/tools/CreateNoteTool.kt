package com.example.data.ai.tools

import com.example.data.ai.ToolSpec
import com.example.data.ai.ToolParam

object CreateNoteTool {
    val spec = ToolSpec(
        name = "create_note",
        description = "Create a new note with a title and content. Content must be formatted in Markdown (headings #, bold **, italic *, lists, code fences ```).",
        parameters = listOf(
            ToolParam("title", "string", "Title of the note", true),
            ToolParam("content", "string", "Content of the note in Markdown format", true)
        )
    )
}
