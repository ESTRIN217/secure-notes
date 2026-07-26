package com.example.data.ai

class ToolRegistry(
    private val tools: MutableList<ToolWithHandler> = mutableListOf()
) {
    data class ToolWithHandler(
        val spec: ToolSpec,
        val handler: suspend (Map<String, Any>) -> String
    )

    fun register(spec: ToolSpec, handler: suspend (Map<String, Any>) -> String) {
        tools.removeAll { it.spec.name == spec.name }
        tools.add(ToolWithHandler(spec, handler))
    }

    fun getSpecs(): List<ToolSpec> = tools.map { it.spec }

    fun getSpecsForApi(): List<Map<String, Any>> = tools.map { tool ->
        mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to tool.spec.name,
                "description" to tool.spec.description,
                "parameters" to mapOf(
                    "type" to "object",
                    "properties" to tool.spec.parameters.associate { param ->
                        param.name to mapOf(
                            "type" to param.type,
                            "description" to param.description
                        )
                    },
                    "required" to tool.spec.parameters.filter { it.required }.map { it.name }
                )
            )
        )
    }

    suspend fun execute(name: String, args: Map<String, Any>): String {
        val tool = tools.find { it.spec.name == name }
            ?: return "Error: Tool '$name' not found"
        return try {
            tool.handler(args)
        } catch (e: Exception) {
            "Error executing '$name': ${e.message}"
        }
    }

    fun isNotEmpty(): Boolean = tools.isNotEmpty()
}
