package com.example.data.ai.tools

import com.example.data.ai.ToolSpec
import com.example.data.ai.ToolParam

object GetNoteTool {
    val spec = ToolSpec(
        name = "get_note",
        description = "Get the full content of a note by its ID.",
        parameters = listOf(
            ToolParam("note_id", "integer", "ID of the note to retrieve", true)
        )
    )
}
