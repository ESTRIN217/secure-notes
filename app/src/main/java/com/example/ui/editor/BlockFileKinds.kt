package com.example.ui.editor

/**
 * Qué archivos se abren en el editor de texto y cuáles se entregan al sistema (spec 014, RF-87).
 *
 * El tipo que declara la plataforma no siempre sirve: hay documentos que llegan como
 * `application/octet-stream` y otros que no traen tipo pero sí extensión. Por eso la respuesta
 * mira **las dos**, y las extensiones son una lista corta y explícita en vez de una regla: lo que
 * no está en la lista no se abre aquí.
 *
 * Función pura: recibe el tipo y la extensión y responde sí o no. No sabe de pantallas ni de
 * intents, y por eso se puede probar sin nada más.
 */
fun isTextLikeFile(mime: String, ext: String): Boolean {
    if (mime.startsWith("text/")) return true
    if (mime == "application/json") return true
    return ext in setOf(
        "md", "markdown", "json", "xml", "yaml", "yml", "csv", "log",
        "kt", "java", "js", "ts", "py", "html", "htm", "css"
    )
}