package com.estrin217.codetools.formatter

import com.estrin217.codetools.syntax.SupportedLanguage

/**
 * Contrato base para los formateadores específicos de cada lenguaje o familia de lenguajes.
 * Cumple con ISP (Interface Segregation) y OCP (Open/Closed Principle).
 */
interface LanguageFormatter {
    /**
     * Determina si este formateador tiene la capacidad de procesar el lenguaje indicado.
     */
    fun canFormat(language: SupportedLanguage): Boolean

    /**
     * Ejecuta el formateo del código fuente con las opciones provistas.
     */
    fun format(code: String, language: SupportedLanguage, options: FormatOptions): FormattingResult
}
