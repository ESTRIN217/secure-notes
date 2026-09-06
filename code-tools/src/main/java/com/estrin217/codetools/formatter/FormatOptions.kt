package com.estrin217.codetools.formatter

/**
 * Resultado de una operación de formateo de código.
 * Cumple con SRP y manejo exhaustivo de estados funcionales.
 */
sealed interface FormattingResult {
    data class Success(
        val formattedCode: String,
        val details: String? = null
    ) : FormattingResult

    data class Error(
        val message: String
    ) : FormattingResult
}

/**
 * Opciones de configuración para el formateador de código.
 *
 * @param indentSize Número de espacios por nivel de indentación (típicamente 2 o 4).
 * @param useSpaces Si es true usa espacios; si es false usa tabulaciones.
 * @param trimTrailingWhitespace Elimina espacios sobrantes al final de cada línea.
 * @param insertFinalNewline Asegura que el archivo termine con un salto de línea limpio.
 * @param maxEmptyLines Cantidad máxima de líneas en blanco consecutivas permitidas.
 * @param isCustom Si el usuario configuró explícitamente estas opciones manualmente.
 */
data class FormatOptions(
    val indentSize: Int = 4,
    val useSpaces: Boolean = true,
    val trimTrailingWhitespace: Boolean = true,
    val insertFinalNewline: Boolean = true,
    val maxEmptyLines: Int = 1,
    val isCustom: Boolean = false
) {
    fun getIndentString(level: Int): String {
        if (level <= 0) return ""
        return if (useSpaces) {
            " ".repeat(indentSize * level)
        } else {
            "\t".repeat(level)
        }
    }
}
