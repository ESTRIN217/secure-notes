package com.example.crash

import java.io.File

/**
 * El informe pendiente en disco (spec 020, RF-117, RF-122, RF-123).
 *
 * **Puro**: recibe la `File` ya resuelta en vez de un `Context`, y por eso sus pruebas corren en un
 * `TemporaryFolder` de verdad **sin Robolectric** — que en la plataforma de desarrollo no se ejecuta
 * (RNF-64, decisión 2 del plan). Escritura, renombrado y borrado son las tres cosas que había que probar.
 *
 * Guarda **solo el último fallo**: cada `guardar` sobrescribe (caso límite 1).
 */
class CrashStore(private val fichero: File) {

    /**
     * Escribe el informe de forma **atómica**: a un temporal y renombrando.
     *
     * Si alguien leyera a mitad —imposible en la práctica, porque el proceso va a morir, pero no cuesta
     * nada— vería el informe anterior entero o nada, nunca medio.
     */
    fun guardar(texto: String) {
        val destino = fichero
        destino.parentFile?.mkdirs()
        val temporal = File(destino.parentFile, "${destino.name}.tmp")
        temporal.writeText(texto, Charsets.UTF_8)
        if (!temporal.renameTo(destino)) {
            // Renombrar puede fallar si el destino está abierto en otro sitio: el informe entra igual.
            destino.writeText(texto, Charsets.UTF_8)
            temporal.delete()
        }
    }

    /** El informe, o `null` si no hay ninguno (RF-123). */
    fun leer(): String? {
        if (!hayPendiente()) return null
        return runCatching { fichero.readText(Charsets.UTF_8) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    fun hayPendiente(): Boolean = fichero.isFile && fichero.length() > 0

    /**
     * Borra el informe y dice si había alguno.
     *
     * «Consumir» es lo que hace que deje de avisarse: sin esto, la app volvería a avisar del mismo fallo en
     * cada arranque (RF-122).
     */
    fun consumir(): Boolean {
        val habia = hayPendiente()
        runCatching { fichero.delete() }
        return habia
    }
}