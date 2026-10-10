package com.example.util

import org.json.JSONObject
import java.io.File

/**
 * El sobre que lleva el Markdown del editor de código hasta la nota (spec 013).
 *
 * ## Por qué existe
 *
 * `CodeEditorActivity` vive **fuera** del flujo de bloqueo de la bóveda y no tiene el `NotesViewModel`:
 * escribir en la base de notas desde ahí saltaría la contraseña maestra (RF-81). El camino es por eso
 * escribir un fichero y arrancar `MainActivity`, que sí está dentro del flujo y sí crea la nota.
 *
 * ## Por qué un fichero y no el Intent
 *
 * El límite de Binder es de unos 1 MB por transacción. El contenido de bloque JSON de una nota de
 * 5 MB lo revienta, y el síntoma es una pantalla en blanco sin explicación (RNF-34). Un fichero en
 * `cacheDir` no tiene ese límite.
 *
 * ## Por qué el nombre es fijo
 *
 * El `Intent` lleva la ruta, y una ruta acaba en un `Log.e` cuando algo falla. Como el nombre del
 * archivo **es** del usuario, la ruta no puede llevarlo: el nombre va **dentro** del sobre, y el
 * fichero se llama siempre igual (RNF-36).
 */
object MarkdownHandoff {

    /** Nombre fijo y sin nada del usuario dentro (RNF-36). */
    const val FILE_NAME = "md_handoff.json"

    private const val KEY_NAME = "name"
    private const val KEY_MARKDOWN = "markdown"

    /**
     * Escribe el sobre y devuelve su ruta absoluta.
     *
     * @param directory donde vive, normalmente `context.cacheDir`.
     */
    fun write(directory: File, fileName: String, markdown: String): String {
        val destino = File(directory, FILE_NAME)
        val sobre = JSONObject()
            .put(KEY_NAME, fileName)
            .put(KEY_MARKDOWN, markdown)
        destino.writeText(sobre.toString())
        return destino.absolutePath
    }

    /**
     * Lee el sobre sin borrarlo.
     *
     * Devuelve `null` en cualquier problema —que no exista, que esté corrupto, que le falte un
     * campo— en vez de propagar la excepción: quien consume está en un `onNewIntent`, y una excepción
     * ahí tumba la aplicación.
     */
    fun read(path: String): Pair<String, String>? {
        val file = File(path)
        if (!file.exists()) return null
        return runCatching {
            val sobre = JSONObject(file.readText())
            val name = sobre.optString(KEY_NAME)
            if (name.isBlank() || !sobre.has(KEY_MARKDOWN)) return null
            name to sobre.getString(KEY_MARKDOWN)
        }.getOrNull()
    }

    /**
     * Lee y borra en un solo paso (RF-82, RNF-35).
     *
     * El borrado va **siempre**, también cuando el contenido estaba corrupto: un sobre a medio
     * consumed en `cacheDir` es un Markdown sin cifrar que se queda ahí, que es lo que este proyecto
     * no puede dejar.
     */
    fun consume(path: String): Pair<String, String>? {
        val leido = read(path)
        File(path).delete()
        return leido
    }
}