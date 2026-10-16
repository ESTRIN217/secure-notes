package com.example.crash

import java.net.URLEncoder

/**
 * La URL de la incidencia de GitHub que abre el botón «Enviar» (spec 020, RF-121).
 *
 * **Puro**: sin `android` y sin Compose (RNF-64). `URLEncoder` es de `java.net`, que existe igual en el SDK de
 * Android, así que no hace falta ni `Uri` ni una dependencia.
 *
 * El cuerpo va **codificado por parámetro y nunca concatenado**: un informe lleva saltos de línea, acentos y
 * `&` a propósito, y sin codificar la URL se parte y la incidencia sale vacía (caso límite 6).
 */
object CrashIssueUrl {

    const val REPOSITORIO = "https://github.com/ESTRIN217/secure-notes"

    private const val PREFIJO_TITULO = "[Crash] "
    private const val TITULO_SIN_TIPO = "Secure Notes"

    /**
     * Título de la incidencia: el **tipo** de excepción y nada más.
     *
     * El mensaje puede llevar un fragmento de una nota del usuario y va al cuerpo —que es donde el usuario
     * puede leerlo y decidir— pero al título no: un título se ve en una lista pública (RNF-67).
     */
    fun titulo(primeraLinea: String): String {
        val tipo = primeraLinea.substringBefore(':').trim()
        return PREFIJO_TITULO + if (tipo.isEmpty()) TITULO_SIN_TIPO else tipo
    }

    /** Cuerpo de la incidencia: el informe entero, en bloque cercado para que GitHub no lo interprete. */
    fun cuerpo(texto: String): String = "$AVISO_DE_REVISAR\n\n```\n$texto\n```"

    fun url(titulo: String, cuerpo: String, base: String = REPOSITORIO): String =
        "$base/issues/new?title=${codificar(titulo)}&body=${codificar(cuerpo)}"

    /**
     * `URLEncoder` pone `+` donde hay espacio, que es lo correcto en un *form body* y raro en una query. Se
     * cambia por `%20` porque quien lee la URL es una persona.
     */
    private fun codificar(valor: String): String = URLEncoder.encode(valor, "UTF-8").replace("+", "%20")

    /**
     * Aviso que viaja en el propio cuerpo de la incidencia. El mensaje de la excepción es el único campo del
     * informe que esta app **no** controla, así que el aviso viaja con él y no solo en la pantalla (RNF-67).
     */
    private val AVISO_DE_REVISAR =
        "Enviado desde Secure Notes. El mensaje de la excepcion puede llevar un fragmento de una nota: " +
            "revisalo antes de publicar."
}